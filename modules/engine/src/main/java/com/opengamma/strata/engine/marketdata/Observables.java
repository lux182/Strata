/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.joda.beans.Bean;
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

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.marketdata.id.MarketDataId;

// TODO Hand written builder
/**
 * A set of observable market data keyed by its {@link MarketDataId}.
 * <p>
 * Observable data is data that can be directly queried from a market data provider, for example Bloomberg or Reuters.
 */
@BeanDefinition
public final class Observables implements ImmutableBean {

  /** Single observable values, keyed by ID. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<? extends MarketDataId<?>, Object> singleValues;

  /** Time series of observable values, keyed by ID. */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableMap<? extends MarketDataId<?>, LocalDateDoubleTimeSeries> timeSeries;

  /**
   * Returns a single value of data with the given ID.
   *
   * @param id  the ID of the required market data
   * @param <T>  the type of the market data
   * @param <I>  the type of the market data ID
   * @return the market data for the specified ID
   * @throws RuntimeException if there is no market data value available for the ID
   */
  @SuppressWarnings("unchecked")
  public <T, I extends MarketDataId<T>> T get(I id) {
    ArgChecker.notNull(id, "id");
    Object value = singleValues.get(id);

    if (value == null) {
      throw new RuntimeException("No observable value found for " + id);
    }
    if (!id.getMarketDataType().isInstance(value)) {
      throw new RuntimeException(Messages.format("Value is not of the expected type. ID: {}, value: {}", id, value));
    }
    return ((T) value);
  }

  /**
   * Returns a time series of data with the given ID.
   *
   * @param id  the ID of the required market data
   * @param <I>  the type of the market data ID
   * @return the market data for the specified ID
   * @throws RuntimeException if there is no time series available for the ID
   */
  @SuppressWarnings("unchecked")
  public <I extends MarketDataId<Double>> LocalDateDoubleTimeSeries getTimeSeries(I id) {
    ArgChecker.notNull(id, "id");
    LocalDateDoubleTimeSeries timeSeries = this.timeSeries.get(id);

    if (timeSeries == null) {
      throw new RuntimeException("No time series found for " + id);
    }
    return timeSeries;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code Observables}.
   * @return the meta-bean, not null
   */
  public static Observables.Meta meta() {
    return Observables.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(Observables.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static Observables.Builder builder() {
    return new Observables.Builder();
  }

  private Observables(
      Map<? extends MarketDataId<?>, Object> singleValues,
      Map<? extends MarketDataId<?>, LocalDateDoubleTimeSeries> timeSeries) {
    JodaBeanUtils.notNull(singleValues, "singleValues");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    this.singleValues = ImmutableMap.copyOf(singleValues);
    this.timeSeries = ImmutableMap.copyOf(timeSeries);
  }

  @Override
  public Observables.Meta metaBean() {
    return Observables.Meta.INSTANCE;
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
   * Gets single observable values, keyed by ID.
   * @return the value of the property, not null
   */
  public ImmutableMap<? extends MarketDataId<?>, Object> getSingleValues() {
    return singleValues;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets time series of observable values, keyed by ID.
   * @return the value of the property, not null
   */
  public ImmutableMap<? extends MarketDataId<?>, LocalDateDoubleTimeSeries> getTimeSeries() {
    return timeSeries;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      Observables other = (Observables) obj;
      return JodaBeanUtils.equal(getSingleValues(), other.getSingleValues()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getSingleValues());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("Observables{");
    buf.append("singleValues").append('=').append(getSingleValues()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(getTimeSeries()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Observables}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code singleValues} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<? extends MarketDataId<?>, Object>> singleValues = DirectMetaProperty.ofImmutable(
        this, "singleValues", Observables.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<? extends MarketDataId<?>, LocalDateDoubleTimeSeries>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", Observables.class, (Class) ImmutableMap.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "singleValues",
        "timeSeries");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1875949450:  // singleValues
          return singleValues;
        case 779431844:  // timeSeries
          return timeSeries;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public Observables.Builder builder() {
      return new Observables.Builder();
    }

    @Override
    public Class<? extends Observables> beanType() {
      return Observables.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code singleValues} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<? extends MarketDataId<?>, Object>> singleValues() {
      return singleValues;
    }

    /**
     * The meta-property for the {@code timeSeries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<? extends MarketDataId<?>, LocalDateDoubleTimeSeries>> timeSeries() {
      return timeSeries;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1875949450:  // singleValues
          return ((Observables) bean).getSingleValues();
        case 779431844:  // timeSeries
          return ((Observables) bean).getTimeSeries();
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
   * The bean-builder for {@code Observables}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<Observables> {

    private Map<? extends MarketDataId<?>, Object> singleValues = ImmutableMap.of();
    private Map<? extends MarketDataId<?>, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(Observables beanToCopy) {
      this.singleValues = beanToCopy.getSingleValues();
      this.timeSeries = beanToCopy.getTimeSeries();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1875949450:  // singleValues
          return singleValues;
        case 779431844:  // timeSeries
          return timeSeries;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1875949450:  // singleValues
          this.singleValues = (Map<? extends MarketDataId<?>, Object>) newValue;
          break;
        case 779431844:  // timeSeries
          this.timeSeries = (Map<? extends MarketDataId<?>, LocalDateDoubleTimeSeries>) newValue;
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
    public Observables build() {
      return new Observables(
          singleValues,
          timeSeries);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code singleValues} property in the builder.
     * @param singleValues  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder singleValues(Map<? extends MarketDataId<?>, Object> singleValues) {
      JodaBeanUtils.notNull(singleValues, "singleValues");
      this.singleValues = singleValues;
      return this;
    }

    /**
     * Sets the {@code timeSeries} property in the builder.
     * @param timeSeries  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder timeSeries(Map<? extends MarketDataId<?>, LocalDateDoubleTimeSeries> timeSeries) {
      JodaBeanUtils.notNull(timeSeries, "timeSeries");
      this.timeSeries = timeSeries;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("Observables.Builder{");
      buf.append("singleValues").append('=').append(JodaBeanUtils.toString(singleValues)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}