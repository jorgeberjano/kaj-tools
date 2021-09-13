package es.jbp.kajtools;

import es.jbp.kajtools.kafka.ConsumerFeedback;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public interface IMessageClient {

  List<String> getAvailableKeys();

  List<String> getAvailableValues();

  List<String> getAvailableHeaders();

  String getDefaultTopic();

  List<String> getAvailableTopics();

  String getValueSchema(String json) throws KajException;

  String getKeySchema(String json) throws KajException;

  void sendFromJson(Environment environment, String topic, String keyJson, String valueJson, String headers)
      throws KajException;

  void consumeLastRecords(Environment environment, String topic, LocalDateTime dateTimeToRewind,
      AtomicBoolean abort, ConsumerFeedback feedback) throws KajException;

  String getKeyClassName();

  String getValueClassName();

  String getFolder();

  String getDomain();

}
