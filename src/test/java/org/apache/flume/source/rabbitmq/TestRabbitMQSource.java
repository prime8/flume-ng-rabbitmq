package org.apache.flume.source.rabbitmq;

import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.flume.CounterGroup;
import org.apache.flume.Event;
import org.apache.flume.PollableSource;
import org.apache.flume.channel.ChannelProcessor;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;

public class TestRabbitMQSource {

    private static final String NO_QUEUE_NAME = "", NO_EXCHANGE_NAME = "";
    private static final byte[] RABBIT_MESSAGE_BODY = "the body".getBytes();
    private static final Long RABBIT_MESSAGE_ID = 987654321L;

    @Mock private CounterGroup counterGroup;
    @Mock private ConnectionFactory connectionFactory;
    @Mock private ChannelProcessor channelProcessor;

    @Mock private Connection connection;
    @Mock private Channel channel;
    @Mock private GetResponse rabbitMessage;
    @Mock private BasicProperties rabbitMessageProperties;
    @Mock private Envelope rabbitMessageEnvelope;
    @Mock private Map<String, Object> rabbitMessageHeaders;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(connectionFactory.getHost()).thenReturn("NoHost");
    }

    private RabbitMQSource setUpSource(String queue, String exchange, String... topics) {
        RabbitMQSource source = new RabbitMQSource(counterGroup, connectionFactory,
                queue, exchange, topics);
        source.setName(getClass().getSimpleName());
        source.setChannelProcessor(channelProcessor);
        return source;
    }

    private void expectSuccessfulConnection() throws IOException {
        when(connectionFactory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);
    }

    private void expectRabbitMessageReceived(Map<String, Object> headers,
            byte[] body, long deliveryTag) throws IOException {
        when(channel.basicGet(NO_QUEUE_NAME, false)).thenReturn(rabbitMessage);
        when(rabbitMessage.getProps()).thenReturn(rabbitMessageProperties);
        when(rabbitMessageProperties.getHeaders()).thenReturn(headers);
        when(rabbitMessage.getBody()).thenReturn(body);
        when(rabbitMessage.getEnvelope()).thenReturn(rabbitMessageEnvelope);
        when(rabbitMessageEnvelope.getDeliveryTag()).thenReturn(deliveryTag);
    }

    @Test
    public void simplestHappyPathShouldGetRabbitMessageAndCreateFlumeEventFromIt()
            throws Exception {
        RabbitMQSource simplestSource = setUpSource(NO_QUEUE_NAME, NO_EXCHANGE_NAME);

        expectSuccessfulConnection();
        expectRabbitMessageReceived(rabbitMessageHeaders, RABBIT_MESSAGE_BODY, RABBIT_MESSAGE_ID);

        assertThat(simplestSource.process(), is(PollableSource.Status.READY));

        verify(channelProcessor).processEvent(withEventWithBody(RABBIT_MESSAGE_BODY));
        verify(channel).basicAck(RABBIT_MESSAGE_ID, false);
    }

    private Event withEventWithBody(final byte[] body) {
        return argThat(new BaseMatcher<Event>() {
            @Override
            public boolean matches(Object actual) {
                return actual != null
                        && actual instanceof Event
                        && ((Event)actual).getBody().equals(body);
            }
            @Override
            public void describeTo(Description description) {
                description.appendText("eventWithBody [")
                    .appendValue(new String(body))
                    .appendText("]");
            }
        });
    }

    @Test
    public void flumeEventHeadersShouldContainLowercasedRabbitMessageHeadersAndTimestamp()
            throws Exception {
        RabbitMQSource simplestSource = setUpSource(NO_QUEUE_NAME, NO_EXCHANGE_NAME);

        expectSuccessfulConnection();
        Map<String, Object> fooHeaders = new HashMap<String, Object>();
        fooHeaders.put("Foo", "Bar");
        expectRabbitMessageReceived(fooHeaders, RABBIT_MESSAGE_BODY, RABBIT_MESSAGE_ID);

        assertThat(simplestSource.process(), is(PollableSource.Status.READY));

        Map<String, String> eventHeaders = new HashMap<String, String>();
        eventHeaders.put("foo", "Bar"); // key is lowercased
        verify(channelProcessor).processEvent(withEventWithHeaders(eventHeaders ));
        verify(channel).basicAck(RABBIT_MESSAGE_ID, false);
    }

    private Event withEventWithHeaders(final Map<String, String> headers) {
        return argThat(new BaseMatcher<Event>() {
            @Override
            public boolean matches(Object actual) {
                return actual != null
                        && actual instanceof Event
                        && hasHeaders(((Event)actual).getHeaders(),headers);
            }
            private boolean hasHeaders(Map<String, String> actual, Map<String, String> expected) {
                for (Map.Entry<String, String> entry: expected.entrySet()) {
                    if (!actual.containsKey(entry.getKey())) return false;
                    if (!actual.get(entry.getKey()).equals(entry.getValue())) return false;
                }
                // timestamp header is added if not present
                return actual.containsKey("timestamp")
                        && Long.valueOf(actual.get("timestamp")) > 0;
            }
            @Override
            public void describeTo(Description description) {
                description.appendText("eventWithHeaders [")
                    .appendValue(headers)
                    .appendText("]");
            }
        });
    }

    @Test
    public void flumeProcessEventFailureShouldCauseRabbitMessageNack() throws Exception {
        RabbitMQSource simplestSource = setUpSource(NO_QUEUE_NAME, NO_EXCHANGE_NAME);

        expectSuccessfulConnection();
        expectRabbitMessageReceived(rabbitMessageHeaders, RABBIT_MESSAGE_BODY, RABBIT_MESSAGE_ID);

        Exception failure = new RuntimeException("testingChannelProcessorFailure");
        doThrow(failure).when(channelProcessor).processEvent(anyEvent());

        assertThat(simplestSource.process(), is(PollableSource.Status.BACKOFF));

        verify(channel).basicNack(RABBIT_MESSAGE_ID, false, true);
    }

    private Event anyEvent() {
        return argThat(any(Event.class));
    }
}
