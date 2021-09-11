package es.jbp.kajtools.kafka;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HeaderItem {
  private String key;
  private String value;
}
