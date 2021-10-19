package es.jbp.kajtools.script.nodes;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.script.ExecutionContext;
import es.jbp.kajtools.script.exception.ScriptExecutionException;
import es.jbp.kajtools.util.TemplateExecutor;

public class IfNode extends SequenceNode {

  private final String condition;

  public IfNode(String condition, int lineNumber) {
    super(lineNumber);
    this.condition = condition;
  }

  @Override
  public void execute(ExecutionContext context) throws ScriptExecutionException {
    TemplateExecutor templateExecutor = context.getTemplateExecutor();

    if (meetsCondition(templateExecutor)) {
      super.execute(context);
    }
  }

  protected boolean meetsCondition(TemplateExecutor templateExecutor) throws ScriptExecutionException {
    try {
      var value = templateExecutor.evaluateExpression(condition);
      return value.toBoolean();
    } catch (ExpressionException e) {
      throw new ScriptExecutionException("No se ha podido evaluar la expresión " + condition, e);
    }
  }
}
