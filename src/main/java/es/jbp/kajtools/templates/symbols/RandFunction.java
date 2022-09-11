package es.jbp.kajtools.templates.symbols;

import es.jbp.expressions.Value;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandFunction extends AbstractFunction {

  @Override
  public Value evaluate(List<Value> parameterList) {
    long limit = getParameterAsLong(parameterList, 0, 10L);
    long result = ThreadLocalRandom.current().nextLong(0, limit);
    return new Value(BigDecimal.valueOf(result));
  }

  @Override
  public int getParameterCount() {
    return 1;
  }
}

