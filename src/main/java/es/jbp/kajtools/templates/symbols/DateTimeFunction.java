package es.jbp.kajtools.templates.symbols;

import es.jbp.expressions.Value;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DateTimeFunction extends AbstractFunction {

  private static final String DEFAULT_PATTERN = "yyyy-dd-MM'T'HH:MM'Z'";

  @Override
  public Value evaluate(List<Value> parameterList) {
    String pattern = getParameterAsString(parameterList, 0, DEFAULT_PATTERN);
    LocalDateTime now = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
    return new Value(now.format(formatter));
  }

  @Override
  public int getParameterCount() {
    return 1;
  }
}

