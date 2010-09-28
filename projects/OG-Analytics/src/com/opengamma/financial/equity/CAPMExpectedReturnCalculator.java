/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.equity;

import org.apache.commons.lang.Validate;

import com.opengamma.financial.timeseries.analysis.DoubleTimeSeriesStatisticsCalculator;
import com.opengamma.financial.timeseries.util.TimeSeriesDataTestUtils;
import com.opengamma.util.timeseries.DoubleTimeSeries;

/**
 * 
 */
public class CAPMExpectedReturnCalculator {
  private final DoubleTimeSeriesStatisticsCalculator _expectedMarketReturnCalculator;
  private final DoubleTimeSeriesStatisticsCalculator _expectedRiskFreeReturnCalculator;

  public CAPMExpectedReturnCalculator(final DoubleTimeSeriesStatisticsCalculator expectedMarketReturnCalculator,
      final DoubleTimeSeriesStatisticsCalculator expectedRiskFreeReturnCalculator) {
    Validate.notNull(expectedMarketReturnCalculator, "expected market return calculator");
    Validate.notNull(expectedRiskFreeReturnCalculator, "expected risk free return calculator");
    _expectedMarketReturnCalculator = expectedMarketReturnCalculator;
    _expectedRiskFreeReturnCalculator = expectedRiskFreeReturnCalculator;
  }

  public Double evaluate(final DoubleTimeSeries<?> marketReturnTS, final DoubleTimeSeries<?> riskFreeReturnTS, final double beta) {
    TimeSeriesDataTestUtils.testNotNullOrEmpty(marketReturnTS);
    TimeSeriesDataTestUtils.testNotNullOrEmpty(riskFreeReturnTS);
    final Double expectedMarketReturn = _expectedMarketReturnCalculator.evaluate(marketReturnTS);
    final Double expectedRiskFreeReturn = _expectedRiskFreeReturnCalculator.evaluate(riskFreeReturnTS);
    return expectedRiskFreeReturn + beta * (expectedMarketReturn - expectedRiskFreeReturn);
  }

}
