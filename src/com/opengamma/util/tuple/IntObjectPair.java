/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 * 
 * Please see distribution for license.
 */
package com.opengamma.util.tuple;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import org.apache.http.util.LangUtils;

/**
 * An immutable pair consisting of an {@code int} and {@code double}.
 * <p>
 * The class provides direct access to the primitive types and implements
 * the relevant fastutil interface.
 *
 * @param <T> The entity type for the Second side of the underlying Pair.
 */
public class IntObjectPair<T> extends Pair<Integer, T> implements Int2ObjectMap.Entry<T> {

  /** The first element. */
  public final int first;  // CSIGNORE
  /** The second element. */
  public final T second;  // CSIGNORE

  /**
   * Constructor.
   * @param first  the first element
   * @param second  the second element
   */
  public IntObjectPair(final int first, final T second) {
    this.first = first;
    this.second = second;
  }

  //-------------------------------------------------------------------------
  @Override
  public Integer getFirst() {
    return first;
  }

  @Override
  public T getSecond() {
    return second;
  }

  public int getFirstInt() {
    return first;
  }

  public T getSecondObject() {
    return second;
  }

  //-------------------------------------------------------------------------
  @Override
  public int getIntKey() {
    return first;
  }

  @Override
  public T setValue(final T value) {
    throw new UnsupportedOperationException("Immutable");
  }

  //-------------------------------------------------------------------------
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof IntObjectPair) {
      final IntObjectPair<T> other = (IntObjectPair<T>) obj;
      return this.first == other.first && LangUtils.equals(this.second, other.second);
    }
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    // see Map.Entry API specification
    return first ^ second.hashCode();
  }

}
