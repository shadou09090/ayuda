package tech.hellsoft.config.dto.local;

import tech.hellsoft.trading.dto.server.Recipe;
import tech.hellsoft.trading.enums.Product;
import tech.hellsoft.trading.enums.RecipeType;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Representación serializable de una receta para snapshots locales.
 */
public final class RecetaLocal implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final RecipeType type;
  private final Map<Product, Integer> ingredientes;
  private final Double bonus;

  private RecetaLocal(RecipeType type, Map<Product, Integer> ingredientes, Double bonus) {
    this.type = type;
    this.ingredientes = ingredientes == null || ingredientes.isEmpty() ? Map.of()
        : new HashMap<>(ingredientes);
    this.bonus = bonus;
  }

  public static RecetaLocal fromRecipe(Recipe receta) {
    if (receta == null) {
      return null;
    }
    RecipeType tipo = receta.getType();
    Map<Product, Integer> ingredientes = receta.getIngredients();
    Double bonus = receta.getPremiumBonus();
    return new RecetaLocal(tipo, ingredientes, bonus);
  }

  public Recipe toRecipe() {
    Recipe.RecipeBuilder builder = Recipe.builder();
    builder.type(type);
    if (!ingredientes.isEmpty()) {
      builder.ingredients(new HashMap<>(ingredientes));
    }
    builder.premiumBonus(bonus);
    return builder.build();
  }

  public RecipeType type() {
    return type;
  }

  public Map<Product, Integer> ingredientes() {
    return ingredientes.isEmpty() ? Map.of() : new HashMap<>(ingredientes);
  }

  public Double bonus() {
    return bonus;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, ingredientes, bonus);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RecetaLocal other)) {
      return false;
    }
    return Objects.equals(type, other.type) && Objects.equals(ingredientes, other.ingredientes)
        && Objects.equals(bonus, other.bonus);
  }
}

