package es.jbp.kajtools.ui.interfaces;

import es.jbp.kajtools.ui.InfoDocument;
import es.jbp.kajtools.ui.InfoMessage;
import es.jbp.kajtools.ui.InfoMessage.Type;
import es.jbp.kajtools.ui.InfoTextPane;
import javax.swing.SwingUtilities;

public interface InfoReportable {

  InfoTextPane getInfoTextPane();
  //void addLink(String key, InfoDocument value);
  void printMessage(InfoMessage infoMessage);
  void printLink(InfoDocument infoDocument);

  default void enqueueLink(InfoDocument infoDocument) {
    SwingUtilities.invokeLater(() -> {
      printLink(infoDocument);
    });
  }

  default void enqueueMessage(InfoMessage infoMessage) {
    SwingUtilities.invokeLater(() -> {
      printMessage(infoMessage);
    });
  }

  static InfoMessage buildSuccessfulMessage(String message) {
    return buildTypedMessage(message + "\n", Type.SUCCESS);
  }

  static InfoMessage buildErrorMessage(String message) {
    return buildTypedMessage(message + "\n", Type.ERROR);
  }

  static InfoMessage buildActionMessage(String message) {
    return buildTypedMessage(message + "\n", Type.ACTION);
  }

  static InfoMessage  buildTraceMessage(String message) {
    return buildTypedMessage(message + "\n", Type.TRACE);
  }

  static InfoMessage buildTypedMessage(String message, Type type) {
    return InfoMessage.builder().mensaje(message).type(type).build();
  }



}
