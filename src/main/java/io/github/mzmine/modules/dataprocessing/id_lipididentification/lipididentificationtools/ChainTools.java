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

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools;

import java.util.ArrayList;
import java.util.List;
import org.openscience.cdk.interfaces.IMolecularFormula;
import io.github.mzmine.util.FormulaUtils;

/**
 * This class contains methods to build fatty acids for MS/MS identification of lipids
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ChainTools {

  /**
   * This method calculates all possible fatty acids formulas for a selected annotated lipid
   */
  public List<String> calculateFattyAcidFormulas() {
    List<String> fattyAcidFormulas = new ArrayList<>();
    int minFattyAcidLength = 2;
    int maxFattyAcidLength = 26;
    int minNumberOfDoubleBonds = 0;
    int maxNumberOfDoubleBonds = 6;
    for (int fattyAcidLength =
        minFattyAcidLength; fattyAcidLength <= maxFattyAcidLength; fattyAcidLength++) {
      for (int fattyAcidDoubleBonds =
          minNumberOfDoubleBonds; fattyAcidDoubleBonds <= maxNumberOfDoubleBonds; fattyAcidDoubleBonds++) {
        if (((fattyAcidDoubleBonds >= 0) && (fattyAcidDoubleBonds <= fattyAcidLength - 1))) {
          fattyAcidFormulas.add(calculateFattyAcidFormula(fattyAcidLength, fattyAcidDoubleBonds));
        }
      }
    }
    return fattyAcidFormulas;
  }

  /**
   * This method calculates all possible hydro carbon formulas for a selected annotated lipid
   */
  public List<String> calculateHydroCarbonFormulas() {
    List<String> hydrocarbonFormulas = new ArrayList<>();
    int minChainLength = 2;
    int maxChainLength = 26;
    int minNumberOfDoubleBonds = 0;
    int maxNumberOfDoubleBonds = 26;
    for (int chainLength = minChainLength; chainLength <= maxChainLength; chainLength++) {
      for (int chainDoubleBonds =
          minNumberOfDoubleBonds; chainDoubleBonds <= maxNumberOfDoubleBonds; chainDoubleBonds++) {
        if (((chainDoubleBonds >= 0) && (chainDoubleBonds <= chainLength - 1))) {
          hydrocarbonFormulas.add(calculateHydroCarbonFormula(chainLength, chainDoubleBonds));
        }
      }
    }
    return hydrocarbonFormulas;
  }


  /**
   * This method creates a String molecularFormula formula for a fatty acid
   */
  public String calculateFattyAcidFormula(int fattyAcidLength, int fattyAcidDoubleBonds) {
    int numberOfHydrogens = fattyAcidLength * 2 - fattyAcidDoubleBonds * 2;
    return "C" + fattyAcidLength + 'H' + numberOfHydrogens + 'O' + 2;
  }

  /**
   * This method creates a String molecularFormula formula for a fatty acid
   */
  public String calculateHydroCarbonFormula(int chainLength, int chainDoubleBonds) {
    int numberOfHydrogens = chainLength * 2 - chainDoubleBonds * 2 + 2;
    return "C" + chainLength + 'H' + numberOfHydrogens;
  }

  /**
   * This method creates the systematic name of a fatty acid
   */
  public String getFattyAcidName(int fattyAcidLength, int fattyAcidDoubleBonds) {
    return "(" + fattyAcidLength + ":" + fattyAcidDoubleBonds + ")";
  }

  public int getChainLengthFromFormula(String fattyAcidFormula) {
    IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula(fattyAcidFormula);
    return FormulaUtils.countElement(formula, "C");
  }

  public int getNumberOfDoubleBondsFromFormula(String fattyAcidFormula) {
    IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula(fattyAcidFormula);
    int chainLength = FormulaUtils.countElement(formula, "C");
    int numberOfHydrogen = FormulaUtils.countElement(formula, "H");
    int delta = (chainLength * 2 - numberOfHydrogen);
    if (delta > 0) {
      return delta / 2;
    } else {
      return 0;
    }
  }

}
