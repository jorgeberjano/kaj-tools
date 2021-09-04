package es.jbp.kajtools;

import es.jbp.kajtools.filter.MessageFilter;
import es.jbp.kajtools.tabla.entities.RecordItem;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public interface IMessageClient {

  List<String> getAvailableValues();

  List<String> getAvailableKeys();

  String getDefaultTopic();

  List<String> getAvailableTopics();

  String getValueSchema(String json) throws KajException;

  String getKeySchema(String json) throws KajException;

  void sendFromJson(Environment environment, String topic, String keyJson, String valueJson)
      throws KajException;

  List<RecordItem> consumeLastRecords(Environment environment, String topic,
      MessageFilter filter, long maxRecordsPerPartition, AtomicBoolean abort) throws KajException;

  String getKeyClassName();

  String getValueClassName();

  String getFolder();

  String getDomain();

}
