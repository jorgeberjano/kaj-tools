package es.jbp.kajtools.ui.interfaces;

import es.jbp.kajtools.ui.InfoMessage;
import es.jbp.kajtools.ui.InfoMessage.Type;
import es.jbp.kajtools.ui.InfoTextPane;

public interface InfoReportablePanel {

  InfoTextPane getInfoTextPane();

  default void enqueueAction(String message) {
    enqueueTypedMessage(message + "\n", Type.ACTION);
  }

  default void enqueueInfoMessage(String message) {
    enqueueTypedMessage(message + "\n", Type.TRACE);
  }

  default void enqueueError(String message) {
    enqueueTypedMessage(message + "\n", Type.ERROR);
  }

  default void enqueueSuccessful(String message) {
    enqueueTypedMessage(message + "\n", Type.SUCCESS);
  }

  default void enqueueTypedMessage(String message, Type type) {
    getInfoTextPane().enqueueInfoMessage(InfoMessage.builder().mensaje(message).type(type).build());
  }

  default void printAction(String message) {
    printTypedMessage(message + "\n", Type.ACTION);
  }

  default void printTrace(String message) {
    printTypedMessage(message + "\n", Type.TRACE);
  }

  default void printError(String message) {
    printTypedMessage(message + "\n", Type.ERROR);
  }

  default void printSuccessful(String message) {
    printTypedMessage(message + "\n", Type.SUCCESS);
  }

  default void printTypedMessage(String message, Type type) {
    printMessage(new InfoMessage(message, type));
  }

  default void printMessage(InfoMessage infoMessage) {
    getInfoTextPane().printInfoMessage(infoMessage);
  }
}
