package es.jbp.kajtools.script;

import es.jbp.kajtools.Environment;
import es.jbp.kajtools.kafka.GenericClient;
import es.jbp.kajtools.templates.TextTemplate;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExecutionContext {
  private Map<String, String> variables;
  private GenericClient kafkaGenericClient;
  private Environment environment;
  private TextTemplate textTemplate;

  public void assignVariableValue(String variableName, String value) {
    variables.put(variableName, value);
    textTemplate.setVariableValues(variables);
  }
}
