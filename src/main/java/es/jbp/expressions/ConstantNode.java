package es.jbp.expressions;

/**
 * Nodo del árbol de expresión que representa una constante.
 * @author Jorge Berjano
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
