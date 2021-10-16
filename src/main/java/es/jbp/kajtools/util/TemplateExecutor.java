package es.jbp.kajtools.util;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.templates.TextTemplate;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import lombok.Getter;

public class TemplateExecutor {

  public static final String I_VARIABLE_NAME = "i";
  public static final String COUNTER_VARIABLE_NAME = "counter";
  private long i;
  private long counter;

  @Getter
  private TextTemplate textTemplate;

  public TemplateExecutor() {
    textTemplate = new TextTemplate();
    textTemplate.setVariableValue(I_VARIABLE_NAME, BigInteger.ZERO);
    textTemplate.setVariableValue(COUNTER_VARIABLE_NAME, BigInteger.ZERO);
  }

  public String processTemplate(String template) throws ExpressionException {

    return textTemplate.process(template);
  }

  public static boolean containsTemplateExpressions(String json) {
    return json.contains("${") || json.contains("$S{") || json.contains("$R{") || json.contains("$I{") || json
        .contains("$F{") || json.contains("$B{");
  }

  public void setVariables(Map<String, String> variables) {
    textTemplate.setVariableValues(variables);
  }

  public String formatJson(String text) throws ExpressionException {
    text = textTemplate.encodeBeforeFormatting(text);

    text = JsonUtils.formatJson(text);

    return textTemplate.decodeAfterFormatting(text);
  }

  public void resetIndexCounter() {
    i = 0;
    textTemplate.setVariableValue(I_VARIABLE_NAME, "0");
  }

  public void avanceCounters() {
    textTemplate.setVariableValue(COUNTER_VARIABLE_NAME, BigInteger.valueOf(++counter));
    textTemplate.setVariableValue(I_VARIABLE_NAME, BigInteger.valueOf(++i));
  }
}
