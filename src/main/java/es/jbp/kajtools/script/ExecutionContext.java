package es.jbp.kajtools.script;

import es.jbp.kajtools.Environment;
import es.jbp.kajtools.kafka.GenericClient;
import es.jbp.kajtools.util.TemplateExecutor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecutionContext {

  private GenericClient kafkaGenericClient;
  private Environment environment;
  private TemplateExecutor templateExecutor;

  public void assignVariableValue(String variableName, String value) {
    templateExecutor.assignVariableValue(variableName, value);
  }

  public String getVariableValue(String variableName) {
    return templateExecutor.getVariableValue(variableName);
  }
}
