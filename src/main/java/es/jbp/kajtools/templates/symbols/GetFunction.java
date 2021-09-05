package es.jbp.kajtools.templates.symbols;

import es.jbp.expressions.Value;
import java.util.List;

public class GetFunction extends MemoryFunction {

  @Override
  public Value evaluate(List<Value> parameterList) {
    String key = getParameterAsString(parameterList, 0, null);
    if (key != null) {
      return getMemorizedValue(key);
    }
    return new Value();
  }

  @Override
  public int getParameterCount() {
    return -1;
  }
}

