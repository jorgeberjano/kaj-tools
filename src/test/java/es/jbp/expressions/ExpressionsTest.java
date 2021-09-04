package es.jbp.expressions;

import java.math.BigInteger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

/**
 * @author Jorge
 */
public class ExpressionsTest {

    private static ExpressionCompiler compiler;

    public ExpressionsTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        compiler = new ExpressionCompiler();
        compiler.setSymbolFactory(new SymbolFactory() {
            @Override
            public Variable createVariable(String nombre) {
                return new Variable() {
                    @Override
                    public Value getValor() {
                        return new Value(BigInteger.valueOf(nombre.length()));
                    }
                };
            }

            @Override
            public Function createFunction(String nombre) {
                return new Function() {
                    @Override
                    public Value evaluate(List<Value> parameterList) {
                        return new Value(BigInteger.valueOf(parameterList.size()));
                    }

                    @Override
                    public int getParameterCount() {
                        return FunctionNode.MULTIPLE_VALUES;
                    }

                    @Override
                    public boolean allowOmitParameters() {
                        return false;
                    }
                };
            }

            @Override
            public Function createOperator(String nombre) {
                return null;
            }
        });
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testOperators() throws ExpressionException {
        evaluar("2 + 5", BigInteger.valueOf(7));
        evaluar("1-1  ", BigInteger.ZERO);
        evaluar(" 1 or  0.0 ", true);
        evaluar("(1 - 1) or 0.0 ", false);
    }

    @Test
    public void testVariables() throws ExpressionException {
        evaluar("pepe + juan", BigInteger.valueOf(8));
        evaluar("pepe  - juan  ", BigInteger.ZERO);
        evaluar(" func(1, 23) ", BigInteger.valueOf(2));
        evaluar(" func() or 0.0 ", false);
    }

    @Test
    public void testFunctions() throws ExpressionException {
        evaluar(" func(func(1), func(\"2\"), func(0.0)) ", BigInteger.valueOf(3));
    }

    private void evaluar(Object expresion, Object resultadoEsperado) throws ExpressionException {

        ExpressionNode nodo = compiler.compile(expresion.toString());

        Value resultado = nodo.evaluate();
        Assert.assertEquals(resultadoEsperado, resultado.getObject());
    }
}
