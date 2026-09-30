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

package io.github.mzmine.modules.visualization.chromatogram_comparison;

import static io.github.mzmine.modules.visualization.chromatogram_comparison.ComparisonSide.A;
import static io.github.mzmine.modules.visualization.chromatogram_comparison.ComparisonSide.B;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Range;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class ChromatogramComparisonTest {

  private static final MZTolerance TOLERANCE = new MZTolerance(0.002, 10);
  private static final double SIGNAL = 1000;
  // 0.1 min per scan
  private static final ComparisonScans SCANS = ComparisonScans.sameScans(rts(40));
  // every mass list has a data point at the m/z of each chromatogram
  private static final MassListProbe ANY_MASS_LIST = (_, mz) -> mz;

  private static float @NotNull [] rts(int numScans) {
    final float[] rts = new float[numScans];
    for (int i = 0; i < numScans; i++) {
      rts[i] = i * 0.1f;
    }
    return rts;
  }

  /**
   * A chromatogram over consecutive scans
   */
  @NotNull
  private static ComparedChromatogram chrom(@NotNull ComparisonSide side, int id, double mz,
      int firstScan, double @NotNull ... intensities) {
    final int[] scans = IntStream.range(firstScan, firstScan + intensities.length).toArray();
    return chrom(side, id, mz, scans, intensities);
  }

  /**
   * The data points have the m/z of the chromatogram rounded to 0.01, so both lists share the data
   * points of an ion like data points of the same mass list, while the m/z of the chromatograms
   * differ.
   */
  @NotNull
  private static ComparedChromatogram chrom(@NotNull ComparisonSide side, int id, double mz,
      int @NotNull [] scans, double @NotNull [] intensities) {
    final double[] mzs = new double[scans.length];
    Arrays.fill(mzs, Math.round(mz * 100) / 100d);
    return ComparedChromatogram.create(side, id, mz, scans, intensities, mzs, null);
  }

  private static void assertRtRange(double lower, double upper, Range<Float> range) {
    assertNotNull(range);
    assertEquals(lower, range.lowerEndpoint(), 1E-5);
    assertEquals(upper, range.upperEndpoint(), 1E-5);
  }

  @NotNull
  private static List<ChromatogramGroup> compare(@NotNull List<ComparedChromatogram> a,
      @NotNull List<ComparedChromatogram> b) {
    return ChromatogramComparison.compare(a, b, SCANS, TOLERANCE, SIGNAL, ANY_MASS_LIST);
  }

  @NotNull
  private static Set<ChromatogramIssue> issues(ChromatogramIssue... issues) {
    return issues.length == 0 ? EnumSet.noneOf(ChromatogramIssue.class)
        : EnumSet.copyOf(List.of(issues));
  }

  @Test
  void identicalChromatogramsHaveNoIssues() {
    final double[] peak = {2000, 5000, 9000, 5000, 2000};
    final List<ChromatogramGroup> groups = compare(List.of(chrom(A, 1, 200.1, 10, peak)),
        List.of(chrom(B, 7, 200.1005, 10, peak)));

    assertEquals(1, groups.size());
    final ChromatogramGroup group = groups.getFirst();
    assertEquals(1, group.id());
    assertEquals(issues(), group.issues());
    assertEquals(1, group.a().chromatograms().getFirst().rowId());
    assertEquals(7, group.b().chromatograms().getFirst().rowId());
    assertEquals(200.10025, group.mz(), 1E-9);
    assertEquals(2.5, group.deltaMzPpm(), 0.01);
    assertEquals(1.2, group.apexRt(), 1E-5);
    assertRtRange(1, 1.4, group.signalRtRange());
  }

  @Test
  void closestChromatogramsFormGroupsAndOthersJoinThem() {
    // A2 is a duplicate of A1 at the edge of the tolerance, A3 has no partner
    final ComparedChromatogram a1 = chrom(A, 1, 300.0500, 10, 5000, 9000, 5000);
    final ComparedChromatogram a2 = chrom(A, 2, 300.0515, 10, 1500);
    final ComparedChromatogram a3 = chrom(A, 3, 300.0600, 10, 5000, 9000, 5000);
    final ComparedChromatogram b1 = chrom(B, 1, 300.0503, 10, 5000, 9000, 5000);
    // B2 and B3 are resolved in both lists
    final ComparedChromatogram a4 = chrom(A, 4, 400.0000, 20, 5000, 9000, 5000);
    final ComparedChromatogram a5 = chrom(A, 5, 400.0030, 20, 5000, 9000, 5000);
    final ComparedChromatogram b2 = chrom(B, 2, 400.0001, 20, 5000, 9000, 5000);
    final ComparedChromatogram b3 = chrom(B, 3, 400.0029, 20, 5000, 9000, 5000);

    final List<ChromatogramGroup> groups = compare(List.of(a5, a3, a1, a4, a2),
        List.of(b3, b2, b1));

    assertEquals(4, groups.size());
    final ChromatogramGroup duplicate = groups.get(0);
    assertEquals(List.of(a1, a2), duplicate.a().chromatograms());
    assertEquals(List.of(b1), duplicate.b().chromatograms());
    assertTrue(duplicate.issues().contains(ChromatogramIssue.MULTIPLE_A));

    final ChromatogramGroup onlyA = groups.get(1);
    assertEquals(List.of(a3), onlyA.a().chromatograms());
    assertTrue(onlyA.b().isEmpty());
    assertEquals(issues(ChromatogramIssue.ONLY_A), onlyA.issues());

    assertEquals(List.of(a4), groups.get(2).a().chromatograms());
    assertEquals(List.of(b2), groups.get(2).b().chromatograms());
    assertEquals(List.of(a5), groups.get(3).a().chromatograms());
    assertEquals(List.of(b3), groups.get(3).b().chromatograms());
    assertEquals(issues(), groups.get(3).issues());
    // ids ascend with the m/z
    assertEquals(List.of(1, 2, 3, 4), groups.stream().map(ChromatogramGroup::id).toList());
  }

  @Test
  void missingSignalOfTheOtherListIsFlagged() {
    // A misses scan 12 and the tail at scans 15 and 16, B misses nothing
    final ComparedChromatogram a = chrom(A, 1, 250.2, new int[]{10, 11, 13, 14},
        new double[]{3000, 6000, 6000, 3000});
    final ComparedChromatogram b = chrom(B, 1, 250.2, 10, 3000, 6000, 9000, 6000, 3000, 1500, 1200);

    final ChromatogramGroup group = compare(List.of(a), List.of(b)).getFirst();

    assertTrue(group.issues().contains(ChromatogramIssue.MISSING_A));
    assertFalse(group.issues().contains(ChromatogramIssue.MISSING_B));
    assertEquals(3, group.a().missingDataPoints());
    assertEquals(9000, group.a().maxMissedIntensity());
    // scan 12 is also a hole between two signals of A
    assertTrue(group.issues().contains(ChromatogramIssue.HOLES_A));
    assertEquals(1, group.a().holeDataPoints());

    final List<MissingRegion> regions = group.a().missingRegions();
    assertEquals(2, regions.size());
    final MissingRegion single = regions.get(0);
    assertTrue(single.isSingleScan());
    assertEquals(12, single.firstScan());
    assertEquals(1.2, single.rtFirst(), 1E-5);
    assertEquals(1.15, single.rtStart(), 1E-5);
    assertEquals(1.25, single.rtEnd(), 1E-5);
    assertTrue(single.otherSideHasSignal());
    final MissingRegion tail = regions.get(1);
    assertEquals(15, tail.firstScan());
    assertEquals(16, tail.lastScan());
    assertEquals(2, tail.numScans());
    assertEquals(1500, tail.otherMaxIntensity());
    assertTrue(group.b().missingRegions().isEmpty());
  }

  @Test
  void holesNeedSignalsOnBothSidesAndAShortGap() {
    // gap of 2 scans between signals, gap of 6 scans (too long), gap next to a weak data point
    final ComparedChromatogram a = chrom(A, 1, 150.05, new int[]{2, 3, 6, 7, 14, 15, 16, 18, 19},
        new double[]{5000, 5000, 5000, 5000, 5000, 5000, 500, 5000, 5000});

    final ChromatogramGroup group = compare(List.of(a), List.of()).getFirst();

    assertEquals(issues(ChromatogramIssue.ONLY_A, ChromatogramIssue.HOLES_A), group.issues());
    assertEquals(2, group.a().holeDataPoints());
    assertEquals(0, group.a().missingDataPoints());
    final List<MissingRegion> regions = group.a().missingRegions();
    assertEquals(1, regions.size());
    assertEquals(4, regions.getFirst().firstScan());
    assertEquals(5, regions.getFirst().lastScan());
    assertFalse(regions.getFirst().otherSideHasSignal());
  }

  @Test
  void gapsWithoutDataPointInTheMassListAreNoHoles() {
    final ComparedChromatogram a = chrom(A, 1, 150.05, new int[]{2, 3, 6, 7},
        new double[]{5000, 5000, 5000, 5000});
    // only the mass list of scan 5 has a data point at the m/z of the chromatogram
    final MassListProbe probe = (scan, mz) -> scan == 5 && Math.abs(mz - 150.05) < 1E-9 ? mz
        : Double.NaN;

    final ChromatogramGroup group = ChromatogramComparison.compare(List.of(a), List.of(), SCANS,
        TOLERANCE, SIGNAL, probe).getFirst();

    assertEquals(1, group.a().holeDataPoints());
    assertEquals(1, group.a().missingRegions().size());
    assertEquals(5, group.a().missingRegions().getFirst().firstScan());
    assertTrue(group.a().missingRegions().getFirst().isSingleScan());
    final ChromatogramGroup noDataPoints = ChromatogramComparison.compare(List.of(a), List.of(),
        SCANS, TOLERANCE, SIGNAL, (_, _) -> Double.NaN).getFirst();
    assertEquals(issues(ChromatogramIssue.ONLY_A), noDataPoints.issues());
  }

  @Test
  void signalsBelowTheThresholdAreNoIssues() {
    final ComparedChromatogram a = chrom(A, 1, 500.5, new int[]{10, 12}, new double[]{800, 900});
    final ComparedChromatogram b = chrom(B, 1, 500.5, 10, 800, 950, 900, 700);

    final ChromatogramGroup group = compare(List.of(a), List.of(b)).getFirst();

    assertEquals(issues(), group.issues());
    // without signal the range covers all data points
    assertRtRange(1, 1.3, group.signalRtRange());
    // a threshold of 0 counts every data point
    final ChromatogramGroup noThreshold = ChromatogramComparison.compare(List.of(a), List.of(b),
        SCANS, TOLERANCE, 0, ANY_MASS_LIST).getFirst();
    assertEquals(issues(ChromatogramIssue.MISSING_A, ChromatogramIssue.HOLES_A),
        noThreshold.issues());
    assertEquals(2, noThreshold.a().missingDataPoints());
  }

  @Test
  void differentDataPointsInTheSameScanAreFlagged() {
    final int[] scans = {10, 11, 12};
    final ComparedChromatogram a = ComparedChromatogram.create(A, 1, 180.0, scans,
        new double[]{3000, 9000, 3000}, new double[]{180.0, 180.0, 180.0}, null);
    // B took a noise data point in scan 12
    final ComparedChromatogram b = ComparedChromatogram.create(B, 1, 180.0, scans,
        new double[]{3000, 9000, 200}, new double[]{180.0, 180.0, 180.0011}, null);

    final ChromatogramGroup group = compare(List.of(a), List.of(b)).getFirst();

    assertEquals(1, group.differentDataPoints());
    assertEquals(issues(ChromatogramIssue.DIFFERENT_DATA_POINTS), group.issues());
  }

  @Test
  void scansNotUsedByABuilderAreNotMissing() {
    final boolean[] usedByA = new boolean[SCANS.size()];
    final boolean[] usedByB = new boolean[SCANS.size()];
    Arrays.fill(usedByA, true);
    Arrays.fill(usedByB, true);
    // the builder of A did not use scan 12
    usedByA[12] = false;
    final ComparisonScans scans = new ComparisonScans(SCANS.rts(), usedByA, usedByB);
    final ComparedChromatogram a = chrom(A, 1, 250.2, new int[]{10, 11, 13},
        new double[]{3000, 6000, 3000});
    final ComparedChromatogram b = chrom(B, 1, 250.2, 10, 3000, 6000, 9000, 3000);

    final ChromatogramGroup group = ChromatogramComparison.compare(List.of(a), List.of(b), scans,
        TOLERANCE, SIGNAL, ANY_MASS_LIST).getFirst();

    assertEquals(issues(), group.issues());
  }

  @Test
  void severityOrderPutsGroupsWithoutIssuesLast() {
    final List<Set<ChromatogramIssue>> sorted = List.of(issues(), issues(ChromatogramIssue.HOLES_B),
            issues(ChromatogramIssue.MISSING_A, ChromatogramIssue.HOLES_A),
            issues(ChromatogramIssue.MISSING_A), issues(ChromatogramIssue.ONLY_B)).stream()
        .sorted(ChromatogramIssue.SEVERITY_ORDER).toList();

    assertEquals(List.of(issues(ChromatogramIssue.ONLY_B),
        issues(ChromatogramIssue.MISSING_A, ChromatogramIssue.HOLES_A),
        issues(ChromatogramIssue.MISSING_A), issues(ChromatogramIssue.HOLES_B), issues()), sorted);
  }

  @Test
  void complementaryChromatogramsOfOneSideAreSplit() {
    // A2 continues the peak of A1 at another m/z and never shares a scan with it
    final ComparedChromatogram a1 = chrom(A, 1, 300.0500, 10, 5000, 9000, 20000, 9000);
    final ComparedChromatogram a2 = chrom(A, 2, 300.0520, 14, 6000, 3000, 1500);
    final ComparedChromatogram b = chrom(B, 1, 300.0510, 10, 5000, 9000, 20000, 9000, 6000, 3000,
        1500);
    final ChromatogramGroup split = compare(List.of(a1, a2), List.of(b)).getFirst();
    assertTrue(split.issues().contains(ChromatogramIssue.SPLIT_A), split.issues().toString());
    assertFalse(split.issues().contains(ChromatogramIssue.MULTIPLE_A));

    // A3 is a peak of its own at another retention time
    final ComparedChromatogram a3 = chrom(A, 3, 300.0520, 30, 3000, 20000, 3000);
    final ComparedChromatogram b2 = chrom(B, 1, 300.0510, new int[]{10, 11, 12, 30, 31, 32},
        new double[]{5000, 20000, 5000, 3000, 20000, 3000});
    final ChromatogramGroup multiple = compare(
        List.of(chrom(A, 1, 300.0500, 10, 5000, 20000, 5000), a3), List.of(b2)).getFirst();
    assertTrue(multiple.issues().contains(ChromatogramIssue.MULTIPLE_A));
    assertFalse(multiple.issues().contains(ChromatogramIssue.SPLIT_A));
  }

  @Test
  void chromatogramThatFailsTheFilterOfTheOtherBuilderIsExpected() {
    // 5 consecutive scans above 1000, the height of 8000 only after a gap
    final ComparedChromatogram lowSegment = chrom(A, 1, 200.1, new int[]{10, 11, 12, 13, 14, 16},
        new double[]{2000, 3000, 4000, 3000, 2000, 8000});
    final SegmentFilter filterB = new SegmentFilter(5, 1000, 8000);
    final ChromatogramGroup expected = ChromatogramComparison.compare(List.of(lowSegment),
        List.of(), SCANS, TOLERANCE, SIGNAL, ANY_MASS_LIST, null, filterB).getFirst();
    assertTrue(expected.issues().contains(ChromatogramIssue.ONLY_A_LOW_SEGMENT));
    assertFalse(expected.issues().contains(ChromatogramIssue.ONLY_A));
    assertTrue(ChromatogramIssue.ONLY_A_LOW_SEGMENT.isExpected());
    assertFalse(GroupFilter.UNEXPECTED_ISSUE.predicate().test(ChromatogramComparison.compare(
        List.of(chrom(A, 1, 200.1, new int[]{10, 11, 12, 13, 14, 16},
            new double[]{2000, 3000, 4000, 3000, 2000, 8000})), List.of(), SCANS, TOLERANCE, 0,
        (_, _) -> Double.NaN, null, filterB).getFirst()));

    final ComparedChromatogram passes = chrom(A, 1, 200.1, 10, 2000, 3000, 9000, 3000, 2000);
    final ChromatogramGroup unexpected = ChromatogramComparison.compare(List.of(passes), List.of(),
        SCANS, TOLERANCE, SIGNAL, ANY_MASS_LIST, null, filterB).getFirst();
    assertEquals(issues(ChromatogramIssue.ONLY_A), unexpected.issues());
    assertTrue(GroupFilter.UNEXPECTED_ISSUE.predicate().test(unexpected));
    // without the filter of B, only A is unexpected
    assertTrue(compare(List.of(lowSegment), List.of()).getFirst().issues()
        .contains(ChromatogramIssue.ONLY_A));
  }

  @Test
  void takenByNamesTheChromatogramsWithTheMissingDataPoints() {
    // A1 misses scans 12 and 13 of B. A2, 12 ppm away in its own group, took the data point of
    // scan 12, the data point of scan 13 is in no chromatogram of A.
    final ComparedChromatogram a1 = chrom(A, 1, 250.2, new int[]{10, 11, 14},
        new double[]{3000, 6000, 3000});
    final ComparedChromatogram a2 = ComparedChromatogram.create(A, 2, 250.203, new int[]{12},
        new double[]{9000}, new double[]{250.2}, null);
    final ComparedChromatogram b = chrom(B, 1, 250.2, 10, 3000, 6000, 9000, 8000, 3000);

    final List<ChromatogramGroup> groups = compare(List.of(a1, a2), List.of(b));
    assertEquals(2, groups.size());
    final ChromatogramGroup group = groups.getFirst();
    assertEquals(2, group.a().missingDataPoints());
    final List<DataPointOwner> takenBy = group.a().takenBy();
    assertEquals(2, takenBy.size(), takenBy.toString());
    assertEquals(a2, takenBy.get(0).chromatogram());
    assertEquals(2, takenBy.get(0).groupId());
    assertEquals(1, takenBy.get(0).count());
    assertTrue(takenBy.get(1).isUnused());
    assertEquals(1, takenBy.get(1).count());
    assertTrue(group.b().takenBy().isEmpty());

    // the group of A2 has no chromatogram of B, B has the signal in group 1
    final ChromatogramGroup onlyA = groups.get(1);
    assertEquals(issues(ChromatogramIssue.ONLY_A), onlyA.issues());
    assertEquals(List.of(new DataPointOwner(b, 1, 1)), onlyA.b().takenBy());
  }

  @Test
  void missingPeakInAnotherChromatogramIsTaken() {
    // a separate peak at 250.2000 in scans 10-17 right before an intense ion 13 ppm away. A has
    // both in their own chromatograms, B joined the peak into the chromatogram of the ion and has
    // another peak of 250.1990 later.
    final double[] peak = {2000, 4000, 7000, 9000, 7000, 4000, 2000, 1500};
    final ComparedChromatogram a1 = ComparedChromatogram.create(A, 1, 250.2000,
        IntStream.range(10, 18).toArray(), peak, filled(8, 250.2000), null);
    final ComparedChromatogram a2 = ComparedChromatogram.create(A, 2, 250.2033,
        IntStream.range(18, 26).toArray(), new double[]{3E4, 6E4, 9E4, 6E4, 3E4, 1E4, 5000, 2000},
        filled(8, 250.2033), null);
    final ComparedChromatogram b1 = ComparedChromatogram.create(B, 1, 250.1990,
        IntStream.range(30, 35).toArray(), new double[]{2000, 5000, 8000, 5000, 2000},
        filled(5, 250.1990), null);
    final double[] joined = new double[16];
    final double[] joinedMzs = new double[16];
    for (int i = 0; i < 16; i++) {
      joined[i] = i < 8 ? peak[i] : a2.intensities()[i - 8];
      joinedMzs[i] = i < 8 ? 250.2000 : 250.2033;
    }
    final ComparedChromatogram b2 = ComparedChromatogram.create(B, 2, 250.2031,
        IntStream.range(10, 26).toArray(), joined, joinedMzs, null);

    final ChromatogramGroup group = compare(List.of(a1, a2), List.of(b1, b2)).getFirst();
    assertEquals(List.of(b1), group.b().chromatograms());
    assertEquals(8, group.b().missingDataPoints());
    assertTrue(group.b().missingPeakTaken());
    assertTrue(group.issues().contains(ChromatogramIssue.TAKEN_B), group.issues().toString());
    assertFalse(group.issues().contains(ChromatogramIssue.MISSING_B));
    assertEquals(List.of(new DataPointOwner(b2, 2, 8)), group.b().takenBy());

    // the builder of B needs 10 consecutive scans, too many for the peak in B2
    final ChromatogramGroup longer = ChromatogramComparison.compare(List.of(a1, a2),
        List.of(b1, b2), SCANS, TOLERANCE, SIGNAL, ANY_MASS_LIST, null,
        new SegmentFilter(10, 1000, 5000)).getFirst();
    assertTrue(longer.issues().contains(ChromatogramIssue.MISSING_B));
    assertFalse(longer.issues().contains(ChromatogramIssue.TAKEN_B));
  }

  @Test
  void apexTakenByASideChromatogramIsMissing() {
    // B2, a co-eluting side signal in its own group, took the apex of the ion in scan 12
    final ComparedChromatogram a = chrom(A, 1, 250.2, 10, 3000, 6000, 9000, 6000, 3000);
    final ComparedChromatogram b1 = chrom(B, 1, 250.2, new int[]{10, 11, 13, 14},
        new double[]{3000, 6000, 6000, 3000});
    final ComparedChromatogram b2 = ComparedChromatogram.create(B, 2, 250.2035,
        new int[]{11, 12, 13}, new double[]{2000, 9000, 2000},
        new double[]{250.2035, 250.2, 250.2035}, null);

    final ChromatogramGroup group = compare(List.of(a), List.of(b1, b2)).getFirst();
    assertEquals(1, group.b().missingDataPoints());
    assertFalse(group.b().missingPeakTaken());
    assertTrue(group.issues().contains(ChromatogramIssue.MISSING_B), group.issues().toString());
    assertEquals(b2, group.b().takenBy().getFirst().chromatogram());
  }

  private static double @NotNull [] filled(int n, double value) {
    final double[] values = new double[n];
    Arrays.fill(values, value);
    return values;
  }

  @Test
  void groupsSortBySeverityThenHeight() {
    final List<ChromatogramGroup> groups = compare(
        List.of(chrom(A, 1, 200.1, 10, 2000, 5000, 2000), chrom(A, 2, 300.1, 10, 2000, 9000, 2000),
            chrom(A, 3, 400.1, 10, 2000, 9000, 2000)),
        List.of(chrom(B, 1, 400.1, 10, 2000, 9000, 2000)));
    final List<Integer> ids = groups.stream().sorted(ChromatogramGroup.SEVERITY_ORDER)
        .map(ChromatogramGroup::id).toList();
    // both only A, the more intense first, the group without issue last
    assertEquals(List.of(2, 1, 3), ids);
  }

  @Test
  void findClosestPrefersTheLowerIndexOnTies() {
    // binary fractions, so the tie is exact
    final double[] mzs = {100.0, 100 + 1 / 512d, 100 + 1 / 256d};
    assertEquals(0, ChromatogramComparison.findClosest(mzs, 100 + 1 / 1024d, TOLERANCE));
    assertEquals(2, ChromatogramComparison.findClosest(mzs, 100.0041, TOLERANCE));
    assertEquals(-1, ChromatogramComparison.findClosest(mzs, 100.1, TOLERANCE));
    assertEquals(-1, ChromatogramComparison.findClosest(new double[0], 100.0, TOLERANCE));
  }
}
