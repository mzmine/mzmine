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
 * Collects the signals a charge hypothesis EMITS at its kept offsets, optionally dropping those that
 * the configured chemistry cannot explain.
 * <p>
 * Keeping an offset says the pattern reaches that offset; it does not make every signal sitting there
 * part of the pattern. A signal that is neither on the exact 13C grid nor attributable to a
 * combination of the elements' isotope mass defects is noise or a co-eluting interferent that happens
 * to fall at a kept offset.
 * <p>
 * decision: the filter is OFF by default and never allowed to empty an offset. Measured over the
 * corpus it lowers the noise leak (0.0174 -> 0.0162) but also pattern completeness (recall 0.9931 ->
 * 0.9909, F1 0.9938 -> 0.9927, elementPrecision 0.8446 -> 0.8365), because a blended fine-structure
 * centroid can land between two isotope defects and is then indistinguishable from contamination.
 * Charge selection is unaffected either way - this runs after scoring.
 */
final class ExplainableSignalFilter {

  /**
   * Largest number of isotope substitutions the filter combines when testing whether an off-grid
   * signal's mass defect is reachable at all.
   */
  private static final int MAX_SUBSTITUTIONS = 8;

  /**
   * Match window as a fraction of the m/z tolerance. Deliberately BELOW 1: candidates are collected
   * within the full tolerance of an isotope distance, so a window equal to the tolerance would call
   * every collected signal explainable and the filter would be a no-op by construction. At half the
   * tolerance a signal must sit closer to a real isotope defect than the collection step required,
   * which is what separates a defect from a coincidence.
   */
  private static final double WINDOW_TOL_FACTOR = 0.5;

  /**
   * Floor for the match window (neutral Da), so a very tight tolerance cannot reject a genuine defect
   * over sub-mDa calibration error.
   */
  private static final double MIN_WINDOW = 0.0015;

  private final boolean enabled;
  private final MZTolerance tol;
  /**
   * Mass-defect deviations from the exact 13C grid that the configured chemistry can produce. Built
   * once from the user's elements plus any auto-detection candidates.
   */
  private final IsotopeDefectTable defects;

  private ExplainableSignalFilter(final boolean enabled, @NotNull final MZTolerance tol,
      @NotNull final IsotopeDefectTable defects) {
    this.enabled = enabled;
    this.tol = tol;
    this.defects = defects;
  }

  /**
   * @param enabled        whether unexplainable signals are actually dropped. A disabled filter still
   *                       collects the emitted signals, it just keeps all of them.
   * @param elements       the elements the user declared.
   * @param autoCandidates the elements element auto-detection may infer (empty when off).
   * @param tol            the m/z tolerance of the search.
   * @return the configured filter.
   */
  public static @NotNull ExplainableSignalFilter create(final boolean enabled,
      @NotNull final List<Element> elements, @NotNull final List<String> autoCandidates,
      @NotNull final MZTolerance tol) {
    // attribution candidates: the elements the user declared, plus the ones auto-detection may infer
    // when it is enabled. decision: NOT the default heavy candidates on top - the filter's promise is
    // "explainable by the chemistry you selected", and unioning Cl/Br/S/Si into every search made the
    // defect grid dense enough to explain almost any deviation, i.e. a filter that filters nothing.
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
   * The signals to emit for this charge hypothesis: every candidate sitting at one of the
   * {@code emitOffsets}, minus - when the filter is enabled - the unexplainable ones that an
   * explainable signal at the same offset dominates.
   *
   * @param candidates  the candidate signals of this charge hypothesis.
   * @param emitOffsets the offsets the pattern reports, relative to {@code baseMz}.
   * @param baseMz      the observed base peak m/z (grid origin, offset 0).
   * @param spacingDa   the 13C spacing at this charge.
   * @param z           the charge hypothesis.
   * @return the signals to emit; never empty as long as {@code candidates} is not (it falls back to
   * all candidates rather than reporting nothing).
   */
  public @NotNull List<DataPoint> collectEmitted(@NotNull final List<DataPoint> candidates,
      @NotNull final Set<Integer> emitOffsets, final double baseMz, final double spacingDa,
      final int z) {
    final List<DataPoint> kept = new ArrayList<>();
    // per kept offset, the strongest signal there that IS explainable
    final Map<Integer, Double> explainedAtOffset = new HashMap<>();
    // explainability is memoized per candidate: the attribution test is the most expensive part of
    // this filter and both passes below need the same answer
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
      // an unexplained signal is dropped only where the offset also holds an explainable signal that
      // DOMINATES it. decision: a signal that dominates its offset is the isotope peak the pattern
      // reaches there - if its measured centroid matches no isotope defect, that is blended fine
      // structure rather than contamination, so it stays even with the filter on.
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
   * Whether a single signal is explainable by this charge hypothesis: it sits on the exact 13C grid
   * of the observed base (within the m/z tolerance), or its neutral-mass deviation from that grid
   * matches the mass defect of one or more heavy isotopes of a candidate element (37Cl/81Br/34S/30Si,
   * 29Si, but also M+1-only isotopes such as 15N/2H).
   *
   * @param mz        the signal's m/z.
   * @param baseMz    the observed base peak's m/z (grid origin, offset 0).
   * @param spacingDa the 13C spacing at this charge.
   * @param z         the charge hypothesis, to convert an m/z deviation to a neutral-mass deviation.
   * @return whether the signal is on the 13C grid or attributable to heavy isotopes.
   */
  private boolean isExplainable(final double mz, final double baseMz, final double spacingDa,
      final int z) {
    final int k = (int) Math.round((mz - baseMz) / spacingDa);
    final double exactGrid = baseMz + k * spacingDa;
    if (tol.checkWithinTolerance(exactGrid, mz)) {
      return true;
    }
    final double deviation = (mz - exactGrid) * z;
    // a signal k offsets from the base can carry at most |k| substitutions (each adds >= 1 to the
    // nominal offset), which bounds the multiplicity the deviation may be a multiple of. Capped at
    // MAX_SUBSTITUTIONS: beyond it the accumulated defect exceeds the tolerance of any real
    // measurement anyway, and the table stays small enough to search per signal.
    final int maxSubstitutions = Math.min(MAX_SUBSTITUTIONS, Math.max(1, Math.abs(k)));
    // decision: the match window is derived from the search's OWN m/z tolerance (as a neutral-mass
    // window, hence x charge) rather than being a fixed constant. Isotope mass defects of mixtures lie
    // ~1 mDa apart - an 81Br spacing sits 0.03 mDa from 15N+18O - so a fixed 6 mDa window declares
    // nearly every collected signal explainable and the filter degenerates into a no-op. Tying it to
    // the tolerance (with the factor below) makes attribution as strict as the data allows:
    // discriminating on FT data, permissive where the peaks themselves are unresolved.
    final double window = Math.max(MIN_WINDOW,
        tol.getMzToleranceForMass(mz) * z * WINDOW_TOL_FACTOR);
    return defects.explains(deviation, window, maxSubstitutions);
  }
}
