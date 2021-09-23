package es.jbp.kajtools.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import es.jbp.kajtools.Environment;
import es.jbp.kajtools.IMessageClient;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.util.AvroUtils;
import es.jbp.kajtools.util.JsonUtils;
import es.jbp.kajtools.util.ResourceUtil;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;


public abstract class AbstractClient<K, V> implements IMessageClient {

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
  public void sendFromJson(Environment environment, String topic, String keyJson, String valueJson, String headers)
      throws KajException {

    try (Producer<K, V> producer = createProducer(environment)) {
      sendDataFromJson(producer, topic, keyJson, valueJson, headers);
    } catch (Throwable ex) {
      throw new KajException("No se ha podido realizar el envío. ", ex);
    }
  }

  private Producer<K, V> createProducer(Environment environment) {
    return new KafkaProducer<>(createProducerProperties(environment));
  }

  private void sendDataFromJson(Producer<K, V> producer, String topic,
      String keyJson, String valueJson, String headers) throws KajException {
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
    Properties headersProperties = new Properties();
    try {
      headersProperties.load(new StringReader(headers));
    } catch (IOException e) {
      throw new KajException("No se han podido parsear las cabeceras", e);
    }
    sendDataList(producer, topic, keyList, valueList, headersProperties);
  }

  protected void sendDataList(Producer<K, V> producer, String topic, List<K> keyList, List<V> valueList,
      Properties headers)
      throws KajException {
    int n = Math.min(keyList.size(), valueList.size());

    for (int i = 0; i < n; i++) {
      sendData(producer, topic, keyList.get(i), valueList.get(i), headers);
    }
  }

  private void sendData(Producer<K, V> producer, String topic, K key, V value, Properties headers)
      throws KajException {
    final ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, key, value);

    headers.forEach((key1, value1) -> producerRecord.headers().add(Objects.toString(key1),
        Objects.toString(value1).getBytes(StandardCharsets.UTF_8)));

    Future<RecordMetadata> futureResponse = producer.send(producerRecord);

    RecordMetadata recordMetadata = null;
    try {
      recordMetadata = futureResponse.get();
    } catch (Exception e) {
      throw new KajException(e.getMessage());
    }
    // TODO: hacer algo con los metadatos
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
      return JsonUtils.createFromJson(keyJson, keyType);
    } catch (Exception ex) {
      throw new KajException("No se puede generar el Key desde el JSON", ex);
    }
  }

  protected List<K> buildKeyListFromJson(String keyArrayJson) throws KajException {
    try {
      return JsonUtils.createFromJson(keyArrayJson, new TypeReference<List<K>>() {
      });
    } catch (Exception ex) {
      throw new KajException(
          "No se puede generar la lista de Keys desde el JSON", ex);
    }
  }

  private V buildValueFromJson(String valueJson) throws KajException {
    try {
      return JsonUtils.createFromJson(valueJson, valueType);
    } catch (Exception ex) {
      throw new KajException("No se puede generar el Value desde el JSON", ex);
    }
  }

  protected List<V> buildValueListFromJson(String valueArrayJson) throws KajException {
    try {
      return JsonUtils.createFromJson(valueArrayJson, new TypeReference<List<V>>() {
      });
    } catch (Exception ex) {
      throw new KajException(
          "No se puede generar la lista de Values desde el JSON", ex);
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
  private final List<String> availableKeys = getAvailableResources("key.json");

  @Getter(lazy = true)
  private final List<String> availableValues = getAvailableResources("value.json");

  @Getter(lazy = true)
  private final List<String> availableHeaders = getAvailableResources("headers.properties");

  private List<String> getAvailableResources(String containing) {
    return ResourceUtil.getResourceFileNames(getFolder())
        .stream().filter(s -> s.toLowerCase().contains(containing))
        .collect(Collectors.toList());
  }

  public String getFolder() {
    return ".";
  }

  @Override
  public String getDomain() {
    String[] nameSplit = getClass().getName().split("\\.");
    return nameSplit.length > 2 ? nameSplit[nameSplit.length - 2] : "";
  }

//  protected Consumer<K, V> createConsumer(Environment environment) {
//    return new KafkaConsumer<>(createConsumerProperties(environment));
//  }

  public static Map<String, Object> createProducerProperties(Environment environment) {
    Map<String, Object> props = createCommonProperties(environment);

    putNotNull(props, ProducerConfig.ACKS_CONFIG, "all");
    putNotNull(props, ProducerConfig.RETRIES_CONFIG, 0);
    putNotNull(props, ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    putNotNull(props, ProducerConfig.LINGER_MS_CONFIG, 0);
    putNotNull(props, ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

    putNotNull(props, ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
    putNotNull(props, ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());

    return props;
  }

  public static Map<String, Object> createConsumerProperties(Environment environment) {
    Map<String, Object> props = createCommonProperties(environment);

    props.put(ConsumerConfig.GROUP_ID_CONFIG, "kaj-tools");

    putNotNull(props, ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
    putNotNull(props, ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());

    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    return props;
  }

  public static Map<String, Object> createCommonProperties(Environment environment) {
    Map<String, Object> props = new HashMap<>();

    putNotNull(props, ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getBootstrapServers());

    putNotNull(props, AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
        environment.getUrlSchemaRegistry());
    putNotNull(props, AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS,
        environment.isAutoRegisterSchemas());

    if (!Objects.isNull(environment.getUserSchemaRegistry())) {
      putNotNull(props, AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
      putNotNull(props, AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG,
          environment.getUserSchemaRegistry() + ":" + environment.getPasswordSchemaRegistry());
    }
    putNotNull(props, "security.protocol", environment.getSecurityProtocol());
    putNotNull(props, "sasl.mechanism", environment.getSaslMechanism());
    putNotNull(props, "sasl.jaas.config", environment.getSaslJaasConfig());
    putNotNull(props, "ssl.truststore.password", environment.getSslTruststorePassword());
    putNotNull(props, "ssl.truststore.location", ResourceUtil.getResourcePath(environment.getSslTruststoreLocation()));

    return props;
  }

  private static void putNotNull(Map<String, Object> props, String key, Object value) {
    if (value != null) {
      props.put(key, value);
    }
  }

  @Override
  public void consumeLastRecords(Environment environment, String topic, LocalDateTime dateTimeToRewind,
      AtomicBoolean abort, ConsumerFeedback feedback) throws KajException {

    try (KafkaConsumer<K, V> consumer = new KafkaConsumer<>(createConsumerProperties(environment))) {

      RecordConsumer<K, V> recordConsumer = new RecordConsumer<>(consumer, keyType, valueType,
          dateTimeToRewind, abort, feedback);
      consumer.subscribe(Collections.singletonList(topic), recordConsumer);

      recordConsumer.startConsumption();

    } catch (Exception ex) {
      throw new KajException("Error al suscribir el consumidor", ex);
    }
  }


}
