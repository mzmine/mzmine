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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The observed signals of one charge hypothesis, indexed once by their integer offset on the
 * charge-adjusted 13C grid ({@code baseMz + k * spacingDa}).
 * <p>
 * This is the single place the 13C grid is walked. Previously the same
 * {@code round((mz - baseMz) / spacingDa)} mapping was recomputed in four places with three
 * different tolerance windows (the exact-13C ladder, the spacing regression, the require-13C gap
 * probe and the fine-structure collapse), which made it easy for the definitions of "on the 13C
 * grid" to drift apart. Here the mapping is done once and every consumer expresses its own window
 * as a {@code toleranceFactor} on top of the shared {@link MZTolerance}.
 * <p>
 * Two views are available per offset:
 * <ul>
 *   <li>the <b>isolated</b> view ({@link #onGridIntensities}, {@link #onGridMz}) - the single signal
 *   closest to the exact 13C position, so heavy isotopes (37Cl/81Br/34S, ~4-5 mDa off grid) and 15N
 *   do not contaminate the carbon ratio;</li>
 *   <li>the <b>collapsed</b> view ({@link #collapsed()}) - every signal at that offset summed, with
 *   an intensity-weighted m/z, used for coverage and the emitted pattern.</li>
 * </ul>
 */
public final class CarbonLadder {

  /**
   * The m/z tolerance is widened by this factor when testing whether a 13C-grid position is
   * occupied, so a heavy isotope (37Cl/81Br) merged with the expected 13C signal - which pulls the
   * observed centroid a few mDa off the exact grid - still counts as present and does not open a
   * false hole that would truncate the pattern early.
   */
  private static final double GAP_TOL_FACTOR = 3d;

  /**
   * Cluster connectivity (see {@link #clusterSpanAround}): how many offsets a single step of the
   * chained walk may span, i.e. one missing position may be bridged. Deliberately tiny: the test
   * only has to tell the searched signal's own envelope (whose isotope peaks are one or two offsets
   * apart, even where a weak intermediate 13C peak fell below the noise floor) from an unrelated
   * cluster the candidate collection chained to through unrelated isotope distances, which is many
   * offsets away.
   */
  private static final int CLUSTER_MAX_GAP = 2;

  private final double baseMz;
  private final double spacingDa;
  private final MZTolerance tol;
  /**
   * All candidate m/z ascending, so {@link #nearestMzWithin} can binary-search a widened window
   * instead of scanning every candidate per probed offset.
   */
  private final double[] sortedMz;
  private final TreeMap<Integer, LadderPeak> byOffset;

  private CarbonLadder(final double baseMz, final double spacingDa, @NotNull final MZTolerance tol,
      final double @NotNull [] sortedMz, @NotNull final TreeMap<Integer, LadderPeak> byOffset) {
    this.baseMz = baseMz;
    this.spacingDa = spacingDa;
    this.tol = tol;
    this.sortedMz = sortedMz;
    this.byOffset = byOffset;
  }

  /**
   * Index the candidates by their 13C-grid offset relative to {@code baseMz} in a single pass.
   *
   * @param candidates the detected signals (order does not matter).
   * @param baseMz     the m/z mapped to offset 0 (the observed base peak).
   * @param spacingDa  the m/z spacing between consecutive offsets (13C distance / charge).
   * @param tol        the m/z tolerance of the source data.
   * @return the indexed ladder (possibly empty).
   */
  public static @NotNull CarbonLadder build(@NotNull final List<DataPoint> candidates,
      final double baseMz, final double spacingDa, @NotNull final MZTolerance tol) {
    final TreeMap<Integer, LadderPeak> byOffset = new TreeMap<>();
    final double[] mzs = new double[candidates.size()];
    int i = 0;
    for (final DataPoint dp : candidates) {
      final double mz = dp.getMZ();
      final double intensity = dp.getIntensity();
      mzs[i++] = mz;
      final int offset = (int) Math.round((mz - baseMz) / spacingDa);
      final double error = Math.abs(mz - (baseMz + offset * spacingDa));
      final LadderPeak prev = byOffset.get(offset);
      if (prev == null) {
        byOffset.put(offset, new LadderPeak(mz, intensity, error, intensity, mz * intensity));
      } else {
        // accumulate the collapsed view; keep the closest-to-grid signal as the isolated one
        final boolean closer = error < prev.gridError();
        byOffset.put(offset,
            new LadderPeak(closer ? mz : prev.nearestMz(), closer ? intensity : prev.nearestIntensity(),
                closer ? error : prev.gridError(), prev.summedIntensity() + intensity,
                prev.weightedMzSum() + mz * intensity));
      }
    }
    Arrays.sort(mzs);
    return new CarbonLadder(baseMz, spacingDa, tol, mzs, byOffset);
  }

  /**
   * @return the exact 13C-grid m/z of {@code offset}.
   */
  public double exactMzAt(final int offset) {
    return baseMz + offset * spacingDa;
  }

  /**
   * @return whether any signal at all was indexed.
   */
  public boolean isEmpty() {
    return byOffset.isEmpty();
  }

  /**
   * Per offset, the intensity of the signal closest to the exact 13C position, restricted to offsets
   * whose closest signal lies within {@code toleranceFactor} times the m/z tolerance of that
   * position. Signals off the exact grid (heavy isotopes, 15N) are excluded, so the carbon envelope
   * is scored on the pure 13C ladder rather than on merged nominal offsets.
   *
   * @param toleranceFactor multiplier on the m/z tolerance ({@code 1.0} = the nominal tolerance).
   * @return offset to intensity, ascending by offset.
   */
  public @NotNull TreeMap<Integer, Double> onGridIntensities(final double toleranceFactor) {
    final TreeMap<Integer, Double> out = new TreeMap<>();
    for (final var e : byOffset.entrySet()) {
      if (isOnGrid(e.getKey(), e.getValue(), toleranceFactor)) {
        out.put(e.getKey(), e.getValue().nearestIntensity());
      }
    }
    return out;
  }

  /**
   * Per offset, the m/z of the signal closest to the exact 13C position, restricted as in
   * {@link #onGridIntensities(double)}. Used by the spacing regression, which needs positions rather
   * than intensities.
   *
   * @param toleranceFactor multiplier on the m/z tolerance.
   * @return offset to m/z, ascending by offset.
   */
  public @NotNull TreeMap<Integer, Double> onGridMz(final double toleranceFactor) {
    final TreeMap<Integer, Double> out = new TreeMap<>();
    for (final var e : byOffset.entrySet()) {
      if (isOnGrid(e.getKey(), e.getValue(), toleranceFactor)) {
        out.put(e.getKey(), e.getValue().nearestMz());
      }
    }
    return out;
  }

  /**
   * The candidate m/z closest to the probed position, within {@code toleranceFactor} times the m/z
   * tolerance. Unlike {@link #onGridIntensities(double)} the signal does not have to round to a
   * particular offset, so a widened window can legitimately match a neighbour - which is the point
   * when a merged heavy isotope pulls the observed centroid off the grid.
   *
   * @param mz              the m/z to probe.
   * @param toleranceFactor multiplier on the m/z tolerance.
   * @return the closest candidate m/z, or {@link Double#NaN} when the position is unoccupied.
   */
  public double nearestMzWithin(final double mz, final double toleranceFactor) {
    if (sortedMz.length == 0) {
      return Double.NaN;
    }
    final double window = tol.getMzToleranceForMass(mz) * toleranceFactor;
    int idx = Arrays.binarySearch(sortedMz, mz);
    if (idx >= 0) {
      return sortedMz[idx];
    }
    idx = -idx - 1; // insertion point: first element greater than mz
    final double above = idx < sortedMz.length ? sortedMz[idx] - mz : Double.MAX_VALUE;
    final double below = idx > 0 ? mz - sortedMz[idx - 1] : Double.MAX_VALUE;
    if (Math.min(above, below) > window) {
      return Double.NaN;
    }
    return above <= below ? sortedMz[idx] : sortedMz[idx - 1];
  }

  /**
   * Every signal collapsed per offset (summed intensity, intensity-weighted mean m/z). This handles
   * isotopic fine structure (e.g. 13C2 vs 34S at the same nominal offset) for scoring, while the raw
   * signals are retained elsewhere for the stored pattern.
   *
   * @return offset to collapsed peak, ascending by offset.
   */
  public @NotNull TreeMap<Integer, OffsetPeak> collapsed() {
    final TreeMap<Integer, OffsetPeak> out = new TreeMap<>();
    for (final var e : byOffset.entrySet()) {
      final LadderPeak p = e.getValue();
      final double mz =
          p.summedIntensity() > 0 ? p.weightedMzSum() / p.summedIntensity() : Double.NaN;
      out.put(e.getKey(), new OffsetPeak(e.getKey(), mz, p.summedIntensity()));
    }
    return out;
  }

  /**
   * Select the gap-free 13C ladder through the observed base (offset 0), as the optional require-13C
   * gate needs it. Prefers the every-13C (step 1) ladder; when that reaches fewer than two signals it
   * falls back to an every-second (step 2) ladder for molecules whose pattern shows only on every
   * second 13C position (an intense +2 heavy comb: Cl/Br/Cu). The step-2 ladder must reach at least
   * three signals so a lone monoisotopic + single heavy M+2 does not qualify as a 13C pattern.
   *
   * @return inclusive {@code [minOffset, maxOffset, step]}, or {@code null} if no ladder qualifies.
   */
  public int @Nullable [] requireC13Span() {
    final int[] s1 = gapFreeSpan(1);
    if (s1[1] - s1[0] >= 1) { // >= 2 signals on the every-13C grid
      return new int[]{s1[0], s1[1], 1};
    }
    final int[] s2 = gapFreeSpan(2);
    if (s2[1] - s2[0] >= 4) { // >= 3 signals on the every-second grid
      return new int[]{s2[0], s2[1], 2};
    }
    return null;
  }

  /**
   * Contiguous, gap-free span of grid offsets around the observed base (offset 0), stepping by
   * {@code step} offsets. Walks outward in both directions and stops at the first stepped position
   * with no signal, so a hole where a peak is expected truncates the span even if signals exist
   * further out. The presence test uses a widened tolerance ({@link #GAP_TOL_FACTOR}) so a heavy
   * isotope merged with the expected 13C peak (shifting it a few mDa off grid) still counts.
   * <p>
   * decision: probed on the NOMINAL 13C grid of the base, not chained on the observed positions.
   * Chaining would be anchor-independent, but the accumulated drift of the nominal grid is also what
   * stops a harmonic (a z=2 comb read as a z=1 ladder, whose steps are only ~4 mDa off) from walking
   * the whole envelope, so it carries real charge-discrimination weight. The searched signal is kept
   * inside the pattern by widening the crop instead (see the caller).
   *
   * @param step the offset step (1 = every 13C, 2 = every second 13C).
   * @return inclusive {@code [minOffset, maxOffset]} span containing offset 0.
   */
  public int @NotNull [] gapFreeSpan(final int step) {
    int hi = 0;
    while (!Double.isNaN(nearestMzWithin(exactMzAt(hi + step), GAP_TOL_FACTOR))) {
      hi += step;
    }
    int lo = 0;
    while (!Double.isNaN(nearestMzWithin(exactMzAt(lo - step), GAP_TOL_FACTOR))) {
      lo -= step;
    }
    return new int[]{lo, hi};
  }

  /**
   * The connected cluster the searched signal belongs to: consecutive positions one 13C spacing
   * apart, each probed from the m/z of the signal the previous step FOUND rather than from a fixed
   * grid. Chaining makes it independent of where in the pattern the search started and immune to the
   * nominal grid's drift against a polyhalogen comb, which is what a cluster test needs - it only
   * decides which signals belong together, never whether a charge is accepted.
   *
   * @param from     the searched signal's offset on this ladder's grid.
   * @param anchorMz the searched signal's m/z.
   * @return inclusive {@code [minOffset, maxOffset]} span containing {@code from}.
   */
  public int @NotNull [] clusterSpanAround(final int from, final double anchorMz) {
    final int up = countClusterSteps(anchorMz, spacingDa);
    final int down = countClusterSteps(anchorMz, -spacingDa);
    return new int[]{from - down, from + up};
  }

  /**
   * @param delta the signed m/z step of one ladder position.
   * @return how many offsets the searched signal's cluster reaches in that direction.
   */
  private int countClusterSteps(final double startMz, final double delta) {
    // a step must advance by at least half a spacing, so a tolerance window wider than the spacing
    // (high charge + wide tolerance) cannot re-find the same signal and stall the walk
    final double minAdvance = Math.abs(delta) / 2d;
    int steps = 0;
    double ref = startMz;
    outer:
    while (true) {
      for (int gap = 1; gap <= CLUSTER_MAX_GAP; gap++) {
        final double found = nearestMzWithin(ref + gap * delta, GAP_TOL_FACTOR);
        if (!Double.isNaN(found) && Math.abs(found - ref) >= minAdvance) {
          ref = found;
          steps += gap;
          continue outer;
        }
      }
      return steps;
    }
  }

  /**
   * @return whether the offset's closest-to-grid signal is within the scaled tolerance of the exact
   * 13C position.
   */
  private boolean isOnGrid(final int offset, @NotNull final LadderPeak peak,
      final double toleranceFactor) {
    return tol.checkWithinTolerance(exactMzAt(offset), peak.nearestMz(), toleranceFactor);
  }

  /**
   * The signals indexed at one grid offset.
   *
   * @param nearestMz        m/z of the signal closest to the exact 13C position.
   * @param nearestIntensity intensity of that signal.
   * @param gridError        absolute m/z distance of that signal from the exact 13C position.
   * @param summedIntensity  summed intensity of ALL signals at this offset.
   * @param weightedMzSum    sum of {@code mz * intensity} over all signals at this offset.
   */
  private record LadderPeak(double nearestMz, double nearestIntensity, double gridError,
                            double summedIntensity, double weightedMzSum) {

  }
}
