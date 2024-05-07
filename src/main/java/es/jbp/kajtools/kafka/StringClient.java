package es.jbp.kajtools.kafka;

import es.jbp.kajtools.Environment;
import es.jbp.kajtools.IMessageClient;
import es.jbp.kajtools.KajException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static es.jbp.kajtools.kafka.AbstractClient.putNotNull;

@Component
public class StringClient implements IMessageClient {

    private final List<IMessageClient> producerList;


    public StringClient(@Lazy List<IMessageClient> producerList) {
        this.producerList = producerList;
    }

    public static Map<String, Object> createProducerProperties(Environment environment) {
        Map<String, Object> props = AbstractClient.createCommonProperties(environment);

        putNotNull(props, ProducerConfig.ACKS_CONFIG, "all");
        putNotNull(props, ProducerConfig.RETRIES_CONFIG, 0);
        putNotNull(props, ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        putNotNull(props, ProducerConfig.LINGER_MS_CONFIG, 0);
        putNotNull(props, ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        putNotNull(props, ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        putNotNull(props, ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return props;
    }

    private List<String> getAvailableItems(Function<IMessageClient, List<String>> accessor) {
        return Collections.emptyList();
//        var itemStream = producerList
//                .stream()
//                .map(accessor)
//                .flatMap(Collection::stream);
//        return Stream.concat(Stream.of(""), itemStream).toList();
    }

    @Override
    public List<String> getAvailableKeys() {
        return getAvailableItems(IMessageClient::getAvailableKeys);
    }

    @Override
    public List<String> getAvailableValues() {
        return getAvailableItems(IMessageClient::getAvailableValues);

    }

    @Override
    public List<String> getAvailableHeaders() {
        return getAvailableItems(IMessageClient::getAvailableHeaders);
    }

    @Override
    public List<String> getAvailableTopics() {
        return Collections.emptyList();
//        var itemStream = applicationContext
//                .getBeansOfType(IMessageClient.class)
//                .values()
//                .stream()
//                .map(IMessageClient::getDefaultTopic);
//        return Stream.concat(Stream.of(""), itemStream).toList();
    }

    @Override
    public String getDefaultTopic() {
        return "";
    }

    @Override
    public String getValueSchema(String json) {
        return "";
    }

    @Override
    public String getKeySchema(String json) {
        return "";
    }

    @Override
    public void sendFromJson(Environment environment, String topic, String keyJson, String valueJson, String headers)
            throws KajException {

        KafkaTemplate<String, String> senderTemplate;
        try {
            senderTemplate = createTemplate(environment);
        } catch (Exception ex) {
            throw new KajException("Error al crear el Template de Kafka. Causa: " + ex.getMessage());
        }
        try {
            var futureResult = senderTemplate.send(topic, keyJson, valueJson);
            var result = futureResult.get();
            //System.out.println(result);
        } catch (Exception ex) {
            throw new KajException("Error al enviar el mensaje al topic.", ex);
        }
    }

    @Override
    public void consumeLastRecords(Environment environment, String topic, LocalDateTime dateTimeToRewind, AtomicBoolean abort, ConsumerFeedback feedback) throws KajException {
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(createConsumerProperties(environment))) {

            RecordConsumer<String, String> recordConsumer = new RecordConsumer<>(consumer, String.class, String.class,
                    dateTimeToRewind, abort, feedback);
            consumer.subscribe(Collections.singletonList(topic), recordConsumer);

            recordConsumer.startConsumption();

        } catch (Exception ex) {
            throw new KajException("Error al suscribir el consumidor", ex);
        }
    }

    private Map<String, Object> createConsumerProperties(Environment environment) {
        Map<String, Object> props = AbstractClient.createConsumerProperties(environment);

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kaj-tools-" + System.getProperty("user.name"));

        putNotNull(props, ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        putNotNull(props, ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return props;
    }

    @Override
    public String getKeyClassName() {
        return "";
    }

    @Override
    public String getValueClassName() {
        return "";
    }

    private KafkaTemplate<String, String> createTemplate(Environment environment) {
        return new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(createProducerProperties(environment)));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public String getResourcesPath() {
        return "";
    }

    @Override
    public String getDomain() {
        return "";
    }
}
