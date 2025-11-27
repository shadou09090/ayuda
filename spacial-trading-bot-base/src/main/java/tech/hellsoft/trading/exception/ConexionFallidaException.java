package tech.hellsoft.trading.exception;

/**
 * Se lanza cuando el cliente no puede establecer la conexión con el
 * servidor de la bolsa.
 */
public class ConexionFallidaException extends Exception {

  public ConexionFallidaException(String message) {
    super(message);
  }

  public ConexionFallidaException(String message, Throwable cause) {
    super(message, cause);
  }
}
