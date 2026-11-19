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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.local_max;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MobilityFlankConsolidatorTest {

  private static final MZTolerance TOLERANCE = new MZTolerance(0.01, 0);
  private static final double THRESHOLD = 25_000;
  private static final double TRUE_MZ = 500.0;
  // saturated apex scans read 6 ppm high
  private static final double SATURATED_MZ = 500.003;
  private static final double OTHER_ION_MZ = 600.0;

  /**
   * 21 mobility scans with a drift peak of the ion at m/z 500 (height up to 80k, clipped scans read
   * high) and an unsaturated ion at m/z 600 in every scan.
   */
  private static List<List<double[]>> frame(final boolean withFlanks, final int gapScan) {
    final List<double[]> mzs = new ArrayList<>();
    final List<double[]> heights = new ArrayList<>();
    for (int scan = 0; scan < 21; scan++) {
      final double height = 80_000 * Math.exp(-0.5 * Math.pow((scan - 10) / 3.0, 2));
      final boolean saturated = height >= THRESHOLD;
      final boolean present = scan != gapScan && (withFlanks || saturated);
      if (present) {
        mzs.add(
            new double[]{saturated ? SATURATED_MZ : TRUE_MZ + (scan % 2) * 0.0002, OTHER_ION_MZ});
        heights.add(new double[]{height, 5_000});
      } else {
        mzs.add(new double[]{OTHER_ION_MZ});
        heights.add(new double[]{5_000});
      }
    }
    return List.of(mzs, heights);
  }

  @Test
  void saturatedMembersGetTheFlankMz() {
    final List<List<double[]>> frame = frame(true, -1);
    final List<double[]> result = MobilityFlankConsolidator.consolidate(frame.get(0), frame.get(1),
        THRESHOLD, TOLERANCE);

    for (int scan = 0; scan < result.size(); scan++) {
      final double[] heights = frame.get(1).get(scan);
      final double mz = result.get(scan)[0];
      if (heights[0] >= THRESHOLD) {
        Assertions.assertEquals(TRUE_MZ, mz, 0.0002, "saturated scan " + scan);
      } else {
        // unsaturated members and other ions are not changed
        Assertions.assertSame(frame.get(0).get(scan), result.get(scan), "scan " + scan);
      }
      Assertions.assertEquals(OTHER_ION_MZ, result.get(scan)[result.get(scan).length - 1]);
    }
  }

  @Test
  void singleMissingScanIsBridged() {
    // the scan next to the saturated core misses the ion, the outer flanks are still used
    final List<List<double[]>> frame = frame(true, 5);
    final List<double[]> result = MobilityFlankConsolidator.consolidate(frame.get(0), frame.get(1),
        THRESHOLD, TOLERANCE);
    Assertions.assertEquals(TRUE_MZ, result.get(10)[0], 0.0002);
  }

  @Test
  void withoutFlanksTheMzIsKept() {
    final List<List<double[]>> frame = frame(false, -1);
    final List<double[]> result = MobilityFlankConsolidator.consolidate(frame.get(0), frame.get(1),
        THRESHOLD, TOLERANCE);
    Assertions.assertEquals(SATURATED_MZ, result.get(10)[0]);
  }

  @Test
  void ionsOutsideTheToleranceAreNotLinked() {
    final List<List<double[]>> frame = frame(true, -1);
    final List<double[]> result = MobilityFlankConsolidator.consolidate(frame.get(0), frame.get(1),
        THRESHOLD, new MZTolerance(0.001, 0));
    // the unsaturated flanks are 3 mDa away from the saturated core -> no group members
    Assertions.assertEquals(SATURATED_MZ, result.get(10)[0]);
  }

  @Test
  void noThresholdNoChange() {
    final List<List<double[]>> frame = frame(true, -1);
    final List<double[]> result = MobilityFlankConsolidator.consolidate(frame.get(0), frame.get(1),
        Double.POSITIVE_INFINITY, TOLERANCE);
    for (int scan = 0; scan < result.size(); scan++) {
      Assertions.assertSame(frame.get(0).get(scan), result.get(scan));
    }
  }
}
