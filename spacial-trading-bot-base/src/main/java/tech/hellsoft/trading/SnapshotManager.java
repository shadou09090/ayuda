package tech.hellsoft.trading;

import tech.hellsoft.trading.cliente.EstadoCliente;
import tech.hellsoft.trading.exception.ConfiguracionInvalidaException;
import tech.hellsoft.trading.exception.SnapshotCorruptoException;

import java.io.*;

/**
 * Utilidades para persistir y restaurar el estado del bot mediante
 * serialización binaria.
 */
public final class SnapshotManager {

  private SnapshotManager() {}

  public static void guardar(EstadoCliente estado, File destino) throws ConfiguracionInvalidaException {
    if (estado == null) {
      throw new ConfiguracionInvalidaException("El estado no puede ser nulo");
    }
    if (destino == null) {
      throw new ConfiguracionInvalidaException("Debe indicar la ruta del snapshot");
    }
    File carpeta = destino.getParentFile();
    if (carpeta != null && !carpeta.exists()) {
      carpeta.mkdirs();
    }
    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(destino))) {
      out.writeObject(estado);
    } catch (IOException e) {
      throw new ConfiguracionInvalidaException("No se pudo guardar snapshot: " + e.getMessage());
    }
  }

  public static EstadoCliente cargar(File origen) throws ConfiguracionInvalidaException, SnapshotCorruptoException {
    if (origen == null) {
      throw new ConfiguracionInvalidaException("Debe indicar el archivo a cargar");
    }
    if (!origen.exists()) {
      throw new ConfiguracionInvalidaException("No existe snapshot en " + origen.getAbsolutePath());
    }
    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(origen))) {
      Object data = in.readObject();
      if (data instanceof EstadoCliente) {
        return (EstadoCliente) data;
      }
        throw new SnapshotCorruptoException(
                origen.getAbsolutePath(),
                "El archivo no contiene un EstadoCliente válido"
        );
    } catch (IOException | ClassNotFoundException e) {
        throw new SnapshotCorruptoException(
                origen.getAbsolutePath(),
                "El archivo no contiene un EstadoCliente válido"
        );
    }
  }
}
