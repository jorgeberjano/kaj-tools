package es.jbp.kajtools.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLDocument;
import org.apache.commons.lang3.tuple.Pair;

public class InfoTextPane extends JTextPane {

  private static Font monospaceFont;
  private static final int FONT_SIZE = 14;

  static {
    try {
      InputStream is = BasePanel.class.getResourceAsStream("/fonts/JetBrainsMono-SemiBold.ttf");
      if (is != null) {
        monospaceFont = Font.createFont(Font.TRUETYPE_FONT, is);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(monospaceFont);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public InfoTextPane() {
    setBackground(InfoMessage.DARK);
    setCaretColor(new Color(-1));
  }

  public void highlightLines(Collection<Pair<Integer, Integer>> positions) {
    setUI(new LineHighlightTextPaneUI(this, positions));
  }

  public void enableLinks() {
    setContentType("text/html");
    setHighlighter(null);
  }

  public void printInfoMessage(InfoMessage infoMessage) {

    var attr = new SimpleAttributeSet();
    var type = infoMessage.getType();

    StyleConstants.setAlignment(attr, StyleConstants.ALIGN_LEFT);
    StyleConstants.setForeground(attr, type == null ? Color.white : type.color);
    StyleConstants.setBackground(attr, type == null ? Color.black : type.backgroundColor);

    StyleConstants.setFontFamily(attr, monospaceFont.getFamily());
    StyleConstants.setFontSize(attr, FONT_SIZE);
    StyleConstants.setBold(attr, false);

    printString(infoMessage.getMensaje(), attr);
  }

  public void printString(String text, AttributeSet attr) {
    var doc = getStyledDocument();
    try {
      doc.insertString(doc.getLength(), text, attr);
      setCaretPosition(doc.getLength());
    } catch (BadLocationException ex) {
      ex.printStackTrace();
    }
  }

  public void printLink(String key, InfoDocument infoDocument) {
    // TODO: comprobar el tipo de documento
    HTMLDocument doc = (HTMLDocument) getStyledDocument();
    try {
      doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), "<a href=\"" + key + "\">" + infoDocument.getTitle() +
          "</a><br>");
    } catch (BadLocationException | IOException ex) {
      System.err.println("No se pudo insertar el link en la consola de información");
    }
  }
}
