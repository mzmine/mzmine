/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.HashMap;
import org.openscience.cdk.formula.IsotopePatternGenerator;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class IsotopePatternCalculator {


  private final IChemObjectBuilder builder;
  private final IsotopePatternGenerator generator;
  HashMap<Integer, IsotopePattern> calculatedIsotopePattern = new HashMap<>();
  HashMap<String, CdkIsotopePattern> calculatedPatterns = new HashMap<>();

  public IsotopePatternCalculator(final double minPatternIntensity, final MZTolerance mzTolerance) {
    builder = SilentChemObjectBuilder.getInstance();
    generator = new IsotopePatternGenerator(minPatternIntensity);
    generator.setMinResolution(mzTolerance.getMzTolerance());
  }

  /**
   * @param row
   * @param formula
   * @param charge
   * @return theoretical isotope pattern of the given chemical formula and charge with isotope
   * signals above the minPatternIntensity
   */

  public IsotopePattern calculateIsotopePattern(FeatureListRow row, String formula, int charge) {
    var pattern = calculatedPatterns.computeIfAbsent(formula, this::predictIsotopePattern);
    var dataPoints = pattern.translateMajorIsotopeTo(row.getAverageMZ(),
        row.getBestFeature().getHeight(), charge);
    var chargedPattern = new SimpleIsotopePattern(dataPoints, charge,
        IsotopePatternStatus.PREDICTED, "");
    calculatedIsotopePattern.put(row.getID(), chargedPattern);
    return chargedPattern;
  }


  private CdkIsotopePattern predictIsotopePattern(final String formula) {
    var elementFormula = MolecularFormulaManipulator.getMolecularFormula(formula, builder);
    return new CdkIsotopePattern(generator.getIsotopes(elementFormula));
  }


}
