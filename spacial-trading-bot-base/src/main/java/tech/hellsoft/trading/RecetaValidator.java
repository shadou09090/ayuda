package tech.hellsoft.trading;

import tech.hellsoft.trading.dto.server.Recipe;
import tech.hellsoft.trading.enums.Product;

import java.util.Map;

/**
 * Utilidades para verificar que el inventario tenga suficientes ingredientes.
 */
public final class RecetaValidator {

  private RecetaValidator() {
  }

  public static boolean puedeProducir(Recipe receta, Map<Product, Integer> inventario) {
    if (receta == null) {
      return false;
    }
    Map<Product, Integer> ingredientes = receta.getIngredients();
    if (ingredientes == null || ingredientes.isEmpty()) {
      return true;
    }
    return ingredientes.entrySet().stream()
        .allMatch(entry -> inventario.getOrDefault(entry.getKey(), 0) >= entry.getValue());
  }
}
