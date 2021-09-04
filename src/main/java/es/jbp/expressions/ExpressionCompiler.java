package es.jbp.expressions;

import es.jbp.expressions.Operator.Addition;
import es.jbp.expressions.Operator.Greater;
import es.jbp.expressions.Operator.GreaterEquals;
import es.jbp.expressions.Operator.Less;
import es.jbp.expressions.Operator.LessEquals;
import es.jbp.expressions.Operator.Multiplication;
import es.jbp.expressions.Operator.Subtract;
import es.jbp.expressions.Token.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Compilador de expresiones. Genera una estructura de árbol que representa la expresión en memoria y sirve para evaluar
 * su valor.
 * @author Jorge Berjano
 */
public class ExpressionCompiler {

  public static final String MISSING_CLOSING_PARENTHESIS = "Falta un paréntesis de cierre";
  public static final String INVALID_OPERATOR = "Operador inválido";
  public static final String SYNTAX_ERROR = "Error de sintaxis";
  public static final String VARIABLE_NOT_FOUND = "Variable no encontrada";
  public static final String FUNCTION_NOT_FOUND = "Función no reconocida";
  public static final String TOO_MANY_CLOSING_PARENTHESIS = "Demasiados paréntesis de cierre en la función";
  public static final String TOO_MANY_PARAMETERS = "Número de parámetros excesivo";
  public static final String MISSING_RIGTH_OPERAND = "Falta el operando derecho";
  public static final String MISSING_LEFT_OPERAND = "Falta el operando izquierdo";
  public static final String TOKEN_UNRECOGNIZED = "Token no reconocido";
  public static final String MISSING_EXPRESSION = "Falta la expresión";
  private final List<Token> TokenList = new ArrayList<>();

  private SymbolFactory symbolFactory;

  public ExpressionCompiler(SymbolFactory symbolFactory) {
    this.symbolFactory = symbolFactory;
  }

  public void setSymbolFactory(SymbolFactory symbolFactory) {
    this.symbolFactory = symbolFactory;
  }

  /**
   * Realiza el análisis lexico y sintáctico de una expresión
   */
  public ExpressionNode compile(String expression) throws ExpressionException {
    if (!lexicalAnalysis(expression)) {
      return null;
    }

    return syntacticalAnalysis();
  }

  private void error(String message, String tokens, int position) throws ExpressionException {
    throw new es.jbp.expressions.ExpressionException(message + " (" + position + "): " + tokens);
  }

  /*
   * Realiza el análisis lexico de una expresión: separa la expresión en tokens.
   */
  public boolean lexicalAnalysis(String expression) throws ExpressionException {
    TokenList.clear();
    if (expression.isEmpty()) {
      error(MISSING_EXPRESSION, "", 0);
      return false;
    }

    int baseIndex = 0;
    int currentIndex = 0;
    Token lastToken = null;

    while (currentIndex < expression.length()) {
      String fragment = mid(expression, baseIndex, currentIndex - baseIndex + 1);

      if ("\"".equals(fragment)) {
        currentIndex = expression.indexOf('"', currentIndex + 1);
        fragment = mid(expression, baseIndex, currentIndex - baseIndex + 1);
      }

      Type tokenType = determinarTipoDeToken(fragment);

      if (tokenType == null) {
        if (lastToken != null) {
          // se agrega el ultimo token valido que no sea un espacio
          if (lastToken.type != Type.SPACE) {
            addToken(lastToken);
            lastToken = null;
          }
          baseIndex = currentIndex;
          continue;
        } else {
          error(TOKEN_UNRECOGNIZED, fragment, baseIndex);
          return false;
        }
      } else { // Token valido
        lastToken = new Token(tokenType);
        lastToken.position = baseIndex;
        lastToken.text = fragment;
      }
      currentIndex++;
    }

    if (lastToken != null && lastToken.type != null &&
        lastToken.type != Type.SPACE) {
      addToken(lastToken);
    }

    return true;
  }

  /**
   * Agrega un token a la lista de tokens y le asignana la prioridad que le corresponda.
   */
  private void addToken(Token token) {
    Token ultimoToken = null;

    if (!TokenList.isEmpty()) {
      ultimoToken = TokenList.get(TokenList.size() - 1);
    }

    if (token.text.compareToIgnoreCase("OR") == 0) {
      token.priority = 0;
    } else if (token.text.compareToIgnoreCase("AND") == 0) {
      token.priority = 1;
    } else if ("+".equals(token.text) || "-".equals(token.text)) {
      if (ultimoToken != null &&
          (ultimoToken.type == Type.IDENTIFIER
              || ultimoToken.type == Type.NUMBER
              || ultimoToken.type == Type.STRING
              || ultimoToken.type == Type.CLOSE_PARENTHESIS)) {
        token.priority = 2;  // operador binario (suma y resta)
      } else {
        token.priority = 3; // operador unario (signo)
      }
    } else if ("*".equals(token.text) || "/".equals(token.text) || "%".equals(token.text)) {
      token.priority = 4;
    } else {
      token.priority = 5;
    }

    TokenList.add(token);
  }


  /**
   * Obtiene el tipo de token.
   */
  Type determinarTipoDeToken(String token) {

    for (Type tipoToken : Type.values()) {
      if (tipoToken.matches(token)) {
        return tipoToken;
      }
    }
    return null;
  }

  /**
   * Realiza el análisis sintáctico
   */
  private ExpressionNode syntacticalAnalysis() throws ExpressionException {
    return parsear(0, TokenList.size());
  }

  /**
   * Parsea el listado de tokens
   */
  private ExpressionNode parsear(int indicePrimero, int indiceUltimo) throws ExpressionException {
    if (TokenList.isEmpty()
        || indicePrimero >= TokenList.size()
        || indiceUltimo > TokenList.size()
        || indicePrimero >= indiceUltimo) {
      return null;
    }

    Token tokenOperador = null;
    int indiceOperador = -1;
    int nivelParentesis = 0;

    // Se busca el operarador de menor prioridad de derecha a izquierda
    for (int i = indiceUltimo - 1; i >= indicePrimero; i--) {
      Token token = TokenList.get(i);

      if (token.type == Type.OPEN_PARENTHESIS) {
        nivelParentesis--;
      } else if (token.type == Type.CLOSE_PARENTHESIS) {
        nivelParentesis++;
      } else if (token.type == Type.OPERATOR && nivelParentesis == 0) {
        // Si hay algun operador a la izquierda, se omite este operador
        if (i - 1 >= indicePrimero && TokenList.get(i - 1).type == Type.OPERATOR) {
          continue;
        }

        if (tokenOperador == null || tokenOperador.priority > token.priority) {
          tokenOperador = token;
          indiceOperador = i;
        }
      }
    }

    Token ultimoToken = TokenList.get(indiceUltimo - 1);

    if (nivelParentesis > 0) {
      error("No se esperaba el paréntesis de cierre", "", ultimoToken.position);
      return null;
    } else if (nivelParentesis < 0) {
      error(MISSING_CLOSING_PARENTHESIS, "", ultimoToken.position);
      return null;
    }

    // Hay un operador: se parsean los operandos y se agregan
    if (tokenOperador != null) {
      Function funcion = crearOperador(tokenOperador.text);
      if (funcion == null) {
        error(INVALID_OPERATOR, tokenOperador.text, ultimoToken.position);
        return null;
      }
      FunctionNode nodoOperador = new FunctionNode(funcion);
      ExpressionNode nodoOperando1 = parsear(indicePrimero, indiceOperador);
      if (nodoOperando1 != null) {
        nodoOperador.addOperand(nodoOperando1);
      } else if ("+".equals(tokenOperador.text) || "-".equals(tokenOperador.text)) {
        nodoOperador.addOperand(new ConstantNode(new Value(BigInteger.ZERO)));
      } else {
        error(MISSING_LEFT_OPERAND, tokenOperador.text, tokenOperador.position);
        return null;
      }

      ExpressionNode nodoOperando2 = parsear(indiceOperador + 1, indiceUltimo);
      if (nodoOperando2 != null) {
        nodoOperador.addOperand(nodoOperando2);
      } else {
        error(MISSING_RIGTH_OPERAND, tokenOperador.text, ultimoToken.position);
        return null;
      }
      return nodoOperador;
    }

    // No hay operadores...
    Token primerToken = TokenList.get(indicePrimero);
    Token segundoToken = (indicePrimero + 1 < indiceUltimo) ?
        TokenList.get(indicePrimero + 1) : null;

    // Es un literal (numero o cadena), una variable o un atributo
    if (segundoToken == null) {
      if (primerToken.type == Type.NUMBER) {
        return crearNodoConstante(primerToken);
      } else if (primerToken.type == Type.IDENTIFIER) {
        return crearNodoVariable(primerToken);
      } else if (primerToken.type == Type.STRING) {
        return crearNodoConstante(primerToken);
      }
    } else {

      // Es una función o un método
      if (segundoToken.type == Type.OPEN_PARENTHESIS && primerToken.type == Type.IDENTIFIER) {
        if (ultimoToken.type != Type.CLOSE_PARENTHESIS) {
          error(MISSING_CLOSING_PARENTHESIS, "", ultimoToken.position);
          return null;
        }

        FunctionNode nodoFuncion = crearNodoFuncion(primerToken);
        return parsearParametrosFuncion(nodoFuncion, primerToken.text, indicePrimero + 2,
            indiceUltimo - 1);
      }

    }

    // Es una expresión entre paréntesis
    if (primerToken.type == Type.OPEN_PARENTHESIS) {
      if (ultimoToken.type != Type.CLOSE_PARENTHESIS) {
        error(MISSING_CLOSING_PARENTHESIS, "", ultimoToken.position);
        return null;
      }
      return parsear(indicePrimero + 1, indiceUltimo - 1);
    }

    StringBuilder subexpresion = new StringBuilder();
    for (int i = indicePrimero; i < indiceUltimo; i++) {
      subexpresion.append(TokenList.get(i).text);
    }

    error(SYNTAX_ERROR, subexpresion.toString(), ultimoToken.position);

    return null;
  }

  public ConstantNode crearNodoConstante(Token token) {
    if (token.text.startsWith("\"")) {
      String valor = mid(token.text, 1, token.text.length() - 2);
      return new ConstantNode(new Value(valor));
    } else if (token.text.contains(".")) {
      BigDecimal valor = new BigDecimal(token.text);
      return new ConstantNode(new Value(valor));
    } else {
      BigInteger valor = new BigInteger(token.text);
      return new ConstantNode(new Value(valor));
    }
  }

  private VariableNode crearNodoVariable(Token token) throws ExpressionException {

    Variable variable = null;
    if (symbolFactory != null) {
      variable = symbolFactory.createVariable(token.text);
    }
    if (variable == null) {
      error(VARIABLE_NOT_FOUND, token.text, token.position);
      return null;
    }

    return new VariableNode(variable);
  }

  private FunctionNode crearNodoFuncion(Token token) throws ExpressionException {
    Function function = createFunction(token.text);
    if (function == null) {
      error(FUNCTION_NOT_FOUND, token.text, token.position);
      return null;
    }
    return new FunctionNode(function);
  }

  /**
   * Parsea todos los parametros de entrada a la funcion reconocida
   */
  private FunctionNode parsearParametrosFuncion(FunctionNode nodoFuncion, String nombreFuncion, int firstIndex,
      int lastIndex)
      throws ExpressionException {
    if (nodoFuncion == null) {
      return null;
    }

    int parenthesisLevel = 0;
    int parameterCount = 0;
    Token token = null;

    for (int i = firstIndex; i < lastIndex; i++) {
      token = TokenList.get(i);
      if (token.type == Type.OPEN_PARENTHESIS) {
        parenthesisLevel++;
      } else if (token.type == Type.CLOSE_PARENTHESIS) {
        parenthesisLevel--;
        if (parenthesisLevel < 0) {
          error(TOO_MANY_CLOSING_PARENTHESIS, nombreFuncion, token.position);
        }
      }
      boolean isLastToken = i == lastIndex - 1;

      if (parenthesisLevel == 0 && (token.type == Type.COLON || isLastToken)) {
        parameterCount++;

        if (nodoFuncion.parameterCount() != FunctionNode.MULTIPLE_VALUES
            && parameterCount > nodoFuncion.parameterCount()) {
          error(TOO_MANY_PARAMETERS, nombreFuncion, token.position);

          return null;
        }
        ExpressionNode parameter = parsear(firstIndex, isLastToken ? i + 1 : i);
        if (parameter != null) {
          nodoFuncion.addOperand(parameter);
        }
        firstIndex = i + 1;
      }
    }

    int position = token != null ? token.position : 0;

    if (parenthesisLevel != 0) {
      error("Falta el paréntesis de cierre en la función", nombreFuncion, position);
      return null;
    }

    if (nodoFuncion.parameterCount() != FunctionNode.MULTIPLE_VALUES
        && !nodoFuncion.allowOmitParameters()
        && nodoFuncion.parameterCount() > parameterCount) {
      error("Número de parámetros insuficiente", nombreFuncion, position);
      return null;
    }

    return nodoFuncion;
  }

  /**
   * Crea un función asociada a un token operador. Si hay operadores de usuario con el mismo nombre que el estándar se
   * devuelve éste.
   */
  private Function crearOperador(String functionName) {
    Function operator = null;
    if (symbolFactory != null) {
      operator = symbolFactory.createOperator(functionName);
      if (operator != null) {
        return operator;
      }
    }

    if ("+".equals(functionName)) {
      operator = new Addition();
    } else if ("-".equals(functionName)) {
      operator = new Subtract();
    } else if ("*".equals(functionName)) {
      operator = new Multiplication();
    } else if ("/".equals(functionName)) {
      operator = new Operator.Division();
    } else if ("<".equals(functionName)) {
      operator = new Less();
    } else if ("<=".equals(functionName)) {
      operator = new LessEquals();
    } else if (">".equals(functionName)) {
      operator = new Greater();
    } else if (">=".equals(functionName)) {
      operator = new GreaterEquals();
    } else if ("and".equals(functionName)) {
      operator = new Operator.And();
    } else if ("or".equals(functionName)) {
      operator = new Operator.Or();
    }
    return operator;
  }

  /**
   * Crea la función asociada al token. El token puede ser un operador, una función estándar o una función de usuario.
   * Si hay funciones de usuario con el mismo nombre que una estándar la se retorna la de usuario.
   */
  private Function createFunction(String functionName) {

    Function function = null;
    if (symbolFactory != null) {
      function = symbolFactory.createFunction(functionName);
    }
    return function;
  }

  private String mid(String texto, int position, int n) {
    return texto.substring(position, position + n);
  }
}
