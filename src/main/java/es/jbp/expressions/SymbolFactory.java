package es.jbp.expressions;

/**
 * Contrato para las factorías de símbolos
 * @author Jorge Berjano
 */
public interface SymbolFactory {
    Variable getVariable(String name);
    Function getFunction(String name);
    Function getOperator(String name);
}
