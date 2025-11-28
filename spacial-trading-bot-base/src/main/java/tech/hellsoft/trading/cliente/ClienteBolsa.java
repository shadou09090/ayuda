package tech.hellsoft.trading.cliente;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import tech.hellsoft.trading.CalculadoraProduccion;
import tech.hellsoft.trading.ConectorBolsa;
import tech.hellsoft.trading.EventListener;
import tech.hellsoft.trading.SnapshotManager;
import tech.hellsoft.trading.config.Configuration;
import tech.hellsoft.trading.dto.client.AcceptOfferMessage;
import tech.hellsoft.trading.dto.client.OrderMessage;
import tech.hellsoft.trading.dto.client.ProductionUpdateMessage;
import tech.hellsoft.trading.dto.server.BalanceUpdateMessage;
import tech.hellsoft.trading.dto.server.BroadcastNotificationMessage;
import tech.hellsoft.trading.dto.server.ErrorMessage;
import tech.hellsoft.trading.dto.server.EventDeltaMessage;
import tech.hellsoft.trading.dto.server.FillMessage;
import tech.hellsoft.trading.dto.server.GlobalPerformanceReportMessage;
import tech.hellsoft.trading.dto.server.InventoryUpdateMessage;
import tech.hellsoft.trading.dto.server.LoginOKMessage;
import tech.hellsoft.trading.dto.server.OfferMessage;
import tech.hellsoft.trading.dto.server.OrderAckMessage;
import tech.hellsoft.trading.dto.server.Recipe;
import tech.hellsoft.trading.dto.server.TickerMessage;
import tech.hellsoft.trading.enums.MessageType;
import tech.hellsoft.trading.enums.OrderMode;
import tech.hellsoft.trading.enums.OrderSide;
import tech.hellsoft.trading.enums.Product;
import tech.hellsoft.trading.exception.ConexionFallidaException;
import tech.hellsoft.trading.exception.ConfiguracionInvalidaException;
import tech.hellsoft.trading.exception.IngredientesInsuficientesException;
import tech.hellsoft.trading.exception.InventarioInsuficienteException;
import tech.hellsoft.trading.exception.ProductoNoAutorizadoException;
import tech.hellsoft.trading.exception.RecetaNoEncontradaException;
import tech.hellsoft.trading.exception.SaldoInsuficienteException;
import tech.hellsoft.trading.exception.SnapshotCorruptoException;
import tech.hellsoft.trading.repository.RecetaRepository;

/**
 * ClienteBolsa - versión más sencilla y didáctica.
 *
 * Mantiene la misma API que tu versión anterior, pero escrita de forma explícita
 * y con menos uso de cosas avanzadas.
 */
public final class ClienteBolsa implements EventListener {

    private final ConectorBolsa conector;
    private final Configuration config;
    private final EstadoCliente estado = new EstadoCliente();
    private final Map<String, OfferMessage> ofertasPendientes = new HashMap<>();
    private String especieActual;
    private String equipoActual;
    private static int consecutivoOrden = 1;

    public ClienteBolsa(ConectorBolsa conectorBolsa, Configuration configuration) {
        this.conector = Objects.requireNonNull(conectorBolsa, "conector");
        this.config = Objects.requireNonNull(configuration, "config");
        this.especieActual = configuration.species();
        this.equipoActual = configuration.team();
    }

    // Exponer estado
    public EstadoCliente estado() {
        return estado;
    }

    // Devolver copia simple de ofertas pendientes
    public Map<String, OfferMessage> ofertasPendientes() {
        return new HashMap<>(ofertasPendientes);
    }

    // Conexión
    public void conectar() {
        conector.addListener(this);
        try {
            conector.conectar(config.host(), config.apiKey());
        } catch (ConexionFallidaException e) {
            // Convertimos a Runtime para no forzar callers, pero dejamos mensaje claro.
            throw new IllegalStateException("No se pudo conectar con la bolsa: " + e.getMessage(), e);
        }
    }

    // ---------------------- ACCIONES DEL USUARIO ----------------------

    // Comprar: valida producto, cantidad, autorización y saldo local antes de enviar orden
    public void comprar(String nombreProducto, int cantidad, String mensaje)
            throws ProductoNoAutorizadoException, SaldoInsuficienteException {

        Product producto = resolverProducto(nombreProducto);
        validarCantidad(cantidad);
        validarAutorizado(producto);

        double precioRef = estado.precioReferencia(producto);
        // asegurar precio mínimo 1.0 (como en tu versión previa)
        if (precioRef <= 0.0) {
            precioRef = 1.0;
        }
        double costoEstimado = precioRef * cantidad;

        if (estado.saldo() < costoEstimado) {
            // lanza excepción con los datos requeridos por el enunciado
            throw new SaldoInsuficienteException(estado.saldo(), costoEstimado);
        }

        OrderMessage orden = construirOrden(producto, OrderSide.BUY, cantidad, mensaje, "Orden CLI");
        conector.enviarOrden(orden);

        System.out.println("Orden BUY enviada -> producto: " + nombre(producto) + " cantidad: " + cantidad
                + " clOrdID: " + orden.getClOrdID());
    }

    // Vender: valida producto, cantidad, autorización e inventario local antes de enviar orden
    public void vender(String nombreProducto, int cantidad, String mensaje)
            throws ProductoNoAutorizadoException, InventarioInsuficienteException {

        Product producto = resolverProducto(nombreProducto);
        validarCantidad(cantidad);
        validarAutorizado(producto);

        int disponible = estado.cantidadDisponible(producto);
        if (disponible < cantidad) {
            // lanza excepción con los datos requeridos por el enunciado
            throw new InventarioInsuficienteException(producto, disponible, cantidad);
        }

        OrderMessage orden = construirOrden(producto, OrderSide.SELL, cantidad, mensaje, "Venta CLI");
        conector.enviarOrden(orden);

        System.out.println("Orden SELL enviada -> producto: " + nombre(producto) + " cantidad: " + cantidad
                + " clOrdID: " + orden.getClOrdID());
    }

    // Producir: busca receta, valida ingredientes si premium, consume ingredientes y registra producción
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

        // Si es premium, verificar ingredientes necesarios
        if (premium) {
            Map<Product, Integer> ingredientes = receta.getIngredients();
            Map<Product, Integer> faltantes = new HashMap<>();

            if (ingredientes != null) {
                for (Map.Entry<Product, Integer> entry : ingredientes.entrySet()) {
                    Product ing = entry.getKey();
                    Integer req = entry.getValue();
                    int disponible = estado.cantidadDisponible(ing);
                    if (req == null) req = 0;
                    if (disponible < req) {
                        faltantes.put(ing, req - disponible);
                    }
                }
            }

            if (!faltantes.isEmpty()) {
                // lanzamos la excepción con mapa de faltantes
                throw new IngredientesInsuficientesException(
                        "Ingredientes insuficientes para producir " + nombre(producto),
                        faltantes
                );
            }
        }

        // chequear que el rol esté disponible (login)
        if (estado.rol() == null) {
            throw new IllegalStateException("El rol aún no está disponible. Espera la confirmación de login.");
        }

        // consumir ingredientes (si premium) y calcular unidades producidas
        if (premium) {
            estado.consumirIngredientes(receta);
        }

        int unidades = CalculadoraProduccion.calcularUnidades(estado.rol());
        if (premium) {
            unidades = CalculadoraProduccion.aplicarBonusPremium(unidades, receta);
        }

        estado.sumarInventario(producto, unidades);

        ProductionUpdateMessage produccion = ProductionUpdateMessage.builder()
                .type(MessageType.PRODUCTION_UPDATE)
                .product(producto)
                .quantity(unidades)
                .build();

        conector.enviarActualizacionProduccion(produccion);

        System.out.println("Producción registrada: " + nombre(producto) + " x" + unidades
                + (premium ? " (premium)" : " (básica)"));
    }

    // Aceptar / rechazar oferta
    public void aceptarOferta(String offerId, boolean aceptar) throws InventarioInsuficienteException {
        OfferMessage oferta = ofertasPendientes.remove(offerId);
        if (oferta == null) {
            System.out.println("No existe la oferta " + offerId);
            return;
        }

        Product producto = oferta.getProduct();
        int solicitada = oferta.getQuantityRequested() == null ? 0 : oferta.getQuantityRequested();

        if (aceptar) {
            int disponible = estado.cantidadDisponible(producto);
            if (disponible < solicitada) {
                throw new InventarioInsuficienteException(producto, disponible, solicitada);
            }
            estado.restarInventario(producto, solicitada);
        }

        double precio = oferta.getMaxPrice() == null ? 0.0 : oferta.getMaxPrice();

        AcceptOfferMessage respuesta = AcceptOfferMessage.builder()
                .type(MessageType.ACCEPT_OFFER)
                .offerId(oferta.getOfferId())
                .accept(aceptar)
                .quantityOffered(aceptar ? solicitada : 0)
                .priceOffered(precio)
                .build();

        conector.enviarRespuestaOferta(respuesta);

        System.out.println((aceptar ? "Aceptada" : "Rechazada") + " oferta " + offerId + " para " + nombre(producto));
    }

    // Snapshots: guardar / cargar (usa SnapshotManager)
    public void guardarSnapshot(File destino) throws ConfiguracionInvalidaException {
        File ruta = prepararRuta(destino);
        SnapshotManager.guardar(estado, ruta);
        System.out.println("Snapshot guardado en " + ruta.getAbsolutePath());
    }

    public void cargarSnapshot(File origen) throws ConfiguracionInvalidaException, SnapshotCorruptoException {
        EstadoCliente restaurado = SnapshotManager.cargar(origen);
        estado.copiarDesde(restaurado);
        System.out.println("Snapshot cargado desde " + origen.getAbsolutePath());
    }

    // Solicitar resync (reenviar login)
    public void resincronizar() {
        conector.enviarLogin(config.apiKey());
        System.out.println("Solicitud de resync enviada.");
    }

    // ---------------------- CALLBACKS (EventListener) ----------------------

    @Override
    public void onLoginOk(LoginOKMessage loginOk) {
        if (loginOk == null) return;

        estado.establecerSaldoInicial(valor(loginOk.getCurrentBalance()));
        estado.reemplazarInventario(loginOk.getInventory());
        estado.asignarRecetas(loginOk.getRecipes());

        especieActual = (loginOk.getSpecies() == null || loginOk.getSpecies().isBlank())
                ? config.species()
                : loginOk.getSpecies();

        equipoActual = (loginOk.getTeam() == null || loginOk.getTeam().isBlank())
                ? config.team()
                : loginOk.getTeam();

        complementarRecetasLocales(especieActual, equipoActual);

        // productos autorizados
        Set<Product> autorizados = new HashSet<>();
        if (loginOk.getAuthorizedProducts() != null) {
            autorizados.addAll(loginOk.getAuthorizedProducts());
        }
        // si el servidor no envía autorizados, usamos recetas asignadas
        if (autorizados.isEmpty()) {
            Map<Product, Recipe> recetasAsignadas = estado.recetas();
            if (recetasAsignadas != null) {
                autorizados.addAll(recetasAsignadas.keySet());
            }
        }
        estado.asignarProductosAutorizados(autorizados);
        estado.asignarRol(loginOk.getRole());

        System.out.println("Login exitoso | Equipo: " + loginOk.getTeam() + " | Especie: " + loginOk.getSpecies()
                + " | Saldo: " + valor(loginOk.getCurrentBalance()));
    }

    private void complementarRecetasLocales(String species, String team) {
        Map<Product, Recipe> locales = RecetaRepository.instancia().recetasPara(species, team);
        if (locales == null || locales.isEmpty()) return;

        boolean actualizado = estado.complementarRecetas(locales);
        if (actualizado) {
            System.out.println("Recetas completadas localmente para " + species);
        }
    }

    @Override
    public void onFill(FillMessage fill) {
        if (fill == null) return;

        OrderSide side = fill.getSide();
        int cantidad = fill.getFillQty() == null ? 0 : fill.getFillQty();
        double precio = fill.getFillPrice() == null ? 0.0 : fill.getFillPrice();
        double total = precio * cantidad;
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
        if (ticker == null) return;
        estado.registrarPrecio(ticker.getProduct(), valor(ticker.getMid()));
    }

    @Override
    public void onOffer(OfferMessage offer) {
        if (offer == null || offer.getOfferId() == null) return;
        ofertasPendientes.put(offer.getOfferId(), offer);

        int cantidad = offer.getQuantityRequested() == null ? 0 : offer.getQuantityRequested();
        double precio = offer.getMaxPrice() == null ? 0.0 : offer.getMaxPrice();
        System.out.println("Oferta " + offer.getOfferId() + " | " + nombre(offer.getProduct())
                + " x" + cantidad + " @ " + precio);
    }

    @Override
    public void onError(ErrorMessage error) {
        if (error == null) return;
        System.out.println("ERROR [" + error.getCode() + "]: " + error.getReason());
    }

    @Override
    public void onOrderAck(OrderAckMessage orderAck) {
        if (orderAck == null) return;
        System.out.println("OrderAck " + orderAck.getClOrdID() + " - " + orderAck.getStatus());
    }

    @Override
    public void onInventoryUpdate(InventoryUpdateMessage inventoryUpdate) {
        if (inventoryUpdate == null) return;
        estado.reemplazarInventario(inventoryUpdate.getInventory());
    }

    @Override
    public void onBalanceUpdate(BalanceUpdateMessage balanceUpdate) {
        if (balanceUpdate == null) return;
        estado.actualizarSaldo(valor(balanceUpdate.getBalance()));
    }

    @Override
    public void onEventDelta(EventDeltaMessage eventDelta) {
        if (eventDelta == null) return;
        System.out.println("EventDelta: " + eventDelta.getType());
    }

    @Override
    public void onBroadcast(BroadcastNotificationMessage broadcast) {
        if (broadcast == null) return;
        System.out.println("Broadcast: " + broadcast.getMessage());
    }

    @Override
    public void onConnectionLost(Throwable throwable) {
        System.out.println("Conexión perdida: " + (throwable != null ? throwable.getMessage() : "desconocido"));
        intentarReconectar();
    }

    @Override
    public void onGlobalPerformanceReport(GlobalPerformanceReportMessage report) {
        if (report == null) return;
        Integer totalTrades = report.getTotalTrades();
        double volumen = valor(report.getTotalVolume());
        System.out.println("Performance global: trades=" + (totalTrades == null ? 0 : totalTrades)
                + " volumen=" + volumen);
    }

    // ---------------------- util privados ----------------------

    private void validarAutorizado(Product producto) throws ProductoNoAutorizadoException {
        if (producto == null || !estado.productoAutorizado(producto)) {
            // obtener lista de permitidos como texto desde estado
            Set<String> permitidos = estado.productosAutorizadosComoTexto();
            throw new ProductoNoAutorizadoException(nombre(producto), permitidos);
        }
    }

    private void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva.");
        }
    }

    public Product resolverProducto(String nombre) throws ProductoNoAutorizadoException {
        if (nombre == null || nombre.isBlank()) {
            throw new ProductoNoAutorizadoException("(vacío)", estado.productosAutorizadosComoTexto());
        }
        String normalizado = nombre.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        for (Product p : Product.values()) {
            if (p.name().equalsIgnoreCase(normalizado) || p.getValue().equalsIgnoreCase(nombre.trim())) {
                return p;
            }
        }
        throw new ProductoNoAutorizadoException(nombre, estado.productosAutorizadosComoTexto());
    }

    private String nombre(Product producto) {
        return producto == null ? "N/D" : producto.getValue();
    }

    private OrderMessage construirOrden(Product producto, OrderSide lado, int cantidad, String mensaje, String fallback) {
        String id = generarClOrdId();
        String texto = (mensaje == null || mensaje.isBlank()) ? fallback : mensaje;
        return OrderMessage.builder()
                .type(MessageType.ORDER)
                .clOrdID(id)
                .side(lado)
                .mode(OrderMode.MARKET)
                .product(producto)
                .qty(cantidad)
                .message(texto)
                .build();
    }

    private synchronized String generarClOrdId() {
        int seq = consecutivoOrden++;
        long ts = System.currentTimeMillis();
        return "ORD-" + ts + "-" + seq;
    }

    private File prepararRuta(File destino) {
        String base = (config.snapshotsDir() == null || config.snapshotsDir().isBlank()) ? "snapshots" : config.snapshotsDir();
        if (destino == null) {
            File carpeta = new File(base);
            if (!carpeta.exists()) carpeta.mkdirs();
            return new File(carpeta, "snapshot-" + System.currentTimeMillis() + ".bin");
        }
        if (destino.isDirectory()) {
            return new File(destino, "snapshot-" + System.currentTimeMillis() + ".bin");
        }
        File padre = destino.getParentFile();
        if (padre != null && !padre.exists()) padre.mkdirs();
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
        if (numero == null) return 0.0;
        return numero;
    }
}