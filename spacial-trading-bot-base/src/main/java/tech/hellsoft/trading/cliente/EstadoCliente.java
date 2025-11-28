package tech.hellsoft.trading.cliente;

import java.util.stream.Collectors;
import tech.hellsoft.trading.dto.local.RecetaLocal;
import tech.hellsoft.trading.dto.local.TeamRoleLocal;
import tech.hellsoft.trading.dto.server.Recipe;
import tech.hellsoft.trading.dto.server.TeamRole;
import tech.hellsoft.trading.enums.Product;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class EstadoCliente implements Serializable {

    private static final long serialVersionUID = 1L;

    private double saldo;
    private double saldoInicial;

    private final Map<Product, Integer> inventario = new HashMap<>();
    private final Map<Product, Double> precios = new HashMap<>();
    private final Map<Product, RecetaLocal> recetas = new HashMap<>();
    private final Set<Product> productosAutorizados = new HashSet<>();

    private TeamRoleLocal rol;

    // SALDO
    public void establecerSaldoInicial(double valor) {
        saldoInicial = valor;
        saldo = valor;
    }

    public void actualizarSaldo(double valor) {
        saldo = valor;
    }

    public void ajustarSaldo(double delta) {
        double nuevoSaldo = saldo + delta;
        saldo = nuevoSaldo;
    }

    public double saldo() {
        return saldo;
    }

    public double saldoInicial() {
        return saldoInicial;
    }
    // INVENTARIO
    public Map<Product, Integer> inventario() {
        // Se crea una copia para no exponer el mapa real
        Map<Product, Integer> copia = new HashMap<>();
        copia.putAll(inventario);
        return copia;
    }

    public void reemplazarInventario(Map<Product, Integer> nuevoInventario) {
        inventario.clear();

        if (nuevoInventario == null) {
            return;
        }

        for (Map.Entry<Product, Integer> entry : nuevoInventario.entrySet()) {
            Product producto = entry.getKey();
            Integer cantidad = entry.getValue();

            if (producto != null && cantidad != null) {
                int cantidadFinal = Math.max(0, cantidad);
                inventario.put(producto, cantidadFinal);
            }
        }
    }

    public void consumirIngredientes(Recipe receta) {
        if (receta == null) {
            return;
        }

        Map<Product, Integer> ingredientes = receta.getIngredients();
        if (ingredientes == null) {
            return;
        }

        for (Map.Entry<Product, Integer> entry : ingredientes.entrySet()) {
            Product producto = entry.getKey();
            Integer requerido = entry.getValue();

            if (producto == null || requerido == null) {
                continue;
            }

            int disponible = inventario.getOrDefault(producto, 0);
            int restante = disponible - requerido;
            inventario.put(producto, restante);
        }
    }

    public void sumarInventario(Product producto, int cantidad) {
        if (producto == null) {
            return;
        }

        int actual = inventario.getOrDefault(producto, 0);
        int nuevoValor = actual + cantidad;

        inventario.put(producto, nuevoValor);
    }

    public void restarInventario(Product producto, int cantidad) {
        if (producto == null) {
            return;
        }

        int actual = inventario.getOrDefault(producto, 0);
        int nuevoValor = actual - cantidad;

        inventario.put(producto, nuevoValor);
    }

    public int cantidadDisponible(Product producto) {
        return inventario.getOrDefault(producto, 0);
    }

    // PRECIOS
    public Map<Product, Double> precios() {
        Map<Product, Double> copia = new HashMap<>();
        copia.putAll(precios);
        return copia;
    }

    public void registrarPrecio(Product producto, double mid) {
        if (producto == null) {
            return;
        }

        precios.put(producto, mid);
    }

    public double precioReferencia(Product producto) {
        return precios.getOrDefault(producto, 0.0);
    }

    // RECETAS
    public Map<Product, Recipe> recetas() {
        Map<Product, Recipe> copia = new HashMap<>();

        for (Map.Entry<Product, RecetaLocal> entry : recetas.entrySet()) {
            Product producto = entry.getKey();
            RecetaLocal local = entry.getValue();

            Recipe recetaConvertida = convertirReceta(local);

            if (producto != null && recetaConvertida != null) {
                copia.put(producto, recetaConvertida);
            }
        }

        return copia;
    }

    public void asignarRecetas(Map<Product, Recipe> nuevasRecetas) {
        recetas.clear();

        if (nuevasRecetas == null) {
            return;
        }

        for (Map.Entry<Product, Recipe> entry : nuevasRecetas.entrySet()) {
            Product producto = entry.getKey();
            Recipe receta = entry.getValue();

            asignarReceta(producto, receta);
        }
    }

    public boolean complementarRecetas(Map<Product, Recipe> nuevasRecetas) {
        boolean cambio = false;

        if (nuevasRecetas == null || nuevasRecetas.isEmpty()) {
            return false;
        }

        for (Map.Entry<Product, Recipe> entry : nuevasRecetas.entrySet()) {

            Product producto = entry.getKey();
            Recipe receta = entry.getValue();

            if (producto == null || receta == null) {
                continue;
            }

            RecetaLocal actual = recetas.get(producto);

            if (actual == null) {
                RecetaLocal creada = RecetaLocal.fromRecipe(receta);

                if (creada != null) {
                    recetas.put(producto, creada);
                    cambio = true;
                }
            }
        }

        return cambio;
    }

    public void asignarReceta(Product producto, Recipe receta) {
        if (producto == null || receta == null) {
            return;
        }

        RecetaLocal local = RecetaLocal.fromRecipe(receta);

        if (local != null) {
            recetas.put(producto, local);
        }
    }

    public Recipe recetaDe(Product producto) {
        RecetaLocal local = recetas.get(producto);
        return convertirReceta(local);
    }

    private Recipe convertirReceta(RecetaLocal local) {
        if (local == null) {
            return null;
        }
        return local.toRecipe();
    }
    // PRODUCTOS AUTORIZADOS
    public void asignarProductosAutorizados(Set<Product> productos) {
        productosAutorizados.clear();

        if (productos == null) {
            return;
        }

        for (Product p : productos) {
            if (p != null) {
                productosAutorizados.add(p);
            }
        }
    }

    public Set<String> productosAutorizadosComoTexto() {
        return productosAutorizados.stream()
                .map(Product::getValue).collect(Collectors.toSet());
    }

    public boolean productoAutorizado(Product producto) {
        if (producto == null) {
            return false;
        }
        return productosAutorizados.contains(producto);
    }

    // ---------------------------------------------------------
    // ROL
    // ---------------------------------------------------------

    public void asignarRol(TeamRole nuevoRol) {
        rol = TeamRoleLocal.from(nuevoRol);
    }

    public TeamRole rol() {
        if (rol == null) {
            return null;
        }
        return rol.toTeamRole();
    }

    // CÁLCULOS
    public double calcularValorInventario() {
        double total = 0.0;

        for (Map.Entry<Product, Integer> entry : inventario.entrySet()) {
            Product producto = entry.getKey();
            Integer cantidad = entry.getValue();

            double precio = precios.getOrDefault(producto, 0.0);
            double aporte = cantidad * precio;

            total += aporte;
        }
        return total;
    }

    public double calcularPL() {
        if (saldoInicial <= 0.0) {
            return 0.0;
        }

        double patrimonio = saldo + calcularValorInventario();
        double diferencia = patrimonio - saldoInicial;

        return (diferencia / saldoInicial) * 100.0;
    }

    // COPIA COMPLETA DEL ESTADO
    public void copiarDesde(EstadoCliente origen) {
        if (origen == null) {
            return;
        }

        saldo = origen.saldo;
        saldoInicial = origen.saldoInicial;

        inventario.clear();
        inventario.putAll(origen.inventario);

        precios.clear();
        precios.putAll(origen.precios);

        recetas.clear();
        recetas.putAll(origen.recetas);

        productosAutorizados.clear();
        productosAutorizados.addAll(origen.productosAutorizados);

        rol = origen.rol;
    }
}