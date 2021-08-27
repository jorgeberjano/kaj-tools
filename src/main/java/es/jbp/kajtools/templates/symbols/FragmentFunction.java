package es.jbp.kajtools.templates.symbols;

import es.jbp.expressions.ExpressionException;
import es.jbp.expressions.Value;
import es.jbp.kajtools.templates.TextTemplate;
import es.jbp.kajtools.util.ResourceUtil;
import java.util.List;

public class FragmentFunction extends AbstractFunction {

  private TextTemplate textTemplate;

  public FragmentFunction(TextTemplate textTemplate) {
    this.textTemplate = textTemplate;
  }

  @Override
  public Value evaluate(List<Value> parameterList) throws ExpressionException {
    String fileName = getParameterAsString(parameterList, 0, null);
    if (fileName == null) {
      return null;
    }
    return new Value(textTemplate.process(ResourceUtil.readResourceString(fileName)));
  }

  @Override
  public int getParameterCount() {
    return 1;
  }
}

