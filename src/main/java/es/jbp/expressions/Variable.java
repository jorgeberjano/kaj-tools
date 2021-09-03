package es.jbp.expressions;

/**
 * Contrato que deben cumplir las variables que se usan en el compilador de expresiones.
 * @author Jorge Berjano
 */
public interface Variable {    
    Value getValor();
}
