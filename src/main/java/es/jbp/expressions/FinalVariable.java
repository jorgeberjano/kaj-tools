package es.jbp.expressions;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Variable con un valor constante.
 * @author Jorge Berjano
 */
public class FinalVariable implements Variable {

  private final Value value;

  public FinalVariable(String value) {
    this.value = new Value(value);
  }

  public FinalVariable(Boolean value) {
    this.value = new Value(value);
  }

  public FinalVariable(BigInteger value) {
    this.value = new Value(value);
  }

  public FinalVariable(BigDecimal value) {
    this.value = new Value(value);
  }

  @Override
  public Value getValor() {
    return value;
  }
}
