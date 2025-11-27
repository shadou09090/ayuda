package tech.hellsoft.trading.exception;

public class SnapshotCorruptoException extends ConfiguracionException {

    private final String rutaArchivo;

    public SnapshotCorruptoException(String rutaArchivo, String razon) {
        super("Snapshot corrupto en '" + rutaArchivo + "': " + razon);
        this.rutaArchivo = rutaArchivo;
    }

    public SnapshotCorruptoException(String rutaArchivo, String razon, Throwable cause) {
        super("Snapshot corrupto en '" + rutaArchivo + "': " + razon, cause);
        this.rutaArchivo = rutaArchivo;
    }

    public String getRutaArchivo() {
        return rutaArchivo;
    }
}
