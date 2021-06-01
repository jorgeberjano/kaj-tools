package es.jbp.kajtools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import es.jbp.kajtools.util.AvroUtils;
import es.jbp.kajtools.util.JsonUtils;
import es.jbp.kajtools.util.ResourceUtil;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public abstract class AbstractTestProducer<K, E> implements TestProducer {

  private final Class<K> keyType;
  private final Class<E> eventType;

  public AbstractTestProducer(Class<K> keyType, Class<E> eventType) {
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
      throws Exception {
    Producer<K, E> producer;
    try {
      producer = createProducer(environment);
    } catch (Throwable ex) {
      throw new RuntimeException("No se ha podido crear el productor. " + ex.getLocalizedMessage(),
          ex.getCause());
    }
    sendDataFromJson(producer, topic, keyJson, eventJson);
    producer.close();
  }

  private Producer<K, E> createProducer(Environment environment) {
    return new KafkaProducer<>(createProperties(environment));
  }

  private void sendDataFromJson(Producer<K, E> producer, String topic, String keyJson,
      String eventJson) throws Exception {
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

  protected void sendDataList(Producer<K,E> producer, String topic, List<K> keyList, List<E> eventList)
      throws ExecutionException, InterruptedException {
    int n = Math.min(keyList.size(), eventList.size());

    for (int i = 0; i < n; i++) {
        sendData(producer, topic, keyList.get(i), eventList.get(i));
    }
  }

  private void sendData(Producer<K, E> producer, String topic, K key, E event)
      throws ExecutionException, InterruptedException {
    final ProducerRecord<K, E> record = new ProducerRecord<>(topic, key, event);
    record.headers().add("app.name", "test-event-producer".getBytes(StandardCharsets.UTF_8));
    record.headers()
        .add("user.name", System.getProperty("user.name").getBytes(StandardCharsets.UTF_8));
    Future<RecordMetadata> futureResponse = producer.send(record);
    RecordMetadata recordMetadata = futureResponse.get();
    System.out.println(recordMetadata);
  }


  @Override
  public String getEventSchema(String json) throws Exception {
    return AvroUtils.extractAvroSchema(buildEvent(json));
  }

  @Override
  public String getKeySchema(String json) throws Exception {
    return AvroUtils.extractAvroSchema(buildKey(json));
  }

  protected K buildKey(String json) throws Exception {
    return buildKeyFromJson(json);
  }

  protected E buildEvent(String json) throws Exception {
    return buildEventFromJson(json);
  }

  protected K buildKeyFromJson(String keyJson) throws Exception {
    try {
      return JsonUtils.stubFromJson(keyJson, keyType);
    } catch (Exception ex) {
      throw new Exception("No se puede generar el Key desde el JSON. Causa: " + ex.getMessage());
    }
  }

  protected List<K> buildKeyListFromJson(String keyArrayJson) throws Exception {
    try {
      return JsonUtils.createFromJson(keyArrayJson, new TypeReference<List<K>>() {
      });
    } catch (Exception ex) {
      throw new Exception(
          "No se puede generar la lista de Keys desde el JSON. Causa: " + ex.getMessage());
    }
  }

  private E buildEventFromJson(String eventJson) throws Exception {
    try {
      return JsonUtils.stubFromJson(eventJson, eventType);
    } catch (Exception ex) {
      throw new Exception("No se puede generar el Event desde el JSON. Causa: " + ex.getMessage());
    }
  }

  protected List<E> buildEventListFromJson(String eventArrayJson) throws Exception {
    try {
      return JsonUtils.createFromJson(eventArrayJson, new TypeReference<List<E>>() {
      });
    } catch (Exception ex) {
      throw new Exception(
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
}
