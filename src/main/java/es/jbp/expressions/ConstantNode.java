package es.jbp.expressions;

/**
 * Nodo del árbol de expresión que representa una constante.
 * @author Jorge Berjano
 */
public class ConstantNode implements ExpressionNode {

    private Value value;

    public ConstantNode(Value value) {
        this.value = value;
    }

    public Value evaluate() {
        return value;
    }

}
