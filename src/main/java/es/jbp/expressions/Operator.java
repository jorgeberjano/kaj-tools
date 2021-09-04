package es.jbp.expressions;

import com.google.common.math.BigIntegerMath;
import es.jbp.expressions.Value.ValueType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.List;
import java.util.function.BiFunction;

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
      valor1 = new Value(BigDecimal.ZERO);
    }
    if (parameterList.size() > 1) {
      valor2 = parameterList.get(1);
    } else {
      valor2 = new Value(BigDecimal.ZERO);
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
        return new Value(valor1.toBigInteger().add(valor2.toBigInteger()));
      } else {
        return new Value(valor1.toBigDecimal().add(valor2.toBigDecimal()));
      }
    }
  }

  public static class Subtract extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      if (valor1.getType() == ValueType.INTEGER && valor2.getType() == ValueType.INTEGER) {
        return new Value(valor1.toBigInteger().subtract(valor2.toBigInteger()));
      } else {
        return new Value(valor1.toBigDecimal().subtract(valor2.toBigDecimal()));
      }
    }
  }

  public static class Multiplication extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      if (valor1.getType() == ValueType.INTEGER && valor2.getType() == ValueType.INTEGER) {
        return new Value(valor1.toBigInteger().multiply(valor2.toBigInteger()));
      } else {
        return new Value(valor1.toBigDecimal().multiply(valor2.toBigDecimal()));
      }
    }
  }

  public static class Division extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      return new Value(valor1.toBigDecimal().divide(valor2.toBigDecimal(), MathContext.DECIMAL32));
    }
  }

  static class Equals extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      return new Value(valor1.toBigDecimal().equals(valor2.toBigDecimal()));
    }
  }

  static class Less extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      return new Value(valor1.toBigDecimal().compareTo(valor2.toBigDecimal()) < 0);
    }
  }

  static class LessEquals extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      return new Value(valor1.toBigDecimal().compareTo(valor2.toBigDecimal()) <= 0);
    }
  }

  static class Greater extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      return new Value(valor1.toBigDecimal().compareTo(valor2.toBigDecimal()) > 0);
    }
  }

  static class GreaterEquals extends Operator {

    @Override
    public Value operar(Value valor1, Value valor2) {
      return new Value(valor1.toBigDecimal().compareTo(valor2.toBigDecimal()) >= 0);
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
