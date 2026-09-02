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

  // an offset is "expected" / still supported if predicted >= this relative intensity
  private static final double ENGINE_CUTOFF = IsotopeEnvelope.SUPPORT_CUTOFF;
  // an alternate charge is flagged in addition to the winner if its bounded quality is within this
  // absolute margin of the best bounded quality. An absolute margin is invariant to peak count and to
  // how many charge hypotheses survived (a maxCharge-dependent probability denominator was the old
  // bug that made the true charge lose its alternate slot as maxCharge grew).
  private static final double ALT_MARGIN = 0.15;
  // an alternate charge must also reach at least this bounded quality to be flagged at all
  private static final double MIN_ALT_QUALITY = 0.1;
  // smallest look-ahead window (in offsets) used to bridge gaps during termination. The effective
  // window scales with the predicted envelope width (see gapHorizon); this is the floor for narrow
  // small-molecule envelopes.
  private static final int MIN_GAP_HORIZON = 4;
  // fraction of the predicted (above-cutoff) envelope width used as the gap look-ahead
  private static final double GAP_HORIZON_FRACTION = 0.25;
  // minimum isolated 13C peaks needed to assess the carbon envelope; below this the carbon fit is
  // neutral (1.0) and heavy-element coverage carries the detection
  private static final int MIN_LADDER_PEAKS = 2;
  // reward for the number of isotope offsets a charge explains: a genuine higher charge explains more
  // real isotope peaks (its own full envelope), so this lets it win over a lower charge that fits only a
  // subsample of the ladder and leaves the intermediate peaks unexplained.
  // decision: the reward counts EVERY kept offset, not only those the predicted envelope covers.
  // Restricting it to predicted support was tried as a harmonic guard and MEASURED WORSE on the whole
  // corpus, in both variants (ALL chargeTop1 0.9958 baseline; 0.9931 gated on upperBound >= cutoff,
  // 0.9885 gated on expected >= cutoff, with the harmonic error rate rising 0.0019 -> 0.0048 -> 0.0096
  // and polyhalogen dropping to 0.9845 / 0.9742). The gate removes the TAIL offsets of a GENUINE
  // envelope - exactly where a real higher charge earns its advantage - faster than it removes an
  // interferent's peaks, which sit inside the doubled charge's (wider) predicted window anyway.
  // So the harmonic incentive is bounded only by the carbon M+1/M plausibility factor; the
  // spacing-consistency term would discriminate the rest but is not folded in (see
  // spacingConsistency). Residual measured harmonic error rate on the corpus: 0.0019.
  private static final double TIE_WEIGHT = 0.1;
  // a charge decided without a genuine 13C ladder (carbon fit fell back to the neutral 1.0) is
  // down-weighted by this factor so it cannot out-compete a charge with a real carbon fit on a tie
  private static final double NEUTRAL_FALLBACK_WEIGHT = 0.6;
  // spacing-consistency: only peaks within this fraction of the m/z tolerance of the exact 13C
  // position enter the spacing regression, so heavy isotopes (Cl/Br/S/Si, ~4-5 mDa off the 13C grid)
  // do not distort it while a near-but-off interferent peak (a fake harmonic ladder) still counts.
  private static final double SPACING_GRID_FACTOR = 0.6;
  // spacing-consistency: residual m/z drift is scored relative to this fraction of the tolerance, so
  // a genuine single-spacing ladder (residual ~0) stays ~1 while an interferent that only nearly
  // aligns to a doubled-charge grid collapses the term. Tighter than the raw tolerance on purpose.
  private static final double SPACING_SIGMA_FACTOR = 0.35;
  // a signal reached only by bridging a gap (not directly adjacent to the kept run) must be at least
  // this fraction of the base peak to be included, so insignificant noise on the tails does not widen
  // the pattern. Contiguous/adjacent signals are always kept to preserve complete isotope envelopes.
  // Kept FLAT on purpose: relaxing it proportionally to the predicted intensity (so the broad tail of
  // a high-charge envelope, whose real peaks sit at a few per mille, is not truncated by a constant
  // tuned on small molecules) was measured and rejected - it raised noiseLeak (0.0116 -> 0.0121 on the
  // noise axis, 0.0174 -> 0.0177 overall) and bought no completeness, because the corpus's wide
  // envelopes already reach patternRecall 1.0000. Revisit only with real data that shows truncation.
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
   * Assemble the per-charge patterns into a single {@link IsotopePattern}, preserving the order the
   * engine selected them in so the preferred pattern is the winning charge.
   * <p>
   * decision: the order is preserved rather than re-derived from the stored
   * {@link IsotopePattern#getScore() score}. The winner is chosen from the bounded quality AND a
   * peak-count reward, while the stored score is the bounded quality times the intensity agreement
   * (a display value that deliberately stays out of the selection). Re-sorting by the score could
   * therefore make {@code pattern.getCharge()} disagree with the charge assigned to the feature.
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
      // decision: require a charge-scaled minimum number of signals so a high charge is only ever
      // reported when there is genuine multi-isotope evidence for it, not a couple of noise peaks that
      // happen to fall on the fine (1.00336/z Da) grid. This is a HARD cutoff (misdetection guard), not
      // a soft score term. See minSignalsForCharge for the fixed levels. Cheap pre-filter on the raw
      // candidate count (a necessary condition); the authoritative gate is on the distinct occupied
      // 13C-grid offsets in scoreCharge (raw = 0 veto below).
      final int minCandidates = minSignalsForCharge(z);
      if (candidates.size() < minCandidates) {
        continue;
      }
      final IsotopeEnvelope env = model.buildEnvelope(mz, z, polarity);
      // carbon M+1/M bounds drive both the optional require-13C gate and the carbon-ratio
      // plausibility penalty in scoreCharge
      final double[] m1Bounds = model.expectedM1RatioBounds(mz, z, polarity);
      final ChargeEval eval = scoreCharge(z, mz, candidates, env, m1Bounds);
      if (eval != null && eval.raw() > 0) {
        // retain the raw candidates, envelope and bounds so the winner can be re-scored after
        // element auto-detection rebuilds its heavy upper bound
        scoredList.add(new Scored(eval, candidates, env, m1Bounds));
      }
    }

    if (scoredList.isEmpty()) {
      return null;
    }

    // decision: select the WINNER by the raw score (bounded quality x peak-count reward). The count
    // reward is what lets a genuine higher charge win: it explains more real isotope peaks than a lower
    // charge that only fits a subsample of the ladder. See TIE_WEIGHT for what that does and does not
    // guard against, and for the measured reason the reward is not restricted to predicted offsets.
    // ALTERNATES, by contrast, are flagged by an absolute margin on the bounded quality (below),
    // which is invariant to peak count and to how many hypotheses survived.
    scoredList.sort((a, b) -> {
      final int byRaw = Double.compare(b.eval().raw(), a.eval().raw());
      return byRaw != 0 ? byRaw : Double.compare(b.eval().quality(), a.eval().quality());
    });

    // optional two-pass element auto-detection: after the winning charge is chosen, infer the heavy
    // elements from the RAW spectrum around the pattern and rebuild the winner's heavy upper bound
    // from the detected per-element atom counts, then re-score that charge only.
    DetectedComposition detectedComposition = null;
    if (elementDetectionMode != ElementDetectionMode.USER_DEFINED) {
      final Scored winner = scoredList.getFirst();
      final int z = winner.eval().charge();
      // raw-spectrum window around the winning pattern so off-ladder S/Si M+2 peaks are recoverable
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

    // reference for flagging alternates and for the display share: the best bounded quality across all
    // surviving charge hypotheses (invariant to peak count and to how many hypotheses survived)
    double bestQuality = 0d;
    double qualitySum = 0d;
    for (final Scored s : scoredList) {
      bestQuality = Math.max(bestQuality, s.eval().quality());
      qualitySum += s.eval().quality();
    }

    final List<IsotopePattern> patterns = new ArrayList<>();
    final List<ChargeScore> scores = new ArrayList<>();
    // aligned by index with scores/patterns: the envelope anchor of each emitted pattern
    final List<PatternAnchor> anchors = new ArrayList<>();
    int bestCharge = 0;
    boolean first = true;
    for (final Scored scored : scoredList) {
      final ChargeEval e = scored.eval();
      // an alternate is flagged only if its bounded quality is within an absolute margin of the best
      // and clears a minimum quality bar; the winner is always emitted.
      final boolean flag =
          first || (e.quality() >= bestQuality - ALT_MARGIN && e.quality() >= MIN_ALT_QUALITY);
      if (flag) {
        // display-only quality share (bounded [0,1]); nothing in the selection reads it
        final double prob = qualitySum > 0 ? e.quality() / qualitySum : 0d;
        final String desc = String.format("IsotopeFinder z=%d p=%.2f %s", e.charge(), prob,
            modeLabel);
        // the stored pattern score is the bounded quality x intensity agreement (display/sorting only);
        // the intensity-upper-bound agreement never enters the charge selection above.
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

    // the searched signal as it was measured; every crop below is grown out of it, so the emitted
    // pattern always contains the signal the pattern was searched for.
    final DataPoint searched = closestCandidate(candidates, searchedMz);
    final double anchorMz = searched != null ? searched.getMZ() : mostIntense(candidates).getMZ();

    // observed base peak (most intense candidate). Used only as the observed grid origin (offset 0);
    // the predicted envelope is slid over the observed ladder rather than pinning the base to a
    // predicted offset, so the score does not depend on where in the pattern the search started.
    DataPoint base = mostIntense(candidates);
    CarbonLadder ladder = CarbonLadder.build(candidates, base.getMZ(), spacingDa, tol);
    int anchorOffset = (int) Math.round((anchorMz - base.getMZ()) / spacingDa);
    // decision: the intensity maximum is only a valid grid origin while it belongs to the SEARCHED
    // signal's own cluster. The candidate collection chains outward through isotope distances and can
    // reach an unrelated, more intense cluster tens of Da away; anchoring there cropped the searched
    // signal out of its own pattern (real case: searching m/z 667.311 emitted a z=2 pattern of
    // 652.306-654.315). When that happens the origin moves to the strongest peak of the searched
    // signal's cluster.
    final int[] cluster = ladder.clusterSpanAround(anchorOffset, anchorMz);
    // decision: only when the searched signal has a cluster of its own (2+ positions). A lone signal
    // - the isolated tail peak of a low-resolution envelope, say - carries no envelope information,
    // and making it the grid origin would decide the charge from a single peak. It is still kept in
    // the emitted pattern by the crop widening below.
    if ((cluster[0] > 0 || cluster[1] < 0) && cluster[1] - cluster[0] >= 1) {
      base = mostIntenseWithin(candidates, base.getMZ(), spacingDa, cluster);
      ladder = CarbonLadder.build(candidates, base.getMZ(), spacingDa, tol);
      anchorOffset = (int) Math.round((anchorMz - base.getMZ()) / spacingDa);
    }
    final double baseMz = base.getMZ();

    // require-13C ladder validation + gap-truncation. Anchored on the observed base (offset 0), walk
    // the 13C grid outward in BOTH directions and require a gap-free ladder: the pattern is truncated
    // at the first missing grid position even if signals exist beyond it (a strong discriminator
    // against fake high-charge ladders from noise/FT ringing). The monoisotopic is NOT required, so a
    // mid-envelope hump without a visible mono (e.g. a protein) is still accepted. See
    // CarbonLadder#requireC13Span for the walk itself and the every-second-position fallback.
    final List<DataPoint> cands;
    final int ladderStep;
    if (requireC13) {
      final int[] span = ladder.requireC13Span();
      if (span == null) {
        return null; // no gap-free 13C (or every-second) ladder through the seed
      }
      ladderStep = span[2];
      // decision: the VALIDATED span decides whether the charge is accepted at all, but the CROP is
      // widened to the searched signal when it falls outside. The nominal 13C grid drifts against a
      // polyhalogen comb (Br/Cl spacing is ~4.5 mDa short of the 13C distance per offset), so the
      // gap-free walk runs out of tolerance a dozen offsets away from the origin - and cropping there
      // would again drop the very signal the pattern belongs to. The signals in between are part of
      // the same cluster by construction (see the base re-anchoring above).
      final int cropLo = Math.min(span[0], anchorOffset);
      final int cropHi = Math.max(span[1], anchorOffset);
      final List<DataPoint> truncated = new ArrayList<>(candidates.size());
      for (final DataPoint dp : candidates) {
        final int k = (int) Math.round((dp.getMZ() - baseMz) / spacingDa);
        if (k >= cropLo && k <= cropHi) {
          truncated.add(dp);
        }
      }
      cands = truncated;
      // re-index the truncated set so every view below sees the validated span only
      ladder = CarbonLadder.build(cands, baseMz, spacingDa, tol);
    } else {
      cands = candidates;
      ladderStep = 1;
    }

    // isolated 13C ladder: per offset, the signal closest to the exact 13C position, so heavy
    // isotopes (37Cl/81Br/34S) and 15N at the same nominal offset do not contaminate the carbon
    // ratio. Offsets are relative to the observed base (may be negative).
    final TreeMap<Integer, Double> carbonLadder = ladder.onGridIntensities(1d);

    // all-signal per-offset map (summed) for coverage, self-consistency and the inclusive kept pattern
    final TreeMap<Integer, OffsetPeak> observed = ladder.collapsed();
    if (observed.isEmpty()) {
      return null;
    }

    // primary, position-agnostic carbon score: slide the carbon Poisson envelope over the isolated
    // 13C ladder. placement = predicted offset that aligns to observed offset 0 (the base).
    final CarbonFit carbonFit = slideCarbonFit(carbonLadder, env);
    final int placement = carbonFit.placement();

    // the monoisotopic -> M+1 (13C) ratio of the isolated carbon ladder, measured once at the
    // placement anchor. Both the optional hard gate below and the soft plausibility penalty further
    // down read it, so the anchor and the mono-dominance test exist in exactly one place.
    final CarbonRatio carbonRatio = CarbonRatio.measure(carbonLadder, placement);

    // require-13C loose shape gate: the mono->M+1 ratio must fall within the loose carbon M+1/M
    // bounds; an "M+1" far too small (FT ringing / not a real 13C peak) or far too large (a
    // co-eluting mono) rejects the hypothesis. Only applied when the anchor really is a dominant
    // monoisotopic and the every-13C (step 1) ladder was used - mid-envelope humps (proteins) and
    // the every-second ladder have no dominant monoisotopic to anchor the ratio. A shifted/merged
    // M+1 (absent from the strict ladder) is left to the soft penalty below.
    if (requireC13 && ladderStep == 1 && carbonRatio.failsRequireC13Gate(m1Bounds)) {
      return null;
    }

    // coverage: predicted-intensity-weighted fraction of the expected carbon envelope explained by
    // ANY observed signal (incl. heavy). decision: weight each expected offset by its predicted
    // relative intensity rather than counting offsets equally, so missing a small tail peak (e.g. a
    // predicted M+3/M+4 at a few percent) costs far less than missing the apex. An unweighted count
    // systematically under-scored low-m/z multiply-charged ions, whose broad high-carbon envelope
    // predicts many small tail offsets that fall below the noise floor of a real spectrum, while the
    // few resolved peaks already match the model well. predicted offset o aligns to observed offset
    // (o - placement).
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
    // because the averagine upper bound cannot reliably bound heavy-halogen envelopes. Bounded [0,1].
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

    // spacing consistency: how well a single m/z spacing explains the on-grid ladder positions.
    // decision: computed and exposed on the ChargeScore for diagnostics, but NOT folded into the
    // selection quality. A naive multiplicative fold regressed polyhalogen combs: a Cl2/Br2 comb at z=2
    // has ~1 Da m/z steps that nearly align to the z=1 13C grid, which wrongly boosted z=1.
    // The carbon M+1/M upper-bound check below is the harmonic discriminator that IS applied, but it
    // only bites when the borrowed "M+1" is implausibly large for the implied mass; a co-eluting
    // compound of similar size and intensity produces a ratio the (mass-scaled) carbon maximum still
    // allows. Folding this term in only between charges in a divisor/multiple relation would restore
    // the guard without the polyhalogen regression - not done here, see TIE_WEIGHT.
    final double spacingConsistency = spacingConsistency(ladder, baseMz);

    // bounded [0,1] quality (carbon fit x coverage), gated by self-consistency for higher charges so a
    // higher charge whose intermediate peaks are absent cannot win. This, times the peak-count reward
    // below, drives the charge selection.
    double quality = carbonFit.score() * coverage;
    if (z > 1) {
      quality *= selfConsistency;
    }
    // decision: a charge decided without a genuine 13C ladder (carbon fit fell back to the neutral
    // 1.0) is down-weighted so it cannot out-compete a charge with a real carbon fit on a tie;
    // detection still succeeds (raw stays > 0), the charge just ranks lower.
    if (!carbonFit.assessed()) {
      quality *= NEUTRAL_FALLBACK_WEIGHT;
    }
    // carbon M+1/M plausibility penalty: a single two-sided factor in (0,1] on the SAME anchored
    // ratio the optional gate above used. Skipped when the mono is absent (protein humps with an
    // invisible monoisotopic) or the carbon ladder was not assessable.
    if (carbonFit.assessed()) {
      quality *= carbonRatio.plausibility(m1Bounds);
    }
    double raw = quality * (1d + TIE_WEIGHT * observedCount);
    // hard misdetection guard: a charge is only accepted when enough signals a genuine 13C distance
    // apart are present. c13Signals counts the isolated 13C-ladder peaks - signals sitting on the exact
    // charge-adjusted 13C grid (1.00336/z Da) - collected in BOTH directions from the base and
    // including it. Heavy isotopes (Cl/Br/S off the 13C grid) and off-grid noise do NOT count, so a
    // high charge must be backed by a real 13C ladder rather than a heavy-isotope comb or a few
    // grid-adjacent noise peaks. Because these are distinct positions on the 13C grid, requiring N of
    // them also requires the pattern to span N-1 charge-adjusted 13C distances (the two coincide).
    final int c13Signals = carbonLadder.size();
    final int minSignals = minSignalsForCharge(z);
    // low charges (floor 2) may still be carried by heavy-isotope spacing alone (e.g. a C,Br molecule
    // with a weak/absent 13C M+1 but a strong 81Br M+2), so there any two isotope signals qualify; the
    // escalated floor for higher charges (z >= 4) must be met by genuine 13C-ladder signals.
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
   * Minimum number of distinct isotope signals (occupied offsets on the charge-adjusted 13C grid)
   * required to accept a charge hypothesis. The floor rises in fixed steps with the charge so a
   * high charge is only reported with genuine multi-isotope evidence, guarding against noise peaks
   * that happen to fall on the fine (1.00336/z Da) grid being read as a high charge state.
   * Deliberately not scaled beyond z=10 (a hard misdetection cutoff, not a graduated score term).
   * <p>
   * Levels: z&le;3 &rarr; 2 (mono + M+1); z 4&ndash;5 &rarr; 3; z 6&ndash;9 &rarr; 4; z&ge;10
   * &rarr; 5. The higher floor only starts above charge 3.
   *
   * @param z the charge hypothesis (&ge; 1).
   * @return the minimum number of distinct 13C-grid offsets required.
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

  /**
   * The candidate closest to the searched m/z, i.e. the searched signal as it was measured.
   *
   * @return the matching candidate, or {@code null} when none falls within the m/z tolerance (the
   * candidate collection seeds on the nearest data point in the spectrum, which may be far away in a
   * sparse spectrum).
   */
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

  /**
   * The most intense candidate whose 13C-grid offset falls inside {@code span}.
   *
   * @param baseMz the grid origin the span's offsets are expressed on.
   * @param span   inclusive {@code [minOffset, maxOffset, step]}.
   * @return the strongest signal of that span (the candidates always occupy at least one offset of
   * it, since the span was walked on the same grid).
   */
  private static @NotNull DataPoint mostIntenseWithin(@NotNull final List<DataPoint> candidates,
      final double baseMz, final double spacingDa, final int @NotNull [] span) {
    DataPoint best = null;
    for (final DataPoint dp : candidates) {
      final int k = (int) Math.round((dp.getMZ() - baseMz) / spacingDa);
      if (k >= span[0] && k <= span[1] && (best == null || dp.getIntensity() > best.getIntensity())) {
        best = dp;
      }
    }
    return best != null ? best : mostIntense(candidates);
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
      // not enough isolated 13C peaks to assess the carbon envelope -> neutral fit, flagged as
      // not assessed so the caller can down-weight a charge decided without a real carbon ladder
      return new CarbonFit(1d, env.baseOffset(), false);
    }
    // Both cosine norms are INVARIANT across placements, so they are hoisted out of the loop:
    // the summation range always contains every ladder key (so the observed norm is just the ladder's
    // own norm) and always covers the full predicted envelope [0, maxOffset] (so the predicted norm
    // is the envelope's own norm). Only the dot product varies, and it only needs the ladder's
    // (sparse) keys - everywhere else the observed intensity is 0 and contributes nothing. This turns
    // O(maxOffset x range) into O(maxOffset x ladderSize) with bit-identical results, which matters
    // for high-charge proteins where the range is widest.
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

    // placement p in 0..maxOffset: observed offset 0 (base) aligns to predicted offset p. Since the
    // denominator is constant and positive, the best cosine is the best dot product.
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
   * Spacing-consistency cue on the isolated 13C ladder: fit a single anchored spacing to the
   * on-grid peak positions and measure the residual m/z drift. A clean single-spacing ladder yields
   * ~1.0; a neighbouring (wrong) charge that only partially aligns accumulates residual across
   * offsets and the term collapses. Uses only m/z positions (not intensities), so it is independent
   * of the carbon fit and of the upper-bound intensity penalty.
   *
   * @param ladder the candidates indexed on the 13C grid.
   * @param baseMz the observed base (offset 0) m/z.
   * @return bounded [0,1] spacing consistency (1 = a single clean spacing explains the ladder).
   */
  private double spacingConsistency(@NotNull final CarbonLadder ladder, final double baseMz) {
    // tight on-grid window: heavy isotopes (~4-5 mDa off the 13C grid) are excluded, so the spacing
    // regression sees only the pure 13C ladder plus any near-but-off interferent peaks
    final TreeMap<Integer, Double> mzByOffset = ladder.onGridMz(SPACING_GRID_FACTOR);
    if (mzByOffset.size() < 2) {
      return 1d; // too few on-grid peaks to assess a spacing -> neutral
    }
    // anchored regression of dmz = mz - baseMz on the integer offset k, through the base (no intercept)
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
    // empirical detection floor of THIS spectrum: the weakest signal that made it into the candidate
    // set, relative to the base. A predicted peak below it would not be visible here even if it
    // existed, so its absence carries no information about the charge.
    // decision: without this, an intensity cutoff (every real spectrum has a noise floor) removed the
    // weak intermediate peaks of a genuine higher charge and this term collapsed, systematically
    // DOWN-calling the charge - every charge error on the benchmark's cutoff axis was 2->1 or 3->1.
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
    // examine predicted offsets relative to the monoisotopic (offset 0). Offsets not divisible by z
    // are the intermediate (e.g. half-spacing) peaks that a lower-charge ladder would not have.
    // Predicted offset o aligns to observed offset (o - placement).
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
    // use the presence fraction directly (no rounding up), so a single spurious half-spacing peak
    // does not fully satisfy the requirement for a higher charge
    return (double) reqPresent / reqTotal;
  }

  /**
   * The offsets the pattern is SCORED on: the supported, bridgeable run of offsets around the base
   * peak (offset 0). See {@link #emittedOffsets} for the reported ones.
   *
   * @param observed the collapsed per-offset signals, relative to the base peak (offset 0).
   */
  private Set<Integer> computeKeptOffsets(@NotNull final TreeMap<Integer, OffsetPeak> observed,
      @NotNull final IsotopeEnvelope env, final int placement, final double baseIntensity) {
    final Set<Integer> kept = new HashSet<>();
    if (observed.isEmpty()) {
      return kept;
    }
    // observed offsets are relative to the base (offset 0). Predicted offset = observed + placement.
    kept.add(0);
    final int maxObs = observed.lastKey();
    final int minObs = observed.firstKey();
    final int horizon = gapHorizon(env);

    // extend upward, bridging gaps only when the envelope still supports a peak ahead and the
    // bridged (gap-crossing) signal is significant, so insignificant noise does not widen the pattern
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
    // extend downward (toward the monoisotopic / lower m/z), symmetric to the upward bridging
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
   * The offsets the pattern is EMITTED at: the scored offsets plus, when the shape-aware walk
   * stopped short of the searched signal (a hole, or a bridge the significance test rejected), the
   * observed offsets between them. The searched signal is always part of its own pattern.
   * <p>
   * decision: this widening is deliberately kept out of the scoring set. Coverage, the peak-count
   * reward and the intensity agreement stay exactly what the shape-aware termination produced, so
   * the charge a spectrum is assigned does not depend on which of its isotope peaks was searched -
   * only the reported signals do, which is the point.
   *
   * @param kept         the scored offsets.
   * @param observed     the collapsed per-offset signals.
   * @param anchorOffset the searched signal's offset.
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
   * Gap look-ahead (in offsets) for the envelope-shape-aware termination, derived from the width of
   * the predicted envelope rather than fixed.
   * <p>
   * decision: a fixed look-ahead is wrong at both ends of the charge range. A small molecule
   * predicts a handful of offsets, so 4 already reaches past its whole envelope; a high-charge
   * protein predicts dozens, and a run of undetected offsets inside such a wide envelope is longer
   * than 4 in absolute terms while being a small fraction of the pattern. The look-ahead is
   * therefore a fraction of the predicted, above-cutoff envelope width, floored at the previous
   * fixed value so nothing narrows. Measured neutral on the corpus (every metric unchanged); it is a
   * structural guard for envelopes wider than the corpus contains, not a scoring change.
   *
   * @param env the predicted envelope of the charge hypothesis.
   * @return the maximum number of offsets a single gap may span.
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
   * @return whether the observed peak reaches {@link #MIN_BRIDGED_REL_INTENSITY} relative to the
   * base peak. Used to reject insignificant signals that would only be reached by bridging a gap.
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

  /**
   * A scored charge hypothesis together with the inputs needed to re-score it after element
   * auto-detection rebuilds the heavy upper bound.
   *
   * @param eval       the current scoring result for this charge.
   * @param candidates the raw candidate signals collected for this charge.
   * @param env        the predicted envelope used to produce {@code eval}.
   * @param m1Bounds   the carbon M+1/M ratio bounds.
   */
  private record Scored(@NotNull ChargeEval eval, @NotNull List<DataPoint> candidates,
                        @NotNull IsotopeEnvelope env, @NotNull double[] m1Bounds) {

  }

  /**
   * Result of sliding the carbon envelope over the observed 13C ladder.
   *
   * @param score     bounded cosine similarity in [0,1] (1.0 when too few 13C peaks to assess).
   * @param placement the predicted offset aligned to observed offset 0 (the base peak).
   * @param assessed  whether a genuine 13C ladder was available to assess the fit (false when the
   *                  ladder had too few peaks and the neutral 1.0 fallback was used).
   */
  private record CarbonFit(double score, int placement, boolean assessed) {

  }
}
