package tech.hellsoft.config;

import tech.hellsoft.trading.enums.Product;
import tech.hellsoft.trading.exception.IngredientesInsuficientesException;
import tech.hellsoft.trading.exception.InventarioInsuficienteException;
import tech.hellsoft.trading.exception.ProductoNoAutorizadoException;
import tech.hellsoft.trading.exception.RecetaNoEncontradaException;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;


public final class AutoProduccionManager {

  private final ClienteBolsa cliente;
  private final EstadoCliente estado;
  private Timer timer;
  private volatile boolean activo;
  private volatile boolean premium;
  private volatile long intervaloMs;
  private volatile Product productoObjetivo;

  public AutoProduccionManager(ClienteBolsa clienteBolsa, EstadoCliente estadoCliente) {
    this.cliente = Objects.requireNonNull(clienteBolsa, "cliente");
    this.estado = Objects.requireNonNull(estadoCliente, "estado");
  }

  public synchronized void iniciar(String nombreProducto, boolean premiumSolicitado, long intervaloSegundos)
      throws ProductoNoAutorizadoException {
    validarParametros(nombreProducto, intervaloSegundos);
    Product producto = cliente.resolverProducto(nombreProducto);
    detenerInterno();

    productoObjetivo = producto;
    premium = premiumSolicitado;
    intervaloMs = Math.max(1000L, intervaloSegundos * 1000L);
    timer = new Timer("auto-produccion", true);
    activo = true;
    timer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        ejecutarCiclo();
      }
    }, 0L, intervaloMs);
    System.out.printf("🤖 AutoProducción activa → %s (%s) cada %d segundos%n", producto.getValue(),
        premium ? "premium" : "básica", intervaloSegundos);
  }

  public synchronized void detener() {
    if (!activo) {
      System.out.println("AutoProducción ya estaba detenida.");
      return;
    }
    detenerInterno();
    System.out.println("AutoProducción detenida.");
  }

  public boolean activo() {
    return activo;
  }

  public String productoActual() {
    Product producto = productoObjetivo;
    if (producto == null) {
      return null;
    }
    return producto.getValue();
  }

  public boolean modoPremium() {
    return premium;
  }

  public long intervaloSegundos() {
    if (intervaloMs <= 0) {
      return 0L;
    }
    return intervaloMs / 1000L;
  }

  private void ejecutarCiclo() {
    if (!activo) {
      return;
    }
    Product objetivo = productoObjetivo;
    if (objetivo == null) {
      return;
    }
    try {
      cliente.producir(objetivo.getValue(), premium);
      liquidarInventario(objetivo);
    } catch (ProductoNoAutorizadoException | RecetaNoEncontradaException | IngredientesInsuficientesException e) {
      System.out.println("⚠️ AutoProducción (producción): " + e.getMessage());
    } catch (RuntimeException e) {
      System.out.println("⚠️ AutoProducción inesperada: " + e.getMessage());
    }
  }

  private void liquidarInventario(Product objetivo) {
    int disponible = estado.cantidadDisponible(objetivo);
    if (disponible <= 0) {
      return;
    }
    try {
      cliente.vender(objetivo.getValue(), disponible, "AutoProducción");
    } catch (ProductoNoAutorizadoException | InventarioInsuficienteException e) {
      System.out.println("⚠️ AutoProducción (venta): " + e.getMessage());
    }
  }

  private void validarParametros(String nombreProducto, long intervaloSegundos) {
    if (nombreProducto == null || nombreProducto.isBlank()) {
      throw new IllegalArgumentException("Debes indicar un producto objetivo.");
    }
    if (intervaloSegundos <= 0) {
      throw new IllegalArgumentException("El intervalo debe ser mayor a cero segundos.");
    }
  }

  private synchronized void detenerInterno() {
    if (timer != null) {
      timer.cancel();
      timer.purge();
      timer = null;
    }
    activo = false;
  }
}

