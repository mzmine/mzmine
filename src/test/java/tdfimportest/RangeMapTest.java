/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package tdfimportest;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

public class RangeMapTest {

  public static final double EPSILON = 1E-15;

  private static Logger logger = Logger.getLogger(RangeMapTest.class.getName());

  /**
   * Creates a new non overlapping range for this range map. Ranges are created seamless, therefore
   * no gaps are introduced during this process.
   *
   * @param rangeMap
   * @param proposedRange The proposed range must not enclose a range in this map without
   *                      overlapping, otherwise the enclosed range will be deleted.
   * @return
   */
  private static Range<Double> createNewNonOverlappingRange(RangeMap<Double, ?> rangeMap,
      final Range<Double> proposedRange) {

    Entry<Range<Double>, ?> lowerEntry = rangeMap.getEntry(
        proposedRange.lowerBoundType() == BoundType.CLOSED ? proposedRange.lowerEndpoint()
            : proposedRange.lowerEndpoint() + EPSILON);

    Entry<Range<Double>, ?> upperEntry = rangeMap.getEntry(
        proposedRange.upperBoundType() == BoundType.CLOSED ? proposedRange.upperEndpoint()
            : proposedRange.upperEndpoint() - EPSILON);

    if (lowerEntry == null && upperEntry == null) {
      return proposedRange;
    }

    if (lowerEntry != null && proposedRange.intersection(lowerEntry.getKey()).isEmpty()
        && upperEntry == null) {
      return proposedRange;
    }

    if (upperEntry != null && proposedRange.intersection(upperEntry.getKey()).isEmpty()
        && lowerEntry == null) {
      return proposedRange;
    }

    if (upperEntry != null && lowerEntry != null && proposedRange.intersection(lowerEntry.getKey())
        .isEmpty() && proposedRange.intersection(upperEntry.getKey()).isEmpty()) {
      return proposedRange;
    }

    BoundType lowerBoundType = proposedRange.lowerBoundType();
    BoundType upperBoundType = proposedRange.upperBoundType();
    double lowerBound = proposedRange.lowerEndpoint();
    double upperBound = proposedRange.upperEndpoint();

    // check if the ranges actually overlap or if they are closed and open
    if (lowerEntry != null && !proposedRange.intersection(lowerEntry.getKey()).isEmpty()) {
      lowerBound = lowerEntry.getKey().upperEndpoint();
      lowerBoundType = BoundType.OPEN;
    }
    if (upperEntry != null && !proposedRange.intersection(upperEntry.getKey()).isEmpty()) {
      upperBound = upperEntry.getKey().lowerEndpoint();
      upperBoundType = BoundType.OPEN;
    }

    return createNewNonOverlappingRange(rangeMap,
        Range.range(lowerBound, lowerBoundType, upperBound, upperBoundType));
  }

  @Test
  public void testOverlappingRanges() {

    RangeMap<Double, Double> map = TreeRangeMap.create();

    MZTolerance mzTolerance = new MZTolerance(0.000, 25);

    double mz1 = 760.5871;
    double mz2 = 760.6010;
    double mz3 = 760.6150;

    Range<Double> range1 = mzTolerance.getToleranceRange(mz1);
    Range<Double> range2 = mzTolerance.getToleranceRange(mz2);
    Range<Double> range3 = mzTolerance.getToleranceRange(mz3);

    map.put(createNewNonOverlappingRange(map, range1), mz1);
    map.put(createNewNonOverlappingRange(map, range2), mz2);
    map.put(createNewNonOverlappingRange(map, range3), mz3);

    logger.info("actual " + map.getEntry(mz1).getKey().toString() + " proposed " + range1);
    logger.info("actual " + map.getEntry(mz2).getKey().toString() + " proposed " + range2);
    logger.info("actual " + map.getEntry(mz3).getKey().toString() + " proposed " + range3);
  }
}
