package es.jbp.kajtools.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import es.jbp.kajtools.Environment;
import es.jbp.kajtools.kafka.GenericClient;
import es.jbp.kajtools.IMessageClient;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.KajToolsApp;
import es.jbp.kajtools.configuration.Configuration;
import es.jbp.kajtools.ui.InfoMessage.Type;
import es.jbp.kajtools.kafka.TopicItem;
import es.jbp.kajtools.util.JsonFirstComparator;
import es.jbp.kajtools.util.JsonUtils;
import es.jbp.kajtools.util.ResourceUtil;
import es.jbp.kajtools.util.SchemaRegistryService;
import es.jbp.kajtools.util.SchemaRegistryService.SubjectType;
import es.jbp.kajtools.util.TemplateExecutor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleContext;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.springframework.web.client.HttpClientErrorException.NotFound;

public class KafkaProducerPanel extends KafkaBasePanel {

  private static final int MAXIMUM_QUANTITY = 10;

  private final SchemaRegistryService schemaRegistryService;
  private String currentDirectory;
  //  private int counter;
  private final Map<String, SchemaCheckStatus> checkedSchemaTopics = new HashMap<>();

  @Getter
  private JPanel contentPane;
  private JButton buttonSend;
  private JComboBox comboTopic;
  private JComboBox comboValue;
  private JComboBox comboKey;
  private JComboBox comboEnvironment;
  private JButton buttonCompareSchemas;
  private JTabbedPane tabbedPane;
  private JPanel tabInfo;
  private JPanel tabValue;
  private JPanel tabKey;
  @Getter
  private JTextPane infoTextPane;
  private JComboBox comboProducer;
  private JButton buttonOpenFileValue;
  private JButton buttonOpenFileKey;
  private JButton cleanButton;
  private JButton copyButton;
  private JComboBox quantityComboBox;
  private JTextField searchTextField;
  private JLabel dangerLabel;
  private JComboBox comboDomain;
  private JButton buttonFindTopic;
  private JButton buttonCheckEnvironment;
  private RTextScrollPane valueScrollPane;
  private RTextScrollPane keyScrollPane;
  private RTextScrollPane variablesScrollPane;
  private RSyntaxTextArea valueEditor;
  private RSyntaxTextArea keyEditor;
  private RSyntaxTextArea variablesEditor;

  public KafkaProducerPanel() {

    $$$setupUI$$$();

    variablesEditor.setText(ResourceUtil.readResourceString("variables.properties"));

    currentDirectory = new File(System.getProperty("user.home")).getPath();
    this.schemaRegistryService = KajToolsApp.getInstance().getSchemaRegistryService();

    dangerLabel.setVisible(false);

    buttonSend.addActionListener(e -> asyncSendEvent());

    // Combo Entorno
    Configuration.getEnvironmentList().forEach(comboEnvironment::addItem);
    comboEnvironment.addActionListener(e -> {
      boolean local = ((Environment) comboEnvironment.getSelectedItem()).getName().toLowerCase()
          .contains("local");
      dangerLabel.setVisible(!local);
      cleanTopics();
    });

    buttonCheckEnvironment.addActionListener(e -> asyncRetrieveTopics());

    // Combo Dominio
    final List<IMessageClient> clientList = KajToolsApp.getInstance().getClientList();
    clientList.stream().map(IMessageClient::getDomain).distinct().forEach(comboDomain::addItem);
    comboDomain.addActionListener(e -> updateProducers());

    // Combo Productores
    comboProducer.addActionListener(e -> updateTopicsKeysAndValues());
    updateProducers();

    // Combos Topics, Values y Keys
    updateTopicsKeysAndValues();

    buttonFindTopic.addActionListener(e -> findTopic());

    buttonCompareSchemas.addActionListener(e -> asyncCheckSchema());
    comboKey.addActionListener(e -> loadResourceForKey());
    comboValue.addActionListener(e -> loadResourceForValue());
    buttonOpenFileKey.addActionListener(e -> openFileForKey());
    buttonOpenFileValue.addActionListener(e -> openFileForEvent());
    cleanButton.addActionListener(e -> cleanEditor());
    copyButton.addActionListener(e -> copyToClipboard());

    IntStream.rangeClosed(1, MAXIMUM_QUANTITY).forEach(quantityComboBox::addItem);

    enableTextSearch(searchTextField, valueEditor, keyEditor, variablesEditor);
  }

  private void findTopic() {
    TopicItem topicItem = selectTopic();
    if (topicItem != null) {
      comboTopic.getEditor().setItem(topicItem.getName());
    }
  }

  private void updateProducers() {
    comboProducer.removeAllItems();
    String domain = Objects.toString(comboDomain.getSelectedItem());
    final List<IMessageClient> producerList = KajToolsApp.getInstance().getClientList();
    producerList.stream()
        .filter(p -> StringUtils.isBlank(domain) || domain.equals(p.getDomain()))
        .forEach(comboProducer::addItem);
  }

  private void updateTopicsKeysAndValues() {
    comboTopic.removeAllItems();
    comboValue.removeAllItems();
    comboKey.removeAllItems();
    IMessageClient producer = (IMessageClient) comboProducer.getSelectedItem();
    if (producer == null) {
      return;
    }
    producer.getAvailableTopics().forEach(comboTopic::addItem);
    comboTopic.setSelectedItem(producer.getDefaultTopic());

    producer.getAvailableValues().stream()
        .sorted(new JsonFirstComparator())
        .forEach(comboValue::addItem);

    producer.getAvailableKeys().stream()
        .sorted(new JsonFirstComparator())
        .forEach(comboKey::addItem);

    loadResourceForKey();
    loadResourceForValue();
  }

  private void openFileForKey() {
    File file = chooseAndReadFile();
    if (file != null) {
      loadJsonFromFile(file, keyEditor);
    }
  }

  private void openFileForEvent() {
    File file = chooseAndReadFile();
    if (file != null) {
      loadJsonFromFile(file, valueEditor);
    }
  }

  private File chooseAndReadFile() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setCurrentDirectory(new File(currentDirectory));
    int result = fileChooser.showOpenDialog(contentPane);
    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      currentDirectory = selectedFile.getPath();
      return selectedFile;
    }
    return null;
  }

  private void loadResourceForKey() {
    String path = Optional.ofNullable(comboKey.getSelectedItem()).map(Object::toString)
        .orElse("");

    loadJsonFromResource(path, keyEditor);
  }

  private void loadResourceForValue() {
    String path = Optional.ofNullable(comboValue.getSelectedItem()).map(Object::toString)
        .orElse("");
    loadJsonFromResource(path, valueEditor);
  }

  private void loadJsonFromResource(String path, RSyntaxTextArea jsonEditor) {

    if (StringUtils.isBlank(path)) {
      return;
    }
    IMessageClient producer = (IMessageClient) comboProducer.getSelectedItem();
    if (producer != null) {
      path = producer.getFolder() + File.separator + path;
    }
    String json = ResourceUtil.readResourceString(path);
    jsonEditor.setText(json);
    jsonEditor.setCaretPosition(0);
  }

  private void loadJsonFromFile(File file, RSyntaxTextArea jsonEditor) {
    try {
      String json = ResourceUtil.readFileString(file);
      jsonEditor.setText(json);
      jsonEditor.setCaretPosition(0);
    } catch (Exception ex) {
      printError("No se ha podido cargar el archivo.");
      printInfo(ex.getMessage());
    }
  }

  private void asyncSendEvent() {
    Environment environment = getEnvironment();
    IMessageClient producer = (IMessageClient) comboProducer.getSelectedItem();
    String topic = comboTopic.getEditor().getItem().toString();

    SchemaCheckStatus status = checkedSchemaTopics.get(environment.getName() + "$" + topic);
    boolean local = environment.getName().toLowerCase().contains("local");
    if (!local && status != SchemaCheckStatus.EQUALS) {
      boolean userAccepts = warnUserAboutCheckSchema(status, topic);
      if (!userAccepts) {
        return;
      }
    }

    printAction("Enviando evento a " + topic);
    String key = keyEditor.getText();
    String event = valueEditor.getText();
    templateExecutor.setVariables(variablesEditor.getText());

    int quantity = (int) quantityComboBox.getSelectedItem();
    executeAsyncTask(() -> sendMessage(environment, producer, topic, key, event, quantity));
  }

  private boolean warnUserAboutCheckSchema(SchemaCheckStatus status, String topic) {
    String message;
    if (status == SchemaCheckStatus.NOT_FOUND) {
      message = "No hay ningún esquema en el Schema Registry.";
    } else if (status == SchemaCheckStatus.NOT_EQUALS) {
      message = "El esquema del AVRO no coincide con el del Schema Registry";
    } else if (status == SchemaCheckStatus.NOT_CHECKED) {
      message = "No se pudo comprobar el esquema. No debería enviar un evento al entorno DEVELOP sin antes comprobarlo";
    } else {
      JOptionPane.showMessageDialog(contentPane,
          "Topic: " + topic + "\n" +
              "Antes de enviar un mensaje debe comprobar el esquema del topic",
          "Atención", JOptionPane.WARNING_MESSAGE);
      return false;
    }

    int response = JOptionPane.showConfirmDialog(contentPane,
        "Topic: " + topic + "\n" + message + "\n" +
            "Si envía el mensaje se actualizará el esquema en el Schema Registry.\n" +
            "¿Desea enviar el mensaje de todas formas?",
        "Atención", JOptionPane.YES_NO_OPTION);
    return response == JOptionPane.YES_OPTION;
  }

  private Void sendMessage(Environment environment, IMessageClient producer,
      String topic, String key, String value, int quantity) {

    templateExecutor.resetIndexCounter();
    for (int i = 1; i <= quantity; i++) {
      sendMessage(environment, producer, topic, key, value);
      templateExecutor.avanceCounters();
    }
    return null;
  }

  private void sendMessage(Environment environment, IMessageClient producer, String topic, String key,
      String event) {

    String jsonKey = getJsonFromTemplate(key, "KEY");
    String jsonEvent = getJsonFromTemplate(event, "EVENT");

    if (StringUtils.isBlank(jsonKey)) {
      enqueueError("No se va a mandar el mensaje porque no se ha indicado ninguna KEY");
      return;
    }
    if (StringUtils.isBlank(jsonEvent)) {
      enqueueError("No se va a mandar el mensaje porque no se ha indicado ningún EVENT");
      return;
    }
    if (StringUtils.isBlank(topic)) {
      enqueueError("No se va a mandar el mensaje porque no se ha indicado ningún topic");
      return;
    }

    try {
      producer.sendFromJson(environment, topic, jsonKey, jsonEvent);
    } catch (KajException ex) {
      enqueueException(ex);
      return;
    }
    enqueueSuccessful("Enviado el evento correctamente");
  }

  private String getJsonFromTemplate(String json, String name) {

    if (!TemplateExecutor.containsTemplateExpressions(json)) {
      return json;
    }

    String generatedJson;
    try {
      generatedJson = templateExecutor.templateToJson(json);
    } catch (Throwable ex) {
      enqueueError("No se ha podido generar el JSON del EVENT a partir de la plantilla");
      enqueueInfo("[" + ex.getClass().getName() + "] " + ex.getMessage());
      return null;
    }
    enqueueInfo("Se ha generado el " + name + ":");
    enqueueInfo(generatedJson);
    return generatedJson;
  }


  private void asyncCheckSchema() {
    IMessageClient producer = (IMessageClient) comboProducer.getSelectedItem();
    if (producer instanceof GenericClient) {
      printError(
          "No es posible comparar los esquemas con el " + producer.getClass().getSimpleName());
      return;
    }
    String topic = comboTopic.getEditor().getItem().toString();
    printAction("Comprobando esquemas del topic " + topic);

    String jsonKey = keyEditor.getText();
    String jsonValue = valueEditor.getText();
    templateExecutor.setVariables(variablesEditor.getText());

    executeAsyncTask(() -> checkSchema(producer, topic, jsonKey, jsonValue, getEnvironment()));
  }

  private Void checkSchema(IMessageClient producer, String topic, String jsonKey,
      String jsonValue, Environment environment) {

    SchemaCheckStatus keySchemaOk = checkSchema(producer, topic, environment, jsonKey, SubjectType.key);
    SchemaCheckStatus valueSchemaOk = checkSchema(producer, topic, environment, jsonValue, SubjectType.value);

    if (keySchemaOk != null && valueSchemaOk != null) {
      checkedSchemaTopics
          .put(environment.getName() + "$" + topic, SchemaCheckStatus.getMoreSignificant(keySchemaOk, valueSchemaOk));
    } else if (checkedSchemaTopics.get(topic) != null) {
      checkedSchemaTopics.remove(topic);
    }
    return null;
  }

  private enum SchemaCheckStatus {
    NOT_CHECKED,
    NOT_FOUND,
    NOT_EQUALS,
    EQUALS;

    static SchemaCheckStatus getMoreSignificant(SchemaCheckStatus s1, SchemaCheckStatus s2) {
      return s1.compareTo(s2) <= 0 ? s1 : s2;
    }
  }

  private SchemaCheckStatus checkSchema(IMessageClient producer, String topic,
      Environment environment, String json, SubjectType type) {

    boolean isKey = type == SubjectType.key;
    json = getJsonFromTemplate(json, type.name());

    String registeredSchema;
    try {
      registeredSchema =
          schemaRegistryService
              .getLatestTopicSchema(topic, type, environment);
    } catch (Exception e) {
      enqueueError(
          "Error obteniendo esquema del " + type + " del topic " + topic
              + " desde el Schema Registry");
      enqueueInfo(e.getMessage());
      if (e instanceof NotFound) {
        return SchemaCheckStatus.NOT_FOUND;
      }
      return SchemaCheckStatus.NOT_CHECKED;
    }

    String avroSchema;
    try {
      avroSchema = isKey ? producer.getKeySchema(json) : producer.getValueSchema(json);
    } catch (Exception e) {
      enqueueError("Error obteniendo esquema AVRO de " +
          (isKey ? producer.getKeyClassName() : producer.getValueClassName()));
      enqueueInfo(e.getMessage());
      return SchemaCheckStatus.NOT_CHECKED;
    }

    enqueueInfo("Comparando con el esquema AVRO de " +
        (isKey ? producer.getKeyClassName() : producer.getValueClassName()));
    return compareSchemas(registeredSchema, avroSchema, type.name());
  }

  private SchemaCheckStatus compareSchemas(String registeredSchema, String avroSchema,
      String objectName) {
    if (avroSchema == null) {
      enqueueError("El esquema del AVRO del " + objectName + " es null");
      return SchemaCheckStatus.NOT_CHECKED;
    }

    if (registeredSchema == null) {
      enqueueError("El esquema del " + objectName + " registrado es null");
      return SchemaCheckStatus.NOT_CHECKED;
    }

    if (!registeredSchema.equals(avroSchema)) {
      enqueueError("Los esquemas del " + objectName + " no coinciden");
      enqueueText(" ", Type.ADDED);
      enqueueInfo(" AVRO");
      enqueueText(" ", Type.DELETED);
      enqueueInfo(" Schema Registry");
      enqueueTextDifferences(JsonUtils.formatJson(registeredSchema),
          JsonUtils.formatJson(avroSchema));
      return SchemaCheckStatus.NOT_EQUALS;
    } else {
      enqueueSuccessful("El esquema del " + objectName + " es correcto");
      enqueueInfo(avroSchema);
      return SchemaCheckStatus.EQUALS;
    }
  }

  private void createUIComponents() {
    valueEditor = createJsonEditor();
    valueScrollPane = createEditorScroll(valueEditor);
    keyEditor = createJsonEditor();
    keyScrollPane = createEditorScroll(keyEditor);
    variablesEditor = createPropertiesEditor();
    variablesScrollPane = createEditorScroll(variablesEditor);
  }

  @Override
  protected void enableButtons(boolean enable) {
    buttonSend.setEnabled(enable);
    buttonCompareSchemas.setEnabled(enable);
  }

  @Override
  protected Optional<JTextComponent> getCurrentEditor() {
    int index = tabbedPane.getSelectedIndex();
    return getUmpteenthEditor(index, infoTextPane, keyEditor, valueEditor, variablesEditor);
  }

  @Override
  protected void showConnectionStatus(Boolean ok) {
    buttonCheckEnvironment.setIcon(ok == null ? iconCheckUndefined : (ok ? iconCheckOk : iconCheckFail));
  }

  @Override
  protected Environment getEnvironment() {
    return (Environment) comboEnvironment.getSelectedItem();
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
    panel1.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
    buttonCompareSchemas = new JButton();
    buttonCompareSchemas.setIcon(new ImageIcon(getClass().getResource("/images/compare.png")));
    buttonCompareSchemas.setText("Comparar esquemas");
    panel1.add(buttonCompareSchemas,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer1 = new Spacer();
    panel1.add(spacer1, new GridConstraints(0, 4, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    buttonSend = new JButton();
    buttonSend.setIcon(new ImageIcon(getClass().getResource("/images/enviar.png")));
    buttonSend.setText("Enviar");
    panel1.add(buttonSend,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    quantityComboBox = new JComboBox();
    quantityComboBox.setEditable(true);
    quantityComboBox.setToolTipText("Cantidad de eventos a enviar");
    panel1.add(quantityComboBox,
        new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    dangerLabel = new JLabel();
    dangerLabel.setIcon(new ImageIcon(getClass().getResource("/images/danger.png")));
    dangerLabel.setText("");
    dangerLabel.setToolTipText("Cuidado, no estas en el entorno local");
    panel1.add(dangerLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new GridLayoutManager(6, 3, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel2,
        new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(615, 121), null, 0, false));
    comboTopic = new JComboBox();
    comboTopic.setEditable(true);
    final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
    comboTopic.setModel(defaultComboBoxModel1);
    panel2.add(comboTopic, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label1 = new JLabel();
    label1.setText("Topic:");
    panel2.add(label1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(137, 16), null, 0,
        false));
    final JLabel label2 = new JLabel();
    label2.setText("Value:");
    panel2.add(label2, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboValue = new JComboBox();
    final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
    comboValue.setModel(defaultComboBoxModel2);
    panel2.add(comboValue, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label3 = new JLabel();
    label3.setText("Entorno:");
    panel2.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label4 = new JLabel();
    label4.setText("Producer:");
    panel2.add(label4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboEnvironment = new JComboBox();
    final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
    comboEnvironment.setModel(defaultComboBoxModel3);
    panel2.add(comboEnvironment,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboProducer = new JComboBox();
    comboProducer.setEditable(false);
    final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
    comboProducer.setModel(defaultComboBoxModel4);
    panel2.add(comboProducer,
        new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    buttonOpenFileValue = new JButton();
    Font buttonOpenFileValueFont = this.$$$getFont$$$(null, -1, -1, buttonOpenFileValue.getFont());
    if (buttonOpenFileValueFont != null) {
      buttonOpenFileValue.setFont(buttonOpenFileValueFont);
    }
    buttonOpenFileValue.setIcon(new ImageIcon(getClass().getResource("/images/folder.png")));
    buttonOpenFileValue.setText("");
    buttonOpenFileValue.setToolTipText("Abrir archivo del value");
    panel2.add(buttonOpenFileValue,
        new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(24, 24),
            new Dimension(24, 24), new Dimension(24, 24), 0, false));
    final JLabel label5 = new JLabel();
    label5.setText("Key:");
    panel2.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboKey = new JComboBox();
    panel2.add(comboKey, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    buttonOpenFileKey = new JButton();
    Font buttonOpenFileKeyFont = this.$$$getFont$$$(null, -1, -1, buttonOpenFileKey.getFont());
    if (buttonOpenFileKeyFont != null) {
      buttonOpenFileKey.setFont(buttonOpenFileKeyFont);
    }
    buttonOpenFileKey.setIcon(new ImageIcon(getClass().getResource("/images/folder.png")));
    buttonOpenFileKey.setText("");
    buttonOpenFileKey.setToolTipText("Abrir archivo del key");
    panel2.add(buttonOpenFileKey,
        new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(24, 24),
            new Dimension(24, 24), new Dimension(24, 24), 0, false));
    final JLabel label6 = new JLabel();
    label6.setText("Dominio:");
    panel2.add(label6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboDomain = new JComboBox();
    panel2.add(comboDomain,
        new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    buttonFindTopic = new JButton();
    Font buttonFindTopicFont = this.$$$getFont$$$(null, -1, -1, buttonFindTopic.getFont());
    if (buttonFindTopicFont != null) {
      buttonFindTopic.setFont(buttonFindTopicFont);
    }
    buttonFindTopic.setIcon(new ImageIcon(getClass().getResource("/images/glasses.png")));
    buttonFindTopic.setText("");
    buttonFindTopic.setToolTipText("Buscar en todos los topics");
    panel2.add(buttonFindTopic,
        new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(24, 24),
            new Dimension(24, 24), new Dimension(24, 24), 0, false));
    buttonCheckEnvironment = new JButton();
    Font buttonCheckEnvironmentFont = this.$$$getFont$$$(null, -1, -1, buttonCheckEnvironment.getFont());
    if (buttonCheckEnvironmentFont != null) {
      buttonCheckEnvironment.setFont(buttonCheckEnvironmentFont);
    }
    buttonCheckEnvironment.setIcon(new ImageIcon(getClass().getResource("/images/check_grey.png")));
    buttonCheckEnvironment.setText("");
    buttonCheckEnvironment.setToolTipText("Comprobar conexión");
    panel2.add(buttonCheckEnvironment,
        new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(24, 24),
            new Dimension(24, 24), new Dimension(24, 24), 0, false));
    final JPanel panel3 = new JPanel();
    panel3.setLayout(new BorderLayout(0, 0));
    contentPane.add(panel3, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, null, null, 0, false));
    final JLabel label7 = new JLabel();
    label7.setIcon(new ImageIcon(getClass().getResource("/images/search.png")));
    label7.setText("");
    panel3.add(label7, BorderLayout.WEST);
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
    tabbedPane.addTab("Información", tabInfo);
    final JScrollPane scrollPane1 = new JScrollPane();
    tabInfo.add(scrollPane1, BorderLayout.CENTER);
    infoTextPane = new JTextPane();
    infoTextPane.setBackground(new Color(-16777216));
    infoTextPane.setCaretColor(new Color(-1));
    infoTextPane.setEditable(false);
    Font infoTextPaneFont = this.$$$getFont$$$("Consolas", -1, 12, infoTextPane.getFont());
    if (infoTextPaneFont != null) {
      infoTextPane.setFont(infoTextPaneFont);
    }
    infoTextPane.setForeground(new Color(-1));
    infoTextPane.putClientProperty("charset", "");
    scrollPane1.setViewportView(infoTextPane);
    tabKey = new JPanel();
    tabKey.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab("Key", tabKey);
    tabKey.add(keyScrollPane, BorderLayout.CENTER);
    tabValue = new JPanel();
    tabValue.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab("Value", tabValue);
    tabValue.add(valueScrollPane, BorderLayout.CENTER);
    final JPanel panel5 = new JPanel();
    panel5.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab("Variables", panel5);
    panel5.add(variablesScrollPane, BorderLayout.CENTER);
    final JPanel panel6 = new JPanel();
    panel6.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
    panel4.add(panel6, BorderLayout.EAST);
    cleanButton = new JButton();
    cleanButton.setIcon(new ImageIcon(getClass().getResource("/images/rubber.png")));
    cleanButton.setText("");
    cleanButton.setToolTipText("Limpiar");
    panel6.add(cleanButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    copyButton = new JButton();
    copyButton.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
    copyButton.setText("");
    copyButton.setToolTipText("Copiar al portapapeles");
    panel6.add(copyButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, null, null, 0, false));
    final Spacer spacer2 = new Spacer();
    panel6.add(spacer2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    final Spacer spacer3 = new Spacer();
    panel6.add(spacer3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, 1,
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

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return contentPane;
  }

}

