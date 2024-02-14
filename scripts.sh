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
