package es.jbp.kajtools;

import es.jbp.kajtools.util.ResourceUtil;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.kafka.clients.producer.ProducerConfig;

public interface KafkaBase {
  default Map<String, Object> createProperties(Environment environment) {
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
