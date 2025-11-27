package tech.hellsoft.trading.exception;

import java.util.Map;
import tech.hellsoft.trading.enums.Product;

public class IngredientesInsuficientesException extends ProduccionException {

    private final Map<Product, Integer> faltantes;

    public IngredientesInsuficientesException(
            String message,
            Map<Product, Integer> faltantes
    ) {
        super(message);
        this.faltantes = faltantes;
    }

    public Map<Product, Integer> getFaltantes() {
        return faltantes;
    }
}
