package es.jbp.kajtools.util;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.templates.TemplateSimbolFactory;
import es.jbp.kajtools.templates.TextTemplate;
import java.math.BigInteger;
import java.util.Map;
import lombok.Getter;

/**
 * Se encarga de sustituir simbolos ${...} de un template por el resultado de evaluar la expresión que contiene.
 * Tambien pewrmite el formateo de JSON templates.
 */
public class TemplateExecutor {

  public static final String I_VARIABLE_NAME = "i";
  public static final String COUNTER_VARIABLE_NAME = "counter";
  private long i;
  private long counter;

  @Getter
  private TextTemplate textTemplate;

  public TemplateExecutor(TemplateSimbolFactory templateSimbolFactory) {
    textTemplate = new TextTemplate(templateSimbolFactory);
    templateSimbolFactory.declareSymbols(textTemplate);
  }

  public TemplateExecutor() {
    textTemplate = new TextTemplate();
  }

  public String processTemplate(String template) throws ExpressionException {

    return textTemplate.process(template);
  }

  public static boolean containsTemplateExpressions(String json) {
    return json.contains("${") || json.contains("$S{") || json.contains("$R{") || json.contains("$I{") || json
        .contains("$F{") || json.contains("$B{");
  }

  public void addVariables(Map<String, String> variables) {
    textTemplate.declareVariableValues(variables);
  }

  public void declareVariableValue(String variable, String value) {
    textTemplate.declareVariableValue(variable, value);
  }

  public void declareVariableValue(String variable, BigInteger value) {
    textTemplate.declareVariableValue(variable, value);
  }

  public String formatJson(String text) throws ExpressionException {
    text = textTemplate.encodeBeforeFormatting(text);

    text = JsonUtils.formatJson(text);

    return textTemplate.decodeAfterFormatting(text);
  }

  public void resetIndexCounter() {
    i = 0;
    textTemplate.declareVariableValue(I_VARIABLE_NAME, "0");
  }

  public void avanceCounters() {
    textTemplate.declareVariableValue(COUNTER_VARIABLE_NAME, BigInteger.valueOf(++counter));
    textTemplate.declareVariableValue(I_VARIABLE_NAME, BigInteger.valueOf(++i));
  }

  public String getVariableValue(String variableName) {
    return textTemplate.getVariableValue(variableName);
  }

  public String evaluateExpression(String expression) throws ExpressionException {
    return textTemplate.evaluateExpression(expression);
  }

  public void assignVariableValue(String variableName, String value) {
    textTemplate.assignVariableValue(variableName, value);
  }
}
