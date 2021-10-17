package es.jbp.kajtools.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import es.jbp.kajtools.Environment;
import es.jbp.kajtools.i18n.I18nService;
import es.jbp.kajtools.ui.InfoDocument.InfoDocumentBuilder;
import es.jbp.kajtools.ui.InfoMessage.Type;
import es.jbp.kajtools.ui.interfaces.InfoReportablePanel;
import es.jbp.kajtools.util.ClassScanner;
import es.jbp.kajtools.util.DeepTestObjectCreator;
import es.jbp.kajtools.util.ResourceUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
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
import javax.swing.JTextPane;
import javax.swing.text.JTextComponent;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.springframework.util.CollectionUtils;

public class JsonGeneratorPanel extends BasePanel implements InfoReportablePanel {

  private final Set<String> classSet;

  private Future<GeneratedResult> futureResult;

  private JTextField filterTextField;
  private JComboBox<String> classComboBox;
  private JTabbedPane tabbedPane;
  private JPanel topPanel;
  private JTextPane infoTextPane;
  private JPanel tabJson;
  private JPanel tabInfo;
  private JPanel tabSchema;
  private JButton generateJsonButton;
  private JButton copyButton;
  @Getter
  private JPanel contentPane;
  private RTextScrollPane jsonScrollPane;
  private JScrollPane infoScrollPane;
  private RTextScrollPane schemaScrollPane;
  private JButton cleanButton;
  private JTextField searchTextField;
  private JButton checkJsonButton;
  private RSyntaxTextArea jsonEditor;
  private RSyntaxTextArea schemaEditor;

  public JsonGeneratorPanel(ComponentFactory componentFactory,
      I18nService i18nService) {

    super(componentFactory, i18nService);

    $$$setupUI$$$();

    super.initialize();

    List<String> lineList = ResourceUtil.readResourceStringList("clases.txt");

    classSet = ClassScanner.create().findClasses(lineList).stream().map(Class::getName)
        .collect(Collectors.toSet());

    String[] arr = classSet.stream().sorted().toArray(String[]::new);
    classComboBox.setModel(new DefaultComboBoxModel<>(arr));
    generateJsonButton.addActionListener(e -> asyncGenerateAll());
    filterTextField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        super.focusLost(e);
        filterClasses();
      }
    });
    copyButton.addActionListener(e -> copyToClipboard());
    cleanButton.addActionListener(e -> cleanEditor());

    enableTextSearch(searchTextField, jsonEditor, schemaEditor);

    checkJsonButton.addActionListener(e -> asyncCheckJson());
  }

  @Override
  public JTextField getSearchTextField() {
    return searchTextField;
  }

  public Optional<JTextComponent> getCurrentEditor() {
    int index = tabbedPane.getSelectedIndex();
    if (index == 0) {
      return Optional.of(infoTextPane);
    } else if (index == 1) {
      return Optional.of(jsonEditor);
    } else if (index == 2) {
      return Optional.of(schemaEditor);
    } else {
      return Optional.empty();
    }
  }

  private void filterClasses() {
    String filtro = filterTextField.getText();
    List<String> listaFiltros = Arrays.asList(filtro.toLowerCase().split("\\s"));

    String[] arr = classSet.stream()
        .filter(s -> containsAll(s, listaFiltros))
        .sorted()
        .toArray(String[]::new);
    classComboBox.setModel(new DefaultComboBoxModel<>(arr));
  }

  private boolean containsAll(String texto, List<String> filterList) {
    String lowercaseText = texto.toLowerCase();
    return filterList.stream().allMatch(lowercaseText::contains);
  }

  private static class GeneratedResult {

    Object object;
    String json;
    String schema;
  }

  private void asyncCheckJson() {

    String nombreClase = classComboBox.getEditor().getItem().toString();
    String json = jsonEditor.getText();
    futureResult = executeAsyncTask(() -> this.checkJson(nombreClase, json), () -> {
    });
  }

  private void asyncGenerateAll() {
    printAction("Generando JSON...");
    String nombreClase = classComboBox.getEditor().getItem().toString();
    futureResult = executeAsyncTask(() -> this.generateAll(nombreClase), this::generateDone);
  }

  private void generateDone() {

    GeneratedResult result = null;
    try {
      result = futureResult.get();
    } catch (Exception e) {
      printException(e);
      return;
    }

    jsonEditor.setText(result.json == null ? "" : result.json);
    jsonEditor.setCaretPosition(0);

    schemaEditor.setText(result.schema == null ? "" : result.schema);
    schemaEditor.setCaretPosition(0);
  }

  private GeneratedResult generateAll(String nombreClase) {
    GeneratedResult result = new GeneratedResult();
    result.object = instantiateObjet(nombreClase);
    result.json = generateJson(result.object);
    result.schema = generateSchema(result.object);
    return result;
  }

  private GeneratedResult checkJson(String nombreClase, String json) {
    GeneratedResult result = new GeneratedResult();
    result.object = instantiateObjetFromJson(nombreClase, json);
    return result;
  }

  private Object instantiateObjet(String nombreClase) {

    DeepTestObjectCreator creator = new DeepTestObjectCreator();

    Object generatedObject = creator.createObject(nombreClase, "");
    if (generatedObject == null) {
      enqueueError("No se ha podido crear la instancia el objeto");
    } else {
      enqueueSuccessful("Se ha creado la instancia del objeto correctamente");
      enqueueLink(InfoDocument.simpleDocument("Object::toString", InfoDocument.Type.INFO, generatedObject.toString()));
    }
    enqueueErrors(creator.getErrors());
    return generatedObject;
  }

  private String generateJson(Object generatedObject) {
    enqueueAction("Generando el JSON...");

    DeepTestObjectCreator creator = new DeepTestObjectCreator();
    String json = creator.getJsonFromObject(generatedObject);
    if (StringUtils.isBlank(json)) {
      enqueueError("No se ha podido generar el JSON");
    } else {
      enqueueSuccessful("Se ha generado el JSON correctamente");
    }
    enqueueErrors(creator.getErrors());
    return json;
  }

  private void enqueueErrors(List<String> errors) {
    if (CollectionUtils.isEmpty(errors)) {
      return;
    }
    InfoDocumentBuilder documentBuilder = InfoDocument.builder().title("errores").type(InfoDocument.Type.INFO);
    ListUtils.emptyIfNull(errors).forEach(e -> documentBuilder.left(new InfoMessage(e, Type.TRACE)));
    enqueueLink(documentBuilder.build());
  }

  private String generateSchema(Object generatedObject) {
    DeepTestObjectCreator creator = new DeepTestObjectCreator();
    String schema = creator.extractAvroSchema(generatedObject);
    if (StringUtils.isBlank(schema)) {
      enqueueError("No se ha podido generar el esquema AVRO");
    } else {
      enqueueSuccessful("Se ha generado el esquema AVRO correctamente");
    }
    enqueueErrors(creator.getErrors());
    return schema;
  }

  private Object instantiateObjetFromJson(String nombreClase, String json) {

    enqueueAction("Generando la instancia del objeto desde el JSON...");

    DeepTestObjectCreator creator = new DeepTestObjectCreator();

    Object generatedObject = creator.createObjectFromJson(nombreClase, json);
    if (generatedObject != null) {
      enqueueSuccessful("Se ha creado la instancia del Objeto desde el JSON correctamente");
      enqueueLink(InfoDocument.simpleDocument("Object::toString()",
          InfoDocument.Type.INFO, generatedObject.toString()));
    } else {
      enqueueError("No se ha podido crear la instancia el Objeto desde el JSON");
    }
    enqueueErrors(creator.getErrors());
    return generatedObject;
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
    contentPane.setLayout(new GridLayoutManager(4, 2, new Insets(10, 10, 10, 10), -1, -1));
    topPanel = new JPanel();
    topPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(topPanel, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, null, null, 0, false));
    final JLabel label1 = new JLabel();
    this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages", "label.filter"));
    topPanel.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    filterTextField = new JTextField();
    topPanel.add(filterTextField,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null,
            0, false));
    final JLabel label2 = new JLabel();
    this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("messages", "label.class"));
    topPanel.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    classComboBox = new JComboBox();
    classComboBox.setEditable(true);
    topPanel.add(classComboBox,
        new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, null, null, 0, false));
    generateJsonButton = new JButton();
    this.$$$loadButtonText$$$(generateJsonButton, this.$$$getMessageFromBundle$$$("messages", "button.json.generate"));
    generateJsonButton.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.generate.all"));
    panel1.add(generateJsonButton,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    final Spacer spacer1 = new Spacer();
    panel1.add(spacer1, new GridConstraints(0, 2, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    checkJsonButton = new JButton();
    this.$$$loadButtonText$$$(checkJsonButton, this.$$$getMessageFromBundle$$$("messages", "button.json.check"));
    checkJsonButton.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.check.json"));
    panel1.add(checkJsonButton,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new BorderLayout(0, 0));
    contentPane.add(panel2,
        new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label3 = new JLabel();
    label3.setIcon(new ImageIcon(getClass().getResource("/images/search.png")));
    label3.setText("");
    panel2.add(label3, BorderLayout.WEST);
    searchTextField = new JTextField();
    panel2.add(searchTextField, BorderLayout.CENTER);
    final JPanel panel3 = new JPanel();
    panel3.setLayout(new BorderLayout(0, 0));
    contentPane.add(panel3, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    tabbedPane = new JTabbedPane();
    panel3.add(tabbedPane, BorderLayout.CENTER);
    tabInfo = new JPanel();
    tabInfo.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "tab.info"), tabInfo);
    infoScrollPane = new JScrollPane();
    tabInfo.add(infoScrollPane, BorderLayout.CENTER);
    infoTextPane.setAutoscrolls(false);
    infoTextPane.setBackground(new Color(-13948117));
    infoTextPane.setCaretColor(new Color(-1));
    infoTextPane.setEditable(false);
    infoTextPane.setForeground(new Color(-1));
    infoScrollPane.setViewportView(infoTextPane);
    tabJson = new JPanel();
    tabJson.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab("JSON", tabJson);
    tabJson.add(jsonScrollPane, BorderLayout.CENTER);
    tabSchema = new JPanel();
    tabSchema.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "tab.avro.schema"), tabSchema);
    tabSchema.add(schemaScrollPane, BorderLayout.CENTER);
    final JPanel panel4 = new JPanel();
    panel4.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
    panel3.add(panel4, BorderLayout.EAST);
    cleanButton = new JButton();
    cleanButton.setHorizontalTextPosition(0);
    cleanButton.setIcon(new ImageIcon(getClass().getResource("/images/rubber.png")));
    cleanButton.setText("");
    cleanButton.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.clean"));
    cleanButton.putClientProperty("html.disable", Boolean.TRUE);
    panel4.add(cleanButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, null, null, 0, false));
    copyButton = new JButton();
    copyButton.setHorizontalTextPosition(0);
    copyButton.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
    copyButton.setText("");
    copyButton.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.copy.clipboard"));
    panel4.add(copyButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, null, null, 0, false));
    final Spacer spacer2 = new Spacer();
    panel4.add(spacer2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    final Spacer spacer3 = new Spacer();
    panel4.add(spacer3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, 1,
        GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 30), null, 0, false));
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

    infoTextPane = new InfoTextPane();

    jsonEditor = createJsonEditor();
    jsonScrollPane = componentFactory.createEditorScroll(jsonEditor);

    schemaEditor = createJsonEditor();
    schemaScrollPane = componentFactory.createEditorScroll(schemaEditor);
  }

  @Override
  public InfoTextPane getInfoTextPane() {
    return (InfoTextPane) infoTextPane;
  }


  protected Environment getEnvironment() {
    return null;
  }
}
