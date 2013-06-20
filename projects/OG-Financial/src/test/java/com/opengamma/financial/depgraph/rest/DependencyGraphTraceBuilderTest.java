/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.financial.depgraph.rest;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.testng.annotations.Test;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;
import org.threeten.bp.ZonedDateTime;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.core.value.MarketDataRequirementNames;
import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetSpecification;
import com.opengamma.engine.DefaultComputationTargetResolver;
import com.opengamma.engine.InMemorySecuritySource;
import com.opengamma.engine.function.AbstractFunction;
import com.opengamma.engine.function.CachingFunctionRepositoryCompiler;
import com.opengamma.engine.function.CompiledFunctionService;
import com.opengamma.engine.function.FunctionCompilationContext;
import com.opengamma.engine.function.FunctionInvoker;
import com.opengamma.engine.function.InMemoryFunctionRepository;
import com.opengamma.engine.function.resolver.DefaultFunctionResolver;
import com.opengamma.engine.marketdata.MarketDataListener;
import com.opengamma.engine.marketdata.MarketDataPermissionProvider;
import com.opengamma.engine.marketdata.MarketDataProvider;
import com.opengamma.engine.marketdata.MarketDataSnapshot;
import com.opengamma.engine.marketdata.availability.DefaultMarketDataAvailabilityProvider;
import com.opengamma.engine.marketdata.availability.DomainMarketDataAvailabilityFilter;
import com.opengamma.engine.marketdata.availability.MarketDataAvailabilityProvider;
import com.opengamma.engine.marketdata.resolver.SingleMarketDataProviderResolver;
import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.engine.target.ComputationTargetReference;
import com.opengamma.engine.target.ComputationTargetRequirement;
import com.opengamma.engine.target.ComputationTargetType;
import com.opengamma.engine.value.ValueProperties;
import com.opengamma.engine.value.ValuePropertyNames;
import com.opengamma.engine.value.ValueRequirement;
import com.opengamma.engine.value.ValueRequirementNames;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.id.ExternalId;
import com.opengamma.id.ExternalScheme;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.util.test.TestGroup;

/**
 * Tests the diagnostic REST exposure of a dependency graph builder.
 */
@Test(groups = TestGroup.UNIT)
public class DependencyGraphTraceBuilderTest {


  private CompiledFunctionService createFunctionCompilationService() {
    final InMemoryFunctionRepository functions = new InMemoryFunctionRepository();
    functions.addFunction(new AbstractFunction.NonCompiled() {

      @Override
      public ComputationTargetType getTargetType() {
        return ComputationTargetType.PRIMITIVE;
      }

      @Override
      public boolean canApplyTo(final FunctionCompilationContext context, final ComputationTarget target) {
        return true;
      }

      @Override
      public Set<ValueRequirement> getRequirements(final FunctionCompilationContext context, final ComputationTarget target, final ValueRequirement desiredValue) {
        throw new OpenGammaRuntimeException("test");
      }

      @Override
      public Set<ValueSpecification> getResults(final FunctionCompilationContext context, final ComputationTarget target) {
        return Collections.singleton(new ValueSpecification(ValueRequirementNames.FAIR_VALUE, target.toSpecification(), ValueProperties.with(
            ValuePropertyNames.FUNCTION, "Test").get()));
      }

      @Override
      public FunctionInvoker getFunctionInvoker() {
        fail();
        return null;
      }

    });
    final FunctionCompilationContext context = new FunctionCompilationContext();
    final InMemorySecuritySource securities = new InMemorySecuritySource();
    context.setSecuritySource(securities);
    context.setRawComputationTargetResolver(new DefaultComputationTargetResolver(securities));
    context.setComputationTargetResolver(context.getRawComputationTargetResolver().atVersionCorrection(VersionCorrection.LATEST));
    return new CompiledFunctionService(functions, new CachingFunctionRepositoryCompiler(), context);
  }

  private DependencyGraphBuilderResourceContextBean createContextBean() {
    final DependencyGraphBuilderResourceContextBean bean = new DependencyGraphBuilderResourceContextBean();
    final CompiledFunctionService cfs = createFunctionCompilationService();
    cfs.initialize();
    bean.setFunctionCompilationContext(cfs.getFunctionCompilationContext());
    bean.setFunctionResolver(new DefaultFunctionResolver(cfs));
    bean.setMarketDataProviderResolver(new SingleMarketDataProviderResolver(new MarketDataProvider() {

      @Override
      public void addListener(final MarketDataListener listener) {
        fail();
      }

      @Override
      public void removeListener(final MarketDataListener listener) {
        fail();
      }

      @Override
      public void subscribe(final ValueSpecification valueSpecification) {
        fail();
      }

      @Override
      public void subscribe(final Set<ValueSpecification> valueSpecifications) {
        fail();
      }

      @Override
      public void unsubscribe(final ValueSpecification valueSpecification) {
        fail();
      }

      @Override
      public void unsubscribe(final Set<ValueSpecification> valueSpecifications) {
        fail();
      }

      @Override
      public MarketDataAvailabilityProvider getAvailabilityProvider(final MarketDataSpecification marketDataSpec) {
        return new DomainMarketDataAvailabilityFilter(Arrays.asList(ExternalScheme.of("Foo")), Arrays.asList(MarketDataRequirementNames.MARKET_VALUE))
            .withProvider(new DefaultMarketDataAvailabilityProvider());
      }

      @Override
      public MarketDataPermissionProvider getPermissionProvider() {
        fail();
        return null;
      }

      @Override
      public boolean isCompatible(final MarketDataSpecification marketDataSpec) {
        fail();
        return false;
      }

      @Override
      public MarketDataSnapshot snapshot(final MarketDataSpecification marketDataSpec) {
        fail();
        return null;
      }

      @Override
      public Duration getRealTimeDuration(final Instant fromInstant, final Instant toInstant) {
        fail();
        return null;
      }

    }));
    return bean;
  }


  private DependencyGraphTraceBuilder createBuilder() {
    return new DependencyGraphTraceBuilder(createContextBean());
  }
  
  public void testSetValuationTime() {
    final DependencyGraphTraceBuilder builder = createBuilder();
    final Instant i1 = builder.getValuationTime();
    Instant instant = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]").toInstant();
    final DependencyGraphTraceBuilder prime = builder.valuationTime(instant);
    final Instant i2 = prime.getValuationTime();
    assertEquals(i1, builder.getValuationTime()); // original unchanged
    assertFalse(Objects.equals(i1, i2));
  }

  // TODO: testSetResolutionTime method

  public void testSetCalculationConfigurationName() {
    final DependencyGraphTraceBuilder builder = createBuilder();
    final String c1 = builder.getCalculationConfigurationName();
    final DependencyGraphTraceBuilder prime = builder.calculationConfigurationName("Foo");
    final String c2 = prime.getCalculationConfigurationName();
    assertEquals(c1, builder.getCalculationConfigurationName()); // original unchanged
    assertFalse(c1.equals(c2));
  }

  public void testSetDefaultProperties() {
    ValueProperties valueProperties = ValueProperties.parse("A=[foo,bar],B=*");
    final DependencyGraphTraceBuilder builder = createBuilder();
    final ValueProperties p1 = builder.getDefaultProperties();
    final DependencyGraphTraceBuilder prime = builder.defaultProperties(valueProperties);
    final ValueProperties p2 = prime.getDefaultProperties();
    assertEquals(p1, builder.getDefaultProperties()); // original unchanged
    assertFalse(p1.equals(p2));
  }

  public void testAddValue() {
    
    final ComputationTargetSpecification target = ComputationTargetSpecification.of(UniqueId.of("Scheme", "PrimitiveValue"));
    final ValueRequirement vr1 = new ValueRequirement("Value1", target);
    final ValueRequirement vr2 = new ValueRequirement("Value2", target);

    final DependencyGraphTraceBuilder builder = createBuilder();
    final Collection<ValueRequirement> r1 = builder.getRequirements();
    final DependencyGraphTraceBuilder prime = builder.addRequirement(vr1);
    final Collection<ValueRequirement> r2 = prime.getRequirements();
    final DependencyGraphTraceBuilder prime2 = prime.addRequirement(vr2);
    final Collection<ValueRequirement> r3 = prime2.getRequirements();
    assertEquals(r1, builder.getRequirements()); // original unchanged
    assertEquals(r2, prime.getRequirements()); // unchanged
    assertEquals(r1.size(), 0);
    assertEquals(r2.size(), 1);
    assertEquals(r3.size(), 2);
  }

  public void testBuild_ok() {
    
    DependencyGraphTraceBuilder builder = createBuilder();
    
    ComputationTargetRequirement ct1 = new ComputationTargetRequirement(ComputationTargetType.parse("PRIMITIVE"), ExternalId.parse("Foo~1"));
    ValueRequirement req1 = parseValueRequirement(MarketDataRequirementNames.MARKET_VALUE, ct1);
    ComputationTargetRequirement ct2 = new ComputationTargetRequirement(ComputationTargetType.parse("PRIMITIVE"), ExternalId.parse("Foo~2"));
    ValueRequirement req2 = parseValueRequirement(MarketDataRequirementNames.MARKET_VALUE, ct2);
    DependencyGraphBuildTrace obj = builder.addRequirement(req1).addRequirement(req2).build();
    
    assertNotNull(obj.getDependencyGraph());
    assertTrue(obj.getExceptionsWithCounts().isEmpty());
    assertTrue(obj.getFailures().isEmpty());
  }

  public void testBuild_exceptions() {
    DependencyGraphTraceBuilder builder = createBuilder();
    
    ComputationTargetRequirement ct1 = new ComputationTargetRequirement(ComputationTargetType.parse("PRIMITIVE"), ExternalId.parse("Foo~1"));
    ValueRequirement req1 = parseValueRequirement(MarketDataRequirementNames.MARKET_VALUE, ct1);
    ComputationTargetSpecification ct2 = new ComputationTargetSpecification(ComputationTargetType.parse("PRIMITIVE"), UniqueId.parse("Foo~Bar"));
    ValueRequirement req2 = parseValueRequirement(ValueRequirementNames.FAIR_VALUE, ct2);
    DependencyGraphBuildTrace obj = builder.addRequirement(req1).addRequirement(req2).build();
    
    assertNotNull(obj.getDependencyGraph());
    assertEquals(2, obj.getExceptionsWithCounts().size());
    assertEquals(1, obj.getFailures().size());
  }

  public void testBuild_failures() {
    DependencyGraphTraceBuilder builder = createBuilder();
    
    ComputationTargetRequirement ct1 = new ComputationTargetRequirement(ComputationTargetType.parse("PRIMITIVE"), ExternalId.parse("Bar~1"));
    ValueRequirement req1 = parseValueRequirement(MarketDataRequirementNames.MARKET_VALUE, ct1);
    ComputationTargetSpecification ct2 = new ComputationTargetSpecification(ComputationTargetType.parse("PRIMITIVE"), UniqueId.parse("Bar~2"));
    ValueRequirement req2 = parseValueRequirement(ValueRequirementNames.PRESENT_VALUE, ct2);
    DependencyGraphBuildTrace obj = builder.addRequirement(req1).addRequirement(req2).build();
    
    assertNotNull(obj.getDependencyGraph());
    assertEquals(2, obj.getExceptionsWithCounts().size());
    assertEquals(2, obj.getFailures().size());
  }

  
  private ValueRequirement parseValueRequirement(final String valueName, final ComputationTargetReference target) {
    final String name;
    final ValueProperties constraints;
    final int i = valueName.indexOf('{');
    if ((i > 0) && (valueName.charAt(valueName.length() - 1) == '}')) {
      name = valueName.substring(0, i);
      constraints = ValueProperties.parse(valueName.substring(i));
    } else {
      name = valueName;
      constraints = ValueProperties.none();
    }
    return new ValueRequirement(name, target, constraints);
  }

  
}
