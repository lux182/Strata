/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.credit.cds;

import java.time.LocalDate;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.ConstantNodalCurve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveExtrapolators;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.math.impl.rootfinding.BracketRoot;
import com.opengamma.strata.math.impl.rootfinding.BrentSingleRootFinder;
import com.opengamma.strata.math.impl.rootfinding.RealSingleRootFinder;
import com.opengamma.strata.product.credit.cds.ResolvedCds;
import com.opengamma.strata.product.credit.cds.ResolvedCdsTrade;

/**
 * Simple credit curve calibrator.
 * <p>
 * This is a bootstrapper for the credit curve that is consistent with ISDA 
 * in that it will produce the same curve from the same inputs (up to numerical round-off). 
 * <p>
 * The external pricer, {@link IsdaCdsTradePricer}, is used in the calibration.
 */
public class SimpleCreditCurveCalibrator extends IsdaCompliantCreditCurveCalibrator {

  /**
   * The default implementation.
   */
  public static final SimpleCreditCurveCalibrator DEFAULT = new SimpleCreditCurveCalibrator();

  /**
   * The root bracket finder.
   */
  private static final BracketRoot BRACKER = new BracketRoot();
  /**
   * The root finder.
   */
  private static final RealSingleRootFinder ROOTFINDER = new BrentSingleRootFinder();

  //-------------------------------------------------------------------------
  /**
   * Constructs a default credit curve builder. 
   * <p>
   * The original ISDA accrual-on-default formula (version 1.8.2 and lower) is used.
   */
  public SimpleCreditCurveCalibrator() {
    super();
  }

  /**
   * Constructors a credit curve calibrator with the accrual-on-default formula specified.
   * 
   * @param formula  the accrual-on-default formula
   */
  public SimpleCreditCurveCalibrator(AccrualOnDefaultFormulae formula) {
    super(formula);
  }

  //-------------------------------------------------------------------------
  @Override
  NodalCurve calibrate(ResolvedCdsTrade[] calibrationCDSs, double[] premiums, double[] pointsUpfront, CurveName name,
      LocalDate valuationDate, CreditDiscountFactors discountFactors, RecoveryRates recoveryRates, ReferenceData refData) {

    int n = calibrationCDSs.length;
    double[] guess = new double[n];
    double[] t = new double[n];
    double[] lgd = new double[n];
    for (int i = 0; i < n; i++) {
      LocalDate endDate = calibrationCDSs[i].getProduct().getProtectionEndDate();
      t[i] = discountFactors.relativeYearFraction(endDate);
      lgd[i] = 1d - recoveryRates.recoveryRate(endDate);
      guess[i] = (premiums[i] + pointsUpfront[i] / t[i]) / lgd[i];
    }
    DoubleArray times = DoubleArray.ofUnsafe(t);
    CurveMetadata baseMetadata = DefaultCurveMetadata.builder()
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.ZERO_RATE)
        .curveName(name)
        .dayCount(discountFactors.getDayCount())
        .build();
    NodalCurve creditCurve = n == 1 ? ConstantNodalCurve.of(baseMetadata, t[0], guess[0])
        : InterpolatedNodalCurve.of(
            baseMetadata,
            times,
            DoubleArray.ofUnsafe(guess),
            CurveInterpolators.PRODUCT_LINEAR,
            CurveExtrapolators.FLAT,
            CurveExtrapolators.PRODUCT_LINEAR);

    for (int i = 0; i < n; i++) {
      Function<Double, Double> func = getPriceFunction(i, calibrationCDSs[i], premiums[i], pointsUpfront[i], valuationDate,
          creditCurve, discountFactors, recoveryRates, refData);
      double[] bracket = BRACKER.getBracketedPoints(func, 0.8 * guess[i], 1.25 * guess[i], 0.0, Double.POSITIVE_INFINITY);
      double zeroRate = bracket[0] > bracket[1] ? ROOTFINDER.getRoot(func, bracket[1], bracket[0])
          : ROOTFINDER.getRoot(func, bracket[0], bracket[1]); //Negative guess handled
      creditCurve = creditCurve.withParameter(i, zeroRate);
    }

    return creditCurve;
  }

  private Function<Double, Double> getPriceFunction(int index, ResolvedCdsTrade cds, double flactionalSpread,
      double pointsUpfront, LocalDate valuationDate, NodalCurve creditCurve, CreditDiscountFactors discountFactors,
      RecoveryRates recoveryRates, ReferenceData refData) {
    ResolvedCds cdsProduct = cds.getProduct();
    Currency currency = cdsProduct.getCurrency();
    StandardId legalEntityId = cdsProduct.getLegalEntityId();
    Pair<StandardId, Currency> pair = Pair.of(legalEntityId, currency);
    CreditRatesProvider ratesbase = CreditRatesProvider.builder()
        .valuationDate(valuationDate)
        .discountCurves(ImmutableMap.of(currency, discountFactors))
        .recoveryRateCurves(ImmutableMap.of(legalEntityId, recoveryRates))
        .build();
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        NodalCurve tempCreditCurve = creditCurve.withParameter(index, x);
        CreditRatesProvider rates = ratesbase.toBuilder()
            .creditCurves(ImmutableMap.of(pair, LegalEntitySurvivalProbabilities.of(
                legalEntityId, IsdaCompliantZeroRateDiscountFactors.of(currency, valuationDate, tempCreditCurve))))
            .build();
        double price = getTradePricer().price(cds, rates, flactionalSpread, PriceType.CLEAN, refData);
        return price - pointsUpfront;
      }
    };
    return func;
  }

}
