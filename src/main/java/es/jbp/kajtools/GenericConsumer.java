package es.jbp.kajtools;

import es.jbp.kajtools.util.SchemaRegistryService;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GenericConsumer implements IConsumer<GenericRecord, GenericRecord> {

  private final SchemaRegistryService schemaRegistryService;
  private final List<IConsumer> consumerListrList;

  public GenericConsumer(
      @Autowired SchemaRegistryService schemaRegistryService,
      @Autowired List<IConsumer> consumerListrList) {
    this.schemaRegistryService = schemaRegistryService;
    this.consumerListrList = consumerListrList;
  }

  @Override
  public List<String> getAvailableTopics() {
    return consumerListrList.stream().map(IConsumer::getDefaultTopic).collect(Collectors.toList());
  }

  @Override
  public String getDefaultTopic() {
    return "";
  }

  private KafkaConsumer<GenericRecord, GenericRecord> createConsumer(Environment environment) {
    return new KafkaConsumer<>(createConsumerProperties(environment));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  @Override
  public String getDomain() {
    return "";
  }
}
