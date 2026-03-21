package util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.SimpleRange.SimpleIntegerRange;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link SimpleIntegerRange} class. This class validates the functionality of
 * the {@code isConnected} method, which determines whether two ranges are connected (overlap or
 * touch).
 */
public class SimpleIntegerRangeTest {

  /**
   * Test case where the ranges overlap.
   */
  @Test
  public void testIsConnectedOverlap() {
    SimpleIntegerRange range1 = new SimpleIntegerRange(1, 5);
    SimpleIntegerRange range2 = new SimpleIntegerRange(4, 8);

    assertTrue(range1.isConnected(range2));
    assertTrue(range2.isConnected(range1));
  }

  /**
   * Test case where the ranges touch at boundaries.
   */
  @Test
  public void testIsConnectedTouchAtBoundary() {
    SimpleIntegerRange range1 = new SimpleIntegerRange(1, 5);
    SimpleIntegerRange range2 = new SimpleIntegerRange(5, 8);

    assertTrue(range1.isConnected(range2));
    assertTrue(range2.isConnected(range1));
  }

  /**
   * Test case where one range completely encloses the other.
   */
  @Test
  public void testIsConnectedEnclosedRange() {
    SimpleIntegerRange range1 = new SimpleIntegerRange(1, 10);
    SimpleIntegerRange range2 = new SimpleIntegerRange(3, 7);

    assertTrue(range1.isConnected(range2));
    assertTrue(range2.isConnected(range1));
  }

  /**
   * Test case where the ranges do not connect.
   */
  @Test
  public void testIsConnectedNoConnection() {
    SimpleIntegerRange range1 = new SimpleIntegerRange(1, 5);
    SimpleIntegerRange range2 = new SimpleIntegerRange(6, 10);

    assertFalse(range1.isConnected(range2));
    assertFalse(range2.isConnected(range1));
  }

  /**
   * Test case where both ranges are identical.
   */
  @Test
  public void testIsConnectedIdenticalRanges() {
    SimpleIntegerRange range1 = new SimpleIntegerRange(1, 5);
    SimpleIntegerRange range2 = new SimpleIntegerRange(1, 5);

    assertTrue(range1.isConnected(range2));
    assertTrue(range2.isConnected(range1));
  }

  /**
   * Test case with a connection to a Guava Range with overlapping boundaries.
   */
  @Test
  public void testIsConnectedGuavaOverlap() {
    SimpleIntegerRange range1 = new SimpleIntegerRange(1, 5);
    Range<Integer> guavaRange = Range.closed(4, 8);

    assertTrue(range1.isConnected(guavaRange));
  }

  /**
   * Test case with a connection to a Guava Range that touches at the boundary.
   */
  @Test
  public void testIsConnectedGuavaTouchAtBoundary() {
    SimpleIntegerRange range1 = new SimpleIntegerRange(1, 5);
    Range<Integer> guavaRange = Range.closed(5, 10);

    assertTrue(range1.isConnected(guavaRange));
  }

  /**
   * Test case with no connection to a Guava Range.
   */
  @Test
  public void testIsConnectedGuavaNoConnection() {
    SimpleIntegerRange range1 = new SimpleIntegerRange(1, 5);
    Range<Integer> guavaRange = Range.closed(6, 10);

    assertFalse(range1.isConnected(guavaRange));
  }
}