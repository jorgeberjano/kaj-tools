package es.jbp.kajtools.script.symbols;

import es.jbp.expressions.ExpressionException;
import es.jbp.expressions.Value;
import es.jbp.kajtools.KajException;
import es.jbp.kajtools.kafka.GenericClient;
import es.jbp.kajtools.script.ExecutionContext;
import es.jbp.kajtools.templates.symbols.AbstractFunction;
import java.util.List;

public class SendFunction extends AbstractFunction {

  private final ExecutionContext context;

  public SendFunction(ExecutionContext context) {
    this.context = context;
  }

  @Override
  public Value evaluate(List<Value> parameterList) throws ExpressionException {
    String topic = getParameterAsString(parameterList, 0, "");
    String key = getParameterAsString(parameterList, 1, "");
    String value = getParameterAsString(parameterList, 2, "");
    String headers = getParameterAsString(parameterList, 3, "");
    GenericClient kafkaGenericClient = context.getKafkaGenericClient();
    try {
      kafkaGenericClient.sendFromJson(context.getEnvironment(), topic, key, value, headers);
    } catch (KajException e) {
      throw new ExpressionException("No se pudo enviar el mensaje", e);
    }
    return new Value();
  }

  @Override
  public int getParameterCount() {
    return -1;
  }
}

