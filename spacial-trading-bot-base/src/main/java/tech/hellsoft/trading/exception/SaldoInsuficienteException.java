package tech.hellsoft.trading.exception;

public class SaldoInsuficienteException extends TradingException {

    private final double saldoDisponible;
    private final double saldoRequerido;

    public SaldoInsuficienteException(double saldoDisponible, double saldoRequerido) {
        super("Saldo insuficiente: disponible=" + saldoDisponible +
                ", requerido=" + saldoRequerido);
        this.saldoDisponible = saldoDisponible;
        this.saldoRequerido = saldoRequerido;
    }

    public double getSaldoDisponible() { return saldoDisponible; }
    public double getSaldoRequerido() { return saldoRequerido; }
}