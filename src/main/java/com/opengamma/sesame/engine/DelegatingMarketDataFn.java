/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Set;

import org.threeten.bp.Period;

import com.opengamma.util.result.FunctionResult;
import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataSeries;
import com.opengamma.sesame.marketdata.MarketDataValues;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.time.LocalDateRange;

/**
 * Simple delegating market data provider that allows the engine to switch market data providers between cycles
 * while the functions still point at the same provider.
 * TODO will need dynamic thread local binding of provider functions to support full reval
 */
/* package */ class DelegatingMarketDataFn implements MarketDataFn {

  private MarketDataFn _delegate;

  /**
   * Sets the underlying provider, should only be called between calculation cycles.
   * @param delegate The underlying market data provider
   */
  /* package */ void setDelegate(MarketDataFn delegate) {
    _delegate = ArgumentChecker.notNull(delegate, "delegate");
  }

  @Override
  public FunctionResult<MarketDataValues> requestData(MarketDataRequirement requirement) {
    return _delegate.requestData(requirement);
  }

  @Override
  public FunctionResult<MarketDataValues> requestData(Set<MarketDataRequirement> requirements) {
    return _delegate.requestData(requirements);
  }

  @Override
  public FunctionResult<MarketDataSeries> requestData(MarketDataRequirement requirement, LocalDateRange dateRange) {
    return _delegate.requestData(requirement, dateRange);
  }

  @Override
  public FunctionResult<MarketDataSeries> requestData(Set<MarketDataRequirement> requirements, LocalDateRange dateRange) {
    return _delegate.requestData(requirements, dateRange);
  }

  @Override
  public FunctionResult<MarketDataSeries> requestData(MarketDataRequirement requirement, Period seriesPeriod) {
    return _delegate.requestData(requirement, seriesPeriod);
  }

  @Override
  public FunctionResult<MarketDataSeries> requestData(Set<MarketDataRequirement> requirements, Period seriesPeriod) {
    return _delegate.requestData(requirements, seriesPeriod);
  }
}
