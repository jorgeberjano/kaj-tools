package es.jbp.kajtools.ui;

import com.google.common.collect.Lists;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import es.jbp.kajtools.Environment;
import es.jbp.kajtools.EnvironmentConfiguration;
import es.jbp.kajtools.IProducer;
import es.jbp.kajtools.ui.InfoMessage.Type;
import es.jbp.kajtools.KajToolsApp;
import es.jbp.kajtools.util.JsonUtils;
import es.jbp.kajtools.util.SchemaRegistryService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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


  public SchemaRegistryPanel() {

    $$$setupUI$$$();

    dangerLabel.setVisible(false);

    // Combo Entorno
    EnvironmentConfiguration.ENVIRONMENT_LIST.stream().forEach(comboEnvironment::addItem);
    comboEnvironment.addActionListener(e -> {
      boolean local = getEnvironment().getName().toLowerCase().contains("local");
      dangerLabel.setVisible(!local);
    });

    // Combo Dominio
    final List<IProducer> producerList = KajToolsApp.getInstance().getProducerList();
    producerList.stream().map(IProducer::getDomain).distinct().forEach(comboDomain::addItem);
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

    JMenuItem deleteActionListener = new JMenuItem("Borrar");
    deleteActionListener.addActionListener(e -> asyncDeleteSelectedSchemaVersion());
    JMenuItem compareActionListener = new JMenuItem("Comparar con anterior");
    compareActionListener.addActionListener(e -> compareSelectedSchemaVersionWithPrevious());
    versionsPopupMenu.add(deleteActionListener);
    versionsPopupMenu.add(compareActionListener);
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
    final List<IProducer> producerList = KajToolsApp.getInstance().getProducerList();
    producerList.stream()
        .filter(p -> StringUtils.isBlank(domain) || domain.equals(p.getDomain()))
        .map(IProducer::getAvailableTopics)
        .flatMap(List::stream)
        .map(s -> Lists.newArrayList(s + "-key", s + "-value"))
        .flatMap(List::stream)
        .forEach(comboSchemaSubject::addItem);
  }

  // Petición de versiones

  private void asyncRequestVersions() {
    clearVersionList();

    String schemaSubject = Objects.toString(comboSchemaSubject.getSelectedItem());
    printAction("Obteniendo las versiones de los esquemas de " + schemaSubject);
    futureVersions = this.<List<String>>executeAsyncTask(
        () -> requestVersions(getEnvironment(), schemaSubject), this::versionsReceived);
  }

  private void clearVersionList() {
    versionsList.setModel(new DefaultListModel());
    schemaEditor.setText("");
  }

  private List<String> requestVersions(Environment environment, String schemaSubject) {
    SchemaRegistryService schemaRegistryService = KajToolsApp.getInstance().getSchemaRegistryService();

    List<String> versionList;
    try {
      versionList = schemaRegistryService
          .getSubjectSchemaVersions(schemaSubject, environment);
    } catch (Throwable ex) {
      enqueueError("No se han podido obtener las versiones de los esquemas de " + schemaSubject);
      enqueueInfo("[" + ex.getClass().getName() + "] " + ex.getMessage());
      return Collections.emptyList();
    }
    int n = versionList.size();
    enqueueSuccessful(n == 0 ? "No hay versiones" : "Hay " + n + " versiones");

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
      printError("Error al recibir las versiones");
      printInfo("[" + ex.getClass().getName() + "] " + ex.getMessage());
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
      printError("No hay versiones");
      return;
    }
    String schemaSubject = comboSchemaSubject.getSelectedItem().toString();

    printAction("Obteniendo todos los esquemas de " + schemaSubject);
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
      printError("Error al recibir las versiones");
      printInfo("[" + ex.getClass().getName() + "] " + ex.getMessage());
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

    printAction("Comparando la versión " + selectedVersion + " con la " + previousVersion);
    printText(" ", Type.ADDED);
    printInfo(" Versión " + selectedVersion);
    printText(" ", Type.DELETED);
    printInfo(" Versión " + previousVersion);
    enqueueTextDifferences(previousSchema, selectedSchema);
    flushMessages();
  }

  private Map<String, String> requestSchemas(Environment environment, String schemaSubject,
      List<String> versions) {
    if (versions == null || versions.isEmpty()) {
      enqueueInfo("No hay ninguna versión");
      return Collections.<String, String>emptyMap();
    }

    SchemaRegistryService schemaRegistryService = KajToolsApp.getInstance().getSchemaRegistryService();
    Map<String, String> schemas = versions.stream()
        .collect(Collectors.toMap(Function.identity(),
            version -> schemaRegistryService
                .getSubjectSchemaVersion(schemaSubject, version, environment)));

    enqueueSuccessful("Esquemas obtenidos: " + versions.size());

    return schemas;
  }

  // Borrado de esquemas
  private void asyncDeleteSelectedSchemaVersion() {

    String version =  Objects.toString(versionsList.getSelectedValue());
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

    printAction("Borrando la versión " + version + " de " + schemaSubject);
    executeAsyncTask(() -> deleteSelectedSchemaVersion(environment, schemaSubject, version),
        this::updateVersionList);
  }

  private Void deleteSelectedSchemaVersion(Environment environment, String schemaSubject,
      String version) {
    SchemaRegistryService schemaRegistryService = KajToolsApp.getInstance().getSchemaRegistryService();
    schemaRegistryService
        .deleteSubjectSchemaVersion(schemaSubject, version, environment);

    enqueueSuccessful("Se ha borrado correctamente la versión " + version);
    versions.remove(version);
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



    printAction("Escritura de nueva versión esquema de " + schemaSubject);
    executeAsyncTask(() -> writeSchema(environment, schemaSubject, jsonSchema),
        this::updateVersionList);
  }

  private Void writeSchema(Environment environment, String schemaSubject, String jsonSchema) {
    SchemaRegistryService schemaRegistryService = KajToolsApp.getInstance().getSchemaRegistryService();
    schemaRegistryService.writeSubjectSchema(schemaSubject, environment, jsonSchema);

    enqueueSuccessful("Se ha escrito correctamente la nueva versión ");
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
    contentPane.setLayout(new GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel1,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL,
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
    label1.setText("Sujeto:");
    panel1.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label2 = new JLabel();
    label2.setText("Entorno:");
    panel1.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboEnvironment = new JComboBox();
    final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
    comboEnvironment.setModel(defaultComboBoxModel2);
    panel1.add(comboEnvironment,
        new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label3 = new JLabel();
    label3.setText("Dominio:");
    panel1.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    comboDomain = new JComboBox();
    panel1.add(comboDomain,
        new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new BorderLayout(0, 0));
    contentPane.add(panel2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, null, null, 0, false));
    final JLabel label4 = new JLabel();
    label4.setIcon(new ImageIcon(getClass().getResource("/images/search.png")));
    label4.setText("");
    panel2.add(label4, BorderLayout.WEST);
    searchTextField = new JTextField();
    panel2.add(searchTextField, BorderLayout.CENTER);
    final JPanel panel3 = new JPanel();
    panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    tabbedPane = new JTabbedPane();
    panel3.add(tabbedPane, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200),
        null, 0, false));
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
    tabSchema = new JPanel();
    tabSchema.setLayout(new BorderLayout(0, 0));
    tabbedPane.addTab("Esquema", tabSchema);
    tabSchema.add(schemaScrollPane, BorderLayout.CENTER);
    final JPanel panel4 = new JPanel();
    panel4.setLayout(new BorderLayout(0, 0));
    panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        null, new Dimension(60, -1), null, 0, false));
    final JLabel label5 = new JLabel();
    label5.setText("Versiones");
    panel4.add(label5, BorderLayout.NORTH);
    final JScrollPane scrollPane2 = new JScrollPane();
    panel4.add(scrollPane2, BorderLayout.CENTER);
    versionsList = new JList();
    scrollPane2.setViewportView(versionsList);
    final JPanel panel5 = new JPanel();
    panel5.setLayout(new GridLayoutManager(1, 7, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED,
        null, null, null, 0, false));
    cleanButton = new JButton();
    cleanButton.setHorizontalTextPosition(0);
    cleanButton.setIcon(new ImageIcon(getClass().getResource("/images/rubber.png")));
    cleanButton.setText("");
    cleanButton.setToolTipText("Limpiar");
    cleanButton.putClientProperty("html.disable", Boolean.TRUE);
    panel5.add(cleanButton,
        new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final Spacer spacer1 = new Spacer();
    panel5.add(spacer1, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    copyButton = new JButton();
    copyButton.setHorizontalTextPosition(0);
    copyButton.setIcon(new ImageIcon(getClass().getResource("/images/copy.png")));
    copyButton.setText("");
    copyButton.setToolTipText("Copiar al portapapeles");
    panel5.add(copyButton, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
        GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    getSchemasButton = new JButton();
    getSchemasButton.setText("Obtener esquemas");
    getSchemasButton.setToolTipText("Obtiene todos los esquemas del Schema Registry ");
    panel5.add(getSchemasButton,
        new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    final Spacer spacer2 = new Spacer();
    panel5.add(spacer2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
        GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    dangerLabel = new JLabel();
    dangerLabel.setIcon(new ImageIcon(getClass().getResource("/images/danger.png")));
    dangerLabel.setText("");
    dangerLabel.setToolTipText("Cuidado, no estas en el entorno local");
    panel5.add(dangerLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    writeSchemaButton = new JButton();
    writeSchemaButton.setText("Escribir esquema");
    panel5.add(writeSchemaButton,
        new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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

  @Override
  protected JTextPane getInfoTextPane() {
    return infoTextPane;
  }

  @Override
  protected void enableButtons(boolean enable) {
    getSchemasButton.setEnabled(enable);
    versionsPopupMenu.setEnabled(enable);
  }

  @Override
  protected Optional<JTextComponent> getCurrentEditor() {
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
  protected void showConnectionStatus(boolean ok) {
  }

  @Override
  protected Environment getEnvironment() {
    return (Environment) comboEnvironment.getSelectedItem();
  }

  private void createUIComponents() {
    schemaEditor = createJsonEditor();
    schemaScrollPane = createEditorScroll(schemaEditor);
  }
}
