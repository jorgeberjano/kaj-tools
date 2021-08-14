package es.jbp.kajtools.ui;

import static org.fife.ui.rsyntaxtextarea.TokenTypes.LITERAL_STRING_DOUBLE_QUOTE;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.SEPARATOR;
import static org.fife.ui.rsyntaxtextarea.TokenTypes.VARIABLE;

import es.jbp.kajtools.ui.InfoMessage.Type;
import es.jbp.kajtools.KajToolsApp;
import es.jbp.kajtools.util.JsonUtils;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingWorker;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

public abstract class BasePanel {

  private final List<InfoMessage> messages = new ArrayList<>();
  private JTextField field;

  protected abstract JTextPane getInfoTextPane();

  protected void enqueueAction(String message) {
    enqueueText(message + "\n", Type.ACTION);
  }

  protected void enqueueInfo(String message) {
    enqueueText(message + "\n", Type.INFO);
  }

  protected void enqueueError(String message) {
    enqueueText(message + "\n", Type.ERROR);
  }

  protected void enqueueSuccessful(String message) {
    enqueueText(message + "\n", Type.SUCCESS);
  }

  protected synchronized void enqueueText(String message, Type type) {
    messages.add(new InfoMessage(message, type));
  }

  protected void flushMessages() {
    messages.forEach(m -> printText(m.getMensaje(), m.getType()));
    messages.clear();
  }

  protected synchronized void asyncTaskFinished() {
    flushMessages();
    printInfo("");
    enableButtons(true);
  }

  protected abstract void enableButtons(boolean enable);

  protected void printAction(String message) {
    printText(message + "\n", Type.ACTION);
  }

  protected void printInfo(String message) {
    printText(message + "\n", Type.INFO);
  }

  protected void printError(String message) {
    printText(message + "\n", Type.ERROR);
  }

  protected void printSuccessful(String message) {
    printText(message + "\n", Type.SUCCESS);
  }

  protected void printText(String message, Type type) {
    StyledDocument doc = getInfoTextPane().getStyledDocument();
    SimpleAttributeSet attr = new SimpleAttributeSet();
    StyleConstants.setForeground(attr, type == null ? Color.white : type.color);
    StyleConstants.setBackground(attr, type == null ? Color.black : type.backgroundColor);
    try {
      doc.insertString(doc.getLength(), message, attr);
    } catch (BadLocationException ex) {
      System.err.println("No se puede insertar el texto en la consola de información");
    }
  }

  protected void copyToClipboard(String json) {
    StringSelection stringSelection = new StringSelection(json);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, null);
  }

  protected RTextScrollPane createEditorScroll(RSyntaxTextArea editor) {
    RTextScrollPane jsonScrollPane = new RTextScrollPane(editor);
    jsonScrollPane.setFoldIndicatorEnabled(true);
    jsonScrollPane.setIconRowHeaderEnabled(true);
    jsonScrollPane.setLineNumbersEnabled(true);
    jsonScrollPane.setAlignmentX(0.0F);
    jsonScrollPane.setAlignmentY(0.0F);
    return jsonScrollPane;
  }

  protected RSyntaxTextArea createJsonEditor() {
    final RSyntaxTextArea jsonEditor = new RSyntaxTextArea();
    jsonEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
    jsonEditor.setCodeFoldingEnabled(true);
    jsonEditor.setAlignmentX(0.0F);
    Font font = new Font("Courier New", Font.PLAIN, 12);
    jsonEditor.setFont(font);
    Theme theme = KajToolsApp.getInstance().getTheme();
    if (theme != null) {
      theme.apply(jsonEditor);
    } else {
      SyntaxScheme scheme = jsonEditor.getSyntaxScheme();
      scheme.getStyle(SEPARATOR).foreground = Color.black;
      scheme.getStyle(VARIABLE).foreground = Color.blue;
      scheme.getStyle(LITERAL_STRING_DOUBLE_QUOTE).foreground = Color.green.darker();
    }
    jsonEditor.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_L && e.isControlDown() && e.isAltDown()) {
          String text = jsonEditor.getText();
          if (!JsonUtils.isTemplate(text)) {
            int position = jsonEditor.getCaretPosition();
            jsonEditor.setText(JsonUtils.formatJson(text));
            jsonEditor.setCaretPosition(position);
          }
        }
        super.keyPressed(e);
      }
    });
    return jsonEditor;
  }

  protected interface AsyncTask<T> {

    T execute();
  }

  interface AsyncListener {

    void done();
  }

  protected void executeAsyncTask(AsyncTask<Void> task) {
    this.<Void>executeAsyncTask(task, null);
  }

  protected <T> Future<T> executeAsyncTask(AsyncTask<T> task, AsyncListener listener) {
    enableButtons(false);
    SwingWorker<T, Void> worker = new SwingWorker<T, Void>() {
      @Override
      protected T doInBackground() {
        return task.execute();
      }

      @Override
      protected void done() {
        asyncTaskFinished();
        if (listener != null) {
          listener.done();
        }
      }
    };
    worker.execute();
    return worker;
  }

  protected void enableTextSearch(JTextField searchTextField, JTextComponent... editors) {
    this.field = searchTextField;
    searchTextField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        findText(true, true);
        getCurrentEditor().ifPresent(JTextComponent::grabFocus);
      }
    });
    searchTextField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        findText(true, true);
      }
    });
    Arrays.stream(editors).forEach(editor -> editor.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        super.keyPressed(e);
        if (e.getKeyCode() == KeyEvent.VK_F3) {
          if (e.isControlDown()) {
            searchTextField.setText(editor.getSelectedText());
          }
          findText(!e.isShiftDown(), false);
        }
      }
    }));
  }

  protected void findText(boolean forward, boolean retryOppositeDirection) {

    String text = field.getText();
    boolean wasFound = findText(forward, text);
    if (!wasFound && retryOppositeDirection) {
      findText(!forward, text);
    }
  }

  private boolean findText(boolean forward, String text) {
    Optional<JTextComponent> editor = getCurrentEditor();
    if (!editor.isPresent() || !(editor.get() instanceof RSyntaxTextArea)) {
      return false;
    }
    SearchContext context = new SearchContext();
    context.setSearchFor(text);
    context.setMatchCase(false);
    context.setRegularExpression(false);
    context.setSearchForward(forward);
    context.setWholeWord(false);
    SearchResult result = SearchEngine.find((RSyntaxTextArea) editor.get(), context);
    return result.wasFound();
  }

  protected void cleanEditor() {
    getCurrentEditor().ifPresent(editor -> editor.setText(""));
  }

  protected abstract Optional<JTextComponent> getCurrentEditor();

  protected void copyToClipboard() {
    getCurrentEditor().ifPresent(editor -> copyToClipboard(editor.getText()));
  }

  protected void enqueueTextDifferences(String textLeft, String textRight) {
    diff_match_patch difference = new diff_match_patch();
    LinkedList<Diff> deltas = difference.diff_main(textLeft, textRight);
    List<Pair<String, Type>> result = new ArrayList<>();

    for (Diff delta : deltas) {
      switch (delta.operation) {
        case EQUAL:
          result.add(new ImmutablePair<>(delta.text, Type.INFO));
          break;
        case INSERT:
          result.add(new ImmutablePair<>(delta.text, Type.ADDED));
          break;
        case DELETE:
          result.add(new ImmutablePair<>(delta.text, Type.DELETED));
          break;
      }
    }

    result.forEach(p -> enqueueText(p.getLeft(), p.getRight()));
    enqueueInfo("");
  }
}
