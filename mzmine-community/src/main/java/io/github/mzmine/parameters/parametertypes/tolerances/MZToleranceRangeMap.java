/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.parameters.parametertypes.tolerances;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * This range map provides options to add values that are mapped by the mztolerance
 *
 * @param <T> the mapped value
 */
@SuppressWarnings("UnstableApiUsage")
public class MZToleranceRangeMap<T> implements RangeMap<Double, T> {

  private final RangeMap<Double, T> map = TreeRangeMap.create();
  private final MZTolerance mzTolerance;

  public MZToleranceRangeMap(MZTolerance mzTolerance) {
    this.mzTolerance = mzTolerance;
  }

  public MZTolerance getMzTolerance() {
    return mzTolerance;
  }

  /**
   * Add new mapping
   *
   * @param mzCenter the key
   * @param value    the new value
   * @return the input value
   */
  public T put(double mzCenter, T value) {
    map.put(mzTolerance.getToleranceRange(mzCenter), value);
    return value;
  }

  /**
   * Put if there is no range that contains mzCenter
   *
   * @param mzCenter the key
   * @param value    the value to put if not already mapped
   * @return the mapped value either the old or the new
   */
  public T putIfAbsent(double mzCenter, T value) {
    T v = get(mzCenter);
    if (v == null) {
      v = put(mzCenter, value);
    }
    return v;
  }

  /**
   * Either return the previously mapped value or generate a new one that is being mapped
   *
   * @param mzCenter        key value to be checked against the range map
   * @param mappingFunction map a new value to the key
   * @return the old value mapped to the key or the generated new value
   */
  public T computeIfAbsent(double mzCenter, Function<? super Double, ? extends T> mappingFunction) {
    Objects.requireNonNull(mappingFunction);
    T v;
    if ((v = get(mzCenter)) == null) {
      T newValue;
      if ((newValue = mappingFunction.apply(mzCenter)) != null) {
        put(mzCenter, newValue);
        return newValue;
      }
    }

    return v;
  }


  @Override
  public T get(Double key) {
    return map.get(key);
  }

  @Override
  public Map.Entry<Range<Double>, T> getEntry(Double key) {
    return map.getEntry(key);
  }

  @Override
  public Range<Double> span() {
    return map.span();
  }

  @Override
  public void put(Range<Double> range, T value) {
    map.put(range, value);
  }

  @Override
  public void putCoalescing(Range<Double> range, T value) {
    map.putCoalescing(range, value);
  }

  @Override
  public void putAll(RangeMap<Double, T> rangeMap) {
    map.putAll(rangeMap);
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public void remove(Range<Double> range) {
    map.remove(range);
  }

  @Override
  public void merge(Range<Double> range, T value,
      BiFunction<? super T, ? super T, ? extends T> remappingFunction) {
    map.merge(range, value, remappingFunction);
  }

  @Override
  public Map<Range<Double>, T> asMapOfRanges() {
    return map.asMapOfRanges();
  }

  @Override
  public Map<Range<Double>, T> asDescendingMapOfRanges() {
    return map.asDescendingMapOfRanges();
  }

  @Override
  public RangeMap<Double, T> subRangeMap(Range<Double> range) {
    return map.subRangeMap(range);
  }
}
