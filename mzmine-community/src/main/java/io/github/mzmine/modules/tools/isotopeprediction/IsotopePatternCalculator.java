/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.tools.isotopeprediction;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.impl.MultiChargeStateIsotopePattern;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.formula.IsotopeContainer;
import org.openscience.cdk.formula.IsotopePatternGenerator;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

/**
 * The reason why we introduce this as a module, rather than simple utility class, is to remember
 * the parameter values.
 */
public class IsotopePatternCalculator implements MZmineModule {

  public static final double ELECTRON_MASS = 5.4857990943E-4;
  public static final double THIRTHEEN_C_DISTANCE = 1.003354831;

  private static final String MODULE_NAME = "Isotope pattern prediction.";

  public static IsotopePattern calculateIsotopePattern(String molecularFormula, double minAbundance,
      int charge, PolarityType polarity) {
    return calculateIsotopePattern(molecularFormula, minAbundance, charge, polarity, false);
  }

  public static IsotopePattern calculateIsotopePattern(String molecularFormula, double minAbundance,
      int charge, PolarityType polarity, boolean storeFormula) {
    return calculateIsotopePattern(molecularFormula, minAbundance, 0.00005f, charge, polarity,
        storeFormula);
  }

  public static IsotopePattern calculateIsotopePattern(String molecularFormula, double minAbundance,
      double mergeWidth, int charge, PolarityType polarity, boolean storeFormula) {

    IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
    molecularFormula = molecularFormula.replace(" ", "");
    IMolecularFormula cdkFormula = MolecularFormulaManipulator.getMolecularFormula(molecularFormula,
        builder);

    return calculateIsotopePattern(cdkFormula, minAbundance, mergeWidth, charge, polarity,
        storeFormula);
  }

  public static IsotopePattern calculateIsotopePattern(IMolecularFormula cdkFormula,
      double minAbundance, int charge, PolarityType polarity) {
    return calculateIsotopePattern(cdkFormula, minAbundance, charge, polarity, false);
  }

  public static IsotopePattern calculateIsotopePattern(IMolecularFormula cdkFormula,
      double minAbundance, int charge, PolarityType polarity, boolean storeFormula) {
    return calculateIsotopePattern(cdkFormula, minAbundance, 0.00005f, charge, polarity, false);
  }

  public static IsotopePattern calculateIsotopePattern(IMolecularFormula cdkFormula,
      double minAbundance, double mergeWidth, int charge, PolarityType polarity,
      boolean storeFormula) {
    // TODO: check if the formula is not too big (>100 of a single atom?).
    // if so, just cancel the prediction

    // Set the minimum abundance of isotope
    // TODO: in the CDK minAbundance is now called minIntensity and refers
    // to the relative intensity
    // in the isotope pattern, should change it here, too
    IsotopePatternGenerator generator = new IsotopePatternGenerator(minAbundance);
    generator.setMinResolution(mergeWidth);
    generator.setStoreFormulas(storeFormula);

    org.openscience.cdk.formula.IsotopePattern pattern = generator.getIsotopes(cdkFormula);

    int numOfIsotopes = pattern.getNumberOfIsotopes();

    DataPoint dataPoints[] = new DataPoint[numOfIsotopes];
    String isotopeComposition[] = new String[numOfIsotopes];
    // For each unit of charge, we have to add or remove a mass of a
    // single electron. If the charge is positive, we remove electron
    // mass. If the charge is negative, we add it.
    charge = Math.abs(charge);
    var electronMass = polarity.getSign() * -1 * charge * ELECTRON_MASS;

    for (int i = 0; i < numOfIsotopes; i++) {
      IsotopeContainer isotope = pattern.getIsotope(i);

      double mass = isotope.getMass() + electronMass;

      if (charge != 0) {
        mass /= charge;
      }

      double intensity = isotope.getIntensity();

      dataPoints[i] = new SimpleDataPoint(mass, intensity);

      if (storeFormula) {
        isotopeComposition[i] = formatCDKString(isotope.toString());
      }
    }

    String formulaString = MolecularFormulaManipulator.getString(cdkFormula);

    if (storeFormula) {
      return new SimpleIsotopePattern(dataPoints, charge, IsotopePatternStatus.PREDICTED,
          formulaString, isotopeComposition);
    } else {
      return new SimpleIsotopePattern(dataPoints, charge, IsotopePatternStatus.PREDICTED,
          formulaString);
    }
  }

  public static HashMap<Double, IsotopePattern> calculateIsotopePatternForResolutions(
      IMolecularFormula cdkFormula, double minAbundance, MZTolerance[] tolerances, int charge,
      PolarityType polarity, boolean storeFormula) {
    // TODO: check if the formula is not too big (>100 of a single atom?).
    // if so, just cancel the prediction

    // Set the minimum abundance of isotope
    // TODO: in the CDK minAbundance is now called minIntensity and refers
    // to the relative intensity
    // in the isotope pattern, should change it here, too
    HashMap<Double, IsotopePattern> calculatedIsotopePatternForResolutions = new HashMap<>();
    for (MZTolerance mzTolerance : tolerances) {
      calculatedIsotopePatternForResolutions.put(mzTolerance.getMzTolerance(),
          calculateIsotopePattern(cdkFormula, minAbundance, mzTolerance.getMzTolerance(), charge,
              polarity, storeFormula));
    }
    return calculatedIsotopePatternForResolutions;
  }


  public static IsotopePattern removeDataPointsBelowIntensity(IsotopePattern pattern,
      double minIntensity) {
    DataPoint[] dp = ScanUtils.extractDataPoints(pattern);
    for (int i = 0; i < pattern.getNumberOfDataPoints(); i++) {
      if (dp[i].getIntensity() < minIntensity) {
        dp[i] = null;
      }
    }

    ArrayList<DataPoint> newDP = new ArrayList<DataPoint>();
    ArrayList<String> newComp = new ArrayList<String>();
    for (int i = 0; i < dp.length; i++) {
      DataPoint p = dp[i];
      if (dp[i] != null) {
        newDP.add(p);
        if (pattern instanceof SimpleIsotopePattern iso) {
          newComp.add(iso.getIsotopeComposition(i));
        }
      }
    }

    return new SimpleIsotopePattern(newDP.toArray(new DataPoint[0]), pattern.getCharge(),
        pattern.getStatus(), pattern.getDescription(), newComp.toArray(new String[0]));
  }

  /**
   * Returns same isotope pattern (same ratios between isotope intensities) with maximum intensity
   * normalized to 1
   */
  public static IsotopePattern normalizeIsotopePattern(IsotopePattern pattern) {
    return normalizeIsotopePattern(pattern, 1);
  }

  /**
   * Returns same isotope pattern (same ratios between isotope intensities) with maximum intensity
   * normalized to given intensity
   */
  public static IsotopePattern normalizeIsotopePattern(IsotopePattern pattern,
      double normalizedValue) {

    DataPoint[] newDataPoints = ScanUtils.normalizeSpectrum(pattern, normalizedValue);
    if (newDataPoints.length == 0) {
      return pattern;
    }

    if (pattern instanceof SimpleIsotopePattern simple
        && ((SimpleIsotopePattern) pattern).getIsotopeCompositions() != null) {
      return new SimpleIsotopePattern(newDataPoints, pattern.getCharge(), pattern.getStatus(),
          pattern.getDescription(), simple.getIsotopeCompositions());
    } else if (pattern instanceof MultiChargeStateIsotopePattern multi) {
      // normalize all patterns for all charge states
      final List<IsotopePattern> patternsForCharges = multi.getPatterns().stream()
          .map(p -> normalizeIsotopePattern(p, normalizedValue)).toList();
      return new MultiChargeStateIsotopePattern(patternsForCharges);
    } else {
      return new SimpleIsotopePattern(newDataPoints, pattern.getCharge(), pattern.getStatus(),
          pattern.getDescription());
    }
  }

  public static IsotopePattern showIsotopePredictionDialog(Window parent,
      boolean valueCheckRequired) {

    ParameterSet parameters = MZmineCore.getConfiguration()
        .getModuleParameters(IsotopePatternCalculator.class);
    ExitCode exitCode = parameters.showSetupDialog(valueCheckRequired);
    if (exitCode != ExitCode.OK) {
      return null;
    }

    String formula = parameters.getParameter(IsotopePatternCalculatorParameters.formula).getValue();
    int charge = parameters.getParameter(IsotopePatternCalculatorParameters.charge).getValue();
    PolarityType polarity = parameters.getParameter(IsotopePatternCalculatorParameters.polarity)
        .getValue();
    double minAbundance = parameters.getParameter(IsotopePatternCalculatorParameters.minAbundance)
        .getValue();

    try {
      IsotopePattern predictedPattern = calculateIsotopePattern(formula, minAbundance, charge,
          polarity);
      return predictedPattern;
    } catch (Exception e) {
      MZmineCore.getDesktop().displayException(e);
    }

    return null;

  }

  static String formatCDKString(String cdkString) {
    int startIndex = cdkString.lastIndexOf("MF=");
    int endIndex = cdkString.length() - 1;

    return cdkString.substring(startIndex + 3, endIndex);
  }

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return IsotopePatternCalculatorParameters.class;
  }
}
