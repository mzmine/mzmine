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
 * Carbon-averagine envelope model. Estimates the carbon count from the searched neutral mass and
 * models the 13C isotope envelope (Poisson or binomial). The {@code expected} intensities are the
 * pure-carbon envelope. The {@code upperBound} additionally convolves heavy-isotope contributions
 * (S/Cl/Br ... at M+2, M+4, ...) derived from the user's element list, so halogenated patterns are
 * not penalized while implausibly large signals are still flagged.
 */
public class CarbonAveragineEnvelopeModel implements EnvelopeModel {

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

  public CarbonAveragineEnvelopeModel(@NotNull final ParameterSet params,
      @NotNull final EnvelopeContext ctx) {
    this.carbonPerDaltonMin = params.getValue(CarbonAveragineEnvelopeParameters.carbonPerDaltonMin);
    this.carbonPerDaltonTypical = params.getValue(
        CarbonAveragineEnvelopeParameters.carbonPerDaltonTypical);
    this.carbonPerDaltonMax = params.getValue(CarbonAveragineEnvelopeParameters.carbonPerDaltonMax);
    this.minRelIntensity = params.getValue(CarbonAveragineEnvelopeParameters.minRelIntensity);
    this.usePoisson = params.getValue(CarbonAveragineEnvelopeParameters.usePoissonNotBinomial);
    this.userHeavies = extractHeavyContributions(ctx.elements());
  }

  @Override
  public @Nullable double[] expectedM1RatioBounds(final double observedMz, final int charge,
      @NotNull final PolarityType polarity) {
    double neutralMass = observedMz * charge - charge * PROTON_MASS * polarity.getSign();
    if (neutralMass <= 0) {
      neutralMass = observedMz * charge;
    }
    // M+1/M ratio for a pure-carbon Poisson envelope is lambda = nC * P_13C
    final double low = Math.max(0, (int) Math.round(neutralMass * carbonPerDaltonMin)) * P_13C;
    final double high = Math.max(0, (int) Math.round(neutralMass * carbonPerDaltonMax)) * P_13C;
    return new double[]{low, high};
  }

  /**
   * @param elements the allowed elements
   * @return the dominant heavy isotope (offset step in Da + fractional abundance) of every element
   * except C and H, keyed by element symbol, used to widen the upper bound.
   */
  private static LinkedHashMap<String, HeavyContribution> extractHeavyContributions(
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
   * @param symbol the element symbol
   * @return the dominant heavy isotope (offset step in Da + fractional abundance) of the element,
   * or null for C/H (13C is modeled by the carbon envelope, 2H is negligible) or when the element
   * has no heavy isotope with a step &gt;= 1.
   */
  private static @Nullable HeavyContribution heavyContributionFor(@NotNull final String symbol) {
    // decision: 13C is modeled by the carbon envelope, 2H is negligible
    if ("C".equals(symbol) || "H".equals(symbol)) {
      return null;
    }
    Isotope dominant = null;
    for (final Isotope iso : IsotopesUtils.getIsotopeRecord(symbol)) {
      final int step = (int) Math.round(iso.deltaMass());
      if (step < 1) {
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
   * @param neutralMass the searched neutral mass
   * @return the crude, mass-proportional estimate of the number of heavy atoms per element
   * (capped), used when a detected atom count is not available.
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
    double neutralMass = observedMz * charge - charge * PROTON_MASS * polarity.getSign();
    if (neutralMass <= 0) {
      neutralMass = observedMz * charge;
    }

    final int nCtypical = Math.max(0, (int) Math.round(neutralMass * carbonPerDaltonTypical));
    final int nCmax = Math.max(0, (int) Math.round(neutralMass * carbonPerDaltonMax));

    final double[] carbonExpected = carbonDistribution(nCtypical);
    final double[] carbonUpper = carbonDistribution(nCmax);

    // assemble per-element heavy atom counts: user heavies at the crude estimate, then detected
    // counts override/extend them.
    final LinkedHashMap<String, Integer> heavyCounts = new LinkedHashMap<>();
    if (includeUserHeavies) {
      final int crude = crudeHeavyAtomCount(neutralMass);
      for (final String sym : userHeavies.keySet()) {
        heavyCounts.put(sym, crude);
      }
    }
    if (detectedHeavyCounts != null) {
      for (final Map.Entry<String, Integer> entry : detectedHeavyCounts.entrySet()) {
        final Integer count = entry.getValue();
        if (count != null && count > 0) {
          heavyCounts.put(entry.getKey(), count);
        }
      }
    }

    // convolve heavy-isotope contributions into the upper bound only
    double[] heavyDist = new double[]{1d};
    for (final Map.Entry<String, Integer> entry : heavyCounts.entrySet()) {
      final String sym = entry.getKey();
      // prefer the cached user contribution, else resolve on the fly for a detected-only element
      HeavyContribution hc = userHeavies.get(sym);
      if (hc == null) {
        hc = heavyContributionFor(sym);
      }
      if (hc == null) {
        continue;
      }
      final double[] elemDist = steppedBinomial(entry.getValue(), hc.abundance(), hc.step());
      heavyDist = convolve(heavyDist, elemDist);
    }
    final double[] upperRaw = convolve(carbonUpper, heavyDist);

    final double[] expected = normalizeToMax(carbonExpected);
    final double[] upperBound = normalizeToMax(upperRaw);
    // the upper bound must dominate the expected intensity at every offset
    for (int i = 0; i < upperBound.length && i < expected.length; i++) {
      upperBound[i] = Math.max(upperBound[i], expected[i]);
    }

    final double spacingDa = IsotopePatternCalculator.THIRTHEEN_C_DISTANCE / charge;
    return trim(expected, upperBound, spacingDa, charge);
  }

  private double[] carbonDistribution(final int nCarbon) {
    return usePoisson ? poisson(nCarbon * P_13C) : binomial(nCarbon, P_13C);
  }

  private double[] poisson(final double lambda) {
    final double[] p = new double[CAP + 1];
    p[0] = Math.exp(-lambda);
    for (int k = 1; k <= CAP; k++) {
      p[k] = p[k - 1] * lambda / k;
    }
    return p;
  }

  private double[] binomial(final int n, final double prob) {
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
   * Binomial distribution of {@code n} heavy atoms whose isotope sits {@code step} Da above the
   * main isotope, mapped onto the Da-offset grid (peaks at 0, step, 2*step, ...).
   */
  private double[] steppedBinomial(final int n, final double abundance, final int step) {
    final double[] bin = binomial(n, abundance);
    final double[] dist = new double[CAP + 1];
    for (int k = 0; k * step <= CAP && k <= n; k++) {
      dist[k * step] += bin[k];
    }
    return dist;
  }

  private double[] convolve(final double[] a, final double[] b) {
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

  private double[] normalizeToMax(final double[] arr) {
    double max = 0d;
    for (final double v : arr) {
      if (v > max) {
        max = v;
      }
    }
    if (max <= 0d) {
      final double[] r = new double[]{1d};
      return r;
    }
    final double[] r = new double[arr.length];
    for (int i = 0; i < arr.length; i++) {
      r[i] = arr[i] / max;
    }
    return r;
  }

  private IsotopeEnvelope trim(final double[] expected, final double[] upperBound,
      final double spacingDa, final int charge) {
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
