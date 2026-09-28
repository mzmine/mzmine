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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeContext;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeEnvelope;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.Isotope;
import io.github.mzmine.util.IsotopesUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.Element;

/**
 * Carbon envelope model. Estimates the carbon count from the searched neutral mass via a
 * configurable carbon-per-Dalton ratio and models the 13C isotope envelope (Poisson or binomial).
 * The {@code expected} intensities are the pure-carbon envelope. The {@code upperBound}
 * additionally convolves heavy-isotope contributions (S/Cl/Br ... at M+2, M+4, ...) derived from
 * the user's element list, so halogenated patterns are not penalized while implausibly large
 * signals are still flagged.
 * <p>
 * This is NOT the averagine model: no average amino-acid residue formula is assumed anywhere. Only
 * the carbon-per-mass ratio matters, and its min/typical/max bounds are user parameters wide enough
 * to cover small molecules, halogen-rich compounds and peptides alike.
 */
public class CarbonEnvelopeModel implements EnvelopeModel {

  // natural abundance fraction of 13C (CDK 0-100 scale -> here as fraction)
  private static final double P_13C = 0.0107;
  private static final double PROTON_MASS = 1.007276466;
  // generous heuristics for the unknown count of heavy-isotope atoms
  private static final double HEAVY_MASS_PER_ATOM = 200d;
  private static final int MAX_HEAVY_ATOMS = 8;
  private static final int CAP = 30;

  private final double carbonPerDaltonMin;
  private final double carbonPerDaltonTypical;
  private final double carbonPerDaltonMax;
  private final double minRelIntensity;
  private final boolean usePoisson;
  // user-configured heavy elements keyed by element symbol, used for the crude upper-bound estimate
  private final LinkedHashMap<String, HeavyContribution> userHeavies;

  public CarbonEnvelopeModel(@NotNull final ParameterSet params,
      @NotNull final EnvelopeContext ctx) {
    this.carbonPerDaltonMin = params.getValue(CarbonEnvelopeParameters.carbonPerDaltonMin);
    this.carbonPerDaltonTypical = params.getValue(CarbonEnvelopeParameters.carbonPerDaltonTypical);
    this.carbonPerDaltonMax = params.getValue(CarbonEnvelopeParameters.carbonPerDaltonMax);
    this.minRelIntensity = params.getValue(CarbonEnvelopeParameters.minRelIntensity);
    this.usePoisson = params.getValue(CarbonEnvelopeParameters.usePoissonNotBinomial);
    this.userHeavies = extractHeavyContributions(ctx.elements());
  }

  @Override
  public double @NotNull [] expectedM1RatioBounds(final double observedMz, final int charge,
      @NotNull final PolarityType polarity) {
    final double neutralMass = neutralMass(observedMz, charge, polarity);
    // M+1/M ratio for a pure-carbon Poisson envelope is lambda = nC * P_13C
    final double low = Math.max(0, (int) Math.round(neutralMass * carbonPerDaltonMin)) * P_13C;
    final double high = Math.max(0, (int) Math.round(neutralMass * carbonPerDaltonMax)) * P_13C;
    return new double[]{low, high};
  }

  /**
   * @return the neutral mass, falling back to {@code mz * z} when the ionization correction would
   * make it non-positive.
   */
  private static double neutralMass(final double observedMz, final int charge,
      @NotNull final PolarityType polarity) {
    final double neutralMass = observedMz * charge - charge * PROTON_MASS * polarity.getSign();
    return neutralMass > 0 ? neutralMass : observedMz * charge;
  }

  /**
   * @param elements the allowed elements
   * @return the dominant heavy isotope (offset step in Da + fractional abundance) of every element
   * except C and H, keyed by element symbol, used to widen the upper bound.
   */
  private static @NotNull LinkedHashMap<String, HeavyContribution> extractHeavyContributions(
      @NotNull final List<Element> elements) {
    final LinkedHashMap<String, HeavyContribution> result = new LinkedHashMap<>();
    for (final Element element : elements) {
      final String symbol = element.getSymbol();
      final HeavyContribution hc = heavyContributionFor(symbol);
      if (hc != null) {
        result.put(symbol, hc);
      }
    }
    return result;
  }

  /**
   * @return the element's dominant heavy isotope as an offset step plus a fractional abundance, or
   * null for C/H and for elements without a heavy isotope at least one nominal mass up.
   */
  private static @Nullable HeavyContribution heavyContributionFor(@NotNull final String symbol) {
    // decision: 13C is modeled by the carbon envelope, 2H is negligible
    if ("C".equals(symbol) || "H".equals(symbol)) {
      return null;
    }
    Isotope dominant = null;
    for (final Isotope iso : IsotopesUtils.getIsotopeRecord(symbol)) {
      if (Math.round(iso.deltaMass()) < 1) {
        continue;
      }
      if (dominant == null || iso.relativeIntensity() > dominant.relativeIntensity()) {
        dominant = iso;
      }
    }
    if (dominant == null) {
      return null;
    }
    final int step = (int) Math.round(dominant.deltaMass());
    final double rel = dominant.relativeIntensity();
    // relativeIntensity is the ratio to the main isotope -> convert to a fractional abundance
    final double abundance = rel / (1d + rel);
    return new HeavyContribution(step, abundance);
  }

  /**
   * @return the stepped binomial contribution of {@code atoms} atoms of one element, or null when
   * it has no usable heavy isotope.
   */
  private double @Nullable [] heavyDistributionFor(@NotNull final String symbol, final int atoms) {
    // prefer the cached user contribution, else resolve on the fly for a detected-only element
    HeavyContribution hc = userHeavies.get(symbol);
    if (hc == null) {
      hc = heavyContributionFor(symbol);
    }
    return hc == null ? null : steppedBinomial(atoms, hc.abundance(), hc.step());
  }

  /**
   * @return the element-wise maximum of two distributions, length = the longer one.
   */
  private static double @NotNull [] maxOf(final double @NotNull [] a, final double @NotNull [] b) {
    final double[] out = new double[Math.max(a.length, b.length)];
    for (int i = 0; i < out.length; i++) {
      final double av = i < a.length ? a[i] : 0d;
      final double bv = i < b.length ? b[i] : 0d;
      out[i] = Math.max(av, bv);
    }
    return out;
  }

  /**
   * @return a crude, capped mass-proportional estimate of the heavy atoms per element, used when no
   * detected atom count is available.
   */
  private int crudeHeavyAtomCount(final double neutralMass) {
    return Math.min(MAX_HEAVY_ATOMS,
        Math.max(1, (int) Math.round(neutralMass / HEAVY_MASS_PER_ATOM)));
  }

  @Override
  public @NotNull IsotopeEnvelope buildEnvelope(final double observedMz, final int charge,
      @NotNull final PolarityType polarity) {
    return buildEnvelope(observedMz, charge, polarity, null, true);
  }

  @Override
  public @NotNull IsotopeEnvelope buildEnvelope(final double observedMz, final int charge,
      @NotNull final PolarityType polarity,
      @Nullable final Map<String, Integer> detectedHeavyCounts, final boolean includeUserHeavies) {
    final double neutralMass = neutralMass(observedMz, charge, polarity);

    final int nCtypical = Math.max(0, (int) Math.round(neutralMass * carbonPerDaltonTypical));
    final int nCmax = Math.max(0, (int) Math.round(neutralMass * carbonPerDaltonMax));

    final double[] carbonExpected = carbonDistribution(nCtypical);
    final double[] carbonUpper = carbonDistribution(nCmax);

    // the user's elements are DECLARED present together, so their contributions convolve
    final LinkedHashMap<String, Integer> coPresent = new LinkedHashMap<>();
    if (includeUserHeavies) {
      final int crude = crudeHeavyAtomCount(neutralMass);
      for (final String sym : userHeavies.keySet()) {
        coPresent.put(sym, crude);
      }
    }
    double[] coPresentDist = new double[]{1d};
    for (final Map.Entry<String, Integer> entry : coPresent.entrySet()) {
      final double[] elemDist = heavyDistributionFor(entry.getKey(), entry.getValue());
      if (elemDist != null) {
        coPresentDist = convolve(coPresentDist, elemDist);
      }
    }

    // decision: DETECTED elements are mutually exclusive ALTERNATIVES, so their bounds combine by
    // envelope-wise MAXIMUM rather than convolution. The detector reports every element it cannot
    // rule out, and convolving would bound the pattern as if the molecule held all of them at once -
    // inflating it multiplicatively, which widens the termination and flattens intensityAgreement.
    // With no detected counts this reduces to the co-present path unchanged.
    double[] upperRaw = convolve(carbonUpper, coPresentDist);
    if (detectedHeavyCounts != null && !detectedHeavyCounts.isEmpty()) {
      double[] best = null;
      for (final Map.Entry<String, Integer> entry : detectedHeavyCounts.entrySet()) {
        final Integer count = entry.getValue();
        if (count == null || count <= 0) {
          continue;
        }
        final double[] elemDist = heavyDistributionFor(entry.getKey(), count);
        if (elemDist == null) {
          continue;
        }
        final double[] candidate = convolve(upperRaw, elemDist);
        best = best == null ? candidate : maxOf(best, candidate);
      }
      if (best != null) {
        upperRaw = best;
      }
    }

    final double[] expected = normalizeToMax(carbonExpected);
    final double[] upperBound = normalizeToMax(upperRaw);
    for (int i = 0; i < upperBound.length && i < expected.length; i++) {
      upperBound[i] = Math.max(upperBound[i], expected[i]);
    }

    final double spacingDa = IsotopePatternCalculator.THIRTHEEN_C_DISTANCE / charge;
    return trim(expected, upperBound, spacingDa, charge);
  }

  private double @NotNull [] carbonDistribution(final int nCarbon) {
    return usePoisson ? poisson(nCarbon * P_13C) : binomial(nCarbon, P_13C);
  }

  private static double @NotNull [] poisson(final double lambda) {
    final double[] p = new double[CAP + 1];
    p[0] = Math.exp(-lambda);
    for (int k = 1; k <= CAP; k++) {
      p[k] = p[k - 1] * lambda / k;
    }
    return p;
  }

  private static double @NotNull [] binomial(final int n, final double prob) {
    final double[] p = new double[CAP + 1];
    if (n <= 0 || prob <= 0) {
      p[0] = 1d;
      return p;
    }
    final double q = 1d - prob;
    p[0] = Math.pow(q, n);
    final int kMax = Math.min(n, CAP);
    for (int k = 1; k <= kMax; k++) {
      p[k] = p[k - 1] * ((double) (n - k + 1) / k) * (prob / q);
    }
    return p;
  }

  /**
   * {@code n} heavy atoms whose isotope sits {@code step} Da up, on the Da-offset grid.
   */
  private static double @NotNull [] steppedBinomial(final int n, final double abundance,
      final int step) {
    final double[] bin = binomial(n, abundance);
    final double[] dist = new double[CAP + 1];
    for (int k = 0; k * step <= CAP && k <= n; k++) {
      dist[k * step] += bin[k];
    }
    return dist;
  }

  private static double @NotNull [] convolve(final double @NotNull [] a, final double @NotNull [] b) {
    final double[] r = new double[CAP + 1];
    for (int i = 0; i < a.length && i <= CAP; i++) {
      if (a[i] == 0d) {
        continue;
      }
      for (int j = 0; j < b.length && i + j <= CAP; j++) {
        r[i + j] += a[i] * b[j];
      }
    }
    return r;
  }

  private static double @NotNull [] normalizeToMax(final double @NotNull [] arr) {
    double max = 0d;
    for (final double v : arr) {
      if (v > max) {
        max = v;
      }
    }
    if (max <= 0d) {
      return new double[]{1d};
    }
    final double[] r = new double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      r[i] = arr[i] / max;
    }
    return r;
  }

  private @NotNull IsotopeEnvelope trim(final double @NotNull [] expected,
      final double @NotNull [] upperBound, final double spacingDa, final int charge) {
    int last = 0;
    final int len = Math.max(expected.length, upperBound.length);
    for (int i = 0; i < len; i++) {
      final double e = i < expected.length ? expected[i] : 0d;
      final double u = i < upperBound.length ? upperBound[i] : 0d;
      if (e >= minRelIntensity || u >= minRelIntensity) {
        last = i;
      }
    }
    final double[] e = new double[last + 1];
    final double[] u = new double[last + 1];
    for (int i = 0; i <= last; i++) {
      e[i] = i < expected.length ? expected[i] : 0d;
      u[i] = i < upperBound.length ? upperBound[i] : 0d;
    }
    return new IsotopeEnvelope(e, u, spacingDa, charge);
  }

  private record HeavyContribution(int step, double abundance) {

  }
}
