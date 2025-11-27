package tech.hellsoft.trading.exception;

/**
 * Se lanza cuando una oferta ya ha expirado localmente
 * y no puede ser aceptada.
 */
public class OfertaExpiradaException extends TradingException {

    public OfertaExpiradaException(String message) {
        super(message);
    }
}
