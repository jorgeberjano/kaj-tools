package es.jbp.kajtools.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import es.jbp.kajtools.Environment;
import es.jbp.kajtools.configuration.Configuration;
import es.jbp.kajtools.IMessageClient;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.KajToolsApp;
import es.jbp.kajtools.filter.MessageFilter;
import es.jbp.kajtools.filter.ScriptMessageFilter;
import es.jbp.tabla.ColoreadorFila;
import es.jbp.tabla.ModeloTablaGenerico;
import es.jbp.tabla.TablaGenerica;
import es.jbp.kajtools.ui.entities.RecordItem;
import es.jbp.kajtools.ui.entities.TopicItem;
import es.jbp.kajtools.util.JsonUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
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

public class KafkaConsumerPanel extends KafkaBasePanel {


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
  private JButton buttonFindTopic;
  private JButton buttonCheckEnvironment;
  private JButton buttonStop;

  private RSyntaxTextArea jsonEditorValue;
  private RSyntaxTextArea jsonEditorKey;
  private RSyntaxTextArea scriptEditorFilter;

  private ModeloTablaGenerico<RecordItem> recordTableModel = new ModeloTablaGenerico<>();
  private Future<List<RecordItem>> futureRecords;

  public KafkaConsumerPanel() {

    $$$setupUI$$$();

    // Combo Entorno
    Configuration.getEnvironmentList().forEach(comboEnvironment::addItem);

    buttonCheckEnvironment.addActionListener(e -> asyncRetrieveTopics());

    // Combo Dominio
    final List<IMessageClient> clientList = KajToolsApp.getInstance().getClientList();
    clientList.stream().map(IMessageClient::getDomain).distinct().forEach(comboDomain::addItem);
    comboDomain.addActionListener(e -> updateConsumers());
    updateConsumers();

    consumeButtom.addActionListener(e -> consume());

    // Combo Consumidores
    comboConsumer.addActionListener(e -> updateTopics());
    updateTopics();

    // Combo tipo de filtro
    comboFilterType.addActionListener(e -> {
      textFieldFilter.setEnabled(comboFilterType.getSelectedIndex() == 1);
      scriptEditorFilter.setEnabled(comboFilterType.getSelectedIndex() == 2);
    });
    textFieldFilter.setEnabled(false);

    // Tabla de registros
    ListSelectionModel selectionModel = recordTable.getSelectionModel();
    selectionModel.addListSelectionListener(this::recordSelected);

    buttonFindTopic.addActionListener(e -> findTopic());

    cleanButton.addActionListener(e -> cleanEditor());
    copyButton.addActionListener(e -> copyToClipboard());

    buttonStop.addActionListener(e -> stopAsyncTasks());

    scriptEditorFilter.setEnabled(false);

    enableTextSearch(searchTextField, jsonEditorValue, jsonEditorKey);
  }

  @Override
  protected void showConnectionStatus(Boolean ok) {
    buttonCheckEnvironment.setIcon(ok == null ? iconCheckUndefined : (ok ? iconCheckOk : iconCheckFail));
  }

  private void findTopic() {
    TopicItem topicItem = selectTopic();
    if (topicItem != null) {
      comboTopic.getEditor().setItem(topicItem.getName());
    }
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
    IMessageClient client = (IMessageClient) comboConsumer.getSelectedItem();
    if (client == null) {
      return;
    }
    client.getAvailableTopics().forEach(comboTopic::addItem);
    comboTopic.setSelectedItem(client.getDefaultTopic());
  }

  private void updateConsumers() {
    comboConsumer.removeAllItems();
    String domain = Objects.toString(comboDomain.getSelectedItem());
    final List<IMessageClient> consumerList = KajToolsApp.getInstance().getClientList();
    consumerList.stream()
        .filter(c -> StringUtils.isBlank(domain) || domain.equals(c.getDomain()))
        .forEach(comboConsumer::addItem);
  }

  private void consume() {

    String topic = comboTopic.getEditor().getItem().toString();

    Object consumerSelectedItem = comboConsumer.getSelectedItem();
    if (consumerSelectedItem == null) {
      printError("Se debe seleccionar un consumidor antes de consumir mensajes");
      return;
    }
    IMessageClient client = (IMessageClient) consumerSelectedItem;

    long maxRecordsPerPartition = NumberUtils.toLong(fieldRewindRecords.getText(), 50);
    int filterType = comboFilterType.getSelectedIndex();
    String textFilter = textFieldFilter.getText().trim();
    String script = scriptEditorFilter.getText().trim();

    MessageFilter filter;

    if (filterType == 1 && StringUtils.isNotBlank(textFilter)) {
      filter = (k, v) -> k.contains(textFilter) || v.contains(textFilter);
    } else if (filterType == 2 && StringUtils.isNotBlank(script)) {
      try {
        filter = new ScriptMessageFilter(script);
      } catch (KajException ex) {
        printException(ex);
        return;
      }
    } else {
      filter = (k, v) -> true;
    }

    recordTableModel.setListaObjetos(new ArrayList<>());
    recordTableModel.actualizar();

    printAction("Consumiendo mensajes del topic " + topic);

    buttonStop.setEnabled(true);

    futureRecords = this.<List<RecordItem>>executeAsyncTask(
        () -> requestRecords(getEnvironment(), topic, client, filter, maxRecordsPerPartition),
        this::recordsReceived);
  }


  private List<RecordItem> requestRecords(Environment environment, String topic, IMessageClient client,
      MessageFilter filter, long maxRecordsPerPartition) {
    try {
      List<RecordItem> records = client
          .consumeLastRecords(environment, topic, filter, maxRecordsPerPartition, abortTasks);
      enqueueSuccessful("Consumidos " + records.size() + " mensajes");
      return records;
    } catch (KajException ex) {
      //enqueueError("No se ha podido consumir ningún registro");
      enqueueException(ex);
    }
    return Collections.emptyList();
  }

  private void recordsReceived() {

    buttonStop.setEnabled(false);

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

    records.forEach(this::printRecordErrors);

  }

  private void printRecordErrors(RecordItem recordItem) {
    if (recordItem.getKeyError() != null) {
      printError(
          "Error en el Key del mensaje de la partición " + recordItem.getPartition() + " con offset " + recordItem
              .getOffset());
      printInfo(recordItem.getKeyError());
    }
    if (recordItem.getValueError() != null) {
      printError(
          "Error en el Value del mensaje de la partición " + recordItem.getPartition() + " con offset " + recordItem
              .getOffset());
      printInfo(recordItem.getValueError());
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
    contentPane.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
    contentPane.setBorder(BorderFactory
        .createTitledBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), null, TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION, null, null));
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel1,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
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
    buttonFindTopic = new JButton();
    Font buttonFindTopicFont = this.$$$getFont$$$(null, -1, -1, buttonFindTopic.getFont());
    if (buttonFindTopicFont != null) {
      buttonFindTopic.setFont(buttonFindTopicFont);
    }
    buttonFindTopic.setIcon(new ImageIcon(getClass().getResource("/images/glasses.png")));
    buttonFindTopic.setText("");
    buttonFindTopic.setToolTipText("Buscar en todos los topics");
    panel1.add(buttonFindTopic,
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
    panel1.add(buttonCheckEnvironment,
        new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(24, 24),
            new Dimension(24, 24), new Dimension(24, 24), 0, false));
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new BorderLayout(0, 0));
    contentPane.add(panel2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
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
    splitPane1.setDividerLocation(150);
    splitPane1.setOrientation(0);
    contentPane.add(splitPane1,
        new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 245),
            null, 0, false));
    final JScrollPane scrollPane1 = new JScrollPane();
    splitPane1.setLeftComponent(scrollPane1);
    recordTable.setAutoCreateRowSorter(false);
    recordTable.setShowHorizontalLines(false);
    scrollPane1.setViewportView(recordTable);
    final JPanel panel3 = new JPanel();
    panel3.setLayout(new BorderLayout(0, 0));
    splitPane1.setRightComponent(panel3);
    tabbedPane = new JTabbedPane();
    panel3.add(tabbedPane, BorderLayout.CENTER);
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
    tabFilter = new JPanel();
    tabFilter.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab("Filtro", tabFilter);
    tabFilter.add(filterScrollPane, BorderLayout.CENTER);
    tabKey = new JPanel();
    tabKey.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab("Key", tabKey);
    tabKey.add(keyScrollPane, BorderLayout.CENTER);
    tabValue = new JPanel();
    tabValue.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab("Value", tabValue);
    tabValue.add(valueScrollPane, BorderLayout.CENTER);
    final JPanel panel4 = new JPanel();
    panel4.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
    panel3.add(panel4, BorderLayout.EAST);
    cleanButton = new JButton();
    cleanButton.setIcon(new ImageIcon(getClass().getResource("/images/rubber.png")));
    cleanButton.setText("");
    cleanButton.setToolTipText("Limpiar");
    panel4.add(cleanButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    copyButton = new JButton();
    copyButton.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
    copyButton.setText("");
    copyButton.setToolTipText("Copiar al portapapeles");
    panel4.add(copyButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, null, null, 0, false));
    final Spacer spacer1 = new Spacer();
    panel4.add(spacer1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    final Spacer spacer2 = new Spacer();
    panel4.add(spacer2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, 1,
        GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 30), null, 0, false));
    final JPanel panel5 = new JPanel();
    panel5.setLayout(new GridLayoutManager(1, 8, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel5,
        new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0,
            false));
    consumeButtom = new JButton();
    consumeButtom.setText("Consumir");
    panel5.add(consumeButtom, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    fieldRewindRecords = new JTextField();
    fieldRewindRecords.setText("50");
    panel5.add(fieldRewindRecords,
        new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(40, -1), null, 0,
            false));
    final JLabel label6 = new JLabel();
    label6.setText("Rebobinar:");
    label6.setToolTipText("Número registros por partición a rebobinar");
    panel5.add(label6, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer3 = new Spacer();
    panel5.add(spacer3, new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    final Spacer spacer4 = new Spacer();
    panel5.add(spacer4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    comboFilterType = new JComboBox();
    final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
    defaultComboBoxModel4.addElement("Sin filtro");
    defaultComboBoxModel4.addElement("Contiene texto");
    defaultComboBoxModel4.addElement("Filtro JavaScript");
    comboFilterType.setModel(defaultComboBoxModel4);
    panel5.add(comboFilterType,
        new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(80, -1), null, 0,
            false));
    textFieldFilter = new JTextField();
    panel5.add(textFieldFilter,
        new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null,
            0, false));
    buttonStop = new JButton();
    buttonStop.setEnabled(false);
    buttonStop.setIcon(new ImageIcon(getClass().getResource("/images/stop.png")));
    buttonStop.setText("");
    panel5.add(buttonStop, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
    tablaGenerica.setColoreador(new ColoreadorFila<RecordItem>() {
      @Override
      public Color determinarColorTexto(RecordItem entidad) {
        if (entidad.getKeyError() != null || entidad.getValueError() != null) {
          return Color.white;
        } else {
          return null;
        }
      }

      @Override
      public Color determinarColorFondo(RecordItem entidad) {
        if (entidad.getKeyError() != null || entidad.getValueError() != null) {
          return Color.red.darker();
        } else {
          return null;
        }
      }
    });
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
    return getUmpteenthEditor(index, infoTextPane, scriptEditorFilter, jsonEditorKey, jsonEditorValue);
//    if (index == 0) {
//      return Optional.of(infoTextPane);
//    } else if (index == 1) {
//      return Optional.of(scriptEditorFilter);
//    } else if (index == 2) {
//      return Optional.of(jsonEditorKey);
//    } else if (index == 3) {
//      return Optional.of(jsonEditorValue);
//    } else {
//      return Optional.empty();
//    }
  }

  @Override
  protected Environment getEnvironment() {
    return (Environment) comboEnvironment.getSelectedItem();
  }
}
