package es.jbp.kajtools.ui;

import com.google.common.collect.Lists;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import es.jbp.kajtools.Environment;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.configuration.Configuration;
import es.jbp.kajtools.IMessageClient;
import es.jbp.kajtools.i18n.I18nService;
import es.jbp.kajtools.kafka.KafkaAdminService;
import es.jbp.kajtools.ui.interfaces.InfoReportable;
import es.jbp.kajtools.util.JsonUtils;
import es.jbp.kajtools.schemaregistry.SchemaRegistryService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.swing.AbstractButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleContext;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class SchemaRegistryPanel extends KafkaBasePanel {

  private JComboBox comboSchemaSubject;
  private JComboBox comboEnvironment;
  private JComboBox comboDomain;
  private JList versionsList;
  private JTabbedPane tabbedPane;
  private JPanel tabInfo;
  private JTextPane infoTextPane;
  private JPanel tabSchema;
  private RTextScrollPane schemaScrollPane;
  private JButton cleanButton;
  private JButton copyButton;
  @Getter
  private JTextField searchTextField;
  @Getter
  private JPanel contentPane;
  private JButton getSchemasButton;
  private JLabel dangerLabel;
  private JButton writeSchemaButton;
  private RSyntaxTextArea schemaEditor;
  private JPopupMenu versionsPopupMenu;

  private Future<Map<String, String>> futureSchemas;
  private Future<List<String>> futureVersions;

  private String currentVersion;
  private List<String> versions;
  private Map<String, SchemaInfo> schemas;

  @Data
  private class SchemaInfo {

    String content;
    Integer position;
    Integer verticalScroll;
    Integer horizontalScroll;

    public SchemaInfo(String schema) {
      this.content = schema;
    }
  }


  public SchemaRegistryPanel(List<IMessageClient> clientList,
      SchemaRegistryService schemaRegistryService,
      KafkaAdminService kafkaAdminService,
      UiComponentCreator componentFactory,
      I18nService i18nService) {
    super(clientList, schemaRegistryService, kafkaAdminService, componentFactory, i18nService);

    $$$setupUI$$$();

    super.initialize();

    dangerLabel.setVisible(false);

    // Combo Entorno
    Configuration.getEnvironmentList().stream().forEach(comboEnvironment::addItem);
    comboEnvironment.addActionListener(e -> {
      boolean local = getEnvironment().getName().toLowerCase().contains("local");
      dangerLabel.setVisible(!local);
    });

    // Combo Dominio
    //final List<IMessageClient> clientList = KajToolsApp.getInstance().getClientList();
    clientList.stream().map(IMessageClient::getDomain).distinct().forEach(comboDomain::addItem);
    comboDomain.addActionListener(e -> updateSchemaSubjects());

    // Lista de subjects
    updateSchemaSubjects();
    comboSchemaSubject.addActionListener(e -> {
      versions = null;
      schemas = null;
      clearVersionList();
    });

    cleanButton.addActionListener(e -> cleanEditor());
    copyButton.addActionListener(e -> copyToClipboard());

    // Lista de versiones
    versionsPopupMenu = new JPopupMenu() {
      @Override
      public void show(Component invoker, int x, int y) {
        int row = versionsList.locationToIndex(new Point(x, y));
        if (row != -1) {
          versionsList.setSelectedIndex(row);
          super.show(invoker, x, y);
        }
      }
    };

    JMenuItem compareActionListener = new JMenuItem("Comparar con anterior");
    compareActionListener.addActionListener(e -> compareSelectedSchemaVersionWithPrevious());
    versionsPopupMenu.add(compareActionListener);

    JMenuItem deleteActionListener = new JMenuItem("Borrar");
    deleteActionListener.addActionListener(e -> asyncDeleteSelectedSchemaVersion());
    versionsPopupMenu.add(deleteActionListener);
    versionsList.setComponentPopupMenu(versionsPopupMenu);
    versionsList.addListSelectionListener(e -> showSelectedSchemaVersion());

    // Botón Obtener esquemas
    getSchemasButton.addActionListener(e -> asyncRequestVersions());

    writeSchemaButton.addActionListener(e -> asyncWriteSchema());

    enableTextSearch(searchTextField, schemaEditor);
  }

  private void showSelectedSchemaVersion() {
    int selectedIndex = versionsList.getSelectedIndex();
    if (selectedIndex < 0 || selectedIndex >= versions.size() || schemas == null) {
      return;
    }
    if (currentVersion != null) {
      Integer position = schemaEditor.getCaretPosition();
      Optional.ofNullable(schemas.get(currentVersion)).ifPresent(info -> {
        info.setPosition(position);
        info.setVerticalScroll(schemaScrollPane.getVerticalScrollBar().getValue());
        info.setHorizontalScroll(schemaScrollPane.getHorizontalScrollBar().getValue());
      });

    }
    currentVersion = versions.get(selectedIndex);
    SchemaInfo schemaInfo = schemas.get(currentVersion);

    schemaEditor.setText(
        Optional.ofNullable(schemaInfo).map(SchemaInfo::getContent).orElse(""));

    schemaEditor.setCaretPosition(Optional.ofNullable(schemaInfo)
        .map(SchemaInfo::getPosition).orElse(0));

    schemaScrollPane.getVerticalScrollBar().setValue(Optional.ofNullable(schemaInfo)
        .map(SchemaInfo::getVerticalScroll).orElse(0));

    schemaScrollPane.getHorizontalScrollBar().setValue(Optional.ofNullable(schemaInfo)
        .map(SchemaInfo::getHorizontalScroll).orElse(0));
  }

  private void updateSchemaSubjects() {
    comboSchemaSubject.removeAllItems();
    currentVersion = null;

    String domain = Objects.toString(comboDomain.getSelectedItem());
    clientList.stream()
        .filter(p -> StringUtils.isBlank(domain) || domain.equals(p.getDomain()))
        .map(IMessageClient::getAvailableTopics)
        .flatMap(List::stream)
        .map(s -> Lists.newArrayList(s + "-key", s + "-value"))
        .flatMap(List::stream)
        .forEach(comboSchemaSubject::addItem);
  }

  // Petición de versiones

  private void asyncRequestVersions() {
    clearVersionList();

    getSchemasButton.setEnabled(false);
    versionsPopupMenu.setEnabled(false);

    String schemaSubject = Objects.toString(comboSchemaSubject.getSelectedItem());
    printMessage(InfoReportable.buildActionMessage(
        "Obteniendo las versiones de los esquemas de " + schemaSubject));

    futureVersions = this.<List<String>>executeAsyncTask(
        () -> requestVersions(getEnvironment(), schemaSubject), this::versionsReceived);
  }

  private void clearVersionList() {
    versionsList.setModel(new DefaultListModel());
    schemaEditor.setText("");
  }

  private List<String> requestVersions(Environment environment, String schemaSubject) {

    List<String> versionList;
    try {
      versionList = schemaRegistryService.getSubjectSchemaVersions(schemaSubject, environment);
    } catch (KajException ex) {
      enqueueException(ex);
      return Collections.emptyList();
    }
    int n = versionList.size();
    enqueueMessage(InfoReportable.buildSuccessfulMessage(n == 0 ? "No hay versiones" : "Hay " + n + " versiones"));

    return versionList;
  }

  public void versionsReceived() {
    if (futureVersions == null) {
      return;
    }

    try {
      versions = futureVersions.get();
      updateVersionList();
    } catch (Throwable ex) {
      printMessage(InfoReportable.buildErrorMessage("Error al recibir las versiones"));
      enqueueException(ex);
    }

    if (versions != null && !versions.isEmpty()) {
      asyncRequestAllSchemas();
    }
  }

  private void updateVersionList() {
    if (versions == null) {
      clearVersionList();
      return;
    }
    DefaultListModel<String> model = new DefaultListModel<>();
    versions.stream().forEach(model::addElement);
    versionsList.setModel(model);
  }

  // Petición de esquemas

  private void asyncRequestAllSchemas() {
    if (versions == null) {
      printMessage(InfoReportable.buildErrorMessage("No hay versiones"));
      return;
    }
    getSchemasButton.setEnabled(false);
    versionsPopupMenu.setEnabled(false);

    String schemaSubject = comboSchemaSubject.getSelectedItem().toString();
    printMessage(InfoReportable.buildActionMessage("Obteniendo todos los esquemas de " + schemaSubject));
    futureSchemas = this.<Map<String, String>>executeAsyncTask(
        () -> requestSchemas(getEnvironment(), schemaSubject, versions), this::schemasReceived);
  }

  public void schemasReceived() {
    if (futureSchemas == null) {
      return;
    }

    try {
      schemas = futureSchemas.get().entrySet().stream()
          .collect(Collectors.toMap(Entry::getKey, entry -> new SchemaInfo(entry.getValue())));
    } catch (Throwable ex) {
      printMessage(InfoReportable.buildErrorMessage("Error al recibir las versiones"));
      printMessage(InfoReportable.buildTraceMessage("[" + ex.getClass().getName() + "] " + ex.getMessage()));
    }

    // Se formatean todos los esquemas
    schemas.forEach((k, v) -> v.content = JsonUtils.formatJson(v.content));
  }

  // Comparación de esquema con el anterior
  private void compareSelectedSchemaVersionWithPrevious() {
    if (versions == null || schemas == null) {
      // TODO: mensaje
      return;
    }
    int selectedIndex = versionsList.getSelectedIndex();
    if (selectedIndex <= 0 || selectedIndex >= versions.size()) {
      // TODO: mensaje
      return;
    }
    String selectedVersion = versions.get(selectedIndex);
    String selectedSchema = schemas.get(selectedVersion).content;
    String previousVersion = versions.get(selectedIndex - 1);
    String previousSchema = schemas.get(previousVersion).content;

    printMessage(
        InfoReportable.buildActionMessage("Comparando la versión " + selectedVersion + " con la " + previousVersion));

    enqueueTextDifferences(" Versión " + selectedVersion, selectedSchema,
        " Versión " + previousVersion, previousSchema);
  }

  private Map<String, String> requestSchemas(Environment environment, String schemaSubject,
      List<String> versions) {
    if (versions == null || versions.isEmpty()) {
      enqueueMessage(InfoReportable.buildTraceMessage("No hay ninguna versión"));
      return Collections.<String, String>emptyMap();
    }

    Map<String, String> schemas = versions.stream()
        .collect(Collectors.toMap(Function.identity(),
            version -> getSubjectSchemaVersion(schemaSubject, version, environment)));

    enqueueMessage(InfoReportable.buildSuccessfulMessage("Esquemas obtenidos: " + versions.size()));

    return schemas;
  }

  private String getSubjectSchemaVersion(String schemaSubject, String version, Environment environment) {
    try {
      return schemaRegistryService.getSubjectSchemaVersion(schemaSubject, version, environment);
    } catch (KajException e) {
      enqueueException(e);
      return "";
    }
  }

  // Borrado de esquemas
  private void asyncDeleteSelectedSchemaVersion() {

    String version = Objects.toString(versionsList.getSelectedValue());
    if (StringUtils.isBlank(version)) {
      return;
    }

    Environment environment = getEnvironment();
    String schemaSubject = Objects.toString(comboSchemaSubject.getSelectedItem());

    int response = JOptionPane.showConfirmDialog(contentPane,
        "!CUIDADO! Se va a borrar la versión " + version + " del esquema de " + schemaSubject + "\n" +
            "¿Esta seguro de lo que lo quiere borrar?",
        "Atención", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (response != JOptionPane.YES_OPTION) {
      return;
    }

    printMessage(InfoReportable.buildActionMessage("Borrando la versión " + version + " de " + schemaSubject));
    executeAsyncTask(() -> deleteSelectedSchemaVersion(environment, schemaSubject, version),
        this::updateVersionList);
  }

  private Void deleteSelectedSchemaVersion(Environment environment, String schemaSubject,
      String version) {
    try {
      schemaRegistryService
          .deleteSubjectSchemaVersion(schemaSubject, version, environment);
      enqueueMessage(InfoReportable.buildSuccessfulMessage("Se ha borrado correctamente la versión " + version));
      versions.remove(version);
    } catch (KajException ex) {
      enqueueException(ex);
    }
    return null;
  }

  // Escritura de esquemas
  private void asyncWriteSchema() {

    Environment environment = getEnvironment();
    String schemaSubject = comboSchemaSubject.getSelectedItem().toString();
    if (StringUtils.isBlank(schemaSubject)) {
      JOptionPane.showMessageDialog(contentPane, "No ha especificado ningún sujeto", "Atención",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    String jsonSchema = schemaEditor.getText();
    if (StringUtils.isBlank(jsonSchema)) {
      JOptionPane.showMessageDialog(contentPane, "No ha especificado ningún esquema", "Atención",
          JOptionPane.ERROR_MESSAGE);
      return;
    }

    int response = JOptionPane.showConfirmDialog(contentPane,
        "!CUIDADO! Se va a escribir una nueva versión del esquema de " + schemaSubject + "\n" +
            "¿Esta seguro de que quiere substituir la versión actual?",
        "Atención", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (response != JOptionPane.YES_OPTION) {
      return;
    }

    printMessage(InfoReportable.buildActionMessage("Escritura de nueva versión esquema de " + schemaSubject));
    executeAsyncTask(() -> writeSchema(environment, schemaSubject, jsonSchema),
        this::updateVersionList);
  }

  private Void writeSchema(Environment environment, String schemaSubject, String jsonSchema) {
    try {
      schemaRegistryService.writeSubjectSchema(schemaSubject, environment, jsonSchema);
      enqueueMessage(InfoReportable.buildSuccessfulMessage("Se ha escrito correctamente la nueva versión "));
    } catch (KajException e) {
      enqueueException(e);
    }
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
    contentPane.setLayout(new GridLayoutManager(4, 2, new Insets(10, 10, 10, 10), -1, -1));
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel1,
        new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboSchemaSubject = new JComboBox();
    comboSchemaSubject.setEditable(true);
    final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
    comboSchemaSubject.setModel(defaultComboBoxModel1);
    panel1.add(comboSchemaSubject,
        new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label1 = new JLabel();
    this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages", "label.subject"));
    panel1.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label2 = new JLabel();
    this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("messages", "label.environment"));
    panel1.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboEnvironment = new JComboBox();
    final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
    comboEnvironment.setModel(defaultComboBoxModel2);
    panel1.add(comboEnvironment,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label3 = new JLabel();
    this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("messages", "label.domain"));
    panel1.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
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
    final JLabel label4 = new JLabel();
    label4.setIcon(new ImageIcon(getClass().getResource("/images/search.png")));
    label4.setText("");
    panel2.add(label4, BorderLayout.WEST);
    searchTextField = new JTextField();
    panel2.add(searchTextField, BorderLayout.CENTER);
    final JPanel panel3 = new JPanel();
    panel3.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel3, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, null, null, 0, false));
    final Spacer spacer1 = new Spacer();
    panel3.add(spacer1, new GridConstraints(0, 4, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    getSchemasButton = new JButton();
    this.$$$loadButtonText$$$(getSchemasButton, this.$$$getMessageFromBundle$$$("messages", "button.get.schemas"));
    getSchemasButton.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.get.schemas"));
    panel3.add(getSchemasButton,
        new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    final Spacer spacer2 = new Spacer();
    panel3.add(spacer2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    dangerLabel = new JLabel();
    dangerLabel.setIcon(new ImageIcon(getClass().getResource("/images/danger.png")));
    dangerLabel.setText("");
    dangerLabel.setToolTipText("Cuidado, no estas en el entorno local");
    panel3.add(dangerLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    writeSchemaButton = new JButton();
    this.$$$loadButtonText$$$(writeSchemaButton, this.$$$getMessageFromBundle$$$("messages", "button.write.schema"));
    writeSchemaButton.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.write.schema"));
    panel3.add(writeSchemaButton,
        new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JPanel panel4 = new JPanel();
    panel4.setLayout(new BorderLayout(0, 0));
    contentPane.add(panel4, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    final JPanel panel5 = new JPanel();
    panel5.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
    panel4.add(panel5, BorderLayout.WEST);
    final JLabel label5 = new JLabel();
    this.$$$loadLabelText$$$(label5, this.$$$getMessageFromBundle$$$("messages", "label.versions"));
    panel5.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, new Dimension(80, -1), new Dimension(80, -1), 0, false));
    final JScrollPane scrollPane1 = new JScrollPane();
    panel5.add(scrollPane1,
        new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(80, -1),
            new Dimension(80, -1), 0, false));
    versionsList = new JList();
    scrollPane1.setViewportView(versionsList);
    tabbedPane = new JTabbedPane();
    panel4.add(tabbedPane, BorderLayout.CENTER);
    tabInfo = new JPanel();
    tabInfo.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "tab.info"), tabInfo);
    final JScrollPane scrollPane2 = new JScrollPane();
    tabInfo.add(scrollPane2, BorderLayout.CENTER);
    infoTextPane.setBackground(new Color(-13948117));
    infoTextPane.setCaretColor(new Color(-1));
    infoTextPane.setEditable(false);
    Font infoTextPaneFont = this.$$$getFont$$$("Consolas", -1, 12, infoTextPane.getFont());
    if (infoTextPaneFont != null) {
      infoTextPane.setFont(infoTextPaneFont);
    }
    infoTextPane.setForeground(new Color(-1));
    infoTextPane.putClientProperty("charset", "");
    scrollPane2.setViewportView(infoTextPane);
    tabSchema = new JPanel();
    tabSchema.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "tab.avro.schema"), tabSchema);
    tabSchema.add(schemaScrollPane, BorderLayout.CENTER);
    final JPanel panel6 = new JPanel();
    panel6.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
    panel4.add(panel6, BorderLayout.EAST);
    cleanButton = new JButton();
    cleanButton.setHorizontalTextPosition(0);
    cleanButton.setIcon(new ImageIcon(getClass().getResource("/images/rubber.png")));
    cleanButton.setText("");
    cleanButton.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.clean"));
    cleanButton.putClientProperty("html.disable", Boolean.TRUE);
    panel6.add(cleanButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, null, null, 0, false));
    copyButton = new JButton();
    copyButton.setHorizontalTextPosition(0);
    copyButton.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
    copyButton.setText("");
    copyButton.setToolTipText(this.$$$getMessageFromBundle$$$("messages", "tooltip.copy.clipboard"));
    panel6.add(copyButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    final Spacer spacer3 = new Spacer();
    panel6.add(spacer3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1,
        GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    final Spacer spacer4 = new Spacer();
    panel6.add(spacer4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, 1,
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

  @Override
  public InfoTextPane getInfoTextPane() {
    return (InfoTextPane) infoTextPane;
  }

  @Override
  protected void asyncTaskFinished() {
    getSchemasButton.setEnabled(true);
    versionsPopupMenu.setEnabled(true);
  }

  @Override
  public Optional<JTextComponent> getCurrentEditor() {
    int index = tabbedPane.getSelectedIndex();
    if (index == 0) {
      return Optional.of(infoTextPane);
    } else if (index == 1) {
      return Optional.of(schemaEditor);
    } else {
      return Optional.empty();
    }
  }

  @Override
  protected void showConnectionStatus(Boolean ok) {
  }

  @Override
  protected Environment getEnvironment() {
    return (Environment) comboEnvironment.getSelectedItem();
  }

  private void createUIComponents() {
    schemaEditor = createJsonEditor();
    schemaScrollPane = componentFactory.createEditorScroll(schemaEditor);
    infoTextPane = new InfoTextPane();
  }
}
