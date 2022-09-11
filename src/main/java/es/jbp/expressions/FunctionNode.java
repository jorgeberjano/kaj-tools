package es.jbp.expressions;

import java.util.ArrayList;
import java.util.List;


/**
 * Nodo del árbol de expresión que representa una función matemática.
 * @author Jorge Berjano
 */
public class FunctionNode implements ExpressionNode {
    private final Function function;
    private final List<ExpressionNode> parameterNodeList = new ArrayList<>();
    
    public static final int MULTIPLE_VALUES = -1;
    
    public FunctionNode(Function funcion) {
        this.function = funcion;
    }

    public int parameterCount() {
        return function.getParameterCount();
    }

    public boolean allowOmitParameters() {
        return function.allowOmitParameters();
    }

    /**
     * Añade un nodo de parámetro de entrada a la función
     */
    public void addOperand(ExpressionNode operandNode) {
        parameterNodeList.add(operandNode);
    }

    /**
     * Ejecuta la función asociada al nodo y devuelve el valor del calculo realizado
     */
    public Value evaluate() throws ExpressionException {
        List<Value> listaValores = new ArrayList<>();
        for(ExpressionNode parameterNode : parameterNodeList) {
            listaValores.add(parameterNode.evaluate());
        }
        return function.evaluate(listaValores);
    }
}
