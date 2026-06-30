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
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.MultiChargeStateIsotopePattern;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.IsotopesUtils;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.Element;

/**
 * Core isotope pattern detection engine. For each charge hypothesis it collects candidate signals
 * (bidirectionally), collapses fine structure, scores the charge against a predicted
 * {@link EnvelopeModel}, and selects the most probable charge while flagging probable alternates.
 * The scoring replaces the previous "charge with most matched peaks" heuristic, which inflated
 * charge states.
 */
public class IsotopeFinderEngine {

  // an offset is "expected" / still supported if predicted >= this relative intensity
  private static final double ENGINE_CUTOFF = 0.02;
  // alternate charges with at least this probability share are flagged in addition to the winner
  private static final double ALT_THRESHOLD = 0.2;
  // look-ahead window (in offsets) used to bridge gaps during termination
  private static final int HORIZON = 4;
  // minimum isolated 13C peaks needed to assess the carbon envelope; below this the carbon fit is
  // neutral (1.0) and heavy-element coverage carries the detection
  private static final int MIN_LADDER_PEAKS = 2;
  // weak tie-breaker so a genuine higher charge (which also explains the intermediate peaks) edges out
  // a lower charge on otherwise-equal quality, without letting peak count dominate the bounded score
  private static final double TIE_WEIGHT = 0.1;
  // relative slack applied to the estimated M+1/M bounds in the optional "require 13C" gate
  private static final double C13_RATIO_SLACK = 0.3;

  private final int maxCharge;
  private final MZTolerance tol;
  private final EnvelopeModel model;
  private final String modeLabel;
  private final boolean requireC13;
  private final DoubleArrayList[] diffsForCharge;
  private final double[] maxDiff;

  public IsotopeFinderEngine(@NotNull final List<Element> elements, final int maxCharge,
      @NotNull final MZTolerance tol, @NotNull final EnvelopeModel model,
      @NotNull final String modeLabel, final boolean requireC13) {
    this.maxCharge = maxCharge;
    this.tol = tol;
    this.model = model;
    this.modeLabel = modeLabel;
    this.requireC13 = requireC13;
    this.diffsForCharge = IsotopesUtils.getIsotopesMzDiffsForCharge(elements, maxCharge);
    this.maxDiff = new double[maxCharge];
    for (int i = 0; i < maxCharge; i++) {
      double m = 0d;
      for (final double d : diffsForCharge[i]) {
        if (d > m) {
          m = d;
        }
      }
      // widen the search window like the original implementation
      maxDiff[i] = m + 10 * tol.getMzToleranceForMass(m);
    }
  }

  /**
   * @return whether any isotope m/z differences exist for the configured elements.
   */
  public boolean hasIsotopeDiffs() {
    return diffsForCharge.length > 0 && !diffsForCharge[0].isEmpty();
  }

  /**
   * Assemble the per-charge patterns (ordered best first) into a single {@link IsotopePattern},
   * forcing the winner to the front so it is the preferred pattern (works around the size-based
   * re-sort in {@link MultiChargeStateIsotopePattern}).
   */
  public static @NotNull IsotopePattern assemble(@NotNull final List<IsotopePattern> bestFirst) {
    if (bestFirst.size() == 1) {
      return bestFirst.getFirst();
    }
    final MultiChargeStateIsotopePattern multi = new MultiChargeStateIsotopePattern(
        bestFirst.getLast());
    for (int i = bestFirst.size() - 2; i >= 0; i--) {
      multi.addPattern(bestFirst.get(i), true); // insert at front, no re-sort
    }
    return multi;
  }

  /**
   * Detect the isotope pattern and charge on a single spectrum.
   *
   * @param spectrum the spectrum to search (most intense scan / best mobility scan).
   * @param mz       the searched signal m/z (feature m/z).
   * @param height   the feature height for IMS intensity normalization.
   * @param polarity ion polarity.
   * @return the detection result, or null if nothing was found.
   */
  public @Nullable DetectionResult detect(@Nullable final MassSpectrum spectrum, final double mz,
      final double height, @NotNull final PolarityType polarity) {
    if (spectrum == null || spectrum.getNumberOfDataPoints() == 0) {
      return null;
    }
    final SimpleDataPoint featureDp = new SimpleDataPoint(mz, height);
    final List<ChargeEval> evals = new ArrayList<>();

    for (int i = 0; i < maxCharge; i++) {
      final int z = i + 1;
      final DoubleArrayList diffs = diffsForCharge[i];
      if (diffs.isEmpty()) {
        continue;
      }
      List<DataPoint> candidates = IsotopesUtils.findIsotopesInScan(diffs, maxDiff[i], tol,
          spectrum, featureDp);
      if (spectrum instanceof MobilityScan && !candidates.isEmpty()) {
        candidates = normalizeImsIntensities(candidates, spectrum, featureDp);
      }
      if (candidates.size() <= 1) { // only the feature peak itself
        continue;
      }
      final IsotopeEnvelope env = model.buildEnvelope(mz, z, polarity);
      final double[] m1Bounds = requireC13 ? model.expectedM1RatioBounds(mz, z, polarity) : null;
      final ChargeEval eval = scoreCharge(z, candidates, env, m1Bounds);
      if (eval != null && eval.raw() > 0) {
        evals.add(eval);
      }
    }

    if (evals.isEmpty()) {
      return null;
    }

    // pseudo-probabilities by linear normalization of the (non-negative, coverage-weighted) scores
    double sum = 0d;
    for (final ChargeEval e : evals) {
      sum += e.raw();
    }
    evals.sort((a, b) -> Double.compare(b.raw(), a.raw())); // best first

    final List<IsotopePattern> patterns = new ArrayList<>();
    final List<ChargeScore> scores = new ArrayList<>();
    int bestCharge = 0;
    boolean first = true;
    for (final ChargeEval e : evals) {
      final double prob = sum > 0 ? e.raw() / sum : 0d;
      if (first || prob >= ALT_THRESHOLD) {
        final String desc = String.format("IsotopeFinder z=%d p=%.2f %s", e.charge(), prob,
            modeLabel);
        patterns.add(
            new SimpleIsotopePattern(e.keptCandidates(), e.charge(), IsotopePatternStatus.DETECTED,
                desc));
        scores.add(new ChargeScore(e.charge(), e.coverage(), e.carbonFit(), e.selfConsistency(),
                e.raw(), prob));
        if (first) {
          bestCharge = e.charge();
        }
      }
      first = false;
    }
    return new DetectionResult(bestCharge, scores, patterns);
  }

  private @Nullable ChargeEval scoreCharge(final int z, @NotNull final List<DataPoint> candidates,
      @NotNull final IsotopeEnvelope env, @Nullable final double[] m1Bounds) {
    final double spacingDa = env.spacingDa();

    // observed base peak (most intense candidate). Used only as the observed grid origin (offset 0);
    // the predicted envelope is slid over the observed ladder rather than pinning the base to a
    // predicted offset, so the score does not depend on where in the pattern the search started.
    DataPoint base = candidates.getFirst();
    for (final DataPoint dp : candidates) {
      if (dp.getIntensity() > base.getIntensity()) {
        base = dp;
      }
    }
    final double baseMz = base.getMZ();

    // isolated 13C ladder on the RAW signals: per offset, the signal closest to the exact 13C
    // position, so heavy isotopes (37Cl/81Br/34S) and 15N at the same nominal offset do not
    // contaminate the carbon ratio. Offsets are relative to the observed base (may be negative).
    final TreeMap<Integer, Double> carbonLadder = buildCarbonLadder(candidates, baseMz, spacingDa);

    // all-signal per-offset map (summed) for coverage, self-consistency and the inclusive kept pattern
    final TreeMap<Integer, OffsetPeak> observed = FineStructureCollapser.collapse(candidates,
        baseMz, 0, spacingDa);
    if (observed.isEmpty()) {
      return null;
    }

    // primary, position-agnostic carbon score: slide the carbon Poisson envelope over the isolated
    // 13C ladder. placement = predicted offset that aligns to observed offset 0 (the base).
    final CarbonFit carbonFit = slideCarbonFit(carbonLadder, env);
    final int placement = carbonFit.placement();

    // optional "require 13C" gate: the resolved 13C M+1 must be present and its M+1/M ratio within
    // the estimated min/max carbon bounds, otherwise this charge hypothesis is rejected. Mono and M+1
    // are read from the isolated 13C ladder at the placement-implied positions.
    if (requireC13) {
      final Double monoIntensity = carbonLadder.get(-placement);
      final Double m1Intensity = carbonLadder.get(-placement + 1);
      if (monoIntensity == null || monoIntensity <= 0 || m1Intensity == null || m1Intensity <= 0) {
        return null;
      }
      if (m1Bounds != null) {
        final double ratio = m1Intensity / monoIntensity;
        if (ratio < m1Bounds[0] * (1d - C13_RATIO_SLACK) || ratio > m1Bounds[1] * (1d
            + C13_RATIO_SLACK)) {
          return null;
        }
      }
    }

    // coverage: fraction of expected carbon offsets explained by ANY observed signal (incl. heavy).
    // predicted offset o aligns to observed offset (o - placement).
    int expectedTotal = 0;
    int expectedPresent = 0;
    for (int o = 0; o <= env.maxOffset(); o++) {
      if (env.expectedAt(o) >= ENGINE_CUTOFF) {
        expectedTotal++;
        if (observed.containsKey(o - placement)) {
          expectedPresent++;
        }
      }
    }
    final double coverage = expectedTotal == 0 ? 1d : (double) expectedPresent / expectedTotal;

    // self consistency: higher charges require their intermediate (e.g. half-spacing) peaks
    final double selfConsistency = selfConsistency(z, observed, env, placement);

    // envelope-shape-aware termination -> keep the supported, bridgeable run of offsets (both
    // directions from the base), so the inclusive pattern keeps heavy isotopes and fine structure.
    final Set<Integer> keptOffsets = computeKeptOffsets(observed, env, placement);
    final List<DataPoint> kept = new ArrayList<>();
    for (final DataPoint dp : candidates) {
      final int offset = (int) Math.round((dp.getMZ() - baseMz) / spacingDa);
      if (keptOffsets.contains(offset)) {
        kept.add(dp);
      }
    }
    if (kept.isEmpty()) {
      kept.addAll(candidates);
    }

    final int observedCount = keptOffsets.size();
    // bounded [0,1] quality (carbon fit x coverage), gated by self-consistency for higher charges so
    // a higher charge whose intermediate peaks are absent cannot win. observedCount enters only as a
    // weak multiplicative tie-breaker, letting a genuine higher charge edge out a lower one on ties.
    double quality = carbonFit.score() * coverage;
    if (z > 1) {
      quality *= selfConsistency;
    }
    double raw = quality * (1d + TIE_WEIGHT * observedCount);
    if (coverage <= 0 || observedCount < 2 || (z > 1 && selfConsistency <= 0)) {
      raw = 0d;
    }

    return new ChargeEval(z, raw, coverage, carbonFit.score(), selfConsistency,
        kept.toArray(new DataPoint[0]));
  }

  /**
   * Per integer offset relative to {@code baseMz}, the intensity of the signal closest to the exact
   * 13C position within tolerance. Signals off the exact 13C grid (heavy isotopes, 15N) are
   * excluded, so the carbon envelope is scored on the pure 13C ladder rather than on merged nominal
   * offsets.
   */
  private @NotNull TreeMap<Integer, Double> buildCarbonLadder(
      @NotNull final List<DataPoint> candidates, final double baseMz, final double spacingDa) {
    final TreeMap<Integer, Double> bestIntensity = new TreeMap<>();
    final TreeMap<Integer, Double> bestError = new TreeMap<>();
    for (final DataPoint dp : candidates) {
      final int k = (int) Math.round((dp.getMZ() - baseMz) / spacingDa);
      final double exactMz = baseMz + k * spacingDa;
      if (!tol.checkWithinTolerance(exactMz, dp.getMZ())) {
        continue; // not on the exact 13C grid -> heavy isotope / different element
      }
      final double err = Math.abs(dp.getMZ() - exactMz);
      final Double prev = bestError.get(k);
      if (prev == null || err < prev) {
        bestError.put(k, err);
        bestIntensity.put(k, dp.getIntensity());
      }
    }
    return bestIntensity;
  }

  /**
   * Slide the predicted carbon envelope over the observed 13C ladder and return the best bounded
   * cosine similarity together with the placement (the predicted offset aligned to observed offset
   * 0). When the ladder has too few isolated 13C peaks the carbon fit is neutral (1.0) and
   * heavy-element coverage carries the detection; the placement then defaults to the predicted base
   * offset.
   */
  private @NotNull CarbonFit slideCarbonFit(@NotNull final TreeMap<Integer, Double> ladder,
      @NotNull final IsotopeEnvelope env) {
    if (ladder.size() < MIN_LADDER_PEAKS) {
      return new CarbonFit(1d, env.baseOffset());
    }
    final int minK = ladder.firstKey();
    final int maxK = ladder.lastKey();
    double bestCos = -1d;
    int bestPlacement = env.baseOffset();
    // placement p in 0..maxOffset: observed offset 0 (base) aligns to predicted offset p
    for (int p = 0; p <= env.maxOffset(); p++) {
      final int from = Math.min(minK, -p);
      final int to = Math.max(maxK, env.maxOffset() - p);
      double dot = 0d;
      double na = 0d;
      double nb = 0d;
      for (int k = from; k <= to; k++) {
        final double obs = ladder.getOrDefault(k, 0d);
        final double pred = env.expectedAt(k + p);
        dot += obs * pred;
        na += obs * obs;
        nb += pred * pred;
      }
      if (na > 0d && nb > 0d) {
        final double cos = dot / (Math.sqrt(na) * Math.sqrt(nb));
        if (cos > bestCos) {
          bestCos = cos;
          bestPlacement = p;
        }
      }
    }
    return new CarbonFit(Math.max(0d, bestCos), bestPlacement);
  }

  private double selfConsistency(final int z, @NotNull final TreeMap<Integer, OffsetPeak> observed,
      @NotNull final IsotopeEnvelope env, final int placement) {
    if (z == 1) {
      return 1d;
    }
    int reqTotal = 0;
    int reqPresent = 0;
    // examine predicted offsets relative to the monoisotopic (offset 0). Offsets not divisible by z
    // are the intermediate (e.g. half-spacing) peaks that a lower-charge ladder would not have.
    // Predicted offset o aligns to observed offset (o - placement).
    for (int o = 1; o <= env.maxOffset(); o++) {
      if (o % z != 0 && env.expectedAt(o) >= ENGINE_CUTOFF) {
        reqTotal++;
        if (observed.containsKey(o - placement)) {
          reqPresent++;
        }
      }
    }
    if (reqTotal == 0) {
      // cannot confirm a higher charge from the available peaks -> do not promote it
      return 0d;
    }
    // use the presence fraction directly (no rounding up), so a single spurious half-spacing peak
    // does not fully satisfy the requirement for a higher charge
    return (double) reqPresent / reqTotal;
  }

  private Set<Integer> computeKeptOffsets(@NotNull final TreeMap<Integer, OffsetPeak> observed,
      @NotNull final IsotopeEnvelope env, final int placement) {
    final Set<Integer> kept = new HashSet<>();
    if (observed.isEmpty()) {
      return kept;
    }
    // observed offsets are relative to the base (offset 0). Predicted offset = observed + placement.
    kept.add(0);
    final int maxObs = observed.lastKey();
    final int minObs = observed.firstKey();

    // extend upward, bridging gaps only when the envelope still supports a peak ahead
    int current = 0;
    boolean advanced = true;
    while (advanced) {
      advanced = false;
      for (int k = current + 1; k <= current + HORIZON && k <= maxObs; k++) {
        if (observed.containsKey(k) && (k == current + 1
            || env.upperBoundAt(k + placement) >= ENGINE_CUTOFF)) {
          kept.add(k);
          current = k;
          advanced = true;
          break;
        }
      }
    }
    // extend downward (toward the monoisotopic / lower m/z), symmetric to the upward bridging
    current = 0;
    advanced = true;
    while (advanced) {
      advanced = false;
      for (int k = current - 1; k >= current - HORIZON && k >= minObs; k--) {
        if (observed.containsKey(k) && (k == current - 1
            || env.upperBoundAt(k + placement) >= ENGINE_CUTOFF)) {
          kept.add(k);
          current = k;
          advanced = true;
          break;
        }
      }
    }
    return kept;
  }

  private List<DataPoint> normalizeImsIntensities(@NotNull final List<DataPoint> candidates,
      @NotNull final MassSpectrum scan, @NotNull final SimpleDataPoint featureDp) {
    final int i = scan.binarySearch(featureDp.getMZ(), DefaultTo.CLOSEST_VALUE);
    if (i < 0) {
      return candidates;
    }
    final double intensity = scan.getIntensityValue(i);
    if (intensity <= 0) {
      return candidates;
    }
    final double factor = featureDp.getIntensity() / intensity;
    final List<DataPoint> out = new ArrayList<>(candidates.size());
    for (final DataPoint c : candidates) {
      if (!c.equals(featureDp)) {
        out.add(new SimpleDataPoint(c.getMZ(), c.getIntensity() * factor));
      } else {
        out.add(featureDp);
      }
    }
    return out;
  }

  private record ChargeEval(int charge, double raw, double coverage, double carbonFit,
                            double selfConsistency, DataPoint[] keptCandidates) {

  }

  /**
   * Result of sliding the carbon envelope over the observed 13C ladder.
   *
   * @param score     bounded cosine similarity in [0,1] (1.0 when too few 13C peaks to assess).
   * @param placement the predicted offset aligned to observed offset 0 (the base peak).
   */
  private record CarbonFit(double score, int placement) {

  }
}
