package es.jbp.kajtools.ui;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

@Data
@Builder
public class InfoDocument {

  @Singular
  private List<InfoMessage> messages;

  public String plainText() {
    return messages.stream()
        .map(InfoMessage::getMensaje)
        .collect(Collectors.joining());
  }

  public static InfoDocument of(String text) {
    return InfoDocument.builder().message(
        InfoMessage.builder().mensaje(text).build()
    ).build();
  }
}
