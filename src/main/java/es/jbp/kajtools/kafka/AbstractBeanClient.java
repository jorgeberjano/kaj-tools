package es.jbp.kajtools.kafka;

import com.google.common.collect.Lists;
import es.jbp.kajtools.Environment;
import es.jbp.kajtools.IMessageClient;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.util.DeepTestObjectCreator;
import es.jbp.kajtools.util.JsonUtils;
import es.jbp.kajtools.util.ResourceUtil;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.Getter;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFutureCallback;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class AbstractBeanClient<K, V> implements IMessageClient {

    private final Class<K> keyType;
    private final Class<V> valueType;
    private final Schema keySchema;
    private final Schema valueSchema;

    protected AbstractBeanClient(Class<K> keyType, Class<V> valueType) {
        this.keyType = keyType;
        this.valueType = valueType;

        keySchema = ReflectData.get().getSchema(keyType);
        valueSchema = ReflectData.get().getSchema(valueType);
    }

    @Override
    public String getKeyClassName() {
        return keyType.getName();
    }

    @Override
    public String getValueClassName() {
        return valueType.getName();
    }

    @Override
    public void sendFromJson(Environment environment, String topic, String keyJson, String valueJson, String headers)
            throws KajException {

        GenericRecord key;
        try {
            K k = JsonUtils.createFromJson(keyJson, keyType);
            key = toGenericRecord(k, keyType, keySchema);
        } catch (Exception ex) {
            throw new KajException("Error al crear el GenericRecord de la Key. Causa: " + ex.getMessage());
        }
        GenericRecord value;
        try {
            V v = JsonUtils.createFromJson(valueJson, valueType);
            value = toGenericRecord(v, valueType, valueSchema);
        } catch (Exception ex) {
            throw new KajException("Error al crear el GenericRecord del Value. Causa: " + ex.getMessage());
        }

        KafkaTemplate<GenericRecord, GenericRecord> senderTemplate;
        try {
            senderTemplate = createTemplate(environment);
        } catch (Exception ex) {
            throw new KajException("Error al crear el Template de Kafka. Causa: " + ex.getMessage());
        }
        try {
            var futureResult = senderTemplate.send(topic, key, value);

            futureResult.addCallback(new ListenableFutureCallback<SendResult<GenericRecord, GenericRecord>>() {
                @Override
                public void onFailure(Throwable throwable) {
                    System.err.println(throwable);
                }

                @Override
                public void onSuccess(SendResult<GenericRecord, GenericRecord> genericRecordGenericRecordSendResult) {
                    System.out.println("ENVIO CORRECTO!!!");
                }
            });
            //var result = futureResult.get();
            //var record = result.getProducerRecord();

        } catch (Throwable ex) {
            throw new KajException("Error al enviar el mensaje al topic.", ex);
        }
    }

    private KafkaTemplate<GenericRecord, GenericRecord> createTemplate(Environment environment) {
        return new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(createProducerProperties(environment)));
    }

    @Override
    public String getValueSchema(String json) throws KajException {
        DeepTestObjectCreator creator = new DeepTestObjectCreator();
        return creator.extractAvroSchema(buildValue(json));
    }

    @Override
    public String getKeySchema(String json) throws KajException {
        DeepTestObjectCreator creator = new DeepTestObjectCreator();
        return creator.extractAvroSchema(buildKey(json));
    }

    protected K buildKey(String json) throws KajException {
        return buildKeyFromJson(json);
    }

    protected V buildValue(String json) throws KajException {
        return buildValueFromJson(json);
    }

    protected K buildKeyFromJson(String keyJson) throws KajException {
        try {
            return JsonUtils.createFromJson(keyJson, keyType);
        } catch (Exception ex) {
            throw new KajException("No se puede generar el Key desde el JSON", ex);
        }
    }

    private V buildValueFromJson(String valueJson) throws KajException {
        try {
            return JsonUtils.createFromJson(valueJson, valueType);
        } catch (Exception ex) {
            throw new KajException("No se puede generar el Value desde el JSON", ex);
        }
    }

    @Override
    public List<String> getAvailableTopics() {
        return Lists.newArrayList(getDefaultTopic());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Getter(lazy = true)
    private final List<String> availableKeys = getAvailableResources("key.json");

    @Getter(lazy = true)
    private final List<String> availableValues = getAvailableResources("value.json");

    @Getter(lazy = true)
    private final List<String> availableHeaders = getAvailableResources("headers.properties");

    private List<String> getAvailableResources(String endingWith) {
        return ResourceUtil.getResourceFileNames(getFolder())
                .stream().filter(s -> s.toLowerCase().endsWith(endingWith))
                .collect(Collectors.toList());
    }

    public String getFolder() {
        return "";
    }

    @Override
    public String getDomain() {
        String[] nameSplit = getClass().getName().split("\\.");
        return nameSplit.length > 2 ? nameSplit[nameSplit.length - 2] : "";
    }

    public static Map<String, Object> createProducerProperties(Environment environment) {
        Map<String, Object> props = createCommonProperties(environment);

        putNotNull(props, ProducerConfig.ACKS_CONFIG, "all");
        putNotNull(props, ProducerConfig.RETRIES_CONFIG, 0);
        putNotNull(props, ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        putNotNull(props, ProducerConfig.LINGER_MS_CONFIG, 0);
        putNotNull(props, ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        putNotNull(props, ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        putNotNull(props, ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());

        //props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

        props.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG, 4000);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 4000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 6000);

        return props;
    }

    public static Map<String, Object> createConsumerProperties(Environment environment) {
        Map<String, Object> props = createCommonProperties(environment);

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "kaj-tools-" + System.getProperty("user.name"));

        putNotNull(props, ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        putNotNull(props, ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());

        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return props;
    }

    public static Map<String, Object> createCommonProperties(Environment environment) {
        Map<String, Object> props = new HashMap<>();

        putNotNull(props, AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getBootstrapServers());

        putNotNull(props, ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, environment.getBootstrapServers());

        putNotNull(props, AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
                ensureFinalSlash(environment.getUrlSchemaRegistry()));
        putNotNull(props, AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS,
                environment.isAutoRegisterSchemas());

        if (!Objects.isNull(environment.getUserSchemaRegistry())) {
            putNotNull(props, AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
            putNotNull(props, AbstractKafkaSchemaSerDeConfig.USER_INFO_CONFIG,
                    environment.getUserSchemaRegistry() + ":" + environment.getPasswordSchemaRegistry());
        }
        putNotNull(props, "security.protocol", environment.getSecurityProtocol());
        putNotNull(props, "sasl.mechanism", environment.getSaslMechanism());
        putNotNull(props, "sasl.jaas.config", environment.getSaslJaasConfig());
        putNotNull(props, "ssl.truststore.password", environment.getSslTruststorePassword());
        putNotNull(props, "ssl.truststore.location", ResourceUtil.getResourcePath(environment.getSslTruststoreLocation()));

        return props;
    }

    private static Object ensureFinalSlash(String url) {
        return Optional.ofNullable(url)
                .map(s -> s.endsWith("/") ? s : s + "/")
                .orElse(null);
    }

    private static void putNotNull(Map<String, Object> props, String key, Object value) {
        if (value != null) {
            props.put(key, value);
        }
    }

    @Override
    public void consumeLastRecords(Environment environment, String topic, LocalDateTime dateTimeToRewind,
                                   AtomicBoolean abort, ConsumerFeedback feedback) throws KajException {

        try (KafkaConsumer<K, V> consumer = new KafkaConsumer<>(createConsumerProperties(environment))) {

            RecordConsumer<K, V> recordConsumer = new RecordConsumer<>(consumer, keyType, valueType,
                    dateTimeToRewind, abort, feedback);
            consumer.subscribe(Collections.singletonList(topic), recordConsumer);

            recordConsumer.startConsumption();

        } catch (Exception ex) {
            throw new KajException("Error al suscribir el consumidor", ex);
        }
    }

    private <T> GenericRecord toGenericRecord(T object, Class<T> clazz, Schema schema) throws IOException {
        String json = object instanceof SpecificRecord ? object.toString() : JsonUtils.toJson(object);
        JsonAvroConverter avroConverter = new JsonAvroConverter();
        GenericData.Record record = avroConverter.convertToGenericDataRecord(json.getBytes(), schema);
        return record;
    }
}

