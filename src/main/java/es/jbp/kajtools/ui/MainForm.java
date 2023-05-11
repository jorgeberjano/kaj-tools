package es.jbp.kajtools.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import es.jbp.kajtools.IMessageClient;
import es.jbp.kajtools.i18n.I18nService;
import es.jbp.kajtools.kafka.KafkaAdminService;
import es.jbp.kajtools.ksqldb.KSqlDbService;
import es.jbp.kajtools.schemaregistry.SchemaRegistryService;
import java.awt.Dimension;
import java.awt.Insets;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import lombok.Getter;

public class MainForm {

  private final SchemaRegistryService schemaRegistryService;
  private final KafkaAdminService kafkaAdmin;
  private final UiComponentCreator componentFactory;
  private final List<IMessageClient> clientList;
  private final I18nService i18nService;
  private final KSqlDbService kSqlDbService;

  private JTabbedPane tabbedPane;
  @Getter
  private JPanel contentPane;
  private JPanel panelProducer;
  private JPanel panelJson;
  private JPanel panelSchema;
  private JPanel panelConsumer;
  private JPanel panelScript;
  private JPanel panelKSqlDb;

  public MainForm(UiComponentCreator componentFactory,
                  SchemaRegistryService schemaRegistryService,
                  KafkaAdminService kafkaAdmin,
                  List<IMessageClient> clientList,
                  I18nService i18nService,
                  KSqlDbService kSqlDbService) {
    this.componentFactory = componentFactory;
    this.schemaRegistryService = schemaRegistryService;
    this.kafkaAdmin = kafkaAdmin;
    this.clientList = clientList;
    this.i18nService = i18nService;
    this.kSqlDbService = kSqlDbService;

// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
    $$$setupUI$$$();
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
    contentPane.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.putClientProperty("html.disable", Boolean.FALSE);
    tabbedPane = new JTabbedPane();
    tabbedPane.setTabPlacement(2);
    contentPane.add(tabbedPane,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200),
            null, 0, false));
    tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "producer.tab"),
        new ImageIcon(getClass().getResource("/images/kafka.png")), panelProducer,
        this.$$$getMessageFromBundle$$$("messages", "producer.tooltip"));
    panelProducer.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
        this.$$$getMessageFromBundle$$$("messages", "producer.title"), TitledBorder.DEFAULT_JUSTIFICATION,
        TitledBorder.DEFAULT_POSITION, null, null));
    tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "consumer.tab"),
        new ImageIcon(getClass().getResource("/images/akfak.png")), panelConsumer,
        this.$$$getMessageFromBundle$$$("messages", "consumer.tooltip"));
    panelConsumer.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
        this.$$$getMessageFromBundle$$$("messages", "consumer.title"), TitledBorder.DEFAULT_JUSTIFICATION,
        TitledBorder.DEFAULT_POSITION, null, null));
    tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "schema.tab"),
        new ImageIcon(getClass().getResource("/images/schemaregistry.png")), panelSchema,
        this.$$$getMessageFromBundle$$$("messages", "schema.tooltip"));
    panelSchema.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0),
        this.$$$getMessageFromBundle$$$("messages", "schema.title"), TitledBorder.DEFAULT_JUSTIFICATION,
        TitledBorder.DEFAULT_POSITION, null, null));
    tabbedPane.addTab(this.$$$getMessageFromBundle$$$("messages", "json.tab"),
        new ImageIcon(getClass().getResource("/images/json.png")), panelJson,
        this.$$$getMessageFromBundle$$$("messages", "json.tooltip"));
    panelJson.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0),
        this.$$$getMessageFromBundle$$$("messages", "json.title"), TitledBorder.DEFAULT_JUSTIFICATION,
        TitledBorder.DEFAULT_POSITION, null, null));
    tabbedPane.addTab("Script", new ImageIcon(getClass().getResource("/images/script.png")), panelScript);
    panelScript.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(),
        this.$$$getMessageFromBundle$$$("messages", "producer.title"), TitledBorder.DEFAULT_JUSTIFICATION,
        TitledBorder.DEFAULT_POSITION, null, null));
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
  public JComponent $$$getRootComponent$$$() {
    return contentPane;
  }

  private void createUIComponents() {
    panelProducer =
        new KafkaProducerPanel(clientList, schemaRegistryService, kafkaAdmin, componentFactory, i18nService)
            .getContentPane();
    panelConsumer =
        new KafkaConsumerPanel(clientList, schemaRegistryService, kafkaAdmin, componentFactory, i18nService)
            .getContentPane();
    panelSchema =
        new SchemaRegistryPanel(clientList, schemaRegistryService, kafkaAdmin, componentFactory, i18nService)
            .getContentPane();
    panelJson = new JsonGeneratorPanel(componentFactory, i18nService).getContentPane();

    panelScript = new ScriptPanel(clientList, schemaRegistryService, kafkaAdmin, componentFactory, i18nService)
        .getContentPane();

    panelKSqlDb = new KSqlDbPanel(kSqlDbService, componentFactory, i18nService).getContentPane();
  }
}
