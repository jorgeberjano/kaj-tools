package es.jbp.kajtools.script.nodes;

import es.jbp.expressions.ExpressionException;
import es.jbp.expressions.Value;
import es.jbp.kajtools.script.ExecutionContext;
import es.jbp.kajtools.script.exception.ScriptExecutionException;
import es.jbp.kajtools.util.TemplateExecutor;
import java.math.BigInteger;
import org.apache.commons.lang3.math.NumberUtils;

public class LoopNode extends SequenceNode {

  private final String times;

  public LoopNode(String times, int lineNumber) {
    super(lineNumber);
    this.times = times;
  }

  @Override
  public void execute(ExecutionContext context) throws ScriptExecutionException {
    TemplateExecutor templateExecutor = context.getTemplateExecutor();
    Value value = null;
    try {
       value = templateExecutor.evaluateExpression(times);
    } catch (ExpressionException e) {
      throw new ScriptExecutionException("No se ha podido evaluar la expresión", e);
    }

    long actualTimes = value.toBigInteger().longValue();
    for (long i = 0; i < actualTimes; i++) {
      if (context.getAbort().get()) {
        return;
      }
      templateExecutor.declareVariableValue("i", BigInteger.valueOf(i));
      super.execute(context);
    }
  }
}
