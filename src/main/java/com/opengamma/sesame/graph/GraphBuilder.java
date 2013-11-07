/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.graph;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.opengamma.core.position.PositionOrTrade;
import com.opengamma.core.security.Security;
import com.opengamma.id.ObjectId;
import com.opengamma.sesame.config.CompositeFunctionConfig;
import com.opengamma.sesame.config.FunctionConfig;
import com.opengamma.sesame.config.GraphConfig;
import com.opengamma.sesame.config.ViewColumn;
import com.opengamma.sesame.config.ViewDef;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.DefaultImplementationProvider;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.FunctionRepo;
import com.opengamma.sesame.function.NoOutputFunction;
import com.opengamma.sesame.function.SecurityFunctionAdapter;
import com.opengamma.util.ArgumentChecker;

/**
 *
 */
public final class GraphBuilder {

  private final FunctionRepo _functionRepo;
  private final ComponentMap _componentMap;
  private final FunctionConfig _defaultConfig;
  private final NodeDecorator _nodeDecorator;
  private final DefaultImplementationProvider _defaultImplProvider;

  public GraphBuilder(FunctionRepo functionRepo,
                      ComponentMap componentMap,
                      FunctionConfig defaultConfig,
                      NodeDecorator nodeDecorator) {
    _functionRepo = ArgumentChecker.notNull(functionRepo, "functionRepo");
    _componentMap = ArgumentChecker.notNull(componentMap, "componentMap");
    _defaultConfig = ArgumentChecker.notNull(defaultConfig, "defaultConfig");
    _nodeDecorator = ArgumentChecker.notNull(nodeDecorator, "nodeDecorator");
    _defaultImplProvider = new DefaultImplementationProvider(functionRepo);
  }

  public GraphModel build(ViewDef viewDef, Collection<? extends PositionOrTrade> inputs) {
    ImmutableMap.Builder<String, Map<ObjectId, FunctionModel>> builder = ImmutableMap.builder();
    for (ViewColumn column : viewDef.getColumns()) {
      ImmutableMap.Builder<ObjectId, FunctionModel> columnBuilder = ImmutableMap.builder();
      for (PositionOrTrade posOrTrade : inputs) {
        // TODO no need to create a FunctionTree for every target, cache on outputName/inputType

        // if we need to support stateful functions this is the place to do it.
        // the FunctionModel could flag if its tree contains any functions annotated as @Stateful and
        // it wouldn't be eligible for sharing with other inputs

        // look for an output for the position or trade
        String posOrTradeOutput = column.getOutputName(posOrTrade.getClass());
        if (posOrTradeOutput != null) {
          FunctionMetadata function = _functionRepo.getOutputFunction(posOrTradeOutput, posOrTrade.getClass());
          if (function != null) {
            FunctionConfig columnConfig = column.getFunctionConfig(posOrTrade.getClass());
            FunctionConfig config = CompositeFunctionConfig.compose(columnConfig, _defaultConfig, _defaultImplProvider);
            GraphConfig graphConfig = new GraphConfig(config, _componentMap, _nodeDecorator);
            FunctionModel functionModel = FunctionModel.forFunction(function, graphConfig);
            columnBuilder.put(posOrTrade.getUniqueId().getObjectId(), functionModel);
            continue;
          }
        }

        // look for an output for the security type
        Security security = posOrTrade.getSecurity();
        String securityOutput = column.getOutputName(security.getClass());
        if (securityOutput != null) {
          FunctionMetadata functionType = _functionRepo.getOutputFunction(securityOutput, security.getClass());
          if (functionType != null) {
            FunctionConfig columnConfig = column.getFunctionConfig(security.getClass());
            FunctionConfig config = CompositeFunctionConfig.compose(columnConfig, _defaultConfig, _defaultImplProvider);
            GraphConfig graphConfig = new GraphConfig(config, _componentMap, _nodeDecorator);
            FunctionModel securityModel = FunctionModel.forFunction(functionType, graphConfig);
            FunctionModel functionModel = SecurityFunctionAdapter.adapt(securityModel);
            columnBuilder.put(posOrTrade.getUniqueId().getObjectId(), functionModel);
            continue;
          }
        }
        FunctionModel functionModel = FunctionModel.forFunction(NoOutputFunction.METADATA);
        // TODO how will this work for in-memory trades? assign an ID? use object identity?
        // at least some of the analytics code assumes a unique ID (on securities) so maybe we have to assign them
        columnBuilder.put(posOrTrade.getUniqueId().getObjectId(), functionModel);
      }
      builder.put(column.getName(), columnBuilder.build());
    }
    return new GraphModel(builder.build());
  }

}
