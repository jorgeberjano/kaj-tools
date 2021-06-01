package es.jbp.kajtools;

import com.google.common.collect.Maps;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import es.jbp.kajtools.InfoMessage.Type;
import es.jbp.kajtools.util.JsonUtils;
import es.jbp.kajtools.util.ResourceUtil;
import es.jbp.kajtools.util.SchemaRegistryService;
import es.jbp.kajtools.util.TemplateExecutor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
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

public class KafkaTestPanel extends BasePanel {

  private static final int MAXIMUM_QUANTITY = 10;

  private final SchemaRegistryService schemaRegistryService;
  private String currentDirectory;
  private final Map<String, Object> templateVariables = new HashMap<>();
  private int counter;
  private final Map<String, SchemaCheckStatus> checkedSchemaTopics = new HashMap<>();

  @Getter
  private JPanel contentPane;
  private JButton buttonSend;
  private JComboBox comboTopic;
  private JComboBox comboEvent;
  private JComboBox comboKey;
  private JComboBox comboEnvironment;
  private JButton buttonCompareSchemas;
  private JTabbedPane tabbedPane;
  private JPanel tabInfo;
  private JPanel tabEvent;
  private JPanel tabKey;
  private RTextScrollPane eventScrollPane;
  private RTextScrollPane keyScrollPane;
  @Getter
  private JTextPane infoTextPane;
  private JComboBox comboProducer;
  private JButton buttonOpenFileEvent;
  private JButton buttonOpenFileKey;
  private JButton cleanButton;
  private JButton copyButton;
  private JComboBox quantityComboBox;
  private JTextField searchTextField;
  private JLabel dangerLabel;
  private JComboBox comboDomain;
  private RSyntaxTextArea jsonEditorEvent;
  private RSyntaxTextArea jsonEditorKey;

  public KafkaTestPanel() {

    $$$setupUI$$$();

    Properties properties = new Properties();
    try {
      properties.load(ResourceUtil.getResourceStream("jsontemplate/variables.properties"));
    } catch (IOException e) {
    }
    templateVariables.putAll(Maps.fromProperties(properties));
    templateVariables.put("i", 1);

    currentDirectory = new File(System.getProperty("user.home")).getPath();
    this.schemaRegistryService = KajToolsApp.getInstance().getSchemaRegistryService();

    dangerLabel.setVisible(false);

    buttonSend.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        asyncSendEvent();
      }
    });

    // Combo Entorno
    EnvironmentConfiguration.ENVIRONMENT_LIST.stream().forEach(comboEnvironment::addItem);
    comboEnvironment.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean local = ((Environment) comboEnvironment.getSelectedItem()).getName()
            .contains("local");
        dangerLabel.setVisible(!local);
      }
    });

    // Combo Dominio
    final List<TestProducer> producerList = KajToolsApp.getInstance().getProducerList();
    producerList.stream().map(TestProducer::getDomain).distinct().forEach(comboDomain::addItem);
    comboDomain.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateProducers();
      }
    });

    // Combo Productores
    comboProducer.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateTopicsEventsAndKeys();
      }
    });
    updateProducers();

    // Combos Topics, Events y Keys
    updateTopicsEventsAndKeys();

    buttonCompareSchemas.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        asyncCheckSchema();
      }
    });
    comboKey.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        loadResourceForKey();
      }
    });
    comboEvent.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        loadResourceForEvent();
      }
    });
    buttonOpenFileKey.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openFileForKey();
      }
    });
    buttonOpenFileEvent.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openFileForEvent();
      }
    });
    cleanButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cleanEditor();
      }
    });
    copyButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        copyToClipboard();
      }
    });

    IntStream.rangeClosed(1, MAXIMUM_QUANTITY).forEach(quantityComboBox::addItem);

    enableTextSearch(searchTextField, jsonEditorEvent, jsonEditorKey);
  }

  private void updateProducers() {
    comboProducer.removeAllItems();
    String domain = comboDomain.getSelectedItem().toString();
    final List<TestProducer> producerList = KajToolsApp.getInstance().getProducerList();
    producerList.stream()
        .filter(p -> StringUtils.isBlank(domain) || domain.equals(p.getDomain()))
        .forEach(comboProducer::addItem);
  }

  private void updateTopicsEventsAndKeys() {
    comboTopic.removeAllItems();
    comboEvent.removeAllItems();
    comboKey.removeAllItems();
    TestProducer producer = (TestProducer) comboProducer.getSelectedItem();
    if (producer == null) {
      return;
    }
    producer.getAvailableTopics().forEach(comboTopic::addItem);
    comboTopic.setSelectedItem(producer.getDefaultTopic());

    producer.getAvailableEvents().stream()
        .sorted(new JsonFirstComparator())
        .forEach(comboEvent::addItem);

    producer.getAvailableKeys().stream()
        .sorted(new JsonFirstComparator())
        .forEach(comboKey::addItem);

    loadResourceForKey();
    loadResourceForEvent();
  }

  private void openFileForKey() {
    File file = chooseAndReadFile();
    if (file != null) {
      loadJsonFromFile(file, jsonEditorKey);
    }
  }

  private void openFileForEvent() {
    File file = chooseAndReadFile();
    if (file != null) {
      loadJsonFromFile(file, jsonEditorEvent);
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
    loadJsonFromResource(path, jsonEditorKey);
  }

  private void loadResourceForEvent() {
    String path = Optional.ofNullable(comboEvent.getSelectedItem()).map(Object::toString)
        .orElse("");
    loadJsonFromResource(path, jsonEditorEvent);
  }

  private void loadJsonFromResource(String path, RSyntaxTextArea jsonEditor) {
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
    Environment environment = (Environment) comboEnvironment.getSelectedItem();
    TestProducer producer = (TestProducer) comboProducer.getSelectedItem();
    String topic = comboTopic.getEditor().getItem().toString();

    SchemaCheckStatus status = checkedSchemaTopics.get(topic);
    boolean local = environment.getName().toLowerCase().contains("local");
    if (!local && status != SchemaCheckStatus.EQUALS) {
      boolean userAccepts = warnUserAboutCheckSchema(status, topic);
      if (!userAccepts) {
        return;
      }
    }

    printAction("Enviando evento a " + topic);
    String key = jsonEditorKey.getText();
    String event = jsonEditorEvent.getText();
    int quantity = (int) quantityComboBox.getSelectedItem();
    executeAsyncTask(() -> sendEvent(environment, producer, topic, key, event, quantity));
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

  private Void sendEvent(Environment environment, TestProducer producer, String topic, String key,
      String event, int quantity) {
    for (int i = 1; i <= quantity; i++) {
      templateVariables.put("i", i);
      templateVariables.put("counter", ++counter);
      sendEvent(environment, producer, topic, key, event);
    }
    return null;
  }

  private void sendEvent(Environment environment, TestProducer producer, String topic, String key,
      String event) {
    TemplateExecutor templateExecutor = new TemplateExecutor(templateVariables);
    String jsonKey = getJson(templateExecutor, key, "KEY");
    String jsonEvent = getJson(templateExecutor, event, "EVENT");

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
    } catch (Exception ex) {
      enqueueError("Error enviando un evento al topic");
      enqueueInfo(ex.getMessage());
      enqueueInfo("Causa: " + ex.getCause().getMessage());
      return;
    }
    enqueueSuccessful("Enviado el evento correctamente");
  }

  private String getJson(TemplateExecutor templateExecutor, String json, String name) {

    if (!JsonUtils.isTemplate(json)) {
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
    TestProducer producer = (TestProducer) comboProducer.getSelectedItem();
    if (producer instanceof GenericTestProducer) {
      printError(
          "No es posible comparar los esquemas con el " + producer.getClass().getSimpleName());
      return;
    }
    String topic = comboTopic.getEditor().getItem().toString();
    printAction("Comprobando esquemas del topic " + topic);

    Environment environment = (Environment) comboEnvironment.getSelectedItem();

    executeAsyncTask(() -> checkSchema(producer, topic, environment));
  }

  private Void checkSchema(TestProducer producer, String topic, Environment environment) {
    SchemaCheckStatus keySchemaOk = checkSchema(producer, topic, environment, true);
    SchemaCheckStatus eventSchemaOk = checkSchema(producer, topic, environment, false);

    if (keySchemaOk != null && eventSchemaOk != null) {
      checkedSchemaTopics
          .put(topic, SchemaCheckStatus.getMoreSignificant(keySchemaOk, eventSchemaOk));
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

  private SchemaCheckStatus checkSchema(TestProducer producer, String topic,
      Environment environment,
      boolean isKey) {
    String registeredSchema, avroSchema;
    String objectName = isKey ? "Key" : "Event";

    try {
      registeredSchema =
          schemaRegistryService
              .getLatestTopicSchema(topic, isKey, environment);
    } catch (Exception e) {
      enqueueError(
          "Error obteniendo esquema del " + objectName + " del topic " + topic
              + " desde el Schema Registry");
      enqueueInfo(e.getMessage());
      if (e instanceof NotFound) {
        return SchemaCheckStatus.NOT_FOUND;
      }
      return SchemaCheckStatus.NOT_CHECKED;
    }

    try {
      avroSchema = isKey ? producer.getKeySchema(jsonEditorKey.getText())
          : producer.getEventSchema(jsonEditorEvent.getText());
    } catch (Exception e) {
      enqueueError("Error obteniendo esquema AVRO de " +
          (isKey ? producer.getKeyClassName() : producer.getEventClassName()));
      enqueueInfo(e.getMessage());
      return SchemaCheckStatus.NOT_CHECKED;
    }

    enqueueInfo("Comparando con el esquema AVRO de " +
        (isKey ? producer.getKeyClassName() : producer.getEventClassName()));
    return compareSchemas(registeredSchema, avroSchema, objectName);
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
    jsonEditorEvent = createJsonEditor();
    eventScrollPane = createEditorScroll(jsonEditorEvent);
    jsonEditorKey = createJsonEditor();
    keyScrollPane = createEditorScroll(jsonEditorKey);
  }

  @Override
  protected void enableButtons(boolean enable) {
    buttonSend.setEnabled(enable);
    buttonCompareSchemas.setEnabled(enable);
  }

  @Override
  protected Optional<JTextComponent> getCurrentEditor() {
    int index = tabbedPane.getSelectedIndex();
    if (index == 0) {
      return Optional.of(infoTextPane);
    } else if (index == 1) {
      return Optional.of(jsonEditorKey);
    } else if (index == 2) {
      return Optional.of(jsonEditorEvent);
    } else {
      return Optional.empty();
    }
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
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(1, 7, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel1,
        new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null,
            null, null, 0, false));
    buttonCompareSchemas = new JButton();
    buttonCompareSchemas.setIcon(new ImageIcon(getClass().getResource("/images/compare.png")));
    buttonCompareSchemas.setText("Comparar esquemas");
    panel1.add(buttonCompareSchemas, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER,
        GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer1 = new Spacer();
    panel1.add(spacer1, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER,
        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null,
        0, false));
    buttonSend = new JButton();
    buttonSend.setIcon(new ImageIcon(getClass().getResource("/images/enviar.png")));
    buttonSend.setText("Enviar");
    panel1.add(buttonSend, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER,
        GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    cleanButton = new JButton();
    cleanButton.setIcon(new ImageIcon(getClass().getResource("/images/rubber.png")));
    cleanButton.setText("");
    cleanButton.setToolTipText("Limpiar");
    panel1.add(cleanButton,
        new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
            false));
    copyButton = new JButton();
    copyButton.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
    copyButton.setText("");
    copyButton.setToolTipText("Copiar al portapapeles");
    panel1.add(copyButton, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER,
        GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    quantityComboBox = new JComboBox();
    quantityComboBox.setEditable(true);
    quantityComboBox.setToolTipText("Cantidad de eventos a enviar");
    panel1.add(quantityComboBox, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST,
        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    dangerLabel = new JLabel();
    dangerLabel.setIcon(new ImageIcon(getClass().getResource("/images/danger.png")));
    dangerLabel.setText("");
    dangerLabel.setToolTipText("Cuidado, no estas en el entorno local");
    panel1.add(dangerLabel,
        new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
            false));
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new GridLayoutManager(6, 3, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH,
        GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(615, 121), null, 0, false));
    comboTopic = new JComboBox();
    comboTopic.setEditable(true);
    final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
    comboTopic.setModel(defaultComboBoxModel1);
    panel2.add(comboTopic, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST,
        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label1 = new JLabel();
    label1.setText("Topic:");
    panel2.add(label1,
        new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null,
            new Dimension(137, 16), null, 0, false));
    final JLabel label2 = new JLabel();
    label2.setText("Event:");
    panel2.add(label2,
        new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
            false));
    comboEvent = new JComboBox();
    final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
    comboEvent.setModel(defaultComboBoxModel2);
    panel2.add(comboEvent, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST,
        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label3 = new JLabel();
    label3.setText("Entorno:");
    panel2.add(label3,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
            false));
    final JLabel label4 = new JLabel();
    label4.setText("Producer:");
    panel2.add(label4,
        new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
            false));
    comboEnvironment = new JComboBox();
    final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
    comboEnvironment.setModel(defaultComboBoxModel3);
    panel2.add(comboEnvironment, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST,
        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboProducer = new JComboBox();
    comboProducer.setEditable(false);
    final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
    comboProducer.setModel(defaultComboBoxModel4);
    panel2.add(comboProducer, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST,
        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    buttonOpenFileEvent = new JButton();
    Font buttonOpenFileEventFont = this.$$$getFont$$$(null, -1, -1, buttonOpenFileEvent.getFont());
    if (buttonOpenFileEventFont != null) {
      buttonOpenFileEvent.setFont(buttonOpenFileEventFont);
    }
    buttonOpenFileEvent.setIcon(new ImageIcon(getClass().getResource("/images/folder.png")));
    buttonOpenFileEvent.setText("");
    buttonOpenFileEvent.setToolTipText("Abrir archivo event");
    panel2.add(buttonOpenFileEvent,
        new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED,
            new Dimension(24, 24), new Dimension(24, 24), new Dimension(24, 24), 0, false));
    final JLabel label5 = new JLabel();
    label5.setText("Key:");
    panel2.add(label5,
        new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
            false));
    comboKey = new JComboBox();
    panel2.add(comboKey, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST,
        GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    buttonOpenFileKey = new JButton();
    Font buttonOpenFileKeyFont = this.$$$getFont$$$(null, -1, -1, buttonOpenFileKey.getFont());
    if (buttonOpenFileKeyFont != null) {
      buttonOpenFileKey.setFont(buttonOpenFileKeyFont);
    }
    buttonOpenFileKey.setIcon(new ImageIcon(getClass().getResource("/images/folder.png")));
    buttonOpenFileKey.setText("");
    buttonOpenFileKey.setToolTipText("Abrir archivo key");
    panel2.add(buttonOpenFileKey,
        new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED,
            new Dimension(24, 24), new Dimension(24, 24), new Dimension(24, 24), 0, false));
    final JLabel label6 = new JLabel();
    label6.setText("Dominio:");
    panel2.add(label6,
        new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0,
            false));
    comboDomain = new JComboBox();
    panel2.add(comboDomain, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST,
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
    tabEvent = new JPanel();
    tabEvent.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab("Event", tabEvent);
    tabEvent.add(eventScrollPane, BorderLayout.CENTER);
    final JPanel panel3 = new JPanel();
    panel3.setLayout(new BorderLayout(0, 0));
    contentPane.add(panel3,
        new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label7 = new JLabel();
    label7.setIcon(new ImageIcon(getClass().getResource("/images/search.png")));
    label7.setText("");
    panel3.add(label7, BorderLayout.WEST);
    searchTextField = new JTextField();
    searchTextField.setText("");
    panel3.add(searchTextField, BorderLayout.CENTER);
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
    return fontWithFallback instanceof FontUIResource ? fontWithFallback
        : new FontUIResource(fontWithFallback);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return contentPane;
  }

}
