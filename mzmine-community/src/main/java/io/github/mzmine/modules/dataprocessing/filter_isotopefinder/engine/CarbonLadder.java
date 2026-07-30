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

  private final double baseMz;
  private final double spacingDa;
  private final MZTolerance tol;
  /**
   * All candidate m/z ascending, so {@link #hasSignalNearGrid} can binary-search a widened window
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
   * Whether any candidate lies within {@code toleranceFactor} times the m/z tolerance of the exact
   * 13C position of {@code offset}. Unlike {@link #onGridIntensities(double)} this does not require
   * the signal to round to {@code offset}, so a widened window can legitimately match a neighbour -
   * which is the point when a merged heavy isotope pulls the observed centroid off the grid.
   *
   * @param offset          the grid offset to probe.
   * @param toleranceFactor multiplier on the m/z tolerance.
   * @return whether the position is occupied.
   */
  public boolean hasSignalNearGrid(final int offset, final double toleranceFactor) {
    if (sortedMz.length == 0) {
      return false;
    }
    final double exactMz = exactMzAt(offset);
    final double window = tol.getMzToleranceForMass(exactMz) * toleranceFactor;
    int idx = Arrays.binarySearch(sortedMz, exactMz);
    if (idx >= 0) {
      return true;
    }
    idx = -idx - 1; // insertion point: first element greater than exactMz
    if (idx < sortedMz.length && sortedMz[idx] - exactMz <= window) {
      return true;
    }
    return idx > 0 && exactMz - sortedMz[idx - 1] <= window;
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
