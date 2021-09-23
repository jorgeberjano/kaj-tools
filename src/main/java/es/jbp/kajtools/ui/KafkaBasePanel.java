package es.jbp.kajtools.ui;

import es.jbp.kajtools.Environment;
import es.jbp.kajtools.kafka.KafkaInvestigator;
import es.jbp.kajtools.KajException;
import es.jbp.tabla.ModeloTablaGenerico;
import es.jbp.kajtools.kafka.TopicItem;
import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;
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

    final TableSelectorPanel<TopicItem> tableSelectorPanel = new TableSelectorPanel<>(this::createTopicModel);
    JPopupMenu popupMenu = new JPopupMenu() {
      @Override
      public void show(Component invoker, int x, int y) {
        int row = tableSelectorPanel.getTable().rowAtPoint(new Point(x, y));
        if (row >= 0) {
          tableSelectorPanel.getTable().getSelectionModel().setSelectionInterval(row, row);
//          versionsList.setSelectedIndex(row);
          super.show(invoker, x, y);
        }
      }
    };
    JMenuItem deleteActionListener = new JMenuItem("Borrar");
    deleteActionListener.addActionListener(e -> asyncDeleteTopic(tableSelectorPanel));
    popupMenu.add(deleteActionListener);
    tableSelectorPanel.setTablePopupMenu(popupMenu);

    showInModalDialog(tableSelectorPanel, "Topics");

    return tableSelectorPanel.getAcceptedItem();
  }

  protected void asyncDeleteTopic(TableSelectorPanel<TopicItem> tableSelectorPanel) {
    TopicItem item = tableSelectorPanel.getSelectedItem();
    if (item == null) {
      return;
    }
    String topicName = item.getName();
    Environment environment = getEnvironment();
    int response = JOptionPane.showConfirmDialog(tableSelectorPanel.getTable(),
        "!CUIDADO! Se va a borrar el topic " + topicName + " del entono " + environment.getName() +
            "\n¿Esta seguro de lo que lo quiere borrar?",
        "Atención", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (response != JOptionPane.YES_OPTION) {
      return;
    }
    printAction("Borrando el topic " + topicName + " del entono " + environment.getName());
    KafkaInvestigator kafka = new KafkaInvestigator();
    try {
      kafka.deleteTopic(topicName, environment);
    } catch(KajException ex) {
      printException(ex);
    }
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

  protected Optional<JTextComponent> getUmpteenthEditor(int index, JTextComponent... editors) {
    if (editors.length > index) {
      return Optional.of(editors[index]);
    } else {
      return Optional.empty();
    }
  }

}
