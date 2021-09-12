package es.jbp.kajtools.ui;

import java.awt.Color;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class InfoMessage {
  public enum Type {
    ACTION(Color.YELLOW, Color.BLACK),
    TRACE(Color.WHITE, Color.BLACK),
    SUCCESS(new Color(0x99FF99), Color.BLACK),
    ERROR(Color.RED.brighter(), Color.BLACK),
    DELETED(Color.BLACK, Color.RED),
    ADDED(Color.BLACK, Color.GREEN);

    public final Color color;
    public final Color backgroundColor;
    Type(Color foreground, Color backgroundColor) {
      this.color = foreground; this.backgroundColor = backgroundColor;
    }
  }
  private String mensaje;
  private Type type;
}
