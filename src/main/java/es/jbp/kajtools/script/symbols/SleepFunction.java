package es.jbp.kajtools.script.symbols;

import es.jbp.expressions.ExpressionException;
import es.jbp.expressions.Value;
import es.jbp.kajtools.script.ExecutionContext;
import es.jbp.kajtools.templates.symbols.AbstractFunction;
import es.jbp.kajtools.ui.interfaces.InfoReportable;
import java.util.List;

public class SleepFunction extends AbstractFunction {

  private final ExecutionContext context;

  public SleepFunction(ExecutionContext context) {
    this.context = context;
  }

  @Override
  public Value evaluate(List<Value> parameterList) throws ExpressionException {
    long ms = getParameterAsLong(parameterList, 0, 1000);
    context.getInfoReportable().enqueueMessage(InfoReportable.buildTraceMessage("Pausa de " + ms + " ms"));
    try {
      Thread.sleep(ms);
    } catch (InterruptedException | NumberFormatException e) {
      throw new ExpressionException("No se ha podido ejecutar el sleep", e);
    }
    return new Value();
  }

  @Override
  public int getParameterCount() {
    return 1;
  }
}

