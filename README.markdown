<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
-->

Flume-ng RabbitMQ
========

This project provides both a RabbitMQ source and sink for Flume-NG.  To use this plugin with your Flume installation, build from source using

<pre>mvn clean package</pre>

and put the resulting jar file in the lib directory in your flume installation.

This project is available under the Apache License.  

Configuration of RabbitMQ Source
------
The configuration of RabbitMQ sources requires that you either declare an exchange name or a queue name.

The exchange name option is helpful if you have declared an exchange in RabbitMQ, but want to use a
default named queue.  If you have a predeclared queue that you want to receive events from, then you can simply declare
the queue name and leave the exchange name out.  Another optional configuration option is the declaration of
topic routing keys that you want to listen to.  This is a comma-delimited list.

**Minimal Config Example**

	agent1.sources.rabbitmq-source1.channels = ch1  
	agent1.sources.rabbitmq-source1.type = org.apache.flume.source.rabbitmq.RabbitMQSource  
	agent1.sources.rabbitmq-source1.hostname = 10.10.10.173  
	
	agent1.sources.rabbitmq-source1.queuename = log_jammin 
	OR
	agent1.sources.rabbitmq-source1.exchangename = log_jammin_exchange

**Full Config Example**

	agent1.sources.rabbitmq-source1.channels = ch1  
	agent1.sources.rabbitmq-source1.type = org.apache.flume.source.rabbitmq.RabbitMQSource  
	agent1.sources.rabbitmq-source1.hostname = 10.10.10.173  
	
	agent1.sources.rabbitmq-source1.queuename = log_jammin
	OR
	agent1.sources.rabbitmq-source1.exchangename = log_jammin_exchange
	
	agent1.sources.rabbitmq-source1.topics = topic1,topic2
	agent1.sources.rabbitmq-source1.username = rabbitmquser
	agent1.sources.rabbitmq-source1.password = p@$$w0rd!
	agent1.sources.rabbitmq-source1.port = 12345
	agent1.sources.rabbitmq-source1.virtualhost = virtualhost1

RabbitMQ Sink
------
**Minimal Config Example**

	agent1.sinks.rabbitmq-sink1.channels = ch1  
	agent1.sinks.rabbitmq-sink1.type = org.apache.flume.sink.rabbitmq.RabbitMQSink  
	agent1.sinks.rabbitmq-sink1.hostname = 10.10.10.173  
	agent1.sinks.rabbitmq-sink1.queuename = log_jammin

**Full Config Example**

	agent1.sinks.rabbitmq-sink1.channels = ch1  
	agent1.sinks.rabbitmq-sink1.type = org.apache.flume.sink.rabbitmq.RabbitMQSink  
	agent1.sources.rabbitmq-source1.hostname = 10.10.10.173  
	agent1.sources.rabbitmq-source1.queuename = log_jammin
	agent1.sources.rabbitmq-source1.username = rabbitmquser
	agent1.sources.rabbitmq-source1.password = p@$$w0rd!
	agent1.sources.rabbitmq-source1.port = 12345
	agent1.sources.rabbitmq-source1.virtualhost = virtualhost1
	agent1.sources.rabbitmq-source1.exchange = exchange1
