package es.jbp.kajtools.ui;

import static org.fife.ui.rsyntaxtextarea.TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.SEPARATOR;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.VARIABLE;

import es.jbp.kajtools.KajToolsApp;
import java.awt.Color;
import java.awt.Font;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.springframework.stereotype.Component;

@Component
public class ComponentFactory {

  private Theme theme;

  public ComponentFactory(Theme theme) {
    this.theme = theme;
  }

  public RSyntaxTextArea createSyntaxEditor() {
    final RSyntaxTextArea jsonEditor = new RSyntaxTextArea();
    jsonEditor.setCodeFoldingEnabled(true);
    jsonEditor.setAlignmentX(0.0F);
    Font font = new Font("Courier New", Font.PLAIN, 12);
    jsonEditor.setFont(font);
    if (theme != null) {
      theme.apply(jsonEditor);
    } else {
      SyntaxScheme scheme = jsonEditor.getSyntaxScheme();
      scheme.getStyle(SEPARATOR).foreground = Color.black;
      scheme.getStyle(VARIABLE).foreground = Color.blue;
      scheme.getStyle(LITERAL_STRING_DOUBLE_QUOTE).foreground = Color.green.darker();
    }

    return jsonEditor;
  }

  public RTextScrollPane createEditorScroll(RSyntaxTextArea syntaxTextArea) {
    RTextScrollPane scrollPane = new RTextScrollPane(syntaxTextArea);
    scrollPane.setFoldIndicatorEnabled(true);
    scrollPane.setIconRowHeaderEnabled(true);
    scrollPane.setLineNumbersEnabled(true);
    scrollPane.setAlignmentX(0.0F);
    scrollPane.setAlignmentY(0.0F);
    return scrollPane;
  }
}
