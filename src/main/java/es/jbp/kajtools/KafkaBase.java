package es.jbp.kajtools;

import es.jbp.kajtools.util.ResourceUtil;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

public interface KafkaBase {

  default Map<String, Object> createProducerProperties(Environment environment) {
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

  default Map<String, Object> createConsumerProperties(Environment environment) {
    Map<String, Object> props = createCommonProperties(environment);

    props.put(ConsumerConfig.GROUP_ID_CONFIG, "kaj-tools");

    putNotNull(props, ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
    putNotNull(props, ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());

    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    return props;
  }

  default Map<String, Object> createCommonProperties(Environment environment) {
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

  default void putNotNull(Map<String, Object> props, String key, Object value) {
    if (value != null) {
      props.put(key, value);
    }
  }
}
