package es.jbp.kajtools.templates;

import es.jbp.expressions.ExpressionCompiler;
import es.jbp.expressions.ExpressionException;
import es.jbp.expressions.ExpressionNode;
import es.jbp.expressions.Value;
import es.jbp.expressions.Value.ValueType;
import java.util.Map;
import java.util.Objects;

public class TextTemplate {

  private enum Context {
    OUTSIDE,
    BEFORE_DOLLAR,
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

    final ExpressionCompiler compilador = new ExpressionCompiler(templateSimbolFactory);

    StringBuilder stringBuilder = new StringBuilder();
    Context context = Context.OUTSIDE;
    int expressionStartIndex = 0;
    ValueType focedValueType = null;
    boolean raw = false;

    for (int i = 0; i < text.length(); i++) {
      char ch = text.charAt(i);
      if (ch == '$' && context == Context.OUTSIDE) {
        context = Context.BEFORE_DOLLAR;
        focedValueType = null;
        raw = false;
        continue;
      } else if (context == Context.BEFORE_DOLLAR) {
        if (ch == 'R') {
          raw = true;
        }
        if (ch == 'S' || ch == 'R') {
          focedValueType = ValueType.STRING;
        } else if (ch == 'I') {
          focedValueType = ValueType.INTEGER;
        } else if (ch == 'F') {
          focedValueType = ValueType.DECIMAL;
        } else if (ch == 'B') {
          focedValueType = ValueType.BOOLEAN;
        } else if (ch == '{') {
          context = Context.INSIDE_EXPRESSION;
          expressionStartIndex = i;
        } else {
          throw new ExpressionException("Falta '{' detrás de '$'");
        }
        continue;
      } else if (ch == '}' && context == Context.INSIDE_EXPRESSION) {
        String expression = text.substring(expressionStartIndex + 1, i);

        ExpressionNode expressionNode = compilador.compile(expression);
        Value value;
        if (expressionNode != null) {
          value = expressionNode.evaluate();
        } else {
          value = new Value();
        }
        stringBuilder.append(toJson(value, focedValueType, raw));

        context = Context.OUTSIDE;
        continue;
      } else if (ch == '"' && context == Context.OUTSIDE) {
        context = Context.INSIDE_STRING;
      } else if (ch == '"' && context == Context.INSIDE_STRING) {
        context = Context.OUTSIDE;
      }

      if (context != Context.INSIDE_EXPRESSION) {
        stringBuilder.append(ch);
      }
    }
    return stringBuilder.toString();
  }

  public String toJson(Value value, ValueType forcedType, boolean raw) {
    ValueType type = forcedType != null ? forcedType : value.getType();

    if (value == null || value.getObject() == null) {
      return "null";
    } else if (type == ValueType.STRING && !raw) {
      return "\"" + value.toString() + "\"";
    } else {
      return Objects.toString(value.getObject(type));
    }
  }
}
