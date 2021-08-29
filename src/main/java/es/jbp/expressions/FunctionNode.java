package es.jbp.expressions;

import java.util.ArrayList;
import java.util.List;


/**
 * Nodo del arbol de expresión que representa una función matemática.
 */
public class FunctionNode implements ExpressionNode {
    private final Function funcion;
    private final List<ExpressionNode> listaNodosParametros = new ArrayList<>();
    
    public static final int MULTIPLES_VALORES = -1;
    
    public FunctionNode(Function funcion) {
        this.funcion = funcion;
    }

    public int numeroParametrosEntrada() {
        return funcion.getParameterCount();
    }

    public boolean allowOmitParameters() {
        return funcion.allowOmitParameters();
    }

    /**
     * Añade un nodo de parámetro de entrada a la función
     */
    public void agregarOperando(ExpressionNode nodoOperando) {
        listaNodosParametros.add(nodoOperando);
    }

    /**
     * Ejecuta la función asociada al nodo y devuelve el valor del calculo realizado
     */
    public Value evaluar() throws ExpressionException {
        List<Value> listaValores = new ArrayList<>();
        for(ExpressionNode nodoOperando : listaNodosParametros) {
            listaValores.add(nodoOperando.evaluar());
        }
        return funcion.evaluate(listaValores);
    }
}
