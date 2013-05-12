package org.apache.flume.source.rabbitmq;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private static final String[] NO_TOPICS = {};
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

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(connectionFactory.getHost()).thenReturn("NoHost");
    }

    @Test
    public void simplestHappyPathShouldGetRabbitMessageAndCreateFlumeEventFromIt()
            throws Exception {
        RabbitMQSource simplestSource = new RabbitMQSource(counterGroup, connectionFactory,
                NO_QUEUE_NAME, NO_EXCHANGE_NAME, NO_TOPICS);
        simplestSource.setName(getClass().getSimpleName());
        simplestSource.setChannelProcessor(channelProcessor);

        when(connectionFactory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);
        when(channel.basicGet(NO_QUEUE_NAME, false)).thenReturn(rabbitMessage);
        when(rabbitMessage.getProps()).thenReturn(rabbitMessageProperties);
        when(rabbitMessage.getBody()).thenReturn(RABBIT_MESSAGE_BODY);
        when(rabbitMessage.getEnvelope()).thenReturn(rabbitMessageEnvelope);
        when(rabbitMessageEnvelope.getDeliveryTag()).thenReturn(RABBIT_MESSAGE_ID);

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
}
