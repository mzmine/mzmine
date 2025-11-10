package util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.SimpleRange.SimpleDoubleRange;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link SimpleDoubleRange} class. This class validates the functionality of the
 * {@code isConnected} method, which determines whether two ranges are connected (overlap or
 * touch).
 */
public class SimpleDoubleRangeTest {

  /**
   * Test case where the ranges overlap.
   */
  @Test
  public void testIsConnectedOverlap() {
    SimpleDoubleRange range1 = new SimpleDoubleRange(1.0, 5.0);
    SimpleDoubleRange range2 = new SimpleDoubleRange(4.0, 8.0);

    assertTrue(range1.isConnected(range2));
    assertTrue(range2.isConnected(range1));
  }

  /**
   * Test case where the ranges touch at boundaries.
   */
  @Test
  public void testIsConnectedTouchAtBoundary() {
    SimpleDoubleRange range1 = new SimpleDoubleRange(1.0, 5.0);
    SimpleDoubleRange range2 = new SimpleDoubleRange(5.0, 8.0);

    assertTrue(range1.isConnected(range2));
    assertTrue(range2.isConnected(range1));
  }

  /**
   * Test case where one range completely encloses the other.
   */
  @Test
  public void testIsConnectedEnclosedRange() {
    SimpleDoubleRange range1 = new SimpleDoubleRange(1.0, 10.0);
    SimpleDoubleRange range2 = new SimpleDoubleRange(3.0, 7.0);

    assertTrue(range1.isConnected(range2));
    assertTrue(range2.isConnected(range1));
  }

  /**
   * Test case where the ranges do not connect.
   */
  @Test
  public void testIsConnectedNoConnection() {
    SimpleDoubleRange range1 = new SimpleDoubleRange(1.0, 5.0);
    SimpleDoubleRange range2 = new SimpleDoubleRange(6.0, 10.0);

    assertFalse(range1.isConnected(range2));
    assertFalse(range2.isConnected(range1));
  }

  /**
   * Test case where both ranges are identical.
   */
  @Test
  public void testIsConnectedIdenticalRanges() {
    SimpleDoubleRange range1 = new SimpleDoubleRange(1.0, 5.0);
    SimpleDoubleRange range2 = new SimpleDoubleRange(1.0, 5.0);

    assertTrue(range1.isConnected(range2));
    assertTrue(range2.isConnected(range1));
  }

  /**
   * Test case with a connection to a Guava Range with overlapping boundaries.
   */
  @Test
  public void testIsConnectedGuavaOverlap() {
    SimpleDoubleRange range1 = new SimpleDoubleRange(1.0, 5.0);
    Range<Double> guavaRange = Range.closed(4.0, 8.0);

    assertTrue(range1.isConnected(guavaRange));
  }

  /**
   * Test case with a connection to a Guava Range that touches at the boundary.
   */
  @Test
  public void testIsConnectedGuavaTouchAtBoundary() {
    SimpleDoubleRange range1 = new SimpleDoubleRange(1.0, 5.0);
    Range<Double> guavaRange = Range.closed(5.0, 10.0);

    assertTrue(range1.isConnected(guavaRange));
  }

  /**
   * Test case with no connection to a Guava Range.
   */
  @Test
  public void testIsConnectedGuavaNoConnection() {
    SimpleDoubleRange range1 = new SimpleDoubleRange(1.0, 5.0);
    Range<Double> guavaRange = Range.closed(6.0, 10.0);

    assertFalse(range1.isConnected(guavaRange));
  }
}