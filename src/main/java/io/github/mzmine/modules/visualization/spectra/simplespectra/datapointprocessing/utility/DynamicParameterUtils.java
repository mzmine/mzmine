/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.utility;

import java.util.logging.Logger;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;

import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPIsotopePatternResult;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult.ResultType;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.IsotopePatternUtils;

/**
 * Used to calculate parameter values based on the results of data point processing modules.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DynamicParameterUtils {

  private static Logger logger = Logger.getLogger(DynamicParameterUtils.class.getName());

  private static float lowerElementBoundaryPercentage = 0.75f;
  private static float upperElementBoundaryPercentage = 1.5f;

  public static float getLowerElementBoundaryPercentage() {
    return lowerElementBoundaryPercentage;
  }

  public static float getUpperElementBoundaryPercentage() {
    return upperElementBoundaryPercentage;
  }

  public static void setLowerElementBoundaryPercentage(float lowerElementBoundaryPercentage) {
    DynamicParameterUtils.lowerElementBoundaryPercentage = lowerElementBoundaryPercentage;
  }

  public static void setUpperElementBoundaryPercentage(float upperElementBoundaryPercentage) {
    DynamicParameterUtils.upperElementBoundaryPercentage = upperElementBoundaryPercentage;
  }

  /**
   * Creates an ElementParameter based on the previous processing results. If no results were
   * detected, the default value is returned. Upper and lower boundaries are chosen according to
   * lowerElementBoundaryPercentage and upperElementBoundaryPercentage values of this utility class.
   * These values can be set via {@link #setLowerElementBoundaryPercentage} and
   * {@link #setUpperElementBoundaryPercentage}. The elements contained in
   * 
   * @param dp The data point to build a parameter for.
   * @param def The default set of parameters.
   * @return The built ElementsCompositionRangeParameter
   */
  public static MolecularFormulaRange buildFormulaRangeOnIsotopePatternResults(
      ProcessedDataPoint dp, MolecularFormulaRange def) {

    DPPIsotopePatternResult result =
        (DPPIsotopePatternResult) dp.getFirstResultByType(ResultType.ISOTOPEPATTERN);
    if (result == null)
      return def;

    if (!(result.getValue() instanceof SimpleIsotopePattern))
      return def;

    SimpleIsotopePattern pattern = (SimpleIsotopePattern) result.getValue();
    String form = IsotopePatternUtils.makePatternSuggestion(pattern.getIsotopeCompositions());

    MolecularFormulaRange range = new MolecularFormulaRange();

    IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula(form);
    if (formula == null) {
      logger.finest("could not generate formula for m/z " + dp.getMZ() + " " + form);
      return def;
    }

    for (IIsotope isotope : def.isotopes())
      range.addIsotope(isotope, def.getIsotopeCountMin(isotope), def.getIsotopeCountMax(isotope));

    for (IIsotope isotope : formula.isotopes()) {
      if (range.contains(isotope))
        continue;

      int count = formula.getIsotopeCount(isotope);

      range.addIsotope(isotope, (int) (count * lowerElementBoundaryPercentage),
          (int) (count * upperElementBoundaryPercentage));
    }

    for (IIsotope isotope : range.isotopes()) {
      int min = range.getIsotopeCountMin(isotope);
      int max = range.getIsotopeCountMax(isotope);
      // logger.info("m/z = " + dp.getMZ() + " " + isotope.getSymbol() + "
      // " + min + " - " + max);
    }

    return range;
  }

}
