package es.jbp.kajtools.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import es.jbp.kajtools.Environment;
import es.jbp.kajtools.EnvironmentConfiguration;
import es.jbp.kajtools.IConsumer;
import es.jbp.kajtools.IProducer;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.KajToolsApp;
import es.jbp.kajtools.filter.MessageFilter;
import es.jbp.kajtools.tabla.ModeloTablaGenerico;
import es.jbp.kajtools.tabla.RecordItem;
import es.jbp.kajtools.tabla.TablaGenerica;
import es.jbp.kajtools.util.JsonUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleContext;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class KafkaConsumerPanel extends BasePanel {


  private JButton consumeButtom;
  @Getter
  private JPanel contentPane;
  private JComboBox comboTopic;
  private JComboBox comboEnvironment;
  private JComboBox comboConsumer;
  private JComboBox comboDomain;
  private JTable recordTable;
  private JTabbedPane tabbedPane;
  private JPanel tabInfo;
  private JTextPane infoTextPane;
  private JPanel tabKey;
  private RTextScrollPane keyScrollPane;
  private JPanel tabValue;
  private RTextScrollPane valueScrollPane;
  private JTextField searchTextField;
  private JTextField fieldRewindRecords;
  private JPanel tabFilter;
  private RTextScrollPane filterScrollPane;
  private JButton cleanButton;
  private JButton copyButton;
  private JCheckBox checkFilter;
  private JComboBox comboFilterType;
  private JTextField textFieldFilter;

  private RSyntaxTextArea jsonEditorValue;
  private RSyntaxTextArea jsonEditorKey;
  private RSyntaxTextArea scriptEditorFilter;

  ModeloTablaGenerico<RecordItem> recordTableModel = new ModeloTablaGenerico<>();
  private Future<List<RecordItem>> futureRecords;

  public KafkaConsumerPanel() {

    $$$setupUI$$$();

    // Combo Entorno
    EnvironmentConfiguration.ENVIRONMENT_LIST.forEach(comboEnvironment::addItem);

    // Combo Dominio
    final List<IProducer> producerList = KajToolsApp.getInstance().getProducerList();
    producerList.stream().map(IProducer::getDomain).distinct().forEach(comboDomain::addItem);
    comboDomain.addActionListener(e -> updateConsumers());
    updateConsumers();

    consumeButtom.addActionListener(e -> consume());

    // Combo Consumidores
    comboConsumer.addActionListener(e -> updateTopics());
    updateTopics();

    // Combo tipo de filtro
    comboFilterType.addActionListener(e -> textFieldFilter.setEnabled(comboFilterType.getSelectedIndex() == 1));
    textFieldFilter.setEnabled(false);

    // Tabla de registros
    ListSelectionModel selectionModel = recordTable.getSelectionModel();
    selectionModel.addListSelectionListener(this::recordSelected);

    cleanButton.addActionListener(e -> cleanEditor());
    copyButton.addActionListener(e -> copyToClipboard());

    enableTextSearch(searchTextField, jsonEditorValue, jsonEditorKey);
  }

  private void recordSelected(ListSelectionEvent e) {
    int index = recordTable.getSelectionModel().getMinSelectionIndex();
    RecordItem selected = recordTableModel.getFila(index);
    if (selected == null) {
      return;
    }
    jsonEditorKey.setText(JsonUtils.formatJson(selected.getKey()));
    jsonEditorKey.setCaretPosition(0);
    jsonEditorValue.setText(JsonUtils.formatJson(selected.getValue()));
    jsonEditorValue.setCaretPosition(0);
  }

  private void updateTopics() {
    comboTopic.removeAllItems();
    IConsumer<?, ?> consumer = (IConsumer<?, ?>) comboConsumer.getSelectedItem();
    if (consumer == null) {
      return;
    }
    consumer.getAvailableTopics().forEach(comboTopic::addItem);
    comboTopic.setSelectedItem(consumer.getDefaultTopic());
  }

  private void updateConsumers() {
    comboConsumer.removeAllItems();
    String domain = Objects.toString(comboDomain.getSelectedItem());
    final List<IConsumer<?, ?>> consumerList = KajToolsApp.getInstance().getConsumerList();
    consumerList.stream()
        .filter(c -> StringUtils.isBlank(domain) || domain.equals(c.getDomain()))
        .forEach(comboConsumer::addItem);
  }

  private void consume() {

    Environment environment = (Environment) comboEnvironment.getSelectedItem();
    String topic = comboTopic.getEditor().getItem().toString();

    Object consumerSelectedItem = comboConsumer.getSelectedItem();
    if (consumerSelectedItem == null) {
      printError("Se debe seleccionar un consumidor antes de consumir mensajes");
      return;
    }
    long maxRecordsPerPartition = NumberUtils.toLong(fieldRewindRecords.getText(), 50);
    int filterType = comboFilterType.getSelectedIndex();
    String textFilter = textFieldFilter.getText().trim();
    String script = scriptEditorFilter.getText().trim();

    IConsumer<?, ?> consumer = (IConsumer<?, ?>) consumerSelectedItem;
    MessageFilter filter;

    if (filterType == 1 && StringUtils.isNotBlank(textFilter)) {
      filter = (k, v) -> k.contains(textFilter) || v.contains(textFilter);
    } else if (filterType == 2 && StringUtils.isNotBlank(script)) {
      try {
        filter = consumer.createScriptFilter(script);
      } catch (KajException ex) {
        printException(ex);
        return;
      }
    } else {
      filter = (k, v) -> true;
    }

    printAction("Consumiendo mensajes del topic " + topic);

    futureRecords = this.<List<RecordItem>>executeAsyncTask(
        () -> requestRecords(environment, topic, consumer, filter, maxRecordsPerPartition),
        this::recordsReceived);
  }

  private List<RecordItem> requestRecords(Environment environment, String topic, IConsumer<?, ?> consumer,
      MessageFilter filter, long maxRecordsPerPartition) {

    try {
      List<RecordItem> records = consumer.consumeLastRecords(environment, topic, filter, maxRecordsPerPartition);
      enqueueSuccessful("Consumidos " + records.size() + " mensajes");
      return records;
    } catch (KajException ex) {
      //enqueueError("No se ha podido consumir ningún registro");
      enqueueException(ex);
    }
    return Collections.emptyList();
  }

  private void recordsReceived() {
    List<RecordItem> records = null;
    try {
      records = futureRecords.get();
    } catch (Exception ex) {
      enqueueError("No de han podido obtener los registros recibidos");
      enqueueInfo(ex.getMessage());
    }
    if (records != null) {
      recordTableModel.setListaObjetos(records);
    }
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
    contentPane.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.setBorder(BorderFactory
        .createTitledBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), null, TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION, null, null));
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel1,
        new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboTopic = new JComboBox();
    comboTopic.setEditable(true);
    final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
    comboTopic.setModel(defaultComboBoxModel1);
    panel1.add(comboTopic, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label1 = new JLabel();
    label1.setText("Topic:");
    panel1.add(label1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(137, 16), null, 0,
        false));
    final JLabel label2 = new JLabel();
    label2.setText("Entorno:");
    panel1.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label3 = new JLabel();
    label3.setText("Consumer:");
    panel1.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboEnvironment = new JComboBox();
    final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
    comboEnvironment.setModel(defaultComboBoxModel2);
    panel1.add(comboEnvironment,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboConsumer = new JComboBox();
    comboConsumer.setEditable(false);
    final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
    comboConsumer.setModel(defaultComboBoxModel3);
    panel1.add(comboConsumer,
        new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label4 = new JLabel();
    label4.setText("Dominio:");
    panel1.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboDomain = new JComboBox();
    panel1.add(comboDomain,
        new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new BorderLayout(0, 0));
    contentPane.add(panel2, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, null, null, 0, false));
    final JLabel label5 = new JLabel();
    label5.setIcon(new ImageIcon(getClass().getResource("/images/search.png")));
    label5.setText("");
    panel2.add(label5, BorderLayout.WEST);
    searchTextField = new JTextField();
    searchTextField.setText("");
    panel2.add(searchTextField, BorderLayout.CENTER);
    final JSplitPane splitPane1 = new JSplitPane();
    splitPane1.setDividerLocation(120);
    splitPane1.setOrientation(0);
    contentPane.add(splitPane1,
        new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200),
            null, 0, false));
    final JScrollPane scrollPane1 = new JScrollPane();
    splitPane1.setLeftComponent(scrollPane1);
    recordTable.setAutoCreateRowSorter(false);
    recordTable.setShowHorizontalLines(false);
    scrollPane1.setViewportView(recordTable);
    tabbedPane = new JTabbedPane();
    splitPane1.setRightComponent(tabbedPane);
    tabInfo = new JPanel();
    tabInfo.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab("Información", tabInfo);
    final JScrollPane scrollPane2 = new JScrollPane();
    tabInfo.add(scrollPane2, BorderLayout.CENTER);
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
    scrollPane2.setViewportView(infoTextPane);
    tabKey = new JPanel();
    tabKey.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab("Key", tabKey);
    tabKey.add(keyScrollPane, BorderLayout.CENTER);
    tabValue = new JPanel();
    tabValue.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab("Value", tabValue);
    tabValue.add(valueScrollPane, BorderLayout.CENTER);
    final JPanel panel3 = new JPanel();
    panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel3,
        new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0,
            false));
    consumeButtom = new JButton();
    consumeButtom.setText("Consumir");
    panel3.add(consumeButtom, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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

  private void createUIComponents() {

    jsonEditorValue = createJsonEditor();
    valueScrollPane = createEditorScroll(jsonEditorValue);
    jsonEditorKey = createJsonEditor();
    keyScrollPane = createEditorScroll(jsonEditorKey);

    scriptEditorFilter = createScriptEditor();
    filterScrollPane = createEditorScroll(scriptEditorFilter);

    TablaGenerica tablaGenerica = new TablaGenerica();
    recordTableModel.agregarColumna("partition", "Partition", 20);
    recordTableModel.agregarColumna("offset", "Offset", 20);
    recordTableModel.agregarColumna("dateTime", "Timestamp", 50);
    recordTableModel.agregarColumna("key", "Key", 200);
    recordTableModel.agregarColumna("value", "Value", 200);
    tablaGenerica.setModel(recordTableModel);
    recordTable = tablaGenerica;
  }

  @Override
  protected JTextPane getInfoTextPane() {
    return infoTextPane;
  }

  @Override
  protected void enableButtons(boolean enable) {
    consumeButtom.setEnabled(enable);
  }

  @Override
  protected Optional<JTextComponent> getCurrentEditor() {
    int index = tabbedPane.getSelectedIndex();
    if (index == 0) {
      return Optional.of(infoTextPane);
    } else if (index == 1) {
      return Optional.of(scriptEditorFilter);
    } else if (index == 2) {
      return Optional.of(jsonEditorKey);
    } else if (index == 3) {
      return Optional.of(jsonEditorValue);
    } else {
      return Optional.empty();
    }
  }
}
