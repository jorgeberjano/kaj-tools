package es.jbp.kajtools.script.symbols;

import es.jbp.expressions.ExpressionException;
import es.jbp.expressions.Value;
import es.jbp.kajtools.script.exception.ScriptExecutionException;
import es.jbp.kajtools.templates.symbols.AbstractFunction;
import java.util.List;

public class SleepFunction extends AbstractFunction {

  @Override
  public Value evaluate(List<Value> parameterList) throws ExpressionException {
    long ms = getParameterAsLong(parameterList, 0, 1000);
    try {
      Thread.sleep(ms);
    } catch (InterruptedException | NumberFormatException e) {
      throw new ExpressionException("No se ha podido ejecutar el sleep", e);
    }
    return new Value();
  }

  @Override
  public int getParameterCount() {
    return -1;
  }
}

