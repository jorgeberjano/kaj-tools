package es.jbp.kajtools.ui;

import java.awt.Color;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class InfoMessage {

  public static final Color DARK = new Color(0x2B2B2B);

  public enum Type {
    ACTION(new Color(0xFEC56B), DARK),
    TRACE(new Color(0xA5B3C2), DARK),
    SUCCESS(new Color(0x598C2A), DARK),
    ERROR(new Color(0xF96966), DARK),
    DELETED(Color.BLACK, new Color(0xF96966)),
    ADDED(Color.BLACK, new Color(0x598C2A));

    public final Color color;
    public final Color backgroundColor;
    Type(Color foreground, Color backgroundColor) {
      this.color = foreground; this.backgroundColor = backgroundColor;
    }
  }
  private String mensaje;
  private Type type;
}
