package es.jbp.kajtools.script;

import es.jbp.expressions.Value;
import es.jbp.kajtools.Environment;
import es.jbp.kajtools.kafka.GenericClient;
import es.jbp.kajtools.ui.interfaces.InfoReportable;
import es.jbp.kajtools.util.TemplateExecutor;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExecutionContext {

  private GenericClient kafkaGenericClient;
  private Map<String, Environment> environments;
  private TemplateExecutor templateExecutor;
  private InfoReportable infoReportable;
  private AtomicBoolean abort;

  public void assignVariableValue(String variableName, Value value) {
    templateExecutor.assignVariableValue(variableName, value);
  }

  public Environment getEnvironment(String environmentName) {
    return environments.get(environmentName);
  }
}
