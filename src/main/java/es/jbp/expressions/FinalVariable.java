package es.jbp.expressions;

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

  public FinalVariable(Long value) {
    this.value = new Value(value);
  }

  public FinalVariable(Double value) {
    this.value = new Value(value);
  }

  @Override
  public Value getValor() {
    return value;
  }
}
