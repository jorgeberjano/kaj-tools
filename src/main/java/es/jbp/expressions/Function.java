package es.jbp.expressions;

import java.util.List;

/**
 * Contrato que deben cumplir las funciones que se usan en el compilador de expresiones.
 */
public interface Function {
    Value evaluate(List<Value> parameterList) throws ExpressionException;
    int getParameterCount();
    boolean allowOmitParameters();
}
