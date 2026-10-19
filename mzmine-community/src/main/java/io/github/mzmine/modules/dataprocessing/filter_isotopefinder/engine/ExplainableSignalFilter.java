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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.Element;

/**
 * Collects the signals a charge hypothesis EMITS at its kept offsets, optionally dropping those the
 * configured chemistry cannot explain: keeping an offset says the pattern REACHES it, not that every
 * signal sitting there belongs to it.
 * <p>
 * decision: OFF by default, and never allowed to empty an offset. Measured over the corpus it lowers
 * the noise leak (0.0174 -> 0.0162) but also completeness (recall 0.9931 -> 0.9909, elementPrecision
 * 0.8446 -> 0.8365), because a blended fine-structure centroid can land between two isotope defects
 * and is then indistinguishable from contamination. Charge selection is unaffected either way, as
 * this runs after scoring.
 */
final class ExplainableSignalFilter {

  private static final int MAX_SUBSTITUTIONS = 8;

  // Deliberately BELOW 1: candidates are collected within the FULL tolerance of an isotope distance,
  // so a window equal to the tolerance calls every collected signal explainable and the filter is a
  // no-op by construction. At half of it a signal must sit closer to a real defect than the
  // collection step required, which is what separates a defect from a coincidence.
  private static final double WINDOW_TOL_FACTOR = 0.5;
  // so a very tight tolerance cannot reject a genuine defect over sub-mDa calibration error
  private static final double MIN_WINDOW = 0.0015;

  private final boolean enabled;
  private final MZTolerance tol;
  private final IsotopeDefectTable defects;

  private ExplainableSignalFilter(final boolean enabled, @NotNull final MZTolerance tol,
      @NotNull final IsotopeDefectTable defects) {
    this.enabled = enabled;
    this.tol = tol;
    this.defects = defects;
  }

  /**
   * @param enabled        whether unexplainable signals are actually dropped. A disabled filter
   *                       still collects the emitted signals, it just keeps all of them.
   * @param autoCandidates the elements auto-detection may infer (empty when off).
   */
  public static @NotNull ExplainableSignalFilter create(final boolean enabled,
      @NotNull final List<Element> elements, @NotNull final List<String> autoCandidates,
      @NotNull final MZTolerance tol) {
    // decision: the user's elements plus the auto-detection candidates, and NOT the default heavy
    // set on top. The promise is "explainable by the chemistry you selected", and unioning Cl/Br/S/Si
    // into every search made the defect grid dense enough to explain almost any deviation.
    final List<String> candidates = new ArrayList<>();
    for (final Element e : elements) {
      final String symbol = e.getSymbol();
      if (symbol != null && !candidates.contains(symbol)) {
        candidates.add(symbol);
      }
    }
    for (final String symbol : autoCandidates) {
      if (!candidates.contains(symbol)) {
        candidates.add(symbol);
      }
    }
    return new ExplainableSignalFilter(enabled, tol,
        IsotopeDefectTable.build(candidates, MAX_SUBSTITUTIONS));
  }

  /**
   * Every candidate sitting at one of the {@code emitOffsets}, minus - when enabled - the
   * unexplainable ones that an explainable signal at the same offset dominates.
   *
   * @param emitOffsets the offsets the pattern reports, relative to {@code baseMz}.
   * @param baseMz      the observed base peak m/z (grid origin, offset 0).
   * @return the signals to emit, falling back to all candidates rather than reporting nothing.
   */
  public @NotNull List<DataPoint> collectEmitted(@NotNull final List<DataPoint> candidates,
      @NotNull final Set<Integer> emitOffsets, final double baseMz, final double spacingDa,
      final int z) {
    final List<DataPoint> kept = new ArrayList<>();
    final Map<Integer, Double> explainedAtOffset = new HashMap<>();
    // memoized: the attribution test is the expensive part and both passes need the same answer
    final boolean[] explainable = new boolean[candidates.size()];
    final int[] offsets = new int[candidates.size()];
    for (int i = 0; i < candidates.size(); i++) {
      final DataPoint dp = candidates.get(i);
      offsets[i] = (int) Math.round((dp.getMZ() - baseMz) / spacingDa);
      if (!enabled || !emitOffsets.contains(offsets[i])) {
        continue;
      }
      explainable[i] = isExplainable(dp.getMZ(), baseMz, spacingDa, z);
      if (explainable[i]) {
        explainedAtOffset.merge(offsets[i], dp.getIntensity(), Math::max);
      }
    }
    for (int i = 0; i < candidates.size(); i++) {
      if (!emitOffsets.contains(offsets[i])) {
        continue;
      }
      final Double explainedIntensity = explainedAtOffset.get(offsets[i]);
      // decision: dropped only where an explainable signal at the same offset DOMINATES it. A signal
      // that dominates its offset IS the isotope peak the pattern reaches there, so a centroid
      // matching no defect is blended fine structure rather than contamination.
      if (!enabled || explainable[i] || explainedIntensity == null
          || explainedIntensity < candidates.get(i).getIntensity()) {
        kept.add(candidates.get(i));
      }
    }
    if (kept.isEmpty()) {
      kept.addAll(candidates);
    }
    return kept;
  }

  /**
   * Whether one signal sits on the exact 13C grid of the observed base, or its neutral-mass
   * deviation from that grid matches the defect of one or more candidate isotopes - M+2 heavies as
   * well as M+1-only ones such as 15N/2H.
   *
   * @param z the charge, to convert an m/z deviation into a neutral-mass deviation.
   */
  private boolean isExplainable(final double mz, final double baseMz, final double spacingDa,
      final int z) {
    final int k = (int) Math.round((mz - baseMz) / spacingDa);
    final double exactGrid = baseMz + k * spacingDa;
    if (tol.checkWithinTolerance(exactGrid, mz)) {
      return true;
    }
    final double deviation = (mz - exactGrid) * z;
    // a signal k offsets out can carry at most |k| substitutions, each adding >= 1 nominal offset
    final int maxSubstitutions = Math.min(MAX_SUBSTITUTIONS, Math.max(1, Math.abs(k)));
    // decision: derived from the search's OWN tolerance, not a fixed constant. Mixture defects lie
    // ~1 mDa apart - an 81Br spacing sits 0.03 mDa from 15N+18O - so a fixed 6 mDa window would call
    // nearly everything explainable. Tying it to the tolerance makes attribution as strict as the
    // data allows: discriminating on FT data, permissive where the peaks are unresolved anyway.
    final double window = Math.max(MIN_WINDOW,
        tol.getMzToleranceForMass(mz) * z * WINDOW_TOL_FACTOR);
    return defects.explains(deviation, window, maxSubstitutions);
  }
}
