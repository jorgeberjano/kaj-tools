package es.jbp.kajtools.script;

import es.jbp.kajtools.KajException;
import es.jbp.kajtools.kafka.GenericClient;

public class SendNode implements ScriptNode {

  private final String topic;
  private final String key;
  private final String value;
  private final String headers;

  public SendNode(String topic, String key, String value, String headers) {
    this.topic = topic;
    this.key = key;
    this.value = value;
    this.headers = headers;
  }

  @Override
  public void execute(ExecutionContext context) throws ScriptExecutionException {
    String actualTopic = extractActualValue(topic, context);
    String actualKey = extractActualValue(key, context);
    String actualValue = extractActualValue(value, context);
    String actualHeaders = extractActualValue(headers, context);
    GenericClient kafkaGenericClient = context.getKafkaGenericClient();

    try {
      kafkaGenericClient.sendFromJson(context.getEnvironment(), actualTopic, actualKey, actualValue, actualHeaders);
    } catch (KajException e) {
      throw new ScriptExecutionException("No se pudo enviar el mensaje", e);
    }
  }

  private String extractActualValue(String param, ExecutionContext context) {
    if (param.startsWith("$")) {
      return context.getVariables().get(param.substring(1));
    } else {
      return param;
    }
  }
}
