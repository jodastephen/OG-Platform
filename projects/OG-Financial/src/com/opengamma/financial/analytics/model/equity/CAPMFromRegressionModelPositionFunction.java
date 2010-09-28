/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.analytics.model.equity;

import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetType;
import com.opengamma.engine.function.FunctionCompilationContext;

/**
 * 
 */
public class CAPMFromRegressionModelPositionFunction extends CAPMFromRegressionModelFunction {

  public CAPMFromRegressionModelPositionFunction(final String startDate) {
    super(startDate);
  }

  @Override
  public boolean canApplyTo(final FunctionCompilationContext context, final ComputationTarget target) {
    return target.getType() == ComputationTargetType.POSITION;
  }

  @Override
  public String getShortName() {
    return "CAPM_RegressionPositionModel";
  }

  @Override
  public ComputationTargetType getTargetType() {
    return ComputationTargetType.POSITION;
  }

  @Override
  public Object getTarget(final ComputationTarget target) {
    return target.getPosition();
  }

}
