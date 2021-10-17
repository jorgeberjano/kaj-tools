package es.jbp.kajtools.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import es.jbp.kajtools.Environment;
import es.jbp.kajtools.IMessageClient;
import es.jbp.kajtools.configuration.Configuration;
import es.jbp.kajtools.i18n.I18nService;
import es.jbp.kajtools.kafka.GenericClient;
import es.jbp.kajtools.kafka.KafkaAdminService;
import es.jbp.kajtools.schemaregistry.ISchemaRegistryService;
import es.jbp.kajtools.script.ExecutionContext;
import es.jbp.kajtools.script.ScriptCompiler;
import es.jbp.kajtools.script.ScriptSimbolFactory;
import es.jbp.kajtools.script.exception.ScriptCompilerException;
import es.jbp.kajtools.script.exception.ScriptExecutionException;
import es.jbp.kajtools.script.nodes.ScriptNode;
import es.jbp.kajtools.util.TemplateExecutor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.swing.AbstractButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleContext;
import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class ScriptPanel extends KafkaBasePanel {

  private final ScriptSimbolFactory scriptSymbolFactory = new ScriptSimbolFactory();
  private final TemplateExecutor scriptTemplateExecutor = new TemplateExecutor(scriptSymbolFactory);

  private JComboBox comboEnvironment;
  @Getter
  private JPanel contentPane;
  private JButton buttonExecute;
  private JTabbedPane tabbedPane;
  private JPanel tabInfo;
  private JPanel tabScript;
  @Getter
  private InfoTextPane infoTextPane;
  private JButton cleanButton;
  private JButton copyButton;
  @Getter
  private JTextField searchTextField;
  private JLabel dangerLabel;
  private JButton buttonCheckEnvironment;
  private RTextScrollPane scriptScrollPane;
  private JButton buttonStop;
  private RSyntaxTextArea scriptEditor;

  private ScriptCompiler scriptCompiler = new ScriptCompiler(scriptSymbolFactory);

  public ScriptPanel(List<IMessageClient> clientList,
      ISchemaRegistryService schemaRegistryService,
      KafkaAdminService kafkaAdmin,
      ComponentFactory componentFactory,
      I18nService i18nService) {
    super(clientList, schemaRegistryService, kafkaAdmin, componentFactory, i18nService);

    $$$setupUI$$$();

    super.initialize();

    Configuration.getEnvironmentList().forEach(comboEnvironment::addItem);
    comboEnvironment.addActionListener(e -> {
      boolean local = ((Environment) comboEnvironment.getSelectedItem()).getName().toLowerCase()
          .contains("local");
      dangerLabel.setVisible(!local);
    });
    dangerLabel.setVisible(false);

    buttonExecute.addActionListener(e -> asyncExecute());

    buttonStop.addActionListener(e -> stopAsyncTasks());

    buttonCheckEnvironment.addActionListener(e -> asyncRetrieveTopics());

    cleanButton.addActionListener(e -> cleanEditor());
    copyButton.addActionListener(e -> copyToClipboard());

    enableTextSearch(searchTextField, scriptEditor);
  }

  private void openFileForScript() {
    File file = chooseAndReadFile();
    if (file != null) {
      loadTextFromFile(file, scriptEditor);
    }
  }

  private void asyncExecute() {
    String scriptCode = scriptEditor.getText();
    GenericClient genericClient = (GenericClient) clientList.stream()
        .filter(GenericClient.class::isInstance)
        .findFirst()
        .orElse(null);

    if (genericClient == null) {
      printError("No se ha definido ningún GenericClient");
      return;
    }
    ExecutionContext context = ExecutionContext.builder()
        .environment(getEnvironment())
        .kafkaGenericClient(genericClient)
        .templateExecutor(scriptTemplateExecutor)
        .build();

    scriptSymbolFactory.setContext(context);

    ScriptNode scriptNode;
    try {
      scriptNode = scriptCompiler.compile(scriptCode);
    } catch (ScriptCompilerException e) {
      printError("El script tiene errores");
      printException(e);
      return;
    }
    buttonExecute.setEnabled(false);
    buttonStop.setEnabled(true);
    enqueueAction("Ejecución de script iniciada");
    executeAsyncTask(() -> executeScript(scriptNode, context));
  }

  private Void executeScript(ScriptNode scriptNode, ExecutionContext context) {

    try {
      scriptNode.execute(context);
      enqueueSuccessful("El script se ha ejecutado correctamente");
    } catch (ScriptExecutionException e) {
      enqueueError("No de ha podido ejecutar el script");
      enqueueException(e);
    }
    // TODO: Esto no debería hacerse en este thread
    buttonStop.setEnabled(false);
    return null;
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT edit this method OR call it in your
   * code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    createUIComponents();
    contentPane = new JPanel();
    contentPane.setLayout(new GridLayoutManager(5, 2, new Insets(10, 10, 10, 10), -1, -1));
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
    buttonExecute = new JButton();
    buttonExecute.setIcon(new ImageIcon(getClass().getResource("/images/execute.png")));
    this.$$$loadButtonText$$$(buttonExecute, this.$$$getMessageFromBundle$$$("messages", "button.execute"));
    panel1.add(buttonExecute,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer1 = new Spacer();
    panel1.add(spacer1, new GridConstraints(0, 3, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    dangerLabel = new JLabel();
    dangerLabel.setIcon(new ImageIcon(getClass().getResource("/images/danger.png")));
    dangerLabel.setText("");
    dangerLabel.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.danger.not.local"));
    panel1.add(dangerLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    buttonStop = new JButton();
    buttonStop.setEnabled(false);
    buttonStop.setIcon(new ImageIcon(getClass().getResource("/images/stop.png")));
    buttonStop.setText("");
    buttonStop.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.stop.consume"));
    panel1.add(buttonStop, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel2,
        new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(615, 40), null, 0, false));
    final JLabel label1 = new JLabel();
    this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages", "label.environment"));
    panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboEnvironment = new JComboBox();
    final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
    comboEnvironment.setModel(defaultComboBoxModel1);
    panel2.add(comboEnvironment,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    buttonCheckEnvironment = new JButton();
    Font buttonCheckEnvironmentFont = this.$$$getFont$$$(null, -1, -1, buttonCheckEnvironment.getFont());
    if (buttonCheckEnvironmentFont != null) {
      buttonCheckEnvironment.setFont(buttonCheckEnvironmentFont);
    }
    buttonCheckEnvironment.setIcon(new ImageIcon(getClass().getResource("/images/check_grey.png")));
    buttonCheckEnvironment.setText("");
    buttonCheckEnvironment.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.check.connection"));
    panel2.add(buttonCheckEnvironment,
        new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(24, 24),
            new Dimension(24, 24), new Dimension(24, 24), 0, false));
    final JPanel panel3 = new JPanel();
    panel3.setLayout(new BorderLayout(0, 0));
    contentPane.add(panel3, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, null, null, 0, false));
    final JLabel label2 = new JLabel();
    label2.setIcon(new ImageIcon(getClass().getResource("/images/search.png")));
    label2.setText("");
    panel3.add(label2, BorderLayout.WEST);
    searchTextField = new JTextField();
    searchTextField.setText("");
    panel3.add(searchTextField, BorderLayout.CENTER);
    final JPanel panel4 = new JPanel();
    panel4.setLayout(new BorderLayout(0, 0));
    contentPane.add(panel4, new GridConstraints(2, 0, 2, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    tabbedPane = new JTabbedPane();
    panel4.add(tabbedPane, BorderLayout.CENTER);
    tabInfo = new JPanel();
    tabInfo.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "tab.info"), tabInfo);
    final JScrollPane scrollPane1 = new JScrollPane();
    tabInfo.add(scrollPane1, BorderLayout.CENTER);
    infoTextPane = new InfoTextPane();
    infoTextPane.setBackground(new Color(-13948117));
    infoTextPane.setCaretColor(new Color(-1));
    infoTextPane.setEditable(false);
    infoTextPane.setEnabled(true);
    Font infoTextPaneFont = this.$$$getFont$$$(null, -1, -1, infoTextPane.getFont());
    if (infoTextPaneFont != null) {
      infoTextPane.setFont(infoTextPaneFont);
    }
    infoTextPane.setForeground(new Color(-1));
    scrollPane1.setViewportView(infoTextPane);
    tabScript = new JPanel();
    tabScript.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "tab.script"), tabScript);
    tabScript.add(scriptScrollPane, BorderLayout.CENTER);
    final JPanel panel5 = new JPanel();
    panel5.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
    panel4.add(panel5, BorderLayout.EAST);
    cleanButton = new JButton();
    cleanButton.setIcon(new ImageIcon(getClass().getResource("/images/rubber.png")));
    cleanButton.setText("");
    cleanButton.setToolTipText("Limpiar");
    panel5.add(cleanButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    copyButton = new JButton();
    copyButton.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
    copyButton.setText("");
    copyButton.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.copy.clipboard"));
    panel5.add(copyButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, null, null, 0, false));
    final Spacer spacer2 = new Spacer();
    panel5.add(spacer2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    final Spacer spacer3 = new Spacer();
    panel5.add(spacer3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, 1,
        GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 30), null, 0, false));
  }

  /**
   * @noinspection ALL
   */
  private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
    if (currentFont == null) {
      return null;
    }
    String resultName;
    if (fontName == null) {
      resultName = currentFont.getName();
    } else {
      Font testFont = new Font(fontName, Font.PLAIN, 10);
      if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
        resultName = fontName;
      } else {
        resultName = currentFont.getName();
      }
    }
    Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(),
        size >= 0 ? size : currentFont.getSize());
    boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
    Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize())
        : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
    return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
  }

  private static Method $$$cachedGetBundleMethod$$$ = null;

  private String $$$getMessageFromBundle$$$(String path, String key) {
    ResourceBundle bundle;
    try {
      Class<?> thisClass = this.getClass();
      if ($$$cachedGetBundleMethod$$$ == null) {
        Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
        $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
      }
      bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
    } catch (Exception e) {
      bundle = ResourceBundle.getBundle(path);
    }
    return bundle.getString(key);
  }

  /**
   * @noinspection ALL
   */
  private void $$$loadLabelText$$$(JLabel component, String text) {
    StringBuffer result = new StringBuffer();
    boolean haveMnemonic = false;
    char mnemonic = '\0';
    int mnemonicIndex = -1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '&') {
        i++;
        if (i == text.length()) {
          break;
        }
        if (!haveMnemonic && text.charAt(i) != '&') {
          haveMnemonic = true;
          mnemonic = text.charAt(i);
          mnemonicIndex = result.length();
        }
      }
      result.append(text.charAt(i));
    }
    component.setText(result.toString());
    if (haveMnemonic) {
      component.setDisplayedMnemonic(mnemonic);
      component.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /**
   * @noinspection ALL
   */
  private void $$$loadButtonText$$$(AbstractButton component, String text) {
    StringBuffer result = new StringBuffer();
    boolean haveMnemonic = false;
    char mnemonic = '\0';
    int mnemonicIndex = -1;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '&') {
        i++;
        if (i == text.length()) {
          break;
        }
        if (!haveMnemonic && text.charAt(i) != '&') {
          haveMnemonic = true;
          mnemonic = text.charAt(i);
          mnemonicIndex = result.length();
        }
      }
      result.append(text.charAt(i));
    }
    component.setText(result.toString());
    if (haveMnemonic) {
      component.setMnemonic(mnemonic);
      component.setDisplayedMnemonicIndex(mnemonicIndex);
    }
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return contentPane;
  }

  private void createUIComponents() {
    scriptEditor = createJsonEditor();
    scriptScrollPane = componentFactory.createEditorScroll(scriptEditor);
  }

  @Override
  public Optional<JTextComponent> getCurrentEditor() {
    int index = tabbedPane.getSelectedIndex();
    return getUmpteenthEditor(index, infoTextPane, scriptEditor);
  }

  @Override
  protected void showConnectionStatus(Boolean ok) {
    buttonCheckEnvironment.setIcon(Optional.ofNullable(ok)
        .map(b -> b ? iconCheckOk : iconCheckFail)
        .orElse(iconCheckUndefined));
  }

  @Override
  protected Environment getEnvironment() {
    return (Environment) comboEnvironment.getSelectedItem();
  }

  public InfoTextPane getInfoTextPane() {
    return (InfoTextPane) infoTextPane;
  }

  @Override
  protected void asyncTaskFinished() {
    super.asyncTaskFinished();
    buttonExecute.setEnabled(true);
    buttonStop.setEnabled(false);
  }

}

