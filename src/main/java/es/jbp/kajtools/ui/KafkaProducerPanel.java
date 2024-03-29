package es.jbp.kajtools.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import es.jbp.kajtools.Environment;
import es.jbp.kajtools.IMessageClient;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.configuration.Configuration;
import es.jbp.kajtools.i18n.I18nService;
import es.jbp.kajtools.kafka.GenericClient;
import es.jbp.kajtools.kafka.KafkaAdminService;
import es.jbp.kajtools.kafka.TopicItem;
import es.jbp.kajtools.schemaregistry.ISchemaRegistryService;
import es.jbp.kajtools.schemaregistry.ISchemaRegistryService.SubjectType;
import es.jbp.kajtools.ui.InfoDocument.Type;
import es.jbp.kajtools.ui.interfaces.InfoReportable;
import es.jbp.kajtools.util.JsonUtils;
import es.jbp.kajtools.util.ResourceUtil;
import es.jbp.kajtools.util.TemplateExecutor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.IntStream;
import javax.swing.AbstractButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleContext;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class KafkaProducerPanel extends KafkaBasePanel {

    private static final int MAXIMUM_QUANTITY = 10;

    private final ISchemaRegistryService schemaRegistryService;

    private String currentDirectory;
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
    private InfoTextPane infoTextPane;
    private JComboBox comboProducer;
    private JButton buttonOpenFileValue;
    private JButton buttonOpenFileKey;
    private JButton cleanButton;
    private JButton copyButton;
    private JComboBox quantityComboBox;
    @Getter
    private JTextField searchTextField;
    private JLabel dangerLabel;
    private JComboBox comboDomain;
    private JButton buttonFindTopic;
    private JButton buttonCheckEnvironment;
    private RTextScrollPane valueScrollPane;
    private RTextScrollPane keyScrollPane;
    private RTextScrollPane variablesScrollPane;
    private JPanel tabHeaders;
    private RTextScrollPane headersScrollPane;
    private JComboBox comboHeaders;
    private JButton buttonOpenFileHeaders;
    private RSyntaxTextArea valueEditor;
    private RSyntaxTextArea keyEditor;
    private RSyntaxTextArea variablesEditor;
    private RSyntaxTextArea headersEditor;

    private String globalHeaders;

    public KafkaProducerPanel(List<IMessageClient> clientList,
                              ISchemaRegistryService schemaRegistryService,
                              KafkaAdminService kafkaAdmin,
                              UiComponentCreator componentFactory) {
        super(clientList, schemaRegistryService, kafkaAdmin, componentFactory);
        this.schemaRegistryService = schemaRegistryService;

        $$$setupUI$$$();

        super.initialize();

        globalHeaders = ResourceUtil.readResourceString("headers.properties");
        variablesEditor.setText(ResourceUtil.readResourceString("variables.properties"));

        currentDirectory = new File(System.getProperty("user.home")).getPath();

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
        clientList.stream().map(IMessageClient::getDomain).distinct().forEach(comboDomain::addItem);
        comboDomain.addActionListener(e -> updateProducers());

        // Combo Productores
        comboProducer.addActionListener(e -> updateCombosDependingOnProducer());
        updateProducers();

        updateCombosDependingOnProducer();

        buttonFindTopic.addActionListener(e -> findTopic());

        buttonCompareSchemas.addActionListener(e -> asyncCheckSchema());

        comboKey.addActionListener(e -> loadResourceForKey());
        buttonOpenFileKey.addActionListener(e -> openFileForKey());

        comboValue.addActionListener(e -> loadResourceForValue());
        buttonOpenFileValue.addActionListener(e -> openFileForValue());

        comboHeaders.addActionListener(e -> loadResourceForHeaders());
        buttonOpenFileHeaders.addActionListener(e -> openFileForHeaders());

        cleanButton.addActionListener(e -> cleanEditor());
        copyButton.addActionListener(e -> copyToClipboard());

        IntStream.rangeClosed(1, MAXIMUM_QUANTITY).forEach(quantityComboBox::addItem);

        enableTextSearch(searchTextField, valueEditor, keyEditor, headersEditor, variablesEditor);
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
        clientList.stream()
                .filter(p -> StringUtils.isBlank(domain) || domain.equals(p.getDomain()) || p instanceof GenericClient)
                .forEach(comboProducer::addItem);
    }

    private void updateCombosDependingOnProducer() {
        comboTopic.removeAllItems();
        comboValue.removeAllItems();
        comboKey.removeAllItems();
        comboHeaders.removeAllItems();

        IMessageClient producer = (IMessageClient) comboProducer.getSelectedItem();
        if (producer == null) {
            return;
        }
        producer.getAvailableTopics().forEach(comboTopic::addItem);
        comboTopic.setSelectedItem(producer.getDefaultTopic());

        producer.getAvailableValues().forEach(comboValue::addItem);

        producer.getAvailableKeys().forEach(comboKey::addItem);

        comboHeaders.addItem("");
        producer.getAvailableHeaders()
                .forEach(comboHeaders::addItem);

        loadResourceForKey();
        loadResourceForValue();
    }

    private void openFileForKey() {
        File file = chooseAndReadFile();
        if (file != null) {
            loadTextFromFile(file, keyEditor);
        }
    }

    private void openFileForValue() {
        File file = chooseAndReadFile();
        if (file != null) {
            loadTextFromFile(file, valueEditor);
        }
    }

    private void openFileForHeaders() {
        File file = chooseAndReadFile();
        if (file != null) {
            loadTextFromFile(file, headersEditor);
        }
    }

    private void loadResourceForKey() {
        String path = Optional.ofNullable(comboKey.getSelectedItem()).map(Object::toString).orElse("");
        loadTextFromResource(path, keyEditor);
    }

    private void loadResourceForValue() {
        String path = Optional.ofNullable(comboValue.getSelectedItem()).map(Object::toString).orElse("");
        loadTextFromResource(path, valueEditor);
    }

    private void loadResourceForHeaders() {
        String path = Optional.ofNullable(comboHeaders.getSelectedItem()).map(Object::toString)
                .orElse("");
        if (StringUtils.isBlank(path)) {
            headersEditor.setText(globalHeaders);
            headersEditor.setCaretPosition(0);
        }
        loadTextFromResource(path, headersEditor);
    }

    private void asyncSendEvent() {
        Environment environment = getEnvironment();
        IMessageClient producer = (IMessageClient) comboProducer.getSelectedItem();
        String topic = comboTopic.getEditor().getItem().toString();

        SchemaCheckStatus status = checkedSchemaTopics.get(environment.getName() + "$" + topic);
        boolean local = environment.getName().toLowerCase().contains("local");
        if (!local && !(producer instanceof GenericClient) && status != SchemaCheckStatus.EQUALS) {
            boolean userAccepts = warnUserAboutCheckSchema(environment, status, topic);
            if (!userAccepts) {
                return;
            }
        }

        printMessage(InfoReportable.buildActionMessage("Enviando evento a " + topic));
        String key = keyEditor.getText();
        String event = valueEditor.getText();
        String headers = headersEditor.getText();
        templateExecutor.addVariables(createVariableMap(variablesEditor.getText()));

        int quantity = Optional.ofNullable(quantityComboBox.getSelectedItem())
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElse(1);

        buttonSend.setEnabled(false);
        buttonCompareSchemas.setEnabled(false);

        executeAsyncTask(() -> sendMessage(environment, producer, topic, key, event, headers, quantity));
    }

    private boolean warnUserAboutCheckSchema(Environment environment, SchemaCheckStatus status, String topic) {
        String message;
        if (status == SchemaCheckStatus.NOT_FOUND) {
            message = "No hay ningún esquema en el Schema Registry.";
        } else if (status == SchemaCheckStatus.NOT_EQUALS) {
            message = "El esquema del AVRO no coincide con el del Schema Registry";
        } else if (status == SchemaCheckStatus.NOT_CHECKED) {
            message = "No se pudo comprobar el esquema. No debería enviar un evento al entorno " + environment.getName() + " sin antes comprobarlo";
        } else {
            JOptionPane.showMessageDialog(contentPane,
                    "Topic: " + topic + "\n" +
                            "Antes de enviar un mensaje debe comprobar el esquema del topic",
                    "Atención", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (environment.isAutoRegisterSchemas()) {
            int response = JOptionPane.showConfirmDialog(contentPane,
                    "Topic: " + topic + "\n" + message + "\n" +
                            "Si envía el mensaje se actualizará el esquema en el Schema Registry.\n" +
                            "¿Desea enviar el mensaje de todas formas?",
                    "Atención", JOptionPane.YES_NO_OPTION);
            return response == JOptionPane.YES_OPTION;
        }
        return true;
    }

    private Void sendMessage(Environment environment, IMessageClient producer,
                             String topic, String key, String value, String headers, int quantity) {

        templateExecutor.resetIndexCounter();
        for (int i = 1; i <= quantity; i++) {
            sendMessage(environment, producer, topic, key, value, headers);
            templateExecutor.avanceCounters();
        }
        return null;
    }

    private void sendMessage(Environment environment, IMessageClient producer, String topic,
                             String key, String value, String headers) {

        if (StringUtils.isBlank(topic)) {
            enqueueMessage(InfoReportable.buildErrorMessage(
                    "No se va a mandar el mensaje porque no se ha indicado ningún topic"));
            return;
        }

        key = processTemplate(key, Type.JSON, "key");
        value = processTemplate(value, Type.JSON, "value");
        headers = processTemplate(headers, Type.PROPERTIES, "headers");

        if (StringUtils.isBlank(key)) {
            enqueueMessage(InfoReportable.buildErrorMessage(
                    "No se va a mandar el mensaje porque no se ha indicado ninguna key"));
            return;
        }
        if (StringUtils.isBlank(value)) {
            enqueueMessage(InfoReportable.buildErrorMessage(
                    "No se va a mandar el mensaje porque no se ha indicado ningún value"));
            return;
        }

        try {
            producer.sendFromJson(environment, topic, key, value, headers);
        } catch (KajException ex) {
            enqueueException(ex);
            return;
        }
        enqueueMessage(InfoReportable.buildSuccessfulMessage("Enviado el evento correctamente"));
    }

    private String processTemplate(String text, Type type, String name) {

        if (!TemplateExecutor.containsTemplateExpressions(text)) {
            return text;
        }

        String generatedText;
        try {
            generatedText = templateExecutor.processTemplate(text);
        } catch (Exception ex) {
            enqueueMessage(InfoReportable.buildErrorMessage(
                    "No se ha podido generar el JSON del EVENT a partir de la plantilla"));
            enqueueException(ex);
            return null;
        }
        enqueueLink(InfoDocument.simpleDocument("generated " + name, type, generatedText));
        return generatedText;
    }

    private void asyncCheckSchema() {
        IMessageClient producer = (IMessageClient) comboProducer.getSelectedItem();
        if (producer instanceof GenericClient) {
            printMessage(InfoReportable.buildErrorMessage(
                    "No es posible comparar los esquemas con el GenericClient"));
            return;
        }
        String topic = comboTopic.getEditor().getItem().toString();
        printMessage(InfoReportable.buildActionMessage("Comprobando esquemas del topic " + topic));

        String jsonKey = keyEditor.getText();
        String jsonValue = valueEditor.getText();
        templateExecutor.addVariables(createVariableMap(variablesEditor.getText()));

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
        json = processTemplate(json, Type.JSON, type.name());

        String registeredSchema;
        try {
            registeredSchema = schemaRegistryService.getLatestTopicSchema(topic, type, environment);
        } catch (KajException e) {
            enqueueException(e);
            return SchemaCheckStatus.NOT_CHECKED;
        }
        if (registeredSchema == null) {
            return SchemaCheckStatus.NOT_FOUND;
        }

        String avroSchema;
        try {
            avroSchema = isKey ? producer.getKeySchema(json) : producer.getValueSchema(json);
        } catch (KajException e) {
            enqueueException(e);
            return SchemaCheckStatus.NOT_CHECKED;
        }

        enqueueMessage(InfoReportable.buildTraceMessage("Comparando con el esquema AVRO de " +
                (isKey ? producer.getKeyClassName() : producer.getValueClassName())));
        return compareSchemas(registeredSchema, avroSchema, type.name());
    }

    private SchemaCheckStatus compareSchemas(String registeredSchema, String avroSchema,
                                             String objectName) {
        if (avroSchema == null) {
            enqueueMessage(InfoReportable.buildErrorMessage("El esquema del AVRO del " + objectName + " es null"));
            return SchemaCheckStatus.NOT_CHECKED;
        }

        if (registeredSchema == null) {
            enqueueMessage(InfoReportable.buildErrorMessage("El esquema del " + objectName + " registrado es null"));
            return SchemaCheckStatus.NOT_CHECKED;
        }
        registeredSchema = JsonUtils.formatJson(registeredSchema);
        avroSchema = JsonUtils.formatJson(avroSchema);
        if (!registeredSchema.equals(avroSchema)) {
            enqueueMessage(InfoReportable.buildErrorMessage("Los esquemas del " + objectName + " no coinciden"));
            enqueueTextDifferences("AVRO", avroSchema, "Schema Registry", registeredSchema);

            return SchemaCheckStatus.NOT_EQUALS;
        } else {
            enqueueMessage(InfoReportable.buildSuccessfulMessage("El esquema del " + objectName + " es correcto"));
            enqueueLink(InfoDocument.simpleDocument(objectName + " schema", Type.JSON, avroSchema));
            return SchemaCheckStatus.EQUALS;
        }
    }

    private void createUIComponents() {
        valueEditor = createJsonEditor();
        valueScrollPane = componentFactory.createEditorScroll(valueEditor);
        keyEditor = createJsonEditor();
        keyScrollPane = componentFactory.createEditorScroll(keyEditor);
        headersEditor = createPropertiesEditor();
        headersScrollPane = componentFactory.createEditorScroll(headersEditor);
        variablesEditor = createPropertiesEditor();
        variablesScrollPane = componentFactory.createEditorScroll(variablesEditor);
    }

    @Override
    protected void asyncTaskFinished() {
        super.asyncTaskFinished();
        buttonSend.setEnabled(true);
        buttonCompareSchemas.setEnabled(true);
    }

    @Override
    public Optional<JTextComponent> getCurrentEditor() {
        int index = tabbedPane.getSelectedIndex();
        return getUmpteenthEditor(index, infoTextPane, keyEditor, valueEditor, variablesEditor);
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

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(5, 2, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        buttonCompareSchemas = new JButton();
        buttonCompareSchemas.setIcon(new ImageIcon(getClass().getResource("/images/compare.png")));
        this.$$$loadButtonText$$$(buttonCompareSchemas, this.$$$getMessageFromBundle$$$("messages", "button.compare.schemas"));
        panel1.add(buttonCompareSchemas, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 4, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonSend = new JButton();
        buttonSend.setIcon(new ImageIcon(getClass().getResource("/images/enviar.png")));
        this.$$$loadButtonText$$$(buttonSend, this.$$$getMessageFromBundle$$$("messages", "button.send"));
        panel1.add(buttonSend, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        quantityComboBox = new JComboBox();
        quantityComboBox.setEditable(true);
        quantityComboBox.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.send.event.count"));
        panel1.add(quantityComboBox, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        dangerLabel = new JLabel();
        dangerLabel.setIcon(new ImageIcon(getClass().getResource("/images/danger.png")));
        dangerLabel.setText("");
        dangerLabel.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.danger.not.local"));
        panel1.add(dangerLabel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(7, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel2, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(615, 121), null, 0, false));
        comboTopic = new JComboBox();
        comboTopic.setEditable(true);
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        comboTopic.setModel(defaultComboBoxModel1);
        panel2.add(comboTopic, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages", "label.topic"));
        panel2.add(label1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(137, 16), null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("messages", "label.value"));
        panel2.add(label2, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboValue = new JComboBox();
        comboValue.setEditable(true);
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        comboValue.setModel(defaultComboBoxModel2);
        panel2.add(comboValue, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("messages", "label.environment"));
        panel2.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Producer:");
        panel2.add(label4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboEnvironment = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        comboEnvironment.setModel(defaultComboBoxModel3);
        panel2.add(comboEnvironment, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboProducer = new JComboBox();
        comboProducer.setEditable(false);
        final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
        comboProducer.setModel(defaultComboBoxModel4);
        panel2.add(comboProducer, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonOpenFileValue = new JButton();
        Font buttonOpenFileValueFont = this.$$$getFont$$$(null, -1, -1, buttonOpenFileValue.getFont());
        if (buttonOpenFileValueFont != null) buttonOpenFileValue.setFont(buttonOpenFileValueFont);
        buttonOpenFileValue.setIcon(new ImageIcon(getClass().getResource("/images/folder.png")));
        buttonOpenFileValue.setText("");
        buttonOpenFileValue.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.open.value.file"));
        panel2.add(buttonOpenFileValue, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(24, 24), new Dimension(24, 24), new Dimension(24, 24), 0, false));
        final JLabel label5 = new JLabel();
        this.$$$loadLabelText$$$(label5, this.$$$getMessageFromBundle$$$("messages", "label.key"));
        panel2.add(label5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboKey = new JComboBox();
        comboKey.setEditable(true);
        panel2.add(comboKey, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonOpenFileKey = new JButton();
        Font buttonOpenFileKeyFont = this.$$$getFont$$$(null, -1, -1, buttonOpenFileKey.getFont());
        if (buttonOpenFileKeyFont != null) buttonOpenFileKey.setFont(buttonOpenFileKeyFont);
        buttonOpenFileKey.setIcon(new ImageIcon(getClass().getResource("/images/folder.png")));
        buttonOpenFileKey.setText("");
        buttonOpenFileKey.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.open.key.file"));
        panel2.add(buttonOpenFileKey, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(24, 24), new Dimension(24, 24), new Dimension(24, 24), 0, false));
        final JLabel label6 = new JLabel();
        this.$$$loadLabelText$$$(label6, this.$$$getMessageFromBundle$$$("messages", "label.domain"));
        panel2.add(label6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboDomain = new JComboBox();
        panel2.add(comboDomain, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonFindTopic = new JButton();
        Font buttonFindTopicFont = this.$$$getFont$$$(null, -1, -1, buttonFindTopic.getFont());
        if (buttonFindTopicFont != null) buttonFindTopic.setFont(buttonFindTopicFont);
        buttonFindTopic.setIcon(new ImageIcon(getClass().getResource("/images/glasses.png")));
        buttonFindTopic.setText("");
        buttonFindTopic.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.search.topic"));
        panel2.add(buttonFindTopic, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(24, 24), new Dimension(24, 24), new Dimension(24, 24), 0, false));
        buttonCheckEnvironment = new JButton();
        Font buttonCheckEnvironmentFont = this.$$$getFont$$$(null, -1, -1, buttonCheckEnvironment.getFont());
        if (buttonCheckEnvironmentFont != null) buttonCheckEnvironment.setFont(buttonCheckEnvironmentFont);
        buttonCheckEnvironment.setIcon(new ImageIcon(getClass().getResource("/images/check_grey.png")));
        buttonCheckEnvironment.setText("");
        buttonCheckEnvironment.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.check.connection"));
        panel2.add(buttonCheckEnvironment, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(24, 24), new Dimension(24, 24), new Dimension(24, 24), 0, false));
        final JLabel label7 = new JLabel();
        this.$$$loadLabelText$$$(label7, this.$$$getMessageFromBundle$$$("messages", "label.headers"));
        panel2.add(label7, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboHeaders = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel5 = new DefaultComboBoxModel();
        comboHeaders.setModel(defaultComboBoxModel5);
        panel2.add(comboHeaders, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonOpenFileHeaders = new JButton();
        Font buttonOpenFileHeadersFont = this.$$$getFont$$$(null, -1, -1, buttonOpenFileHeaders.getFont());
        if (buttonOpenFileHeadersFont != null) buttonOpenFileHeaders.setFont(buttonOpenFileHeadersFont);
        buttonOpenFileHeaders.setIcon(new ImageIcon(getClass().getResource("/images/folder.png")));
        buttonOpenFileHeaders.setText("");
        buttonOpenFileHeaders.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.open.value.file"));
        panel2.add(buttonOpenFileHeaders, new GridConstraints(6, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(24, 24), new Dimension(24, 24), new Dimension(24, 24), 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel3, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setIcon(new ImageIcon(getClass().getResource("/images/search.png")));
        label8.setText("");
        panel3.add(label8, BorderLayout.WEST);
        searchTextField = new JTextField();
        searchTextField.setText("");
        panel3.add(searchTextField, BorderLayout.CENTER);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel4, new GridConstraints(2, 0, 2, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        if (infoTextPaneFont != null) infoTextPane.setFont(infoTextPaneFont);
        infoTextPane.setForeground(new Color(-1));
        scrollPane1.setViewportView(infoTextPane);
        tabKey = new JPanel();
        tabKey.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "tab.key"), tabKey);
        tabKey.add(keyScrollPane, BorderLayout.CENTER);
        tabValue = new JPanel();
        tabValue.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "tab.value"), tabValue);
        tabValue.add(valueScrollPane, BorderLayout.CENTER);
        tabHeaders = new JPanel();
        tabHeaders.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "tab.headers"), tabHeaders);
        tabHeaders.add(headersScrollPane, BorderLayout.CENTER);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new BorderLayout(0, 0));
        tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "tab.variables"), panel5);
        panel5.add(variablesScrollPane, BorderLayout.CENTER);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel4.add(panel6, BorderLayout.EAST);
        cleanButton = new JButton();
        cleanButton.setIcon(new ImageIcon(getClass().getResource("/images/rubber.png")));
        cleanButton.setText("");
        cleanButton.setToolTipText("Limpiar");
        panel6.add(cleanButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        copyButton = new JButton();
        copyButton.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
        copyButton.setText("");
        copyButton.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.copy.clipboard"));
        panel6.add(copyButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel6.add(spacer2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel6.add(spacer3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 30), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
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
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
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
                if (i == text.length()) break;
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
                if (i == text.length()) break;
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

}

