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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.IOException;
import java.util.HashMap;
import org.openscience.cdk.formula.IsotopePatternGenerator;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class IsotopePatternCalculator {



  HashMap<Integer, IsotopePattern> calculatedIsotopePattern = new HashMap<>();
  private IChemObjectBuilder builder = null;
  private IChemObjectBuilder builder2 = null;
  HashMap<String, org.openscience.cdk.formula.IsotopePattern> calculatedPatterns = new HashMap<>();
  HashMap<String, Double> majorIsotopeOfPattern = new HashMap<>();
  double majorIntensity;
  DataPoint[] calculatedPatternDPs;

  /**
   *
   * @param row
   * @param formula
   * @param minPatternIntensity
   * @param mzTolerance
   * @param charge
   * @return theoretical isotope pattern of the given chemical formula and charge with isotope signals
   * above the minPatternIntensity
   */

  public IsotopePattern calculateIsotopePattern (FeatureListRow row, String formula, double minPatternIntensity,
      MZTolerance mzTolerance, int charge) {
    try {
      calculatedPatternDPs = calculateIsotopePatternDataPoints(row, formula,
          minPatternIntensity, mzTolerance, charge);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    calculatedIsotopePattern.put(row.getID(),
        new SimpleIsotopePattern(calculatedPatternDPs, charge, IsotopePatternStatus.DETECTED,
            ""));
    return calculatedIsotopePattern.get(row.getID());
  }

  /**
   *
   * @param row
   * @param formula
   * @param minPatternIntensity
   * @param mzTolerance
   * @param charge
   * @return data points of the theoretical isotope pattern
   * @throws IOException
   */

  public DataPoint[] calculateIsotopePatternDataPoints(FeatureListRow row, String formula,
      double minPatternIntensity, MZTolerance mzTolerance, int charge) throws IOException {
    if (!calculatedPatterns.containsKey(formula)) {
      builder = SilentChemObjectBuilder.getInstance();
      IMolecularFormula elementFormula = MolecularFormulaManipulator.getMolecularFormula(formula,
          builder);
      org.openscience.cdk.formula.IsotopePatternGenerator generator = new IsotopePatternGenerator(
          minPatternIntensity);
      generator.setMinResolution(mzTolerance.getMzTolerance());
      calculatedPatterns.put(formula, generator.getIsotopes(elementFormula));
      builder2 = SilentChemObjectBuilder.getInstance();
      IMolecularFormula majorElementFormula = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(
          formula, builder2);
      majorIsotopeOfPattern.put(formula, MolecularFormulaManipulator.getMass(majorElementFormula));
    }
    org.openscience.cdk.formula.IsotopePattern pattern = calculatedPatterns.get(formula);
    double[] massDiff = new double[pattern.getNumberOfIsotopes()];
    pattern.setCharge(charge);
    for (int i = 0; i < pattern.getNumberOfIsotopes(); i++) {
      massDiff[i] = (pattern.getIsotope(i).getMass() - majorIsotopeOfPattern.get(formula)) / charge;
    }

    for (int i = 0; i < pattern.getNumberOfIsotopes(); i++) {
      if (mzTolerance.checkWithinTolerance(pattern.getIsotope(i).getMass(),
          majorIsotopeOfPattern.get(formula))) {
        majorIntensity = pattern.getIsotope(i).getIntensity();
      }
    }
    DataPoint monoIsotope = new SimpleDataPoint(row.getAverageMZ(), row.getSumIntensity());

    DataPoint[] dp = new DataPoint[pattern.getNumberOfIsotopes()];
    for (int j = 0; j < pattern.getNumberOfIsotopes(); j++) {
      double calculatedMass = monoIsotope.getMZ() + massDiff[j];
      double calculatedIntensity =
          (pattern.getIsotope(j).getIntensity() / majorIntensity) * row.getBestFeature()
              .getHeight();
      dp[j] = new SimpleDataPoint(calculatedMass, calculatedIntensity);
    }
    return dp;
  }


}
