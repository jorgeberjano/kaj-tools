package es.jbp.kajtools.kafka;

import es.jbp.kajtools.Environment;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.kafka.TopicItem.TopicItemBuilder;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.stereotype.Service;

@Service
public class KafkaAdminService {

  public List<TopicItem> getTopics(Environment environment) throws KajException {
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(
        AbstractClient.createConsumerProperties(environment))) {
      Map<String, List<PartitionInfo>> topics = consumer.listTopics(Duration.ofSeconds(10));
      return topics.entrySet().stream()
          .map(this::createTopicItem)
          .sorted((t1, t2) -> StringUtils.compare(t1.getName(), t2.getName()))
          .collect(Collectors.toList());
    } catch (Exception ex) {
      throw new KajException("No se ha podido obtener la lista de topics", ex);
    }
  }

  public Map<String, String> getTopicConfig(String topicName, Environment environment) {
    try (var admin = AdminClient.create(AbstractClient.createCommonProperties(environment))) {
      var configResourceSet = Collections.singleton(new ConfigResource(ConfigResource.Type.TOPIC, topicName));
      DescribeConfigsResult configsResult = admin.describeConfigs(configResourceSet);

      final Map<String, String> result = new HashMap<>();
      try {
        Object[] allConfigs = configsResult.all().get().values().toArray();
        if (allConfigs.length == 0) {
          return null;
        }
        var config = (Config) allConfigs[0];
        config.entries().forEach(currentConfig -> result.put(currentConfig.name(), currentConfig.value()));

      } catch (InterruptedException | ExecutionException ex) {
        result.put("error", ex.getMessage());
      }
      return result;
    }
  }

//  public List<TopicItem> getTopics2(Environment environment) throws KajException {
//    try (AdminClient admin = KafkaAdminClient.create(GenericClient.createCommonProperties(environment))) {
//
//      List<TopicItem> list = new ArrayList<>();
//
//      for (TopicListing topicListing : admin.listTopics().listings().get()) {
//        String topicName = topicListing.name();
//        var configResourceSet = Collections.singleton(new ConfigResource(ConfigResource.Type.TOPIC, topicName));
//        DescribeConfigsResult configsResult = admin.describeConfigs(configResourceSet);
//        TopicItem topicItem = getTopicDetails(topicName, configsResult);
//        if (topicItem != null) {
//          list.add(topicItem);
//        }
//      }
//      return list;
//    } catch (InterruptedException | ExecutionException ex) {
//      throw new KajException("No se ha podido obtener la lista de topics", ex);
//    }
//  }

//  public TopicItem getTopicDetails2(String topicName, DescribeConfigsResult configsResult) {
//
//    TopicItemBuilder builder = TopicItem.builder().name(topicName);
//
//    try {
//      Object[] allConfigs = configsResult.all().get().values().toArray();
//      if (allConfigs.length == 0) {
//        return null;
//      }
//      Config config = (Config) allConfigs[0];
//      config.entries().stream()
//          .sorted()
//          .forEach(currentConfig -> builder.config(currentConfig.value(), currentConfig.value()));
//
//    } catch (InterruptedException | ExecutionException ex) {
//      builder.config("error", ex.getMessage());
//    }
//    return builder.build();
//  }

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
