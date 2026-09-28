/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.SimpleRange;
import io.github.mzmine.datamodel.SimpleRange.SimpleDoubleRange;
import io.github.mzmine.datamodel.SimpleRange.SimpleFloatRange;
import io.github.mzmine.datamodel.SimpleRange.SimpleIntegerRange;
import io.github.mzmine.util.RangeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SimpleRange} and its three implementations. Covers the factory methods,
 * bounds, {@code contains}, {@code length} and the conversion to a guava {@link Range}.
 * <p>
 * A separate block of tests pins down whether {@code -Double.MAX_VALUE}/{@code Double.MAX_VALUE}
 * (and the float equivalents) are acceptable substitutes for {@link Range#all()}, which
 * {@link SimpleRange} cannot represent because it is always a closed range.
 */
public class SimpleRangeTest {

  // -------------------------------------------------------------------------------------------
  // factories
  // -------------------------------------------------------------------------------------------

  @Test
  public void testOfPrimitives() {
    Assertions.assertEquals(new SimpleIntegerRange(1, 5), SimpleRange.of(1, 5));
    Assertions.assertEquals(new SimpleIntegerRange(1, 5), SimpleRange.ofInteger(1, 5));
    Assertions.assertEquals(new SimpleDoubleRange(1.0, 5.0), SimpleRange.of(1.0, 5.0));
    Assertions.assertEquals(new SimpleDoubleRange(1.0, 5.0), SimpleRange.ofDouble(1.0, 5.0));
    Assertions.assertEquals(new SimpleFloatRange(1f, 5f), SimpleRange.of(1f, 5f));
    Assertions.assertEquals(new SimpleFloatRange(1f, 5f), SimpleRange.ofFloat(1f, 5f));
  }

  /**
   * The float overloads must not be swallowed by the double ones.
   */
  @Test
  public void testOfPrimitivesOverloadResolution() {
    Assertions.assertInstanceOf(SimpleFloatRange.class, SimpleRange.of(1f, 5f));
    Assertions.assertInstanceOf(SimpleDoubleRange.class, SimpleRange.of(1.0, 5.0));
    Assertions.assertInstanceOf(SimpleIntegerRange.class, SimpleRange.of(1, 5));
  }

  @Test
  public void testOfGuavaClosedRange() {
    Assertions.assertEquals(new SimpleDoubleRange(1.0, 5.0),
        SimpleRange.ofDouble(Range.closed(1.0, 5.0)));
    Assertions.assertEquals(new SimpleDoubleRange(1.0, 5.0),
        SimpleDoubleRange.of(Range.closed(1.0, 5.0)));
    Assertions.assertEquals(new SimpleIntegerRange(1, 5),
        SimpleRange.ofInteger(Range.closed(1, 5)));
    Assertions.assertEquals(new SimpleIntegerRange(1, 5),
        SimpleIntegerRange.of(Range.closed(1, 5)));
    Assertions.assertEquals(new SimpleFloatRange(1f, 5f),
        SimpleRange.ofFloat(Range.closed(1f, 5f)));
    Assertions.assertEquals(new SimpleFloatRange(1f, 5f),
        SimpleFloatRange.of(Range.closed(1f, 5f)));
  }

  @Test
  public void testOfNullReturnsNull() {
    Assertions.assertNull(SimpleRange.ofDouble(null));
    Assertions.assertNull(SimpleRange.ofFloat(null));
    Assertions.assertNull(SimpleRange.ofInteger(null));
  }

  @Test
  public void testGuavaOrNull() {
    Assertions.assertNull(SimpleRange.guavaOrNull(null));
    Assertions.assertEquals(Range.closed(1.0, 5.0),
        SimpleRange.guavaOrNull(new SimpleDoubleRange(1.0, 5.0)));
    Assertions.assertEquals(Range.closed(1f, 5f),
        SimpleRange.guavaOrNull(new SimpleFloatRange(1f, 5f)));
    Assertions.assertEquals(Range.closed(1, 5),
        SimpleRange.guavaOrNull(new SimpleIntegerRange(1, 5)));
  }

  // -------------------------------------------------------------------------------------------
  // bounds, length, contains, guava
  // -------------------------------------------------------------------------------------------

  @Test
  public void testBoundsAndLength() {
    Assertions.assertEquals(1, new SimpleIntegerRange(1, 5).lowerBound());
    Assertions.assertEquals(5, new SimpleIntegerRange(1, 5).upperBound());
    Assertions.assertEquals(4, new SimpleIntegerRange(1, 5).length());

    Assertions.assertEquals(1.0, new SimpleDoubleRange(1.0, 5.0).lowerBound());
    Assertions.assertEquals(5.0, new SimpleDoubleRange(1.0, 5.0).upperBound());
    Assertions.assertEquals(4.0, new SimpleDoubleRange(1.0, 5.0).length());

    Assertions.assertEquals(1f, new SimpleFloatRange(1f, 5f).lowerBound());
    Assertions.assertEquals(5f, new SimpleFloatRange(1f, 5f).upperBound());
    Assertions.assertEquals(4f, new SimpleFloatRange(1f, 5f).length());
  }

  @Test
  public void testSingletonRange() {
    final SimpleDoubleRange single = new SimpleDoubleRange(3.0, 3.0);
    Assertions.assertEquals(0.0, single.length());
    Assertions.assertTrue(single.contains(3.0));
    Assertions.assertFalse(single.contains(Math.nextUp(3.0)));
    Assertions.assertFalse(single.contains(Math.nextDown(3.0)));
  }

  @Test
  public void testContainsIsInclusiveOnBothBounds() {
    final SimpleIntegerRange ints = new SimpleIntegerRange(1, 5);
    Assertions.assertTrue(ints.contains(1));
    Assertions.assertTrue(ints.contains(5));
    Assertions.assertFalse(ints.contains(0));
    Assertions.assertFalse(ints.contains(6));

    final SimpleDoubleRange doubles = new SimpleDoubleRange(1.0, 5.0);
    Assertions.assertTrue(doubles.contains(1.0));
    Assertions.assertTrue(doubles.contains(5.0));
    Assertions.assertFalse(doubles.contains(Math.nextDown(1.0)));
    Assertions.assertFalse(doubles.contains(Math.nextUp(5.0)));

    final SimpleFloatRange floats = new SimpleFloatRange(1f, 5f);
    Assertions.assertTrue(floats.contains(1f));
    Assertions.assertTrue(floats.contains(5f));
    Assertions.assertFalse(floats.contains(Math.nextDown(1f)));
    Assertions.assertFalse(floats.contains(Math.nextUp(5f)));
  }

  @Test
  public void testContainsNaN() {
    Assertions.assertFalse(new SimpleDoubleRange(1.0, 5.0).contains(Double.NaN));
    Assertions.assertFalse(new SimpleFloatRange(1f, 5f).contains(Float.NaN));
  }

  @Test
  public void testGuavaIsAlwaysClosed() {
    Assertions.assertEquals(Range.closed(1.0, 5.0), new SimpleDoubleRange(1.0, 5.0).guava());
    Assertions.assertEquals(Range.closed(1f, 5f), new SimpleFloatRange(1f, 5f).guava());
    Assertions.assertEquals(Range.closed(1, 5), new SimpleIntegerRange(1, 5).guava());
  }

  @Test
  public void testRoundTripClosedRange() {
    final Range<Double> doubles = Range.closed(1.0, 5.0);
    Assertions.assertEquals(doubles, SimpleRange.ofDouble(doubles).guava());

    final Range<Integer> ints = Range.closed(1, 5);
    Assertions.assertEquals(ints, SimpleRange.ofInteger(ints).guava());

    final Range<Float> floats = Range.closed(1f, 5f);
    Assertions.assertEquals(floats, SimpleRange.ofFloat(floats).guava());
  }

  // -------------------------------------------------------------------------------------------
  // Range.all() substitution: are the MAX_VALUE bounds good enough?
  // -------------------------------------------------------------------------------------------

  @Test
  public void testAllIsMappedToMaxValueBounds() {
    Assertions.assertEquals(new SimpleDoubleRange(-Double.MAX_VALUE, Double.MAX_VALUE),
        SimpleRange.ofDouble(Range.all()));
    Assertions.assertEquals(new SimpleDoubleRange(-Double.MAX_VALUE, Double.MAX_VALUE),
        SimpleDoubleRange.of(Range.all()));
    Assertions.assertEquals(new SimpleFloatRange(-Float.MAX_VALUE, Float.MAX_VALUE),
        SimpleRange.ofFloat(Range.all()));
    Assertions.assertEquals(new SimpleFloatRange(-Float.MAX_VALUE, Float.MAX_VALUE),
        SimpleFloatRange.of(Range.all()));
    // integers have no infinities, so MIN_VALUE/MAX_VALUE is an exact substitute for the bounds
    Assertions.assertEquals(new SimpleIntegerRange(Integer.MIN_VALUE, Integer.MAX_VALUE),
        SimpleRange.ofInteger(Range.all()));
    Assertions.assertEquals(new SimpleIntegerRange(Integer.MIN_VALUE, Integer.MAX_VALUE),
        SimpleIntegerRange.of(Range.all()));
  }

  /**
   * OK: every finite value is contained, which is what callers of {@link Range#all()} usually
   * mean.
   */
  @Test
  public void testAllSubstituteContainsAllFiniteValues() {
    final SimpleDoubleRange doubles = SimpleRange.ofDouble(Range.all());
    Assertions.assertTrue(doubles.contains(-Double.MAX_VALUE));
    Assertions.assertTrue(doubles.contains(Double.MAX_VALUE));
    Assertions.assertTrue(doubles.contains(0d));
    Assertions.assertTrue(doubles.contains(-Double.MIN_VALUE));
    Assertions.assertTrue(doubles.contains(1e308));

    final SimpleFloatRange floats = SimpleRange.ofFloat(Range.all());
    Assertions.assertTrue(floats.contains(-Float.MAX_VALUE));
    Assertions.assertTrue(floats.contains(Float.MAX_VALUE));
    Assertions.assertTrue(floats.contains(0f));
    Assertions.assertTrue(floats.contains(-Float.MIN_VALUE));
    Assertions.assertTrue(floats.contains(1e38f));

    final SimpleIntegerRange ints = SimpleRange.ofInteger(Range.all());
    Assertions.assertTrue(ints.contains(Integer.MIN_VALUE));
    Assertions.assertTrue(ints.contains(Integer.MAX_VALUE));
    Assertions.assertTrue(ints.contains(0));
  }

  /**
   * NOT OK: {@link Range#all()} contains the infinities, the MAX_VALUE substitute does not. Only
   * relevant if a value can ever become infinite.
   */
  @Test
  public void testAllSubstituteDoesNotContainInfinity() {
    final SimpleDoubleRange doubles = SimpleRange.ofDouble(Range.all());
    Assertions.assertTrue(Range.<Double>all().contains(Double.NEGATIVE_INFINITY));
    Assertions.assertTrue(Range.<Double>all().contains(Double.POSITIVE_INFINITY));
    Assertions.assertFalse(doubles.contains(Double.NEGATIVE_INFINITY));
    Assertions.assertFalse(doubles.contains(Double.POSITIVE_INFINITY));

    final SimpleFloatRange floats = SimpleRange.ofFloat(Range.all());
    Assertions.assertTrue(Range.<Float>all().contains(Float.NEGATIVE_INFINITY));
    Assertions.assertTrue(Range.<Float>all().contains(Float.POSITIVE_INFINITY));
    Assertions.assertFalse(floats.contains(Float.NEGATIVE_INFINITY));
    Assertions.assertFalse(floats.contains(Float.POSITIVE_INFINITY));
  }

  /**
   * NOT OK: {@link Range#all()} contains NaN, the MAX_VALUE substitute does not. The substitute
   * agrees with its own {@link SimpleRange#guava()} closed range though, so the behaviour is at
   * least consistent once converted.
   */
  @Test
  public void testAllSubstituteDoesNotContainNaN() {
    final SimpleDoubleRange doubles = SimpleRange.ofDouble(Range.all());
    Assertions.assertTrue(Range.<Double>all().contains(Double.NaN));
    Assertions.assertFalse(doubles.contains(Double.NaN));
    Assertions.assertFalse(doubles.guava().contains(Double.NaN));

    final SimpleFloatRange floats = SimpleRange.ofFloat(Range.all());
    Assertions.assertTrue(Range.<Float>all().contains(Float.NaN));
    Assertions.assertFalse(floats.contains(Float.NaN));
    Assertions.assertFalse(floats.guava().contains(Float.NaN));
  }

  /**
   * OK: {@code upper - lower} would overflow for the substituted all-range, so {@code length()}
   * saturates at the MAX_VALUE of the type instead of wrapping around (int) or returning infinity
   * (float and double).
   */
  @Test
  public void testAllSubstituteLengthSaturatesAtMaxValue() {
    Assertions.assertEquals(Double.MAX_VALUE, SimpleRange.ofDouble(Range.all()).length());
    Assertions.assertEquals(Float.MAX_VALUE, SimpleRange.ofFloat(Range.all()).length());
    Assertions.assertEquals(Integer.MAX_VALUE, SimpleRange.ofInteger(Range.all()).length());

    // RangeUtils delegates, so it saturates the same way
    Assertions.assertEquals(Double.MAX_VALUE,
        RangeUtils.rangeLength(SimpleRange.ofDouble(Range.all())));
    Assertions.assertEquals(Float.MAX_VALUE,
        RangeUtils.rangeLength(SimpleRange.ofFloat(Range.all())));
    Assertions.assertEquals(Integer.MAX_VALUE,
        RangeUtils.rangeLength(SimpleRange.ofInteger(Range.all())));
  }

  /**
   * Saturation must only kick in on overflow, ordinary ranges keep the exact difference.
   */
  @Test
  public void testLengthDoesNotSaturateWithoutOverflow() {
    Assertions.assertEquals(Integer.MAX_VALUE - 1,
        new SimpleIntegerRange(0, Integer.MAX_VALUE - 1).length());
    Assertions.assertEquals(Integer.MAX_VALUE,
        new SimpleIntegerRange(0, Integer.MAX_VALUE).length());
    Assertions.assertEquals(Double.MAX_VALUE, new SimpleDoubleRange(0d, Double.MAX_VALUE).length());
    Assertions.assertEquals(Float.MAX_VALUE, new SimpleFloatRange(0f, Float.MAX_VALUE).length());
  }

  /**
   * An actually infinite bound is not an overflow, so its infinite length is kept.
   */
  @Test
  public void testLengthKeepsInfiniteBounds() {
    Assertions.assertEquals(Double.POSITIVE_INFINITY,
        new SimpleDoubleRange(0d, Double.POSITIVE_INFINITY).length());
    Assertions.assertEquals(Float.POSITIVE_INFINITY,
        new SimpleFloatRange(0f, Float.POSITIVE_INFINITY).length());
  }

  /**
   * NOT OK: the conversion is lossy. {@code ofDouble(Range.all()).guava()} is a closed range and no
   * longer equal to {@link Range#all()}.
   */
  @Test
  public void testAllSubstituteDoesNotRoundTrip() {
    Assertions.assertNotEquals(Range.<Double>all(), SimpleRange.ofDouble(Range.all()).guava());
    Assertions.assertNotEquals(Range.<Float>all(), SimpleRange.ofFloat(Range.all()).guava());
    Assertions.assertNotEquals(Range.<Integer>all(), SimpleRange.ofInteger(Range.all()).guava());

    Assertions.assertEquals(Range.closed(-Double.MAX_VALUE, Double.MAX_VALUE),
        SimpleRange.ofDouble(Range.all()).guava());
    Assertions.assertEquals(Range.closed(-Float.MAX_VALUE, Float.MAX_VALUE),
        SimpleRange.ofFloat(Range.all()).guava());
  }

  /**
   * OK: the substituted all-range is connected to every finite range, which is the behaviour
   * {@link Range#all()} would show.
   */
  @Test
  public void testAllSubstituteIsConnectedToEveryFiniteRange() {
    final SimpleDoubleRange allDoubles = SimpleRange.ofDouble(Range.all());
    Assertions.assertTrue(allDoubles.isConnected(new SimpleDoubleRange(1.0, 5.0)));
    Assertions.assertTrue(new SimpleDoubleRange(1.0, 5.0).isConnected(allDoubles));
    Assertions.assertTrue(allDoubles.isConnected(Range.closed(1.0, 5.0)));
    Assertions.assertTrue(allDoubles.isConnected(allDoubles));

    final SimpleFloatRange allFloats = SimpleRange.ofFloat(Range.all());
    Assertions.assertTrue(allFloats.isConnected(new SimpleFloatRange(1f, 5f)));
    Assertions.assertTrue(new SimpleFloatRange(1f, 5f).isConnected(allFloats));
    Assertions.assertTrue(allFloats.isConnected(Range.closed(1f, 5f)));
    Assertions.assertTrue(allFloats.isConnected(allFloats));

    final SimpleIntegerRange allInts = SimpleRange.ofInteger(Range.all());
    Assertions.assertTrue(allInts.isConnected(new SimpleIntegerRange(1, 5)));
    Assertions.assertTrue(new SimpleIntegerRange(1, 5).isConnected(allInts));
    Assertions.assertTrue(allInts.isConnected(Range.closed(1, 5)));
    Assertions.assertTrue(allInts.isConnected(allInts));
  }

  /**
   * OK: {@code isConnected(Range)} substitutes open bounds instead of throwing, so an unbounded
   * guava range is connected to everything.
   */
  @Test
  public void testIsConnectedAcceptsUnboundedGuavaRange() {
    final SimpleDoubleRange doubles = new SimpleDoubleRange(1.0, 5.0);
    Assertions.assertTrue(doubles.isConnected(Range.all()));
    Assertions.assertTrue(doubles.isConnected(Range.atLeast(1.0)));
    Assertions.assertTrue(doubles.isConnected(Range.atMost(5.0)));
    Assertions.assertFalse(doubles.isConnected(Range.atLeast(6.0)));
    Assertions.assertFalse(doubles.isConnected(Range.atMost(0.0)));

    final SimpleFloatRange floats = new SimpleFloatRange(1f, 5f);
    Assertions.assertTrue(floats.isConnected(Range.all()));
    Assertions.assertTrue(floats.isConnected(Range.atLeast(1f)));
    Assertions.assertFalse(floats.isConnected(Range.atLeast(6f)));

    final SimpleIntegerRange ints = new SimpleIntegerRange(1, 5);
    Assertions.assertTrue(ints.isConnected(Range.all()));
    Assertions.assertTrue(ints.isConnected(Range.atLeast(1)));
    Assertions.assertFalse(ints.isConnected(Range.atLeast(6)));
  }

  /**
   * OK: the interface factories delegate to the record factories, so a half bounded range gets the
   * MAX_VALUE substitute on the open side rather than throwing.
   */
  @Test
  public void testHalfBoundedRangeConversion() {
    Assertions.assertEquals(new SimpleDoubleRange(1.0, Double.MAX_VALUE),
        SimpleRange.ofDouble(Range.atLeast(1.0)));
    Assertions.assertEquals(new SimpleDoubleRange(-Double.MAX_VALUE, 5.0),
        SimpleRange.ofDouble(Range.atMost(5.0)));
    Assertions.assertEquals(new SimpleFloatRange(1f, Float.MAX_VALUE),
        SimpleRange.ofFloat(Range.atLeast(1f)));
    Assertions.assertEquals(new SimpleFloatRange(-Float.MAX_VALUE, 5f),
        SimpleRange.ofFloat(Range.atMost(5f)));
    Assertions.assertEquals(new SimpleIntegerRange(1, Integer.MAX_VALUE),
        SimpleRange.ofInteger(Range.atLeast(1)));
    Assertions.assertEquals(new SimpleIntegerRange(Integer.MIN_VALUE, 5),
        SimpleRange.ofInteger(Range.atMost(5)));

    // the interface factories and the record factories now agree
    Assertions.assertEquals(SimpleDoubleRange.of(Range.atLeast(1.0)),
        SimpleRange.ofDouble(Range.atLeast(1.0)));
    Assertions.assertEquals(SimpleFloatRange.of(Range.atMost(5f)),
        SimpleRange.ofFloat(Range.atMost(5f)));
    Assertions.assertEquals(SimpleIntegerRange.of(Range.atLeast(1)),
        SimpleRange.ofInteger(Range.atLeast(1)));
  }

  /**
   * Guava open bounds are exclusive, but {@link SimpleRange} is always closed, so the endpoint
   * itself is pulled into the range.
   */
  @Test
  public void testOpenBoundBecomesClosed() {
    Assertions.assertEquals(new SimpleDoubleRange(1.0, 5.0),
        SimpleRange.ofDouble(Range.open(1.0, 5.0)));
    Assertions.assertTrue(SimpleRange.ofDouble(Range.open(1.0, 5.0)).contains(1.0));
    Assertions.assertFalse(Range.open(1.0, 5.0).contains(1.0));
  }

  /**
   * OK: the MAX_VALUE bounds survive a conversion to guava and back, so the substitute is stable
   * once it has been applied.
   */
  @Test
  public void testAllSubstituteIsStableOnRepeatedConversion() {
    final SimpleDoubleRange doubles = SimpleRange.ofDouble(Range.all());
    Assertions.assertEquals(doubles, SimpleRange.ofDouble(doubles.guava()));

    final SimpleFloatRange floats = SimpleRange.ofFloat(Range.all());
    Assertions.assertEquals(floats, SimpleFloatRange.of(floats.guava()));

    final SimpleIntegerRange ints = SimpleRange.ofInteger(Range.all());
    Assertions.assertEquals(ints, SimpleRange.ofInteger(ints.guava()));
  }
}
