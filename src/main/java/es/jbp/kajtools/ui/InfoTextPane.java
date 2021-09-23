package es.jbp.kajtools.ui;

import es.jbp.kajtools.ui.InfoMessage.Type;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import javax.swing.JEditorPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

public class InfoTextPane extends JTextPane {

  private static Font font;

  static {
    try {
      InputStream is = BasePanel.class.getResourceAsStream("/fonts/JetBrainsMono-SemiBold.ttf");
      if (is != null) {
        font = Font.createFont(Font.TRUETYPE_FONT, is);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(font);
      }
    } catch (Exception ex) {
      System.err.println(ex);
    }
  }

  public InfoTextPane() {
    setBackground(InfoMessage.DARK);
    setCaretColor(new Color(-1));

    setUI (new LineHighlightTextPaneUI(this));
  }

  public void enableLinks() {
    setContentType("text/html");
    setHighlighter(null);
  }

  public void enqueueInfoMessage(InfoMessage infoMessage) {
    SwingUtilities.invokeLater(() -> {
      printInfoMessage(infoMessage);
    });
  }

  public void printInfoMessage(InfoMessage infoMessage) {

    SimpleAttributeSet attr = new SimpleAttributeSet();
    final Type type = infoMessage.getType();

    StyleConstants.setAlignment(attr, StyleConstants.ALIGN_LEFT);
    StyleConstants.setForeground(attr, type == null ? Color.white : type.color);
    StyleConstants.setBackground(attr, type == null ? Color.black : type.backgroundColor);

    StyleConstants.setFontFamily(attr, font.getFamily());
    StyleConstants.setFontSize(attr, 14);
    StyleConstants.setBold(attr, false);

    printString(infoMessage.getMensaje(), attr);
  }


  public void printString(String text, AttributeSet attr) {
    StyledDocument doc = getStyledDocument();
    try {
      doc.insertString(doc.getLength(), text, attr);
    } catch (BadLocationException ex) {
      System.err.println("No se pudo insertar el texto en la consola de información");
    }
  }
}
