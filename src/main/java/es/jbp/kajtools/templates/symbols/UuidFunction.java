package es.jbp.kajtools.templates.symbols;

import es.jbp.expressions.Value;
import java.util.List;
import java.util.UUID;

public class UuidFunction extends AbstractFunction {


  @Override
  public Value evaluate(List<Value> parameterList) {
    Value value = randomElement(parameterList);
    if (value == null) {
      return generateRandValue();
    }
    String key = value.toString();

    Value memorizedValue = getMemorizedValue(key);
    if (memorizedValue != null) {
      return memorizedValue;
    }
    Value randValue = generateRandValue();
    memorizeValue(key, randValue);
    return randValue;
  }


  private Value generateRandValue() {
    return new Value(UUID.randomUUID().toString());
  }

  @Override
  public int getParameterCount() {
    return -1;
  }
}

