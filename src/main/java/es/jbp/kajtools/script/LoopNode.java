package es.jbp.kajtools.script;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.templates.TextTemplate;
import org.apache.commons.lang3.math.NumberUtils;

public class LoopNode extends SequenceNode {

  private final String times;

  public LoopNode(String times) {
    this.times = times;
  }

  @Override
  public void execute(ExecutionContext context) throws ScriptExecutionException {
    TextTemplate textTemplate = context.getTextTemplate();
    String value = null;
    try {
       value = textTemplate.process(times);
    } catch (ExpressionException e) {
      throw new ScriptExecutionException("No se ha podido evaluar la expresión", e);
    }

    long actualTimes = NumberUtils.toLong(value, 1);

  }
}
