package tech.hellsoft.config.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import tech.hellsoft.trading.dto.server.Recipe;
import tech.hellsoft.trading.enums.Product;
import tech.hellsoft.trading.enums.RecipeType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Carga las recetas locales descritas en la guía oficial. Sirve como respaldo
 * cuando el servidor no entrega las recetas al iniciar sesión.
 */
public final class RecetaRepository {

  private static final String DEFAULT_PATH = "src/main/resources/recetas/especies.json";
  private static final Gson GSON = new Gson();
  private static final RecetaRepository INSTANCE = cargarDefault();

  private final Map<String, Map<Product, Recipe>> catalogoPorEspecie;

  private RecetaRepository(Map<String, Map<Product, Recipe>> catalogo) {
    this.catalogoPorEspecie = catalogo;
  }

  public static RecetaRepository instancia() {
    return INSTANCE;
  }

  public Map<Product, Recipe> recetasPara(String especie, String equipo) {
    String clave = normalizarEntrada(especie, equipo);
    if (clave.isBlank()) {
      return Collections.emptyMap();
    }
    Map<Product, Recipe> recetas = catalogoPorEspecie.get(clave);
    if (recetas == null) {
      return Collections.emptyMap();
    }
    return new HashMap<>(recetas);
  }

  public Map<Product, Recipe> recetasParaEspecie(String especie) {
    return recetasPara(especie, null);
  }

  public Recipe recetaPara(String especie, String equipo, Product producto) {
    if (producto == null) {
      return null;
    }
    Map<Product, Recipe> recetas = recetasPara(especie, equipo);
    if (recetas.isEmpty()) {
      return null;
    }
    return recetas.get(producto);
  }

  public Recipe recetaPara(String especie, Product producto) {
    if (especie == null || producto == null) {
      return null;
    }
    Map<Product, Recipe> recetas = recetasParaEspecie(especie);
    if (recetas.isEmpty()) {
      return null;
    }
    return recetas.get(producto);
  }

  private static RecetaRepository cargarDefault() {
    try {
      return cargar(Paths.get(DEFAULT_PATH));
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudo cargar recetas locales: " + e.getMessage(), e);
    }
  }

  private static RecetaRepository cargar(Path ruta) throws IOException {
    if (!Files.exists(ruta)) {
      return new RecetaRepository(Collections.emptyMap());
    }
    String json = Files.readString(ruta);
    Type tipo = new TypeToken<Map<String, Map<String, RecetaJson>>>() {
    }.getType();
    Map<String, Map<String, RecetaJson>> bruto = GSON.fromJson(json, tipo);
    if (bruto == null) {
      return new RecetaRepository(Collections.emptyMap());
    }
    Map<String, Map<Product, Recipe>> catalogo = new HashMap<>();
    for (Map.Entry<String, Map<String, RecetaJson>> entradaEspecie : bruto.entrySet()) {
      String especie = normalizarClave(entradaEspecie.getKey());
      Map<Product, Recipe> recetas = convertirRecetas(entradaEspecie.getValue());
      catalogo.put(especie, recetas);
    }
    return new RecetaRepository(catalogo);
  }

  private static Map<Product, Recipe> convertirRecetas(Map<String, RecetaJson> data) {
    if (data == null || data.isEmpty()) {
      return Collections.emptyMap();
    }
    return data.values().stream().map(RecetaRepository::convertir).filter(Objects::nonNull)
        .collect(Collectors.toMap(RecetaDetalle::producto, RecetaDetalle::receta, (a, b) -> b,
            () -> new EnumMap<>(Product.class)));
  }

  private static RecetaDetalle convertir(RecetaJson json) {
    if (json == null || json.producto == null) {
      return null;
    }
    Product producto = parsearProducto(json.producto);
    if (producto == null) {
      return null;
    }
    Recipe.RecipeBuilder builder = Recipe.builder();
    Map<Product, Integer> ingredientes = convertirIngredientes(json.ingredientes);
    if (ingredientes == null || ingredientes.isEmpty()) {
      builder.type(RecipeType.BASIC);
    } else {
      builder.type(RecipeType.PREMIUM);
      builder.ingredients(ingredientes);
    }
    builder.premiumBonus(json.bonusPremium);
    return new RecetaDetalle(producto, builder.build());
  }

  private static Map<Product, Integer> convertirIngredientes(Map<String, Integer> ingredientes) {
    if (ingredientes == null || ingredientes.isEmpty()) {
      return Collections.emptyMap();
    }
    EnumMap<Product, Integer> resultado = new EnumMap<>(Product.class);
    for (Map.Entry<String, Integer> entry : ingredientes.entrySet()) {
      Product producto = parsearProducto(entry.getKey());
      Integer cantidad = entry.getValue();
      if (producto != null && cantidad != null) {
        resultado.put(producto, cantidad);
      }
    }
    return resultado;
  }

  private static Product parsearProducto(String nombre) {
    if (nombre == null || nombre.isBlank()) {
      return null;
    }
    try {
      return Product.valueOf(normalizarProducto(nombre));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static String normalizarProducto(String nombre) {
    return nombre.trim().toUpperCase().replace('-', '_').replace(' ', '_');
  }

  private static String normalizarClave(String especie) {
    return especie == null ? "" : especie.trim().toUpperCase().replace("-", "").replace("_", "").replace(" ", "");
  }

  private static String normalizarEntrada(String especie, String equipo) {
    String deducida = deducirEspecie(especie, equipo);
    if (deducida == null || deducida.isBlank()) {
      return "";
    }
    return normalizarClave(deducida);
  }

  private static String deducirEspecie(String especie, String equipo) {
    if (equipo != null && !equipo.isBlank()) {
      String equipoNormalizado = equipo.toUpperCase().replace(" ", "").replace("-", "").replace("DE", "")
          .replace("DEL", "").replace("LOS", "").replace("LAS", "");
      if (equipoNormalizado.contains("MINERO") && equipoNormalizado.contains("SEBO")) {
        return "MINEROSDELSEBO";
      }
      if (equipoNormalizado.contains("MINERO") && equipoNormalizado.contains("GUACATRON")) {
        return "MINEROSDELSEBO";
      }
    }
    if (especie != null && !especie.isBlank()) {
      String especieNormalizada = especie.toUpperCase().replace(" ", "").replace("-", "");
      if ("PREMIUM".equals(especieNormalizada) || especieNormalizada.contains("MINERO")) {
        return "MINEROSDELSEBO";
      }
      return especieNormalizada;
    }
    return "";
  }

  private static final class RecetaJson {
    String producto;
    Map<String, Integer> ingredientes;
    Double bonusPremium;
  }

  private static final class RecetaDetalle {

    private final Product producto;
    private final Recipe receta;

    RecetaDetalle(Product producto, Recipe receta) {
      this.producto = producto;
      this.receta = receta;
    }

    public Product producto() {
      return producto;
    }

    public Recipe receta() {
      return receta;
    }
  }
}

