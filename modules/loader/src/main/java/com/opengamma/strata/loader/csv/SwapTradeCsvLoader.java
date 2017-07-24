/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.loader.csv.TradeCsvLoader.BUY_SELL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.CONVENTION_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.DATE_ADJ_CNV_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.END_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.FIXED_RATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.NOTIONAL_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.PERIOD_TO_START_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.START_DATE_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.TENOR_FIELD;
import static com.opengamma.strata.loader.csv.TradeCsvLoader.TRADE_DATE_FIELD;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConvention;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.SingleCurrencySwapConvention;
import com.opengamma.strata.product.swap.type.XCcyIborIborSwapConvention;

/**
 * Loads Swap trades from CSV files.
 */
final class SwapTradeCsvLoader {

  // CSV column headers
  private static final String ROLL_CONVENTION_FIELD = "Roll Convention";
  private static final String STUB_CONVENTION_FIELD = "Stub Convention";
  private static final String FIRST_REGULAR_START_DATE_FIELD = "First Regular Start Date";
  private static final String LAST_REGULAR_END_DATE_FIELD = "Last Regular End Date";
  private static final String FX_RATE_FIELD = "FX Rate";

  /**
   * Parses from the CSV row.
   * 
   * @param row  the CSV row
   * @param info  the trade info
   * @param refData  the reference data
   * @return the loaded trades, all errors are captured in the result
   */
  static Trade parse(CsvRow row, TradeInfo info, ReferenceData refData) {
    // using an 'unnecessary' nested class to allow parsing logic to be split
    // into smaller methods without passing lots of parameters around
    return new Parsed(row).parse(info, refData);
  }

  //-------------------------------------------------------------------------
  // Restricted constructor.
  private SwapTradeCsvLoader() {
  }

  //-------------------------------------------------------------------------
  // the parsed row
  static class Parsed {

    private final BuySell buySell;
    private final double notional;
    private final double fixedRate;
    private final Optional<String> conventionOpt;
    private final Optional<Period> periodToStartOpt;
    private final Optional<Tenor> tenorOpt;
    private final Optional<LocalDate> startDateOpt;
    private final Optional<LocalDate> endDateOpt;
    private final Optional<RollConvention> rollConventionOpt;
    private final Optional<StubConvention> stubConventionOpt;
    private final Optional<LocalDate> firstRegularStartDateOpt;
    private final Optional<LocalDate> lastRegEndDateOpt;
    private final BusinessDayConvention dateCnv;
    private final Optional<HolidayCalendarId> dateCalOpt;
    private final Optional<Double> fxRateOpt;

    // parse the CSV row
    Parsed(CsvRow row) {
      buySell = row.findValue(BUY_SELL_FIELD).map(s -> BuySell.of(s)).orElse(BuySell.BUY);
      notional = new BigDecimal(row.getValue(NOTIONAL_FIELD)).doubleValue();
      fixedRate = new BigDecimal(row.getValue(FIXED_RATE_FIELD)).divide(BigDecimal.valueOf(100)).doubleValue();
      conventionOpt = row.findValue(CONVENTION_FIELD);
      periodToStartOpt = row.findValue(PERIOD_TO_START_FIELD).map(s -> Tenor.parse(s).getPeriod());
      tenorOpt = row.findValue(TENOR_FIELD).map(s -> Tenor.parse(s));
      startDateOpt = row.findValue(START_DATE_FIELD).map(s -> TradeCsvLoader.parseDate(s));
      endDateOpt = row.findValue(END_DATE_FIELD).map(s -> TradeCsvLoader.parseDate(s));
      rollConventionOpt = row.findValue(ROLL_CONVENTION_FIELD).map(s -> RollConvention.of(s));
      stubConventionOpt = row.findValue(STUB_CONVENTION_FIELD).map(s -> StubConvention.of(s));
      firstRegularStartDateOpt =
          row.findValue(FIRST_REGULAR_START_DATE_FIELD).map(s -> TradeCsvLoader.parseDate(s));
      lastRegEndDateOpt = row.findValue(LAST_REGULAR_END_DATE_FIELD).map(s -> TradeCsvLoader.parseDate(s));
      dateCnv = row.findValue(DATE_ADJ_CNV_FIELD)
          .map(s -> BusinessDayConvention.of(s)).orElse(BusinessDayConventions.MODIFIED_FOLLOWING);
      dateCalOpt = row.findValue(DATE_ADJ_CAL_FIELD).map(s -> HolidayCalendarId.of(s));
      fxRateOpt = row.findValue(FX_RATE_FIELD).map(str -> new BigDecimal(str).doubleValue());
    }

    public Trade parse(TradeInfo info, ReferenceData refData) {
      if (conventionOpt.isPresent()) {
        return parseWithConvention(info, refData);
      }
      throw invalidFields();
    }

    // parse using a cross currency convention
    private Trade parseWithConvention(TradeInfo info, ReferenceData refData) {

      // explicit dates take precedence over relative ones
      if (startDateOpt.isPresent() && endDateOpt.isPresent()) {
        validateExplicitDates();
        LocalDate startDate = startDateOpt.get();
        LocalDate endDate = endDateOpt.get();
        if (fxRateOpt.isPresent()) {
          XCcyIborIborSwapConvention convention = XCcyIborIborSwapConvention.of(conventionOpt.get());
          double notionalFlat = notional * fxRateOpt.get();
          SwapTrade trade = convention.toTrade(info, startDate, endDate, buySell, notional, notionalFlat, fixedRate);
          return adjustTrade(trade);
        } else {
          SingleCurrencySwapConvention convention = SingleCurrencySwapConvention.of(conventionOpt.get());
          SwapTrade trade = convention.toTrade(info, startDate, endDate, buySell, notional, fixedRate);
          return adjustTrade(trade);
        }
      }

      // relative dates
      if (periodToStartOpt.isPresent() && tenorOpt.isPresent() && info.getTradeDate().isPresent()) {
        validateRelativeDates();
        LocalDate tradeDate = info.getTradeDate().get();
        Period periodToStart = periodToStartOpt.get();
        Tenor tenor = tenorOpt.get();
        if (fxRateOpt.isPresent()) {
          XCcyIborIborSwapConvention convention = XCcyIborIborSwapConvention.of(conventionOpt.get());
          double notionalFlat = notional * fxRateOpt.get();
          SwapTrade trade =
              convention.createTrade(tradeDate, periodToStart, tenor, buySell, notional, notionalFlat, fixedRate, refData);
          trade = trade.toBuilder().info(info).build();
          return adjustTrade(trade);
        } else {
          SingleCurrencySwapConvention convention = SingleCurrencySwapConvention.of(conventionOpt.get());
          SwapTrade trade = convention.createTrade(tradeDate, periodToStart, tenor, buySell, notional, fixedRate, refData);
          trade = trade.toBuilder().info(info).build();
          return adjustTrade(trade);
        }
      }
      throw invalidFields();
    }

    // when using explicit dates, ensure relative dates not present
    private void validateExplicitDates() {
      if (periodToStartOpt.isPresent() || tenorOpt.isPresent()) {
        throw new IllegalArgumentException(
            "CSV file 'Swap' trade had invalid combination of fields. When these fields are found " +
                ImmutableList.of(CONVENTION_FIELD, START_DATE_FIELD, END_DATE_FIELD) +
                " then these fields must not be present " +
                ImmutableList.of(PERIOD_TO_START_FIELD, TENOR_FIELD));
      }
    }

    // when using relative dates, ensure explicit dates not present
    private void validateRelativeDates() {
      if (startDateOpt.isPresent() || endDateOpt.isPresent()) {
        throw new IllegalArgumentException(
            "CSV file 'Swap' trade had invalid combination of fields. When these fields are found " +
                ImmutableList.of(CONVENTION_FIELD, PERIOD_TO_START_FIELD, TENOR_FIELD, TRADE_DATE_FIELD) +
                " then these fields must not be present " +
                ImmutableList.of(START_DATE_FIELD, END_DATE_FIELD));
      }
    }

    // creates an exception explaining the problem
    private IllegalArgumentException invalidFields() {
      return new IllegalArgumentException(
          "CSV file 'Swap' trade had invalid combination of fields. These fields are mandatory:" +
              ImmutableList.of(BUY_SELL_FIELD, NOTIONAL_FIELD, FIXED_RATE_FIELD) +
              " and one of these combinations is mandatory: " +
              ImmutableList.of(CONVENTION_FIELD, TRADE_DATE_FIELD, PERIOD_TO_START_FIELD, TENOR_FIELD) +
              " or " +
              ImmutableList.of(CONVENTION_FIELD, START_DATE_FIELD, END_DATE_FIELD));
    }

    // adjust trade based on additional fields specified
    private Trade adjustTrade(SwapTrade trade) {

      if (!rollConventionOpt.isPresent() &&
          !stubConventionOpt.isPresent() &&
          !firstRegularStartDateOpt.isPresent() &&
          !lastRegEndDateOpt.isPresent() &&
          !dateCalOpt.isPresent()) {
        return trade;
      }
      ImmutableList.Builder<SwapLeg> legBuilder = ImmutableList.builder();
      for (SwapLeg leg : trade.getProduct().getLegs()) {
        RateCalculationSwapLeg swapLeg = (RateCalculationSwapLeg) leg;
        PeriodicSchedule.Builder scheduleBuilder = swapLeg.getAccrualSchedule().toBuilder();
        rollConventionOpt.ifPresent(rc -> scheduleBuilder.rollConvention(rc));
        stubConventionOpt.ifPresent(sc -> scheduleBuilder.stubConvention(sc));
        firstRegularStartDateOpt.ifPresent(date -> scheduleBuilder.firstRegularStartDate(date));
        lastRegEndDateOpt.ifPresent(date -> scheduleBuilder.lastRegularEndDate(date));
        dateCalOpt.ifPresent(cal -> scheduleBuilder.businessDayAdjustment(BusinessDayAdjustment.of(dateCnv, cal)));
        legBuilder.add(swapLeg.toBuilder()
            .accrualSchedule(scheduleBuilder.build())
            .build());
      }
      return trade.toBuilder()
          .product(trade.getProduct().toBuilder()
              .legs(legBuilder.build())
              .build())
          .build();
    }

  }

}
