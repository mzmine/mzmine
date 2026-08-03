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
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.Isotope;
import io.github.mzmine.util.IsotopesUtils;
import io.github.mzmine.util.MathUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
   * Padding (neutral Da, divided by the charge) added on both sides of a detected pattern when
   * collecting the raw spectrum window the detector runs on: roughly one extra M+2 spacing, so an
   * off-ladder heavy M+2 peak just beyond the emitted pattern is still seen.
   */
  private static final double WINDOW_PAD_DA = 2.5;

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
   * Floor for the intensity-reachability ranking weight. The weight is
   * {@code min(1, observedM2Ratio / perAtomAbundance)}, so an element whose per-atom M+2 is far
   * stronger than anything observed (Br on a weak comb) ranks low - but never below this floor,
   * because a weak or partly unresolved M+2 peak is not proof of absence.
   */
  private static final double MIN_REACH_WEIGHT = 0.05;

  /**
   * Fraction of one atom's per-atom M+2 abundance the strongest observed M+2 pair must reach for an
   * element to be POSSIBLE at all. Much looser than a "which element is it" gate (it was 0.5 when the
   * detector picked a single winner): it only rejects elements whose single atom would have to produce
   * an M+2 peak far larger than anything in the spectrum.
   */
  private static final double REACH_FLOOR_FRACTION = 0.15;

  /**
   * Minimum score for an element to be reported as possible. The score is dominated by
   * {@code exp(-(defectDeviation / defectSigma)^2)}, and {@code defectSigma} is self-calibrating from
   * the observed spacing spread, so this is a "consistent with the measured defect at the precision
   * this data supports" test: a clean CHNO pattern rejects all candidates (its 13C+15N +2 peak is many
   * sigma from every heavy defect) while a jittered or wide-tolerance pattern admits several.
   */
  private static final double MIN_CONFIDENCE = 0.2;

  /**
   * Largest single-element atom count the M+2 intensity is allowed to imply. Beyond it the count is
   * clamped (and {@link #ATOM_SOFT_CAP} ranks the candidate down) rather than the element being
   * rejected.
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

  /**
   * Minimum per-atom abundance (relative to the most abundant isotope) for an isotope to count as
   * naturally occurring in {@link #isotopeGridDeviations}. Excludes the radioactive/synthetic
   * isotopes the CDK record also carries, whose abundance is 0 or undefined.
   */
  private static final double MIN_NATURAL_ABUNDANCE_REL = 1e-7;

  private ElementAutoDetector() {
  }

  /**
   * The raw-spectrum window the detector should be run on for a detected pattern: every data point
   * within {@link #WINDOW_PAD_DA}{@code /charge} of the pattern's m/z range.
   * <p>
   * decision: the detector must NOT be fed only the emitted pattern. Heavy M+2 evidence frequently
   * sits at an offset the pattern did not keep (a weak 34S/30Si M+2 below the inclusion threshold, or
   * just past the pattern's last offset), and without it the element is undetectable. This is the
   * single definition of that window, shared by {@link IsotopeFinderEngine} (which detects during
   * processing) and the benchmark's element metric, so the measurement cannot drift from what the
   * engine actually does.
   *
   * @param spectrum    the source spectrum; data points must be sorted ascending by m/z.
   * @param patternLoMz lowest m/z of the detected pattern.
   * @param patternHiMz highest m/z of the detected pattern.
   * @param charge      the detected charge (values &lt; 1 are treated as 1).
   * @return the data points inside the padded window, in spectrum order.
   */
  public static @NotNull List<DataPoint> collectDetectionWindow(
      @NotNull final MassSpectrum spectrum, final double patternLoMz, final double patternHiMz,
      final int charge) {
    final double pad = WINDOW_PAD_DA / Math.max(1, charge);
    final double loMz = patternLoMz - pad;
    final double hiMz = patternHiMz + pad;
    final List<DataPoint> out = new ArrayList<>();
    final int n = spectrum.getNumberOfDataPoints();
    for (int i = 0; i < n; i++) {
      final double m = spectrum.getMzValue(i);
      if (m < loMz) {
        continue;
      }
      if (m > hiMz) {
        break; // sorted ascending by m/z
      }
      out.add(new SimpleDataPoint(m, spectrum.getIntensityValue(i)));
    }
    return out;
  }

  /**
   * Detect heavy elements from the default candidate set ({@link #DEFAULT_CANDIDATES}) at a known
   * charge.
   *
   * @param signals the pattern signals (m/z + intensity); order does not matter
   * @param charge  the pattern charge (values &lt; 1 are treated as 1)
   * @param tol     the m/z tolerance of the source data
   * @return the detected composition (possibly empty)
   */
  @NotNull
  public static DetectedComposition detect(@Nullable final List<DataPoint> signals,
      final int charge, @NotNull final MZTolerance tol) {
    return detect(signals, charge, tol, DEFAULT_CANDIDATES);
  }

  /**
   * Detect heavy elements from a custom candidate set at a known charge.
   *
   * @param signals    the pattern signals (m/z + intensity); order does not matter
   * @param charge     the pattern charge (values &lt; 1 are treated as 1)
   * @param tol        the m/z tolerance of the source data
   * @param candidates the heavy-element symbols to consider
   * @return the detected composition (possibly empty)
   */
  @NotNull
  public static DetectedComposition detect(@Nullable final List<DataPoint> signals,
      final int charge, @NotNull final MZTolerance tol,
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
    final List<double[]> heavyPairs = pairsInBand(sorted, z, minPeak, bandLo, bandHi);
    if (heavyPairs.isEmpty()) {
      return DetectedComposition.empty();
    }
    double maxHeavyInt = 0d;
    for (final double[] p : heavyPairs) {
      maxHeavyInt = Math.max(maxHeavyInt, p[1]);
    }

    // 29Si / 33S M+1 band (below the 13C M+1 position), base-relative - the Si fingerprint used to
    // separate Si from the defect-degenerate Cl.
    final double m1Ratio = strongestM1Heavy(sorted, z, minPeak, baseInt, tolNeutral, elements);

    // Robust defect from the SIGNIFICANT heavy peaks only (partner >= a fraction of the strongest
    // heavy peak). This keeps weak 13C/15N combinations in high-carbon molecules from pulling the
    // median toward the wrong element; the dominant heavy element's peaks drive it.
    final DoubleArrayList strongSpacings = new DoubleArrayList();
    for (final double[] p : heavyPairs) {
      if (p[1] >= SIGNIFICANT_FRACTION * maxHeavyInt) {
        strongSpacings.add(p[0]);
      }
    }
    final double[] spacings = strongSpacings.toDoubleArray();
    // robust to per-peak m/z jitter: the median's error shrinks with the count, which recovers
    // sub-tolerance defect precision
    final double medDelta = MathUtils.calcMedian(spacings);
    // strength of the strongest heavy M+2 signal relative to the base peak, used both for the
    // intensity-reachability ranking and for the rough atom count
    final double maxRatio = maxHeavyInt / baseInt;
    // Self-calibrating defect sigma from the observed spread of the spacings: tight (near the floor)
    // for a clean comb, so the defect sharply separates neighbouring elements; wide when the peaks are
    // m/z-shifted, so the score degrades gracefully instead of collapsing. Beyond the spread the
    // defect simply cannot separate elements closer than the shift (e.g. Cl vs Br, 0.9 mDa).
    // decision: the sigma must also respect the precision the PAIR COUNT supports. The spread alone is
    // 0 for a single pair, which claimed sub-mDa precision on data that has none - it made the defect
    // test reject every candidate on merged / unit-resolution patterns (elementContainment on
    // unit_resolution collapsed to 0.021) even though such data cannot separate the elements at all.
    // The median of n measurements each within +/- tol carries an error of ~tol/sqrt(n).
    final double countSigma = tolNeutral / Math.sqrt(Math.max(1, spacings.length));
    final double defectSigma = Math.max(MIN_DEFECT_SIGMA,
        Math.max(1.5d * stdDevOf(spacings), countSigma));

    // Membership: an element is POTENTIAL when some observed pair's neutral spacing matches one of its
    // isotope distances within the m/z tolerance (scaled by charge). decision: this - not the scoring -
    // decides who is reported. The score below only RANKS the potential set, because at any realistic
    // tolerance the candidate defects (0.2-2.2 mDa apart) cannot be resolved, so an intensity or defect
    // gate that removes a matching element is asserting a distinction the data does not support.
    final LinkedHashSet<String> spacingMatched = new LinkedHashSet<>();
    for (final ElementIsotopes e : elements) {
      // intensity IMPOSSIBILITY (not a preference): one atom of this element must produce an M+2 peak
      // of its per-atom abundance, so an element whose single-atom M+2 is far above anything observed
      // cannot be present at all. One Br needs a ~97 % M+2 - at a 5 mDa tolerance a plain 13C+15N peak
      // (2.00039) sits within tolerance of the 81Br distance (1.99795), and without this floor every
      // CHNO molecule would report Br as possible. The floor is deliberately far below one atom's worth
      // so a weak or partly unresolved M+2 peak still admits the element.
      if (maxRatio < REACH_FLOOR_FRACTION * e.m2Rel()) {
        continue;
      }
      for (final double[] p : heavyPairs) {
        if (Math.abs(p[0] - e.m2Delta()) <= tolNeutral) {
          spacingMatched.add(e.symbol());
          break;
        }
      }
    }
    if (spacingMatched.isEmpty()) {
      return DetectedComposition.empty();
    }

    return classify(elements, spacingMatched, medDelta, maxRatio, m1Ratio, defectSigma);
  }

  /**
   * Try several charge states when the charge is unknown, returning the detected composition per
   * charge (1..{@code maxCharge}).
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
    for (int z = 1; z <= Math.max(1, maxCharge); z++) {
      final DetectedComposition c = detect(signals, z, tol, candidates);
      if (!c.elements().isEmpty()) {
        byCharge.put(z, c);
      }
    }
    return byCharge;
  }

  /**
   * Attribute a single off-13C-grid signal to the most likely candidate heavy element, purely from
   * its mass defect relative to the exact 13C grid. Used both to label detected isotope-pattern
   * peaks in the compound dashboard and to decide whether an off-grid signal is explainable at all
   * (see the emitted-pattern filter in {@code IsotopeFinderEngine}).
   * <p>
   * {@code neutralDeviationFromGrid} is the neutral-mass deviation of the signal from the nearest
   * exact 13C-grid position ({@code (observedMz - exactGridMz) * charge}). Because accompanying 13C
   * atoms sit exactly on the grid, this deviation is invariant to how many carbons accompany the
   * heavy substitution: e.g. a single 37Cl (whether alone or with extra 13C) always deviates
   * {@code m2Delta - 2*C13 ~ -9.7 mDa} from the grid. Each candidate's M+2 (and, for M+1-bearing
   * elements such as Si, M+1) deviation is compared and the nearest within {@code windowNeutral} is
   * returned. Cl and Br differ by only ~0.9 mDa so are barely separable on a single peak; the
   * pattern-level {@link #detect} confidence remains the authoritative element call.
   *
   * @param neutralDeviationFromGrid signed neutral-mass deviation from the nearest exact 13C grid
   *                                 position (Da).
   * @param candidates               heavy-element symbols to consider.
   * @param windowNeutral            maximum |deviation - candidate deviation| (Da) to accept a
   *                                 match.
   * @return the best-matching element symbol, or {@code null} if none matches within the window.
   */
  @Nullable
  public static String attributeHeavyElement(final double neutralDeviationFromGrid,
      @NotNull final List<String> candidates, final double windowNeutral) {
    return attributeHeavyIsotope(neutralDeviationFromGrid, buildElementIsotopes(candidates),
        windowNeutral);
  }

  /**
   * Same as {@link #attributeHeavyElement(double, List, double)} on an already-resolved candidate
   * isotope table, so a caller that attributes many signals does not rebuild it per signal.
   *
   * @param elements the candidate elements' isotope table, see {@link #buildElementIsotopes}.
   */
  @Nullable
  static String attributeHeavyIsotope(final double neutralDeviationFromGrid,
      @NotNull final List<ElementIsotopes> elements, final double windowNeutral) {
    String best = null;
    double bestDist = windowNeutral;
    for (final ElementIsotopes e : elements) {
      // M+2 deviation from the nearest carbon grid (round(m2Delta) == 2 for Cl/Br/S/Si-30)
      final double devM2 = e.m2Delta() - Math.round(e.m2Delta()) * C13;
      final double dM2 = Math.abs(neutralDeviationFromGrid - devM2);
      if (dM2 < bestDist) {
        bestDist = dM2;
        best = e.symbol();
      }
      // M+1 deviation for genuinely M+1-bearing elements (29Si); trace M+1 isotopes are ignored
      if (e.m1Delta() != null && e.m1Rel() >= SIGNIFICANT_M1_REL) {
        final double devM1 = e.m1Delta() - Math.round(e.m1Delta()) * C13;
        final double dM1 = Math.abs(neutralDeviationFromGrid - devM1);
        if (dM1 < bestDist) {
          bestDist = dM1;
          best = e.symbol();
        }
      }
    }
    return best;
  }

  /**
   * Every distinct neutral-mass deviation from the exact 13C grid that ONE isotope substitution of
   * the given elements can produce, i.e. {@code deltaMass - round(deltaMass) * C13} per isotope.
   * <p>
   * Unlike {@link #buildElementIsotopes} this is not restricted to the M+2 isotope of heavy
   * elements: an M+1-only isotope such as 15N (-6.3 mDa) or 2H (+2.9 mDa) produces resolvable fine
   * structure just as 37Cl does, and a filter that cannot account for it treats real signals as
   * unexplained. Elements without any isotope (or with only the most abundant one) contribute
   * nothing. Multiple substitutions of one isotope are expanded by {@link IsotopeDefectTable}.
   * <p>
   * Only isotopes that actually occur in nature are included: the CDK record also carries
   * radioactive/synthetic isotopes (36Cl, 38Cl, ...) whose deviations are large and would make the
   * table permissive enough to explain almost any mass defect.
   *
   * @param candidates element symbols to consider.
   * @return the distinct deviations (Da), unsorted; empty if the candidates have no isotopes.
   */
  static double @NotNull [] isotopeGridDeviations(@NotNull final List<String> candidates) {
    final DoubleArrayList out = new DoubleArrayList();
    for (final String symbol : candidates) {
      for (final Isotope iso : IsotopesUtils.getIsotopeRecord(symbol)) {
        final double delta = iso.deltaMass();
        if (delta < 0.5d) {
          continue; // the most abundant isotope itself (delta 0), no shift
        }
        if (!(iso.relativeIntensity() > MIN_NATURAL_ABUNDANCE_REL)) {
          continue; // not naturally abundant (or NaN): cannot produce an observable signal
        }
        final double dev = delta - Math.round(delta) * C13;
        boolean known = false;
        for (final double d : out) {
          if (Math.abs(d - dev) < 1e-6) {
            known = true;
            break;
          }
        }
        if (!known) {
          out.add(dev);
        }
      }
    }
    return out.toDoubleArray();
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
    for (final double[] pair : pairsInBand(sorted, z, minPeak, bandLo, bandHi)) {
      bestInt = Math.max(bestInt, pair[1]);
    }
    return baseInt > 0d ? bestInt / baseInt : 0d;
  }

  /**
   * Every (lower, higher) pair of significant signals whose measured NEUTRAL spacing falls inside
   * {@code [bandLo, bandHi]}. Bidirectional over the whole envelope, so it is mono-independent: the
   * comb of spaced pairs survives even when the monoisotopic is below the detection threshold.
   * <p>
   * Shared by the M+2 heavy band and the 29Si M+1 fingerprint band, which previously ran two copies
   * of this scan.
   *
   * @param sorted  the signals, ascending by m/z.
   * @param z       the pattern charge (m/z spacings are multiplied by it to get neutral spacings).
   * @param minPeak minimum intensity for a signal to take part in a pair.
   * @param bandLo  inclusive lower neutral-spacing bound.
   * @param bandHi  inclusive upper neutral-spacing bound.
   * @return list of {@code {measuredNeutralSpacing, higherPeakIntensity}}.
   */
  @NotNull
  private static List<double[]> pairsInBand(@NotNull final List<DataPoint> sorted, final int z,
      final double minPeak, final double bandLo, final double bandHi) {
    final List<double[]> pairs = new ArrayList<>();
    for (int i = 0; i < sorted.size(); i++) {
      if (sorted.get(i).getIntensity() < minPeak) {
        continue;
      }
      final double pMz = sorted.get(i).getMZ();
      for (int j = i + 1; j < sorted.size(); j++) {
        final double d = (sorted.get(j).getMZ() - pMz) * z;
        if (d > bandHi) {
          break; // sorted by m/z -> all further j are even larger
        }
        if (d >= bandLo && sorted.get(j).getIntensity() >= minPeak) {
          pairs.add(new double[]{d, sorted.get(j).getIntensity()});
        }
      }
    }
    return pairs;
  }

  /**
   * Score each candidate from the robust median M+2 defect (position), the strongest M+2 ratio
   * (intensity reachability + atom-count plausibility) and the Si M+1 fingerprint, then report EVERY
   * candidate the evidence cannot rule out, best first.
   * <p>
   * decision: report the whole ambiguity set rather than one winner. The candidate M+2 defects sit
   * 0.2-2.2 mDa apart (Cl vs Br is 0.9 mDa), which is below the achievable precision on most data, so
   * picking a single element there is a coin flip presented as a result. It also failed in the
   * direction that matters: the previous rule co-detected a second element only when the two defects
   * were separated by more than {@code 2 x defectSigma}, and since sigma grows with the observed
   * spacing jitter, a WIDER tolerance - less able to discriminate - produced a MORE confident-looking
   * single-element answer. With the default candidate set that test could only ever fire for Br+S.
   * <p>
   * The consumer wants the set: the composition feeds the heavy-isotope UPPER BOUND of the predicted
   * envelope, and a bound must cover every element still in play. Elements are still ranked - the
   * per-element {@link DetectedComposition#confidence()} carries the score and the iteration order is
   * best first - so a caller that needs one label can take the first.
   */
  @NotNull
  private static DetectedComposition classify(@NotNull final List<ElementIsotopes> elements,
      @NotNull final Set<String> spacingMatched, final double medDelta, final double maxRatio,
      final double m1Ratio, final double defectSigma) {
    final Map<String, Double> score = new LinkedHashMap<>();
    final Map<String, int[]> counts = new LinkedHashMap<>();

    for (final ElementIsotopes e : elements) {
      // membership is decided by the spacing match; the loop below only ranks those
      if (!spacingMatched.contains(e.symbol())) {
        continue;
      }
      // rough atom count from the strongest (base-relative) M+2 signal, clamped rather than used as a
      // gate: an element whose per-atom abundance makes the observed M+2 look like a fractional or an
      // absurd atom count is ranked down (atomPrior below), not removed.
      final int atoms = Math.max(1,
          Math.min(MAX_PLAUSIBLE_ATOMS, (int) Math.round(maxRatio / e.m2Rel())));
      // position: how well the robust median spacing matches this element's exact M+2 defect. The
      // median is robust for multi-atom combs (many pairs); for a pattern with too few heavy peaks to
      // average, discrimination degrades gracefully.
      final double defect = (medDelta - e.m2Delta()) / defectSigma;
      // soft down-weight for elements needing an implausibly large atom count to explain the observed
      // M+2 strength - stops a weak element (S/Si) from claiming a strong halogen comb when a widened
      // (jittered) sigma leaves the defect unable to discriminate.
      final double atomPrior = atoms <= ATOM_SOFT_CAP ? 1d : ATOM_SOFT_CAP / atoms;
      // intensity plausibility as a RANKING term (it used to be a hard gate): the strongest M+2 pair
      // should reach about one atom's worth of this element's per-atom abundance. Br needs a ~97 % M+2
      // per atom, so a weak comb ranks Br far below S/Si without claiming Br is impossible.
      final double reach = e.m2Rel() <= 0d ? 1d : Math.min(1d, maxRatio / e.m2Rel());
      double s = Math.exp(-defect * defect) * atomPrior * Math.max(reach, MIN_REACH_WEIGHT);

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

      // final membership test: consistent with the MEASURED defect at the precision this data
      // supports. The spacing match above is a coarse per-pair test against the raw tolerance; this
      // uses the robust median defect against the self-calibrating sigma, which is what keeps a plain
      // CHNO pattern (whose +2 peak is 13C+15N, many sigma from every heavy defect) from reporting
      // S/Si as possible while still admitting several candidates on jittered or unresolved data.
      if (s < MIN_CONFIDENCE) {
        continue;
      }
      score.put(e.symbol(), s);
      counts.put(e.symbol(), new int[]{atoms, atoms});
    }

    if (score.isEmpty()) {
      return DetectedComposition.empty();
    }

    // report every surviving candidate, ranked best first - no "winner takes the slot" step. The score
    // expresses which of the (often indistinguishable) matches the defect and intensities favour.
    final List<ElementIsotopes> reported = new ArrayList<>();
    for (final ElementIsotopes e : elements) {
      if (score.containsKey(e.symbol())) {
        reported.add(e);
      }
    }
    reported.sort(
        Comparator.comparingDouble((ElementIsotopes e) -> score.get(e.symbol())).reversed());

    final LinkedHashSet<String> detected = new LinkedHashSet<>();
    final Map<String, Double> confidence = new LinkedHashMap<>();
    final Map<String, int[]> keptCounts = new LinkedHashMap<>();
    for (final ElementIsotopes e : reported) {
      detected.add(e.symbol());
      confidence.put(e.symbol(), Math.min(1d, score.get(e.symbol())));
      keptCounts.put(e.symbol(), counts.get(e.symbol()));
    }

    return new DetectedComposition(detected, keptCounts, confidence);
  }

  /**
   * Population standard deviation of the values (0 for fewer than two values).
   * <p>
   * decision: deliberately NOT {@link io.github.mzmine.util.MathUtils#calcStd}, which is the SAMPLE
   * (n-1) deviation. The spacings are the whole population of observed pairs, and the sample
   * correction would inflate the self-calibrating defect sigma for the small pair counts that are
   * the common case.
   */
  private static double stdDevOf(final double @NotNull [] values) {
    if (values.length < 2) {
      return 0d;
    }
    double mean = 0d;
    for (final double v : values) {
      mean += v;
    }
    mean /= values.length;
    double sumSq = 0d;
    for (final double v : values) {
      sumSq += (v - mean) * (v - mean);
    }
    return Math.sqrt(sumSq / values.length);
  }

  /**
   * Build the diagnostic M+2 (and optional M+1) isotope of each candidate from CDK data, skipping
   * elements without an M+2 isotope.
   */
  @NotNull
  static List<ElementIsotopes> buildElementIsotopes(@NotNull final List<String> candidates) {
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
