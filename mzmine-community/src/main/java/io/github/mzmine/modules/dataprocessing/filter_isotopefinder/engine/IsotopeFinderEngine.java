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
  private static final double ENGINE_CUTOFF = 0.02;
  // an alternate charge is flagged in addition to the winner if its bounded quality is within this
  // absolute margin of the best bounded quality. An absolute margin is invariant to peak count and to
  // how many charge hypotheses survived (a maxCharge-dependent probability denominator was the old
  // bug that made the true charge lose its alternate slot as maxCharge grew).
  private static final double ALT_MARGIN = 0.15;
  // an alternate charge must also reach at least this bounded quality to be flagged at all
  private static final double MIN_ALT_QUALITY = 0.1;
  // look-ahead window (in offsets) used to bridge gaps during termination
  private static final int HORIZON = 4;
  // minimum isolated 13C peaks needed to assess the carbon envelope; below this the carbon fit is
  // neutral (1.0) and heavy-element coverage carries the detection
  private static final int MIN_LADDER_PEAKS = 2;
  // reward for the number of isotope offsets a charge explains: a genuine higher charge explains more
  // real isotope peaks (its own full envelope), so this lets it win over a lower charge that fits only a
  // subsample of the ladder and leaves the intermediate peaks unexplained.
  // KNOWN LIMITATION: this reward is currently applied WITHOUT a harmonic guard. A doubled charge that
  // interleaves a co-eluting interferent's peaks explains twice as many offsets and can therefore win on
  // raw score. The spacing-consistency term below would discriminate it, but it is deliberately not
  // folded into the quality (see spacingConsistency) because a naive multiplicative fold regressed
  // polyhalogen combs. Measured effect: chargeTop1 ~0.55 on the interference/combined benchmark axes
  // versus ~0.99 elsewhere, while chargeRecallAlt stays ~0.95 (the true charge is found, just not
  // ranked first).
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
  // relative slack applied to the estimated M+1/M bounds in the optional "require 13C" gate
  private static final double C13_RATIO_SLACK = 0.3;
  // lower bound of the "require 13C" gate as a fraction of the carbon MINIMUM (1/20-C-per-Da) M+1/M
  // prediction. Deliberately well below 1: heteroatom-rich (Cl/Br/S/metal) molecules legitimately have
  // far fewer carbons per Dalton than the averagine minimum, so their real 13C M+1/M falls below that
  // minimum; a too-tight lower bound (the old m1Bounds[0]*(1-slack)) wrongly rejected such valid singly
  // charged patterns. This effectively allows down to ~1/40 C per Da while still rejecting an "M+1" too
  // small to be a real 13C peak. Only used by the optional gate (off by default), not the scoring.
  private static final double REQUIRE_C13_LOWER_FACTOR = 0.5;
  // FT-ringing guard: fraction of the carbon MINIMUM M+1/M prediction below which the observed 13C M+1
  // is treated as implausibly small ("not a real 13C peak"). Deliberately far below 1 (a quarter of the
  // already-conservative 1/20-C-per-Da minimum) so genuine low-carbon / heteroatom-rich molecules -
  // which stay within ~2x of their prediction - are never penalised, while low-intensity FT ringing
  // mistaken for a high-charge 13C ladder (M+1 orders of magnitude too small for the implied mass) is.
  private static final double C13_RATIO_LOWER_FACTOR = 0.25;
  // the base peak counts as the monoisotopic (so its M+1/M is bounded by the mono carbon prediction)
  // only when no 13C-ladder peak below it reaches this fraction of the base; mid-envelope apices
  // (proteins, halogen combs) have significant peaks below and are exempt from the lower-bound check.
  private static final double MONO_DOMINANCE_FRACTION = 0.1;
  // hardest floor the FT-ringing penalty can drive the quality to (keeps raw finite / comparable)
  private static final double C13_RATIO_LOWER_PENALTY_FLOOR = 1e-3;
  // a signal reached only by bridging a gap (not directly adjacent to the kept run) must be at least
  // this fraction of the base peak to be included, so insignificant noise on the tails does not widen
  // the pattern. Contiguous/adjacent signals are always kept to preserve complete isotope envelopes.
  private static final double MIN_BRIDGED_REL_INTENSITY = 0.005;
  // require-13C gap-truncation: the m/z tolerance is widened by this factor when testing whether a
  // 13C-grid position is occupied, so a heavy isotope (37Cl/81Br) merged with the expected 13C signal -
  // which pulls the observed centroid a few mDa off the exact grid - still counts as present and does
  // not open a false hole that would truncate the pattern early.
  private static final double REQUIRE_C13_GAP_TOL_FACTOR = 3d;
  // heavy-isotope per-signal attribution window (neutral Da): a signal's mass-defect deviation from
  // the 13C grid must be within this of a candidate element's M+2/M+1 defect to be labelled with it.
  private static final double HEAVY_ATTR_WINDOW = 0.006;

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
  // developer-only: retain rich per-charge scoring diagnostics on the DetectionResult. Off in the
  // normal processing run (the diagnostics are recomputed on demand by the compound dashboard).
  private final boolean keepDiagnostics;

  public IsotopeFinderEngine(@NotNull final List<Element> elements, final int maxCharge,
      @NotNull final MZTolerance tol, @NotNull final EnvelopeModel model,
      @NotNull final String modeLabel, final boolean requireC13) {
    this(elements, maxCharge, tol, model, modeLabel, requireC13, ElementDetectionMode.USER_DEFINED,
        List.of());
  }

  public IsotopeFinderEngine(@NotNull final List<Element> elements, final int maxCharge,
      @NotNull final MZTolerance tol, @NotNull final EnvelopeModel model,
      @NotNull final String modeLabel, final boolean requireC13,
      @NotNull final ElementDetectionMode elementDetectionMode,
      @NotNull final List<String> autoCandidates) {
    this(elements, maxCharge, tol, model, modeLabel, requireC13, elementDetectionMode,
        autoCandidates, false);
  }

  public IsotopeFinderEngine(@NotNull final List<Element> elements, final int maxCharge,
      @NotNull final MZTolerance tol, @NotNull final EnvelopeModel model,
      @NotNull final String modeLabel, final boolean requireC13,
      @NotNull final ElementDetectionMode elementDetectionMode,
      @NotNull final List<String> autoCandidates, final boolean keepDiagnostics) {
    this.maxCharge = maxCharge;
    this.tol = tol;
    this.model = model;
    this.modeLabel = modeLabel;
    this.requireC13 = requireC13;
    this.elementDetectionMode = elementDetectionMode;
    this.autoCandidates = autoCandidates;
    this.keepDiagnostics = keepDiagnostics;
    // user heavies are only added on top of the detected ones in the combined mode
    this.includeUserHeavies = elementDetectionMode == ElementDetectionMode.USER_PLUS_AUTO;
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
      // carbon M+1/M bounds drive both the optional require-13C gate and the carbon-ratio plausibility
      // penalty in scoreCharge, so compute them whenever the model can (null in formula-prediction mode).
      final double[] m1Bounds = model.expectedM1RatioBounds(mz, z, polarity);
      final ChargeEval eval = scoreCharge(z, candidates, env, m1Bounds);
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
    // charge that only fits a subsample of the ladder. Note that nothing here discriminates a harmonic
    // doubling that borrows a co-eluting interferent's peaks - see TIE_WEIGHT for the known limitation.
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
    if (elementDetectionMode != ElementDetectionMode.USER_DEFINED && !scoredList.isEmpty()) {
      final Scored winner = scoredList.getFirst();
      final int z = winner.eval().charge();
      // raw-spectrum window around the winning pattern so off-ladder S/Si M+2 peaks are recoverable
      double lo = mz;
      double hi = mz;
      for (final DataPoint dp : winner.eval().keptCandidates()) {
        lo = Math.min(lo, dp.getMZ());
        hi = Math.max(hi, dp.getMZ());
      }
      final double pad = 2.5d / z; // ~one extra M+2 spacing at this charge
      final List<DataPoint> rawWindow = collectRawWindow(spectrum, lo - pad, hi + pad);
      final DetectedComposition comp = ElementAutoDetector.detect(rawWindow, z, mz * z, tol,
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
        final ChargeEval reEval = scoreCharge(z, winner.candidates(), env2, winner.m1Bounds());
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
    // developer-only: aligned by index with scores/patterns. Winner (index 0) carries the detected
    // composition; alternates carry null (element auto-detection re-scores the winner only).
    final List<ChargeDiagnostics> diagnostics = keepDiagnostics ? new ArrayList<>() : null;
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
        if (diagnostics != null) {
          diagnostics.add(buildDiagnostics(e, scored.env(), scored.m1Bounds(), score,
              first ? detectedComposition : null));
        }
        if (first) {
          bestCharge = e.charge();
        }
      }
      first = false;
    }
    return new DetectionResult(bestCharge, scores, patterns, detectedComposition, diagnostics);
  }

  /**
   * Build the rich per-charge diagnostics for one emitted charge (developer-only, only called when
   * {@code keepDiagnostics}). Attributes each kept signal to the 13C grid or a candidate heavy
   * element from its mass defect, and snapshots the predicted envelope and M+1 bounds used to
   * score.
   */
  private @NotNull ChargeDiagnostics buildDiagnostics(@NotNull final ChargeEval e,
      @NotNull final IsotopeEnvelope env, final double @Nullable [] m1Bounds,
      @NotNull final ChargeScore score, @Nullable final DetectedComposition comp) {
    final double baseMz = e.baseMz();
    final int placement = e.placement();
    final double spacingDa = env.spacingDa();
    double baseIntensity = 0d;
    for (final DataPoint dp : e.keptCandidates()) {
      baseIntensity = Math.max(baseIntensity, dp.getIntensity());
    }
    // candidate heavy elements for per-signal attribution: the detected composition when available,
    // otherwise the default candidate set so alternates still get a best-effort label.
    final List<String> heavyCandidates =
        comp != null && !comp.elements().isEmpty() ? List.copyOf(comp.elements())
            : ElementAutoDetector.DEFAULT_CANDIDATES;
    final List<DataPoint> sorted = new ArrayList<>(List.of(e.keptCandidates()));
    sorted.sort((a, b) -> Double.compare(a.getMZ(), b.getMZ()));
    final List<IsotopeSignalAttribution> signals = new ArrayList<>(sorted.size());
    for (final DataPoint dp : sorted) {
      final int k = (int) Math.round((dp.getMZ() - baseMz) / spacingDa);
      final int offsetFromMono = k + placement;
      final double exactGrid = baseMz + k * spacingDa;
      final boolean onGrid = tol.checkWithinTolerance(exactGrid, dp.getMZ());
      final double relInt = baseIntensity > 0d ? dp.getIntensity() / baseIntensity : 0d;
      final IsotopeAssignment assignment;
      final String label;
      if (onGrid) {
        if (offsetFromMono == 0) {
          assignment = IsotopeAssignment.MONOISOTOPIC;
          label = "M";
        } else {
          assignment = IsotopeAssignment.CARBON_13;
          label = (offsetFromMono > 0 ? "+" : "") + offsetFromMono + " ¹³C";
        }
      } else {
        // neutral-mass deviation from the exact 13C grid at this nominal offset (see attributeHeavyElement)
        final double deviation = (dp.getMZ() - exactGrid) * env.charge();
        final String el = ElementAutoDetector.attributeHeavyElement(deviation, heavyCandidates,
            HEAVY_ATTR_WINDOW);
        assignment = el != null ? IsotopeAssignment.HEAVY_ISOTOPE : IsotopeAssignment.UNEXPLAINED;
        label = el != null ? el : "heavy";
      }
      signals.add(
          new IsotopeSignalAttribution(dp.getMZ(), dp.getIntensity(), offsetFromMono, assignment,
              label, relInt));
    }
    return new ChargeDiagnostics(env.charge(), baseMz, baseIntensity, placement, spacingDa,
        env.expected().clone(), env.upperBound().clone(),
        m1Bounds == null ? null : m1Bounds.clone(), signals, score, comp);
  }

  /**
   * Collect the raw spectrum data points whose m/z falls within {@code [loMz, hiMz]}, so element
   * auto-detection can see off-ladder heavy M+2 peaks that were not kept in the pattern.
   */
  private @NotNull List<DataPoint> collectRawWindow(@NotNull final MassSpectrum spectrum,
      final double loMz, final double hiMz) {
    final List<DataPoint> out = new ArrayList<>();
    final int n = spectrum.getNumberOfDataPoints();
    for (int i = 0; i < n; i++) {
      final double m = spectrum.getMzValue(i);
      if (m < loMz) {
        continue;
      }
      if (m > hiMz) {
        break; // data points are sorted ascending by m/z
      }
      out.add(new SimpleDataPoint(m, spectrum.getIntensityValue(i)));
    }
    return out;
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

    // index every candidate on the 13C grid once; the isolated ladder, the collapsed per-offset map,
    // the spacing regression and the require-13C gap probe are all views on this single walk.
    CarbonLadder ladder = CarbonLadder.build(candidates, baseMz, spacingDa, tol);

    // require-13C ladder validation + gap-truncation. Anchored on the observed base (offset 0), walk
    // the 13C grid outward in BOTH directions and require a gap-free ladder: the pattern is truncated
    // at the first missing grid position even if signals exist beyond it (a strong discriminator
    // against fake high-charge ladders from noise/FT ringing). The monoisotopic is NOT required, so a
    // mid-envelope hump without a visible mono (e.g. a protein) is still accepted. Presence uses a
    // widened tolerance (REQUIRE_C13_GAP_TOL_FACTOR) so a heavy isotope merged with the expected 13C
    // signal does not open a false hole. If the every-13C (step 1) ladder does not reach at least two
    // signals, fall back to an every-second (step 2) ladder: molecules dominated by an intense +2
    // heavy comb (Cl/Br/Cu, e.g. pigment green 7 C32HCl15CuN8) show a clean ladder only on every
    // second 13C position while the intervening pure-13C peaks are swallowed by the noise floor.
    final List<DataPoint> cands;
    final int ladderStep;
    if (requireC13) {
      final int[] span = requireC13LadderSpan(ladder);
      if (span == null) {
        return null; // no gap-free 13C (or every-second) ladder through the seed
      }
      ladderStep = span[2];
      final List<DataPoint> truncated = new ArrayList<>(candidates.size());
      for (final DataPoint dp : candidates) {
        final int k = (int) Math.round((dp.getMZ() - baseMz) / spacingDa);
        if (k >= span[0] && k <= span[1]) {
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
    final CarbonRatio carbonRatio = carbonRatioAt(carbonLadder, placement);

    // require-13C loose shape gate: the mono->M+1 ratio must fall within the loose carbon M+1/M
    // bounds; an "M+1" far too small (FT ringing / not a real 13C peak) or far too large (a
    // co-eluting mono) rejects the hypothesis. Only applied when the anchor really is a dominant
    // monoisotopic and the every-13C (step 1) ladder was used - mid-envelope humps (proteins) and
    // the every-second ladder have no dominant monoisotopic to anchor the ratio. A shifted/merged
    // M+1 (absent from the strict ladder) is left to the soft penalty below.
    if (requireC13 && ladderStep == 1 && m1Bounds != null && failsRequireC13Ratio(carbonRatio,
        m1Bounds)) {
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
    final List<DataPoint> kept = new ArrayList<>();
    for (final DataPoint dp : cands) {
      final int offset = (int) Math.round((dp.getMZ() - baseMz) / spacingDa);
      if (keptOffsets.contains(offset)) {
        kept.add(dp);
      }
    }
    if (kept.isEmpty()) {
      kept.addAll(cands);
    }

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
    if (m1Bounds != null && carbonFit.assessed()) {
      quality *= carbonRatioPlausibility(carbonRatio, m1Bounds);
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
   * Measure the monoisotopic &rarr; M+1 (13C) intensity ratio of the isolated carbon ladder at the
   * placement anchor, i.e. the observed offset that the sliding envelope fit aligned to predicted
   * offset 0.
   * <p>
   * decision: this is the single anchor convention for every M+1/M test. Previously the harmonic
   * upper-bound penalty used the placement anchor while the require-13C gate and the FT-ringing
   * lower-bound penalty used the observed base, so for any mid-envelope apex ({@code placement != 0}
   * - every polyhalogen and every protein) the "symmetric" tests silently compared different peak
   * pairs. The mono-dominance guard is applied here once as well.
   *
   * @param carbonLadder the isolated exact-13C ladder (offset &rarr; intensity).
   * @param placement    the predicted offset aligned to observed offset 0.
   * @return the measured ratio, or {@link CarbonRatio#ABSENT} when the anchor peak is missing.
   */
  private static @NotNull CarbonRatio carbonRatioAt(
      @NotNull final TreeMap<Integer, Double> carbonLadder, final int placement) {
    final int monoOffset = -placement;
    final Double monoI = carbonLadder.get(monoOffset);
    if (monoI == null || monoI <= 0d) {
      return CarbonRatio.ABSENT;
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
   * @param ratio    the anchored ratio from {@link #carbonRatioAt}.
   * @param m1Bounds {@code {min, max}} carbon M+1/M prediction for the implied neutral mass.
   * @return the factor to multiply into the charge quality.
   */
  private static double carbonRatioPlausibility(@NotNull final CarbonRatio ratio,
      final double @NotNull [] m1Bounds) {
    if (!ratio.present()) {
      return 1d;
    }
    final double hi = m1Bounds[1] * (1d + C13_RATIO_SLACK);
    if (hi > 0d && ratio.value() > hi) {
      return hi / ratio.value();
    }
    // the lower bound is far more aggressive, so it only fires when the anchor demonstrably is the
    // dominant monoisotopic AND the observed base; mid-envelope apices (proteins, halogen combs) are
    // exempt because their real M+1/M is legitimately below the averagine carbon minimum.
    final double lo = m1Bounds[0] * C13_RATIO_LOWER_FACTOR;
    if (ratio.supportsLowerBound() && lo > 0d && ratio.value() < lo) {
      return Math.max(C13_RATIO_LOWER_PENALTY_FLOOR, ratio.value() / lo);
    }
    return 1d;
  }

  /**
   * The optional require-13C hard gate on the same anchored ratio.
   * <p>
   * The lower bound uses {@link #REQUIRE_C13_LOWER_FACTOR} rather than the (much looser)
   * {@link #C13_RATIO_LOWER_FACTOR} of the soft penalty: this gate is opt-in and is meant to reject
   * outright, but it must still not reject heteroatom-rich, carbon-poor molecules whose real 13C
   * M+1/M is legitimately below the averagine carbon minimum. The upper bound is the same as the
   * soft penalty's - an "M+1" too large to be 13C (a co-eluting mono).
   *
   * @return whether the hypothesis must be rejected.
   */
  private static boolean failsRequireC13Ratio(@NotNull final CarbonRatio ratio,
      final double @NotNull [] m1Bounds) {
    // a missing M+1 is left to the soft penalty; the gate only judges a ratio it could measure
    if (!ratio.supportsLowerBound() || ratio.value() <= 0d) {
      return false;
    }
    return ratio.value() < m1Bounds[0] * REQUIRE_C13_LOWER_FACTOR
        || ratio.value() > m1Bounds[1] * (1d + C13_RATIO_SLACK);
  }

  /**
   * Select the gap-free 13C ladder through the observed base for the require-13C gate. Prefers the
   * every-13C (step 1) ladder; when that reaches fewer than two signals it falls back to an
   * every-second (step 2) ladder for molecules whose pattern shows only on every second 13C
   * position (an intense +2 heavy comb: Cl/Br/Cu). The step-2 ladder must reach at least three
   * signals (base plus two more on the step-2 grid) so a lone monoisotopic + single heavy M+2 does
   * not qualify as a 13C pattern.
   *
   * @param ladder the candidates indexed on the 13C grid.
   * @return inclusive {@code [minOffset, maxOffset, step]}, or {@code null} if no ladder qualifies.
   */
  private static int @Nullable [] requireC13LadderSpan(@NotNull final CarbonLadder ladder) {
    final int[] s1 = gapFreeSpan(ladder, 1);
    if (s1[1] - s1[0] >= 1) { // >= 2 signals on the every-13C grid
      return new int[]{s1[0], s1[1], 1};
    }
    final int[] s2 = gapFreeSpan(ladder, 2);
    if (s2[1] - s2[0] >= 4) { // >= 3 signals on the every-second grid
      return new int[]{s2[0], s2[1], 2};
    }
    return null;
  }

  /**
   * Contiguous, gap-free span of grid offsets around the observed base (offset 0), stepping by
   * {@code step} offsets. Walks outward in both directions and stops at the first stepped position
   * with no signal, so a hole where a peak is expected truncates the span even if signals exist
   * further out. The presence test uses a widened tolerance ({@link #REQUIRE_C13_GAP_TOL_FACTOR})
   * so a heavy isotope merged with the expected 13C peak (shifting it a few mDa off grid) still
   * counts.
   *
   * @param ladder the candidates indexed on the 13C grid.
   * @param step   the offset step (1 = every 13C, 2 = every second 13C).
   * @return inclusive {@code [minOffset, maxOffset]} span containing offset 0.
   */
  private static int @NotNull [] gapFreeSpan(@NotNull final CarbonLadder ladder, final int step) {
    int hi = 0;
    while (ladder.hasSignalNearGrid(hi + step, REQUIRE_C13_GAP_TOL_FACTOR)) {
      hi += step;
    }
    int lo = 0;
    while (ladder.hasSignalNearGrid(lo - step, REQUIRE_C13_GAP_TOL_FACTOR)) {
      lo -= step;
    }
    return new int[]{lo, hi};
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

    // extend upward, bridging gaps only when the envelope still supports a peak ahead and the
    // bridged (gap-crossing) signal is significant, so insignificant noise does not widen the pattern
    int current = 0;
    boolean advanced = true;
    while (advanced) {
      advanced = false;
      for (int k = current + 1; k <= current + HORIZON && k <= maxObs; k++) {
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
      for (int k = current - 1; k >= current - HORIZON && k >= minObs; k--) {
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
   * @param m1Bounds   the carbon M+1/M ratio bounds (may be null in formula-prediction mode).
   */
  private record Scored(@NotNull ChargeEval eval, @NotNull List<DataPoint> candidates,
                        @NotNull IsotopeEnvelope env, @Nullable double[] m1Bounds) {

  }

  /**
   * The monoisotopic &rarr; M+1 (13C) intensity ratio of the isolated carbon ladder at the placement
   * anchor.
   *
   * @param present      whether the anchor (monoisotopic) peak exists with a positive intensity; if
   *                     not, no M+1/M test can be applied.
   * @param value        {@code I(M+1) / I(M)}; {@code 0} when the M+1 is absent, which is itself
   *                     meaningful (the FT-ringing signature).
   * @param anchorIsMono whether the anchor dominates the ladder peaks below it, i.e. it really is
   *                     the monoisotopic rather than a mid-envelope apex.
   * @param anchorIsBase whether the anchor is the observed base peak ({@code placement == 0}). The
   *                     aggressive lower-bound tests additionally require this: a carbon-poor
   *                     halogenated molecule whose apex sits mid-envelope legitimately has an M+1/M
   *                     far below the averagine carbon minimum, and penalising it costs real
   *                     polyhalogen charge calls.
   */
  private record CarbonRatio(boolean present, double value, boolean anchorIsMono,
                             boolean anchorIsBase) {

    private static final CarbonRatio ABSENT = new CarbonRatio(false, 0d, false, false);

    /**
     * @return whether the aggressive lower-bound tests (the FT-ringing penalty and the optional
     * require-13C gate) may be applied to this ratio.
     */
    private boolean supportsLowerBound() {
      return present && anchorIsMono && anchorIsBase;
    }
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
