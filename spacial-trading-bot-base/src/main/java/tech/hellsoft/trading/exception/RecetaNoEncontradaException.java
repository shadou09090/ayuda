package tech.hellsoft.trading.exception;

public class RecetaNoEncontradaException extends ProduccionException {

    private final String producto;

    public RecetaNoEncontradaException(String producto) {
        super("No existe receta para el producto: " + producto);
        this.producto = producto;
    }

    public String getProducto() {
        return producto;
    }
}
