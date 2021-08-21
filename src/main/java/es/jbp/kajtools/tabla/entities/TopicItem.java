package es.jbp.kajtools.tabla.entities;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicItem {
  private String name;
  private int partitions;

  @Override
  public String toString() {
    return name;
  }
}
