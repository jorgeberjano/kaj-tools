package es.jbp.kajtools.ui;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.i18n.I18nService;
import es.jbp.kajtools.ui.InfoDocument.Type;
import es.jbp.kajtools.ui.interfaces.DialogueablePanel;
import es.jbp.kajtools.ui.interfaces.InfoReportablePanel;
import es.jbp.kajtools.ui.interfaces.SearchablePanel;
import es.jbp.kajtools.util.JsonUtils;
import es.jbp.kajtools.util.TemplateExecutor;
import java.awt.Component;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

public abstract class BasePanel implements InfoReportablePanel, SearchablePanel {

  protected TemplateExecutor templateExecutor = new TemplateExecutor();
  protected TextComparator comparator = new TextComparator();

  protected AtomicBoolean abortTasks = new AtomicBoolean();

  private final Map<String, InfoDocument> linksMap = new HashMap<>();

  protected ComponentFactory componentFactory;
  protected final I18nService i18nService;

  protected BasePanel(ComponentFactory componentFactory, I18nService i18nService) {
    this.componentFactory = componentFactory;
    this.i18nService = i18nService;
  }

  protected abstract Component getContentPane();


  public void initialize() {
    InfoTextPane textPane = getInfoTextPane();
    textPane.enableLinks();
    textPane.addHyperlinkListener(e -> {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        showLinkContent(e.getDescription());
      }
    });
  }

  protected synchronized void asyncTaskFinished() {
    printTrace("");
    enableButtons(true);
  }

  protected abstract void enableButtons(boolean enable);

  protected void enqueueException(Throwable ex) {
    SwingUtilities.invokeLater(() -> {
      printException(ex);
    });
  }

  protected void enqueueLink(InfoDocument infoDocument) {
    SwingUtilities.invokeLater(() -> {
      printLink(infoDocument);
    });
  }

  protected void enqueueTextDifferences(String leftTitle, String leftText, String rightTitle, String rightText) {

    InfoDocument differencesDocument = comparator.compare(
        leftTitle, JsonUtils.formatJson(leftText),
        rightTitle, JsonUtils.formatJson(rightText));
    enqueueLink(differencesDocument);
  }

  protected void printLink(InfoDocument infoDocument) {
    String key = UUID.randomUUID().toString();

    linksMap.put(key, infoDocument);

    HTMLDocument doc = (HTMLDocument) getInfoTextPane().getStyledDocument();
    try {
      doc.insertAfterEnd(doc.getCharacterElement(doc.getLength()), "<a href=\"" + key + "\">" + infoDocument.getTitle() +
          "</a><br>");
    } catch (BadLocationException | IOException ex) {
      System.err.println("No se pudo insertar el link en la consola de información");
    }
  }

  protected void printException(Throwable ex) {
    printError(ex.getMessage());

    if (ex.getCause() != null) {
      printLink(InfoDocument.simpleDocument("exception", Type.INFO, extractCause(ex)));
    }
  }

  private String extractCause(Throwable ex) {
    StringBuilder result = new StringBuilder();
    Throwable cause = ex.getCause();
    for (int i = 0; cause != null && i < 10; i ++) {
      result.append(cause.getClass() + ": " + cause.getMessage() + "\n");
      cause = cause.getCause();
    }
    return result.toString();
  }

  protected void copyToClipboard(String json) {
    StringSelection stringSelection = new StringSelection(json);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, null);
  }

//  protected RTextScrollPane createEditorScroll(RSyntaxTextArea editor) {
//    RTextScrollPane jsonScrollPane = new RTextScrollPane(editor);
//    jsonScrollPane.setFoldIndicatorEnabled(true);
//    jsonScrollPane.setIconRowHeaderEnabled(true);
//    jsonScrollPane.setLineNumbersEnabled(true);
//    jsonScrollPane.setAlignmentX(0.0F);
//    jsonScrollPane.setAlignmentY(0.0F);
//    return jsonScrollPane;
//  }

  protected RSyntaxTextArea createJsonEditor() {
    final RSyntaxTextArea jsonEditor = componentFactory.createSyntaxEditor();
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
      printError("No de pudo formatear el JSON");
      printException(e);
      return;
    }

    jsonEditor.setText(text);
    jsonEditor.setCaretPosition(position);
  }

  protected RSyntaxTextArea createScriptEditor() {
    RSyntaxTextArea editor = componentFactory.createSyntaxEditor();
    editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
    return editor;
  }

  protected RSyntaxTextArea createPropertiesEditor() {
    RSyntaxTextArea editor = componentFactory.createSyntaxEditor();
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
    showInfoDocument(linksMap.get(key), false, null);
  }

  protected void showInfoDocument(InfoDocument infoDocument, boolean modal, Component parent) {
    if (infoDocument == null) {
      return;
    }
    String title = infoDocument.getTitle();
    switch (Optional.ofNullable(infoDocument.getType()).orElse(Type.INFO)) {
      case DIFF:
        var diffPanel = new DiffPanel();
        diffPanel.setDocument(infoDocument);
        showDialog(diffPanel, title, modal, parent);
        break;
      case JSON:
        var jsonPanel = new RSyntaxPanel(componentFactory);
        jsonPanel.setContent(JsonUtils.formatJson(infoDocument.plainText()), SyntaxConstants.SYNTAX_STYLE_JSON);
        showDialog(jsonPanel, title, modal, parent);
        break;
      case PROPERTIES:
        var propertiesPanel = new RSyntaxPanel(componentFactory);
        propertiesPanel.setContent(infoDocument.plainText(), SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE);
        showDialog(propertiesPanel, title, modal, parent);
        break;
      default:
        var infoPanel = new InfoPanel();
        infoPanel.setDocument(infoDocument);
        showDialog(infoPanel, title, modal, parent);
    }
  }

  private void showDialog(DialogueablePanel dialogueable, String title, boolean modal, Component parent) {
    if (modal) {
      showInModalDialog(dialogueable, title, parent);
    } else {
      showInNonModalDialog(dialogueable, title, parent);
    }
  }

  protected void showInModalDialog(DialogueablePanel dialogueable, String title, Component parent) {
    JPanel panel = dialogueable.getMainPanel();
    panel.setBounds(0, 0, 400, 450);
    JDialog dialog = new JDialog();
    dialog.setTitle(title);
    dialog.setSize(800, 450);
    dialog.setResizable(true);
    dialog.setLocationRelativeTo(Optional.ofNullable(parent).orElse(getContentPane()));
    dialog.setContentPane(panel);
    dialogueable.bindDialog(dialog);
    dialog.setModal(true);
    dialog.setVisible(true);
  }

  protected void showInNonModalDialog(DialogueablePanel dialogueable, String title, Component parent) {
    JPanel panel = dialogueable.getMainPanel();
    panel.setBounds(0, 0, 400, 450);
    JFrame dialog = new JFrame();
    dialog.setTitle(title);
    dialog.setSize(800, 450);
    dialog.setResizable(true);
    dialog.setLocationRelativeTo(Optional.ofNullable(parent).orElse(getContentPane()));
    dialog.setContentPane(panel);
    dialogueable.bindDialog(dialog);
    dialog.setVisible(true);
  }

}



