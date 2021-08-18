package es.jbp.kajtools;

import es.jbp.kajtools.filter.MessageFilter;
import es.jbp.kajtools.filter.ScriptMessageFilter;
import es.jbp.kajtools.tabla.RecordItem;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

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

  default List<RecordItem> consumeLastRecords(Environment environment, String topic,
      MessageFilter filter,  long maxRecordsPerPartition) throws KajException {
    try (KafkaConsumer<K, E> consumer = new KafkaConsumer<>(createConsumerProperties(environment))) {
      consumer.subscribe(Collections.singletonList(topic));

      consumer.poll(Duration.ofSeconds(5));

      List<RecordItem> latestRecords = new ArrayList<>();

      Map<TopicPartition, Long> offsets = consumer.endOffsets(consumer.assignment());

      for (Map.Entry<TopicPartition, Long> offsetEntry : offsets.entrySet()) {
        TopicPartition topicPartition = offsetEntry.getKey();
        Long offset = offsetEntry.getValue();
        final long newOffset = offset - maxRecordsPerPartition;
        consumer.seek(topicPartition, newOffset < 0 ? 0 : newOffset);

        ConsumerRecords<K, E> records;
        do {
          records = consumer.poll(Duration.ofSeconds(1));
          for (ConsumerRecord<K, E> r : records) {
            if (filter.satisfyCondition(Objects.toString(r.key()), Objects.toString(r.value()))) {
              latestRecords.add(createRecordItem(r));
            }
          }
        } while (!records.isEmpty());
      }

      return latestRecords;
    } catch (KajException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new KajException("Error al consumir mensajes", ex);
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

  default MessageFilter createScriptFilter(String script) throws KajException {
    return new ScriptMessageFilter(script);
  }
}
