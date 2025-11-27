package tech.hellsoft.config.exception;

/**
 * Se lanza cuando el inventario local no tiene suficientes unidades para
 * completar una operación de venta o para consumir ingredientes durante la
 * producción.
 */
public final class InventarioInsuficienteException extends TradingException {

  public InventarioInsuficienteException(String message) {
    super(message);
  }
}
