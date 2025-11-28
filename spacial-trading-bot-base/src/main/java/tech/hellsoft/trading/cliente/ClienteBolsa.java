public class ClienteBolsa implements EventListener {
    private ConectorBolsa conector;
    private EstadoCliente estado;
    public ClienteBolsa(ConectorBolsa conector) {
        this.conector = conector;
        this.estado = new EstadoCliente();
    }
    // ========== CALLBACKS DEL SDK ==========
    @Override
    public void onLoginOk(LoginOk msg) {
        // Inicializar estado con datos del servidor
        estado.setSaldo(msg.getSaldoInicial());
        estado.setSaldoInicial(msg.getSaldoInicial());
        estado.setRecetas(msg.getRecetas());
        estado.setRol(msg.getRol());
        estado.setProductosAutorizados(msg.getProductosAutorizados());
        System.out.println("✅ Conectado como " + msg.getEquipo());
    }
    @Override
    public void onFill(Fill fill) {
        if (fill.getSide().equals("BUY")) {
            // Restar dinero, sumar inventario
        } else {
            // Sumar dinero, restar inventario
        }
        System.out.println("P&L: " + estado.calcularPL() + "%");
    }
    @Override
    public void onTicker(Ticker ticker) {
        estado.getPreciosActuales().put(ticker.getProducto(), ticker.getMid());
    }
    @Override
    public void onOffer(Offer offer) {
        // Decidir si aceptar basado en precio y disponibilidad
    }
    @Override
    public void onError(ErrorMessage error) {
        switch (error.getCodigo()) {
            case "INVALID_TOKEN":
                System.exit(1);
                break;
            // ... más casos
        }
    }
    @Override
    public void onConexionPerdida(Exception e) {
        System.out.println("⚠ Conexión perdida");
    }
    // ========== MÉTODOS PÚBLICOS ==========
    public void comprar(String producto, int cantidad, String mensaje)
            throws SaldoInsuficienteException {
        // Validar saldo → lanzar excepción si falla
        // Crear orden → enviar
    }
    public void vender(String producto, int cantidad, String mensaje)
            throws InventarioInsuficienteException {
        // Validar inventario → lanzar excepción si falla
        // Crear orden → enviar
    }
    public void producir(String producto, boolean premium)
            throws ProductoNoAutorizadoException, RecetaNoEncontradaException,
            IngredientesInsuficientesException {
        // Validaciones → calcular → actualizar → notificar
    }
    public EstadoCliente getEstado() {
        return estado;
    }
}