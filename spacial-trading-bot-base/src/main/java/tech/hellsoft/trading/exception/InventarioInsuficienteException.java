package tech.hellsoft.trading.exception;

import tech.hellsoft.trading.enums.Product;

/**
 * Se lanza cuando el inventario local no tiene suficientes unidades para
 * completar una operación (venta, producir, aceptar oferta).
 *
 * Requerido por el documento:
 *  - producto involucrado
 *  - cantidad disponible
 *  - cantidad requerida
 */
public final class InventarioInsuficienteException extends TradingException {

    private final Product producto;
    private final int disponible;
    private final int requerido;

    public InventarioInsuficienteException(Product producto, int disponible, int requerido) {
        super(String.format(
                "Inventario insuficiente para %s: disponible=%d, requerido=%d",
                producto != null ? producto.getValue() : "Producto desconocido",
                disponible,
                requerido
        ));
        this.producto = producto;
        this.disponible = disponible;
        this.requerido = requerido;
    }

    public Product getProducto() {
        return producto;
    }

    public int getDisponible() {
        return disponible;
    }

    public int getRequerido() {
        return requerido;
    }
}
