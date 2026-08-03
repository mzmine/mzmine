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
 * The monoisotopic &rarr; M+1 (13C) intensity ratio of the isolated carbon ladder at the placement
 * anchor, together with the two tests the isotope finder applies to it: the soft
 * {@link #plausibility} factor that always runs, and the hard {@link #failsRequireC13Gate} that only
 * the opt-in require-13C mode runs.
 * <p>
 * decision: measured at ONE anchor convention (the observed offset that the sliding envelope fit
 * aligned to predicted offset 0) and kept in one place. Previously the harmonic upper-bound penalty
 * used the placement anchor while the require-13C gate and the FT-ringing lower-bound penalty used
 * the observed base, so for any mid-envelope apex ({@code placement != 0} - every polyhalogen and
 * every protein) the "symmetric" tests silently compared different peak pairs.
 *
 * @param present        whether the anchor (monoisotopic) peak exists with a positive intensity; if
 *                       not, no M+1/M test can be applied.
 * @param value          {@code I(M+1) / I(M)}; {@code 0} when the M+1 is absent, which is itself
 *                       meaningful (the FT-ringing signature).
 * @param anchorIsMono   whether the anchor dominates the ladder peaks below it, i.e. it really is
 *                       the monoisotopic rather than a mid-envelope apex.
 * @param anchorIsBase   whether the anchor is the observed base peak ({@code placement == 0}). The
 *                       aggressive lower-bound tests additionally require this: a carbon-poor
 *                       halogenated molecule whose apex sits mid-envelope legitimately has an M+1/M
 *                       far below the averagine carbon minimum, and penalising it costs real
 *                       polyhalogen charge calls.
 */
public record CarbonRatio(boolean present, double value, boolean anchorIsMono,
                          boolean anchorIsBase) {

  /**
   * No measurable ratio (the anchor peak is missing).
   */
  public static final CarbonRatio ABSENT = new CarbonRatio(false, 0d, false, false);

  // relative slack applied to the estimated M+1/M upper bound in both tests
  private static final double SLACK = 0.3;
  // FT-ringing guard: fraction of the carbon MINIMUM M+1/M prediction below which the observed 13C
  // M+1 is treated as implausibly small ("not a real 13C peak"). Deliberately far below 1 (a quarter
  // of the already-conservative 1/20-C-per-Da minimum) so genuine low-carbon / heteroatom-rich
  // molecules - which stay within ~2x of their prediction - are never penalised, while low-intensity
  // FT ringing mistaken for a high-charge 13C ladder (M+1 orders of magnitude too small for the
  // implied mass) is.
  private static final double LOWER_FACTOR = 0.25;
  // hardest floor the FT-ringing penalty can drive the quality to (keeps raw finite / comparable)
  private static final double LOWER_PENALTY_FLOOR = 1e-3;
  // lower bound of the "require 13C" gate as a fraction of the carbon MINIMUM (1/20-C-per-Da) M+1/M
  // prediction. Deliberately well below 1: heteroatom-rich (Cl/Br/S/metal) molecules legitimately
  // have far fewer carbons per Dalton than the averagine minimum, so their real 13C M+1/M falls below
  // that minimum; a too-tight lower bound wrongly rejected such valid singly charged patterns. This
  // effectively allows down to ~1/40 C per Da while still rejecting an "M+1" too small to be a real
  // 13C peak. Looser than LOWER_FACTOR is NOT possible here - this gate rejects outright.
  private static final double REQUIRE_C13_LOWER_FACTOR = 0.5;
  // the anchor counts as the monoisotopic (so its M+1/M is bounded by the mono carbon prediction)
  // only when no 13C-ladder peak below it reaches this fraction of the anchor; mid-envelope apices
  // (proteins, halogen combs) have significant peaks below and are exempt from the lower-bound check.
  private static final double MONO_DOMINANCE_FRACTION = 0.1;

  /**
   * Measure the ratio on the isolated carbon ladder at the placement anchor.
   *
   * @param carbonLadder the isolated exact-13C ladder (offset &rarr; intensity), offsets relative to
   *                     the observed base peak.
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
    // a MISSING M+1 is a ratio of 0, not "unmeasurable": that is exactly the FT-ringing signature
    // the lower bound must catch.
    return new CarbonRatio(true, (m1I != null ? m1I : 0d) / monoI,
        maxBelow < MONO_DOMINANCE_FRACTION * monoI, placement == 0);
  }

  /**
   * Two-sided plausibility of the carbon M+1/M ratio as a multiplicative factor in {@code (0,1]}.
   * <ul>
   *   <li><b>Upper</b> (the harmonic-doubling discriminator): the isolated 13C M+1/M must not exceed
   *   the maximum carbon prediction. A charge whose implied 13C M+1 is implausibly large - e.g. a
   *   doubling whose "M+1" slot is really a co-eluting compound's monoisotopic - is down-weighted in
   *   proportion to the overshoot.</li>
   *   <li><b>Lower</b> (the FT-ringing discriminator): when the anchor really is a dominant
   *   monoisotopic, its M+1 must not fall far below the MINIMUM carbon prediction for the mass this
   *   charge implies. Low-intensity ringing around a strong singly charged signal forms a fake
   *   fine-spaced high-charge ladder whose "M+1" is far too small to be a real 13C peak.</li>
   * </ul>
   * Both use the reliable CARBON bounds (not the heavy-halogen upper bound) on the isolated 13C
   * ladder, so heavy isotopes never trigger a penalty. A genuine higher charge is a valid sub-grid
   * of the pattern and keeps a plausible ratio.
   *
   * @param m1Bounds {@code {min, max}} carbon M+1/M prediction for the implied neutral mass.
   * @return the factor to multiply into the charge quality.
   */
  public double plausibility(final double @NotNull [] m1Bounds) {
    if (!present) {
      return 1d;
    }
    final double hi = m1Bounds[1] * (1d + SLACK);
    if (hi > 0d && value > hi) {
      return hi / value;
    }
    // the lower bound is far more aggressive, so it only fires when the anchor demonstrably is the
    // dominant monoisotopic AND the observed base; mid-envelope apices (proteins, halogen combs) are
    // exempt because their real M+1/M is legitimately below the averagine carbon minimum.
    final double lo = m1Bounds[0] * LOWER_FACTOR;
    if (supportsLowerBound() && lo > 0d && value < lo) {
      return Math.max(LOWER_PENALTY_FLOOR, value / lo);
    }
    return 1d;
  }

  /**
   * The optional require-13C hard gate on the same anchored ratio.
   * <p>
   * The lower bound uses {@link #REQUIRE_C13_LOWER_FACTOR} rather than the (much looser)
   * {@link #LOWER_FACTOR} of {@link #plausibility}: this gate is opt-in and is meant to reject
   * outright, but it must still not reject heteroatom-rich, carbon-poor molecules whose real 13C
   * M+1/M is legitimately below the averagine carbon minimum. The upper bound is the same as the soft
   * penalty's - an "M+1" too large to be 13C (a co-eluting mono).
   *
   * @param m1Bounds {@code {min, max}} carbon M+1/M prediction for the implied neutral mass.
   * @return whether the hypothesis must be rejected.
   */
  public boolean failsRequireC13Gate(final double @NotNull [] m1Bounds) {
    // a missing M+1 is left to the soft penalty; the gate only judges a ratio it could measure
    if (!supportsLowerBound() || value <= 0d) {
      return false;
    }
    return value < m1Bounds[0] * REQUIRE_C13_LOWER_FACTOR || value > m1Bounds[1] * (1d + SLACK);
  }

  /**
   * @return whether the aggressive lower-bound tests (the FT-ringing penalty and the optional
   * require-13C gate) may be applied to this ratio.
   */
  private boolean supportsLowerBound() {
    return present && anchorIsMono && anchorIsBase;
  }
}
