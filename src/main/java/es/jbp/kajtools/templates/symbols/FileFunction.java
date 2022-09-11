package es.jbp.kajtools.templates.symbols;

import es.jbp.expressions.ExpressionException;
import es.jbp.expressions.Value;
import es.jbp.kajtools.templates.TextTemplate;
import es.jbp.kajtools.util.ResourceUtil;
import java.util.List;

public class FileFunction extends AbstractFunction {

  private final TextTemplate textTemplate;

  public FileFunction(TextTemplate textTemplate) {
    this.textTemplate = textTemplate;
  }

  @Override
  public Value evaluate(List<Value> parameterList) throws ExpressionException {
    String fileName = getParameterAsString(parameterList, 0, null);
    if (fileName == null) {
      return null;
    }
    String fragment = ResourceUtil.readResourceString(fileName);
    return new Value(textTemplate.process(fragment));
  }

  @Override
  public int getParameterCount() {
    return 1;
  }
}

