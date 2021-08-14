package es.jbp.kajtools;

import es.jbp.kajtools.tabla.RecordItem;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Vector;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public interface IConsumer<K, E> extends KafkaBase {

  String getDefaultTopic();

  List<String> getAvailableTopics();

  String getDomain();

  default Map<String, Object> createConsumerProperties(Environment environment) {
    Map<String, Object> props = KafkaBase.super.createProperties(environment);

    props.put(ConsumerConfig.GROUP_ID_CONFIG, "kaj-tools");

    putNotNull(props, ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
    putNotNull(props, ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());

    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    return props;
  }

  default List<RecordItem> consumeLastRecords(Environment environment, String topic, long maxRecordsPerPartition) throws KajException {
    try (KafkaConsumer<K, E> consumer = new KafkaConsumer<>(createConsumerProperties(environment))) {
      consumer.subscribe(Collections.singletonList(topic));

      consumer.poll(Duration.ofSeconds(10));

      List<RecordItem> latestRecords = new ArrayList<>();

      consumer.endOffsets(consumer.assignment()).forEach((topicPartition, offset) -> {

        final long newOffset = offset - maxRecordsPerPartition;
        consumer.seek(topicPartition, newOffset < 0 ? 0 : newOffset);

        ConsumerRecords<K, E> records;
        do {
          records = consumer.poll(Duration.ofSeconds(1));
          records.forEach(r -> latestRecords.add(createRecordItem(r)));
        } while (!records.isEmpty());
      });

      return latestRecords;
    } catch (Exception ex) {
      throw new KajException(ex.getMessage());
    }
  }

  default RecordItem createRecordItem(ConsumerRecord<K, E> rec) {
    String jsonKey = String.valueOf(rec.key());
    String jsonEvent = String.valueOf(rec.value());

    LocalDateTime dateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(rec.timestamp()),
            TimeZone.getDefault().toZoneId());

    return RecordItem.builder()
        .partition(rec.partition())
        .offset(rec.offset())
        .dateTime(dateTime)
        .key(jsonKey)
        .event(jsonEvent).build();
  }
  

}
