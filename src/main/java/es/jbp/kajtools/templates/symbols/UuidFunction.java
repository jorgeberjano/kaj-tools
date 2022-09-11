package es.jbp.kajtools.templates.symbols;

import es.jbp.expressions.Value;
import java.util.List;
import java.util.UUID;

public class UuidFunction extends AbstractFunction {

  @Override
  public Value evaluate(List<Value> parameterList) {
    return new Value(UUID.randomUUID().toString());
  }

  @Override
  public int getParameterCount() {
    return 0;
  }
}

