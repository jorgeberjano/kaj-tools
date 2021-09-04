package es.jbp.kajtools.util;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.templates.TextTemplate;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import lombok.Getter;

public class TemplateExecutor {

  @Getter
  private TextTemplate textTemplate;

  public TemplateExecutor() {
    textTemplate = new TextTemplate();
  }

  public String templateToJson(String template) throws ExpressionException {

    return textTemplate.process(template);
  }

  public static boolean containsTemplateExpressions(String json) {
    return json.contains("${") || json.contains("$S{") || json.contains("$R{") || json.contains("$I{") || json
        .contains("$F{") || json.contains("$B{");
  }

  public void setVariables(String variablesProperties) {
    Properties properties = new Properties();
    try {
      properties.load(new StringReader(variablesProperties));
    } catch (IOException e) {
      System.err.println("No se han cargado las variables: " + e.getMessage());
    }
    Map<String, String> variables = new HashMap<>();
    properties.forEach((k, v) -> variables.put(Objects.toString(k), Objects.toString(v)));
    textTemplate.setVariableValues(variables);
  }

  public void setVariableValue(String variableName, String value) {
    textTemplate.setVariableValue(variableName, value);
  }

  public String formatJson(String text) throws ExpressionException {
    text = textTemplate.encodeBeforeFormatting(text);

    text = JsonUtils.formatJson(text);

    return textTemplate.decodeAfterFormatting(text);
  }
}
