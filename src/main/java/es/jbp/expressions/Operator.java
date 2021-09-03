package es.jbp.expressions;

import es.jbp.expressions.Value.ValueType;
import java.util.List;

/**
 * Operador matemático representado como una función
 * @author Jorge Berjano
 */
public abstract class Operator implements Function {

  @Override
  public Value evaluate(List<Value> parameterList) {
    Value valor1;
    Value valor2;

    if (!parameterList.isEmpty()) {
      valor1 = parameterList.get(0);
    } else {
      valor1 = new Value(0.0);
    }
    if (parameterList.size() > 1) {
      valor2 = parameterList.get(1);
    } else {
      valor2 = new Value(0.0);
    }
    return operar(valor1, valor2);
  }

  @Override
  public int getParameterCount() {
    return 2;
  }

  public boolean allowOmitParameters() {
    return false;
  }

  public abstract Value operar(Value resultado, Value valor);

  public static class Addition extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      if (valor1.getType() == ValueType.INTEGER && valor2.getType() == ValueType.INTEGER) {
        return new Value(valor1.toLong() + valor2.toLong());
      } else {
        return new Value(valor1.toDouble() + valor2.toDouble());
      }
    }
  }

  public static class Substract extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      if (valor1.getType() == ValueType.INTEGER && valor2.getType() == ValueType.INTEGER) {
        return new Value(valor1.toLong() - valor2.toLong());
      } else {
        return new Value(valor1.toDouble() - valor2.toDouble());
      }
    }
  }

  public static class Multiplication extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      if (valor1.getType() == ValueType.INTEGER && valor2.getType() == ValueType.INTEGER) {
        return new Value(valor1.toLong() * valor2.toLong());
      } else {
        return new Value(valor1.toDouble() * valor2.toDouble());
      }
    }
  }

  public static class Division extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      return new Value(valor1.toDouble() / valor2.toDouble());
    }
  }

  static class Less extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      return new Value(valor1.toDouble() < valor2.toDouble());
    }
  }

  static class LessEquals extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      return new Value(valor1.toDouble() <= valor2.toDouble());
    }
  }

  static class Greater extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      return new Value(valor1.toDouble() > valor2.toDouble());
    }
  }

  static class GreaterEquals extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      return new Value(valor1.toDouble() >= valor2.toDouble());
    }
  }

  static class And extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      return new Value(valor1.toBoolean() && valor2.toBoolean());
    }
  }

  static class Or extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      return new Value(valor1.toBoolean() || valor2.toBoolean());
    }
  }
}
