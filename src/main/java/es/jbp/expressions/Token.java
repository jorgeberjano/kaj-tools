package es.jbp.expressions;

import java.util.regex.Pattern;

/**
 * Representa un token generado por el analizador lexico.
 * @author Jorge Berjano
 */
public class Token {

  public enum Type {
    SPACE("[\\s\\t\\r\\n]+"),
    OPERATOR("(<|<=|>|>=|=|<>|\\+|\\-|\\*|/|\\^|%|[Aa][Nn][Dd]|[Oo][Rr])"),
    NUMBER("[0-9]*\\.?[0-9]*([eE][-+]?[0-9]*)?"),
    COLON(","),
    IDENTIFIER("[A-Za-z_][A-Za-z_0-9]*[\\.]*[A-Za-z_0-9]*"),
    STRING("\"[^\"]*\""),
    OPEN_PARENTHESIS("\\("),
    CLOSE_PARENTHESIS("\\)");

    private Pattern regexPattern;

    Type(String regex) {
      regexPattern = Pattern.compile(regex);
    }

    public boolean matches(String token) {
      return regexPattern.matcher(token).matches();
    }
  }

  Type type;
  String text;
  int position;
  int priority;

  public Token(Type type) {
    this.type = type;
    position = 0;
    priority = 0;
  }
}
