package tech.hellsoft.trading.dto.client;

import com.google.gson.annotations.SerializedName;
import tech.hellsoft.trading.enums.MessageType;
import tech.hellsoft.trading.enums.OrderMode;
import tech.hellsoft.trading.enums.OrderSide;
import tech.hellsoft.trading.enums.Product;

import java.util.Objects;

/**
 * Copia local del DTO de órdenes del SDK. Se replica para garantizar que el campo clOrdID se serialice con el nombre
 * camelCase que exige el servidor (clOrdID) según la guía oficial de la Bolsa Interestelar.
 */
public final class OrderMessage {

  private MessageType type;

  @SerializedName("clOrdID")
  private String clOrdID;

  private OrderSide side;

  private OrderMode mode;

  private Product product;

  private Integer qty;

  private Double limitPrice;

  private String expiresAt;

  private String message;

  private String debugMode;

  public OrderMessage() {
  }

  public OrderMessage(MessageType type, String clOrdID, OrderSide side, OrderMode mode, Product product, Integer qty,
      Double limitPrice, String expiresAt, String message, String debugMode) {
    this.type = type;
    this.clOrdID = clOrdID;
    this.side = side;
    this.mode = mode;
    this.product = product;
    this.qty = qty;
    this.limitPrice = limitPrice;
    this.expiresAt = expiresAt;
    this.message = message;
    this.debugMode = debugMode;
  }

  public static OrderMessageBuilder builder() {
    return new OrderMessageBuilder();
  }

  public MessageType getType() {
    return type;
  }

  public void setType(MessageType type) {
    this.type = type;
  }

  public String getClOrdID() {
    return clOrdID;
  }

  public void setClOrdID(String clOrdID) {
    this.clOrdID = clOrdID;
  }

  public OrderSide getSide() {
    return side;
  }

  public void setSide(OrderSide side) {
    this.side = side;
  }

  public OrderMode getMode() {
    return mode;
  }

  public void setMode(OrderMode mode) {
    this.mode = mode;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public Integer getQty() {
    return qty;
  }

  public void setQty(Integer qty) {
    this.qty = qty;
  }

  public Double getLimitPrice() {
    return limitPrice;
  }

  public void setLimitPrice(Double limitPrice) {
    this.limitPrice = limitPrice;
  }

  public String getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(String expiresAt) {
    this.expiresAt = expiresAt;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getDebugMode() {
    return debugMode;
  }

  public void setDebugMode(String debugMode) {
    this.debugMode = debugMode;
  }

  @Override
  public String toString() {
    return "OrderMessage(type=" + type + ", clOrdID=" + clOrdID + ", side=" + side + ", mode=" + mode + ", product="
        + product + ", qty=" + qty + ", limitPrice=" + limitPrice + ", expiresAt=" + expiresAt + ", message=" + message
        + ", debugMode=" + debugMode + ")";
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OrderMessage that)) {
      return false;
    }
    return type == that.type && Objects.equals(clOrdID, that.clOrdID) && side == that.side && mode == that.mode
        && product == that.product && Objects.equals(qty, that.qty) && Objects.equals(limitPrice, that.limitPrice)
        && Objects.equals(expiresAt, that.expiresAt) && Objects.equals(message, that.message)
        && Objects.equals(debugMode, that.debugMode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, clOrdID, side, mode, product, qty, limitPrice, expiresAt, message, debugMode);
  }

  public static final class OrderMessageBuilder {

    private MessageType type;
    private String clOrdID;
    private OrderSide side;
    private OrderMode mode;
    private Product product;
    private Integer qty;
    private Double limitPrice;
    private String expiresAt;
    private String message;
    private String debugMode;

    private OrderMessageBuilder() {
    }

    public OrderMessageBuilder type(MessageType type) {
      this.type = type;
      return this;
    }

    public OrderMessageBuilder clOrdID(String clOrdID) {
      this.clOrdID = clOrdID;
      return this;
    }

    public OrderMessageBuilder side(OrderSide side) {
      this.side = side;
      return this;
    }

    public OrderMessageBuilder mode(OrderMode mode) {
      this.mode = mode;
      return this;
    }

    public OrderMessageBuilder product(Product product) {
      this.product = product;
      return this;
    }

    public OrderMessageBuilder qty(Integer qty) {
      this.qty = qty;
      return this;
    }

    public OrderMessageBuilder limitPrice(Double limitPrice) {
      this.limitPrice = limitPrice;
      return this;
    }

    public OrderMessageBuilder expiresAt(String expiresAt) {
      this.expiresAt = expiresAt;
      return this;
    }

    public OrderMessageBuilder message(String message) {
      this.message = message;
      return this;
    }

    public OrderMessageBuilder debugMode(String debugMode) {
      this.debugMode = debugMode;
      return this;
    }

    public OrderMessage build() {
      return new OrderMessage(type, clOrdID, side, mode, product, qty, limitPrice, expiresAt, message, debugMode);
    }

    @Override
    public String toString() {
      return "OrderMessage.OrderMessageBuilder(type=" + type + ", clOrdID=" + clOrdID + ", side=" + side + ", mode="
          + mode + ", product=" + product + ", qty=" + qty + ", limitPrice=" + limitPrice + ", expiresAt=" + expiresAt
          + ", message=" + message + ", debugMode=" + debugMode + ")";
    }
  }
}

