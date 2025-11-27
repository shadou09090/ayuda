package tech.hellsoft.trading.exception;

public abstract class ConfiguracionException extends Exception {

  public ConfiguracionException(String message) {
    super(message);
  }

  public ConfiguracionException(String message, Throwable cause) {
    super(message, cause);
  }
}
