package es.jbp.kajtools;

import es.jbp.kajtools.util.ResourceUtil;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.kafka.clients.producer.ProducerConfig;

public interface IProducer extends KafkaBase {

  List<String> getAvailableValues();

  List<String> getAvailableKeys();

  String getDefaultTopic();

  List<String> getAvailableTopics();

  String getValueSchema(String json) throws KajException;

  String getKeySchema(String json) throws KajException;

  void sendFromJson(Environment environment, String topic, String keyJson, String valueJson)
      throws KajException;

  String getKeyClassName();

  String getValueClassName();

  String getFolder();

  String getDomain();

  default Map<String, Object> createProducerProperties(Environment environment) {
    Map<String, Object> props = KafkaBase.super.createProperties(environment);

    putNotNull(props, ProducerConfig.ACKS_CONFIG, "all");
    putNotNull(props, ProducerConfig.RETRIES_CONFIG, 0);
    putNotNull(props, ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    putNotNull(props, ProducerConfig.LINGER_MS_CONFIG, 0);
    putNotNull(props, ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

    putNotNull(props, ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
    putNotNull(props, ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());

    return props;
  }

}
