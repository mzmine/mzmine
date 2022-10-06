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
