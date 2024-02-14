# AVRO SCHEMA EVOLUTION ISSUES IN PULSAR-FLINK CONNECTOR
This project in an MRE for some issues spotted when using Avro schema evolution in the Pulsar-Flink connector.
Library versions:
* Pulsar 3.0.1
* Flink 1.17.2
* Pulsar-Flink connector 4.1.0-1.17

# BUILD PROJECT TO GENERATE SCHEMAS AND MODELS
This is a simple Maven project, build it with:
```sh
mvn clean package
```
# Configure Pulsar

## Copy schemas
Original (raw) Avro schemas (.avsc files) are available as:
* v1: models-v1/src/main/resources/schemas/avro/test/v1/TestMessage.avsc
* v1: models-v2/src/main/resources/schemas/avro/test/v2/TestMessage.avsc
* v3: models-v3/src/main/resources/schemas/avro/test/v3/TestMessage.avsc

Copy generated Avro schemas to your Pulsar's schemas directory (the paths may be adjusted, here they match the script below):
* models-v1/target/schemas/avro/test/v1/TestMessage.json -> /pulsar/schemas/test12/v1/
* models-v2/target/schemas/avro/test/v1/TestMessage.json -> /pulsar/schemas/test12/v2/
* models-v3/target/schemas/avro/test/v3/TestMessage.json -> /pulsar/schemas/test12/v3/

## Run script
Execute the following script:
```sh
#!/bin/bash

cd /pulsar/bin

## NAMESPACES
#!/bin/bash

cd /pulsar/bin

## NAMESPACES
./pulsar-admin namespaces create public/test12
./pulsar-admin namespaces set-is-allow-auto-update-schema --disable public/test12
./pulsar-admin namespaces set-schema-compatibility-strategy --compatibility ALWAYS_COMPATIBLE public/test12
./pulsar-admin namespaces set-schema-validation-enforce --enable public/test12

## TOPICS
#./pulsar-admin topics delete-partitioned-topic persistent://public/test12/topic1

./pulsar-admin topics create-partitioned-topic --partitions 1 persistent://public/test12/topic1
./pulsar-admin topics create-subscription persistent://public/test12/topic1 -s topic1-flink

## SCHEMAS
#./pulsar-admin schemas delete persistent://public/test12/topic1

./pulsar-admin schemas upload \
    --filename /pulsar/schemas/test12/v1/TestMessage.json \
    persistent://public/test12/topic1
./pulsar-admin schemas upload \
    --filename /pulsar/schemas/test12/v2/TestMessage.json \
    persistent://public/test12/topic1
./pulsar-admin schemas upload \
    --filename /pulsar/schemas/test12/v3/TestMessage.json \
    persistent://public/test12/topic1
```

# USAGE
The tested Flink job is *com.example.test12.flink.job.MessageJob1* (in module flink-v1) expecting on the message schema v1. There are subtle differences in the schemas v2 and v3 comparing to v1:
* v1 -> v2: 
    * added field *description*
* v1 -> v3:
    * removed field *partInformation*

Each schema version has its own producer:
* v1: *com.example.test12.pulsar.producer.MessageProducer1* in module pulsar-v1
* v2: *com.example.test12.pulsar.producer.MessageProducer2* in module pulsar-v2
* v3: *com.example.test12.pulsar.producer.MessageProducer3* in module pulsar-v3

Run *MessageJob1* and a selected producer to observe the behaviour.

# BEHAVIOUR
## VALID: expected schema v1 vs. actual schema v1 (just a working reference)
Producer to use: *MessageProducer1*

Sent message: 
```
{"eventIdentifier": "eventId", "active": true, "partInformation": [{"genuinePartNumber": "number1", "genuinePartQuantity": 1, "genuinePartMeasure": "measure1"}, {"genuinePartNumber": "number2", "genuinePartQuantity": 2, "genuinePartMeasure": "measure2"}]}
```
Expected decoded message:
```
{"eventIdentifier": "eventId", "active": true, "partInformation": [{"genuinePartNumber": "number1", "genuinePartQuantity": 1, "genuinePartMeasure": "measure1"}, {"genuinePartNumber": "number2", "genuinePartQuantity": 2, "genuinePartMeasure": "measure2"}]}
```
Actual result:
```
{"eventIdentifier": "eventId", "active": true, "partInformation": [{"genuinePartNumber": "number1", "genuinePartQuantity": 1, "genuinePartMeasure": "measure1"}, {"genuinePartNumber": "number2", "genuinePartQuantity": 2, "genuinePartMeasure": "measure2"}]}
```
## VALID: expected schema v1 vs. actual schema v2
Producer to use: *MessageProducer2*

Sent message: 
```
{"eventIdentifier": "eventId", "active": true, "partInformation": [{"genuinePartNumber": "number1", "genuinePartQuantity": 1, "genuinePartMeasure": "measure1"}, {"genuinePartNumber": "number2", "genuinePartQuantity": 2, "genuinePartMeasure": "measure2"}], "description": "description"}
```
Expected decoded message (field *description* present in v2 while absent in v1, should be ignored):
```
{"eventIdentifier": "eventId", "active": true, "partInformation": [{"genuinePartNumber": "number1", "genuinePartQuantity": 1, "genuinePartMeasure": "measure1"}, {"genuinePartNumber": "number2", "genuinePartQuantity": 2, "genuinePartMeasure": "measure2"}]}
```
Actual result:
```
{"eventIdentifier": "eventId", "active": true, "partInformation": [{"genuinePartNumber": "number1", "genuinePartQuantity": 1, "genuinePartMeasure": "measure1"}, {"genuinePartNumber": "number2", "genuinePartQuantity": 2, "genuinePartMeasure": "measure2"}]}
```
## INVALID: expected schema v1 vs. actual schema v3
Producer to use: *MessageProducer3*

Sent message: 
```
{"eventIdentifier": "eventId", "active": true}
```
Expected decoded message (*partInformation* present in v1 but absent in v2, should be set to *null*):
```
{"eventIdentifier": "eventId", "active": true, "partInformation": null}
```
Actual result (exception):
```
org.apache.pulsar.client.api.SchemaSerializationException: java.io.EOFException
    at org.apache.pulsar.client.impl.schema.reader.AvroReader.read(AvroReader.java:80)
    at org.apache.pulsar.client.api.schema.SchemaReader.read(SchemaReader.java:40)
    at org.apache.pulsar.client.impl.schema.reader.AbstractMultiVersionReader.read(AbstractMultiVersionReader.java:61)
    at org.apache.pulsar.client.api.schema.SchemaReader.read(SchemaReader.java:40)
    at org.apache.pulsar.client.impl.schema.AbstractStructSchema.decode(AbstractStructSchema.java:66)
    at org.apache.flink.connector.pulsar.source.reader.deserializer.PulsarSchemaWrapper.deserialize(PulsarSchemaWrapper.java:66)
    at org.apache.flink.connector.pulsar.source.reader.PulsarRecordEmitter.emitRecord(PulsarRecordEmitter.java:53)
    at org.apache.flink.connector.pulsar.source.reader.PulsarRecordEmitter.emitRecord(PulsarRecordEmitter.java:33)
    at org.apache.flink.connector.base.source.reader.SourceReaderBase.pollNext(SourceReaderBase.java:144)
    at org.apache.flink.connector.pulsar.source.reader.PulsarSourceReader.pollNext(PulsarSourceReader.java:130)
    at org.apache.flink.streaming.api.operators.SourceOperator.emitNext(SourceOperator.java:419)
    at org.apache.flink.streaming.runtime.io.StreamTaskSourceInput.emitNext(StreamTaskSourceInput.java:68)
    at org.apache.flink.streaming.runtime.io.StreamOneInputProcessor.processInput(StreamOneInputProcessor.java:65)
    at org.apache.flink.streaming.runtime.tasks.StreamTask.processInput(StreamTask.java:550)
    at org.apache.flink.streaming.runtime.tasks.mailbox.MailboxProcessor.runMailboxLoop(MailboxProcessor.java:231)
    at org.apache.flink.streaming.runtime.tasks.StreamTask.runMailboxLoop(StreamTask.java:839)
    at org.apache.flink.streaming.runtime.tasks.StreamTask.invoke(StreamTask.java:788)
    at org.apache.flink.runtime.taskmanager.Task.runWithSystemExitMonitoring(Task.java:952)
    at org.apache.flink.runtime.taskmanager.Task.restoreAndInvoke(Task.java:931)
    at org.apache.flink.runtime.taskmanager.Task.doRun(Task.java:745)
    at org.apache.flink.runtime.taskmanager.Task.run(Task.java:562)
    at java.base/java.lang.Thread.run(Thread.java:833)
Caused by: java.io.EOFException: null
    at org.apache.avro.io.BinaryDecoder.ensureBounds(BinaryDecoder.java:542)
    at org.apache.avro.io.BinaryDecoder.readInt(BinaryDecoder.java:173)
    at org.apache.avro.io.BinaryDecoder.readIndex(BinaryDecoder.java:493)
    at org.apache.avro.io.ResolvingDecoder.readIndex(ResolvingDecoder.java:282)
    at org.apache.avro.generic.GenericDatumReader.readWithoutConversion(GenericDatumReader.java:188)
    at org.apache.avro.specific.SpecificDatumReader.readField(SpecificDatumReader.java:136)
    at org.apache.avro.reflect.ReflectDatumReader.readField(ReflectDatumReader.java:298)
    at org.apache.avro.generic.GenericDatumReader.readRecord(GenericDatumReader.java:248)
    at org.apache.avro.specific.SpecificDatumReader.readRecord(SpecificDatumReader.java:123)
    at org.apache.avro.generic.GenericDatumReader.readWithoutConversion(GenericDatumReader.java:180)
    at org.apache.avro.generic.GenericDatumReader.read(GenericDatumReader.java:161)
    at org.apache.avro.generic.GenericDatumReader.read(GenericDatumReader.java:154)
    at org.apache.pulsar.client.impl.schema.reader.AvroReader.read(AvroReader.java:78)
    ... 21 common frames omitted
```
