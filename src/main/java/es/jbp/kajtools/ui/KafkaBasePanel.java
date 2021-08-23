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
  protected final ImageIcon iconCheckUndefined = new ImageIcon(getClass().getResource("/images/check_grey.png"));

  @Getter
  protected List<TopicItem> topics;

  protected void asyncRetrieveTopics() {

    printAction("Obteniendo la lista de topics");
    executeAsyncTask(() -> retrieveTopics(getEnvironment()));
  }

  protected Void retrieveTopics(Environment environment) {
    KafkaInvestigator kafkaInvestigator = new KafkaInvestigator();
    try {
      topics = kafkaInvestigator.getTopics(environment);
      showConnectionStatus(Boolean.TRUE);
      enqueueSuccessful("Se han obtenido " + topics.size() + " topics");
    } catch(KajException ex) {
      topics = new ArrayList<>();
      printException(ex);
      showConnectionStatus(false);
    }
    return null;
  }

  protected void cleanTopics() {
    topics = null;
    showConnectionStatus(null);
  }

  protected abstract void showConnectionStatus(Boolean b);

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
      retrieveTopics(getEnvironment());
    }
    tableModel.setListaObjetos(topics);
    return tableModel;
  }

}
