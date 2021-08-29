package es.jbp.expressions;

/**
 *
 * @author Jorge
 */
public class VariableNode implements ExpressionNode {
    
    private  Variable variable;
    
    public VariableNode(Variable variable) {
        this.variable = variable;
    }
    
    /*!
     * Devuelve el valor de la variable
     */
    public Value evaluar() {
        return variable.getValor();
    }

    /*!
     * Devuelve la variable contenida en el nodo.
     */
    public Variable getVariable() {
        return variable;
    }
}
