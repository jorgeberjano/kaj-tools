package es.jbp.kajtools.kafka;

import es.jbp.kajtools.util.JsonUtils;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.AllArgsConstructor;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;

@AllArgsConstructor
public class RecordConsumer<K, V> implements ConsumerRebalanceListener {
  private final KafkaConsumer<K, V> consumer;
  private final Class<K> keyType;
  private final Class<V> valueType;
  private LocalDateTime dateTimeToRewind;
  private final AtomicBoolean abort;
  private final ConsumerFeedback feedback;

  private static final int START_TIMEOUT = 20;
  private static final int WAITING_TIMEOUT = 5;

  @Override
  public void onPartitionsRevoked(Collection<TopicPartition> collection) {
    feedback.message("Partitions revoked");
  }

  @Override
  public void onPartitionsAssigned(Collection<TopicPartition> collection) {
    feedback.message("Partitions assigned");
    rewindOffsets();
  }

  private void rewindOffsets() {

    Set<TopicPartition> topicPartitions = consumer.assignment();

    if (dateTimeToRewind == null) {
      consumer.seekToBeginning(topicPartitions);
      return;
    }

    consumer.seekToEnd(topicPartitions);

    long time = dateTimeToRewind.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    Map<TopicPartition, Long> topicPartitionToTimestampMap = topicPartitions.stream()
        .collect(Collectors.toMap(tp -> tp, tp -> time));

    Map<TopicPartition, OffsetAndTimestamp> offsets = consumer.offsetsForTimes(topicPartitionToTimestampMap);

    for (Map.Entry<TopicPartition, OffsetAndTimestamp> offsetEntry : offsets.entrySet()) {
      TopicPartition topicPartition = offsetEntry.getKey();
      if (offsetEntry.getValue() != null) {
        long offset = offsetEntry.getValue().offset();
        consumer.seek(topicPartition, offset);
      }
    }
  }

  public void startConsumption() {

    consumeRecords(abort, feedback, consumer);

    feedback.finished();
  }
  private void consumeRecords(AtomicBoolean abort, ConsumerFeedback feedback, KafkaConsumer<K, V> consumer) {

    ConsumerRecords<K, V> records;
    Instant baseInstant = Instant.now();
    boolean starting = true;
    do {
      records = consumer.poll(Duration.ofMillis(100));
      if (!records.isEmpty()) {
        List<RecordItem> messages = consume(records);
        feedback.consumedRecords(messages);
        baseInstant = Instant.now();
      }
      if (!mustWait(baseInstant, starting)) {
        feedback.message("Timeout");
        break;
      }
    } while (!abort.get());
  }

  private List<RecordItem> consume(ConsumerRecords<K, V> records) {
    List<RecordItem> messages = new ArrayList<>();
    for (ConsumerRecord<K, V> rec : records) {
      messages.add(createRecordItem(rec));
    }
    return messages;
  }

  private boolean mustWait(Instant baseInstant, boolean starting) {
    return Duration.between(baseInstant, Instant.now())
        .compareTo(Duration.ofSeconds(starting ? START_TIMEOUT : WAITING_TIMEOUT)) < 0;
  }

  private RecordItem createRecordItem(ConsumerRecord<K, V> rec) {

    String jsonKey = String.valueOf(rec.key());
    String jsonValue = String.valueOf(rec.value());

    String keyError = null;
    String valueError = null;
    if (!keyType.equals(GenericRecord.class)) {
      try {
        K key = JsonUtils.createFromJson(jsonKey, keyType);
      } catch (IOException e) {
        keyError = e.getMessage();
      }
    }

    if (!valueType.equals(GenericRecord.class)) {
      try {
        V value = JsonUtils.createFromJson(jsonValue, valueType);
      } catch (IOException e) {
        valueError = e.getMessage();
      }
    }

    LocalDateTime dateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(rec.timestamp()),
            TimeZone.getDefault().toZoneId());

    return RecordItem.builder()
        .partition(rec.partition())
        .offset(rec.offset())
        .dateTime(dateTime)
        .key(jsonKey)
        .keyError(keyError)
        .value(jsonValue)
        .headers(toHeaderItems(rec.headers()))
        .valueError(valueError)
        .build();
  }

  private List<HeaderItem> toHeaderItems(Headers headers) {
    return StreamSupport.stream(headers.spliterator(), false)
        .map(this::toHeaderItem)
        .collect(Collectors.toList());
  }

  private HeaderItem toHeaderItem(Header header) {
    return HeaderItem.builder()
        .key(header.key())
        .value(new String(header.value()))
        .build();
  }

}
