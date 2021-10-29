package es.jbp.kajtools.kafka;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.apache.kafka.common.header.Headers;

@Data
@Builder
public class RecordItem {

  private int partition;
  private long offset;
  private LocalDateTime dateTime;
  private String key;
  private String value;
  private List<HeaderItem> headers;
  private List<String> errors;
  private boolean matchFilter;
}
