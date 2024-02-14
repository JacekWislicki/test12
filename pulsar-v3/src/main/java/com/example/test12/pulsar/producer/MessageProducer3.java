package com.example.test12.pulsar.producer;

import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;

import com.example.test12.commons.pulsar.producer.AbstractProducer;
import com.example.test12.commons.utils.Config;
import com.example.test12.model.TestMessage;

public class MessageProducer3 extends AbstractProducer<TestMessage> {

    MessageProducer3(String topic) throws PulsarClientException {
        super(topic, Schema.AVRO(TestMessage.class));
    }

    private TestMessage buildTestMessage() {
        TestMessage message = new TestMessage();
        message.setEventIdentifier("eventId");
        message.setActive(true);
        return message;
    }

    public static void main(String[] args) throws PulsarClientException {
        try (MessageProducer3 producer = new MessageProducer3(Config.TOPIC_1_NAME);) {
            TestMessage message = producer.buildTestMessage();
            producer.produce(message);
        }
    }
}
