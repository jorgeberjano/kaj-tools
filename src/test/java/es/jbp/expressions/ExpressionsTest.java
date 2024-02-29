package es.jbp.expressions;

import org.junit.*;

import java.math.BigInteger;
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
        SymbolFactory symbolFactory = new SymbolFactory() {
            @Override
            public Variable getVariable(String nombre) {
                return new Variable() {
                    @Override
                    public Value getValue() {
                        return new Value(BigInteger.valueOf(nombre.length()));
                    }

                    @Override
                    public void setValue(Value value) {
                    }
                };
            }

            @Override
            public Function getFunction(String nombre) {
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
            public Function getOperator(String nombre) {
                return null;
            }
        };
        compiler = new ExpressionCompiler(symbolFactory);
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

    private void evaluar(Object expression, Object resultadoEsperado) throws ExpressionException {

        ExpressionNode nodo = compiler.compile(expression.toString());

        Value resultado = nodo.evaluate();
        Assert.assertEquals(resultadoEsperado, resultado.getObject());
    }
}
