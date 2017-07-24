/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static java.util.stream.Collectors.toList;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.io.CsvIterator;
import com.opengamma.strata.collect.io.CsvRow;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.io.UnicodeBom;
import com.opengamma.strata.collect.result.FailureItem;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.ValueWithFailures;
import com.opengamma.strata.product.Trade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.TradeInfoBuilder;
import com.opengamma.strata.product.deposit.type.TermDepositConventions;
import com.opengamma.strata.product.fra.type.FraConventions;
import com.opengamma.strata.product.swap.type.SingleCurrencySwapConvention;

/**
 * Loads trades from CSV files.
 * <p>
 * The trades are expected to be in a CSV format known to Strata.
 * The parser is flexible, understanding a number of different ways to define each trade.
 * Columns may occur in any order.
 * 
 * <h4>Common</h4>
 * <p>
 * The following standard columns are supported:<br />
 * <ul>
 * <li>The 'Type' column is required, and must be the instrument type,
 *   such as 'Fra' or 'Swap'
 * <li>The 'Id Scheme' column is optional, and is the name of the scheme that the trade
 *   identifier is unique within, such as 'OG-Trade'
 * <li>The 'Id' column is optional, and is the identifier of the trade,
 *   such as 'FRA12345'
 * <li>The 'Trade Date' column is optional, and is the date that the trade occurred,
 *   such as '2017-08-01'
 * <li>The 'Trade Time' column is optional, and is the time of day that the trade occurred,
 *   such as '11:30'
 * <li>The 'Trade Zone' column is optional, and is the time-zone that the trade occurred,
 *   such as 'Europe/London'
 * </ul>
 * 
 * <h4>Fra</h4>
 * <p>
 * The following columns are supported for 'Fra' trades:
 * <ul>
 * <li>'Buy Sell' - mandatory
 * <li>'Notional' - mandatory
 * <li>'Fixed Rate' - mandatory, percentage
 * <li>'Convention' - see below, see {@link FraConventions}
 * <li>'Period To Start' - see below
 * <li>'Start Date' - see below
 * <li>'End Date' - see below
 * <li>'Index' - see below
 * <li>'Interpolated Index' - see below
 * <li>'Day Count' - see below
 * <li>'Date Convention' - optional
 * <li>'Date Calendar' - optional
 * </ul>
 * <p>
 * Valid combinations to define a FRA are:
 * <ul>
 * <li>'Convention', 'Trade Date', 'Period To Start'
 * <li>'Convention', 'Start Date', 'End Date'
 * <li>'Index', 'Start Date', 'End Date' plus optionally 'Interpolated Index', 'Day Count'
 * </ul>
 * 
 * <h4>Swap</h4>
 * <p>
 * The following columns are supported for 'Swap' trades:
 * <ul>
 * <li>'Buy Sell' - mandatory
 * <li>'Notional' - mandatory
 * <li>'Fixed Rate' - mandatory, percentage (treated as the spread for some swap types)
 * <li>'Convention' - mandatory, see {@link SingleCurrencySwapConvention} implementations
 * <li>'Period To Start'- see below
 * <li>'Tenor'- see below
 * <li>'Start Date'- see below
 * <li>'End Date'- see below
 * <li>'Roll Convention' - optional
 * <li>'Stub Convention' - optional
 * <li>'First Regular Start Date' - optional
 * <li>'Last Regular End Date' - optional
 * <li>'Date Convention' - optional
 * <li>'Date Calendar' - optional
 * </ul>
 * <p>
 * Valid combinations to define a Swap are:
 * <ul>
 * <li>'Convention', 'Trade Date', 'Period To Start', 'Tenor'
 * <li>'Convention', 'Start Date', 'End Date'
 * </ul>
 * 
 * <h4>Term Deposit</h4>
 * <p>
 * The following columns are supported for 'TermDeposit' trades:
 * <ul>
 * <li>'Buy Sell' - mandatory
 * <li>'Notional' - mandatory
 * <li>'Fixed Rate' - mandatory, percentage
 * <li>'Convention'- see below, see {@link TermDepositConventions} implementations
 * <li>'Tenor'- see below
 * <li>'Start Date'- see below
 * <li>'End Date'- see below
 * <li>'Currency'- see below
 * <li>'Day Count'- see below
 * <li>'Date Convention' - optional
 * <li>'Date Calendar' - optional
 * </ul>
 * <p>
 * Valid combinations to define a Term Deposit are:
 * <ul>
 * <li>'Convention', 'Trade Date', 'Period To Start'
 * <li>'Convention', 'Start Date', 'End Date'
 * <li>'Start Date', 'End Date', 'Currency', 'Day Count'
 * </ul>
 */
public final class TradeCsvLoader {

  // DMY with slashes format
  private static final DateTimeFormatter DD_MM_YY_SLASH = DateTimeFormatter.ofPattern("dd/MM/yy", Locale.ENGLISH);
  private static final DateTimeFormatter DD_MM_YYYY_SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter YYYY_MM_DD_SLASH = DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.ENGLISH);
  private static final DateTimeFormatter YYYY_MM_DD_DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
  private static final DateTimeFormatter D_MMM_YYYY_DASH =
      new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-yyyy").toFormatter(Locale.ENGLISH);
  private static final DateTimeFormatter D_MMM_YYYY_NODASH =
      new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dMMMyyyy").toFormatter(Locale.ENGLISH);

  // shared CSV headers
  static final String TRADE_DATE_FIELD = "Trade Date";
  static final String CONVENTION_FIELD = "Convention";
  static final String BUY_SELL_FIELD = "Buy Sell";
  static final String CURRENCY_FIELD = "Currency";
  static final String NOTIONAL_FIELD = "Notional";
  static final String INDEX_FIELD = "Index";
  static final String INTERPOLATED_INDEX_FIELD = "Interpolated Index";
  static final String FIXED_RATE_FIELD = "Fixed Rate";
  static final String PERIOD_TO_START_FIELD = "Period To Start";
  static final String TENOR_FIELD = "Tenor";
  static final String START_DATE_FIELD = "Start Date";
  static final String END_DATE_FIELD = "End Date";
  static final String DATE_ADJ_CNV_FIELD = "Date Convention";
  static final String DATE_ADJ_CAL_FIELD = "Date Calendar";
  static final String DAY_COUNT_FIELD = "Day Count";

  // CSV column headers
  private static final String TYPE_FIELD = "Type";
  private static final String ID_SCHEME_FIELD = "Id Scheme";
  private static final String ID_FIELD = "Id";
  private static final String TRADE_TIME_FIELD = "Trade Time";
  private static final String TRADE_ZONE_FIELD = "Trade Zone";

  /**
   * The reference data.
   */
  private final ReferenceData refData;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance that uses the standard set of reference data.
   * 
   * @return the loader
   */
  public static TradeCsvLoader of() {
    return new TradeCsvLoader(ReferenceData.standard());
  }

  /**
   * Obtains an instance that uses the standard set of reference data.
   * 
   * @param refData  the reference data
   * @return the loader
   */
  public static TradeCsvLoader of(ReferenceData refData) {
    return new TradeCsvLoader(refData);
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   * 
   * @param refData  the reference data
   */
  private TradeCsvLoader(ReferenceData refData) {
    this.refData = ArgChecker.notNull(refData, "refData");
  }

  //-------------------------------------------------------------------------
  /**
   * Loads one or more CSV format trade files.
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * This method uses {@link UnicodeBom} to interpret it.
   * 
   * @param resources  the CSV resources
   * @return the loaded trades, trade-level errors are captured in the result
   */
  public ValueWithFailures<List<Trade>> load(ResourceLocator... resources) {
    return load(Arrays.asList(resources));
  }

  /**
   * Loads one or more CSV format trade files.
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * This method uses {@link UnicodeBom} to interpret it.
   * 
   * @param resources  the CSV resources
   * @return the loaded trades, all errors are captured in the result
   */
  public ValueWithFailures<List<Trade>> load(Collection<ResourceLocator> resources) {
    Collection<CharSource> charSources = resources.stream()
        .map(r -> UnicodeBom.toCharSource(r.getByteSource()))
        .collect(toList());
    return parse(charSources);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses one or more CSV format trade files.
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using {@link UnicodeBom}.
   * 
   * @param charSources  the CSV character sources
   * @return the loaded trades, all errors are captured in the result
   */
  public ValueWithFailures<List<Trade>> parse(Collection<CharSource> charSources) {
    return parse(charSources, tradeType -> true);
  }

  /**
   * Parses one or more CSV format trade files.
   * <p>
   * A predicate is specified that can be used to filter the trades based on the trade type.
   * <p>
   * CSV files sometimes contain a Unicode Byte Order Mark.
   * Callers are responsible for handling this, such as by using {@link UnicodeBom}.
   * 
   * @param charSources  the CSV character sources
   * @param tradeTypePredicate  the predicate used to select the trade type, returns true to retain, false to drop
   * @return the loaded trades, all errors are captured in the result
   */
  public ValueWithFailures<List<Trade>> parse(Collection<CharSource> charSources, Predicate<String> tradeTypePredicate) {
    try {
      ValueWithFailures<List<Trade>> result = ValueWithFailures.of(ImmutableList.of());
      for (CharSource charSource : charSources) {
        ValueWithFailures<List<Trade>> singleResult = parseFile(charSource, tradeTypePredicate);
        result = result.combinedWith(singleResult, Guavate::concatToList);
      }
      return result;

    } catch (RuntimeException ex) {
      return ValueWithFailures.of(ImmutableList.of(), FailureItem.of(FailureReason.ERROR, ex));
    }
  }

  // loads a single CSV file, filtering by trade type
  private ValueWithFailures<List<Trade>> parseFile(CharSource charSource, Predicate<String> tradeTypePredicate) {
    try (CsvIterator csv = CsvIterator.of(charSource, true)) {
      if (!csv.headers().contains(TYPE_FIELD)) {
        return ValueWithFailures.of(ImmutableList.of(),
            FailureItem.of(FailureReason.PARSING, "CSV file does not contain 'Type' header: {}", charSource));
      }
      return parseFile(csv);

    } catch (RuntimeException ex) {
      return ValueWithFailures.of(ImmutableList.of(),
          FailureItem.of(FailureReason.PARSING, ex, "CSV file could not be parsed: {}", charSource));
    }
  }

  // loads a single CSV file
  private ValueWithFailures<List<Trade>> parseFile(CsvIterator csv) {
    List<Trade> trades = new ArrayList<>();
    List<FailureItem> failures = new ArrayList<>();
    int line = 2;
    for (CsvRow row : (Iterable<CsvRow>) () -> csv) {
      try {
        String type = row.getField(TYPE_FIELD).toUpperCase(Locale.ENGLISH);
        TradeInfo info = parseTradeInfo(row);
        switch (type.toUpperCase(Locale.ENGLISH)) {
          case "FRA":
            trades.add(FraTradeCsvLoader.parse(row, info, refData));
            break;
          case "SWAP":
            trades.add(SwapTradeCsvLoader.parse(row, info, refData));
            break;
          case "TERMDEPOSIT":
          case "TERM DEPOSIT":
            trades.add(TermDepositTradeCsvLoader.parse(row, info, refData));
            break;
          default:
            failures.add(FailureItem.of(FailureReason.PARSING, "CSV file trade type '{}' is not known at line {}", type, line));
            break;
        }
        line++;
      } catch (RuntimeException ex) {
        failures.add(FailureItem.of(FailureReason.PARSING, ex, "CSV file trade could not be parsed at line {}", line));
      }
    }
    return ValueWithFailures.of(trades, failures);
  }

  // parse the trade info
  private TradeInfo parseTradeInfo(CsvRow row) {
    TradeInfoBuilder infoBuilder = TradeInfo.builder();
    String scheme = row.findField(ID_SCHEME_FIELD).orElse("OG-Trade");
    row.findValue(ID_FIELD).ifPresent(id -> infoBuilder.id(StandardId.of(scheme, id)));
    row.findValue(TRADE_DATE_FIELD).ifPresent(dateStr -> infoBuilder.tradeDate(parseDate(dateStr)));
    row.findValue(TRADE_TIME_FIELD).ifPresent(timeStr -> infoBuilder.tradeTime(LocalTime.parse(timeStr)));
    row.findValue(TRADE_ZONE_FIELD).ifPresent(zoneStr -> infoBuilder.zone(ZoneId.of(zoneStr)));
    return infoBuilder.build();
  }

  //-------------------------------------------------------------------------
  // parses a date
  static LocalDate parseDate(String dateStr) {
    try {
      if (dateStr.isEmpty()) {
        return null;
      }
      // yyyy-MM-dd
      if (dateStr.charAt(4) == '-' && dateStr.charAt(7) == '-') {
        return LocalDate.parse(dateStr, YYYY_MM_DD_DASH);
      }
      // yyyy/MM/dd
      if (dateStr.charAt(4) == '/' && dateStr.charAt(7) == '/') {
        return LocalDate.parse(dateStr, YYYY_MM_DD_SLASH);
      }
      // dd/MM/yy
      // dd/MM/yyyy
      if (dateStr.charAt(2) == '/' && dateStr.charAt(5) == '/') {
        if (dateStr.length() == 8) {
          return LocalDate.parse(dateStr, DD_MM_YY_SLASH);
        } else {
          return LocalDate.parse(dateStr, DD_MM_YYYY_SLASH);
        }
      }
      // d-MMM-yyyy
      if (dateStr.charAt(dateStr.length() - 5) == '-') {
        return LocalDate.parse(dateStr, D_MMM_YYYY_DASH);
      }
      // dMMMyyyy (and all others)
      return LocalDate.parse(dateStr, D_MMM_YYYY_NODASH);

    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException(
          "Unknown date format, must be formatted as yyyy-MM-dd, dd/MM/yyyy, yyyy/MM/dd, 'd-MMM-yyyy' or 'dMMMyyyy': " + dateStr,
          ex);
    }
  }

}
