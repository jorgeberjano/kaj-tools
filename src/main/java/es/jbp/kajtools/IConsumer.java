package es.jbp.kajtools;

import es.jbp.kajtools.filter.MessageFilter;
import es.jbp.kajtools.filter.ScriptMessageFilter;
import es.jbp.kajtools.tabla.entities.RecordItem;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

public interface IConsumer<K extends GenericRecord, V extends GenericRecord> extends KafkaBase {

  String getDefaultTopic();

  List<String> getAvailableTopics();

  String getDomain();

  default List<RecordItem> consumeLastRecords(Environment environment, String topic,
      MessageFilter filter,  long maxRecordsPerPartition) throws KajException {
    try (KafkaConsumer<K, V> consumer =
        new KafkaConsumer<>(createConsumerProperties(environment))) {
      consumer.subscribe(Collections.singletonList(topic));

      consumer.poll(Duration.ofSeconds(5));

      List<RecordItem> latestRecords = new ArrayList<>();

      Map<TopicPartition, Long> offsets = consumer.endOffsets(consumer.assignment());

      for (Map.Entry<TopicPartition, Long> offsetEntry : offsets.entrySet()) {
        TopicPartition topicPartition = offsetEntry.getKey();
        Long offset = offsetEntry.getValue();
        final long newOffset = offset - maxRecordsPerPartition;
        consumer.seek(topicPartition, newOffset < 0 ? 0 : newOffset);

        ConsumerRecords<K, V> records;
        do {
          records = consumer.poll(Duration.ofSeconds(1));
          for (ConsumerRecord<K, V> r : records) {
            K key = r.key();

            System.out.println(key.getClass());
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

  default RecordItem createRecordItem(ConsumerRecord<K, V> rec) {
    String jsonKey = String.valueOf(rec.key());
    String jsonValue = String.valueOf(rec.value());

    LocalDateTime dateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(rec.timestamp()),
            TimeZone.getDefault().toZoneId());

    return RecordItem.builder()
        .partition(rec.partition())
        .offset(rec.offset())
        .dateTime(dateTime)
        .key(jsonKey)
        .value(jsonValue).build();
  }

  default MessageFilter createScriptFilter(String script) throws KajException {
    return new ScriptMessageFilter(script);
  }
}
