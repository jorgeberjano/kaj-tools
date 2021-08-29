package es.jbp.expressions;

/**
 *
 * @author Jorge
 */
public interface ExpressionNode {
    Value evaluar() throws ExpressionException;
}
