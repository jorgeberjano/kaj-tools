package es.jbp.expressions;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

/**
 * Representación de un valor de tipo booleano, numérico entero, numérico real o cadena de texto.
 * @author Jorge Berjano
 */
public class Value {

    public enum ValueType {
        BOOLEAN,
        INTEGER,
        DECIMAL,
        STRING
    }

    private final ValueType type;
    private final Object variant;

    public Value() {
        type = ValueType.STRING;
        variant = "";
    }

    public Value(boolean valor) {
        type = ValueType.BOOLEAN;
        variant = valor;
    }

    public Value(BigInteger valor) {
        type = ValueType.INTEGER;
        variant = valor;
    }

    public Value(BigDecimal valor) {

        type = ValueType.DECIMAL;
        variant = valor;
    }

    public Value(String valor) {
        type = ValueType.STRING;
        variant = valor;
    }

    public ValueType getType() {
        return type;
    }

    public String toString() {
        return Objects.toString(variant);
    }

    public BigInteger toBigInteger() {
        if (variant == null) {
            return BigInteger.valueOf(0L);
        }
        if (variant instanceof Boolean) {
            return (boolean) variant ? BigInteger.ONE : BigInteger.ZERO;
        }
        if (variant instanceof BigInteger) {
            return (BigInteger) variant;
        }
        if (variant instanceof BigDecimal) {
            return ((BigDecimal) variant).toBigInteger();
        }
        return new BigDecimal(variant.toString()).toBigInteger();
    }

    public BigDecimal toBigDecimal() {
        if (variant == null) {
            return new BigDecimal(0);
        }
        if (variant instanceof Boolean) {
            return (boolean) variant ? BigDecimal.ONE : BigDecimal.ZERO;
        }

        if (variant instanceof BigDecimal) {
            return (BigDecimal) variant;
        }
        return new BigDecimal(variant.toString());
    }

    public Boolean toBoolean() {
        if (variant instanceof Boolean) {
            return (Boolean) variant;
        } else if (variant instanceof Number) {
            return ((Number) variant).doubleValue() != 0;
        }
        return variant != null;
    }
    public Object getObject() {
        return getObject(type);
    }

    public Object getObject(ValueType tipoRequerido) {
        if (variant == null) {
            return null;
        }
        if (tipoRequerido == null) {
            tipoRequerido = type;
        }
        switch (tipoRequerido) {
            case BOOLEAN:
                return toBoolean();
            case STRING:
                return toString();
            case INTEGER:
                return toBigInteger();
            case DECIMAL:
                return toBigDecimal();
            default:
                return null;
        }
    }

}
