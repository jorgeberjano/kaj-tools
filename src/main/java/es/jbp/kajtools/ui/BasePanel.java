package es.jbp.kajtools.ui;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.ui.InfoMessage.Type;
import es.jbp.kajtools.ui.interfaces.DialogueablePanel;
import es.jbp.kajtools.ui.interfaces.InfoReportablePanel;
import es.jbp.kajtools.ui.interfaces.SearchablePanel;
import es.jbp.kajtools.util.JsonUtils;
import es.jbp.kajtools.util.TemplateExecutor;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public abstract class BasePanel implements InfoReportablePanel, SearchablePanel {

  protected TemplateExecutor templateExecutor = new TemplateExecutor();
  protected TextComparator comparator = new TextComparator();

  protected AtomicBoolean abortTasks = new AtomicBoolean();

  private final Map<String, InfoDocument> linksMap = new HashMap<>();

  protected abstract Component getContentPane();

  public void initialize() {
    JTextPane textPane = getInfoTextPane();
    textPane.addHyperlinkListener(e -> {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        showLinkContent(e.getDescription());
      }
    });
    textPane.setContentType("text/html");
    textPane.setHighlighter(null);
  }

  protected synchronized void asyncTaskFinished() {
    printTrace("");
    enableButtons(true);
  }

  protected abstract void enableButtons(boolean enable);

  protected void enqueueException(KajException ex) {
    enqueueError(ex.getMessage());
    if (ex.getCause() != null) {
      enqueueLink("excepcion", ex.getCause().getMessage());

      if (ex.getCause().getCause() != null) {
        enqueueLink("{cause}", ex.getCause().getCause().getMessage());
      }
    }
  }

  protected void enqueueLink(String text, String documentText) {
    enqueueLink(text, InfoDocument.builder().message(
        InfoMessage.builder().mensaje(documentText).build()
    ).build());
  }

  protected void enqueueLink(String text, InfoDocument infoDocument) {
    SwingUtilities.invokeLater(() -> {
      printLink(text, infoDocument);
    });
  }

  protected void printTextDifferences(String rightTitle, String rightText, String leftTitle, String leftText) {

    InfoDocument differencesDocument = comparator.compare(
        leftTitle, JsonUtils.formatJson(leftText),
        rightTitle, JsonUtils.formatJson(rightText));
    printLink("differences", differencesDocument);
  }

  protected void printLink(String text, InfoDocument infoDocument) {
    String key = text + "_" + UUID.randomUUID();

    linksMap.put(key, infoDocument);

    HTMLDocument doc = (HTMLDocument) getInfoTextPane().getStyledDocument();
    try {
      doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), "<a href=\"" + key + "\">" + text + "</a><br>");
    } catch (BadLocationException | IOException ex) {
      System.err.println("No se pudo insertar el link en la consola de información");
    }
  }

  protected void printException(KajException ex) {
    printError(ex.getMessage());
    if (ex.getCause() != null) {
      printTrace(ex.getCause().getMessage());
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
    final RSyntaxTextArea jsonEditor = ComponentFactory.createSyntaxEditor();
    jsonEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
    jsonEditor.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_L && e.isControlDown() && e.isAltDown()) {
          formatJsonInEditor(jsonEditor);
        }
        super.keyPressed(e);
      }
    });
    return jsonEditor;
  }

  protected void formatJsonInEditor(RSyntaxTextArea jsonEditor) {
    String text = jsonEditor.getText();
    int position = jsonEditor.getCaretPosition();

    try {
      text = templateExecutor.formatJson(text);
    } catch (ExpressionException e) {
      System.err.println("No de pudo formatear el JSON");
      return;
    }

    jsonEditor.setText(text);
    jsonEditor.setCaretPosition(position);
  }

  protected RSyntaxTextArea createScriptEditor() {
    RSyntaxTextArea editor = ComponentFactory.createSyntaxEditor();
    editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
    return editor;
  }

  protected RSyntaxTextArea createPropertiesEditor() {
    RSyntaxTextArea editor = ComponentFactory.createSyntaxEditor();
    editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE);
    return editor;
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

  protected void stopAsyncTasks() {
    abortTasks.set(true);
  }

  protected <T> Future<T> executeAsyncTask(AsyncTask<T> task, AsyncListener listener) {
    abortTasks.set(false);
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


  protected void cleanEditor() {
    getCurrentEditor().ifPresent(editor -> {
      editor.setText("");
      if (editor == getInfoTextPane()) {
        linksMap.clear();
      }
    });
  }

  protected void copyToClipboard() {
    getCurrentEditor().ifPresent(editor -> copyToClipboard(editor.getText()));
  }

  protected void showLinkContent(String key) {
    InfoDocument infoDocument = linksMap.get(key);
    if (key.contains("json") || key.contains("schema")) {
      RSyntaxPanel panel = new RSyntaxPanel();
      panel.setContent(JsonUtils.formatJson(infoDocument.plainText()), SyntaxConstants.SYNTAX_STYLE_JSON);
      showInModalDialog(panel);
    } else {
      InfoPanel panel = new InfoPanel();
      panel.setDocument(infoDocument);
      showInModalDialog(panel);
    }
  }

  protected void showInModalDialog(DialogueablePanel dialogable) {
    JPanel panel = dialogable.getMainPanel();
    panel.setBounds(0, 0, 400, 450);
    JDialog dialog = new JDialog();
    dialog.setTitle("Topics");
    dialog.setSize(800, 450);
    dialog.setResizable(true);
    dialog.setLocationRelativeTo(getContentPane());
    dialog.setContentPane(panel);
    dialogable.bindDialog(dialog);
    dialog.setModal(true);
    dialog.setVisible(true);
  }

}



