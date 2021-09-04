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

  public static final String FALTA_UN_PARENTESIS_DE_CIERRE = "Falta un paréntesis de cierre";
  public static final String OPERADOR_INVALIDO = "Operador inválido";
  public static final String ERROR_DE_SINTAXIS = "Error de sintaxis";
  public static final String VARIABLE_NO_ENCONTRADA = "Variable no encontrada";
  public static final String FUNCION_NO_RECONOCIDA = "Función no reconocida";
  public static final String DEMASIADOS_PARENTESIS_DE_CIERRE_EN_LA_FUNCION = "Demasiados paréntesis de cierre en la función";
  public static final String NUMERO_DE_PARAMETROS_EXCESIVO = "Número de parámetros excesivo";
  public static final String FALTA_EL_OPERANDO_DERECHO = "Falta el operando derecho";
  public static final String FALTA_EL_OPERANDO_IZQUIERDO = "Falta el operando izquierdo";
  public static final String TOKEN_NO_RECONOCIDO = "Token no reconocido";
  private final List<Token> listaTokens = new ArrayList<>();

  private SymbolFactory symbolFactory;

  public ExpressionCompiler(SymbolFactory symbolFactory) {
    this.symbolFactory = symbolFactory;
  }

  public void setSymbolFactory(SymbolFactory symbolFactory) {
    this.symbolFactory = symbolFactory;
  }

  /**
   * Realiza el analisis lexico y sintactico de la expresion que se le inyecta
   */
  public ExpressionNode compile(String expression) throws ExpressionException {
    if (!lexicalAnalisis(expression)) {
      return null;
    }

    return analisisSintactico();
  }

  private void error(String message, String tokens, int position) throws ExpressionException {
    throw new es.jbp.expressions.ExpressionException(message + " (" + position + "): " + tokens);
  }

  /*
   * Analisis lexico: separa la expresión en tokens.
   */
  public boolean lexicalAnalisis(String expression) throws ExpressionException {
    listaTokens.clear();
    if (expression.isEmpty()) {
      error("Falta la expresión", "", 0);
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
          error(TOKEN_NO_RECONOCIDO, fragment, baseIndex);
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

    if (!listaTokens.isEmpty()) {
      ultimoToken = listaTokens.get(listaTokens.size() - 1);
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

    listaTokens.add(token);
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
   * Realiza el analisis sintactico
   */
  private ExpressionNode analisisSintactico() throws ExpressionException {
    return parsear(0, listaTokens.size());
  }

  /**
   * Parsea el listado de tokens
   */
  private ExpressionNode parsear(int indicePrimero, int indiceUltimo) throws ExpressionException {
    if (listaTokens.isEmpty()
        || indicePrimero >= listaTokens.size()
        || indiceUltimo > listaTokens.size()
        || indicePrimero >= indiceUltimo) {
      return null;
    }

    Token tokenOperador = null;
    int indiceOperador = -1;
    int nivelParentesis = 0;

    // Se busca el operarador de menor prioridad de derecha a izquierda
    for (int i = indiceUltimo - 1; i >= indicePrimero; i--) {
      Token token = listaTokens.get(i);

      if (token.type == Type.OPEN_PARENTHESIS) {
        nivelParentesis--;
      } else if (token.type == Type.CLOSE_PARENTHESIS) {
        nivelParentesis++;
      } else if (token.type == Type.OPERATOR && nivelParentesis == 0) {
        // Si hay algun operador a la izquierda, se omite este operador
        if (i - 1 >= indicePrimero && listaTokens.get(i - 1).type == Type.OPERATOR) {
          continue;
        }

        if (tokenOperador == null || tokenOperador.priority > token.priority) {
          tokenOperador = token;
          indiceOperador = i;
        }
      }
    }

    Token ultimoToken = listaTokens.get(indiceUltimo - 1);

    if (nivelParentesis > 0) {
      error("No se esperaba el paréntesis de cierre", "", ultimoToken.position);
      return null;
    } else if (nivelParentesis < 0) {
      error(FALTA_UN_PARENTESIS_DE_CIERRE, "", ultimoToken.position);
      return null;
    }

    // Hay un operador: se parsean los operandos y se agregan
    if (tokenOperador != null) {
      Function funcion = crearOperador(tokenOperador.text);
      if (funcion == null) {
        error(OPERADOR_INVALIDO, tokenOperador.text, ultimoToken.position);
        return null;
      }
      FunctionNode nodoOperador = new FunctionNode(funcion);
      ExpressionNode nodoOperando1 = parsear(indicePrimero, indiceOperador);
      if (nodoOperando1 != null) {
        nodoOperador.agregarOperando(nodoOperando1);
      } else if ("+".equals(tokenOperador.text) || "-".equals(tokenOperador.text)) {
        nodoOperador.agregarOperando(new ConstantNode(new Value(BigInteger.ZERO)));
      } else {
        error(FALTA_EL_OPERANDO_IZQUIERDO, tokenOperador.text, tokenOperador.position);
        return null;
      }

      ExpressionNode nodoOperando2 = parsear(indiceOperador + 1, indiceUltimo);
      if (nodoOperando2 != null) {
        nodoOperador.agregarOperando(nodoOperando2);
      } else {
        error(FALTA_EL_OPERANDO_DERECHO, tokenOperador.text, ultimoToken.position);
        return null;
      }
      return nodoOperador;
    }

    // No hay operadores...
    Token primerToken = listaTokens.get(indicePrimero);
    Token segundoToken = (indicePrimero + 1 < indiceUltimo) ?
        listaTokens.get(indicePrimero + 1) : null;

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
          error(FALTA_UN_PARENTESIS_DE_CIERRE, "", ultimoToken.position);
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
        error(FALTA_UN_PARENTESIS_DE_CIERRE, "", ultimoToken.position);
        return null;
      }
      return parsear(indicePrimero + 1, indiceUltimo - 1);
    }

    StringBuilder subexpresion = new StringBuilder();
    for (int i = indicePrimero; i < indiceUltimo; i++) {
      subexpresion.append(listaTokens.get(i).text);
    }

    error(ERROR_DE_SINTAXIS, subexpresion.toString(), ultimoToken.position);

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
      error(VARIABLE_NO_ENCONTRADA, token.text, token.position);
      return null;
    }

    return new VariableNode(variable);
  }

  private FunctionNode crearNodoFuncion(Token token) throws ExpressionException {
    Function function = crearFuncion(token.text);
    if (function == null) {
      error(FUNCION_NO_RECONOCIDA, token.text, token.position);
      return null;
    }
    return new FunctionNode(function);
  }

  /**
   * Parsea todos los parametros de entrada a la funcion reconocida
   */
  private FunctionNode parsearParametrosFuncion(FunctionNode nodoFuncion, String nombreFuncion, int indicePrimero,
      int indiceUltimo)
      throws ExpressionException {
    if (nodoFuncion == null) {
      return null;
    }

    int nivelParentesis = 0;
    int contadorParametros = 0;
    Token token = null;

    for (int i = indicePrimero; i < indiceUltimo; i++) {
      token = listaTokens.get(i);
      if (token.type == Type.OPEN_PARENTHESIS) {
        nivelParentesis++;
      } else if (token.type == Type.CLOSE_PARENTHESIS) {
        nivelParentesis--;
        if (nivelParentesis < 0) {
          error(DEMASIADOS_PARENTESIS_DE_CIERRE_EN_LA_FUNCION, nombreFuncion, token.position);
        }
      }
      boolean esUltimoToken = i == indiceUltimo - 1;

      if (nivelParentesis == 0 && (token.type == Type.COLON || esUltimoToken)) {
        contadorParametros++;

        if (nodoFuncion.numeroParametrosEntrada() != FunctionNode.MULTIPLES_VALORES
            && contadorParametros > nodoFuncion.numeroParametrosEntrada()) {
          error(NUMERO_DE_PARAMETROS_EXCESIVO, nombreFuncion, token.position);

          return null;
        }
        ExpressionNode parametro = parsear(indicePrimero, esUltimoToken ? i + 1 : i);
        if (parametro != null) {
          nodoFuncion.agregarOperando(parametro);
        }
        indicePrimero = i + 1;
      }
    }

    int posicion = token != null ? token.position : 0;

    if (nivelParentesis != 0) {
      error("Falta el paréntesis de cierre en la función", nombreFuncion, posicion);
      return null;
    }

    if (nodoFuncion.numeroParametrosEntrada() != FunctionNode.MULTIPLES_VALORES
        && !nodoFuncion.allowOmitParameters()
        && nodoFuncion.numeroParametrosEntrada() > contadorParametros) {
      error("Número de parámetros insuficiente", nombreFuncion, posicion);
      return null;
    }

    return nodoFuncion;
  }

  /**
   * Crea un función asociada a un token operador. Si hay operadores de usuario con el mismo nombre que el estándar se
   * devuelve éste.
   */
  private Function crearOperador(String nombreFuncion) {
    Function operator = null;
    if (symbolFactory != null) {
      operator = symbolFactory.createOperator(nombreFuncion);
      if (operator != null) {
        return operator;
      }
    }

    if ("+".equals(nombreFuncion)) {
      operator = new Addition();
    } else if ("-".equals(nombreFuncion)) {
      operator = new Subtract();
    } else if ("*".equals(nombreFuncion)) {
      operator = new Multiplication();
    } else if ("/".equals(nombreFuncion)) {
      operator = new Operator.Division();
    } else if ("<".equals(nombreFuncion)) {
      operator = new Less();
    } else if ("<=".equals(nombreFuncion)) {
      operator = new LessEquals();
    } else if (">".equals(nombreFuncion)) {
      operator = new Greater();
    } else if (">=".equals(nombreFuncion)) {
      operator = new GreaterEquals();
    } else if ("and".equals(nombreFuncion)) {
      operator = new Operator.And();
    } else if ("or".equals(nombreFuncion)) {
      operator = new Operator.Or();
    }
    return operator;
  }

  /**
   * Crea la función asociada al token. El token puede ser un operador, una función estándar o una función de usuario.
   * Si hay funciones de usuario con el mismo nombre que una estándar la se retorna la de usuario.
   */
  private Function crearFuncion(String nombreFuncion) {

    Function function = null;
    if (symbolFactory != null) {
      function = symbolFactory.createFunction(nombreFuncion);
    }
    return function;
  }

  private String mid(String texto, int posicion, int n) {
    return texto.substring(posicion, posicion + n);
  }
}
