package es.jbp.kajtools.ui;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class InfoDocument {

  public static enum Type {
    INFO,
    JSON,
    PROPERTIES,
    DIFF
  }

  private String title;

  private Type type;

  @Singular("left")
  private List<InfoMessage> leftMessages;

  @Singular("right")
  private List<InfoMessage> rightMessages;

  public String plainText() {
    return leftMessages.stream()
        .map(InfoMessage::getMensaje)
        .collect(Collectors.joining());
  }

  public static InfoDocument simpleDocument(String title, Type type, String text) {
    return InfoDocument.builder()
        .type(type)
        .title(title)
        .left(InfoMessage.builder().mensaje(text).build()
    ).build();
  }

}
