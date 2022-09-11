package es.jbp.kajtools.ui;

import com.google.common.primitives.Ints;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import es.jbp.kajtools.ui.interfaces.DialogueablePanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import lombok.Getter;
import org.apache.commons.lang3.math.NumberUtils;


public class CreateTopicPanel implements DialogueablePanel {

  private JPanel panelForm;
  private JTextField textFieldTopic;
  private JTextField textFieldPartitions;
  private JTextField textFieldReplicas;
  private JButton buttonOk;
  private JButton buttonCancel;
  private Window dialog;

  @Getter
  private String topic;
  @Getter
  private Optional<Integer> partitions = Optional.empty();
  @Getter
  private Optional<Short> replicas = Optional.empty();

  public CreateTopicPanel() {
    buttonOk.addActionListener(e -> {
      topic = textFieldTopic.getText();
      partitions = Optional.of(textFieldPartitions.getText()).filter(NumberUtils::isParsable).map(NumberUtils::toInt);
      replicas = Optional.of(textFieldPartitions.getText()).filter(NumberUtils::isParsable).map(NumberUtils::toShort);
      dialog.setVisible(false);

    });
    buttonCancel.addActionListener(e -> dialog.setVisible(false));
  }

  @Override
  public JPanel getMainPanel() {
    return panelForm;
  }

  @Override
  public void bindDialog(Window dialog) {
    this.dialog = dialog;
  }

  {
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
    panelForm = new JPanel();
    panelForm.setLayout(new BorderLayout(0, 0));
    panelForm.setBorder(BorderFactory
        .createTitledBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), null, TitledBorder.DEFAULT_JUSTIFICATION,
            TitledBorder.DEFAULT_POSITION, null, null));
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:d:grow",
        "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
    panelForm.add(panel1, BorderLayout.CENTER);
    final JLabel label1 = new JLabel();
    this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("messages", "label.partitions"));
    CellConstraints cc = new CellConstraints();
    panel1.add(label1, cc.xy(1, 3));
    final JLabel label2 = new JLabel();
    this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("messages", "label.replicas"));
    panel1.add(label2, cc.xy(1, 5));
    final JLabel label3 = new JLabel();
    this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("messages", "label.topic"));
    panel1.add(label3, cc.xy(1, 1));
    textFieldTopic = new JTextField();
    panel1.add(textFieldTopic, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
    textFieldPartitions = new JTextField();
    panel1.add(textFieldPartitions, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
    textFieldReplicas = new JTextField();
    panel1.add(textFieldReplicas, cc.xy(3, 5, CellConstraints.FILL, CellConstraints.DEFAULT));
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
    panelForm.add(panel2, BorderLayout.SOUTH);
    buttonOk = new JButton();
    this.$$$loadButtonText$$$(buttonOk, this.$$$getMessageFromBundle$$$("messages", "button.ok"));
    panel2.add(buttonOk);
    buttonCancel = new JButton();
    this.$$$loadButtonText$$$(buttonCancel, this.$$$getMessageFromBundle$$$("messages", "button.cancel"));
    panel2.add(buttonCancel);
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
    return panelForm;
  }

}
