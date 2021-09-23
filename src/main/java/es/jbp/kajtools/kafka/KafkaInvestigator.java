package es.jbp.kajtools.kafka;

import es.jbp.kajtools.Environment;
import es.jbp.kajtools.KajException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;

public class KafkaInvestigator {

  public List<TopicItem> getTopics(Environment environment) throws KajException {
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(
        GenericClient.createConsumerProperties(environment))) {
      Map<String, List<PartitionInfo>> topics = consumer.listTopics(Duration.ofSeconds(10));
      return topics.entrySet().stream()
          .map(this::createTopicItem)
          .sorted((t1, t2) -> StringUtils.compare(t1.getName(), t2.getName()))
          .collect(Collectors.toList());
    } catch (Exception ex) {
      throw new KajException("No se ha podido obtener la lista de topics", ex);
    }
  }

  private TopicItem createTopicItem(Entry<String, List<PartitionInfo>> topicEntry) {
    return TopicItem.builder()
        .name(topicEntry.getKey())
        .partitions(topicEntry.getValue().size())
        .build();
  }

  public void deleteTopic(String topicName, Environment environment) throws KajException {
    AdminClient admin = KafkaAdminClient.create(GenericClient.createCommonProperties(environment));
    DeleteTopicsResult result = admin.deleteTopics(Collections.singleton(topicName));
    try {
      result.all().get();
    } catch (InterruptedException | ExecutionException e) {
      new KajException("No se ha podido borrar el topic", e);
    }
  }
}
