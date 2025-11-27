package tech.hellsoft.trading.exception;

public class ConfiguracionInvalidaException extends ConfiguracionException {

  public ConfiguracionInvalidaException(String message) {
    super(message);
  }

  public ConfiguracionInvalidaException(String message, Throwable cause) {
    super(message, cause);
  }
}
