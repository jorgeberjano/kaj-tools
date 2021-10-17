package es.jbp.kajtools.script.nodes;

import es.jbp.expressions.ExpressionException;
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
    String value = null;
    try {
       value = templateExecutor.processTemplate(times);
    } catch (ExpressionException e) {
      throw new ScriptExecutionException("No se ha podido evaluar la expresión", e);
    }

    long actualTimes = NumberUtils.toLong(value, 1);
    for (long i = 0; i < actualTimes; i++) {
      templateExecutor.declareVariableValue("i", BigInteger.valueOf(i));
      super.execute(context);
    }
  }
}
