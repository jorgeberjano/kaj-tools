package es.jbp.kajtools.templates.symbols;

import es.jbp.expressions.Value;
import java.util.List;

public class SetFunction extends MemoryFunction {

  @Override
  public Value evaluate(List<Value> parameterList) {
    String key = getParameterAsString(parameterList, 0, null);
    Value value = parameterList.get(1);
    if (key != null) {
      memorizeValue(key, value);
    }
    return value;
  }

  @Override
  public int getParameterCount() {
    return -1;
  }
}

