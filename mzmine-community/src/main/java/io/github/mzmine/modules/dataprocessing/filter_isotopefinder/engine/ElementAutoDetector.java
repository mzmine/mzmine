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
import io.github.mzmine.util.Isotope;
import io.github.mzmine.util.IsotopesUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Standalone detector that infers which popular heavy elements (default {@code Cl, Br, S, Si}) are
 * <i>possible</i> in an isotope pattern, purely from the m/z spacings between an element's major
 * isotopes (M+2 for Cl/Br/S/Si, plus the 29Si M+1 fingerprint) and the relative intensities of the
 * spaced signals. All isotope masses/abundances are pulled from CDK via
 * {@link IsotopesUtils#getIsotopeRecord(String)} - nothing is hardcoded, so the candidate list is
 * freely extensible.
 * <p>
 * Design (robust to real high-resolution data):
 * <ul>
 *   <li><b>Position-agnostic / mono-independent:</b> detection works off <i>pairs</i> of signals
 *   spaced by the element isotope delta, gathered in BOTH directions across the whole envelope. The
 *   seed signal may be any member of the pattern and the monoisotopic peak may be missing (below the
 *   detection threshold, e.g. large poly-halogen or protein humps) - the comb of spaced pairs is
 *   still there.</li>
 *   <li><b>Charge-aware:</b> the spacing is {@code isotopeDelta / z}; all match windows are scaled by
 *   the m/z tolerance at the peak's m/z times the charge. {@link #detectAcrossCharges} tries several
 *   charges when the charge is unknown.</li>
 *   <li><b>Tolerance-robust:</b> peak matching uses the supplied {@link MZTolerance}; because the four
 *   candidate M+2 defects sit within ~1-2 mDa (below a typical 5 mDa tolerance) they cannot be
 *   separated from a single shifted peak. The detector therefore uses the <i>median</i> spacing over
 *   many pairs (whose error shrinks with the pair count) to recover sub-tolerance defect precision,
 *   and leans on intensity (per-atom M+2 abundance) plus the 29Si M+1 to disambiguate.</li>
 * </ul>
 * The atom count is deliberately NOT resolved (it is unknown and may be large); {@code counts} is a
 * best-effort hint only. The element metric reads only {@link DetectedComposition#elements()}.
 */
public final class ElementAutoDetector {

  /**
   * Default candidate heavy elements the detector tries to infer.
   */
  @NotNull
  public static final List<String> DEFAULT_CANDIDATES = List.of("Cl", "Br", "S", "Si");

  /**
   * Exact 13C-12C mass spacing; used to keep the heavy M+2 band below the pure-carbon 13C2
   * position.
   */
  private static final double C13 = 1.0033548;

  /**
   * A signal only takes part in a pair when it reaches this fraction of the base (most intense)
   * peak, so noise near the baseline does not create spurious combs or inflate intensity ratios.
   */
  private static final double MIN_PEAK_REL = 0.01;

  /**
   * Per-atom M+2 abundance above which an element is "strong" (only Br, ~0.97, among the
   * defaults).
   */
  private static final double STRONG_ABUNDANCE = 0.5;

  /**
   * An element is intensity-reachable only if the strongest observed M+2 pair ratio reaches this
   * fraction of the element's per-atom M+2 abundance (i.e. at least ~half of one atom's worth).
   * This is what stops a weak M+2 comb from being read as Br.
   */
  private static final double REACH_FRACTION = 0.5;

  /**
   * Implausibly large single-element atom count (from the M+2 intensity) above which a candidate is
   * rejected - lets a strong comb pick the higher-abundance element (Cl) over an absurd count of a
   * low-abundance one (Si).
   */
  private static final int MAX_PLAUSIBLE_ATOMS = 40;

  /**
   * Atom count up to which an element is not penalised. Above it the candidate is softly
   * down-weighted ({@code cap/atoms}): a strong M+2 comb explained by only a few Cl/Br atoms is
   * preferred over the same comb requiring dozens of low-abundance S/Si atoms. Unlike a "fewest
   * atoms" prior this does NOT favour Br over Cl (both are well under the cap), so it only removes
   * wrong-magnitude elements.
   */
  private static final double ATOM_SOFT_CAP = 8d;

  /**
   * Floor for the defect-discrimination sigma (Da, neutral mass). The median pair spacing is far
   * more precise than one peak, so the defect can separate elements ~1 mDa apart even at a looser
   * per-peak tolerance.
   */
  private static final double MIN_DEFECT_SIGMA = 0.0009;

  /**
   * Minimum best-element score for a detection to be reported.
   */
  private static final double MIN_CONFIDENCE = 0.2;

  /**
   * Minimum 29Si-band M+1 pair ratio (relative to the base peak) to count as an Si fingerprint.
   */
  private static final double SI_M1_MIN = 0.02;

  /**
   * A heavy M+2 peak enters the defect median only when it reaches this fraction of the strongest
   * heavy peak, so weak 13C/15N combinations do not pull the median toward the wrong element.
   */
  private static final double SIGNIFICANT_FRACTION = 0.3;

  /**
   * Minimum per-atom M+1 abundance for an element to be treated as genuinely M+1-bearing (only
   * 29Si, ~0.05, among the defaults). Elements with a mere trace M+1 isotope (e.g. Cl/S) are NOT,
   * so the Si M+1 fingerprint boost/penalty is not misapplied to them.
   */
  private static final double SIGNIFICANT_M1_REL = 0.02;

  private ElementAutoDetector() {
  }

  /**
   * Detect heavy elements from the default candidate set ({@link #DEFAULT_CANDIDATES}) at a known
   * charge.
   *
   * @param signals     the pattern signals (m/z + intensity); order does not matter
   * @param charge      the pattern charge (values &lt; 1 are treated as 1)
   * @param neutralMass rough neutral mass of the ion (currently unused by the detection core; kept
   *                    for API stability and future carbon-count use)
   * @param tol         the m/z tolerance of the source data
   * @return the detected composition (possibly empty)
   */
  @NotNull
  public static DetectedComposition detect(@Nullable final List<DataPoint> signals,
      final int charge, final double neutralMass, @NotNull final MZTolerance tol) {
    return detect(signals, charge, neutralMass, tol, DEFAULT_CANDIDATES);
  }

  /**
   * Detect heavy elements from a custom candidate set at a known charge.
   *
   * @param signals     the pattern signals (m/z + intensity); order does not matter
   * @param charge      the pattern charge (values &lt; 1 are treated as 1)
   * @param neutralMass rough neutral mass of the ion (currently unused by the detection core)
   * @param tol         the m/z tolerance of the source data
   * @param candidates  the heavy-element symbols to consider
   * @return the detected composition (possibly empty)
   */
  @NotNull
  public static DetectedComposition detect(@Nullable final List<DataPoint> signals,
      final int charge, final double neutralMass, @NotNull final MZTolerance tol,
      @NotNull final List<String> candidates) {
    if (signals == null || signals.size() < 2 || candidates.isEmpty()) {
      return DetectedComposition.empty();
    }
    final int z = Math.max(1, charge);

    final List<DataPoint> sorted = new ArrayList<>(signals);
    sorted.sort(Comparator.comparingDouble(DataPoint::getMZ));
    double baseInt = 0d;
    for (final DataPoint dp : sorted) {
      baseInt = Math.max(baseInt, dp.getIntensity());
    }
    if (baseInt <= 0d) {
      return DetectedComposition.empty();
    }
    final double minPeak = baseInt * MIN_PEAK_REL;

    final List<ElementIsotopes> elements = buildElementIsotopes(candidates);
    if (elements.isEmpty()) {
      return DetectedComposition.empty();
    }

    // neutral-mass matching window: m/z tolerance at a representative m/z, scaled by charge. A fixed
    // m/z error maps to a charge-times-larger neutral-mass error, so discrimination degrades with z.
    final double medMz = sorted.get(sorted.size() / 2).getMZ();
    final double tolNeutral = Math.max(1e-4, tol.getMzToleranceForMass(medMz) * z);

    // heavy M+2 band: spans the candidate M+2 defects, widened by the tolerance, but kept below the
    // 13C2 position (2*C13 ~ 2.0067) so pure-carbon peaks never enter.
    double minM2 = Double.POSITIVE_INFINITY;
    double maxM2 = Double.NEGATIVE_INFINITY;
    for (final ElementIsotopes e : elements) {
      minM2 = Math.min(minM2, e.m2Delta());
      maxM2 = Math.max(maxM2, e.m2Delta());
    }
    // widen by 2x the tolerance: each of the two peaks in a pair can be shifted by up to the
    // tolerance, so their measured spacing can be off by twice that. Keep the upper edge below 13C2.
    final double bandLo = minM2 - 2d * tolNeutral;
    final double bandHi = Math.min(maxM2 + 2d * tolNeutral,
        2d * C13 - Math.max(0.004, 0.5 * tolNeutral));

    // Collect heavy M+2 evidence (bidirectional: every lower/higher signal pair at ~+2 Da). Each
    // qualifying higher peak is a heavy-M+2 signal: record its measured neutral spacing (for the
    // robust defect) and track the strongest such peak RELATIVE TO THE BASE. Base-relative strength is
    // used (not the partner ratio) because a partner ratio is inflated by a weak lower peak, letting
    // 13C/15N combinations in high-carbon molecules masquerade as a strong heavy signal.
    final List<double[]> heavyPairs = new ArrayList<>(); // {measuredNeutralSpacing, partnerIntensity}
    double maxHeavyInt = 0d;
    for (int i = 0; i < sorted.size(); i++) {
      final double pMz = sorted.get(i).getMZ();
      if (sorted.get(i).getIntensity() < minPeak) {
        continue;
      }
      for (int j = i + 1; j < sorted.size(); j++) {
        final double d = (sorted.get(j).getMZ() - pMz) * z;
        if (d > bandHi) {
          break; // sorted by m/z -> all further j are even larger
        }
        if (d >= bandLo && sorted.get(j).getIntensity() >= minPeak) {
          heavyPairs.add(new double[]{d, sorted.get(j).getIntensity()});
          maxHeavyInt = Math.max(maxHeavyInt, sorted.get(j).getIntensity());
        }
      }
    }
    if (heavyPairs.isEmpty()) {
      return DetectedComposition.empty();
    }

    // 29Si / 33S M+1 band (below the 13C M+1 position), base-relative - the Si fingerprint used to
    // separate Si from the defect-degenerate Cl.
    final double m1Ratio = strongestM1Heavy(sorted, z, minPeak, baseInt, tolNeutral, elements);

    // Robust defect from the SIGNIFICANT heavy peaks only (partner >= a fraction of the strongest
    // heavy peak). This keeps weak 13C/15N combinations in high-carbon molecules from pulling the
    // median toward the wrong element; the dominant heavy element's peaks drive it.
    final List<Double> strongSpacings = new ArrayList<>();
    for (final double[] p : heavyPairs) {
      if (p[1] >= SIGNIFICANT_FRACTION * maxHeavyInt) {
        strongSpacings.add(p[0]);
      }
    }
    final double medDelta = medianOf(strongSpacings);
    final double maxRatio = maxHeavyInt / baseInt;
    // Self-calibrating defect sigma from the observed spread of the spacings: tight (near the floor)
    // for a clean comb, so the defect sharply separates neighbouring elements; wide when the peaks are
    // m/z-shifted, so the score degrades gracefully instead of collapsing. Beyond the spread the
    // defect simply cannot separate elements closer than the shift (e.g. Cl vs Br, 0.9 mDa).
    final double defectSigma = Math.max(MIN_DEFECT_SIGMA, 1.5d * stdDevOf(strongSpacings));

    return classify(elements, medDelta, maxRatio, m1Ratio, defectSigma);
  }

  /**
   * Try several charge states when the charge is unknown, returning the detected composition per
   * charge (1..{@code maxCharge}). The neutral mass is estimated as {@code lowestMz * z}.
   *
   * @param signals    the pattern signals
   * @param maxCharge  highest charge to try (&gt;= 1)
   * @param tol        the m/z tolerance of the source data
   * @param candidates the heavy-element symbols to consider
   * @return charge to detected composition (only charges with a non-empty detection are included)
   */
  @NotNull
  public static Map<Integer, DetectedComposition> detectAcrossCharges(
      @Nullable final List<DataPoint> signals, final int maxCharge, @NotNull final MZTolerance tol,
      @NotNull final List<String> candidates) {
    final Map<Integer, DetectedComposition> byCharge = new TreeMap<>();
    if (signals == null || signals.isEmpty()) {
      return byCharge;
    }
    double lowestMz = Double.POSITIVE_INFINITY;
    for (final DataPoint dp : signals) {
      lowestMz = Math.min(lowestMz, dp.getMZ());
    }
    for (int z = 1; z <= Math.max(1, maxCharge); z++) {
      final double neutralMass = lowestMz * z;
      final DetectedComposition c = detect(signals, z, neutralMass, tol, candidates);
      if (!c.elements().isEmpty()) {
        byCharge.put(z, c);
      }
    }
    return byCharge;
  }

  /**
   * Strongest M+1 heavy peak intensity RELATIVE TO THE BASE whose neutral spacing (from any lower
   * signal) sits in the 29Si/33S band (below the 13C M+1 position), i.e. the Si fingerprint.
   * Bidirectional (all pairs), so it is mono-independent; base-relative so a weak lower peak cannot
   * inflate it.
   */
  private static double strongestM1Heavy(@NotNull final List<DataPoint> sorted, final int z,
      final double minPeak, final double baseInt, final double tolNeutral,
      @NotNull final List<ElementIsotopes> elements) {
    double lo = Double.POSITIVE_INFINITY;
    double hi = Double.NEGATIVE_INFINITY;
    for (final ElementIsotopes e : elements) {
      // only genuinely M+1-bearing elements (29Si) define the band; trace M+1 isotopes are ignored
      if (e.m1Delta() != null && e.m1Rel() >= SIGNIFICANT_M1_REL) {
        lo = Math.min(lo, e.m1Delta());
        hi = Math.max(hi, e.m1Delta());
      }
    }
    if (lo == Double.POSITIVE_INFINITY) {
      return 0d;
    }
    // tight lower margin (<= 2.5 mDa) so a heavy-isotope + 13C artifact just below 29Si (e.g. a
    // Br+13C pair at ~0.9946 Da) does not masquerade as an Si M+1 signal
    final double bandLo = lo - Math.min(tolNeutral, 0.0025);
    // keep below the 13C M+1 position so the (much stronger) 13C peak never counts as an Si signal
    final double bandHi = Math.min(hi + tolNeutral, C13 - Math.max(0.003, 0.5 * tolNeutral));
    double bestInt = 0d;
    for (int i = 0; i < sorted.size(); i++) {
      final double pMz = sorted.get(i).getMZ();
      if (sorted.get(i).getIntensity() < minPeak) {
        continue;
      }
      for (int j = i + 1; j < sorted.size(); j++) {
        final double d = (sorted.get(j).getMZ() - pMz) * z;
        if (d > bandHi) {
          break;
        }
        if (d >= bandLo && sorted.get(j).getIntensity() >= minPeak) {
          bestInt = Math.max(bestInt, sorted.get(j).getIntensity());
        }
      }
    }
    return baseInt > 0d ? bestInt / baseInt : 0d;
  }

  /**
   * Score each candidate from the robust median M+2 defect (position), the strongest M+2 ratio
   * (intensity reachability + atom-count plausibility) and the Si M+1 fingerprint, then report the
   * best element plus any co-detected element that is clearly defect-separated from it.
   */
  @NotNull
  private static DetectedComposition classify(@NotNull final List<ElementIsotopes> elements,
      final double medDelta, final double maxRatio, final double m1Ratio,
      final double defectSigma) {
    final Map<String, Double> score = new LinkedHashMap<>();
    final Map<String, int[]> counts = new LinkedHashMap<>();
    ElementIsotopes best = null;
    double bestScore = 0d;

    for (final ElementIsotopes e : elements) {
      // intensity reachability: the strongest M+2 pair must reach ~half of one atom's worth
      if (maxRatio < REACH_FRACTION * e.m2Rel()) {
        continue;
      }
      // rough atom count from the strongest (base-relative) M+2 signal; reject absurd counts of a
      // low-abundance element (e.g. reading a strong Cl comb as dozens of Si atoms)
      final int atoms = (int) Math.round(maxRatio / e.m2Rel());
      if (atoms < 1 || atoms > MAX_PLAUSIBLE_ATOMS) {
        continue;
      }
      // position: how well the robust median spacing matches this element's exact M+2 defect. The
      // base-relative reach gate above already excludes the wrong-magnitude elements (a weak comb
      // cannot be Br/Cl; a single strong atom cannot look like Si), so the defect chooses among the
      // magnitude-plausible candidates. The median is robust for multi-atom combs (many pairs); for a
      // pattern with too few heavy peaks to average, discrimination degrades gracefully.
      final double defect = (medDelta - e.m2Delta()) / defectSigma;
      // soft down-weight for elements needing an implausibly large atom count to explain the observed
      // M+2 strength - stops a weak element (S/Si) from claiming a strong halogen comb when a widened
      // (jittered) sigma leaves the defect unable to discriminate.
      final double atomPrior = atoms <= ATOM_SOFT_CAP ? 1d : ATOM_SOFT_CAP / atoms;
      double s = Math.exp(-defect * defect) * atomPrior;

      // Si vs the defect-degenerate Cl: only Si carries a genuine M+1 isotope (29Si). Boost the
      // M+1-bearing element (Si) when the M+1 fingerprint is present and damp it when absent; damp a
      // no-M+1 element (Cl) when a strong M+1 is present (it argues for Si over Cl). Elements with only
      // a trace M+1 (Cl, S) are treated as no-M+1 via the significant-abundance check.
      final boolean bearsM1 = e.m1Delta() != null && e.m1Rel() >= SIGNIFICANT_M1_REL;
      if (bearsM1 && e.m2Rel() < STRONG_ABUNDANCE) {
        s *= m1Ratio >= SI_M1_MIN ? 1.3d : 0.7d;
      } else if (!bearsM1 && e.m2Rel() < STRONG_ABUNDANCE && m1Ratio >= SI_M1_MIN) {
        s *= 0.7d;
      }

      score.put(e.symbol(), s);
      counts.put(e.symbol(), new int[]{Math.max(1, atoms), Math.max(1, atoms)});
      if (s > bestScore) {
        bestScore = s;
        best = e;
      }
    }

    if (best == null || bestScore < MIN_CONFIDENCE) {
      return DetectedComposition.empty();
    }

    final LinkedHashSet<String> detected = new LinkedHashSet<>();
    final Map<String, Double> confidence = new LinkedHashMap<>();
    final Map<String, int[]> keptCounts = new LinkedHashMap<>();
    detected.add(best.symbol());
    confidence.put(best.symbol(), Math.min(1d, bestScore));
    keptCounts.put(best.symbol(), counts.get(best.symbol()));

    // co-detect a second element only when it scores well AND its M+2 defect is clearly separated
    // from the primary (more than the achievable precision) - i.e. resolved, low-jitter data. At a
    // loose tolerance the sigma is large, so this stays conservative and reports a single element.
    for (final ElementIsotopes e : elements) {
      if (e.symbol().equals(best.symbol())) {
        continue;
      }
      final Double s = score.get(e.symbol());
      if (s == null || s < MIN_CONFIDENCE || s < 0.6d * bestScore) {
        continue;
      }
      if (Math.abs(e.m2Delta() - best.m2Delta()) > 2d * defectSigma) {
        detected.add(e.symbol());
        confidence.put(e.symbol(), Math.min(1d, s));
        keptCounts.put(e.symbol(), counts.get(e.symbol()));
      }
    }

    return new DetectedComposition(detected, keptCounts, confidence);
  }

  /**
   * Median of the measured neutral M+2 spacings. Robust to per-peak m/z jitter: its error shrinks
   * with the count, recovering sub-tolerance defect precision.
   */
  private static double medianOf(@NotNull final List<Double> values) {
    final double[] d = new double[values.size()];
    for (int i = 0; i < d.length; i++) {
      d[i] = values.get(i);
    }
    java.util.Arrays.sort(d);
    final int mid = d.length / 2;
    return d.length % 2 == 1 ? d[mid] : (d[mid - 1] + d[mid]) / 2d;
  }

  /**
   * Population standard deviation of the values (0 for fewer than two values).
   */
  private static double stdDevOf(@NotNull final List<Double> values) {
    if (values.size() < 2) {
      return 0d;
    }
    double mean = 0d;
    for (final double v : values) {
      mean += v;
    }
    mean /= values.size();
    double sumSq = 0d;
    for (final double v : values) {
      sumSq += (v - mean) * (v - mean);
    }
    return Math.sqrt(sumSq / values.size());
  }

  /**
   * Build the diagnostic M+2 (and optional M+1) isotope of each candidate from CDK data, skipping
   * elements without an M+2 isotope.
   */
  @NotNull
  private static List<ElementIsotopes> buildElementIsotopes(
      @NotNull final List<String> candidates) {
    final List<ElementIsotopes> out = new ArrayList<>(candidates.size());
    for (final String symbol : candidates) {
      final List<Isotope> record = IsotopesUtils.getIsotopeRecord(symbol);
      final Isotope m2 = pickNearestDelta(record, 2d);
      if (m2 == null) {
        continue;
      }
      final Isotope m1 = pickNearestDelta(record, 1d);
      out.add(new ElementIsotopes(symbol, m2.deltaMass(), m2.relativeIntensity(),
          m1 == null ? null : m1.deltaMass(), m1 == null ? 0d : m1.relativeIntensity()));
    }
    return out;
  }

  /**
   * Pick the most abundant isotope whose delta mass is within 0.5 Da of {@code targetDelta}, or
   * {@code null} if none (e.g. Cl/Br have no natural M+1 isotope).
   */
  @Nullable
  private static Isotope pickNearestDelta(@NotNull final List<Isotope> record,
      final double targetDelta) {
    Isotope best = null;
    for (final Isotope iso : record) {
      if (Math.abs(iso.deltaMass() - targetDelta) <= 0.5d && (best == null
          || iso.relativeIntensity() > best.relativeIntensity())) {
        best = iso;
      }
    }
    return best;
  }
}
