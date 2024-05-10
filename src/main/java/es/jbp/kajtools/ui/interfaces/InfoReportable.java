package es.jbp.kajtools.ui.interfaces;

import es.jbp.kajtools.ui.InfoDocument;
import es.jbp.kajtools.ui.InfoMessage;
import es.jbp.kajtools.ui.InfoMessage.Type;
import es.jbp.kajtools.ui.InfoTextPane;

import javax.swing.SwingUtilities;

public interface InfoReportable {

    InfoTextPane getInfoTextPane();

    void printMessage(InfoMessage infoMessage);

    void printLink(InfoDocument infoDocument);

    default void update() {
    }

    default void enqueueLink(InfoDocument infoDocument) {
        SwingUtilities.invokeLater(() -> printLink(infoDocument));
    }

    default void enqueueMessage(InfoMessage infoMessage) {
        SwingUtilities.invokeLater(() -> printMessage(infoMessage));
    }

    default void enqueueException(Exception ex) {
        printMessage(InfoReportable.buildErrorMessage(ex.getMessage()));
        if (ex.getCause() != null) {
            enqueueLink(buildExceptionDocument(ex));
        }
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

    static InfoMessage buildTraceMessage(String message) {
        return buildTypedMessage(message + "\n", Type.TRACE);
    }

    static InfoMessage buildTypedMessage(String message, Type type) {
        return InfoMessage.builder().mensaje(message).type(type).build();
    }

    default InfoDocument buildExceptionDocument(Throwable ex) {
        return InfoDocument.simpleDocument("exception", InfoDocument.Type.INFO, extractCause(ex));
    }

    static String extractCause(Throwable ex) {
        StringBuilder result = new StringBuilder();
        Throwable cause = ex.getCause();
        for (int i = 0; cause != null && i < 10; i++) {
            result.append(cause.getClass())
                    .append(": ")
                    .append(cause.getMessage())
                    .append("\n");
            cause = cause.getCause();
        }
        return result.toString();
    }
}
