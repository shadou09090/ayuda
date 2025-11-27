package tech.hellsoft.config;

import tech.hellsoft.trading.dto.server.OfferMessage;
import tech.hellsoft.trading.exception.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CLI simple para interactuar con el bot durante el torneo.
 */
public final class ConsolaInteractiva {

  private final ClienteBolsa cliente;
  private final EstadoCliente estado;
  private final AutoProduccionManager autoManager;
  private final Scanner scanner;

  public ConsolaInteractiva(ClienteBolsa clienteBolsa, EstadoCliente estadoCliente) {
    this.cliente = clienteBolsa;
    this.estado = estadoCliente;
    this.autoManager = new AutoProduccionManager(clienteBolsa, estadoCliente);
    this.scanner = new Scanner(System.in);
  }

  public void iniciar() {
    imprimirBanner();
    while (true) {
      System.out.print("\n> ");
      if (!scanner.hasNextLine()) {
        return;
      }
      String input = scanner.nextLine().trim();
      if (input.isEmpty()) {
        continue;
      }
      if ("exit".equalsIgnoreCase(input) || "salir".equalsIgnoreCase(input)) {
        System.out.println("👋 Cerrando Trading Bot...");
        return;
      }
      procesarComando(input);
    }
  }

  private void procesarComando(String input) {
    String[] partes = input.split("\\s+");
    String comando = partes[0].toLowerCase(Locale.ROOT);
    try {
      switch (comando) {
      case "help": {
        imprimirAyuda();
        break;
      }
      case "status": {
        imprimirEstado();
        break;
      }
      case "inventario": {
        imprimirInventario();
        break;
      }
      case "precios": {
        imprimirPrecios();
        break;
      }
      case "comprar": {
        ejecutarCompra(partes);
        break;
      }
      case "vender": {
        ejecutarVenta(partes);
        break;
      }
      case "producir": {
        ejecutarProduccion(partes);
        break;
      }
      case "ofertas": {
        imprimirOfertas();
        break;
      }
      case "aceptar": {
        aceptarOferta(partes, true);
        break;
      }
      case "rechazar": {
        aceptarOferta(partes, false);
        break;
      }
      case "snapshot": {
        manejarSnapshot(partes);
        break;
      }
      case "resync": {
        cliente.resincronizar();
        break;
      }
      case "auto": {
        manejarAuto(partes);
        break;
      }
      default: {
        System.out.println("❌ Comando desconocido. Usa 'help' para ver opciones.");
        break;
      }
      }
    } catch (Exception e) {
      System.out.println("⚠️ " + e.getMessage());
    }
  }

  private void imprimirBanner() {
    System.out.println("╔═══════════════════════════════════════════════╗");
    System.out.println("║   🥑 Bolsa Interestelar - Consola CLI       ║");
    System.out.println("╚═══════════════════════════════════════════════╝");
    imprimirAyuda();
  }

  private void imprimirAyuda() {
    System.out.println("\nComandos disponibles:");
    System.out.println(" status                      → Saldo, inventario y P&L");
    System.out.println(" inventario                  → Lista inventario actual");
    System.out.println(" precios                     → Últimos precios conocidos");
    System.out.println(" comprar <prod> <qty> [msg]  → Envía orden de compra");
    System.out.println(" vender <prod> <qty> [msg]   → Envía orden de venta");
    System.out.println(" producir <prod> <tipo>      → tipo: basico|premium");
    System.out.println(" ofertas                     → Ofertas pendientes");
    System.out.println(" aceptar <offerId>           → Acepta oferta existente");
    System.out.println(" rechazar <offerId> [motivo] → Rechaza oferta");
    System.out.println(" snapshot save <ruta>        → Guarda estado");
    System.out.println(" snapshot load <ruta>        → Carga estado");
    System.out.println(" resync                      → Solicita resync al servidor");
    System.out.println(" auto start <p> <modo> [s]   → Activa auto-producción");
    System.out.println(" auto stop                   → Detiene auto-producción");
    System.out.println(" auto status                 → Estado del auto manager");
    System.out.println(" exit                        → Terminar aplicación\n");
  }

  private void imprimirEstado() {
    double saldo = estado.saldo();
    double inventario = estado.calcularValorInventario();
    double patrimonio = saldo + inventario;
    System.out.printf("💰 Saldo disponible: %.2f%n", saldo);
    System.out.printf("📦 Valor inventario: %.2f%n", inventario);
    System.out.printf("💎 Patrimonio neto: %.2f%n", patrimonio);
    System.out.printf("📈 P&L: %.2f%%%n", estado.calcularPL());
  }

  private void imprimirInventario() {
    if (estado.inventario().isEmpty()) {
      System.out.println("Inventario vacío");
      return;
    }
    estado.inventario().forEach((producto, cantidad) -> System.out.printf("- %s: %d%n", producto.getValue(), cantidad));
  }

  private void imprimirPrecios() {
    if (estado.precios().isEmpty()) {
      System.out.println("Sin tickers recibidos aún.");
      return;
    }
    estado.precios().forEach((producto, precio) -> System.out.printf("- %s: %.2f%n", producto.getValue(), precio));
  }

  private void ejecutarCompra(String[] partes) throws SaldoInsuficienteException, ProductoNoAutorizadoException {
    if (partes.length < 3) {
      System.out.println("Uso: comprar <producto> <cantidad> [mensaje]");
      return;
    }
    String producto = partes[1];
    int cantidad = Integer.parseInt(partes[2]);
    String mensaje = partes.length > 3 ? unirMensaje(partes, 3) : "Orden de compra";
    cliente.comprar(producto, cantidad, mensaje);
  }

  private void ejecutarVenta(String[] partes) throws InventarioInsuficienteException, ProductoNoAutorizadoException {
    if (partes.length < 3) {
      System.out.println("Uso: vender <producto> <cantidad> [mensaje]");
      return;
    }
    String producto = partes[1];
    int cantidad = Integer.parseInt(partes[2]);
    String mensaje = partes.length > 3 ? unirMensaje(partes, 3) : "Orden de venta";
    cliente.vender(producto, cantidad, mensaje);
  }

  private void ejecutarProduccion(String[] partes)
      throws ProductoNoAutorizadoException, RecetaNoEncontradaException, IngredientesInsuficientesException {
    if (partes.length < 3) {
      System.out.println("Uso: producir <producto> <basico|premium>");
      return;
    }
    String producto = partes[1];
    boolean premium = "premium".equalsIgnoreCase(partes[2]);
    cliente.producir(producto, premium);
  }

  private void imprimirOfertas() {
    Map<String, OfferMessage> pendientes = cliente.ofertasPendientes();
    if (pendientes.isEmpty()) {
      System.out.println("No hay ofertas recibidas.");
      return;
    }
    pendientes.values().forEach(oferta -> {
      String producto = oferta.getProduct() != null ? oferta.getProduct().getValue() : "-";
      Integer solicitada = oferta.getQuantityRequested();
      Double precioMaximo = oferta.getMaxPrice();
      String comprador = oferta.getBuyer();
      int cantidad = solicitada == null ? 0 : solicitada;
      double precio = precioMaximo == null ? 0.0 : precioMaximo;
      String compradorFinal = comprador == null || comprador.isBlank() ? "-" : comprador;
      System.out.printf("• %s | %s x%d @ %.2f (buyer: %s)%n", oferta.getOfferId(), producto, cantidad, precio,
          compradorFinal);
    });
  }

  private void aceptarOferta(String[] partes, boolean aceptar) throws InventarioInsuficienteException {
    if (partes.length < 2) {
      System.out.println(aceptar ? "Uso: aceptar <offerId>" : "Uso: rechazar <offerId>");
      return;
    }
    cliente.aceptarOferta(partes[1], aceptar);
  }

  private void manejarSnapshot(String[] partes) throws ConfiguracionInvalidaException, SnapshotCorruptoException {
    if (partes.length < 3) {
      System.out.println("Uso: snapshot <save|load> <ruta>");
      return;
    }
    String accion = partes[1];
    File ruta = new File(partes[2]);
    if ("save".equalsIgnoreCase(accion)) {
      cliente.guardarSnapshot(ruta);
      return;
    }
    if ("load".equalsIgnoreCase(accion)) {
      cliente.cargarSnapshot(ruta);
      return;
    }
    System.out.println("Acción inválida. Usa snapshot save|load.");
  }

  private String unirMensaje(String[] partes, int inicio) {
    List<String> tokens = Arrays.stream(partes).skip(inicio).collect(Collectors.toList());
    return String.join(" ", tokens);
  }

  private void manejarAuto(String[] partes) {
    if (partes.length < 2) {
      System.out.println("Uso: auto start|stop|status ...");
      return;
    }
    String accion = partes[1].toLowerCase(Locale.ROOT);
    switch (accion) {
    case "start": {
      iniciarAuto(partes);
      break;
    }
    case "stop": {
      detenerAuto();
      break;
    }
    case "status": {
      imprimirEstadoAuto();
      break;
    }
    default: {
      System.out.println("Acción inválida. Usa auto start|stop|status.");
      break;
    }
    }
  }

  private void iniciarAuto(String[] partes) {
    if (partes.length < 4) {
      System.out.println("Uso: auto start <producto> <basico|premium> [intervaloSeg]");
      return;
    }
    String producto = partes[2];
    boolean premium = "premium".equalsIgnoreCase(partes[3]);
    long intervalo = 10L;
    if (partes.length > 4) {
      intervalo = Long.parseLong(partes[4]);
    }
    try {
      autoManager.iniciar(producto, premium, intervalo);
    } catch (Exception e) {
      System.out.println("⚠️ " + e.getMessage());
    }
  }

  private void detenerAuto() {
    autoManager.detener();
  }

  private void imprimirEstadoAuto() {
    if (!autoManager.activo()) {
      System.out.println("AutoProducción está detenida.");
      return;
    }
    String producto = autoManager.productoActual();
    long intervalo = autoManager.intervaloSegundos();
    String modo = autoManager.modoPremium() ? "premium" : "básica";
    System.out.printf("AutoProducción activa → %s (%s) cada %d segundos%n", producto, modo, intervalo);
  }
}
