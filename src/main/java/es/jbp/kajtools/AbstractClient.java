package es.jbp.kajtools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import es.jbp.kajtools.tabla.RecordItem;
import es.jbp.kajtools.util.AvroUtils;
import es.jbp.kajtools.util.JsonUtils;
import es.jbp.kajtools.util.ResourceUtil;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public abstract class AbstractClient<K, E> implements IProducer, IConsumer<K, E> {

  private final Class<K> keyType;
  private final Class<E> eventType;

  protected AbstractClient(Class<K> keyType, Class<E> eventType) {
    this.keyType = keyType;
    this.eventType = eventType;
  }

  @Override
  public String getKeyClassName() {
    return keyType.getName();
  }

  @Override
  public String getEventClassName() {
    return eventType.getName();
  }

  @Override
  public void sendFromJson(Environment environment, String topic, String keyJson, String eventJson)
      throws KajException {

    try (Producer<K, E> producer = createProducer(environment)) {
      sendDataFromJson(producer, topic, keyJson, eventJson);
    } catch (Throwable ex) {
      throw new KajException("No se ha podido realizar el envío. " + ex.getLocalizedMessage());
    }
  }

  private Producer<K, E> createProducer(Environment environment) {
    return new KafkaProducer<>(createProducerProperties(environment));
  }

  private void sendDataFromJson(Producer<K, E> producer, String topic, String keyJson,
      String eventJson) throws KajException {
    List<K> keyList;
    if (JsonUtils.isArray(keyJson)) {
      keyList = buildKeyListFromJson(keyJson);
    } else {
      keyList = Collections.singletonList(buildKeyFromJson(keyJson));
    }
    List<E> eventList;
    if (JsonUtils.isArray(keyJson)) {
      eventList = buildEventListFromJson(eventJson);
    } else {
      eventList = Collections.singletonList(buildEventFromJson(eventJson));
    }

    sendDataList(producer, topic, keyList, eventList);
  }

  protected void sendDataList(Producer<K, E> producer, String topic, List<K> keyList, List<E> eventList)
      throws KajException {
    int n = Math.min(keyList.size(), eventList.size());

    for (int i = 0; i < n; i++) {
      sendData(producer, topic, keyList.get(i), eventList.get(i));
    }
  }

  private void sendData(Producer<K, E> producer, String topic, K key, E event)
      throws KajException {
    final ProducerRecord<K, E> producerRecord = new ProducerRecord<>(topic, key, event);
    producerRecord.headers().add("app.name", "test-event-producer".getBytes(StandardCharsets.UTF_8));
    producerRecord.headers()
        .add("user.name", System.getProperty("user.name").getBytes(StandardCharsets.UTF_8));
    Future<RecordMetadata> futureResponse = producer.send(producerRecord);

    RecordMetadata recordMetadata = null;
    try {
      recordMetadata = futureResponse.get();
    } catch (Exception e) {
      throw new KajException(e.getMessage());
    }
    System.out.println(recordMetadata);
  }


  @Override
  public String getEventSchema(String json) throws KajException {
    return AvroUtils.extractAvroSchema(buildEvent(json));
  }

  @Override
  public String getKeySchema(String json) throws KajException {
    return AvroUtils.extractAvroSchema(buildKey(json));
  }

  protected K buildKey(String json) throws KajException {
    return buildKeyFromJson(json);
  }

  protected E buildEvent(String json) throws KajException {
    return buildEventFromJson(json);
  }

  protected K buildKeyFromJson(String keyJson) throws KajException {
    try {
      return JsonUtils.stubFromJson(keyJson, keyType);
    } catch (Exception ex) {
      throw new KajException("No se puede generar el Key desde el JSON. Causa: " + ex.getMessage());
    }
  }

  protected List<K> buildKeyListFromJson(String keyArrayJson) throws KajException {
    try {
      return JsonUtils.createFromJson(keyArrayJson, new TypeReference<List<K>>() {
      });
    } catch (Exception ex) {
      throw new KajException(
          "No se puede generar la lista de Keys desde el JSON. Causa: " + ex.getMessage());
    }
  }

  private E buildEventFromJson(String eventJson) throws KajException {
    try {
      return JsonUtils.stubFromJson(eventJson, eventType);
    } catch (Exception ex) {
      throw new KajException("No se puede generar el Event desde el JSON. Causa: " + ex.getMessage());
    }
  }

  protected List<E> buildEventListFromJson(String eventArrayJson) throws KajException {
    try {
      return JsonUtils.createFromJson(eventArrayJson, new TypeReference<List<E>>() {
      });
    } catch (Exception ex) {
      throw new KajException(
          "No se puede generar la lista de Events desde el JSON. Causa: " + ex.getMessage());
    }
  }

  @Override
  public List<String> getAvailableTopics() {
    return Lists.newArrayList(getDefaultTopic());
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  @Getter(lazy = true)
  private final List<String> availableKeys = ResourceUtil.getResourceFileNames(getFolder()).stream()
      .filter(s -> s.toLowerCase().contains("key.")).collect(
          Collectors.toList());

  @Getter(lazy = true)
  private final List<String> availableEvents = ResourceUtil.getResourceFileNames(getFolder())
      .stream().filter(s -> s.toLowerCase().contains("event.")).collect(
          Collectors.toList());

  public String getFolder() {
    return ".";
  }

  @Override
  public String getDomain() {
    String[] nameSplit = getClass().getName().split("\\.");
    return nameSplit.length > 2 ? nameSplit[nameSplit.length - 2] : "";
  }

  // CONSUMER

  private Consumer<K, E> createConsumer(Environment environment) {
    return new KafkaConsumer<>(createConsumerProperties(environment));
  }



//  public List<RecordItem> consumeJsonEvents(Environment environment, String topic) {
//
//    try (Consumer<K, E> consumer = createConsumer(environment)) {
//
//      List<RecordItem> list = new ArrayList<>();
//      consumer.subscribe(Collections.singletonList(topic));
//
//      for (int i = 0; i < 10; i++) {
//        ConsumerRecords<K, E> records = consumer.poll(Duration.ofMillis(500));
//        for (ConsumerRecord<K, E> rec : records) {
//          RecordItem item = createRecordTableItem(rec);
//          if (item != null) {
//            list.add(item);
//          }
//        }
//      }
//      return list;
//    }
//  }

//  private RecordItem createRecordTableItem(ConsumerRecord<K, E> rec) {
//    String jsonKey = String.valueOf(rec.key());
//    String jsonEvent = String.valueOf(rec.value());
//
//    LocalDateTime dateTime =
//        LocalDateTime.ofInstant(Instant.ofEpochMilli(rec.timestamp()),
//            TimeZone.getDefault().toZoneId());
//
//    return RecordItem.builder()
//        .partition(rec.partition())
//        .offset(rec.offset())
//        .dateTime(dateTime)
//        .key(jsonKey)
//        .event(jsonEvent).build();
//  }



}
