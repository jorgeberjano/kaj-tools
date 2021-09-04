package es.jbp.expressions;

/**
 * Nodo del árbol de expresión que representa una expresión.
 * @author Jorge Berjano
 */
public interface ExpressionNode {
    Value evaluate() throws ExpressionException;
}
