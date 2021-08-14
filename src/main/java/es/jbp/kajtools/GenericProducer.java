package es.jbp.kajtools;

import es.jbp.kajtools.util.JsonGenericRecordReader;
import es.jbp.kajtools.util.SchemaRegistryService;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class GenericProducer implements IProducer {

  private final SchemaRegistryService schemaRegistryService;
  private final List<IProducer> producerList;

  public GenericProducer(
      @Autowired SchemaRegistryService schemaRegistryService,
      @Autowired List<IProducer> producerList) {
    this.schemaRegistryService = schemaRegistryService;
    this.producerList = producerList;
  }

  @Override
  public List<String> getAvailableEvents() {
    return Stream.concat(Stream.of(""),
        producerList.stream().map(IProducer::getAvailableEvents).flatMap(Collection::stream))
        .collect(Collectors.toList());
  }

  @Override
  public List<String> getAvailableKeys() {
    return Stream.concat(Stream.of(""),
        producerList.stream().map(IProducer::getAvailableKeys).flatMap(Collection::stream))
        .collect(Collectors.toList());
  }

  @Override
  public List<String> getAvailableTopics() {
    return producerList.stream().map(IProducer::getDefaultTopic).collect(Collectors.toList());
  }

  @Override
  public String getDefaultTopic() {
    return "";
  }

  @Override
  public String getEventSchema(String json) throws KajException {
    return "";
  }

  @Override
  public String getKeySchema(String json) throws KajException {
    return "";
  }

  @Override
  public void sendFromJson(Environment environment, String topic, String keyJson, String eventJson)
      throws KajException {

    String keySchema, eventSchema;

    try {
      keySchema = schemaRegistryService.getTopicKeySchema(topic, environment);
    } catch (Exception ex) {
      throw new KajException("Error al obtener el esquema de la Key. Causa: " + ex.getMessage());
    }
    try {
      eventSchema = schemaRegistryService.getTopicEventSchema(topic, environment);
    } catch (Exception ex) {
      throw new KajException("Error al obtener el esquema del Event. Causa: " + ex.getMessage());
    }

    GenericRecord key, event;
    try {
      key = composeRecord(keySchema, keyJson);
    } catch (Exception ex) {
      throw new KajException("Error al crear el GenericRecord de la Key. Causa: " + ex.getMessage());
    }
    try {
      event = composeRecord(eventSchema, eventJson);
    } catch (Exception ex) {
      throw new KajException("Error al crear el GenericRecord del Event. Causa: " + ex.getMessage());
    }

    KafkaTemplate<GenericRecord, GenericRecord> senderTemplate;
    try {
      senderTemplate = createTemplate(environment);
    } catch (Exception ex) {
      throw new KajException("Error al crear el Template de Kafka. Causa: " + ex.getMessage());
    }
    try {
      senderTemplate.send(topic, key, event);
    } catch (Exception ex) {
      throw new KajException("Error al enviar el evento al topic. Causa: " + ex.getMessage());
    }
  }

  @Override
  public String getKeyClassName() {
    return "";
  }

  @Override
  public String getEventClassName() {
    return "";
  }

  private KafkaTemplate<GenericRecord, GenericRecord> createTemplate(Environment environment) {
    return new KafkaTemplate<>(
        new DefaultKafkaProducerFactory<>(createProducerProperties(environment)));
  }

  private GenericRecord composeRecord(String schemaSource, String json) throws Exception {
    Schema.Parser schemaParser = new Schema.Parser();
    Schema schema = schemaParser.parse(schemaSource);
    JsonGenericRecordReader reader = new JsonGenericRecordReader();
    return reader.read(json.getBytes(), schema);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  @Override
  public String getFolder() {
    return ".";
  }

  @Override
  public String getDomain() {
    return "";
  }
}
