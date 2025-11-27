package tech.hellsoft.config;

import tech.hellsoft.trading.config.Configuration;
import tech.hellsoft.trading.exception.ConfiguracionInvalidaException;
import tech.hellsoft.trading.util.ConfigLoader;

public final class Main {

  private static final String DEFAULT_CONFIG = "src/main/resources/config.json";

  private Main() {
  }

  public static void main(String[] args) {
    try {
      Configuration config = cargarConfiguracion(args);
      imprimirBanner(config.team());
      ClienteBolsa cliente = inicializarCliente(config);
      ConsolaInteractiva consola = new ConsolaInteractiva(cliente, cliente.estado());
      consola.iniciar();
    } catch (Exception errorCritico) {
      System.err.println("❌ Error crítico: " + errorCritico.getMessage());
      errorCritico.printStackTrace();
      System.exit(1);
    }
  }

  private static Configuration cargarConfiguracion(String[] args) throws ConfiguracionInvalidaException {
    if (args != null && args.length > 0 && args[0] != null && !args[0].isBlank()) {
      return ConfigLoader.load(args[0]);
    }
    return ConfigLoader.load(DEFAULT_CONFIG);
  }

  private static ClienteBolsa inicializarCliente(Configuration config) {
    ConectorBolsa conector = new ConectorBolsa();
    ClienteBolsa cliente = new ClienteBolsa(conector, config);
    cliente.conectar();
    return cliente;
  }

  private static void imprimirBanner(String team) {
    System.out.println("╔══════════════════════════════════════════════════════════╗");
    System.out.println("║  🥑 Bolsa Interestelar de Aguacates Andorianos 🥑      ║");
    System.out.println("║  Trading Bot CLI - Java Básico                          ║");
    System.out.println("╚══════════════════════════════════════════════════════════╝");
    if (team == null || team.isBlank()) {
      System.out.println();
      return;
    }
    System.out.println("Equipo: " + team);
    System.out.println();
  }
}

