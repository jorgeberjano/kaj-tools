package es.jbp.kajtools;

import es.jbp.kajtools.tabla.entities.TopicItem;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;

public class KafkaInvestigator implements KafkaBase {

  public List<TopicItem> getTopics(Environment environment) throws KajException {
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(
        createConsumerProperties(environment))) {
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
}
