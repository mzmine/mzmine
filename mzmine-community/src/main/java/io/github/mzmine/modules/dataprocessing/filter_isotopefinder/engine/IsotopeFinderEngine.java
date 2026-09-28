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
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.ElementDetectionMode;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.IsotopesUtils;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  private static final double ENGINE_CUTOFF = IsotopeEnvelope.SUPPORT_CUTOFF;
  // ABSOLUTE margin on purpose: invariant to peak count and to how many hypotheses survived. A
  // maxCharge-dependent denominator used to make the true charge lose its alternate slot as
  // maxCharge grew.
  private static final double ALT_MARGIN = 0.15;
  private static final double MIN_ALT_QUALITY = 0.1;
  private static final int MIN_GAP_HORIZON = 4;
  private static final double GAP_HORIZON_FRACTION = 0.25;
  // below this the carbon fit is neutral and heavy-element coverage carries the detection
  private static final int MIN_LADDER_PEAKS = 2;
  // peak-count reward: lets a genuine higher charge beat a lower one that fits only a subsample.
  // decision: counts EVERY kept offset. Restricting it to predicted support as a harmonic guard
  // MEASURED WORSE - chargeTop1 0.9958 -> 0.9931 (upperBound gate) / 0.9885 (expected gate),
  // harmonic error 0.0019 -> 0.0048 / 0.0096 - because it strips a genuine envelope's tail offsets
  // faster than an interferent's, which sit inside the doubled charge's wider window anyway.
  private static final double TIE_WEIGHT = 0.1;
  // so a charge decided without a real 13C ladder cannot out-compete one with a real carbon fit
  private static final double NEUTRAL_FALLBACK_WEIGHT = 0.6;
  // keeps heavy isotopes (~4-5 mDa off the 13C grid) out of the spacing regression while still
  // admitting a near-but-off interferent peak, i.e. a fake harmonic ladder
  private static final double SPACING_GRID_FACTOR = 0.6;
  // tighter than the raw tolerance so an only-nearly-aligned interferent collapses the term
  private static final double SPACING_SIGMA_FACTOR = 0.35;
  // floor for a signal reached only by bridging a gap; contiguous signals are always kept.
  // decision: FLAT, not scaled with the predicted intensity - that raised noiseLeak (0.0116 ->
  // 0.0121) for no recall gain. Revisit only with real data that shows truncation.
  private static final double MIN_BRIDGED_REL_INTENSITY = 0.005;

  private final int maxCharge;
  private final MZTolerance tol;
  private final EnvelopeModel model;
  private final String modeLabel;
  private final boolean requireC13;
  private final DoubleArrayList[] diffsForCharge;
  private final double[] maxDiff;
  private final ElementDetectionMode elementDetectionMode;
  private final List<String> autoCandidates;
  private final boolean includeUserHeavies;
  // collects the emitted signals of a charge and, when the opt-in filter is enabled, drops those the
  // configured chemistry cannot explain
  private final ExplainableSignalFilter signalFilter;

  /**
   * @param config the full engine configuration, see {@link IsotopeFinderEngineConfig#of}.
   */
  public IsotopeFinderEngine(@NotNull final IsotopeFinderEngineConfig config) {
    final List<Element> elements = config.elements();
    this.maxCharge = config.maxCharge();
    this.tol = config.tol();
    this.model = config.model();
    this.modeLabel = config.modeLabel();
    this.requireC13 = config.requireC13();
    this.elementDetectionMode = config.elementDetectionMode();
    this.autoCandidates = config.autoCandidates();
    // user heavies are only added on top of the detected ones in the combined mode
    this.includeUserHeavies = elementDetectionMode == ElementDetectionMode.USER_PLUS_AUTO;
    this.signalFilter = ExplainableSignalFilter.create(config.explainableSignalsOnly(), elements,
        autoCandidates, tol);
    this.diffsForCharge = IsotopesUtils.getIsotopesMzDiffsForCharge(elements, maxCharge);
    this.maxDiff = new double[maxCharge];
    for (int i = 0; i < maxCharge; i++) {
      double m = 0d;
      for (final double d : diffsForCharge[i]) {
        if (d > m) {
          m = d;
        }
      }
      maxDiff[i] = m + 10 * tol.getMzToleranceForMass(m);
    }
  }

  public boolean hasIsotopeDiffs() {
    return diffsForCharge.length > 0 && !diffsForCharge[0].isEmpty();
  }

  /**
   * Assemble the per-charge patterns into one {@link IsotopePattern}, winner first.
   * <p>
   * decision: keep the selection order rather than re-deriving it from the stored
   * {@link IsotopePattern#getScore() score} - the winner comes from quality AND the peak-count
   * reward, while the score is quality x intensity agreement (display only), so re-sorting by it
   * could make {@code pattern.getCharge()} disagree with the feature's charge.
   *
   * @param bestFirst the per-charge patterns in selection order, winner first.
   */
  public static @NotNull IsotopePattern assemble(@NotNull final List<IsotopePattern> bestFirst) {
    if (bestFirst.size() == 1) {
      return bestFirst.getFirst();
    }
    return MultiChargeStateIsotopePattern.ofRanked(bestFirst);
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
    final List<Scored> scoredList = new ArrayList<>();

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
      // necessary condition only; the real gate is on distinct 13C-grid offsets in scoreCharge
      final int minCandidates = minSignalsForCharge(z);
      if (candidates.size() < minCandidates) {
        continue;
      }
      final IsotopeEnvelope env = model.buildEnvelope(mz, z, polarity);
      final double[] m1Bounds = model.expectedM1RatioBounds(mz, z, polarity);
      final ChargeEval eval = scoreCharge(z, mz, candidates, env, m1Bounds);
      if (eval != null && eval.raw() > 0) {
        // raw inputs kept so the winner can be re-scored once auto-detection rebuilds its bound
        scoredList.add(new Scored(eval, candidates, env, m1Bounds));
      }
    }

    if (scoredList.isEmpty()) {
      return null;
    }

    // decision: the WINNER goes by raw score (quality x peak-count reward, see TIE_WEIGHT); the
    // ALTERNATES below go by an absolute margin on quality alone.
    scoredList.sort((a, b) -> {
      final int byRaw = Double.compare(b.eval().raw(), a.eval().raw());
      return byRaw != 0 ? byRaw : Double.compare(b.eval().quality(), a.eval().quality());
    });

    // optional second pass: infer heavy elements from the RAW spectrum around the winning pattern
    // (off-ladder S/Si M+2 peaks are only there), rebuild its upper bound, re-score that charge
    DetectedComposition detectedComposition = null;
    if (elementDetectionMode != ElementDetectionMode.USER_DEFINED) {
      final Scored winner = scoredList.getFirst();
      final int z = winner.eval().charge();
      double lo = mz;
      double hi = mz;
      for (final DataPoint dp : winner.eval().keptCandidates()) {
        lo = Math.min(lo, dp.getMZ());
        hi = Math.max(hi, dp.getMZ());
      }
      final List<DataPoint> rawWindow = ElementAutoDetector.collectDetectionWindow(spectrum, lo, hi,
          z);
      final DetectedComposition comp = ElementAutoDetector.detect(rawWindow, z, tol,
          autoCandidates);
      if (!comp.elements().isEmpty()) {
        detectedComposition = comp;
        final Map<String, Integer> counts = new LinkedHashMap<>();
        for (final String sym : comp.elements()) {
          final int[] c = comp.counts().get(sym);
          counts.put(sym, c != null && c.length > 0 ? Math.max(1, c[c.length - 1]) : 1);
        }
        final IsotopeEnvelope env2 = model.buildEnvelope(mz, z, polarity, counts,
            includeUserHeavies);
        final ChargeEval reEval = scoreCharge(z, mz, winner.candidates(), env2, winner.m1Bounds());
        if (reEval != null && reEval.raw() > 0) {
          scoredList.set(0, new Scored(reEval, winner.candidates(), env2, winner.m1Bounds()));
        }
      }
    }

    double bestQuality = 0d;
    double qualitySum = 0d;
    for (final Scored s : scoredList) {
      bestQuality = Math.max(bestQuality, s.eval().quality());
      qualitySum += s.eval().quality();
    }

    final List<IsotopePattern> patterns = new ArrayList<>();
    final List<ChargeScore> scores = new ArrayList<>();
    final List<PatternAnchor> anchors = new ArrayList<>();
    int bestCharge = 0;
    boolean first = true;
    for (final Scored scored : scoredList) {
      final ChargeEval e = scored.eval();
      // the winner is always emitted; alternates must be near the best and clear a floor
      final boolean flag =
          first || (e.quality() >= bestQuality - ALT_MARGIN && e.quality() >= MIN_ALT_QUALITY);
      if (flag) {
        final double prob = qualitySum > 0 ? e.quality() / qualitySum : 0d;
        final String desc = String.format("IsotopeFinder z=%d p=%.2f %s", e.charge(), prob,
            modeLabel);
        // display/sorting only - the intensity-bound agreement never enters the charge selection
        final double patternScore = e.quality() * e.intensityAgreement();
        patterns.add(new SimpleIsotopePattern(e.keptCandidates(), e.charge(), patternScore,
            IsotopePatternStatus.DETECTED, desc));
        final ChargeScore score = new ChargeScore(e.charge(), e.coverage(), e.carbonFit(),
            e.selfConsistency(), e.spacingConsistency(), e.intensityAgreement(), patternScore,
            e.raw(), prob);
        scores.add(score);
        anchors.add(new PatternAnchor(scored.env(), e.baseMz(), e.placement()));
        if (first) {
          bestCharge = e.charge();
        }
      }
      first = false;
    }
    return new DetectionResult(bestCharge, scores, patterns, anchors, detectedComposition);
  }

  private @Nullable ChargeEval scoreCharge(final int z, final double searchedMz,
      @NotNull final List<DataPoint> candidates, @NotNull final IsotopeEnvelope env,
      final double @NotNull [] m1Bounds) {
    final double spacingDa = env.spacingDa();

    // every crop below is grown out of this, so the emitted pattern always contains the searched signal
    final DataPoint searched = closestCandidate(candidates, searchedMz);
    final double anchorMz = searched != null ? searched.getMZ() : mostIntense(candidates).getMZ();

    // the most intense candidate is the grid ORIGIN (offset 0) only - the predicted envelope is slid
    // over the ladder rather than pinned to it, so the score is independent of where the search started
    DataPoint base = mostIntense(candidates);
    CarbonLadder ladder = CarbonLadder.build(candidates, base.getMZ(), spacingDa, tol);
    int anchorOffset = (int) Math.round((anchorMz - base.getMZ()) / spacingDa);
    // decision: that origin is only valid while it belongs to the SEARCHED signal's own cluster.
    // Candidate collection chains outward through isotope distances and can reach an unrelated,
    // stronger cluster tens of Da away (real case: searching m/z 667.311 emitted a z=2 pattern of
    // 652.306-654.315). 2+ positions required, or a single peak would decide the charge.
    final OffsetSpan cluster = ladder.clusterSpanAround(anchorOffset, anchorMz);
    if (!cluster.contains(0) && cluster.size() >= 2) {
      base = mostIntenseWithin(candidates, base.getMZ(), spacingDa, cluster);
      ladder = CarbonLadder.build(candidates, base.getMZ(), spacingDa, tol);
      anchorOffset = (int) Math.round((anchorMz - base.getMZ()) / spacingDa);
    }
    final double baseMz = base.getMZ();

    // require-13C: truncate at the first missing grid position even if signals exist beyond it - a
    // strong discriminator against fake high-charge ladders from noise / FT ringing. The mono is NOT
    // required, so a mid-envelope hump without a visible mono (a protein) still passes.
    final List<DataPoint> cands;
    final int ladderStep;
    if (requireC13) {
      final OffsetSpan validated = ladder.requireC13Span();
      if (validated == null) {
        return null; // no gap-free 13C (or every-second) ladder through the seed
      }
      ladderStep = validated.step();
      // decision: the VALIDATED span decides acceptance, but the CROP widens to the searched signal.
      // The nominal 13C grid drifts against a polyhalogen comb (~4.5 mDa per offset), so the gap-free
      // walk runs out of tolerance a dozen offsets out, and cropping there would drop the very signal
      // the pattern belongs to.
      final OffsetSpan crop = validated.extendTo(anchorOffset);
      final List<DataPoint> truncated = new ArrayList<>(candidates.size());
      for (final DataPoint dp : candidates) {
        if (crop.contains((int) Math.round((dp.getMZ() - baseMz) / spacingDa))) {
          truncated.add(dp);
        }
      }
      cands = truncated;
      ladder = CarbonLadder.build(cands, baseMz, spacingDa, tol);
    } else {
      cands = candidates;
      ladderStep = 1;
    }

    // isolated ladder: heavy isotopes and 15N at the same nominal offset must not contaminate the
    // carbon ratio. Offsets are relative to the observed base, so they may be negative.
    final TreeMap<Integer, Double> carbonLadder = ladder.onGridIntensities(1d);

    final TreeMap<Integer, OffsetPeak> observed = ladder.collapsed();
    if (observed.isEmpty()) {
      return null;
    }

    // placement = the predicted offset that aligns to observed offset 0 (the base)
    final CarbonFit carbonFit = slideCarbonFit(carbonLadder, env);
    final int placement = carbonFit.placement();

    // measured once at the placement anchor: both the hard gate below and the soft plausibility
    // penalty further down read it, so the anchor and mono-dominance test exist in one place
    final CarbonRatio carbonRatio = CarbonRatio.measure(carbonLadder, placement);

    // an "M+1" far too small (FT ringing) or too large (a co-eluting mono) rejects the hypothesis.
    // Only where the anchor really is a dominant mono on the every-13C ladder: mid-envelope humps and
    // the every-second ladder have no mono to anchor the ratio. A merged M+1 is left to the soft penalty.
    if (requireC13 && ladderStep == 1 && carbonRatio.failsRequireC13Gate(m1Bounds)) {
      return null;
    }

    // predicted offset o aligns to observed offset (o - placement).
    // decision: weighted by predicted intensity, not an offset count - missing a small tail peak must
    // cost far less than missing the apex. An unweighted count under-scored low-m/z multiply-charged
    // ions, whose broad envelope predicts many tail offsets below a real spectrum's noise floor.
    double expectedWeight = 0d;
    double presentWeight = 0d;
    for (int o = 0; o <= env.maxOffset(); o++) {
      final double w = env.expectedAt(o);
      if (w >= ENGINE_CUTOFF) {
        expectedWeight += w;
        if (observed.containsKey(o - placement)) {
          presentWeight += w;
        }
      }
    }
    final double coverage = expectedWeight <= 0d ? 1d : presentWeight / expectedWeight;

    // self consistency: higher charges require their intermediate (e.g. half-spacing) peaks - but
    // only those that would actually be detectable above this spectrum's own intensity floor
    final double selfConsistency = selfConsistency(z, observed, env, placement, base.getIntensity());

    // envelope-shape-aware termination -> keep the supported, bridgeable run of offsets (both
    // directions from the base), so the inclusive pattern keeps heavy isotopes and fine structure.
    // Insignificant signals reached only by bridging a gap are dropped so the pattern does not span
    // too wide over noise; contiguous signals are always kept. In require-13C mode the accepted
    // ladder span already defines a validated gap-free pattern, so keep all of it (the every-second
    // ladder has intentional single-offset gaps the shape-aware termination would otherwise prune).
    final double baseIntensity = base.getIntensity();
    final Set<Integer> keptOffsets = requireC13 ? new HashSet<>(observed.keySet())
        : computeKeptOffsets(observed, env, placement, baseIntensity);
    // the offsets actually reported, which always reach the searched signal (see emittedOffsets)
    final Set<Integer> emitOffsets = emittedOffsets(keptOffsets, observed, anchorOffset);
    // the signals reported at those offsets, minus the unexplainable ones when the opt-in filter is
    // enabled (see ExplainableSignalFilter). Charge selection above is unaffected either way.
    final List<DataPoint> kept = signalFilter.collectEmitted(cands, emitOffsets, baseMz, spacingDa,
        z);

    // intensity agreement: fraction of the observed intensity that stays within the plausible upper
    // bound of the predicted envelope (signals within the bound, incl. heavy isotopes, add no
    // penalty). This feeds the stored/sorting pattern score only, NOT the charge selection below,
    // because the carbon-model upper bound cannot reliably bound heavy-halogen envelopes. Bounded [0,1].
    double excess = 0d;
    double totalRel = 0d;
    for (final int k : keptOffsets) {
      final OffsetPeak peak = observed.get(k);
      if (peak == null) {
        continue;
      }
      final double relObs = peak.intensity() / baseIntensity;
      final double predUpper = env.upperBoundAt(k + placement);
      excess += Math.max(0d, relObs - predUpper);
      totalRel += relObs;
    }
    final double intensityAgreement = totalRel > 0d ? Math.max(0d, 1d - excess / totalRel) : 1d;

    final int observedCount = keptOffsets.size();

    // decision: diagnostics only, NOT folded into the selection quality. A naive multiplicative fold
    // regressed polyhalogen combs: a Cl2/Br2 comb at z=2 has ~1 Da steps that nearly align to the z=1
    // grid, which wrongly boosted z=1. Folding it in only between divisor/multiple charge pairs would
    // restore the harmonic guard without that regression - not done, see TIE_WEIGHT.
    final double spacingConsistency = spacingConsistency(ladder, baseMz);

    // self-consistency gate: a higher charge whose intermediate peaks are absent must not win
    double quality = carbonFit.score() * coverage;
    if (z > 1) {
      quality *= selfConsistency;
    }
    // detection still succeeds (raw stays > 0); the charge just ranks below one with a real fit
    if (!carbonFit.assessed()) {
      quality *= NEUTRAL_FALLBACK_WEIGHT;
    }
    // two-sided factor on the SAME anchored ratio the optional gate above used
    if (carbonFit.assessed()) {
      quality *= carbonRatio.plausibility(m1Bounds);
    }
    double raw = quality * (1d + TIE_WEIGHT * observedCount);
    // hard misdetection guard. Counting ONLY isolated 13C-ladder peaks (heavy isotopes and off-grid
    // noise excluded) is what forces a high charge to be backed by a real ladder.
    final int c13Signals = carbonLadder.size();
    final int minSignals = minSignalsForCharge(z);
    // at floor 2, heavy-isotope spacing alone may carry it (a C,Br molecule with a weak 13C M+1 but a
    // strong 81Br M+2), so any two isotope signals qualify; the z >= 4 floors need real 13C signals
    final boolean enoughSignals =
        c13Signals >= minSignals || (minSignals <= 2 && observedCount >= 2);
    if (coverage <= 0 || !enoughSignals || (z > 1 && selfConsistency <= 0)) {
      raw = 0d;
    }

    return new ChargeEval(z, raw, quality, coverage, carbonFit.score(), selfConsistency,
        spacingConsistency, intensityAgreement, carbonFit.assessed(),
        kept.toArray(new DataPoint[0]), baseMz, placement);
  }

  /**
   * Distinct 13C-grid offsets required to accept a charge hypothesis. The floor rises with the
   * charge so a high charge needs genuine multi-isotope evidence rather than a few noise peaks that
   * happen to land on the fine (1.00336/z Da) grid. Deliberately flat beyond z=10 - this is a hard
   * misdetection cutoff, not a graduated score term.
   */
  private static int minSignalsForCharge(final int z) {
    if (z > 9) {
      return 5;
    }
    if (z > 5) {
      return 4;
    }
    if (z > 3) {
      return 3;
    }
    return 2;
  }

  private @Nullable DataPoint closestCandidate(@NotNull final List<DataPoint> candidates,
      final double searchedMz) {
    DataPoint best = null;
    double bestDiff = Double.MAX_VALUE;
    for (final DataPoint dp : candidates) {
      final double diff = Math.abs(dp.getMZ() - searchedMz);
      if (diff < bestDiff && tol.checkWithinTolerance(searchedMz, dp.getMZ())) {
        best = dp;
        bestDiff = diff;
      }
    }
    return best;
  }

  private static @NotNull DataPoint mostIntense(@NotNull final List<DataPoint> candidates) {
    DataPoint best = candidates.getFirst();
    for (final DataPoint dp : candidates) {
      if (dp.getIntensity() > best.getIntensity()) {
        best = dp;
      }
    }
    return best;
  }

  private static @NotNull DataPoint mostIntenseWithin(@NotNull final List<DataPoint> candidates,
      final double baseMz, final double spacingDa, @NotNull final OffsetSpan span) {
    DataPoint best = null;
    for (final DataPoint dp : candidates) {
      final int k = (int) Math.round((dp.getMZ() - baseMz) / spacingDa);
      if (span.contains(k) && (best == null || dp.getIntensity() > best.getIntensity())) {
        best = dp;
      }
    }
    return best != null ? best : mostIntense(candidates);
  }


  /**
   * Slide the predicted carbon envelope over the observed 13C ladder, returning the best bounded
   * cosine similarity and the placement. Position-agnostic on purpose: the score must not depend on
   * which peak of the pattern was searched.
   */
  private @NotNull CarbonFit slideCarbonFit(@NotNull final TreeMap<Integer, Double> ladder,
      @NotNull final IsotopeEnvelope env) {
    if (ladder.size() < MIN_LADDER_PEAKS) {
      // neutral fit, flagged unassessed so the caller can down-weight this charge
      return new CarbonFit(1d, env.baseOffset(), false);
    }
    // both norms are INVARIANT across placements, so only the dot product varies - and that needs
    // only the ladder's sparse keys. O(maxOffset x range) -> O(maxOffset x ladderSize), bit-identical.
    double observedNormSq = 0d;
    for (final double obs : ladder.values()) {
      observedNormSq += obs * obs;
    }
    double predictedNormSq = 0d;
    for (int o = 0; o <= env.maxOffset(); o++) {
      final double pred = env.expectedAt(o);
      predictedNormSq += pred * pred;
    }
    if (observedNormSq <= 0d || predictedNormSq <= 0d) {
      // no placement could yield a defined cosine; a 13C ladder WAS available, so still "assessed"
      return new CarbonFit(0d, env.baseOffset(), true);
    }
    final double norm = Math.sqrt(observedNormSq) * Math.sqrt(predictedNormSq);

    double bestDot = Double.NEGATIVE_INFINITY;
    int bestPlacement = env.baseOffset();
    for (int p = 0; p <= env.maxOffset(); p++) {
      double dot = 0d;
      for (final var entry : ladder.entrySet()) {
        dot += entry.getValue() * env.expectedAt(entry.getKey() + p);
      }
      if (dot > bestDot) {
        bestDot = dot;
        bestPlacement = p;
      }
    }
    return new CarbonFit(Math.max(0d, bestDot / norm), bestPlacement, true);
  }

  /**
   * Fit one anchored spacing to the on-grid peak positions and score the residual m/z drift: a
   * clean single-spacing ladder gives ~1, a wrong neighbouring charge accumulates residual and
   * collapses the term. Positions only, so it is independent of the carbon fit and the intensity
   * penalty.
   *
   * @return bounded [0,1] (1 = one clean spacing explains the ladder).
   */
  private double spacingConsistency(@NotNull final CarbonLadder ladder, final double baseMz) {
    // tight window: excludes heavy isotopes but keeps near-but-off interferent peaks
    final TreeMap<Integer, Double> mzByOffset = ladder.onGridMz(SPACING_GRID_FACTOR);
    if (mzByOffset.size() < 2) {
      return 1d; // too few on-grid peaks to assess a spacing -> neutral
    }
    double sumKd = 0d;
    double sumK2 = 0d;
    for (final var entry : mzByOffset.entrySet()) {
      final int k = entry.getKey();
      final double dmz = entry.getValue() - baseMz;
      sumKd += k * dmz;
      sumK2 += (double) k * k;
    }
    if (sumK2 == 0d) {
      return 1d; // only the base carries a zero offset -> nothing to regress
    }
    final double slope = sumKd / sumK2;
    double sumSq = 0d;
    for (final var entry : mzByOffset.entrySet()) {
      final int k = entry.getKey();
      final double dmz = entry.getValue() - baseMz;
      final double resid = dmz - slope * k;
      sumSq += resid * resid;
    }
    final double eps = Math.sqrt(sumSq / mzByOffset.size());
    final double sigma = SPACING_SIGMA_FACTOR * tol.getMzToleranceForMass(baseMz);
    if (sigma <= 0d) {
      return 1d;
    }
    final double ratio = eps / sigma;
    return Math.exp(-ratio * ratio);
  }

  private double selfConsistency(final int z, @NotNull final TreeMap<Integer, OffsetPeak> observed,
      @NotNull final IsotopeEnvelope env, final int placement, final double baseIntensity) {
    if (z == 1) {
      return 1d;
    }
    // detection floor of THIS spectrum: a predicted peak below it would not be visible even if it
    // existed, so its absence says nothing about the charge.
    // decision: without this, a noise floor removed a genuine higher charge's weak intermediate peaks
    // and this term collapsed - every charge error on the benchmark's cutoff axis was 2->1 or 3->1.
    double floor = Double.MAX_VALUE;
    if (baseIntensity > 0d) {
      for (final OffsetPeak peak : observed.values()) {
        floor = Math.min(floor, peak.intensity() / baseIntensity);
      }
    }
    if (floor == Double.MAX_VALUE) {
      floor = 0d;
    }

    int reqTotal = 0;
    int reqPresent = 0;
    // offsets not divisible by z are the intermediate peaks a lower-charge ladder would not have
    for (int o = 1; o <= env.maxOffset(); o++) {
      if (o % z != 0 && env.expectedAt(o) >= ENGINE_CUTOFF) {
        if (observed.containsKey(o - placement)) {
          reqTotal++;
          reqPresent++;
        } else if (env.expectedAt(o) >= floor) {
          // predicted ABOVE this spectrum's detection floor but absent -> genuine evidence against
          reqTotal++;
        }
        // predicted below the floor and absent -> undetectable either way, so it is not counted
      }
    }
    if (reqTotal == 0) {
      // cannot confirm a higher charge from the available peaks -> do not promote it
      return 0d;
    }
    // the raw fraction, so one spurious half-spacing peak does not fully satisfy a higher charge
    return (double) reqPresent / reqTotal;
  }

  /**
   * The offsets the pattern is SCORED on: the supported, bridgeable run around the base (offset 0).
   * See {@link #emittedOffsets} for the reported ones, which can be wider.
   */
  private Set<Integer> computeKeptOffsets(@NotNull final TreeMap<Integer, OffsetPeak> observed,
      @NotNull final IsotopeEnvelope env, final int placement, final double baseIntensity) {
    final Set<Integer> kept = new HashSet<>();
    if (observed.isEmpty()) {
      return kept;
    }
    kept.add(0);
    final int maxObs = observed.lastKey();
    final int minObs = observed.firstKey();
    final int horizon = gapHorizon(env);

    // bridge a gap only where the envelope still supports a peak ahead AND the gap-crossing signal
    // is significant, so noise cannot widen the pattern. Downward below is symmetric.
    int current = 0;
    boolean advanced = true;
    while (advanced) {
      advanced = false;
      for (int k = current + 1; k <= current + horizon && k <= maxObs; k++) {
        if (observed.containsKey(k) && (k == current + 1 || (
            env.upperBoundAt(k + placement) >= ENGINE_CUTOFF && significant(observed.get(k),
                baseIntensity)))) {
          kept.add(k);
          current = k;
          advanced = true;
          break;
        }
      }
    }
    current = 0;
    advanced = true;
    while (advanced) {
      advanced = false;
      for (int k = current - 1; k >= current - horizon && k >= minObs; k--) {
        if (observed.containsKey(k) && (k == current - 1 || (
            env.upperBoundAt(k + placement) >= ENGINE_CUTOFF && significant(observed.get(k),
                baseIntensity)))) {
          kept.add(k);
          current = k;
          advanced = true;
          break;
        }
      }
    }
    return kept;
  }

  /**
   * The offsets the pattern is EMITTED at: the scored offsets, widened to reach the searched signal
   * when the shape-aware walk stopped short of it. The searched signal is always in its own pattern.
   * <p>
   * decision: the widening stays OUT of the scoring set, so the assigned charge does not depend on
   * which isotope peak was searched - only the reported signals do.
   */
  private static @NotNull Set<Integer> emittedOffsets(@NotNull final Set<Integer> kept,
      @NotNull final TreeMap<Integer, OffsetPeak> observed, final int anchorOffset) {
    if (kept.contains(anchorOffset)) {
      return kept;
    }
    final Set<Integer> emitted = new HashSet<>(kept);
    for (int k = Math.min(anchorOffset, 0); k <= Math.max(anchorOffset, 0); k++) {
      if (observed.containsKey(k)) {
        emitted.add(k);
      }
    }
    return emitted;
  }

  /**
   * How many offsets one gap may span, scaled to the predicted envelope width.
   * <p>
   * decision: a fixed look-ahead is wrong at both ends of the charge range - 4 offsets already reach
   * past a small molecule's whole envelope, while inside a protein's dozens it is a tiny fraction.
   * Floored so nothing narrows; measured neutral on the corpus, i.e. a structural guard for
   * envelopes wider than the corpus contains rather than a scoring change.
   */
  private static int gapHorizon(@NotNull final IsotopeEnvelope env) {
    int supported = 0;
    for (int o = 0; o <= env.maxOffset(); o++) {
      if (env.expectedAt(o) >= ENGINE_CUTOFF) {
        supported++;
      }
    }
    return Math.max(MIN_GAP_HORIZON, (int) Math.round(supported * GAP_HORIZON_FRACTION));
  }

  /**
   * @return whether the peak clears {@link #MIN_BRIDGED_REL_INTENSITY}; only gap-bridged signals
   * are subject to it.
   */
  private boolean significant(@Nullable final OffsetPeak peak, final double baseIntensity) {
    if (peak == null || baseIntensity <= 0d) {
      return true;
    }
    return peak.intensity() / baseIntensity >= MIN_BRIDGED_REL_INTENSITY;
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

  private record ChargeEval(int charge, double raw, double quality, double coverage,
                            double carbonFit, double selfConsistency, double spacingConsistency,
                            double intensityAgreement, boolean carbonAssessed,
                            DataPoint[] keptCandidates, double baseMz, int placement) {

  }

  private record Scored(@NotNull ChargeEval eval, @NotNull List<DataPoint> candidates,
                        @NotNull IsotopeEnvelope env, @NotNull double[] m1Bounds) {

  }

  /**
   * @param score     bounded cosine similarity, 1.0 when too few 13C peaks to assess.
   * @param placement the predicted offset aligned to observed offset 0 (the base peak).
   * @param assessed  false when the neutral 1.0 fallback was used instead of a real ladder fit.
   */
  private record CarbonFit(double score, int placement, boolean assessed) {

  }
}
