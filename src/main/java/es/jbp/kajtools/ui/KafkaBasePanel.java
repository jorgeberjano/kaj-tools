package es.jbp.kajtools.ui;

import es.jbp.kajtools.Environment;
import es.jbp.kajtools.IMessageClient;
import es.jbp.kajtools.kafka.KafkaInvestigator;
import es.jbp.kajtools.KajException;
import es.jbp.tabla.ModeloTablaGenerico;
import es.jbp.kajtools.kafka.TopicItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import lombok.Getter;

public abstract class KafkaBasePanel extends BasePanel {

  protected final List<IMessageClient> clientList;

  protected final ImageIcon iconCheckOk = new ImageIcon(getClass().getResource("/images/check_green.png"));
  protected final ImageIcon iconCheckFail = new ImageIcon(getClass().getResource("/images/check_red.png"));
  protected final ImageIcon iconCheckUndefined = new ImageIcon(getClass().getResource("/images/check_grey.png"));

  @Getter
  protected List<TopicItem> topics;

  public KafkaBasePanel(ComponentFactory componentFactory, List<IMessageClient> clientList) {
    super(componentFactory);
    this.clientList = clientList;
  }

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

    showInModalDialog(tableSelectorPanel, "Topics");

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


  protected Optional<JTextComponent> getUmpteenthEditor(int index, JTextComponent... editors) {
    if (editors.length > index) {
      return Optional.of(editors[index]);
    } else {
      return Optional.empty();
    }
  }

}
