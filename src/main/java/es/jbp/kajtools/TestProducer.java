package es.jbp.kajtools;

import es.jbp.kajtools.util.ResourceUtil;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.kafka.clients.producer.ProducerConfig;

public interface TestProducer {

  List<String> getAvailableEvents();

  List<String> getAvailableKeys();

  String getDefaultTopic();

  List<String> getAvailableTopics();

  String getEventSchema(String json) throws Exception;

  String getKeySchema(String json) throws Exception;

  void sendFromJson(Environment environment, String topic, String keyJson, String eventJson)
      throws Exception;

  String getKeyClassName();

  String getEventClassName();

  String getFolder();

  String getDomain();

  default Map<String, Object> createProperties(Environment environment) {
    Map<String, Object> props = new HashMap<>();

    putNotNull(props, ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getBootstrapServers());
    putNotNull(props, ProducerConfig.ACKS_CONFIG, "all");
    putNotNull(props, ProducerConfig.RETRIES_CONFIG, 0);
    putNotNull(props, ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    putNotNull(props, ProducerConfig.LINGER_MS_CONFIG, 0);
    putNotNull(props, ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
    putNotNull(props, KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
        environment.getUrlSchemaRegistry());
    putNotNull(props, KafkaAvroDeserializerConfig.AUTO_REGISTER_SCHEMAS,
        environment.isAutoRegisterSchemas());

    putNotNull(props, ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
    putNotNull(props, ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());

    if (!Objects.isNull(environment.getUserSchemaRegistry())) {
      putNotNull(props, KafkaAvroDeserializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
      putNotNull(props, KafkaAvroDeserializerConfig.USER_INFO_CONFIG,
          environment.getUserSchemaRegistry() + ":" + environment.getPasswordSchemaRegistry());
    }
    putNotNull(props, "security.protocol", environment.getSecurityProtocol());
    putNotNull(props, "sasl.mechanism", environment.getSaslMechanism());
    putNotNull(props, "sasl.jaas.config", environment.getSaslJaasConfig());
    putNotNull(props, "ssl.truststore.password", environment.getSslTruststorePassword());
    putNotNull(props, "ssl.truststore.location", ResourceUtil.getResourcePath(environment.getSslTruststoreLocation()));

    return props;
  }
  
  default void putNotNull(Map<String, Object> props, String key, Object value) {
    if (value != null) {
      props.put(key, value);
    }    
  }
}
