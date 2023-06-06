package es.jbp.kajtools.ui;

import es.jbp.kajtools.Environment;
import es.jbp.kajtools.IMessageClient;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.i18n.I18nService;
import es.jbp.kajtools.kafka.KafkaAdminService;
import es.jbp.kajtools.kafka.TopicItem;
import es.jbp.kajtools.schemaregistry.ISchemaRegistryService;
import es.jbp.kajtools.ui.InfoDocument.Type;
import es.jbp.kajtools.ui.interfaces.InfoReportable;
import es.jbp.tabla.ModeloTablaGenerico;
import java.awt.Component;
import java.awt.Point;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class KafkaBasePanel extends BasePanel {

  protected final List<IMessageClient> clientList;
  protected final ISchemaRegistryService schemaRegistryService;
  protected final KafkaAdminService kafkaAdminService;

  protected final ImageIcon iconCheckOk = new ImageIcon(getClass().getResource("/images/check_green.png"));
  protected final ImageIcon iconCheckFail = new ImageIcon(getClass().getResource("/images/check_red.png"));
  protected final ImageIcon iconCheckUndefined = new ImageIcon(getClass().getResource("/images/check_grey.png"));

  @Getter
  protected List<TopicItem> topics;

  protected KafkaBasePanel(List<IMessageClient> clientList,
      ISchemaRegistryService schemaRegistryService,
      KafkaAdminService kafkaAdminService,
      UiComponentCreator componentFactory) {
    super(componentFactory);
    this.clientList = clientList;
    this.schemaRegistryService = schemaRegistryService;
    this.kafkaAdminService = kafkaAdminService;
  }

  protected abstract Environment getEnvironment();

  protected void asyncRetrieveTopics() {

    printMessage(InfoReportable.buildActionMessage("Obteniendo la lista de topics"));
    executeAsyncTask(() -> retrieveTopics(getEnvironment()));
  }

  protected Void retrieveTopics(Environment environment) {
    try {
      topics = kafkaAdminService.getTopics(environment);
      showConnectionStatus(Boolean.TRUE);
      printMessage(InfoReportable.buildSuccessfulMessage(
          "Se han obtenido " + CollectionUtils.size(topics) + " " + "topics"));
    } catch (KajException ex) {
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

  protected TopicItem selectTopic() {

    final TableSelectorPanel<TopicItem> tableSelectorPanel = new TableSelectorPanel<>(this::createTopicModel);
    JPopupMenu popupMenu = new JPopupMenu() {
      @Override
      public void show(Component invoker, int x, int y) {
        int row = tableSelectorPanel.getTable().rowAtPoint(new Point(x, y));
        if (row >= 0) {
          tableSelectorPanel.getTable().getSelectionModel().setSelectionInterval(row, row);
          super.show(invoker, x, y);
        }
      }
    };

    var configActionListener = new JMenuItem("Configuración");
    configActionListener.addActionListener(e -> showTopicConfig(tableSelectorPanel));
    popupMenu.add(configActionListener);

    var createActionListener = new JMenuItem("Crear");
    createActionListener.addActionListener(e -> asyncCreateTopic());
    popupMenu.add(createActionListener);
    tableSelectorPanel.setTablePopupMenu(popupMenu);

    var deleteActionListener = new JMenuItem("Borrar");
    deleteActionListener.addActionListener(e -> asyncDeleteTopic(tableSelectorPanel));
    popupMenu.add(deleteActionListener);
    tableSelectorPanel.setTablePopupMenu(popupMenu);

    showInModalDialog(tableSelectorPanel, "Topics", null);

    return tableSelectorPanel.getAcceptedItem();
  }

  protected void showTopicConfig(TableSelectorPanel<TopicItem> tableSelectorPanel) {
    String topicName = Optional.ofNullable(tableSelectorPanel.getSelectedItem())
        .map(TopicItem::getName)
        .orElse(null);
    if (StringUtils.isBlank(topicName)) {
      return;
    }
    Map<String, String> config = kafkaAdminService.getTopicConfig(topicName, getEnvironment());
    String propertiesText = config.entrySet().stream()
        .sorted((e1, e2) -> StringUtils.compare(e1.getKey(), e2.getKey()))
        .map(e -> e.getKey() + " = " + e.getValue())
        .collect(Collectors.joining("\n"));
    showInfoDocument(InfoDocument.simpleDocument(topicName, Type.PROPERTIES, propertiesText), true,
        tableSelectorPanel.getMainPanel());
  }


  protected void asyncDeleteTopic(TableSelectorPanel<TopicItem> tableSelectorPanel) {
    String topicName = Optional.ofNullable(tableSelectorPanel.getSelectedItem())
        .map(TopicItem::getName)
        .orElse(null);
    if (StringUtils.isBlank(topicName)) {
      return;
    }
    var environment = getEnvironment();
    int response = JOptionPane.showConfirmDialog(tableSelectorPanel.getTable(),
        "!CUIDADO! Se va a borrar el topic " + topicName + " del entono " + environment.getName() +
            "\n¿Esta seguro de lo que lo quiere borrar?",
        "Atención", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (response != JOptionPane.YES_OPTION) {
      return;
    }
    printMessage(InfoReportable.buildActionMessage(
        "Borrando el topic " + topicName + " del entono " + environment.getName()));

    // TODO: hacer esto asincrono
    var kafka = new KafkaAdminService();
    try {
      kafka.deleteTopic(topicName, environment);
      printMessage(InfoReportable.buildSuccessfulMessage("Se ha borrado el topic correctamente"));
    } catch (KajException ex) {
      printException(ex);
    }
  }

  protected void asyncCreateTopic() {

    var createTopicPanel = new CreateTopicPanel();
    showInModalDialog(createTopicPanel, "Topics", null);
    String topicName = createTopicPanel.getTopic();
    if (StringUtils.isBlank(topicName)) {
        return;
    }
    Optional<Integer> partitions = createTopicPanel.getPartitions();
    Optional<Short> replicas = createTopicPanel.getReplicas();

    var environment = getEnvironment();
    printMessage(InfoReportable.buildActionMessage(
        "Creando el topic " + topicName + " del entono " + environment.getName()));

    // TODO: hacer esto asincrono
    var kafka = new KafkaAdminService();
    try {
      kafka.createTopic(topicName, partitions, replicas, environment);
      printMessage(InfoReportable.buildSuccessfulMessage("Se ha creado el topic correctamente"));
    } catch (KajException ex) {
      printException(ex);
    }
  }

  public Map<String, String> createVariableMap(String text) {
    Properties properties = new Properties();
    try {
      properties.load(new StringReader(text));
    } catch (IOException e) {
      printMessage(InfoReportable.buildErrorMessage("No se han podido cargar las variables"));
      printException(e);
    }
    Map<String, String> variables = new HashMap<>();
    properties.forEach((k, v) -> variables.put(Objects.toString(k), Objects.toString(v)));
    return variables;
  }

}
