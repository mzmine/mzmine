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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder.formula;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeContext;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.EnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.engine.IsotopeEnvelope;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeModel;
import io.github.mzmine.modules.dataprocessing.filter_isotopefinder.signal.CarbonAveragineEnvelopeParameters;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.formula.MolecularFormulaGenerator;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

/**
 * Formula-prediction envelope model. Enumerates candidate formulas for the searched neutral mass
 * and unions their CDK-predicted isotope patterns: the per-offset {@code upperBound} is the maximum
 * and {@code expected} is the mean across candidates. Above
 * {@link FormulaEnvelopeParameters#maxNeutralMassCutoff} or when no formula is found, it delegates
 * to a carbon-averagine fallback.
 */
public class FormulaEnvelopeModel implements EnvelopeModel {

  private static final Logger logger = Logger.getLogger(FormulaEnvelopeModel.class.getName());
  private static final double PROTON_MASS = 1.007276466;
  private static final int CAP = 30;

  private final MolecularFormulaRange elementCounts;
  private final MZTolerance formulaMzTolerance;
  private final double maxNeutralMassCutoff;
  private final int maxCandidateFormulas;
  private final double minRelIntensity;
  private final CarbonAveragineEnvelopeModel fallback;

  public FormulaEnvelopeModel(@NotNull final ParameterSet params,
      @NotNull final EnvelopeContext ctx) {
    this.elementCounts = params.getValue(FormulaEnvelopeParameters.elements);
    this.formulaMzTolerance = params.getValue(FormulaEnvelopeParameters.formulaMzTolerance);
    this.maxNeutralMassCutoff = params.getValue(FormulaEnvelopeParameters.maxNeutralMassCutoff);
    this.maxCandidateFormulas = params.getValue(FormulaEnvelopeParameters.maxCandidateFormulas);
    this.minRelIntensity = params.getValue(FormulaEnvelopeParameters.minRelIntensity);
    // composition (not inheritance): reuse the averagine model as a guardrail fallback
    this.fallback = new CarbonAveragineEnvelopeModel(
        CarbonAveragineEnvelopeParameters.createDefault(), ctx);
  }

  @Override
  public @NotNull IsotopeEnvelope buildEnvelope(final double observedMz, final int charge,
      @NotNull final PolarityType polarity) {
    double neutralMass = observedMz * charge - charge * PROTON_MASS * polarity.getSign();
    if (neutralMass <= 0) {
      neutralMass = observedMz * charge;
    }

    // guardrail: large masses explode the formula enumeration -> use the averagine fallback
    if (neutralMass > maxNeutralMassCutoff) {
      return fallback.buildEnvelope(observedMz, charge, polarity);
    }

    final double spacingDa = IsotopePatternCalculator.THIRTHEEN_C_DISTANCE / charge;
    final double[] sumExpected = new double[CAP + 1];
    final double[] upper = new double[CAP + 1];
    int formulaCount = 0;

    try {
      final IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
      final Range<Double> massRange = formulaMzTolerance.getToleranceRange(neutralMass);
      final MolecularFormulaGenerator generator = new MolecularFormulaGenerator(builder,
          massRange.lowerEndpoint(), massRange.upperEndpoint(), elementCounts);

      IMolecularFormula formula;
      while ((formula = generator.getNextFormula()) != null) {
        if (formulaCount >= maxCandidateFormulas) {
          break;
        }
        final IsotopePattern pattern = IsotopePatternCalculator.calculateIsotopePattern(formula,
            minRelIntensity, charge, polarity);
        if (pattern == null || pattern.getNumberOfDataPoints() == 0) {
          continue;
        }
        accumulate(pattern, spacingDa, sumExpected, upper);
        formulaCount++;
      }
    } catch (Exception | NoClassDefFoundError ex) {
      logger.log(Level.WARNING,
          "Formula enumeration failed, using averagine fallback: " + ex.getMessage(), ex);
      return fallback.buildEnvelope(observedMz, charge, polarity);
    }

    if (formulaCount == 0) {
      // nothing enumerated (e.g. too tight element ranges) -> fallback
      return fallback.buildEnvelope(observedMz, charge, polarity);
    }

    final double[] expected = new double[CAP + 1];
    for (int i = 0; i <= CAP; i++) {
      expected[i] = sumExpected[i] / formulaCount;
    }
    normalizeToMax(expected);
    normalizeToMax(upper);
    for (int i = 0; i <= CAP; i++) {
      upper[i] = Math.max(upper[i], expected[i]);
    }
    return trim(expected, upper, spacingDa, charge);
  }

  /**
   * Bin one predicted pattern (normalized to base peak = 1) onto integer Da offsets relative to its
   * monoisotopic (lowest) m/z, updating the running sum (for the mean) and the per-offset maximum.
   */
  private void accumulate(@NotNull final IsotopePattern pattern, final double spacingDa,
      final double[] sumExpected, final double[] upper) {
    final int n = pattern.getNumberOfDataPoints();
    double minMz = Double.MAX_VALUE;
    double maxIntensity = 0d;
    for (int i = 0; i < n; i++) {
      minMz = Math.min(minMz, pattern.getMzValue(i));
      maxIntensity = Math.max(maxIntensity, pattern.getIntensityValue(i));
    }
    if (maxIntensity <= 0d) {
      return;
    }
    final double[] offsetIntensity = new double[CAP + 1];
    for (int i = 0; i < n; i++) {
      final int offset = (int) Math.round((pattern.getMzValue(i) - minMz) / spacingDa);
      if (offset < 0 || offset > CAP) {
        continue;
      }
      // collapse fine structure within a nominal offset by summing
      offsetIntensity[offset] += pattern.getIntensityValue(i) / maxIntensity;
    }
    for (int o = 0; o <= CAP; o++) {
      sumExpected[o] += offsetIntensity[o];
      upper[o] = Math.max(upper[o], offsetIntensity[o]);
    }
  }

  private void normalizeToMax(final double[] arr) {
    double max = 0d;
    for (final double v : arr) {
      if (v > max) {
        max = v;
      }
    }
    if (max <= 0d) {
      return;
    }
    for (int i = 0; i < arr.length; i++) {
      arr[i] /= max;
    }
  }

  private IsotopeEnvelope trim(final double[] expected, final double[] upperBound,
      final double spacingDa, final int charge) {
    int last = 0;
    for (int i = 0; i <= CAP; i++) {
      if (expected[i] >= minRelIntensity || upperBound[i] >= minRelIntensity) {
        last = i;
      }
    }
    final double[] e = new double[last + 1];
    final double[] u = new double[last + 1];
    System.arraycopy(expected, 0, e, 0, last + 1);
    System.arraycopy(upperBound, 0, u, 0, last + 1);
    return new IsotopeEnvelope(e, u, spacingDa, charge);
  }
}
