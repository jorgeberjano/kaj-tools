package es.jbp.kajtools.kafka;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

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
