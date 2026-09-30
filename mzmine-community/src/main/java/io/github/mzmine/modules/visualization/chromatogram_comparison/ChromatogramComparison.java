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

import com.google.common.collect.Range;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Groups the chromatograms of two feature lists of the same raw data file by m/z and finds
 * potential issues, e.g., data points that one list misses. Used to compare chromatogram builders.
 */
public final class ChromatogramComparison {

  /**
   * Gaps of up to this number of scans between two signals above the threshold are holes. Longer
   * gaps usually separate peaks.
   */
  public static final int MAX_HOLE_SCANS = 5;

  /**
   * Both builders copy the data points of the same mass lists, so a data point of the same scan has
   * the same m/z in both lists.
   */
  static final double SAME_DATA_POINT_RELATIVE_MZ = 1E-7;

  /**
   * Split chromatograms of one ion: the other chromatogram has a data point within this number of
   * scans of the apex, at least the apex intensity divided by the factor. The defaults of the fast
   * chromatogram builder for complementary channels.
   */
  private static final int SPLIT_MAX_SCAN_DISTANCE = 4;
  private static final double SPLIT_INTENSITY_FACTOR = 5;
  /**
   * Min consecutive scans of a missing peak in another chromatogram if the filter of the builder is
   * unknown, the default of the chromatogram builders.
   */
  private static final int DEFAULT_MIN_CONSECUTIVE_SCANS = 5;

  private static final Comparator<ComparedChromatogram> BY_MZ = Comparator.comparingDouble(
      ComparedChromatogram::mz).thenComparingInt(ComparedChromatogram::rowId);
  private static final Comparator<ComparedChromatogram> BY_HEIGHT = Comparator.comparingDouble(
      ComparedChromatogram::height).reversed().thenComparing(BY_MZ);

  private ChromatogramComparison() {
  }

  /**
   * Compares without the filters of the builders.
   *
   * @see #compare(List, List, ComparisonScans, MZTolerance, double, MassListProbe, SegmentFilter,
   * SegmentFilter)
   */
  public static @NotNull List<ChromatogramGroup> compare(@NotNull List<ComparedChromatogram> a,
      @NotNull List<ComparedChromatogram> b, @NotNull ComparisonScans scans,
      @NotNull MZTolerance mzTolerance, double minSignalIntensity,
      @NotNull MassListProbe massListProbe) {
    return compare(a, b, scans, mzTolerance, minSignalIntensity, massListProbe, null, null);
  }

  /**
   * Each chromatogram is connected to the chromatogram of the other list with the closest m/z
   * within the tolerance, connected chromatograms form a group. A group has at least one
   * chromatogram and may have several of the same list, e.g., if one list splits a signal.
   *
   * @param a                  chromatograms of list A
   * @param b                  chromatograms of list B, scan indices of the same scans
   * @param mzTolerance        maximum m/z distance of the closest chromatogram of the other list
   * @param minSignalIntensity data points of at least this intensity are signals. Only signals
   *                           count as missing in the other list and holes are gaps between two
   *                           signals.
   * @param massListProbe      a gap scan is a hole if its mass list has a data point at the m/z of
   *                           the chromatogram
   * @param filterA            the filter of the builder of list A, a chromatogram of list B that
   *                           fails it is expected to be missing in A. Null if unknown.
   * @param filterB            the filter of the builder of list B
   * @return all groups sorted by m/z
   */
  public static @NotNull List<ChromatogramGroup> compare(@NotNull List<ComparedChromatogram> a,
      @NotNull List<ComparedChromatogram> b, @NotNull ComparisonScans scans,
      @NotNull MZTolerance mzTolerance, double minSignalIntensity,
      @NotNull MassListProbe massListProbe, @Nullable SegmentFilter filterA,
      @Nullable SegmentFilter filterB) {
    final List<ComparedChromatogram> sortedA = a.stream().sorted(BY_MZ).toList();
    final List<ComparedChromatogram> sortedB = b.stream().sorted(BY_MZ).toList();
    final int numA = sortedA.size();

    // nodes 0 to numA-1 are the chromatograms of A, followed by those of B
    final int[] parents = new int[numA + sortedB.size()];
    Arrays.setAll(parents, i -> i);
    connectClosest(sortedA, 0, sortedB, numA, mzTolerance, parents);
    connectClosest(sortedB, numA, sortedA, 0, mzTolerance, parents);

    // group members by their root, iteration in m/z order of the first member
    final Map<Integer, List<ComparedChromatogram>> membersA = new LinkedHashMap<>();
    final Map<Integer, List<ComparedChromatogram>> membersB = new LinkedHashMap<>();
    for (int node = 0; node < parents.length; node++) {
      final int root = findRoot(parents, node);
      membersA.computeIfAbsent(root, _ -> new ArrayList<>());
      membersB.computeIfAbsent(root, _ -> new ArrayList<>());
      if (node < numA) {
        membersA.get(root).add(sortedA.get(node));
      } else {
        membersB.get(root).add(sortedB.get(node - numA));
      }
    }

    final int[] usedBeforeA = scans.usedScansBefore(ComparisonSide.A);
    final int[] usedBeforeB = scans.usedScansBefore(ComparisonSide.B);
    final DataPointIndex indexA = new DataPointIndex(sortedA, scans.size());
    final DataPointIndex indexB = new DataPointIndex(sortedB, scans.size());
    final List<PendingGroup> groups = new ArrayList<>(membersA.size());
    for (final Integer root : membersA.keySet()) {
      groups.add(
          createGroup(membersA.get(root), membersB.get(root), scans, usedBeforeA, usedBeforeB,
              indexA, indexB, minSignalIntensity, massListProbe, filterA, filterB));
    }
    groups.sort(Comparator.comparingDouble((PendingGroup group) -> group.group().mz())
        .thenComparingDouble(group -> group.group().apexRt()));

    // the ids ascend with the m/z, the chromatograms that took missing data points refer to them
    final Map<ComparedChromatogram, Integer> groupIds = new IdentityHashMap<>();
    for (int i = 0; i < groups.size(); i++) {
      for (final ComparisonSide side : ComparisonSide.values()) {
        for (final ComparedChromatogram chromatogram : groups.get(i).group().side(side)
            .chromatograms()) {
          groupIds.put(chromatogram, i + 1);
        }
      }
    }
    final List<ChromatogramGroup> result = new ArrayList<>(groups.size());
    for (int i = 0; i < groups.size(); i++) {
      final PendingGroup pending = groups.get(i);
      final ChromatogramGroup group = pending.group();
      result.add(group.withSides(group.a().withTakenBy(owners(pending.missedA(), indexA, groupIds)),
          group.b().withTakenBy(owners(pending.missedB(), indexB, groupIds))).withId(i + 1));
    }
    return result;
  }

  /**
   * A group before the ids are known.
   *
   * @param missedA scan to m/z of the data points that A misses, see {@link DataPointOwner}
   * @param missedB same for B
   */
  private record PendingGroup(@NotNull ChromatogramGroup group,
                              @NotNull TreeMap<Integer, Double> missedA,
                              @NotNull TreeMap<Integer, Double> missedB) {

  }

  /**
   * @return the chromatograms of the list with the missed data points, most data points first,
   * unused data points last
   */
  @NotNull
  private static List<DataPointOwner> owners(@NotNull TreeMap<Integer, Double> missed,
      @NotNull DataPointIndex index, @NotNull Map<ComparedChromatogram, Integer> groupIds) {
    if (missed.isEmpty()) {
      return List.of();
    }
    // null collects the unused data points
    final Map<ComparedChromatogram, Integer> counts = new IdentityHashMap<>();
    for (final Entry<Integer, Double> dataPoint : missed.entrySet()) {
      counts.merge(index.find(dataPoint.getKey(), dataPoint.getValue()), 1, Integer::sum);
    }
    final List<DataPointOwner> owners = new ArrayList<>(counts.size());
    counts.forEach((chromatogram, count) -> owners.add(new DataPointOwner(chromatogram,
        chromatogram == null ? 0 : groupIds.getOrDefault(chromatogram, 0), count)));
    owners.sort(Comparator.comparing(DataPointOwner::isUnused)
        .thenComparing(DataPointOwner::count, Comparator.reverseOrder())
        .thenComparingInt(DataPointOwner::groupId)
        .thenComparingInt(owner -> owner.isUnused() ? 0 : owner.chromatogram().rowId()));
    return List.copyOf(owners);
  }

  /**
   * Connects each chromatogram to the closest chromatogram of the other list within the tolerance.
   */
  private static void connectClosest(@NotNull List<ComparedChromatogram> from, int fromOffset,
      @NotNull List<ComparedChromatogram> to, int toOffset, @NotNull MZTolerance mzTolerance,
      int @NotNull [] parents) {
    final double[] toMzs = to.stream().mapToDouble(ComparedChromatogram::mz).toArray();
    for (int i = 0; i < from.size(); i++) {
      final int closest = findClosest(toMzs, from.get(i).mz(), mzTolerance);
      if (closest >= 0) {
        union(parents, fromOffset + i, toOffset + closest);
      }
    }
  }

  /**
   * @param sortedMzs ascending
   * @return index of the closest m/z within the tolerance, the lower index on ties, -1 if none
   */
  static int findClosest(double @NotNull [] sortedMzs, double mz,
      @NotNull MZTolerance mzTolerance) {
    // first index with an m/z of at least mz
    int low = 0;
    int high = sortedMzs.length;
    while (low < high) {
      final int mid = (low + high) >>> 1;
      if (sortedMzs[mid] < mz) {
        low = mid + 1;
      } else {
        high = mid;
      }
    }
    int closest = -1;
    if (low > 0) {
      closest = low - 1;
    }
    if (low < sortedMzs.length && (closest < 0 || sortedMzs[low] - mz < mz - sortedMzs[closest])) {
      closest = low;
    }
    if (closest < 0 || !mzTolerance.checkWithinTolerance(mz, sortedMzs[closest])) {
      return -1;
    }
    return closest;
  }

  private static int findRoot(int @NotNull [] parents, int node) {
    int root = node;
    while (parents[root] != root) {
      root = parents[root];
    }
    // path compression
    while (parents[node] != root) {
      final int next = parents[node];
      parents[node] = root;
      node = next;
    }
    return root;
  }

  private static void union(int @NotNull [] parents, int x, int y) {
    final int rootX = findRoot(parents, x);
    final int rootY = findRoot(parents, y);
    // the lower node stays root, keeps the order of the groups independent of the union order
    if (rootX < rootY) {
      parents[rootY] = rootX;
    } else if (rootY < rootX) {
      parents[rootX] = rootY;
    }
  }

  @NotNull
  private static PendingGroup createGroup(@NotNull List<ComparedChromatogram> membersA,
      @NotNull List<ComparedChromatogram> membersB, @NotNull ComparisonScans scans,
      int @NotNull [] usedBeforeA, int @NotNull [] usedBeforeB, @NotNull DataPointIndex indexA,
      @NotNull DataPointIndex indexB, double minSignalIntensity,
      @NotNull MassListProbe massListProbe, @Nullable SegmentFilter filterA,
      @Nullable SegmentFilter filterB) {
    final List<ComparedChromatogram> chromatogramsA = membersA.stream().sorted(BY_HEIGHT).toList();
    final List<ComparedChromatogram> chromatogramsB = membersB.stream().sorted(BY_HEIGHT).toList();
    final Signal signalA = Signal.union(chromatogramsA);
    final Signal signalB = Signal.union(chromatogramsB);

    final TreeMap<Integer, Double> missedA = new TreeMap<>();
    final TreeMap<Integer, Double> missedB = new TreeMap<>();
    final GroupSide a = analyzeSide(ComparisonSide.A, chromatogramsA, signalA, signalB,
        !chromatogramsB.isEmpty(), scans, usedBeforeA, indexA, minConsecutiveScans(filterA),
        minSignalIntensity, massListProbe, missedA);
    final GroupSide b = analyzeSide(ComparisonSide.B, chromatogramsB, signalB, signalA,
        !chromatogramsA.isEmpty(), scans, usedBeforeB, indexB, minConsecutiveScans(filterB),
        minSignalIntensity, massListProbe, missedB);
    final int differentDataPoints = countDifferentDataPoints(signalA, signalB, minSignalIntensity);

    final EnumSet<ChromatogramIssue> issues = EnumSet.noneOf(ChromatogramIssue.class);
    for (final GroupSide side : List.of(a, b)) {
      final ComparisonSide s = side.side();
      final boolean isA = s == ComparisonSide.A;
      if (!side.isEmpty() && (isA ? b : a).isEmpty()) {
        // decision: the other builder drops chromatograms that fail its filter, expected
        final SegmentFilter otherFilter = isA ? filterB : filterA;
        final int[] otherUsedBefore = isA ? usedBeforeB : usedBeforeA;
        final boolean lowSegment = otherFilter != null && side.chromatograms().stream()
            .noneMatch(chromatogram -> otherFilter.passes(chromatogram, otherUsedBefore));
        issues.add(lowSegment ? ChromatogramIssue.onlyLowSegment(s) : ChromatogramIssue.only(s));
      }
      if (side.missingDataPoints() > 0) {
        // decision: a missing peak that another chromatogram of the list holds is resolved there,
        // e.g., the fast builder joins a separate peak into the channel of an intense neighbor
        issues.add(
            side.missingPeakTaken() ? ChromatogramIssue.taken(s) : ChromatogramIssue.missing(s));
      }
      if (side.holeDataPoints() > 0) {
        issues.add(ChromatogramIssue.holes(s));
      }
      if (side.chromatograms().size() > 1) {
        issues.add(isSplit(side.chromatograms()) ? ChromatogramIssue.split(s)
            : ChromatogramIssue.multiple(s));
      }
    }
    if (differentDataPoints > 0) {
      issues.add(ChromatogramIssue.DIFFERENT_DATA_POINTS);
    }

    final Double mzA = a.mz();
    final Double mzB = b.mz();
    final double mz = mzA == null ? mzB : mzB == null ? mzA : (mzA + mzB) / 2;

    final int apexScan =
        signalA.maxIntensity() >= signalB.maxIntensity() ? signalA.apexScan() : signalB.apexScan();
    final float apexRt = apexScan < 0 ? Float.NaN : scans.rt(apexScan);

    return new PendingGroup(new ChromatogramGroup(0, mz, apexRt, a, b, differentDataPoints,
        Collections.unmodifiableSet(issues),
        signalRtRange(signalA, signalB, a, b, scans, minSignalIntensity)), missedA, missedB);
  }

  private static int minConsecutiveScans(@Nullable SegmentFilter filter) {
    return filter == null ? DEFAULT_MIN_CONSECUTIVE_SCANS : filter.minConsecutiveScans();
  }

  /**
   * @param ownIndex            the data points of all chromatograms of the list of this side
   * @param minConsecutiveScans the missing peak is taken if another chromatogram holds it in this
   *                            number of consecutive scans
   * @param missed              receives scan to m/z of the data points that this side misses: data
   *                            points of the mass lists in holes, signals of the other side in
   *                            scans without own data point, or all signals of the other side if
   *                            this side is empty
   */
  @NotNull
  private static GroupSide analyzeSide(@NotNull ComparisonSide side,
      @NotNull List<ComparedChromatogram> chromatograms, @NotNull Signal own, @NotNull Signal other,
      boolean otherHasChromatograms, @NotNull ComparisonScans scans, int @NotNull [] usedBefore,
      @NotNull DataPointIndex ownIndex, int minConsecutiveScans, double minSignalIntensity,
      @NotNull MassListProbe massListProbe, @NotNull TreeMap<Integer, Double> missed) {
    // scans without data point -> highest intensity of the other side, 0 without data point
    final TreeMap<Integer, Double> missingScans = new TreeMap<>();

    int holeDataPoints = 0;
    for (int i = 0; i + 1 < own.size(); i++) {
      final int left = own.scans[i];
      final int right = own.scans[i + 1];
      final int gap = usedBefore[right] - usedBefore[left + 1];
      if (gap < 1 || gap > MAX_HOLE_SCANS || own.intensities[i] < minSignalIntensity
          || own.intensities[i + 1] < minSignalIntensity) {
        continue;
      }
      // a sparse signal is no hole, the builder can only use data points of the mass list
      final double mz = (own.mzs[i] + own.mzs[i + 1]) / 2;
      for (int scan = left + 1; scan < right; scan++) {
        final double found =
            scans.isUsedBy(side, scan) ? massListProbe.findMz(scan, mz) : Double.NaN;
        if (!Double.isNaN(found)) {
          holeDataPoints++;
          missingScans.put(scan, other.intensityAt(scan));
          missed.put(scan, found);
        }
      }
    }

    int missingDataPoints = 0;
    double maxMissedIntensity = 0;
    // index in the other signal of the most intense missing data point
    int missedApex = -1;
    boolean allMissingTaken = true;
    if (otherHasChromatograms) {
      for (int i = 0; i < other.size(); i++) {
        final int scan = other.scans[i];
        final double intensity = other.intensities[i];
        if (intensity < minSignalIntensity || !scans.isUsedBy(side, scan) || own.contains(scan)) {
          continue;
        }
        // the signal of the other side is the data point that this side misses
        missed.put(scan, other.mzs[i]);
        if (!chromatograms.isEmpty()) {
          missingDataPoints++;
          if (intensity > maxMissedIntensity) {
            maxMissedIntensity = intensity;
            missedApex = i;
          }
          missingScans.put(scan, intensity);
          allMissingTaken &= ownIndex.find(scan, other.mzs[i]) != null;
        }
      }
    }
    final boolean missingPeakTaken = missingDataPoints > 0 && allMissingTaken
        && takenRun(other, missedApex, ownIndex, usedBefore) >= minConsecutiveScans;

    return new GroupSide(side, chromatograms, own.size(), missingDataPoints, maxMissedIntensity,
        missingPeakTaken, holeDataPoints, toRegions(side, missingScans, scans, minSignalIntensity),
        List.of());
  }

  /**
   * @param apex index of a data point of the other signal that a chromatogram of the own list
   *             holds
   * @return the number of consecutive scans of the builder around the apex in which the same
   * chromatogram holds the data points of the other signal
   */
  private static int takenRun(@NotNull Signal other, int apex, @NotNull DataPointIndex ownIndex,
      int @NotNull [] usedBefore) {
    final ComparedChromatogram taker = ownIndex.find(other.scans[apex], other.mzs[apex]);
    int first = apex;
    while (first > 0 && isNextUsedScan(other.scans[first - 1], other.scans[first], usedBefore)
        && ownIndex.find(other.scans[first - 1], other.mzs[first - 1]) == taker) {
      first--;
    }
    int last = apex;
    while (last + 1 < other.size() && isNextUsedScan(other.scans[last], other.scans[last + 1],
        usedBefore) && ownIndex.find(other.scans[last + 1], other.mzs[last + 1]) == taker) {
      last++;
    }
    return last - first + 1;
  }

  /**
   * @return true if the builder used no scan between both scans
   */
  private static boolean isNextUsedScan(int scan, int next, int @NotNull [] usedBefore) {
    return usedBefore[next] - usedBefore[scan + 1] == 0;
  }

  /**
   * @param byHeight the chromatograms of one side of a group, most intense first
   * @return true if a chromatogram continues another one, see {@link #continuesApex}
   */
  static boolean isSplit(@NotNull List<ComparedChromatogram> byHeight) {
    for (int weaker = 1; weaker < byHeight.size(); weaker++) {
      for (int stronger = 0; stronger < weaker; stronger++) {
        if (continuesApex(byHeight.get(weaker), byHeight.get(stronger))) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * The rule of the fast chromatogram builder for complementary channels: one ion yields one data
   * point per scan, so two chromatograms that never share a scan are parts of one ion if the other
   * continues the intensity next to the apex of the chromatogram. Otherwise, the chromatogram is a
   * peak of its own.
   */
  private static boolean continuesApex(@NotNull ComparedChromatogram chromatogram,
      @NotNull ComparedChromatogram other) {
    final int[] scans = chromatogram.scans();
    int apex = 0;
    for (int i = 1; i < scans.length; i++) {
      if (chromatogram.intensities()[i] > chromatogram.intensities()[apex]) {
        apex = i;
      }
    }
    final int apexScan = scans[apex];
    final double minIntensity = chromatogram.intensities()[apex] / SPLIT_INTENSITY_FACTOR;
    final int[] otherScans = other.scans();
    int k = Arrays.binarySearch(otherScans, apexScan - SPLIT_MAX_SCAN_DISTANCE);
    boolean touch = false;
    for (k = k >= 0 ? k : -k - 1;
        k < otherScans.length && otherScans[k] <= apexScan + SPLIT_MAX_SCAN_DISTANCE; k++) {
      if (otherScans[k] == apexScan) {
        return false;
      }
      touch |= other.intensities()[k] >= minIntensity;
    }
    if (!touch) {
      return false;
    }
    // never share a scan
    int i = 0;
    int j = 0;
    while (i < scans.length && j < otherScans.length) {
      if (scans[i] == otherScans[j]) {
        return false;
      }
      if (scans[i] < otherScans[j]) {
        i++;
      } else {
        j++;
      }
    }
    return true;
  }

  /**
   * Joins consecutive missing scans
   */
  @NotNull
  private static List<MissingRegion> toRegions(@NotNull ComparisonSide side,
      @NotNull TreeMap<Integer, Double> missingScans, @NotNull ComparisonScans scans,
      double minSignalIntensity) {
    final List<MissingRegion> regions = new ArrayList<>();
    int first = -1;
    int last = -1;
    double otherMax = 0;
    for (final Entry<Integer, Double> entry : missingScans.entrySet()) {
      final int scan = entry.getKey();
      if (first >= 0 && scan == last + 1) {
        last = scan;
        otherMax = Math.max(otherMax, entry.getValue());
        continue;
      }
      if (first >= 0) {
        regions.add(createRegion(side, first, last, otherMax, scans, minSignalIntensity));
      }
      first = scan;
      last = scan;
      otherMax = entry.getValue();
    }
    if (first >= 0) {
      regions.add(createRegion(side, first, last, otherMax, scans, minSignalIntensity));
    }
    return regions;
  }

  @NotNull
  private static MissingRegion createRegion(@NotNull ComparisonSide side, int first, int last,
      double otherMax, @NotNull ComparisonScans scans, double minSignalIntensity) {
    final float rtFirst = scans.rt(first);
    final float rtLast = scans.rt(last);
    final float rtStart = first > 0 ? (scans.rt(first - 1) + rtFirst) / 2 : rtFirst;
    final float rtEnd = last + 1 < scans.size() ? (rtLast + scans.rt(last + 1)) / 2 : rtLast;
    return new MissingRegion(side, first, last, rtFirst, rtLast, rtStart, rtEnd, otherMax,
        otherMax > 0 && otherMax >= minSignalIntensity);
  }

  private static int countDifferentDataPoints(@NotNull Signal a, @NotNull Signal b,
      double minSignalIntensity) {
    int different = 0;
    int i = 0;
    int j = 0;
    while (i < a.size() && j < b.size()) {
      if (a.scans[i] < b.scans[j]) {
        i++;
      } else if (b.scans[j] < a.scans[i]) {
        j++;
      } else {
        final double mzA = a.mzs[i];
        final boolean same = Math.abs(mzA - b.mzs[j]) <= SAME_DATA_POINT_RELATIVE_MZ * mzA;
        if (!same && Math.max(a.intensities[i], b.intensities[j]) >= minSignalIntensity) {
          different++;
        }
        i++;
        j++;
      }
    }
    return different;
  }

  /**
   * @return range of the signals and missing data points, or of all data points if no data point
   * reaches the threshold, null without data points
   */
  private static Range<Float> signalRtRange(@NotNull Signal signalA, @NotNull Signal signalB,
      @NotNull GroupSide a, @NotNull GroupSide b, @NotNull ComparisonScans scans,
      double minSignalIntensity) {
    float min = Float.POSITIVE_INFINITY;
    float max = Float.NEGATIVE_INFINITY;
    for (final Signal signal : List.of(signalA, signalB)) {
      for (int i = 0; i < signal.size(); i++) {
        if (signal.intensities[i] >= minSignalIntensity) {
          min = Math.min(min, scans.rt(signal.scans[i]));
          max = Math.max(max, scans.rt(signal.scans[i]));
        }
      }
    }
    for (final GroupSide side : List.of(a, b)) {
      for (final MissingRegion region : side.missingRegions()) {
        min = Math.min(min, region.rtFirst());
        max = Math.max(max, region.rtLast());
      }
    }
    if (min > max) {
      // no signal above the threshold, use all data points
      for (final Signal signal : List.of(signalA, signalB)) {
        if (signal.size() > 0) {
          min = Math.min(min, scans.rt(signal.scans[0]));
          max = Math.max(max, scans.rt(signal.scans[signal.size() - 1]));
        }
      }
    }
    return min > max ? null : Range.closed(min, max);
  }

  /**
   * The data points of all chromatograms of one side, the highest data point of each scan.
   */
  private record Signal(int @NotNull [] scans, double @NotNull [] intensities,
                        double @NotNull [] mzs) {

    private static final Signal EMPTY = new Signal(new int[0], new double[0], new double[0]);

    @NotNull
    static Signal union(@NotNull List<ComparedChromatogram> chromatograms) {
      Signal union = EMPTY;
      for (final ComparedChromatogram chromatogram : chromatograms) {
        final Signal signal = new Signal(chromatogram.scans(), chromatogram.intensities(),
            chromatogram.mzs());
        union = union.size() == 0 ? signal : merge(union, signal);
      }
      return union;
    }

    @NotNull
    private static Signal merge(@NotNull Signal x, @NotNull Signal y) {
      final int capacity = x.size() + y.size();
      final int[] scans = new int[capacity];
      final double[] intensities = new double[capacity];
      final double[] mzs = new double[capacity];
      int i = 0;
      int j = 0;
      int n = 0;
      while (i < x.size() || j < y.size()) {
        final boolean takeX;
        if (j >= y.size()) {
          takeX = true;
        } else if (i >= x.size()) {
          takeX = false;
        } else if (x.scans[i] != y.scans[j]) {
          takeX = x.scans[i] < y.scans[j];
        } else {
          // same scan, keep the higher data point
          takeX = x.intensities[i] >= y.intensities[j];
          if (takeX) {
            j++;
          } else {
            i++;
          }
        }
        if (takeX) {
          scans[n] = x.scans[i];
          intensities[n] = x.intensities[i];
          mzs[n++] = x.mzs[i++];
        } else {
          scans[n] = y.scans[j];
          intensities[n] = y.intensities[j];
          mzs[n++] = y.mzs[j++];
        }
      }
      return new Signal(Arrays.copyOf(scans, n), Arrays.copyOf(intensities, n),
          Arrays.copyOf(mzs, n));
    }

    int size() {
      return scans.length;
    }

    boolean contains(int scan) {
      return Arrays.binarySearch(scans, scan) >= 0;
    }

    /**
     * @return the intensity in this scan, 0 without data point
     */
    double intensityAt(int scan) {
      final int index = Arrays.binarySearch(scans, scan);
      return index >= 0 ? intensities[index] : 0d;
    }

    double maxIntensity() {
      double max = 0;
      for (final double intensity : intensities) {
        max = Math.max(max, intensity);
      }
      return max;
    }

    /**
     * @return scan of the highest data point, -1 if empty
     */
    int apexScan() {
      int apex = -1;
      for (int i = 0; i < size(); i++) {
        if (apex < 0 || intensities[i] > intensities[apex]) {
          apex = i;
        }
      }
      return apex < 0 ? -1 : scans[apex];
    }
  }
}
