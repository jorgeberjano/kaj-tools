package es.jbp.kajtools.templates.symbols;

import es.jbp.expressions.Value;
import java.util.List;

public class AnyFunction extends AbstractFunction {

  @Override
  public Value evaluate(List<Value> parameterList) {
    return randomElement(parameterList);
  }

  @Override
  public int getParameterCount() {
    return -1;
  }
}

