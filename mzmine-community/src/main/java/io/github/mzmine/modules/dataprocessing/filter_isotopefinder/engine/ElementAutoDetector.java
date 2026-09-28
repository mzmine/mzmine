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
import io.github.mzmine.util.IsotopePatternUtils;
import io.github.mzmine.util.IsotopesUtils;
import io.github.mzmine.util.MathUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
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
 * Infers which heavy elements (default {@code Cl, Br, S, Si}) are <i>possible</i> in an isotope
 * pattern, from the m/z spacings between an element's major isotopes (M+2, plus the 29Si M+1
 * fingerprint) and the intensities of the spaced signals. Masses and abundances come from CDK via
 * {@link IsotopesUtils#getIsotopeRecord(String)}, so the candidate list is freely extensible.
 * <p>
 * Three properties it needs on real data:
 * <ul>
 *   <li><b>mono-independent</b> - it works off PAIRS of spaced signals gathered in both directions,
 *   so the seed may be any peak of the pattern and the monoisotopic may be missing entirely (large
 *   poly-halogen or protein humps);</li>
 *   <li><b>charge-aware</b> - spacings are {@code isotopeDelta / z} and every window scales with the
 *   tolerance times the charge;</li>
 *   <li><b>tolerance-robust</b> - the four candidate M+2 defects lie within ~2 mDa, well under a
 *   typical 5 mDa tolerance, so one peak cannot separate them. The MEDIAN spacing over many pairs
 *   recovers sub-tolerance precision; intensity and the 29Si M+1 disambiguate the rest.</li>
 * </ul>
 * Atom counts are deliberately NOT resolved; {@code counts} is a best-effort hint only.
 */
public final class ElementAutoDetector {

  @NotNull
  public static final List<String> DEFAULT_CANDIDATES = List.of("Cl", "Br", "S", "Si");

  // padding (neutral Da / charge) around the pattern for the raw detection window: ~one extra M+2
  // spacing, so an off-ladder heavy M+2 just past the emitted pattern is still seen
  private static final double WINDOW_PAD_DA = 2.5;
  // base-relative floor for a signal to take part in a pair, so baseline noise cannot build
  // spurious combs or inflate the ratios
  private static final double MIN_PEAK_REL = 0.01;

  /**
   * Per-atom M+2 abundance above which an element's own comb is decisive enough that the 29Si M+1
   * fingerprint must not weigh against it in {@link #classify} - a molecule can carry Si AND Br, so
   * an Si M+1 is no argument against a near-1:1 M+2 comb. Only Br clears it among the defaults
   * (81Br/79Br ~0.97); Cl (~0.32), S (~0.04) and Si (~0.03) stay below, so a present Si M+1 boosts
   * Si and damps the M+1-less Cl and S.
   * <p>
   * RANKING only (a 0.7/1.3 factor): membership is decided by the spacing match plus
   * {@link #REACH_FLOOR_FRACTION}, neither of which reads this constant.
   */
  private static final double STRONG_ABUNDANCE = 0.5;

  // floor for the reachability weight min(1, observedM2Ratio / perAtomAbundance): a weak or partly
  // unresolved M+2 peak is not proof of absence
  private static final double MIN_REACH_WEIGHT = 0.05;
  // fraction of one atom's M+2 abundance the strongest observed pair must reach for the element to
  // be POSSIBLE. Deliberately far below one atom's worth - it only rejects elements whose single
  // atom would have to produce an M+2 larger than anything in the spectrum.
  private static final double REACH_FLOOR_FRACTION = 0.15;
  // minimum score to be reported. Because the score is dominated by exp(-(defect/sigma)^2) and sigma
  // self-calibrates, this reads as "consistent with the measured defect at the precision this data
  // supports": clean CHNO rejects every candidate, jittered or wide-tolerance data admits several.
  private static final double MIN_CONFIDENCE = 0.2;
  // atom count the M+2 intensity may imply before it is clamped (and ranked down, not rejected)
  private static final int MAX_PLAUSIBLE_ATOMS = 40;
  // above this the candidate is down-weighted by cap/atoms, so a strong comb from a few Cl/Br atoms
  // beats the same comb needing dozens of low-abundance S/Si. Both Cl and Br are well under the cap,
  // so unlike a "fewest atoms" prior this only removes wrong-magnitude elements.
  private static final double ATOM_SOFT_CAP = 8d;
  // sigma floor (Da): the median pair spacing is far more precise than one peak, so the defect can
  // still separate elements ~1 mDa apart at a looser per-peak tolerance
  private static final double MIN_DEFECT_SIGMA = 0.0009;
  // base-relative 29Si-band M+1 ratio that counts as an Si fingerprint
  private static final double SI_M1_MIN = 0.02;
  // share of the strongest heavy peak an M+2 must reach to enter the defect median, so weak 13C/15N
  // combinations cannot pull it toward the wrong element
  private static final double SIGNIFICANT_FRACTION = 0.3;
  // per-atom M+1 abundance to count as genuinely M+1-bearing (only 29Si among the defaults), so the
  // Si fingerprint boost is not misapplied to trace-M+1 elements like Cl/S
  private static final double SIGNIFICANT_M1_REL = 0.02;
  // excludes the radioactive/synthetic isotopes the CDK record also carries (abundance 0 or NaN),
  // whose large deviations would let isotopeGridDeviations explain almost any mass defect
  private static final double MIN_NATURAL_ABUNDANCE_REL = 1e-7;

  private ElementAutoDetector() {
  }

  /**
   * The raw-spectrum window the detector should be run on for a detected pattern: every data point
   * within {@link #WINDOW_PAD_DA}{@code /charge} of the pattern's m/z range.
   * <p>
   * decision: the detector must NOT be fed only the emitted pattern - heavy M+2 evidence frequently
   * sits at an offset the pattern did not keep (a weak 34S/30Si M+2 below the inclusion threshold, or
   * just past the last offset), and without it the element is undetectable. Shared by
   * {@link IsotopeFinderEngine} and the benchmark's element metric so the measurement cannot drift
   * from what the engine does.
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

  @NotNull
  public static DetectedComposition detect(@Nullable final List<DataPoint> signals,
      final int charge, @NotNull final MZTolerance tol) {
    return detect(signals, charge, tol, DEFAULT_CANDIDATES);
  }

  /**
   * @param signals    the pattern signals; order does not matter
   * @param charge     the pattern charge (values &lt; 1 are treated as 1)
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

    // a fixed m/z error maps to a charge-times-larger NEUTRAL-mass error, so discrimination
    // legitimately degrades with z
    final double medMz = sorted.get(sorted.size() / 2).getMZ();
    final double tolNeutral = Math.max(1e-4, tol.getMzToleranceForMass(medMz) * z);

    double minM2 = Double.POSITIVE_INFINITY;
    double maxM2 = Double.NEGATIVE_INFINITY;
    for (final ElementIsotopes e : elements) {
      minM2 = Math.min(minM2, e.m2Delta());
      maxM2 = Math.max(maxM2, e.m2Delta());
    }
    // band over the candidate M+2 defects, widened 2x (both peaks of a pair can be off by the full
    // tolerance) but held below the pure-carbon 13C2 position so 13C2 peaks never enter
    final double bandLo = minM2 - 2d * tolNeutral;
    final double bandHi = Math.min(maxM2 + 2d * tolNeutral,
        2d * IsotopePatternUtils.C13_MZ_DELTA - Math.max(0.004, 0.5 * tolNeutral));

    // decision: heavy-peak strength is measured RELATIVE TO THE BASE, not as the partner ratio - a
    // partner ratio is inflated by a weak lower peak, which let 13C/15N combinations in high-carbon
    // molecules masquerade as a strong heavy signal.
    final List<double[]> heavyPairs = pairsInBand(sorted, z, minPeak, bandLo, bandHi);
    if (heavyPairs.isEmpty()) {
      return DetectedComposition.empty();
    }
    double maxHeavyInt = 0d;
    for (final double[] p : heavyPairs) {
      maxHeavyInt = Math.max(maxHeavyInt, p[1]);
    }

    // the Si fingerprint that separates Si from the defect-degenerate Cl
    final double m1Ratio = strongestM1Heavy(sorted, z, minPeak, baseInt, tolNeutral, elements);

    // significant peaks only, so weak 13C/15N combinations cannot pull the median off the element
    final DoubleArrayList strongSpacings = new DoubleArrayList();
    for (final double[] p : heavyPairs) {
      if (p[1] >= SIGNIFICANT_FRACTION * maxHeavyInt) {
        strongSpacings.add(p[0]);
      }
    }
    final double[] spacings = strongSpacings.toDoubleArray();
    final double medDelta = MathUtils.calcMedian(spacings);
    final double maxRatio = maxHeavyInt / baseInt;
    // self-calibrating: tight for a clean comb so the defect separates neighbours sharply, wide for
    // m/z-shifted peaks so the score degrades instead of collapsing.
    // decision: it must also respect the precision the PAIR COUNT supports (~tol/sqrt(n)). The spread
    // alone is 0 for a single pair, claiming sub-mDa precision on data that has none, which made the
    // test reject every candidate at unit resolution (elementContainment there collapsed to 0.021).
    final double countSigma = tolNeutral / Math.sqrt(Math.max(1, spacings.length));
    final double defectSigma = Math.max(MIN_DEFECT_SIGMA,
        Math.max(1.5d * stdDevOf(spacings), countSigma));

    // decision: MEMBERSHIP is the spacing match (plus the impossibility floor below), not the
    // scoring - the score only RANKS. At any realistic tolerance the candidate defects (0.2-2.2 mDa
    // apart) are unresolvable, so a gate that removed a matching element would assert a distinction
    // the data does not support.
    final LinkedHashSet<String> spacingMatched = new LinkedHashSet<>();
    for (final ElementIsotopes e : elements) {
      // IMPOSSIBILITY, not a preference: one atom must produce its per-atom M+2 abundance. Without
      // this every CHNO molecule reports Br, since at a 5 mDa tolerance a plain 13C+15N peak
      // (2.00039) falls within tolerance of the 81Br distance (1.99795).
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
   * Every distinct neutral-mass deviation from the exact 13C grid that ONE isotope substitution of
   * the given elements can produce. Multiple substitutions are expanded by
   * {@link IsotopeDefectTable}.
   * <p>
   * Unlike {@link #buildElementIsotopes}, NOT restricted to M+2: an M+1-only isotope such as 15N
   * (-6.3 mDa) or 2H (+2.9 mDa) produces resolvable fine structure just as 37Cl does, and a filter
   * blind to it would treat real signals as unexplained.
   *
   * @return the distinct deviations (Da), unsorted.
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
        final double dev = delta - Math.round(delta) * IsotopePatternUtils.C13_MZ_DELTA;
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
   * The Si fingerprint: strongest base-relative M+1 peak whose neutral spacing sits in the 29Si
   * band, below the 13C M+1 position.
   */
  private static double strongestM1Heavy(@NotNull final List<DataPoint> sorted, final int z,
      final double minPeak, final double baseInt, final double tolNeutral,
      @NotNull final List<ElementIsotopes> elements) {
    double lo = Double.POSITIVE_INFINITY;
    double hi = Double.NEGATIVE_INFINITY;
    for (final ElementIsotopes e : elements) {
      // only genuinely M+1-bearing elements (29Si) define the band
      if (e.m1Delta() != null && e.m1Rel() >= SIGNIFICANT_M1_REL) {
        lo = Math.min(lo, e.m1Delta());
        hi = Math.max(hi, e.m1Delta());
      }
    }
    if (lo == Double.POSITIVE_INFINITY) {
      return 0d;
    }
    // tight lower margin so a Br+13C artifact just below 29Si (~0.9946 Da) cannot pass as an Si
    // M+1; the upper edge stays below the 13C M+1 so the much stronger 13C peak never counts
    final double bandLo = lo - Math.min(tolNeutral, 0.0025);
    final double bandHi = Math.min(hi + tolNeutral,
        IsotopePatternUtils.C13_MZ_DELTA - Math.max(0.003, 0.5 * tolNeutral));
    double bestInt = 0d;
    for (final double[] pair : pairsInBand(sorted, z, minPeak, bandLo, bandHi)) {
      bestInt = Math.max(bestInt, pair[1]);
    }
    return baseInt > 0d ? bestInt / baseInt : 0d;
  }

  /**
   * Every (lower, higher) pair of significant signals whose measured NEUTRAL spacing falls in
   * {@code [bandLo, bandHi]}. Scanning the whole envelope both ways is what makes the detector
   * mono-independent: the comb of spaced pairs survives a monoisotopic below the detection floor.
   *
   * @param sorted the signals, ascending by m/z.
   * @param z      the pattern charge (m/z spacings are multiplied by it to get neutral spacings).
   * @return {@code {measuredNeutralSpacing, higherPeakIntensity}} per pair.
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
   * Rank the spacing-matched candidates by median M+2 defect, M+2 intensity and the Si M+1
   * fingerprint, and report every one the evidence cannot rule out, best first.
   * <p>
   * decision: the whole ambiguity set, not one winner. The candidate defects sit 0.2-2.2 mDa apart
   * (Cl vs Br is 0.9 mDa), below the achievable precision on most data. The old single-winner rule
   * also failed in the direction that matters: it added a second element only when the defects were
   * more than {@code 2 x defectSigma} apart, and sigma grows with jitter, so a WIDER tolerance
   * produced a MORE confident-looking answer. The consumer wants the set anyway - it feeds the
   * heavy-isotope UPPER BOUND, which must cover every element still in play.
   */
  @NotNull
  private static DetectedComposition classify(@NotNull final List<ElementIsotopes> elements,
      @NotNull final Set<String> spacingMatched, final double medDelta, final double maxRatio,
      final double m1Ratio, final double defectSigma) {
    final Map<String, Double> score = new LinkedHashMap<>();
    final Map<String, int[]> counts = new LinkedHashMap<>();

    for (final ElementIsotopes e : elements) {
      if (!spacingMatched.contains(e.symbol())) {
        continue;
      }
      // clamped, not gated: an implausible count only ranks the element down via atomPrior
      final int atoms = Math.max(1,
          Math.min(MAX_PLAUSIBLE_ATOMS, (int) Math.round(maxRatio / e.m2Rel())));
      final double defect = (medDelta - e.m2Delta()) / defectSigma;
      // stops a weak element (S/Si) claiming a strong halogen comb when a jittered sigma leaves the
      // defect unable to discriminate
      final double atomPrior = atoms <= ATOM_SOFT_CAP ? 1d : ATOM_SOFT_CAP / atoms;
      // RANKING, not a gate: Br needs a ~97 % M+2 per atom, so a weak comb ranks Br far below S/Si
      // without claiming Br is impossible
      final double reach = e.m2Rel() <= 0d ? 1d : Math.min(1d, maxRatio / e.m2Rel());
      double s = Math.exp(-defect * defect) * atomPrior * Math.max(reach, MIN_REACH_WEIGHT);

      // Si vs the defect-degenerate Cl: only Si carries a genuine M+1 (29Si), so its presence boosts
      // Si and damps the no-M+1 candidates (Cl, S), and its absence damps Si. Br is exempt, see
      // STRONG_ABUNDANCE. Ranking only - membership is already decided.
      final boolean bearsM1 = e.m1Delta() != null && e.m1Rel() >= SIGNIFICANT_M1_REL;
      if (bearsM1 && e.m2Rel() < STRONG_ABUNDANCE) {
        s *= m1Ratio >= SI_M1_MIN ? 1.3d : 0.7d;
      } else if (!bearsM1 && e.m2Rel() < STRONG_ABUNDANCE && m1Ratio >= SI_M1_MIN) {
        s *= 0.7d;
      }

      // the spacing match was only a coarse per-pair test against the raw tolerance; this median /
      // sigma test is what keeps a plain CHNO pattern (whose +2 peak is 13C+15N, many sigma from
      // every heavy defect) from reporting S/Si
      if (s < MIN_CONFIDENCE) {
        continue;
      }
      score.put(e.symbol(), s);
      counts.put(e.symbol(), new int[]{atoms, atoms});
    }

    if (score.isEmpty()) {
      return DetectedComposition.empty();
    }

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
   * POPULATION standard deviation, deliberately not {@link io.github.mzmine.util.MathUtils#calcStd}
   * (which is the sample n-1 form): the spacings are the whole population of observed pairs, and the
   * sample correction would inflate the defect sigma at the small pair counts that are the norm.
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
