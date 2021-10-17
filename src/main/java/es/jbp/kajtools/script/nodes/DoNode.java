package es.jbp.kajtools.script.nodes;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.script.ExecutionContext;
import es.jbp.kajtools.script.exception.ScriptExecutionException;

public class DoNode extends ScriptNode {

  private final String expression;

  public DoNode(String expression, int lineNumber) {
    super(lineNumber);
    this.expression = expression;
  }

  @Override
  public void execute(ExecutionContext context) throws ScriptExecutionException {
    try {
      context.getTemplateExecutor().evaluateExpression(expression);
    } catch (ExpressionException e) {
      throw new ScriptExecutionException("Error de ejecución en linea " + getLine(), e);
    }
  }
}
