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

  public void establecerSaldoInicial(double valor) {
    saldoInicial = valor;
    saldo = valor;
  }

  public void actualizarSaldo(double valor) {
    saldo = valor;
  }

  public void ajustarSaldo(double delta) {
    saldo += delta;
  }

  public double saldo() {
    return saldo;
  }

  public double saldoInicial() {
    return saldoInicial;
  }

  public Map<Product, Integer> inventario() {
    return new HashMap<>(inventario);
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
        inventario.put(producto, Math.max(0, cantidad));
      }
    }
  }

  public void consumirIngredientes(Recipe receta) {
    if (receta == null || receta.getIngredients() == null) {
      return;
    }
    for (Map.Entry<Product, Integer> entry : receta.getIngredients().entrySet()) {
      Product producto = entry.getKey();
      Integer requerido = entry.getValue();
      if (producto == null || requerido == null) {
        continue;
      }
      int disponible = inventario.getOrDefault(producto, 0);
      inventario.put(producto, disponible - requerido);
    }
  }

  public Map<Product, Double> precios() {
    return new HashMap<>(precios);
  }

  public Map<Product, Recipe> recetas() {
    Map<Product, Recipe> copia = new HashMap<>();
    for (Map.Entry<Product, RecetaLocal> entry : recetas.entrySet()) {
      Recipe receta = convertirReceta(entry.getValue());
      if (entry.getKey() != null && receta != null) {
        copia.put(entry.getKey(), receta);
      }
    }
    return copia;
  }

    public Set<String> productosAutorizadosComoTexto() {
        return productosAutorizados.stream()
                .map(Product::getValue)
                .collect(Collectors.toSet());
    }

    public void registrarPrecio(Product producto, double mid) {
    if (producto == null) {
      return;
    }
    precios.put(producto, mid);
  }

  public void asignarRecetas(Map<Product, Recipe> nuevasRecetas) {
    recetas.clear();
    if (nuevasRecetas == null) {
      return;
    }
    nuevasRecetas.forEach((producto, receta) -> asignarReceta(producto, receta));
  }

  public boolean complementarRecetas(Map<Product, Recipe> nuevasRecetas) {
    boolean modifico = false;
    if (nuevasRecetas == null || nuevasRecetas.isEmpty()) {
      return false;
    }
    for (Map.Entry<Product, Recipe> entry : nuevasRecetas.entrySet()) {
      Product producto = entry.getKey();
      Recipe receta = entry.getValue();
      if (producto == null || receta == null) {
        continue;
      }
      if (!recetas.containsKey(producto) || recetas.get(producto) == null) {
        RecetaLocal local = RecetaLocal.fromRecipe(receta);
        if (local != null) {
          recetas.put(producto, local);
          modifico = true;
        }
      }
    }
    return modifico;
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

  public void asignarProductosAutorizados(Set<Product> productos) {
    productosAutorizados.clear();
    if (productos != null) {
      for (Product producto : productos) {
        if (producto != null) {
          productosAutorizados.add(producto);
        }
      }
    }
  }

  public void asignarRol(TeamRole nuevoRol) {
    rol = TeamRoleLocal.from(nuevoRol);
  }

  public TeamRole rol() {
    if (rol == null) {
      return null;
    }
    return rol.toTeamRole();
  }

  public void sumarInventario(Product producto, int cantidad) {
    if (producto == null) {
      return;
    }
    inventario.put(producto, inventario.getOrDefault(producto, 0) + cantidad);
  }

  public void restarInventario(Product producto, int cantidad) {
    if (producto == null) {
      return;
    }
    inventario.put(producto, inventario.getOrDefault(producto, 0) - cantidad);
  }

  public int cantidadDisponible(Product producto) {
    return inventario.getOrDefault(producto, 0);
  }

  public double precioReferencia(Product producto) {
    return precios.getOrDefault(producto, 0.0);
  }

  public double calcularValorInventario() {
    double total = 0.0;
    for (Map.Entry<Product, Integer> entry : inventario.entrySet()) {
      double precio = precios.getOrDefault(entry.getKey(), 0.0);
      total += entry.getValue() * precio;
    }
    return total;
  }

  public double calcularPL() {
    if (saldoInicial <= 0.0) {
      return 0.0;
    }
    double patrimonio = saldo + calcularValorInventario();
    return ((patrimonio - saldoInicial) / saldoInicial) * 100.0;
  }

  public boolean productoAutorizado(Product producto) {
    return producto != null && productosAutorizados.contains(producto);
  }

  public Recipe recetaDe(Product producto) {
    RecetaLocal local = recetas.get(producto);
    return convertirReceta(local);
  }

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

  private Recipe convertirReceta(RecetaLocal local) {
    if (local == null) {
      return null;
    }
    return local.toRecipe();
  }
}
