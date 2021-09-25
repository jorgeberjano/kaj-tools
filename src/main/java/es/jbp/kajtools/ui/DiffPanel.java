package es.jbp.kajtools.ui;

import es.jbp.kajtools.ui.InfoMessage.Type;
import es.jbp.kajtools.ui.interfaces.DialogueablePanel;
import java.awt.BorderLayout;
import java.awt.Window;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import org.apache.commons.lang3.tuple.Pair;

public class DiffPanel implements DialogueablePanel {

  private JPanel mainPanel;
  private InfoTextPane leftTextPane;
  private InfoTextPane rightTextPane;
  private JScrollPane leftScroll;
  private JScrollPane rightScroll;

  public DiffPanel() {
    $$$setupUI$$$();
    rightScroll.getVerticalScrollBar().setModel(leftScroll.getVerticalScrollBar().getModel());

    int height = leftTextPane.getFontMetrics(leftTextPane.getFont()).getHeight();
    rightScroll.getVerticalScrollBar().setUnitIncrement(height);
    leftScroll.getVerticalScrollBar().setUnitIncrement(height);

  }

  private void createUIComponents() {
    leftTextPane = new InfoTextPane();
    rightTextPane = new InfoTextPane();

  }

  @Override
  public JPanel getMainPanel() {
    return mainPanel;
  }

  @Override
  public void bindDialog(Window dialog) {
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT edit this method OR call it in your
   * code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    createUIComponents();
    mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout(0, 0));
    final JSplitPane splitPane1 = new JSplitPane();
    splitPane1.setDividerLocation(400);
    splitPane1.setResizeWeight(0.5);
    mainPanel.add(splitPane1, BorderLayout.CENTER);
    leftScroll = new JScrollPane();
    leftScroll.setHorizontalScrollBarPolicy(32);
    leftScroll.setVerticalScrollBarPolicy(22);
    splitPane1.setLeftComponent(leftScroll);
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new BorderLayout(0, 0));
    leftScroll.setViewportView(panel1);
    leftTextPane.setEditable(false);
    panel1.add(leftTextPane, BorderLayout.CENTER);
    rightScroll = new JScrollPane();
    rightScroll.setHorizontalScrollBarPolicy(32);
    rightScroll.setVerticalScrollBarPolicy(22);
    splitPane1.setRightComponent(rightScroll);
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new BorderLayout(0, 0));
    rightScroll.setViewportView(panel2);
    rightTextPane.setEditable(false);
    panel2.add(rightTextPane, BorderLayout.CENTER);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return mainPanel;
  }

  public void setDocument(InfoDocument document) {
    printMessages(document.getLeftMessages(), leftTextPane);
    printMessages(document.getRightMessages(), rightTextPane);
  }

  private void printMessages(List<InfoMessage> messages, InfoTextPane textPane) {
    Set<Pair<Integer, Integer>> highlightPositions = new TreeSet<>();

    for (InfoMessage message : messages) {
      int position = textPane.getCaretPosition();
      textPane.printInfoMessage(message);
      if (message.getType() == Type.DELETED || message.getType() == Type.ADDED || message.getType() == Type.MISSING) {
        highlightPositions.add(Pair.of(position, textPane.getCaretPosition()));
      }
    }
    textPane.highlightLines(highlightPositions);
    textPane.setCaretPosition(0);
  }
}
