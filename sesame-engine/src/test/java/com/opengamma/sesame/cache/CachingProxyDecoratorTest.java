/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.cache;

import static com.opengamma.sesame.config.ConfigBuilder.argument;
import static com.opengamma.sesame.config.ConfigBuilder.arguments;
import static com.opengamma.sesame.config.ConfigBuilder.config;
import static com.opengamma.sesame.config.ConfigBuilder.function;
import static com.opengamma.sesame.config.ConfigBuilder.implementations;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import org.testng.annotations.Test;
import org.threeten.bp.ZonedDateTime;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.opengamma.id.ExternalIdBundle;
import com.opengamma.sesame.EngineTestUtils;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.SimpleEnvironment;
import com.opengamma.sesame.config.EngineUtils;
import com.opengamma.sesame.config.FunctionModelConfig;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.sesame.function.FunctionMetadata;
import com.opengamma.sesame.function.Output;
import com.opengamma.sesame.function.scenarios.AbstractScenarioArgument;
import com.opengamma.sesame.function.scenarios.FilteredScenarioDefinition;
import com.opengamma.sesame.function.scenarios.ScenarioFunction;
import com.opengamma.sesame.graph.FunctionBuilder;
import com.opengamma.sesame.graph.FunctionModel;
import com.opengamma.sesame.marketdata.FieldName;
import com.opengamma.sesame.marketdata.MarketDataSource;
import com.opengamma.util.result.Result;
import com.opengamma.util.test.TestGroup;

@SuppressWarnings("unchecked")
@Test(groups = TestGroup.UNIT)
public class CachingProxyDecoratorTest {

  private static final Set<Class<?>> NO_COMPONENTS = ComponentMap.EMPTY.getComponentTypes();

  private final CacheProvider _cacheProvider = EngineTestUtils.createCacheProvider();

  /** check the cache contains the item returns from the function */
  @Test
  public void oneLookup() throws Exception {
    FunctionModelConfig config = config(implementations(TestFn.class, Impl.class),
                                        arguments(function(Impl.class, argument("s", "s"))));
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheProvider);
    FunctionMetadata metadata = EngineUtils.createMetadata(TestFn.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata, config, NO_COMPONENTS, cachingDecorator);
    TestFn fn = (TestFn) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    Method foo = EngineUtils.getMethod(TestFn.class, "foo");
    CachingProxyDecorator.Handler invocationHandler = (CachingProxyDecorator.Handler) Proxy.getInvocationHandler(fn);
    Impl delegate = (Impl) invocationHandler.getDelegate();
    MethodInvocationKey key = new MethodInvocationKey(delegate, foo, new Object[]{"bar"});

    Object results = fn.foo("bar");
    Object value = _cacheProvider.get().getIfPresent(key);
    assertNotNull(value);
    assertSame(value, results);
  }

  /** check that multiple instances of the same function return the cached value when invoked with the same args */
  @Test
  public void multipleFunctions() {
    FunctionModelConfig config = config(implementations(TestFn.class, Impl.class),
                                        arguments(function(Impl.class, argument("s", "s"))));
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheProvider);
    FunctionMetadata metadata = EngineUtils.createMetadata(TestFn.class, "foo");
    FunctionBuilder functionBuilder = new FunctionBuilder();

    FunctionModel functionModel1 = FunctionModel.forFunction(metadata, config, NO_COMPONENTS, cachingDecorator);
    TestFn fn1 = (TestFn) functionModel1.build(functionBuilder, ComponentMap.EMPTY).getReceiver();

    FunctionModel functionModel2 = FunctionModel.forFunction(metadata, config, NO_COMPONENTS, cachingDecorator);
    TestFn fn2 = (TestFn) functionModel2.build(functionBuilder, ComponentMap.EMPTY).getReceiver();

    assertSame(fn1.foo("bar"), fn2.foo("bar"));
  }

  /**
   * check that multiple identical calls produce the same value even if the underlying function doesn't.
   * this isn't how functions are supposed to work but it demonstrates a point for testing
   */
  @Test
  public void multipleCalls() {
    FunctionModelConfig config = config(implementations(TestFn.class, Impl.class),
                                        arguments(function(Impl.class, argument("s", "s"))));
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheProvider);
    FunctionMetadata metadata = EngineUtils.createMetadata(TestFn.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata, config, NO_COMPONENTS, cachingDecorator);
    TestFn fn = (TestFn) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    assertSame(fn.foo("bar"), fn.foo("bar"));
  }

  @Test
  public void sameFunctionDifferentConstructorArgs() {
    FunctionModelConfig config1 = config(implementations(TestFn.class, Impl.class),
                                         arguments(function(Impl.class, argument("s", "a string"))));
    FunctionModelConfig config2 = config(implementations(TestFn.class, Impl.class),
                                         arguments(function(Impl.class, argument("s", "a different string"))));
    FunctionMetadata metadata = EngineUtils.createMetadata(TestFn.class, "foo");
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheProvider);

    FunctionBuilder functionBuilder = new FunctionBuilder();
    FunctionModel functionModel1 = FunctionModel.forFunction(metadata, config1, NO_COMPONENTS, cachingDecorator);
    TestFn fn1 = (TestFn) functionModel1.build(functionBuilder, ComponentMap.EMPTY).getReceiver();
    FunctionModel functionModel2 = FunctionModel.forFunction(metadata, config2, NO_COMPONENTS, cachingDecorator);
    TestFn fn2 = (TestFn) functionModel2.build(functionBuilder, ComponentMap.EMPTY).getReceiver();

    Object val1 = fn1.foo("bar");
    Object val2 = fn2.foo("bar");
    assertTrue(val1 != val2);
  }

  interface TestFn {

    @Cacheable
    @Output("Foo")
    Object foo(String arg);
  }

  public static class Impl implements TestFn {

    private final String _s;

    public Impl(String s) {
      _s = s;
    }

    @Override
    public Object foo(String arg) {
      return _s + new Object();
    }
  }

  /* package */ interface TopLevelFn {

    @Output("topLevel")
    Object fn();
  }

  public static class TopLevel implements TopLevelFn {

    private final DelegateFn _delegateFn;

    public TopLevel(DelegateFn delegateFn) {
      _delegateFn = delegateFn;
    }

    @Override
    @Cacheable
    public Object fn() {
      return _delegateFn.fn();
    }
  }

  /* package */ interface DelegateFn {

    Object fn();
  }

  public static class Delegate1 implements DelegateFn {

    private final String _s;

    public Delegate1(String s) {
      _s = s;
    }

    @Override
    public Object fn() {
      return _s + new Object();
    }
  }

  public static class Delegate2 implements DelegateFn {

    private final String _s;

    public Delegate2(String s) {
      _s = s;
    }

    @Override
    public Object fn() {
      return _s + new Object();
    }
  }

  /**
   * 2 functions where the top level function is the same and the dependency functions are the same implementation
   * type but have different constructor args.
   */
  @Test
  public void sameFunctionDifferentDependencyInstances() {
    FunctionModelConfig config1 = config(implementations(TopLevelFn.class, TopLevel.class,
                                                         DelegateFn.class, Delegate1.class),
                                         arguments(function(Delegate1.class, argument("s", "a string"))));
    FunctionModelConfig config2 = config(implementations(TopLevelFn.class, TopLevel.class,
                                                         DelegateFn.class, Delegate1.class),
                                         arguments(function(Delegate1.class, argument("s", "a different string"))));
    FunctionMetadata metadata = EngineUtils.createMetadata(TopLevelFn.class, "fn");
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheProvider);

    FunctionBuilder functionBuilder = new FunctionBuilder();
    FunctionModel functionModel1 = FunctionModel.forFunction(metadata, config1, NO_COMPONENTS, cachingDecorator);
    TopLevelFn fn1 = (TopLevelFn) functionModel1.build(functionBuilder, ComponentMap.EMPTY).getReceiver();
    FunctionModel functionModel2 = FunctionModel.forFunction(metadata, config2, NO_COMPONENTS, cachingDecorator);
    TopLevelFn fn2 = (TopLevelFn) functionModel2.build(functionBuilder, ComponentMap.EMPTY).getReceiver();

    Object val1 = fn1.fn();
    Object val2 = fn2.fn();
    assertTrue(val1 != val2);
  }

  /**
   * 2 functions where the top level function is the same and the dependency functions implement the same interface
   * but are instances of different classes.
   */
  @Test
  public void sameFunctionDifferentDependencyTypes() {
    FunctionModelConfig config1 = config(implementations(TopLevelFn.class, TopLevel.class,
                                                         DelegateFn.class, Delegate1.class),
                                         arguments(function(Delegate1.class, argument("s", "a string"))));
    FunctionModelConfig config2 = config(implementations(TopLevelFn.class, TopLevel.class,
                                                         DelegateFn.class, Delegate2.class),
                                         arguments(function(Delegate2.class, argument("s", "a string"))));
    FunctionMetadata metadata = EngineUtils.createMetadata(TopLevelFn.class, "fn");
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheProvider);

    FunctionBuilder functionBuilder = new FunctionBuilder();
    FunctionModel functionModel1 = FunctionModel.forFunction(metadata, config1, NO_COMPONENTS, cachingDecorator);
    TopLevelFn fn1 = (TopLevelFn) functionModel1.build(functionBuilder, ComponentMap.EMPTY).getReceiver();
    FunctionModel functionModel2 = FunctionModel.forFunction(metadata, config2, NO_COMPONENTS, cachingDecorator);
    TopLevelFn fn2 = (TopLevelFn) functionModel2.build(functionBuilder, ComponentMap.EMPTY).getReceiver();

    Object val1 = fn1.fn();
    Object val2 = fn2.fn();
    assertTrue(val1 != val2);
  }

  /** check caching works when the class method is annotated and the interface isn't */
  @Test
  public void annotationOnClass() throws Exception {
    FunctionModelConfig config = config(implementations(TestFn2.class, Impl2.class));
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheProvider);
    FunctionMetadata metadata = EngineUtils.createMetadata(TestFn2.class, "foo");
    FunctionModel functionModel = FunctionModel.forFunction(metadata, config, NO_COMPONENTS, cachingDecorator);
    TestFn2 fn = (TestFn2) functionModel.build(new FunctionBuilder(), ComponentMap.EMPTY).getReceiver();
    Method foo = EngineUtils.getMethod(TestFn2.class, "foo");
    CachingProxyDecorator.Handler invocationHandler = (CachingProxyDecorator.Handler) Proxy.getInvocationHandler(fn);
    Impl2 delegate = (Impl2) invocationHandler.getDelegate();
    MethodInvocationKey key = new MethodInvocationKey(delegate, foo, new Object[]{"bar"});

    Object results = fn.foo("bar");
    Object value = _cacheProvider.get().getIfPresent(key);
    assertNotNull(value);
    assertSame(value, results);
  }

  interface TestFn2 {

    @Output("Foo")
    Object foo(String arg);
  }

  public static class Impl2 implements TestFn2 {

    @Cacheable
    @Override
    public Object foo(String arg) {
      return new Object();
    }
  }

  /** Check the expected cache keys are pushed onto a thread local stack while a cacheable method executes. */
  @Test
  public void executingMethods() {
    FunctionModelConfig config = config(implementations(ExecutingMethodsI1.class, ExecutingMethodsC1.class,
                                                        ExecutingMethodsI2.class, ExecutingMethodsC2.class));
    ExecutingMethodsThreadLocal executingMethods = new ExecutingMethodsThreadLocal();
    ComponentMap components = ComponentMap.of(ImmutableMap.<Class<?>, Object>of(ExecutingMethodsThreadLocal.class,
                                                                                executingMethods));
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheProvider, executingMethods);
    ExecutingMethodsI1 i1 = FunctionModel.build(ExecutingMethodsI1.class, config, components, cachingDecorator);
    i1.fn("s", 1);
  }

  interface ExecutingMethodsI1 {

    @Output("abc")
    @Cacheable
    Object fn(String s, int i);
  }

  public static class ExecutingMethodsC1 implements ExecutingMethodsI1 {

    private final ExecutingMethodsI2 _i2;
    private final ExecutingMethodsThreadLocal _executingMethods;

    public ExecutingMethodsC1(ExecutingMethodsI2 i2, ExecutingMethodsThreadLocal executingMethods) {
      _i2 = i2;
      // this is a bit grubby but necessary so the method keys can be checked
      CachingProxyDecorator.Handler handler = (CachingProxyDecorator.Handler) Proxy.getInvocationHandler(i2);
      ExecutingMethodsC2 c2 = (ExecutingMethodsC2) handler.getDelegate();
      c2._c1 = this;
      _executingMethods = executingMethods;
    }

    @Override
    public Object fn(String s, int i) {
      Method fn = EngineUtils.getMethod(ExecutingMethodsI1.class, "fn");
      MethodInvocationKey key = new MethodInvocationKey(this, fn, new Object[]{s, i});
      LinkedList<MethodInvocationKey> expected = Lists.newLinkedList();
      expected.add(key);
      assertEquals(expected, _executingMethods.get());
      Object retVal = _i2.fn(s, i, s + i);
      assertEquals(expected, _executingMethods.get());
      return retVal;
    }
  }

  interface ExecutingMethodsI2 {

    @Cacheable
    Object fn(String s, int i, String s2);
  }

  public static class ExecutingMethodsC2 implements ExecutingMethodsI2 {

    private final ExecutingMethodsThreadLocal _executingMethods;

    private ExecutingMethodsC1 _c1;

    public ExecutingMethodsC2(ExecutingMethodsThreadLocal executingMethods) {
      _executingMethods = executingMethods;
    }

    @Override
    public Object fn(String s, int i, String s2) {
      Method fn1 = EngineUtils.getMethod(ExecutingMethodsI1.class, "fn");
      MethodInvocationKey key1 = new MethodInvocationKey(_c1, fn1, new Object[]{s, i});
      Method fn2 = EngineUtils.getMethod(ExecutingMethodsI2.class, "fn");
      MethodInvocationKey key2 = new MethodInvocationKey(this, fn2, new Object[]{s, i, s2});
      LinkedList<MethodInvocationKey> expected = Lists.newLinkedList();
      expected.add(key2);
      expected.add(key1);
      assertEquals(expected, _executingMethods.get());
      return "not used";
    }
  }

  /**
   * check that scenario arguments in the environment are only included the cache key for functions whose return value
   * can be affected by the scenario.
   */
  @Test
  public void pruneScenarioArguments() throws ExecutionException, InterruptedException {
    FunctionModelConfig config = config(implementations(Fn1.class, ScenarioImpl1.class,
                                                        Fn2.class, ScenarioImpl2.class));
    CachingProxyDecorator cachingDecorator = new CachingProxyDecorator(_cacheProvider);
    Fn1 i1 = FunctionModel.build(Fn1.class, config, ComponentMap.EMPTY, cachingDecorator);
    ZonedDateTime valuationTime = ZonedDateTime.now();
    MarketDataSource marketDataSource = new MarketDataSource() {
      @Override
      public Result<?> get(ExternalIdBundle id, FieldName fieldName) {
        throw new UnsupportedOperationException("get not implemented");
      }
    };
    // for calls to ScenarioArgumentsC1 the args for ScenarioArgumentsC2 will be included in the key
    // because ScenarioArgumentsC1 calls ScenarioArgumentsC2
    FilteredScenarioDefinition scenarioDef1 = new FilteredScenarioDefinition(new Args1(), new Args2());
    // for calls to ScenarioArgumentsC2 the args for ScenarioArgumentsC1 will be filtered out because
    // ScenarioArgumentsC2 doesn't call ScenarioArgumentsC1 and therefore its scenario arguments can't affect
    // any values calculated by ScenarioArgumentsC2
    FilteredScenarioDefinition scenarioDef2 = new FilteredScenarioDefinition(new Args2());

    // env1 is passed to the functions. it contains scenario arguments for all classes
    SimpleEnvironment env1 = new SimpleEnvironment(valuationTime, marketDataSource, scenarioDef1);
    // env2 is the environment that should be passed to ScenarioArgumentsC2 - its scenario arguments have been
    // filtered to only include the ones applicable to ScenarioArgumentsC2 and its dependencies
    SimpleEnvironment env2 = new SimpleEnvironment(valuationTime, marketDataSource, scenarioDef2);
    i1.fn(env1, "s1", 1);
    i1.fn(env1, "s2", 2);

    ScenarioImpl1 c1 = (ScenarioImpl1) EngineUtils.getProxiedObject(i1);
    ScenarioImpl2 c2 = (ScenarioImpl2) EngineUtils.getProxiedObject(c1._fn2);
    Method method1 = EngineUtils.getMethod(Fn1.class, "fn");
    Method method2 = EngineUtils.getMethod(Fn2.class, "fn");

    checkValueIsInCache(env1, "s1", 1, c1, method1, "S1 1");
    checkValueIsInCache(env1, "s2", 2, c1, method1, "S2 2");
    checkValueIsInCache(env2, "s1", 1, c2, method2, "s1 1");
    checkValueIsInCache(env2, "s2", 2, c2, method2, "s2 2");
  }

  /**
   * Checks a value is in the cache after a call to a cacheable method.
   * <p>
   * The arguments, the receiver and the method are used to build a key which is used to look up the cached value.
   *
   * @param env  the environment argument to the method
   * @param stringArg  the string argument to the method
   * @param intArg  the int argument to the method
   * @param receiver  the receiver of the method call
   * @param method  the method called
   * @param expectedValue  the value that should be in the cache
   */
  private void checkValueIsInCache(Environment env,
                                   String stringArg,
                                   int intArg,
                                   Object receiver,
                                   Method method,
                                   String expectedValue) throws InterruptedException, ExecutionException {
    MethodInvocationKey key = new MethodInvocationKey(receiver, method, new Object[]{env, stringArg, intArg});
    Object value = _cacheProvider.get().getIfPresent(key);
    assertNotNull(value);
    assertEquals(expectedValue, value);
  }

  interface Fn1 {

    @Cacheable
    Object fn(Environment env, String s, Integer i);
  }

  public static class ScenarioImpl1 implements Fn1, ScenarioFunction<Args1, ScenarioImpl1> {

    private final Fn2 _fn2;

    public ScenarioImpl1(Fn2 fn2) {
      _fn2 = fn2;
    }

    @Override
    public Object fn(Environment env, String s, Integer i) {
      return _fn2.fn(env, s, i).toUpperCase();
    }

    @Nullable
    @Override
    public Class<Args1> getArgumentType() {
      return Args1.class;
    }
  }

  interface Fn2 {

    @Cacheable
    String fn(Environment env, String s, Integer i);
  }

  public static class ScenarioImpl2 implements Fn2, ScenarioFunction<Args2, ScenarioImpl2> {

    @Override
    public String fn(Environment env, String s, Integer i) {
      return s + " " + i;
    }

    @Nullable
    @Override
    public Class<Args2> getArgumentType() {
      return Args2.class;
    }
  }

  public static class Args1 extends AbstractScenarioArgument<Args1, ScenarioImpl1> {

    private Args1() {
      super(ScenarioImpl1.class);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Args1;
    }

    @Override
    public int hashCode() {
      return 1;
    }
  }

  public static class Args2 extends AbstractScenarioArgument<Args2, ScenarioImpl2> {

    private Args2() {
      super(ScenarioImpl2.class);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Args2;
    }

    @Override
    public int hashCode() {
      return 2;
    }
  }
}

