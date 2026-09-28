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

import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;

/**
 * The mono &rarr; M+1 (13C) ratio of the isolated carbon ladder at the placement anchor, plus the
 * two tests applied to it: the soft {@link #plausibility} factor and the hard
 * {@link #failsRequireC13Gate} of the opt-in require-13C mode.
 * <p>
 * decision: ONE anchor convention, in one place. The harmonic upper-bound penalty used to anchor on
 * the placement while the require-13C gate and the FT-ringing penalty anchored on the observed base,
 * so for any mid-envelope apex ({@code placement != 0} - every polyhalogen and protein) the
 * "symmetric" tests silently compared different peak pairs.
 *
 * @param present      whether the anchor peak exists at all; if not, no M+1/M test applies.
 * @param value        {@code I(M+1) / I(M)}, and 0 when the M+1 is absent - itself meaningful, as
 *                     that is the FT-ringing signature.
 * @param anchorIsMono whether the anchor dominates the ladder peaks below it, i.e. really is the
 *                     monoisotopic rather than a mid-envelope apex.
 * @param anchorIsBase whether the anchor is the observed base. The aggressive lower-bound tests
 *                     also require this: a carbon-poor halogenated molecule with a mid-envelope apex
 *                     legitimately falls below the carbon-model minimum, and penalising it costs
 *                     real polyhalogen charge calls.
 */
record CarbonRatio(boolean present, double value, boolean anchorIsMono,
                          boolean anchorIsBase) {

  public static final CarbonRatio ABSENT = new CarbonRatio(false, 0d, false, false);

  private static final double SLACK = 0.3;
  // FT-ringing guard, as a fraction of the carbon MINIMUM M+1/M. Far below 1 (a quarter of the
  // already-conservative 1/20-C-per-Da minimum) so genuine low-carbon molecules - within ~2x of
  // their prediction - are never penalised, while ringing read as a high-charge ladder (M+1 orders
  // of magnitude too small for the implied mass) is.
  private static final double LOWER_FACTOR = 0.25;
  private static final double LOWER_PENALTY_FLOOR = 1e-3;
  // same bound for the require-13C gate. Tighter than LOWER_FACTOR because the gate rejects
  // outright, but still well below 1: heteroatom-rich molecules legitimately have far fewer carbons
  // per Dalton than the model minimum, and a tighter bound rejected valid singly charged patterns.
  private static final double REQUIRE_C13_LOWER_FACTOR = 0.5;
  // the anchor counts as the monoisotopic only when no ladder peak below it reaches this fraction of
  // it, exempting mid-envelope apices (proteins, halogen combs) from the lower-bound check
  private static final double MONO_DOMINANCE_FRACTION = 0.1;

  /**
   * @param carbonLadder the isolated exact-13C ladder, offsets relative to the observed base peak.
   * @param placement    the predicted offset aligned to observed offset 0.
   * @return the measured ratio, or {@link #ABSENT} when the anchor peak is missing.
   */
  public static @NotNull CarbonRatio measure(@NotNull final TreeMap<Integer, Double> carbonLadder,
      final int placement) {
    final int monoOffset = -placement;
    final Double monoI = carbonLadder.get(monoOffset);
    if (monoI == null || monoI <= 0d) {
      return ABSENT;
    }
    double maxBelow = 0d;
    for (final double below : carbonLadder.headMap(monoOffset).values()) {
      maxBelow = Math.max(maxBelow, below);
    }
    final Double m1I = carbonLadder.get(monoOffset + 1);
    // a MISSING M+1 is a ratio of 0, not "unmeasurable" - that is the FT-ringing signature itself
    return new CarbonRatio(true, (m1I != null ? m1I : 0d) / monoI,
        maxBelow < MONO_DOMINANCE_FRACTION * monoI, placement == 0);
  }

  /**
   * Two-sided plausibility factor in {@code (0,1]}, catching two different misdetections:
   * <ul>
   *   <li><b>upper</b> - harmonic doubling, where the "M+1" slot is really a co-eluting compound's
   *   monoisotopic, so the implied 13C M+1/M overshoots the maximum carbon prediction;</li>
   *   <li><b>lower</b> - FT ringing around a strong singly charged signal, which forms a fake
   *   fine-spaced high-charge ladder whose "M+1" is far too small to be a real 13C peak.</li>
   * </ul>
   * Both read the CARBON bounds on the ISOLATED 13C ladder, never the heavy-halogen upper bound, so
   * heavy isotopes cannot trigger a penalty and a genuine higher charge - a valid sub-grid of the
   * pattern - keeps a plausible ratio.
   *
   * @param m1Bounds {@code {min, max}} carbon M+1/M prediction for the implied neutral mass.
   */
  public double plausibility(final double @NotNull [] m1Bounds) {
    if (!present) {
      return 1d;
    }
    final double hi = m1Bounds[1] * (1d + SLACK);
    if (hi > 0d && value > hi) {
      return hi / value;
    }
    // far more aggressive, so it fires only when the anchor demonstrably is the dominant mono AND
    // the observed base - see supportsLowerBound
    final double lo = m1Bounds[0] * LOWER_FACTOR;
    if (supportsLowerBound() && lo > 0d && value < lo) {
      return Math.max(LOWER_PENALTY_FLOOR, value / lo);
    }
    return 1d;
  }

  /**
   * The opt-in require-13C hard gate on the same anchored ratio. Same upper bound as
   * {@link #plausibility}, tighter lower bound - see {@link #REQUIRE_C13_LOWER_FACTOR}.
   */
  public boolean failsRequireC13Gate(final double @NotNull [] m1Bounds) {
    // a missing M+1 is left to the soft penalty; the gate only judges a ratio it could measure
    if (!supportsLowerBound() || value <= 0d) {
      return false;
    }
    return value < m1Bounds[0] * REQUIRE_C13_LOWER_FACTOR || value > m1Bounds[1] * (1d + SLACK);
  }

  private boolean supportsLowerBound() {
    return present && anchorIsMono && anchorIsBase;
  }
}
