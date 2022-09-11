package es.jbp.kajtools.templates.symbols;

import es.jbp.expressions.Value;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StrFunction extends AbstractFunction {

  private final Map<String, Value> memory = new HashMap<>();

  @Override
  public Value evaluate(List<Value> parameterList) {
    StringBuilder stringBuilder = new StringBuilder();

    for (Value value : parameterList) {
        stringBuilder.append(value.toString());
    }
    return new Value(stringBuilder.toString());
  }

  @Override
  public int getParameterCount() {
    return -1;
  }
}
