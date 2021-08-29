package es.jbp.expressions;

/**
 * Contrato para las factorias de simbolos
 * @author Jorge
 */
public interface SymbolFactory {
    Variable createVariable(String name);
    Function createFunction(String name);
    Function createOperator(String name);
}
