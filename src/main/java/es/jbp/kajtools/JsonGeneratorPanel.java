package es.jbp.kajtools;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import es.jbp.kajtools.util.ClassScanner;
import es.jbp.kajtools.util.DeepTestObjectCreator;
import es.jbp.kajtools.util.JsonTemplateCreator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
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
import org.apache.commons.lang3.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class JsonGeneratorPanel extends BasePanel {

  private final Set<String> classSet;

  private Future<GeneratedResult> futureResult;

  private JTextField filterTextField;
  private JComboBox classComboBox;
  private JTabbedPane tabbedPane;
  private JPanel topPanel;
  @Getter
  private JTextPane infoTextPane;
  private JPanel tabJson;
  private JPanel tabInfo;
  private JPanel tabSchema;
  private JButton generarteButton;
  private JButton copyButton;
  @Getter
  private JPanel contentPane;
  private RTextScrollPane jsonScrollPane;
  private JScrollPane infoScrollPane;
  private RTextScrollPane schemaScrollPane;
  private JButton cleanButton;
  private JTextField searchTextField;
  private JPanel tabTemplate;
  private RTextScrollPane templateScrollPane;
  private JButton generatrFromJsonButton;
  private RSyntaxTextArea jsonEditor;
  private RSyntaxTextArea schemaEditor;
  private RSyntaxTextArea templateEditor;

  public JsonGeneratorPanel() {
    $$$setupUI$$$();
    InputStream inputStream = JsonGeneratorPanel.class.getClassLoader()
        .getResourceAsStream("clases.txt");
    List<String> lineList = new BufferedReader(new InputStreamReader(inputStream,
        StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
    classSet = ClassScanner.create().findClasses(lineList).stream().map(c -> c.getName())
        .collect(Collectors.toSet());

    String[] arr = classSet.stream().sorted().toArray(size -> new String[size]);
    classComboBox.setModel(new DefaultComboBoxModel(arr));
    generarteButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        asyncGenerateAll();
      }
    });
    filterTextField.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(FocusEvent e) {
        super.focusLost(e);
        filterClasses();
      }
    });
    copyButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        copyToClipboard();
      }
    });
    cleanButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cleanEditor();
      }
    });

    enableTextSearch(searchTextField, jsonEditor, schemaEditor, templateEditor);

    generatrFromJsonButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        asyncGenerateFromJson();
      }
    });
  }

  protected Optional<JTextComponent> getCurrentEditor() {
    int index = tabbedPane.getSelectedIndex();
    if (index == 0) {
      return Optional.of(infoTextPane);
    } else if (index == 1) {
      return Optional.of(jsonEditor);
    } else if (index == 2) {
      return Optional.of(schemaEditor);
    } else if (index == 3) {
      return Optional.of(templateEditor);
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
        .toArray(size -> new String[size]);
    classComboBox.setModel(new DefaultComboBoxModel(arr));
  }

  private boolean containsAll(String texto, List<String> filterList) {
    String lowercaseText = texto.toLowerCase();
    return filterList.stream().allMatch(s -> lowercaseText.contains(s));
  }

  private class GeneratedResult {
    Object object;
    String json;
    String schema;
    String template;
  }

  private void asyncGenerateFromJson() {

    String nombreClase = classComboBox.getEditor().getItem().toString();
    String json = jsonEditor.getText();
    futureResult = executeAsyncTask(() -> this.generateFromJson(nombreClase, json), this::generateDone);
  }

  private void asyncGenerateAll() {
    printAction("Generando JSON, AVRO y Plantilla...");
    String nombreClase = classComboBox.getEditor().getItem().toString();
    futureResult = executeAsyncTask(() -> this.generateAll(nombreClase), this::generateDone);
  }

  private void generateDone() {

    GeneratedResult result = null;
    try {
      result = futureResult.get();
    } catch (Exception e) {
      printError("No se ha podido obtener el resultado. Causa: " + e.getMessage());
      return;
    }

    jsonEditor.setText(result.json == null ? "" : result.json);
    jsonEditor.setCaretPosition(0);

    schemaEditor.setText(result.schema == null ? "" : result.schema);
    schemaEditor.setCaretPosition(0);

    templateEditor.setText(result.template == null ? "" : result.template);
    templateEditor.setCaretPosition(0);
  }

  private GeneratedResult generateAll(String nombreClase) {
    GeneratedResult result = new GeneratedResult();
    result.object = instantiateObjet(nombreClase);
    result.json = generateJson(result.object);
    result.schema = generateSchema(result.object);
    result.template = generateTemplate(result.json);
    return result;
  }

  private GeneratedResult generateFromJson(String nombreClase, String json) {
    GeneratedResult result = new GeneratedResult();
    result.object = instantiateObjetFromJson(nombreClase, json);
    result.schema = generateSchema(result.object);
    result.template = generateTemplate(json);
    return result;
  }

  private Object instantiateObjet(String nombreClase) {
    enqueueAction("Generando la instancia del objeto...");

    DeepTestObjectCreator creator = new DeepTestObjectCreator();

    Object generatedObject = creator.createObject(nombreClase, "");
    if (generatedObject == null) {
      enqueueError("No se ha podido crear la instancia el objeto");
    } else {
      enqueueSuccessful("Se ha creado la instancia del objeto correctamente");
      enqueueInfo(generatedObject.toString());
    }
    creator.getErrors().forEach(m -> enqueueError(m));
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
    creator.getErrors().forEach(m -> enqueueError(m));
    return json;
  }

  private String generateSchema(Object generatedObject) {
    DeepTestObjectCreator creator = new DeepTestObjectCreator();
    String schema = creator.extractAvroSchema(generatedObject);
    if (StringUtils.isBlank(schema)) {
      enqueueError("No se ha podido generar el esquema AVRO");
    } else {
      enqueueSuccessful("Se ha generado el esquema AVRO correctamente");
    }
    creator.getErrors().forEach(m -> enqueueError(m));
    return schema;
  }

  private String generateTemplate(String json) {

    enqueueAction("Generando la plantilla...");

    JsonTemplateCreator creator = new JsonTemplateCreator(json);
    String template = creator.create();
    if (!StringUtils.isBlank(json)) {
      enqueueSuccessful("Se ha generado la plantilla correctamente");
    }
    return template;
  }

  private Object instantiateObjetFromJson(String nombreClase, String json) {

    enqueueAction("Generando la instancia del objeto desde el JSON...");

    DeepTestObjectCreator creator = new DeepTestObjectCreator();

    Object generatedObject = creator.createObjectFromJson(nombreClase, json);
    if (generatedObject != null) {
      enqueueSuccessful("Se ha creado la instancia del Objeto desde el JSON correctamente");
      enqueueInfo(generatedObject.toString());
    } else {
      enqueueError("No se ha podido crear la instancia el Objeto desde el JSON");
    }
    creator.getErrors().forEach(m -> enqueueError(m));
    return generatedObject;
  }


  /**
   * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT edit this method OR
   * call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    createUIComponents();
    contentPane = new JPanel();
    contentPane.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
    topPanel = new JPanel();
    topPanel.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(topPanel,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label1 = new JLabel();
    label1.setText("Filtro:");
    topPanel.add(label1,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
            false));
    filterTextField = new JTextField();
    topPanel.add(filterTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST,
        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW,
        GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    final JLabel label2 = new JLabel();
    label2.setText("Clase:");
    topPanel.add(label2,
        new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
            false));
    classComboBox = new JComboBox();
    classComboBox.setEditable(true);
    topPanel.add(classComboBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST,
        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    tabbedPane = new JTabbedPane();
    contentPane.add(tabbedPane,
        new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
            new Dimension(200, 200), null, 0, false));
    tabInfo = new JPanel();
    tabInfo.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab("Información", tabInfo);
    infoScrollPane = new JScrollPane();
    tabInfo.add(infoScrollPane, BorderLayout.CENTER);
    infoTextPane = new JTextPane();
    infoTextPane.setAutoscrolls(false);
    infoTextPane.setBackground(new Color(-16777216));
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
    tabbedPane.addTab("Esquema AVRO", tabSchema);
    tabSchema.add(schemaScrollPane, BorderLayout.CENTER);
    tabTemplate = new JPanel();
    tabTemplate.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab("Plantilla", tabTemplate);
    tabTemplate.add(templateScrollPane, BorderLayout.CENTER);
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel1,
        new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    generarteButton = new JButton();
    generarteButton.setText("Generar");
    generarteButton.setToolTipText("Generar objeto, JSON y esquema");
    panel1.add(generarteButton,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null,
            null, 0, false));
    cleanButton = new JButton();
    cleanButton.setHorizontalTextPosition(0);
    cleanButton.setIcon(new ImageIcon(getClass().getResource("/images/rubber.png")));
    cleanButton.setText("");
    cleanButton.setToolTipText("Limpiar");
    cleanButton.putClientProperty("html.disable", Boolean.TRUE);
    panel1.add(cleanButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER,
        GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer1 = new Spacer();
    panel1.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER,
        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null,
        0, false));
    copyButton = new JButton();
    copyButton.setHorizontalTextPosition(0);
    copyButton.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
    copyButton.setText("");
    copyButton.setToolTipText("Copiar al portapapeles");
    panel1.add(copyButton,
        new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null,
            null, 0, false));
    generatrFromJsonButton = new JButton();
    generatrFromJsonButton.setText("Generar desde JSON");
    generatrFromJsonButton.setToolTipText("Generar objeto, esquema y plantilla desde el JSON");
    panel1.add(generatrFromJsonButton,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null,
            null, 0, false));
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new BorderLayout(0, 0));
    contentPane.add(panel2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER,
        GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label3 = new JLabel();
    label3.setIcon(new ImageIcon(getClass().getResource("/images/search.png")));
    label3.setText("");
    panel2.add(label3, BorderLayout.WEST);
    searchTextField = new JTextField();
    panel2.add(searchTextField, BorderLayout.CENTER);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return contentPane;
  }

  private void createUIComponents() {
    jsonEditor = createJsonEditor();
    jsonScrollPane = createEditorScroll(jsonEditor);

    schemaEditor = createJsonEditor();
    schemaScrollPane = createEditorScroll(schemaEditor);

    templateEditor = createJsonEditor();
    templateScrollPane = createEditorScroll(templateEditor);
  }

  @Override
  protected void enableButtons(boolean enable) {
  }
}
