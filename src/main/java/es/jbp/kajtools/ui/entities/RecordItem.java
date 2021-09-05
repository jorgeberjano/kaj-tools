package es.jbp.kajtools.ui.entities;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecordItem {

  private int partition;
  private long offset;
  private LocalDateTime dateTime;
  private String key;
  private String value;
  private String keyError;
  private String valueError;
}
