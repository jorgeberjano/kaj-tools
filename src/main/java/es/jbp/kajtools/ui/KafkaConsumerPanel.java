package es.jbp.kajtools.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import es.jbp.kajtools.Environment;
import es.jbp.kajtools.IMessageClient;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.configuration.Configuration;
import es.jbp.kajtools.filter.MessageFilter;
import es.jbp.kajtools.filter.ScriptMessageFilter;
import es.jbp.kajtools.i18n.I18nService;
import es.jbp.kajtools.kafka.ConsumerFeedback;
import es.jbp.kajtools.kafka.HeaderItem;
import es.jbp.kajtools.kafka.KafkaAdminService;
import es.jbp.kajtools.kafka.RecordItem;
import es.jbp.kajtools.kafka.RewindPolicy;
import es.jbp.kajtools.kafka.TopicItem;
import es.jbp.kajtools.schemaregistry.SchemaRegistryService;
import es.jbp.kajtools.ui.InfoDocument.Type;
import es.jbp.kajtools.ui.interfaces.InfoReportable;
import es.jbp.kajtools.util.JsonUtils;
import es.jbp.tabla.ColoreadorFila;
import es.jbp.tabla.ModeloTablaGenerico;
import es.jbp.tabla.TablaGenerica;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.swing.AbstractButton;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
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


  public static final int CONTIENE_TEXTO = 1;
  public static final int FILTRO_JAVASCRIPT = 2;
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
  private JPanel tabKey;
  private RTextScrollPane keyScrollPane;
  private JPanel tabValue;
  private RTextScrollPane valueScrollPane;
  @Getter
  private JTextField searchTextField;
  private JTextField fieldMaxRecords;
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
  private JLabel labelCounter;
  private JComboBox<RewindPolicy> comboRewind;
  private JPanel tabHeaders;
  private JScrollPane headersScrollPane;
  private JTable headersTable;
  private final ModeloTablaGenerico<HeaderItem> headersTableModel = new ModeloTablaGenerico<>();

  private RSyntaxTextArea jsonEditorValue;
  private RSyntaxTextArea jsonEditorKey;
  private RSyntaxTextArea scriptEditorFilter;

  private final ModeloTablaGenerico<RecordItem> recordTableModel = new ModeloTablaGenerico<>();
  private List<RecordItem> recordItemsList = new ArrayList<>();
  private MessageFilter filter;
  private int maxRecords = 50;
  private int recordConsumedCount = 0;
  @Getter
  private InfoTextPane infoTextPane;

  public KafkaConsumerPanel(List<IMessageClient> clientList,
      SchemaRegistryService schemaRegistryService,
      KafkaAdminService kafkaInvestigator,
      ComponentFactory componentFactory,
      I18nService i18nService) {

    super(clientList, schemaRegistryService, kafkaInvestigator, componentFactory, i18nService);

    $$$setupUI$$$();

    super.initialize();

    // Combo Entorno
    Configuration.getEnvironmentList().forEach(comboEnvironment::addItem);

    buttonCheckEnvironment.addActionListener(e -> asyncRetrieveTopics());

    // Combo Dominio
    clientList.stream().map(IMessageClient::getDomain).distinct().forEach(comboDomain::addItem);
    comboDomain.addActionListener(e -> updateConsumers());
    updateConsumers();

    consumeButtom.addActionListener(e -> consume());

    // Combo Consumidores
    comboConsumer.addActionListener(e -> updateTopics());
    updateTopics();

    // Combo rebobinado
    comboRewind.setModel(new DefaultComboBoxModel<>(RewindPolicy.values()));

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

    jsonEditorValue.setEditable(false);
    jsonEditorKey.setEditable(false);

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

    headersTableModel.setListaObjetos(selected.getHeaders());
    headersTableModel.actualizar();
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
    clientList.stream()
        .filter(c -> StringUtils.isBlank(domain) || domain.equals(c.getDomain()))
        .forEach(comboConsumer::addItem);
  }

  private void consume() {

    String topic = comboTopic.getEditor().getItem().toString();

    Object consumerSelectedItem = comboConsumer.getSelectedItem();
    if (consumerSelectedItem == null) {
      printMessage(InfoReportable.buildErrorMessage(
          "Se debe seleccionar un consumidor antes de consumir mensajes"));
      return;
    }
    IMessageClient client = (IMessageClient) consumerSelectedItem;

    maxRecords = NumberUtils.toInt(fieldMaxRecords.getText(), 0);
    int filterType = comboFilterType.getSelectedIndex();
    String textFilter = textFieldFilter.getText().trim();
    String script = scriptEditorFilter.getText().trim();

    if (filterType == CONTIENE_TEXTO && StringUtils.isNotBlank(textFilter)) {
      filter = (rec) -> rec.getKey().contains(textFilter) || rec.getValue().contains(textFilter);
    } else if (filterType == FILTRO_JAVASCRIPT && StringUtils.isNotBlank(script)) {
      try {
        filter = new ScriptMessageFilter(script);
      } catch (KajException ex) {
        printException(ex);
        return;
      }
    } else {
      filter = (rec) -> true;
    }

    labelCounter.setText("0");

    recordItemsList = new ArrayList<>();
    recordTableModel.setListaObjetos(recordItemsList);
    recordTableModel.actualizar();

    final LocalDateTime dateTimeToRewind = calculateDateTimeToRewind();

    consumeButtom.setEnabled(false);
    buttonStop.setEnabled(true);
    recordConsumedCount = 0;

    printMessage(InfoReportable.buildActionMessage("Consumiendo mensajes del topic " + topic));

    this.<Void>executeAsyncTask(() -> requestRecords(getEnvironment(), topic, dateTimeToRewind, client));
  }

  private LocalDateTime calculateDateTimeToRewind() {
    LocalDateTime dateTimeToRewind = null;
    switch ((RewindPolicy) Objects.requireNonNull(comboRewind.getSelectedItem())) {

      case LAST_MINUTE:
        dateTimeToRewind = LocalDateTime.now().minusMinutes(1);
        break;
      case LAST_5_MINUTES:
        dateTimeToRewind = LocalDateTime.now().minusMinutes(5);
        break;
      case LAST_15_MINUTES:
        dateTimeToRewind = LocalDateTime.now().minusMinutes(15);
        break;
      case LAST_30_MINUTES:
        dateTimeToRewind = LocalDateTime.now().minusMinutes(30);
        break;
      case LAST_HOUR:
        dateTimeToRewind = LocalDateTime.now().minusHours(1);
        break;
      case LAST_DAY:
        dateTimeToRewind = LocalDateTime.now().minusDays(1);
        break;
      case LAST_WEEK:
        dateTimeToRewind = LocalDateTime.now().minusWeeks(1);
        break;
      case LAST_MONTH:
        dateTimeToRewind = LocalDateTime.now().minusMonths(1);
        break;
      case LAST_YEAR:
        dateTimeToRewind = LocalDateTime.now().minusYears(1);
        break;
    }
    return dateTimeToRewind;
  }

  private void showMoreRecords(List<RecordItem> recordItems) {

    for (RecordItem rec : recordItems) {
      try {
        if (maxRecords > 0 && recordItemsList.size() >= maxRecords) {
          abortTasks.set(true);
          break;
        }
        recordConsumedCount++;
        if (filter.satisfyCondition(rec)) {
          recordItemsList.add(rec);
        }
        if (StringUtils.isNotBlank(rec.getValueError())) {
          printMessage(InfoReportable.buildErrorMessage("Error en el value del mensaje con partición "
              + rec.getPartition() + " y offset " + rec.getOffset()));
          printLink(InfoDocument.simpleDocument("error", Type.INFO, rec.getValueError()));
        }
        if (StringUtils.isNotBlank(rec.getKeyError())) {
          printMessage(InfoReportable.buildErrorMessage("Error en el key del mensaje con partición "
              + rec.getPartition() + " y offset " + rec.getOffset()));
          printLink(InfoDocument.simpleDocument("error", Type.INFO, rec.getKeyError()));
        }
      } catch (KajException ex) {
        printException(ex);
      }

    }
    recordTableModel.actualizar();
    int matches = recordItemsList.size();

    labelCounter.setText(matches == recordConsumedCount ? "" + recordConsumedCount
        : matches + "/" + recordConsumedCount);


  }

  private Void requestRecords(Environment environment, String topic, LocalDateTime dateTimeToRewind,
      IMessageClient client) {
    try {
      client.consumeLastRecords(environment, topic, dateTimeToRewind, abortTasks, new ConsumerFeedback() {
        @Override
        public void consumedRecords(List<RecordItem> records) {
          SwingUtilities.invokeLater(() -> {
            showMoreRecords(records);
          });
        }

        @Override
        public void message(String message) {
          enqueueMessage(InfoReportable.buildTraceMessage(message));
        }

        @Override
        public void finished() {
          buttonStop.setEnabled(false);
        }
      });
      enqueueMessage(InfoReportable.buildSuccessfulMessage("Consumidos " + recordItemsList.size() + " mensajes"));
    } catch (KajException ex) {
      enqueueException(ex);
    }
    return null;
  }

//  private void printRecordErrors(RecordItem recordItem) {
//    if (recordItem.getKeyError() != null) {
//      printError(
//          "Error en el Key del mensaje de la partición " + recordItem.getPartition() + " con offset " + recordItem
//              .getOffset());
//      printTrace(recordItem.getKeyError());
//    }
//    if (recordItem.getValueError() != null) {
//      printError(
//          "Error en el Value del mensaje de la partición " + recordItem.getPartition() + " con offset " + recordItem
//              .getOffset());
//      printTrace(recordItem.getValueError());
//    }
//  }

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
    this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages", "label.topic"));
    panel1.add(label1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(137, 16), null, 0,
        false));
    final JLabel label2 = new JLabel();
    this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("messages", "label.environment"));
    panel1.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label3 = new JLabel();
    this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("messages", "label.consumer"));
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
    this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("messages", "label.domain"));
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
    buttonFindTopic.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.search.topic"));
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
    buttonCheckEnvironment.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.check.connection"));
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
    tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "tab.info"), tabInfo);
    final JScrollPane scrollPane2 = new JScrollPane();
    tabInfo.add(scrollPane2, BorderLayout.CENTER);
    infoTextPane = new InfoTextPane();
    infoTextPane.setBackground(new Color(-13948117));
    infoTextPane.setCaretColor(new Color(-1));
    infoTextPane.setEditable(false);
    Font infoTextPaneFont = this.$$$getFont$$$(null, -1, -1, infoTextPane.getFont());
    if (infoTextPaneFont != null) {
      infoTextPane.setFont(infoTextPaneFont);
    }
    infoTextPane.setForeground(new Color(-1));
    scrollPane2.setViewportView(infoTextPane);
    tabFilter = new JPanel();
    tabFilter.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "tab.filter"), tabFilter);
    tabFilter.add(filterScrollPane, BorderLayout.CENTER);
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
    headersScrollPane = new JScrollPane();
    tabHeaders.add(headersScrollPane, BorderLayout.CENTER);
    headersScrollPane.setViewportView(headersTable);
    final JPanel panel4 = new JPanel();
    panel4.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
    panel3.add(panel4, BorderLayout.EAST);
    cleanButton = new JButton();
    cleanButton.setIcon(new ImageIcon(getClass().getResource("/images/rubber.png")));
    cleanButton.setText("");
    cleanButton.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.clean"));
    panel4.add(cleanButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    copyButton = new JButton();
    copyButton.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
    copyButton.setText("");
    copyButton.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.copy.clipboard"));
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
    panel5.setLayout(new GridLayoutManager(1, 10, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel5,
        new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0,
            false));
    consumeButtom = new JButton();
    this.$$$loadButtonText$$$(consumeButtom, this.$$$getMessageFromBundle$$$("messages", "button.consume"));
    panel5.add(consumeButtom, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    fieldMaxRecords = new JTextField();
    fieldMaxRecords.setText("50");
    fieldMaxRecords.setToolTipText("Número máximo de registros a consumir");
    panel5.add(fieldMaxRecords, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(40, -1), null, 0,
        false));
    final JLabel label6 = new JLabel();
    this.$$$loadLabelText$$$(label6, this.$$$getMessageFromBundle$$$("messages", "label.show.only"));
    label6.setToolTipText("Número registros por partición a rebobinar");
    panel5.add(label6, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer3 = new Spacer();
    panel5.add(spacer3, new GridConstraints(0, 9, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
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
        new GridConstraints(0, 7, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(80, -1), null, 0,
            false));
    textFieldFilter = new JTextField();
    panel5.add(textFieldFilter,
        new GridConstraints(0, 8, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null,
            0, false));
    buttonStop = new JButton();
    buttonStop.setEnabled(false);
    buttonStop.setIcon(new ImageIcon(getClass().getResource("/images/stop.png")));
    buttonStop.setText("");
    buttonStop.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.stop.consume"));
    panel5.add(buttonStop, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    labelCounter = new JLabel();
    labelCounter.setHorizontalAlignment(0);
    labelCounter.setText("0");
    panel5.add(labelCounter, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(80, -1),
        new Dimension(80, -1), null, 0, false));
    comboRewind = new JComboBox();
    panel5.add(comboRewind,
        new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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

    jsonEditorValue = createJsonEditor();
    valueScrollPane = componentFactory.createEditorScroll(jsonEditorValue);
    jsonEditorKey = createJsonEditor();
    keyScrollPane = componentFactory.createEditorScroll(jsonEditorKey);
    scriptEditorFilter = createScriptEditor();
    filterScrollPane = componentFactory.createEditorScroll(scriptEditorFilter);

    TablaGenerica recordTable = new TablaGenerica();
    recordTableModel.agregarColumna("partition", "Partition", 20);
    recordTableModel.agregarColumna("offset", "Offset", 20);
    recordTableModel.agregarColumna("dateTime", "Timestamp", 50);
    recordTableModel.agregarColumna("key", "Key", 200);
    recordTableModel.agregarColumna("value", "Value", 200);
    recordTable.setModel(recordTableModel);
    recordTable.setColoreador(new ColoreadorFila<RecordItem>() {
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
    this.recordTable = recordTable;

    TablaGenerica headersTable = new TablaGenerica();
    headersTableModel.agregarColumna("key", "Header key", 20);
    headersTableModel.agregarColumna("value", "Header value", 20);
    headersTable.setModel(headersTableModel);
    this.headersTable = headersTable;
  }

  @Override
  protected void asyncTaskFinished() {
    super.asyncTaskFinished();
    consumeButtom.setEnabled(true);
    buttonStop.setEnabled(false);
  }

  @Override
  public Optional<JTextComponent> getCurrentEditor() {

    int index = tabbedPane.getSelectedIndex();
    return getUmpteenthEditor(index, infoTextPane, scriptEditorFilter, jsonEditorKey, jsonEditorValue);
  }

  @Override
  protected Environment getEnvironment() {
    return (Environment) comboEnvironment.getSelectedItem();
  }
}
