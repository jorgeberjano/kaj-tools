package es.jbp.expressions;

import java.util.Objects;

/**
 * @author Jorge Berjano
 */
public class Value {

    public enum ValueType {
        BOOLEAN,
        INTEGER,
        FLOAT,
        STRING
    }

    private final ValueType type;
    private final Object variant;

    public Value(boolean valor) {
        type = ValueType.BOOLEAN;
        variant = valor;
    }

    public Value(long valor) {
        type = ValueType.INTEGER;
        variant = valor;
    }

    public Value(double valor) {

        type = ValueType.FLOAT;
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

    public Long toLong() {
        if (variant == null) {
            return 0L;
        }
        if (variant instanceof Boolean) {
            return (boolean) variant ? 1L : 0L;
        }
        if (variant instanceof Number) {
            return ((Number) variant).longValue();
        }
        return Long.parseLong(variant.toString());
    }

    public Double toDouble() {
        if (variant == null) {
            return 0.0;
        }
        if (variant instanceof Boolean) {
            return (boolean) variant ? 1.0 : 0.0;
        }

        if (variant instanceof Double) {
            return (Double) variant;
        }
        return Double.parseDouble(variant.toString());
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
                return toLong();
            case FLOAT:
                return toDouble();
            default:
                return null;
        }
    }

}
