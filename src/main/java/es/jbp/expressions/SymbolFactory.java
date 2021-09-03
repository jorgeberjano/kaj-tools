package es.jbp.expressions;

/**
 * Contrato para las factorías de símbolos
 * @author Jorge Berjano
 */
public interface SymbolFactory {
    Variable createVariable(String name);
    Function createFunction(String name);
    Function createOperator(String name);
}
