package es.jbp.expressions;

import java.math.BigDecimal;
import java.math.BigInteger;
import lombok.Getter;
import lombok.Setter;

/**
 * Variable con un valor.
 * @author Jorge Berjano
 */
public class VariableWithValue implements Variable {

  @Getter
  @Setter
  private Value value;

  public VariableWithValue(String value) {
    this.value = new Value(value);
  }

  public VariableWithValue(Boolean value) {
    this.value = new Value(value);
  }

  public VariableWithValue(BigInteger value) {
    this.value = new Value(value);
  }

  public VariableWithValue(BigDecimal value) {
    this.value = new Value(value);
  }

}
