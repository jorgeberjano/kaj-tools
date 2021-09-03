package es.jbp.expressions;

/**
 * Nodo del árbol de expresión que representa una variable
 * @author Jorge Berjano
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
