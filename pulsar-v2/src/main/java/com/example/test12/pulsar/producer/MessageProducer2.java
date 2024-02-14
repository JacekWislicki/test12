package com.example.test12.pulsar.producer;

import java.util.Arrays;
import java.util.List;

import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;

import com.example.test12.commons.pulsar.producer.AbstractProducer;
import com.example.test12.commons.utils.Config;
import com.example.test12.model.PartInformation;
import com.example.test12.model.TestMessage;

public class MessageProducer2 extends AbstractProducer<TestMessage> {

    MessageProducer2(String topic) throws PulsarClientException {
        super(topic, Schema.AVRO(TestMessage.class));
    }

    private TestMessage buildTestMessage() {
        TestMessage message = new TestMessage();
        message.setEventIdentifier("eventId");
        message.setActive(true);
        List<PartInformation> parts = Arrays.asList(
            new PartInformation("number1", 1, "measure1"),
            new PartInformation("number2", 2, "measure2"));
        message.setPartInformation(parts);
        message.setDescription("description");
        return message;
    }

    public static void main(String[] args) throws PulsarClientException {
        try (MessageProducer2 producer = new MessageProducer2(Config.TOPIC_1_NAME);) {
            TestMessage message = producer.buildTestMessage();
            producer.produce(message);
        }
    }
}
