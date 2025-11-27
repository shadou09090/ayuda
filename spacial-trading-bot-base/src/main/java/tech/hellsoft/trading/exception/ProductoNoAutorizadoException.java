package tech.hellsoft.trading.exception;

import java.util.Set;

public class ProductoNoAutorizadoException extends TradingException {

    private final String producto;
    private final Set<String> productosPermitidos;

    public ProductoNoAutorizadoException(String producto, Set<String> productosPermitidos) {
        super("Producto no autorizado: " + producto);
        this.producto = producto;
        this.productosPermitidos = productosPermitidos;
    }

    public String getProducto() {
        return producto;
    }

    public Set<String> getProductosPermitidos() {
        return productosPermitidos;
    }
}
