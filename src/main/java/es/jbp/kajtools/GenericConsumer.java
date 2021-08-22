package es.jbp.kajtools;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.avro.generic.GenericRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GenericConsumer implements IConsumer<GenericRecord, GenericRecord> {

  private final List<IConsumer<? extends GenericRecord, ? extends GenericRecord>> consumerList;

  public GenericConsumer(
      @Autowired List<IConsumer<? extends GenericRecord, ? extends GenericRecord>> consumerListrList) {
    this.consumerList = consumerListrList;
  }

  @Override
  public List<String> getAvailableTopics() {
    return consumerList.stream().map(IConsumer::getDefaultTopic).collect(Collectors.toList());
  }

  @Override
  public String getDefaultTopic() {
    return "";
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
