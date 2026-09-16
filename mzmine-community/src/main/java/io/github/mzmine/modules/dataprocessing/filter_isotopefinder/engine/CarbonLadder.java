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
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The observed signals of one charge hypothesis, indexed once by their integer offset on the
 * charge-adjusted 13C grid ({@code baseMz + k * spacingDa}).
 * <p>
 * decision: the single place the 13C grid is walked. That mapping used to be recomputed by the
 * exact-13C ladder, the spacing regression, the require-13C gap probe and the fine-structure
 * collapse with three different tolerance windows, so their definitions of "on the 13C grid" could
 * drift apart. Here every consumer instead expresses its own window as a {@code toleranceFactor} on
 * the shared {@link MZTolerance}.
 * <p>
 * Two views per offset: the <b>isolated</b> one ({@link #onGridIntensities}, {@link #onGridMz})
 * keeps only the signal closest to the exact 13C position, so heavy isotopes and 15N cannot
 * contaminate the carbon ratio, while the <b>collapsed</b> one ({@link #collapsed()}) sums
 * everything at that offset for coverage and the emitted pattern.
 */
final class CarbonLadder {

  // widens the tolerance when testing whether a grid position is OCCUPIED, so a heavy isotope
  // merged with the expected 13C signal - pulling the centroid a few mDa off grid - still counts and
  // does not open a false hole that truncates the pattern early
  private static final double GAP_TOL_FACTOR = 3d;
  // how many offsets one step of the chained cluster walk may span, i.e. one missing position may be
  // bridged. Deliberately tiny: it only has to tell the searched signal's own envelope from an
  // unrelated cluster many offsets away.
  private static final int CLUSTER_MAX_GAP = 2;

  private final double baseMz;
  private final double spacingDa;
  private final MZTolerance tol;
  // ascending, so nearestMzWithin can binary-search instead of scanning per probed offset
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
   * Index the candidates by their 13C-grid offset in a single pass.
   *
   * @param candidates the detected signals; order does not matter.
   * @param baseMz     the m/z mapped to offset 0 (the observed base peak).
   * @param spacingDa  13C distance / charge.
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
        byOffset.put(offset, new LadderPeak(closer ? mz : prev.nearestMz(),
            closer ? intensity : prev.nearestIntensity(), closer ? error : prev.gridError(),
            prev.summedIntensity() + intensity, prev.weightedMzSum() + mz * intensity));
      }
    }
    Arrays.sort(mzs);
    return new CarbonLadder(baseMz, spacingDa, tol, mzs, byOffset);
  }

  public double exactMzAt(final int offset) {
    return baseMz + offset * spacingDa;
  }

  public boolean isEmpty() {
    return byOffset.isEmpty();
  }

  /**
   * Per offset, the intensity of the signal closest to the exact 13C position - and only where that
   * signal is within {@code toleranceFactor} times the tolerance of it, so the carbon envelope is
   * scored on the pure 13C ladder rather than on merged nominal offsets.
   *
   * @param toleranceFactor multiplier on the m/z tolerance ({@code 1.0} = nominal).
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
   * As {@link #onGridIntensities(double)} but returning positions, for the spacing regression.
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
   * The candidate m/z closest to the probed position, within {@code toleranceFactor} times the
   * tolerance. Unlike {@link #onGridIntensities(double)} the signal need not round to a particular
   * offset, so a widened window may legitimately match a neighbour - which is the point when a
   * merged heavy isotope pulls the centroid off grid.
   *
   * @return the closest candidate m/z, or {@link Double#NaN} when the position is unoccupied.
   */
  private double nearestMzWithin(final double mz, final double toleranceFactor) {
    final int idx = BinarySearch.binarySearch(sortedMz, mz, DefaultTo.CLOSEST_VALUE);
    if (idx < 0) {
      return Double.NaN; // no candidates at all
    }
    final double window = tol.getMzToleranceForMass(mz) * toleranceFactor;
    final double nearest = sortedMz[idx];
    return Math.abs(nearest - mz) <= window ? nearest : Double.NaN;
  }

  /**
   * Every signal summed per offset, with an intensity-weighted m/z. Collapses isotopic fine
   * structure (13C2 vs 34S at one nominal offset) for SCORING only - the raw signals are kept
   * elsewhere for the stored pattern.
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
   * The gap-free 13C ladder the optional require-13C gate needs. Prefers every-13C; falls back to
   * every-SECOND position for patterns that only show there (an intense +2 comb: Cl/Br/Cu), where it
   * demands three signals so a lone mono + single heavy M+2 cannot pass as a 13C pattern.
   *
   * @return the qualifying span, or {@code null} if neither ladder qualifies.
   */
  public @Nullable OffsetSpan requireC13Span() {
    final OffsetSpan everyC13 = gapFreeSpan(1);
    if (everyC13.size() >= 2) {
      return everyC13;
    }
    final OffsetSpan everySecond = gapFreeSpan(2);
    return everySecond.size() >= 3 ? everySecond : null;
  }

  /**
   * Gap-free span of grid offsets around the observed base, walking outward both ways and stopping
   * at the first stepped position with no signal - so a hole truncates the span even if signals
   * exist further out.
   * <p>
   * decision: probed on the NOMINAL grid of the base, not chained on the observed positions.
   * Chaining would be anchor-independent, but the nominal grid's accumulated drift is also what
   * stops a harmonic (a z=2 comb read as z=1, only ~4 mDa off per step) from walking the whole
   * envelope, so it carries real charge-discrimination weight. The searched signal is kept in the
   * pattern by widening the crop instead - see the caller.
   */
  private @NotNull OffsetSpan gapFreeSpan(final int step) {
    int hi = 0;
    while (!Double.isNaN(nearestMzWithin(exactMzAt(hi + step), GAP_TOL_FACTOR))) {
      hi += step;
    }
    int lo = 0;
    while (!Double.isNaN(nearestMzWithin(exactMzAt(lo - step), GAP_TOL_FACTOR))) {
      lo -= step;
    }
    return new OffsetSpan(lo, hi, step);
  }

  /**
   * The connected cluster the searched signal belongs to, each step probed from the m/z the previous
   * step FOUND rather than from a fixed grid.
   * <p>
   * decision: chained here, unlike {@link #gapFreeSpan}, because this only decides which signals
   * belong together and never whether a charge is accepted - so it should be independent of where
   * the search started and immune to the nominal grid's drift against a polyhalogen comb.
   *
   * @param from     the searched signal's offset on this ladder's grid.
   * @param anchorMz the searched signal's m/z.
   */
  public @NotNull OffsetSpan clusterSpanAround(final int from, final double anchorMz) {
    final int up = countClusterSteps(anchorMz, spacingDa);
    final int down = countClusterSteps(anchorMz, -spacingDa);
    return OffsetSpan.of(from - down, from + up);
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

  private boolean isOnGrid(final int offset, @NotNull final LadderPeak peak,
      final double toleranceFactor) {
    return tol.checkWithinTolerance(exactMzAt(offset), peak.nearestMz(), toleranceFactor);
  }

  /**
   * The signals indexed at one grid offset: {@code nearest*} describe the one closest to the exact
   * 13C position ({@code gridError} being its distance from it), the other two accumulate ALL of
   * them.
   */
  private record LadderPeak(double nearestMz, double nearestIntensity, double gridError,
                            double summedIntensity, double weightedMzSum) {

  }
}
