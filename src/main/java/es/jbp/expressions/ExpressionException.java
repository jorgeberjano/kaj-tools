package es.jbp.expressions;

/**
 * Excepción que produce el compilador de expresiones cuando hay un error de sintaxis o al evaluar un nodo expresión
 * cuando se produce un error de ejecución.
 * @author Jorge Berjano
 */
public class ExpressionException extends Exception {

  public ExpressionException(String message) {
    super(message);
  }
}
