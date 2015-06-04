/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.report;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.finance.Trade;

/**
 * Represents a portfolio of trades.
 */
@BeanDefinition(builderScope = "private")
public final class TradePortfolio implements ImmutableBean {

  // TODO - temporary representation of a portfolio

  /** The trades. */
  @PropertyDefinition(validate = "notNull")
  private final List<Trade> trades;

  /**
   * Constructs a portfolio from a list of trades.
   * 
   * @param trades  the list of trades
   * @return the portfolio
   */
  public static TradePortfolio of(List<Trade> trades) {
    return new TradePortfolio(trades);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code TradePortfolio}.
   * @return the meta-bean, not null
   */
  public static TradePortfolio.Meta meta() {
    return TradePortfolio.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(TradePortfolio.Meta.INSTANCE);
  }

  private TradePortfolio(
      List<Trade> trades) {
    JodaBeanUtils.notNull(trades, "trades");
    this.trades = ImmutableList.copyOf(trades);
  }

  @Override
  public TradePortfolio.Meta metaBean() {
    return TradePortfolio.Meta.INSTANCE;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the trades.
   * @return the value of the property, not null
   */
  public List<Trade> getTrades() {
    return trades;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      TradePortfolio other = (TradePortfolio) obj;
      return JodaBeanUtils.equal(getTrades(), other.getTrades());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getTrades());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("TradePortfolio{");
    buf.append("trades").append('=').append(JodaBeanUtils.toString(getTrades()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code TradePortfolio}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code trades} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<List<Trade>> trades = DirectMetaProperty.ofImmutable(
        this, "trades", TradePortfolio.class, (Class) List.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "trades");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -865715313:  // trades
          return trades;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends TradePortfolio> builder() {
      return new TradePortfolio.Builder();
    }

    @Override
    public Class<? extends TradePortfolio> beanType() {
      return TradePortfolio.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code trades} property.
     * @return the meta-property, not null
     */
    public MetaProperty<List<Trade>> trades() {
      return trades;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -865715313:  // trades
          return ((TradePortfolio) bean).getTrades();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code TradePortfolio}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<TradePortfolio> {

    private List<Trade> trades = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -865715313:  // trades
          return trades;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -865715313:  // trades
          this.trades = (List<Trade>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public TradePortfolio build() {
      return new TradePortfolio(
          trades);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("TradePortfolio.Builder{");
      buf.append("trades").append('=').append(JodaBeanUtils.toString(trades));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
