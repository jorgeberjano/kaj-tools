package es.jbp.kajtools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import es.jbp.kajtools.util.AvroUtils;
import es.jbp.kajtools.util.JsonUtils;
import es.jbp.kajtools.util.ResourceUtil;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public abstract class AbstractClient<K, V> implements IProducer, IConsumer<K, V> {

  private final Class<K> keyType;
  private final Class<V> valueType;

  protected AbstractClient(Class<K> keyType, Class<V> valueType) {
    this.keyType = keyType;
    this.valueType = valueType;
  }

  @Override
  public String getKeyClassName() {
    return keyType.getName();
  }

  @Override
  public String getValueClassName() {
    return valueType.getName();
  }

  @Override
  public void sendFromJson(Environment environment, String topic, String keyJson, String valueJson)
      throws KajException {

    try (Producer<K, V> producer = createProducer(environment)) {
      sendDataFromJson(producer, topic, keyJson, valueJson);
    } catch (Throwable ex) {
      throw new KajException("No se ha podido realizar el envío. " + ex.getLocalizedMessage());
    }
  }

  private Producer<K, V> createProducer(Environment environment) {
    return new KafkaProducer<>(createProducerProperties(environment));
  }

  private void sendDataFromJson(Producer<K, V> producer, String topic, String keyJson,
      String valueJson) throws KajException {
    List<K> keyList;
    if (JsonUtils.isArray(keyJson)) {
      keyList = buildKeyListFromJson(keyJson);
    } else {
      keyList = Collections.singletonList(buildKeyFromJson(keyJson));
    }
    List<V> valueList;
    if (JsonUtils.isArray(keyJson)) {
      valueList = buildValueListFromJson(valueJson);
    } else {
      valueList = Collections.singletonList(buildValueFromJson(valueJson));
    }

    sendDataList(producer, topic, keyList, valueList);
  }

  protected void sendDataList(Producer<K, V> producer, String topic, List<K> keyList, List<V> valueList)
      throws KajException {
    int n = Math.min(keyList.size(), valueList.size());

    for (int i = 0; i < n; i++) {
      sendData(producer, topic, keyList.get(i), valueList.get(i));
    }
  }

  private void sendData(Producer<K, V> producer, String topic, K key, V value)
      throws KajException {
    final ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, key, value);
    producerRecord.headers().add("app.name", "kaj-tools".getBytes(StandardCharsets.UTF_8));
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
  public String getValueSchema(String json) throws KajException {
    return AvroUtils.extractAvroSchema(buildValue(json));
  }

  @Override
  public String getKeySchema(String json) throws KajException {
    return AvroUtils.extractAvroSchema(buildKey(json));
  }

  protected K buildKey(String json) throws KajException {
    return buildKeyFromJson(json);
  }

  protected V buildValue(String json) throws KajException {
    return buildValueFromJson(json);
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

  private V buildValueFromJson(String valueJson) throws KajException {
    try {
      return JsonUtils.stubFromJson(valueJson, valueType);
    } catch (Exception ex) {
      throw new KajException("No se puede generar el Value desde el JSON. Causa: " + ex.getMessage());
    }
  }

  protected List<V> buildValueListFromJson(String valueArrayJson) throws KajException {
    try {
      return JsonUtils.createFromJson(valueArrayJson, new TypeReference<List<V>>() {
      });
    } catch (Exception ex) {
      throw new KajException(
          "No se puede generar la lista de Values desde el JSON. Causa: " + ex.getMessage());
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
  private final List<String> availableValues = ResourceUtil.getResourceFileNames(getFolder())
      .stream().filter(s -> s.toLowerCase().contains("value.")).collect(
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

  protected Consumer<K, V> createConsumer(Environment environment) {
    return new KafkaConsumer<>(createConsumerProperties(environment));
  }



}
