package tech.hellsoft.trading.exception;

/**
 * Se lanza cuando no existe un precio de mercado disponible
 * para un producto al intentar comprar o vender.
 */
public class PrecioNoDisponibleException extends TradingException {

    public PrecioNoDisponibleException(String message) {
        super(message);
    }
}
