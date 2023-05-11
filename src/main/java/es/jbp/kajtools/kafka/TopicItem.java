package es.jbp.kajtools.kafka;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicItem {

  private String name;
  private int partitions;
//  @Singular
//  private Map<String, String> configs;

  @Override
  public String toString() {
    return name;
  }
}
