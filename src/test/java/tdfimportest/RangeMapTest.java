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

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.scans.SpectraMerging;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RangeMapTest {

  public static final double EPSILON = 1E-15;

  private static Logger logger = Logger.getLogger(RangeMapTest.class.getName());

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

    map.put(SpectraMerging.createNewNonOverlappingRange(map, range1), mz1);
    map.put(SpectraMerging.createNewNonOverlappingRange(map, range2), mz2);
    map.put(SpectraMerging.createNewNonOverlappingRange(map, range3), mz3);

    logger.info("actual " + map.getEntry(mz1).getKey().toString() + " proposed " + range1);
    logger.info("actual " + map.getEntry(mz2).getKey().toString() + " proposed " + range2);
    logger.info("actual " + map.getEntry(mz3).getKey().toString() + " proposed " + range3);

    Assertions.assertEquals(760.5680853225, map.getEntry(mz1).getKey().lowerEndpoint());
    Assertions.assertEquals(760.6061146774999, map.getEntry(mz1).getKey().upperEndpoint());
    Assertions.assertEquals(760.5680853225, map.getEntry(mz2).getKey().lowerEndpoint());
    Assertions.assertEquals(760.6061146774999, map.getEntry(mz2).getKey().upperEndpoint());
    Assertions.assertEquals(760.6061146774999, map.getEntry(mz3).getKey().lowerEndpoint());
    Assertions.assertEquals(760.634015375, map.getEntry(mz3).getKey().upperEndpoint());
  }

}
