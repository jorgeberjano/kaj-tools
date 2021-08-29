package es.jbp.expressions;

/**
 *
 * @author Jorge
 */
public class ConstantNode implements ExpressionNode {
    
    public ConstantNode(Value valor) {
        this.valor = valor;
    }
    /*!
     * Devuelve el valor de la constante
     */
    public Value evaluar() {
        return valor;
    }
    
    private Value valor;
}
