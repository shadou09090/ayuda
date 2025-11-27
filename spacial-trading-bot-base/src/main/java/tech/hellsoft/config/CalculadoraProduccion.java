package tech.hellsoft.config;

import tech.hellsoft.trading.dto.server.Recipe;
import tech.hellsoft.trading.dto.server.TeamRole;


public final class CalculadoraProduccion {

  private CalculadoraProduccion() {
  }

  public static int calcularUnidades(TeamRole rol) {
    if (rol == null) {
      return 0;
    }
    return calcularRecursivo(0, rol);
  }

  private static int calcularRecursivo(int nivel, TeamRole rol) {
    if (rol.getMaxDepth() == null || nivel > rol.getMaxDepth()) {
      return 0;
    }
    double baseEnergy = valor(rol.getBaseEnergy());
    double levelEnergy = valor(rol.getLevelEnergy());
    double energia = baseEnergy + levelEnergy * nivel;

    double decay = valor(rol.getDecay(), 1.0);
    double branches = rol.getBranches() == null ? 1.0 : rol.getBranches();
    double factor = Math.pow(decay, nivel) * Math.pow(branches, nivel);

    int contribucion = (int) Math.round(energia * factor);
    return contribucion + calcularRecursivo(nivel + 1, rol);
  }

  public static int aplicarBonusPremium(int unidadesBase, Recipe receta) {
    double bonus = receta != null && receta.getPremiumBonus() != null ? receta.getPremiumBonus() : 1.0;
    return (int) Math.round(unidadesBase * bonus);
  }

  private static double valor(Double numero) {
    return valor(numero, 0.0);
  }

  private static double valor(Double numero, double defecto) {
    if (numero == null) {
      return defecto;
    }
    return numero;
  }
}
