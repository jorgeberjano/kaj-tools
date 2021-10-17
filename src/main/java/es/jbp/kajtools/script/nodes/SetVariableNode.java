package es.jbp.kajtools.script.nodes;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.script.ExecutionContext;
import es.jbp.kajtools.script.exception.ScriptExecutionException;

public class SetVariableNode extends ScriptNode {

  private final String variableName;
  private final String valueExpression;

  public SetVariableNode(String variableName, String valueExpression, int lineNumber) {
    super(lineNumber);
    this.variableName = variableName;
    this.valueExpression = valueExpression;
  }

  @Override
  public void execute(ExecutionContext context) throws ScriptExecutionException {
    var templateExecutor = context.getTemplateExecutor();
    try {
      String value = templateExecutor.evaluateExpression(valueExpression);
      context.assignVariableValue(variableName, value);
    } catch (ExpressionException e) {
      throw new ScriptExecutionException("No se ha podido evaluar la expresión", e);
    }
  }
}
