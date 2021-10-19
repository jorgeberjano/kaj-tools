package es.jbp.kajtools.script.symbols;

import es.jbp.expressions.ExpressionException;
import es.jbp.expressions.Value;
import es.jbp.kajtools.script.ExecutionContext;
import es.jbp.kajtools.templates.symbols.AbstractFunction;
import es.jbp.kajtools.ui.interfaces.InfoReportable;
import java.util.List;

public class PrintFunction extends AbstractFunction {

  private final ExecutionContext context;

  public PrintFunction(ExecutionContext context) {
    this.context = context;
  }

  @Override
  public Value evaluate(List<Value> parameterList) throws ExpressionException {
    String text = getParameterAsString(parameterList, 0, "");
    context.getInfoReportable().enqueueMessage(InfoReportable.buildTraceMessage(text));
    return new Value();
  }

  @Override
  public int getParameterCount() {
    return 1;
  }
}

