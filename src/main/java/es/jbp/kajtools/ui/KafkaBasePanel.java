package es.jbp.kajtools.ui;

import es.jbp.kajtools.Environment;
import es.jbp.kajtools.KafkaInvestigator;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.tabla.ModeloTablaGenerico;
import es.jbp.kajtools.tabla.entities.TopicItem;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JPanel;
import lombok.Getter;

public abstract class KafkaBasePanel extends BasePanel {

  protected final ImageIcon iconCheckOk = new ImageIcon(getClass().getResource("/images/check_green.png"));
  protected final ImageIcon iconCheckFail = new ImageIcon(getClass().getResource("/images/check_red.png"));

  @Getter
  protected List<TopicItem> topics;

  protected void retrieveTopics() {

    KafkaInvestigator kafkaInvestigator = new KafkaInvestigator();
    try {
      topics = kafkaInvestigator.getTopics(getEnvironment());
      showConnectionStatus(true);
    } catch(KajException ex) {
      topics = new ArrayList<>();
      printException(ex);
      showConnectionStatus(false);
    }

  }

  protected abstract void showConnectionStatus(boolean b);

  protected abstract Environment getEnvironment();

  protected TopicItem selectTopic() {

    TableSelectorPanel<TopicItem> tableSelectorPanel = new TableSelectorPanel<>(this::createTopicModel);

    JPanel panel = tableSelectorPanel.getContentPane();
    panel.setBounds(0, 0, 400, 450);
    JDialog dialog = new JDialog();
    dialog.setTitle("Topics");
    dialog.setSize(800, 450);
    dialog.setLocationRelativeTo(getContentPane());
    dialog.setContentPane(panel);
    tableSelectorPanel.bindDialog(dialog);
    dialog.setModal(true);
    dialog.setVisible(true);

    return tableSelectorPanel.getSelectedItem();
  }

  protected ModeloTablaGenerico<TopicItem> createTopicModel(boolean update) {
    ModeloTablaGenerico<TopicItem> tableModel = new ModeloTablaGenerico<>();
    tableModel.agregarColumna("name", "Topic", 200);
    tableModel.agregarColumna("partitions", "Particiones", 20);
    if (update || topics == null) {
      retrieveTopics();
    }
    tableModel.setListaObjetos(topics);
    return tableModel;
  }


}
