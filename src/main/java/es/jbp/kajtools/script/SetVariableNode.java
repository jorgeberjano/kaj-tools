package es.jbp.kajtools.script;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.templates.TextTemplate;

public class SetVariableNode implements ScriptNode {

  private final String variableName;
  private final String valueExpression;

  public SetVariableNode(String variableName, String valueExpression) {
    this.variableName = variableName;
    this.valueExpression = valueExpression;
  }

  @Override
  public void execute(ExecutionContext context) throws ScriptExecutionException {
    TextTemplate textTemplate = context.getTextTemplate();
    try {
      String value = textTemplate.process(valueExpression);
      context.assignVariableValue(variableName, value);
    } catch (ExpressionException e) {
      throw new ScriptExecutionException("No se ha podido evaluar la expresión", e);
    }
  }
}
