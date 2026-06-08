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
  // allow observed intensities up to this multiple of the predicted upper bound
  private static final double UPPER_SLACK = 1.5;
  private static final double EXCESS_WEIGHT = 1.0;
  // exponents weighting the sub-scores in the combined raw score
  private static final double W_ENVELOPE = 1.5;
  private static final double W_SELF = 2.0;

  private final int maxCharge;
  private final MZTolerance tol;
  private final EnvelopeModel model;
  private final String modeLabel;
  private final DoubleArrayList[] diffsForCharge;
  private final double[] maxDiff;

  public IsotopeFinderEngine(@NotNull final List<Element> elements, final int maxCharge,
      @NotNull final MZTolerance tol, @NotNull final EnvelopeModel model,
      @NotNull final String modeLabel) {
    this.maxCharge = maxCharge;
    this.tol = tol;
    this.model = model;
    this.modeLabel = modeLabel;
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
      final ChargeEval eval = scoreCharge(z, candidates, env);
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
        scores.add(
            new ChargeScore(e.charge(), e.spacingFraction(), e.envelopeFit(), e.selfConsistency(),
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
      @NotNull final IsotopeEnvelope env) {
    final double spacingDa = env.spacingDa();

    // observed base peak
    DataPoint base = candidates.getFirst();
    for (final DataPoint dp : candidates) {
      if (dp.getIntensity() > base.getIntensity()) {
        base = dp;
      }
    }
    final double baseMz = base.getMZ();
    final int predictedBaseOffset = env.baseOffset();

    final TreeMap<Integer, OffsetPeak> observed = FineStructureCollapser.collapse(candidates,
        baseMz, predictedBaseOffset, spacingDa);
    double baseIntensity = 0d;
    for (final OffsetPeak p : observed.values()) {
      baseIntensity = Math.max(baseIntensity, p.intensity());
    }
    if (baseIntensity <= 0 || observed.isEmpty()) {
      return null;
    }

    // spacing fraction: how many expected offsets are observed
    int expectedTotal = 0;
    int expectedPresent = 0;
    for (int o = 0; o <= env.maxOffset(); o++) {
      if (env.expectedAt(o) >= ENGINE_CUTOFF) {
        expectedTotal++;
        if (observed.containsKey(o)) {
          expectedPresent++;
        }
      }
    }
    final double spacingFraction =
        expectedTotal == 0 ? 1d : (double) expectedPresent / expectedTotal;

    // one-sided envelope fit: penalize implausibly large observed intensities only
    double envelopeFit = 1d;
    for (final OffsetPeak p : observed.values()) {
      final double rel = p.intensity() / baseIntensity;
      final double tolerated = Math.max(env.upperBoundAt(p.offset()) * UPPER_SLACK, ENGINE_CUTOFF);
      if (rel > tolerated) {
        final double excess = (rel - tolerated) / rel; // 0..1
        envelopeFit *= Math.max(0d, 1d - excess * EXCESS_WEIGHT);
      }
    }

    // self consistency: higher charges require their intermediate (e.g. half-spacing) peaks
    final double selfConsistency = selfConsistency(z, observed, env);

    // envelope-shape-aware termination -> keep only the supported, bridgeable run of offsets
    final Set<Integer> keptOffsets = computeKeptOffsets(observed, env, predictedBaseOffset);
    final List<DataPoint> kept = new ArrayList<>();
    for (final DataPoint dp : candidates) {
      final int offset = predictedBaseOffset + (int) Math.round((dp.getMZ() - baseMz) / spacingDa);
      if (keptOffsets.contains(offset)) {
        kept.add(dp);
      }
    }
    if (kept.isEmpty()) {
      kept.addAll(candidates);
    }

    // coverage: number of explained offsets. This is the tie-breaker that lets a genuine higher
    // charge (which explains the intermediate peaks too) win over a lower charge that only explains
    // a subset. It cannot inflate the charge because selfConsistency gates higher charges to 0 when
    // their required intermediate peaks are absent.
    final int observedCount = keptOffsets.size();
    double raw =
        spacingFraction * Math.pow(envelopeFit, W_ENVELOPE) * Math.pow(selfConsistency, W_SELF)
            * observedCount;
    if (spacingFraction <= 0 || observedCount < 2 || (z > 1 && selfConsistency <= 0)) {
      raw = 0d;
    }

    return new ChargeEval(z, raw, spacingFraction, envelopeFit, selfConsistency,
        kept.toArray(new DataPoint[0]));
  }

  private double selfConsistency(final int z, @NotNull final TreeMap<Integer, OffsetPeak> observed,
      @NotNull final IsotopeEnvelope env) {
    if (z == 1) {
      return 1d;
    }
    final int maxObs = observed.lastKey();
    int reqTotal = 0;
    int reqPresent = 0;
    for (int o = 1; o <= maxObs; o++) {
      // intermediate offsets that a charge-1 ladder would not have
      if (o % z != 0 && env.expectedAt(o) >= ENGINE_CUTOFF) {
        reqTotal++;
        if (observed.containsKey(o)) {
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
      @NotNull final IsotopeEnvelope env, final int baseOffset) {
    final Set<Integer> kept = new HashSet<>();
    if (observed.isEmpty()) {
      return kept;
    }
    kept.add(baseOffset);
    final int maxObs = observed.lastKey();
    final int minObs = observed.firstKey();

    // extend upward, bridging gaps only when the envelope still supports a peak ahead
    int current = baseOffset;
    boolean advanced = true;
    while (advanced) {
      advanced = false;
      for (int k = current + 1; k <= current + HORIZON && k <= maxObs; k++) {
        if (observed.containsKey(k) && (k == current + 1 || env.upperBoundAt(k) >= ENGINE_CUTOFF)) {
          kept.add(k);
          current = k;
          advanced = true;
          break;
        }
      }
    }
    // extend downward
    current = baseOffset;
    advanced = true;
    while (advanced) {
      advanced = false;
      for (int k = current - 1; k >= current - HORIZON && k >= minObs; k--) {
        if (observed.containsKey(k) && (k == current - 1 || env.upperBoundAt(k) >= ENGINE_CUTOFF)) {
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

  private record ChargeEval(int charge, double raw, double spacingFraction, double envelopeFit,
                            double selfConsistency, DataPoint[] keptCandidates) {

  }
}
