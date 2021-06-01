package es.jbp.kajtools;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import lombok.Getter;

public class MainForm {

//  private final List<TestProducer> producerList;
//  private final SchemaRegistryService schemaRegistryService;

  private JTabbedPane tabbedPane;
  @Getter
  private JPanel contentPane;
  private JPanel panelKafka;
  private JPanel panelJson;
  private JPanel panelSchema;

//  public MainForm() {
//    List<TestProducer> producerList,
//      SchemaRegistryService schemaRegistryService) {
//
//    this.producerList = producerList;
//    this.schemaRegistryService = schemaRegistryService;
//  }


  {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
    $$$setupUI$$$();
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
    contentPane.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
    contentPane.putClientProperty("html.disable", Boolean.FALSE);
    tabbedPane = new JTabbedPane();
    tabbedPane.setTabPlacement(2);
    contentPane.add(tabbedPane,
        new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null,
            new Dimension(200, 200), null, 0, false));
    tabbedPane
        .addTab("Kafka", new ImageIcon(getClass().getResource("/images/kafka.png")), panelKafka,
            "Producir eventos Kafka");
    panelKafka.setBorder(BorderFactory
        .createTitledBorder(BorderFactory.createEmptyBorder(), "Kafka producer",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
    tabbedPane.addTab("Schema", new ImageIcon(getClass().getResource("/images/schemaregistry.png")),
        panelSchema);
    panelSchema.setBorder(BorderFactory
        .createTitledBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0), "Schema Registry",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
    tabbedPane.addTab("JSON", new ImageIcon(getClass().getResource("/images/json.png")), panelJson,
        "Generar Json y esquemas");
    panelJson.setBorder(BorderFactory
        .createTitledBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0), "JSON generator",
            TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return contentPane;
  }

  private void createUIComponents() {
    panelKafka = new KafkaTestPanel().getContentPane();
    panelJson = new JsonGeneratorPanel().getContentPane();
    panelSchema = new SchemaRegistryPanel().getContentPane();
  }
}
