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
  public static final String FUNCTION_NOT_FOUND = "Función no encotrada";
  public static final String TOO_MANY_CLOSING_PARENTHESIS = "Demasiados paréntesis de cierre en la función";
  public static final String TOO_MANY_PARAMETERS = "Número de parámetros excesivo";
  public static final String MISSING_RIGHT_OPERAND = "Falta el operando derecho";
  public static final String MISSING_LEFT_OPERAND = "Falta el operando izquierdo";
  public static final String UNRECOGNIZED_TOKEN = "Token no reconocido";
  public static final String MISSING_EXPRESSION = "Falta la expresión";
  public static final String UNEXPECTED_CLOSING_PARENTHESIS = "No se esperaba el paréntesis de cierre";
  public static final String MISSING_FUNCTION_CLOSING_PARENTHESIS = "Falta el paréntesis de cierre en la función";
  public static final String TOO_FEW_PARAMETERS = "Número de parámetros insuficiente";
  private final List<Token> tokenList = new ArrayList<>();

  private SymbolFactory symbolFactory;

  public ExpressionCompiler() {
  }

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
    tokenList.clear();
    if (expression.isEmpty()) {
      error(MISSING_EXPRESSION, "", 0);
      return false;
    }

    int baseIndex = 0;
    int currentIndex = 0;
    Token lastToken = null;

    while (currentIndex < expression.length()) {
      String fragment = mid(expression, baseIndex, currentIndex - baseIndex + 1);

      Type tokenType = determinarTipoDeToken(fragment);

      if (tokenType == null) {
        if (lastToken != null) {
          // Se agrega el ultimo token valido
          addToken(lastToken);
          lastToken = null;
          baseIndex = currentIndex;
          continue;
        }
      } else { // Token valido
        lastToken = new Token(tokenType);
        lastToken.position = baseIndex;
        lastToken.text = fragment;
      }
      currentIndex++;
    }

    if (lastToken != null && lastToken.type != null) {
      addToken(lastToken);
    }

    return true;
  }

  /**
   * Agrega un token a la lista de tokens y le asigna la prioridad que le corresponda.
   */
  private void addToken(Token token) {

    if (token.type == Type.SPACE) {
      return;
    }

    Token ultimoToken = null;

    if (!tokenList.isEmpty()) {
      ultimoToken = tokenList.get(tokenList.size() - 1);
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

    tokenList.add(token);
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
    return parse(0, tokenList.size());
  }

  /**
   * Parsea el listado de tokens
   */
  private ExpressionNode parse(int firstIndex, int lastIndex) throws ExpressionException {
    if (tokenList.isEmpty()
        || firstIndex >= tokenList.size()
        || lastIndex > tokenList.size()
        || firstIndex >= lastIndex) {
      return null;
    }

    Token operatorToken = null;
    int operatorIndex = -1;
    int parenthesisLevel = 0;

    // Se busca el operador de menor prioridad de derecha a izquierda
    for (int i = lastIndex - 1; i >= firstIndex; i--) {
      Token token = tokenList.get(i);

      if (token.type == Type.OPEN_PARENTHESIS) {
        parenthesisLevel--;
      } else if (token.type == Type.CLOSE_PARENTHESIS) {
        parenthesisLevel++;
      } else if (token.type == Type.OPERATOR && parenthesisLevel == 0) {
        // Si hay algun operador a la izquierda, se omite este operador
        if (i - 1 >= firstIndex && tokenList.get(i - 1).type == Type.OPERATOR) {
          continue;
        }

        if (operatorToken == null || operatorToken.priority > token.priority) {
          operatorToken = token;
          operatorIndex = i;
        }
      }
    }

    Token lastToken = tokenList.get(lastIndex - 1);

    if (parenthesisLevel > 0) {
      error(UNEXPECTED_CLOSING_PARENTHESIS, "", lastToken.position);
      return null;
    } else if (parenthesisLevel < 0) {
      error(MISSING_CLOSING_PARENTHESIS, "", lastToken.position);
      return null;
    }

    // Hay un operador: se parsean los operandos y se agregan
    if (operatorToken != null) {
      Function function = createOperator(operatorToken.text);
      if (function == null) {
        error(INVALID_OPERATOR, operatorToken.text, lastToken.position);
        return null;
      }
      FunctionNode operatorNode = new FunctionNode(function);
      ExpressionNode operandoNode1 = parse(firstIndex, operatorIndex);
      if (operandoNode1 != null) {
        operatorNode.addOperand(operandoNode1);
      } else if ("+".equals(operatorToken.text) || "-".equals(operatorToken.text)) {
        operatorNode.addOperand(new ConstantNode(new Value(BigInteger.ZERO)));
      } else {
        error(MISSING_LEFT_OPERAND, operatorToken.text, operatorToken.position);
        return null;
      }

      ExpressionNode operandNode2 = parse(operatorIndex + 1, lastIndex);
      if (operandNode2 != null) {
        operatorNode.addOperand(operandNode2);
      } else {
        error(MISSING_RIGHT_OPERAND, operatorToken.text, lastToken.position);
        return null;
      }
      return operatorNode;
    }

    // No hay operadores...
    Token primerToken = tokenList.get(firstIndex);
    Token segundoToken = (firstIndex + 1 < lastIndex) ?
        tokenList.get(firstIndex + 1) : null;

    // Es un literal (numero o cadena), una variable o un atributo
    if (segundoToken == null) {
      if (primerToken.type == Type.NUMBER) {
        return createConstantNode(primerToken);
      } else if (primerToken.type == Type.IDENTIFIER) {
        return createVariableNode(primerToken);
      } else if (primerToken.type == Type.STRING) {
        return createConstantNode(primerToken);
      }
    } else {

      // Es una función o un método
      if (segundoToken.type == Type.OPEN_PARENTHESIS && primerToken.type == Type.IDENTIFIER) {
        if (lastToken.type != Type.CLOSE_PARENTHESIS) {
          error(MISSING_CLOSING_PARENTHESIS, "", lastToken.position);
          return null;
        }

        FunctionNode nodoFuncion = createFunctionNode(primerToken);
        return parseFunctionParameters(nodoFuncion, primerToken.text, firstIndex + 2,
            lastIndex - 1);
      }

    }

    // Es una expresión entre paréntesis
    if (primerToken.type == Type.OPEN_PARENTHESIS) {
      if (lastToken.type != Type.CLOSE_PARENTHESIS) {
        error(MISSING_CLOSING_PARENTHESIS, "", lastToken.position);
        return null;
      }
      return parse(firstIndex + 1, lastIndex - 1);
    }

    StringBuilder subexpresion = new StringBuilder();
    for (int i = firstIndex; i < lastIndex; i++) {
      subexpresion.append(tokenList.get(i).text);
    }

    error(SYNTAX_ERROR, subexpresion.toString(), lastToken.position);

    return null;
  }

  public ConstantNode createConstantNode(Token token) {
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

  private VariableNode createVariableNode(Token token) throws ExpressionException {

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

  private FunctionNode createFunctionNode(Token token) throws ExpressionException {
    Function function = createFunction(token.text);
    if (function == null) {
      error(FUNCTION_NOT_FOUND, token.text, token.position);
      return null;
    }
    return new FunctionNode(function);
  }

  private FunctionNode parseFunctionParameters(FunctionNode functionNode, String functionName, int firstIndex,
      int lastIndex)
      throws ExpressionException {
    if (functionNode == null) {
      return null;
    }

    int parenthesisLevel = 0;
    int parameterCount = 0;
    Token token = null;

    for (int i = firstIndex; i < lastIndex; i++) {
      token = tokenList.get(i);
      if (token.type == Type.OPEN_PARENTHESIS) {
        parenthesisLevel++;
      } else if (token.type == Type.CLOSE_PARENTHESIS) {
        parenthesisLevel--;
        if (parenthesisLevel < 0) {
          error(TOO_MANY_CLOSING_PARENTHESIS, functionName, token.position);
        }
      }
      boolean isLastToken = i == lastIndex - 1;

      if (parenthesisLevel == 0 && (token.type == Type.COLON || isLastToken)) {
        parameterCount++;

        if (functionNode.parameterCount() != FunctionNode.MULTIPLE_VALUES
            && parameterCount > functionNode.parameterCount()) {
          error(TOO_MANY_PARAMETERS, functionName, token.position);

          return null;
        }
        ExpressionNode parameter = parse(firstIndex, isLastToken ? i + 1 : i);
        if (parameter != null) {
          functionNode.addOperand(parameter);
        }
        firstIndex = i + 1;
      }
    }

    int position = token != null ? token.position : 0;

    if (parenthesisLevel != 0) {
      error(MISSING_FUNCTION_CLOSING_PARENTHESIS, functionName, position);
      return null;
    }

    if (functionNode.parameterCount() != FunctionNode.MULTIPLE_VALUES
        && !functionNode.allowOmitParameters()
        && functionNode.parameterCount() > parameterCount) {
      error(TOO_FEW_PARAMETERS, functionName, position);
      return null;
    }

    return functionNode;
  }

  /**
   * Crea un función asociada a un token operador. Si hay operadores de usuario con el mismo nombre que el estándar se
   * devuelve éste.
   */
  private Function createOperator(String functionName) {
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
