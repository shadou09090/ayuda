package tech.hellsoft.config;

import tech.hellsoft.trading.config.Configuration;
import tech.hellsoft.trading.dto.client.AcceptOfferMessage;
import tech.hellsoft.trading.dto.client.OrderMessage;
import tech.hellsoft.trading.dto.client.ProductionUpdateMessage;
import tech.hellsoft.trading.dto.server.*;
import tech.hellsoft.trading.enums.MessageType;
import tech.hellsoft.trading.enums.OrderMode;
import tech.hellsoft.trading.enums.OrderSide;
import tech.hellsoft.trading.enums.Product;
import tech.hellsoft.trading.exception.*;
import tech.hellsoft.trading.repository.RecetaRepository;

import java.io.File;
import java.util.*;


public final class ClienteBolsa implements EventListener {

  private final ConectorBolsa conector;
  private final Configuration config;
  private static int consecutivoOrden = 1;
  private final EstadoCliente estado = new EstadoCliente();
  private final Map<String, OfferMessage> ofertasPendientes = new HashMap<>();
  private String especieActual;
  private String equipoActual;

  public ClienteBolsa(ConectorBolsa conectorBolsa, Configuration configuration) {
    this.conector = Objects.requireNonNull(conectorBolsa, "conector");
    this.config = Objects.requireNonNull(configuration, "config");
    this.especieActual = configuration.species();
    this.equipoActual = configuration.team();
  }

  public EstadoCliente estado() {
    return estado;
  }

  public Map<String, OfferMessage> ofertasPendientes() {
    return new HashMap<>(ofertasPendientes);
  }

  public void conectar() {
    conector.addListener(this);
    try {
      conector.conectar(config.host(), config.apiKey());
    } catch (tech.hellsoft.trading.exception.ConexionFallidaException e) {
      throw new IllegalStateException("No se pudo conectar con la bolsa: " + e.getMessage(), e);
    }
  }

  public void comprar(String nombreProducto, int cantidad, String mensaje)
      throws ProductoNoAutorizadoException, SaldoInsuficienteException {
    Product producto = resolverProducto(nombreProducto);
    validarCantidad(cantidad);
    validarAutorizado(producto);

    double costoEstimado = Math.max(estado.precioReferencia(producto), 1.0) * cantidad;
    if (estado.saldo() < costoEstimado) {
      throw new SaldoInsuficienteException(
          String.format("Saldo insuficiente. Requerido %.2f, disponible %.2f", costoEstimado, estado.saldo()));
    }

    OrderMessage orden = construirOrden(producto, OrderSide.BUY, cantidad, mensaje, "Orden CLI");
    conector.enviarOrden(orden);
    System.out.printf(" Orden BUY enviada: ", nombre(producto), cantidad, orden.getClOrdID());
  }

  public void vender(String nombreProducto, int cantidad, String mensaje)
      throws ProductoNoAutorizadoException, InventarioInsuficienteException {
    Product producto = resolverProducto(nombreProducto);
    validarCantidad(cantidad);
    validarAutorizado(producto);

    int disponible = estado.cantidadDisponible(producto);
    if (disponible < cantidad) {
      throw new InventarioInsuficienteException(String.format("Inventario insuficiente (%d disponibles)", disponible));
    }

    OrderMessage orden = construirOrden(producto, OrderSide.SELL, cantidad, mensaje, "Venta CLI");
    conector.enviarOrden(orden);
    System.out.printf(" Orden SELL enviada: ", nombre(producto), cantidad, orden.getClOrdID());
  }

  public void producir(String nombreProducto, boolean premium)
      throws ProductoNoAutorizadoException, RecetaNoEncontradaException, IngredientesInsuficientesException {
    Product producto = resolverProducto(nombreProducto);
    validarAutorizado(producto);

    Recipe receta = estado.recetaDe(producto);
    if (receta == null) {
      Recipe local = RecetaRepository.instancia().recetaPara(especieActual, equipoActual, producto);
      if (local != null) {
        estado.asignarReceta(producto, local);
        receta = local;
      }
    }
    if (receta == null) {
      throw new RecetaNoEncontradaException("No existe receta para " + nombre(producto));
    }

    if (premium && !RecetaValidator.puedeProducir(receta, estado.inventario())) {
      throw new IngredientesInsuficientesException("No hay ingredientes suficientes para " + nombre(producto));
    }

    if (estado.rol() == null) {
      throw new IllegalStateException("El rol aún no está disponible. Espera la confirmación de login.");
    }

    if (premium) {
      estado.consumirIngredientes(receta);
    }

    int unidades = CalculadoraProduccion.calcularUnidades(estado.rol());
    if (premium) {
      unidades = CalculadoraProduccion.aplicarBonusPremium(unidades, receta);
    }
    estado.sumarInventario(producto, unidades);

    ProductionUpdateMessage produccion = ProductionUpdateMessage.builder().type(MessageType.PRODUCTION_UPDATE)
        .product(producto).quantity(unidades).build();
    conector.enviarActualizacionProduccion(produccion);

    System.out.printf(" Producción registrada:", nombre(producto), unidades,
        premium ? "premium" : "básica");
  }

  public void aceptarOferta(String offerId, boolean aceptar) throws InventarioInsuficienteException {
    OfferMessage oferta = ofertasPendientes.remove(offerId);
    if (oferta == null) {
      System.out.println("No existe la oferta " + offerId);
      return;
    }
    Product producto = oferta.getProduct();
    Integer cantidadSolicitada = oferta.getQuantityRequested();
    int solicitada = cantidadSolicitada == null ? 0 : cantidadSolicitada;
    if (aceptar) {
      if (estado.cantidadDisponible(producto) < solicitada) {
        throw new InventarioInsuficienteException("No tienes inventario para aceptar la oferta");
      }
      estado.restarInventario(producto, solicitada);
    }

    Double precioMaximo = oferta.getMaxPrice();
    double precio = precioMaximo == null ? 0.0 : precioMaximo;

    AcceptOfferMessage respuesta = AcceptOfferMessage.builder().type(MessageType.ACCEPT_OFFER)
        .offerId(oferta.getOfferId()).accept(aceptar).quantityOffered(aceptar ? solicitada : 0).priceOffered(precio)
        .build();

    conector.enviarRespuestaOferta(respuesta);
    System.out.printf("%s oferta %s para %s%n", aceptar ? " Aceptada" : " Rechazada", oferta.getOfferId(),
        nombre(producto));
  }

  public void guardarSnapshot(File destino) throws ConfiguracionInvalidaException {
    File ruta = prepararRuta(destino);
    SnapshotManager.guardar(estado, ruta);
    System.out.println("💾 Snapshot guardado en " + ruta.getAbsolutePath());
  }

  public void cargarSnapshot(File origen) throws ConfiguracionInvalidaException, SnapshotCorruptoException {
    EstadoCliente restaurado = SnapshotManager.cargar(origen);
    estado.copiarDesde(restaurado);
    System.out.println("📂 Snapshot cargado desde " + origen.getAbsolutePath());
  }

  public void resincronizar() {
    conector.enviarLogin(config.apiKey());
    System.out.println("🔄 Solicitud de resync enviada.");
  }

  // ----------------------------- CALLBACKS DEL SDK -----------------------------

  @Override
  public void onLoginOk(LoginOKMessage loginOk) {
    if (loginOk == null) {
      return;
    }

    estado.establecerSaldoInicial(valor(loginOk.getCurrentBalance()));
    estado.reemplazarInventario(loginOk.getInventory());
    estado.asignarRecetas(loginOk.getRecipes());
    especieActual = loginOk.getSpecies() == null || loginOk.getSpecies().isBlank() ? config.species()
        : loginOk.getSpecies();
    equipoActual = loginOk.getTeam() == null || loginOk.getTeam().isBlank() ? config.team() : loginOk.getTeam();
    complementarRecetasLocales(especieActual, equipoActual);

    Set<Product> autorizados = new HashSet<>();
    if (loginOk.getAuthorizedProducts() != null) {
      autorizados.addAll(loginOk.getAuthorizedProducts());
    }
    if (autorizados.isEmpty()) {
      Map<Product, Recipe> recetasAsignadas = estado.recetas();
      if (recetasAsignadas != null) {
        autorizados.addAll(recetasAsignadas.keySet());
      }
    }
    estado.asignarProductosAutorizados(autorizados);
    estado.asignarRol(loginOk.getRole());

    System.out.printf("✅ Login exitoso | Equipo: %s | Especie: %s | Saldo: %.2f%n", loginOk.getTeam(),
        loginOk.getSpecies(), valor(loginOk.getCurrentBalance()));
  }

  private void complementarRecetasLocales(String species, String team) {
    Map<Product, Recipe> locales = RecetaRepository.instancia().recetasPara(species, team);

    if (locales.isEmpty()) {
      return;
    }
    boolean actualizado = estado.complementarRecetas(locales);
    if (actualizado) {
      System.out.printf("ℹ️ Recetas completadas localmente para %s%n", species);
    }
  }

  @Override
  public void onFill(FillMessage fill) {
    if (fill == null) {
      return;
    }
    OrderSide side = fill.getSide();
    Integer qty = fill.getFillQty();
    int cantidad = qty == null ? 0 : qty;
    Double precio = fill.getFillPrice();
    double total = (precio == null ? 0.0 : precio) * cantidad;
    Product producto = fill.getProduct();

    if (side == OrderSide.BUY) {
      estado.ajustarSaldo(-total);
      estado.sumarInventario(producto, cantidad);
      return;
    }
    if (side == OrderSide.SELL) {
      estado.ajustarSaldo(total);
      estado.restarInventario(producto, cantidad);
    }
  }

  @Override
  public void onTicker(TickerMessage ticker) {
    if (ticker == null) {
      return;
    }
    estado.registrarPrecio(ticker.getProduct(), valor(ticker.getMid()));
  }

  @Override
  public void onOffer(OfferMessage offer) {
    if (offer == null || offer.getOfferId() == null) {
      return;
    }
    ofertasPendientes.put(offer.getOfferId(), offer);
    Integer cantidadSolicitada = offer.getQuantityRequested();
    Double precioMaximo = offer.getMaxPrice();
    int cantidad = cantidadSolicitada == null ? 0 : cantidadSolicitada;
    double precio = precioMaximo == null ? 0.0 : precioMaximo;
    System.out.printf("📬 Oferta %s | %s x%d @ %.2f%n", offer.getOfferId(), nombre(offer.getProduct()), cantidad,
        precio);
  }

  @Override
  public void onError(ErrorMessage error) {
    if (error == null) {
      return;
    }
    System.out.printf("ERROR [%s]: %s%n", error.getCode(), error.getReason());
  }

  @Override
  public void onOrderAck(OrderAckMessage orderAck) {
    if (orderAck == null) {
      return;
    }
    System.out.printf(" OrderAck %s - %s%n", orderAck.getClOrdID(), orderAck.getStatus());
  }

  @Override
  public void onInventoryUpdate(InventoryUpdateMessage inventoryUpdate) {
    if (inventoryUpdate == null) {
      return;
    }
    estado.reemplazarInventario(inventoryUpdate.getInventory());
  }

  @Override
  public void onBalanceUpdate(BalanceUpdateMessage balanceUpdate) {
    if (balanceUpdate == null) {
      return;
    }
    estado.actualizarSaldo(valor(balanceUpdate.getBalance()));
  }

  @Override
  public void onEventDelta(EventDeltaMessage eventDelta) {
    if (eventDelta == null) {
      return;
    }
    System.out.println("EventDelta: " + eventDelta.getType());
  }

  @Override
  public void onBroadcast(BroadcastNotificationMessage broadcast) {
    if (broadcast == null) {
      return;
    }
    System.out.println(" Broadcast: " + broadcast.getMessage());
  }

  @Override
  public void onConnectionLost(Throwable throwable) {
    System.out.println(" Conexión perdida: " + (throwable != null ? throwable.getMessage() : "desconocido"));
    intentarReconectar();
  }

  @Override
  public void onGlobalPerformanceReport(GlobalPerformanceReportMessage report) {
    if (report == null) {
      return;
    }
    Integer totalTrades = report.getTotalTrades();
    double volumen = valor(report.getTotalVolume());
    System.out.printf("🌐 Performance global: trades=%d volumen=%.2f%n", totalTrades == null ? 0 : totalTrades,
        volumen);
  }



  private void validarAutorizado(Product producto) throws ProductoNoAutorizadoException {
    if (!estado.productoAutorizado(producto)) {
      throw new ProductoNoAutorizadoException("Producto no autorizado: " + nombre(producto));
    }
  }

  private void validarCantidad(int cantidad) {
    if (cantidad <= 0) {
      throw new IllegalArgumentException("La cantidad debe ser positiva.");
    }
  }

  Product resolverProducto(String nombre) throws ProductoNoAutorizadoException {
    if (nombre == null || nombre.isBlank()) {
      throw new ProductoNoAutorizadoException("Debes indicar un producto.");
    }
    String normalizado = nombre.trim().toUpperCase().replace('-', '_').replace(' ', '_');
    for (Product product : Product.values()) {
      if (product.name().equalsIgnoreCase(normalizado) || product.getValue().equalsIgnoreCase(nombre.trim())) {
        return product;
      }
    }
    throw new ProductoNoAutorizadoException("Producto desconocido: " + nombre);
  }

  private String nombre(Product producto) {
    return producto == null ? "N/D" : producto.getValue();
  }

  private OrderMessage construirOrden(Product producto, OrderSide lado, int cantidad, String mensaje, String fallback) {
    String id = generarClOrdId();
    String texto = mensaje == null || mensaje.isBlank() ? fallback : mensaje;
    return OrderMessage.builder().type(MessageType.ORDER).clOrdID(id).side(lado).mode(OrderMode.MARKET).product(producto)
        .qty(cantidad).message(texto).build();
  }

  private synchronized String generarClOrdId() {
    int consecutivo = consecutivoOrden;
    consecutivoOrden++;
    long timestamp = System.currentTimeMillis();
    return "ORD-" + timestamp + "-" + consecutivo;
  }

  private File prepararRuta(File destino) {
    String base = config.snapshotsDir() == null || config.snapshotsDir().isBlank()
        ? "snapshots"
        : config.snapshotsDir();
    if (destino == null) {
      File carpeta = new File(base);
      if (!carpeta.exists()) {
        carpeta.mkdirs();
      }
      return new File(carpeta, "snapshot-" + System.currentTimeMillis() + ".bin");
    }
    if (destino.isDirectory()) {
      return new File(destino, "snapshot-" + System.currentTimeMillis() + ".bin");
    }
    File padre = destino.getParentFile();
    if (padre != null && !padre.exists()) {
      padre.mkdirs();
    }
    return destino;
  }

  private void intentarReconectar() {
    try {
      Thread.sleep(3000L);
      conectar();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (RuntimeException e) {
      System.out.println("No se pudo reconectar automáticamente: " + e.getMessage());
    }
  }

  private double valor(Double numero) {
    if (numero == null) {
      return 0.0;
    }
    return numero;
  }
}
