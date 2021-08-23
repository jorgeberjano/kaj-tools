package es.jbp.kajtools;

import es.jbp.kajtools.filter.MessageFilter;
import es.jbp.kajtools.tabla.entities.RecordItem;
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

public interface IMessageClient {

  List<String> getAvailableValues();

  List<String> getAvailableKeys();

  String getDefaultTopic();

  List<String> getAvailableTopics();

  String getValueSchema(String json) throws KajException;

  String getKeySchema(String json) throws KajException;

  void sendFromJson(Environment environment, String topic, String keyJson, String valueJson)
      throws KajException;

  List<RecordItem> consumeLastRecords(Environment environment, String topic,
      MessageFilter filter,  long maxRecordsPerPartition) throws KajException;

  String getKeyClassName();

  String getValueClassName();

  String getFolder();

  String getDomain();

}
