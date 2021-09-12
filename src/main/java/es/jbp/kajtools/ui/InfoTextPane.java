package es.jbp.kajtools.ui;

import es.jbp.kajtools.KajException;
import es.jbp.kajtools.ui.InfoMessage.Type;
import java.awt.Color;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class InfoTextPane extends JTextPane {

  public InfoTextPane() {
    setBackground(new Color(-16777216));
    setCaretColor(new Color(-1));
  }

  public void enqueueTypedMessage(String message, Type type) {
    enqueueInfoMessage(InfoMessage.builder().mensaje(message).type(type).build());
  }

  public void enqueueInfoMessage(InfoMessage infoMessage) {
    SwingUtilities.invokeLater(() -> {
      printInfoMessage(infoMessage);
    });
  }

  public void printTypedMessage(String message, Type type) {
    printInfoMessage(InfoMessage.builder().mensaje(message).type(type).build());
  }

  public void printInfoMessage(InfoMessage infoMessage) {
    StyledDocument doc = getStyledDocument();
    SimpleAttributeSet attr = new SimpleAttributeSet();
    final Type type = infoMessage.getType();
    StyleConstants.setForeground(attr, type == null ? Color.white : type.color);
    StyleConstants.setBackground(attr, type == null ? Color.black : type.backgroundColor);
    try {
      doc.insertString(doc.getLength(), infoMessage.getMensaje(), attr);
    } catch (BadLocationException ex) {
      System.err.println("No se pudo insertar el texto en la consola de información");
    }
  }

}
