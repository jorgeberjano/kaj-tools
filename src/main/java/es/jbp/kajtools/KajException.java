package es.jbp.kajtools;

public class KajException extends Exception {

  public KajException(String message) {
    super(message);
  }

  public KajException(String message, Throwable cause) {
    super(message, cause);
  }
}
