package es.jbp.kajtools.templates;

import es.jbp.expressions.ExpressionCompiler;
import es.jbp.expressions.ExpressionException;
import es.jbp.expressions.ExpressionNode;
import es.jbp.expressions.Value;
import es.jbp.expressions.Value.ValueType;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

public class TextTemplate {

  private static final String BEGIN_ENCODED_EXPRESSION = "\"»»»»»";
  private static final String END_ENCODED_EXPRESSION = "«««««\"";

  private enum Context {
    OUTSIDE,
    INSIDE_STRING,
    INSIDE_EXPRESSION
  }

  private final TemplateSimbolFactory templateSimbolFactory = new TemplateSimbolFactory(this);

  public void setVariableValues(Map<String, String> variableValues) {
    templateSimbolFactory.setVariableValues(variableValues);
  }

  public void setVariableValue(String variableName, String value) {
    templateSimbolFactory.setVariableValue(variableName, value);
  }

  public String process(String text) throws ExpressionException {
    return processWithExpressionSubstitution(text, this::evaluateExpression);
  }

  public String encodeBeforeFormatting(String text) throws ExpressionException {
    return processWithExpressionSubstitution(text, this::encodeExpression);
  }

  public String decodeAfterFormatting(String text) {
    StringBuilder builder = new StringBuilder();

    while (StringUtils.isNotBlank(text)) {
      int beginIndex = text.indexOf(BEGIN_ENCODED_EXPRESSION);

      if (beginIndex < 0) {
        builder.append(text);
        break;
      }
      builder.append(text.substring(0, beginIndex));
      text = text.substring(beginIndex);

      int endIndex = text.indexOf(END_ENCODED_EXPRESSION);
      if (endIndex >= 0) {
        String expression = text.substring(BEGIN_ENCODED_EXPRESSION.length(), endIndex);
        builder.append(StringEscapeUtils.unescapeJava(expression));
        text = text.substring(endIndex + END_ENCODED_EXPRESSION.length());
      }
    }

    return builder.toString();
  }

  private String encodeExpression(String text) {
    text = StringEscapeUtils.escapeJava(text);
    return BEGIN_ENCODED_EXPRESSION + text + END_ENCODED_EXPRESSION;
  }

  private interface ExpressionSubstitution {

    String substitute(String text) throws ExpressionException;
  }

  private String processWithExpressionSubstitution(String text, ExpressionSubstitution substitution)
      throws ExpressionException {

    StringBuilder stringBuilder = new StringBuilder();
    Context context = Context.OUTSIDE;
    int expressionStartIndex = 0;
    char previousChar = 0;
    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      switch (context) {
        case OUTSIDE:
          if (ch == '$') {
            context = Context.INSIDE_EXPRESSION;
            expressionStartIndex = i;
            ch = 0;
          } else if (ch == '"') {
            context = Context.INSIDE_STRING;
          }
          break;
        case INSIDE_EXPRESSION:
          if (ch == '}') {
            context = Context.OUTSIDE;
            String expression = text.substring(expressionStartIndex, i + 1);
            String expressionSubstitute = substitution.substitute(expression);
            stringBuilder.append(expressionSubstitute);
          }
          ch = 0;
          break;
        case INSIDE_STRING:
          if (ch == '"' && previousChar != '\\') {
            context = Context.OUTSIDE;
          }
      }

      if (ch != 0) {
        stringBuilder.append(ch);
      }
      previousChar = ch;
    }
    return stringBuilder.toString();
  }

  private String evaluateExpression(String text) throws ExpressionException {
    final ExpressionCompiler compilador = new ExpressionCompiler(templateSimbolFactory);

    char ch = text.charAt(1);

    ValueType valueType = null;
    boolean raw = ch == 'R';
    if (ch == 'S' || raw) {
      valueType = ValueType.STRING;
    } else if (ch == 'I') {
      valueType = ValueType.INTEGER;
    } else if (ch == 'F') {
      valueType = ValueType.DECIMAL;
    } else if (ch == 'B') {
      valueType = ValueType.BOOLEAN;
    }
    int beginIndex = text.indexOf('{');
    int endIndex = text.lastIndexOf('}');
    String expression = text.substring(beginIndex + 1, endIndex);
    ExpressionNode expressionNode = compilador.compile(expression);
    Value value = null;
    if (expressionNode != null) {
      value = expressionNode.evaluate();
    }
    if (value == null || value.getObject() == null) {
      return "null";
    }
    if (valueType == null) {
      valueType = value.getType();
    }
    if (valueType == ValueType.STRING && !raw) {
      return "\"" + value.toString() + "\"";
    } else {
      return Objects.toString(value.getObject(valueType));
    }
  }
}
