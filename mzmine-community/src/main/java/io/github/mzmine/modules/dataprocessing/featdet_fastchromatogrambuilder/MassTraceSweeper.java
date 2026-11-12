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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Streaming mass trace detection in a single loop over the scans. Scans are processed in retention
 * time order. Each scan is merged against the active traces, which are kept sorted by their
 * intensity weighted center m/z, so a scan costs O(data points + active traces) and no global sort
 * of all data points is needed.
 * <p>
 * A data point joins the active trace with the lowest matching cost within the m/z tolerance and
 * every trace receives at most one data point per scan. Conflicts, i.e., multiple data points and
 * traces within the tolerance of each other, are resolved by a greedy matching on the cost, so a
 * data point that loses a trace can still join the next best trace. Data points without a trace
 * start a new trace. Traces without a data point for more than {@link #getMaxGapScans()} scans are
 * closed, traces with a single data point already after one scan without data point.
 * <p>
 * The sweeper can record the trace of every data point in {@link TraceAssignments}, the second pass
 * of the {@link FastChromatogramBuilder} routes the data points with them instead of detecting the
 * traces again.
 * <p>
 * Contract: the sweeper is deterministic. Feeding the same scans to a new instance yields identical
 * traces, trace ids and listener events.
 */
final class MassTraceSweeper {

  private static final int NONE = -1;
  private static final int INITIAL_CAPACITY = 1024;

  private final double absoluteTolerance;
  private final double relativeTolerance;
  private final int maxGapScans;
  private final int singleDataPointMaxGapScans;
  private final double intensityJumpWeight;
  private final double logIntensityJumpFactor;
  private final MassTraceSweepListener listener;
  private final boolean trackCollisions;
  private final double collisionToleranceFactor;

  // trace statistics per slot (struct of arrays), slots of closed traces are reused
  private long[] slotTraceId;
  private double[] slotSumIntensity;
  private double[] slotSumMzIntensity;
  private double[] slotCenter;
  private double[] slotLastIntensity;
  private double[] slotMaxIntensity;
  private int[] slotFirstScan;
  private int[] slotLastScan;
  private int[] slotCount;
  private int usedSlots = 0;
  private int[] freeSlots;
  private int numFreeSlots = 0;

  // active traces sorted by center m/z. Center and the last scan index before the trace is closed
  // are kept in parallel arrays, so the per scan maintenance reads sequential memory only.
  private int[] activeSlot;
  private double[] activeCenter;
  private int[] activeExpiry;
  private int numActive = 0;
  // data point index of the current scan assigned to an active position, or NONE
  private int[] posDp;

  // current scan data, only valid data points sorted by m/z
  private double[] mzs;
  private double[] intensities;
  private double[] dpTolerance;
  // candidate traces of a data point are the active positions [dpLo, dpHi)
  private int[] dpLo;
  private int[] dpHi;
  // assigned active position of a data point or NONE
  private int[] dpPos;
  private int[] newSlots;
  private int numDps = 0;

  // candidate pairs of one conflict component
  private int[] pairDp = new int[16];
  private int[] pairPos = new int[16];
  private double[] pairCost = new double[16];
  private int[] pairOrder = new int[16];
  private int numPairs = 0;
  // cheapest pair first, ties by data point and trace position to stay deterministic
  private final IntComparator pairComparator = (a, b) -> {
    int result = Double.compare(pairCost[a], pairCost[b]);
    if (result != 0) {
      return result;
    }
    result = Integer.compare(pairDp[a], pairDp[b]);
    if (result != 0) {
      return result;
    }
    return Integer.compare(pairPos[a], pairPos[b]);
  };

  private long nextTraceId = 0;
  private int scanIndex = -1;
  private int maxActiveTraces = 0;

  private final ScanDataPoints points = new ScanDataPoints();
  // the trace of each data point for the second pass, null if not recorded
  private final @Nullable TraceAssignments assignments;
  private int[] dpCode;
  private int maxCode = 0;

  /**
   * @param tolerance m/z tolerance between a data point and the center of a trace
   * @param options   gaps, matching cost and collision window, see
   *                  {@link FastChromatogramBuilderOptions}
   * @param listener  receives the trace events
   */
  MassTraceSweeper(@NotNull MZTolerance tolerance, @NotNull FastChromatogramBuilderOptions options,
      @NotNull MassTraceSweepListener listener) {
    this(tolerance, options, listener, null);
  }

  /**
   * @param assignments receives the trace of every data point, may be null
   */
  MassTraceSweeper(@NotNull MZTolerance tolerance, @NotNull FastChromatogramBuilderOptions options,
      @NotNull MassTraceSweepListener listener, @Nullable TraceAssignments assignments) {
    this.assignments = assignments;
    if (!(options.intensityJumpFactor() >= 1d)) {
      throw new IllegalArgumentException("intensityJumpFactor must be >= 1");
    }
    this.collisionToleranceFactor = options.collisionToleranceFactor();
    this.absoluteTolerance = tolerance.getMzTolerance();
    this.relativeTolerance = tolerance.getPpmTolerance() / 1E6;
    this.maxGapScans = options.maxGapScans();
    this.singleDataPointMaxGapScans = Math.min(maxGapScans, options.singleDataPointMaxGapScans());
    this.intensityJumpWeight = options.intensityJumpWeight();
    this.logIntensityJumpFactor = Math.log(options.intensityJumpFactor());
    this.listener = listener;
    this.trackCollisions = listener.isCollisionTrackingEnabled();

    slotTraceId = new long[INITIAL_CAPACITY];
    slotSumIntensity = new double[INITIAL_CAPACITY];
    slotSumMzIntensity = new double[INITIAL_CAPACITY];
    slotCenter = new double[INITIAL_CAPACITY];
    slotLastIntensity = new double[INITIAL_CAPACITY];
    slotMaxIntensity = new double[INITIAL_CAPACITY];
    slotFirstScan = new int[INITIAL_CAPACITY];
    slotLastScan = new int[INITIAL_CAPACITY];
    slotCount = new int[INITIAL_CAPACITY];
    freeSlots = new int[INITIAL_CAPACITY];

    activeSlot = new int[INITIAL_CAPACITY];
    activeCenter = new double[INITIAL_CAPACITY];
    activeExpiry = new int[INITIAL_CAPACITY];
    posDp = new int[INITIAL_CAPACITY];

    dpTolerance = new double[INITIAL_CAPACITY];
    dpLo = new int[INITIAL_CAPACITY];
    dpHi = new int[INITIAL_CAPACITY];
    dpPos = new int[INITIAL_CAPACITY];
    newSlots = new int[INITIAL_CAPACITY];
    dpCode = new int[INITIAL_CAPACITY];
  }

  /**
   * Loads the current scan of scans and assigns its data points to traces.
   */
  void processScan(@NotNull MzIntensityScans scans) {
    scanIndex++;
    loadScan(scans);
    findCandidates();
    matchDataPoints();
    if (trackCollisions) {
      reportCollisions();
    }
    maxCode = 0;
    updateAssignedTraces();
    final int numNew = startNewTraces();
    rebuildActiveTraces(numNew);
    if (assignments != null) {
      assignments.addScan(dpCode, numDps, maxCode);
    }
    listener.onScanFinished(this, scanIndex);
  }

  /**
   * Closes all remaining active traces. Call after the last scan.
   */
  void finish() {
    for (int p = 0; p < numActive; p++) {
      closeTrace(activeSlot[p]);
    }
    numActive = 0;
  }

  /**
   * Same as {@link MZTolerance#getMzToleranceForMass(double)} without the division.
   */
  private double toleranceFor(double mz) {
    return Math.max(absoluteTolerance, mz * relativeTolerance);
  }

  private void loadScan(@NotNull MzIntensityScans scans) {
    points.load(scans);
    numDps = points.size();
    mzs = points.mzs();
    intensities = points.intensities();
    ensureScanCapacity(numDps);
  }

  /**
   * Candidate traces of each data point are the active traces with a center within the tolerance.
   * Both the data points and the active traces are sorted by m/z and the window bounds increase
   * with m/z, so two pointers find all windows in linear time.
   */
  private void findCandidates() {
    Arrays.fill(posDp, 0, numActive, NONE);
    int lo = 0;
    for (int i = 0; i < numDps; i++) {
      final double mz = mzs[i];
      final double tol = toleranceFor(mz);
      dpTolerance[i] = tol;
      final double min = mz - tol;
      while (lo < numActive && activeCenter[lo] < min) {
        lo++;
      }
      final double max = mz + tol;
      int hi = lo;
      while (hi < numActive && activeCenter[hi] <= max) {
        hi++;
      }
      dpLo[i] = lo;
      dpHi[i] = hi;
      dpPos[i] = NONE;
    }
  }

  /**
   * Splits the data points into components of overlapping candidate windows. Components are
   * independent and usually contain a single data point and a single trace.
   */
  private void matchDataPoints() {
    int i = 0;
    while (i < numDps) {
      if (dpLo[i] == dpHi[i]) {
        i++;
        continue;
      }
      final int first = i;
      int last = i;
      int componentHi = dpHi[i];
      for (int j = i + 1; j < numDps; j++) {
        if (dpLo[j] == dpHi[j]) {
          continue;
        }
        if (dpLo[j] >= componentHi) {
          break;
        }
        componentHi = Math.max(componentHi, dpHi[j]);
        last = j;
      }
      resolveComponent(first, last);
      i = last + 1;
    }
  }

  private void resolveComponent(int first, int last) {
    // fast path: one data point with one candidate trace
    if (first == last && dpHi[first] - dpLo[first] == 1) {
      assign(first, dpLo[first]);
      return;
    }

    numPairs = 0;
    for (int d = first; d <= last; d++) {
      for (int p = dpLo[d]; p < dpHi[d]; p++) {
        addPair(d, p, matchCost(d, p));
      }
    }
    for (int k = 0; k < numPairs; k++) {
      pairOrder[k] = k;
    }
    IntArrays.quickSort(pairOrder, 0, numPairs, pairComparator);
    for (int k = 0; k < numPairs; k++) {
      final int pair = pairOrder[k];
      final int d = pairDp[pair];
      final int p = pairPos[pair];
      if (dpPos[d] == NONE && posDp[p] == NONE) {
        assign(d, p);
      }
    }
  }

  /**
   * Squared m/z distance relative to the tolerance. Optionally, intensity jumps beyond the
   * tolerated factor add a cost because chromatographic peaks change smoothly between scans. This
   * keeps a noise signal close to the center from replacing the actual signal of a trace and keeps
   * a strong new ion from taking over the trace of a weak neighbor within the tolerance.
   */
  private double matchCost(int d, int p) {
    final double distance = Math.abs(mzs[d] - activeCenter[p]) / dpTolerance[d];
    double cost = distance * distance;
    if (intensityJumpWeight > 0d) {
      final double logRatio = Math.abs(Math.log(intensities[d] / slotLastIntensity[activeSlot[p]]));
      final double excess = logRatio - logIntensityJumpFactor;
      if (excess > 0d) {
        cost += intensityJumpWeight * excess * excess;
      }
    }
    return cost;
  }

  private void addPair(int d, int p, double cost) {
    if (numPairs == pairDp.length) {
      final int capacity = numPairs * 2;
      pairDp = Arrays.copyOf(pairDp, capacity);
      pairPos = Arrays.copyOf(pairPos, capacity);
      pairCost = Arrays.copyOf(pairCost, capacity);
      pairOrder = Arrays.copyOf(pairOrder, capacity);
    }
    pairDp[numPairs] = d;
    pairPos[numPairs] = p;
    pairCost[numPairs] = cost;
    numPairs++;
  }

  private void assign(int d, int p) {
    dpPos[d] = p;
    posDp[p] = d;
  }

  /**
   * Reports pairs of active traces within the collision tolerance that both received a data point.
   * Uses the centers before this scan. Traces started in this scan are not active yet and never
   * collide.
   */
  private void reportCollisions() {
    for (int p = 0; p < numActive; p++) {
      if (posDp[p] == NONE) {
        continue;
      }
      final double center = activeCenter[p];
      final double max = center + collisionToleranceFactor * toleranceFor(center);
      for (int q = p + 1; q < numActive && activeCenter[q] <= max; q++) {
        if (posDp[q] != NONE) {
          listener.onCollision(this, activeSlot[p], activeSlot[q]);
        }
      }
    }
  }

  /**
   * Adds the assigned data points. The centers are updated in place and may leave the m/z order by
   * a tiny amount, which is restored in {@link #rebuildActiveTraces(int)}.
   */
  private void updateAssignedTraces() {
    for (int d = 0; d < numDps; d++) {
      final int p = dpPos[d];
      if (p == NONE) {
        continue;
      }
      final int slot = activeSlot[p];
      addDataPoint(slot, mzs[d], intensities[d]);
      activeCenter[p] = slotCenter[slot];
      // the trace has at least two data points now
      activeExpiry[p] = scanIndex + maxGapScans;
      setCode(d, slot, false);
      listener.onDataPointAssigned(this, slot, scanIndex, mzs[d], intensities[d]);
    }
  }

  private void setCode(int d, int slot, boolean newTrace) {
    final int code = TraceAssignments.code(slot, newTrace);
    dpCode[d] = code;
    if (code > maxCode) {
      maxCode = code;
    }
  }

  /**
   * @return number of new traces in {@link #newSlots}, sorted by m/z
   */
  private int startNewTraces() {
    int numNew = 0;
    for (int d = 0; d < numDps; d++) {
      if (dpPos[d] != NONE) {
        continue;
      }
      final int slot = allocateSlot();
      slotTraceId[slot] = nextTraceId++;
      slotSumIntensity[slot] = 0d;
      slotSumMzIntensity[slot] = 0d;
      slotMaxIntensity[slot] = 0d;
      slotFirstScan[slot] = scanIndex;
      slotCount[slot] = 0;
      listener.onTraceCreated(this, slot);
      addDataPoint(slot, mzs[d], intensities[d]);
      setCode(d, slot, true);
      listener.onDataPointAssigned(this, slot, scanIndex, mzs[d], intensities[d]);
      newSlots[numNew++] = slot;
    }
    return numNew;
  }

  private void addDataPoint(int slot, double mz, double intensity) {
    slotSumIntensity[slot] += intensity;
    slotSumMzIntensity[slot] += mz * intensity;
    slotCenter[slot] = slotSumMzIntensity[slot] / slotSumIntensity[slot];
    slotLastIntensity[slot] = intensity;
    if (intensity > slotMaxIntensity[slot]) {
      slotMaxIntensity[slot] = intensity;
    }
    slotLastScan[slot] = scanIndex;
    slotCount[slot]++;
  }

  /**
   * Closes stale traces, restores the m/z order after the center updates and merges the new traces,
   * all in place.
   */
  private void rebuildActiveTraces(int numNew) {
    ensureActiveCapacity(numActive + numNew);
    // compaction, the write position never passes the read position
    int m = 0;
    for (int p = 0; p < numActive; p++) {
      final int slot = activeSlot[p];
      final int expiry = activeExpiry[p];
      if (scanIndex > expiry) {
        closeTrace(slot);
        continue;
      }
      // insertion sort step, centers of neighboring traces may have crossed by a tiny amount
      final double center = activeCenter[p];
      int k = m;
      while (k > 0 && activeCenter[k - 1] > center) {
        activeSlot[k] = activeSlot[k - 1];
        activeCenter[k] = activeCenter[k - 1];
        activeExpiry[k] = activeExpiry[k - 1];
        k--;
      }
      activeSlot[k] = slot;
      activeCenter[k] = center;
      activeExpiry[k] = expiry;
      m++;
    }

    // merge the new traces from the back, equal centers keep the existing trace first
    int i = m - 1;
    int j = numNew - 1;
    int out = m + numNew - 1;
    final int newExpiry = scanIndex + singleDataPointMaxGapScans;
    while (j >= 0) {
      final int newSlot = newSlots[j];
      final double newCenter = slotCenter[newSlot];
      if (i >= 0 && activeCenter[i] > newCenter) {
        activeSlot[out] = activeSlot[i];
        activeCenter[out] = activeCenter[i];
        activeExpiry[out] = activeExpiry[i];
        i--;
      } else {
        activeSlot[out] = newSlot;
        activeCenter[out] = newCenter;
        activeExpiry[out] = newExpiry;
        j--;
      }
      out--;
    }
    numActive = m + numNew;
    maxActiveTraces = Math.max(maxActiveTraces, numActive);
  }

  private void closeTrace(int slot) {
    listener.onTraceClosed(this, slot);
    if (numFreeSlots == freeSlots.length) {
      freeSlots = Arrays.copyOf(freeSlots, freeSlots.length * 2);
    }
    freeSlots[numFreeSlots++] = slot;
  }

  private int allocateSlot() {
    if (numFreeSlots > 0) {
      return freeSlots[--numFreeSlots];
    }
    if (usedSlots == slotTraceId.length) {
      final int capacity = grow(usedSlots);
      slotTraceId = Arrays.copyOf(slotTraceId, capacity);
      slotSumIntensity = Arrays.copyOf(slotSumIntensity, capacity);
      slotSumMzIntensity = Arrays.copyOf(slotSumMzIntensity, capacity);
      slotCenter = Arrays.copyOf(slotCenter, capacity);
      slotLastIntensity = Arrays.copyOf(slotLastIntensity, capacity);
      slotMaxIntensity = Arrays.copyOf(slotMaxIntensity, capacity);
      slotFirstScan = Arrays.copyOf(slotFirstScan, capacity);
      slotLastScan = Arrays.copyOf(slotLastScan, capacity);
      slotCount = Arrays.copyOf(slotCount, capacity);
    }
    return usedSlots++;
  }

  private void ensureScanCapacity(int n) {
    if (dpPos.length >= n) {
      return;
    }
    final int capacity = Math.max(n, grow(dpPos.length));
    dpTolerance = new double[capacity];
    dpLo = new int[capacity];
    dpHi = new int[capacity];
    dpPos = new int[capacity];
    newSlots = new int[capacity];
    dpCode = new int[capacity];
  }

  private void ensureActiveCapacity(int n) {
    if (activeSlot.length >= n) {
      return;
    }
    final int capacity = Math.max(n, grow(activeSlot.length));
    activeSlot = Arrays.copyOf(activeSlot, capacity);
    activeCenter = Arrays.copyOf(activeCenter, capacity);
    activeExpiry = Arrays.copyOf(activeExpiry, capacity);
    posDp = new int[capacity];
  }

  private static int grow(int size) {
    return Math.max(16, size + (size >> 1));
  }

  long getTraceId(int slot) {
    return slotTraceId[slot];
  }

  double getCenter(int slot) {
    return slotCenter[slot];
  }

  double getSumIntensity(int slot) {
    return slotSumIntensity[slot];
  }

  double getSumMzIntensity(int slot) {
    return slotSumMzIntensity[slot];
  }

  double getMaxIntensity(int slot) {
    return slotMaxIntensity[slot];
  }

  int getCount(int slot) {
    return slotCount[slot];
  }

  int getFirstScan(int slot) {
    return slotFirstScan[slot];
  }

  int getLastScan(int slot) {
    return slotLastScan[slot];
  }

  int getMaxGapScans() {
    return maxGapScans;
  }

  /**
   * @return number of slots that are needed to store the traces, grows with the number of traces
   * that are active at the same time
   */
  int getSlotCapacity() {
    return usedSlots;
  }

  long getNumTracesCreated() {
    return nextTraceId;
  }

  long getNumDataPoints() {
    return points.getNumDataPoints();
  }

  /**
   * @return the max intensity of all valid data points of the processed scans
   */
  double getMaxDataPointIntensity() {
    return points.getMaxIntensity();
  }

  int getMaxActiveTraces() {
    return maxActiveTraces;
  }
}
