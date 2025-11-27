package tech.hellsoft.trading.exception;

/**
 * Se lanza cuando una orden es inválida antes de enviarla:
 * cantidad <= 0, producto vacío, mensaje inválido, etc.
 */
public class ValidacionOrdenException extends TradingException {

    public ValidacionOrdenException(String message) {
        super(message);
    }
}
