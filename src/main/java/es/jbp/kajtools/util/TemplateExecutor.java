package es.jbp.kajtools.util;

import es.jbp.expressions.ExpressionException;
import es.jbp.kajtools.templates.TextTemplate;
import java.util.Map;
import lombok.Getter;

public class TemplateExecutor {

  @Getter
  private TextTemplate textTemplate;

  public TemplateExecutor(Map<String, String> variables) {
    textTemplate = TextTemplate.builder().variables(variables).build();
  }

  public String templateToJson(String template) throws ExpressionException {

    return textTemplate.process(template);
  }

  public boolean containsTemplateExpressions(String json) {
    return json.contains("${") || json.contains("$S{") || json.contains("$R{") || json.contains("$I{") || json
        .contains("$F{") || json.contains("$B{");
  }
}
