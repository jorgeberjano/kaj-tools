package es.jbp.kajtools.tabla;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecordItem {
  private int partition;
  private long offset;
  private LocalDateTime dateTime;
  private String key;
  private String event;
}
