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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleRating;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import java.util.Set;
import java.util.stream.Collectors;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class SphingolipidSpeciesLevelMatchedLipidFactory implements ISpeciesLevelMatchedLipidFactory {

  private static final MSMSLipidTools MSMS_LIPID_TOOLS = new MSMSLipidTools();

  @Override
  public MatchedLipid validateSpeciesLevelAnnotation(double accurateMz, ILipidAnnotation speciesLevelAnnotation, Set<LipidFragment> annotatedFragments,
      DataPoint[] massList, double minMsMsScore, MZTolerance mzTolRangeMSMS, IonizationType ionizationType) {
    Set<LipidFragment> speciesLevelFragments = annotatedFragments.stream().filter(
        lipidFragment -> lipidFragment.getLipidFragmentInformationLevelType().equals(LipidAnnotationLevel.SPECIES_LEVEL)).collect(Collectors.toSet());
    if (!speciesLevelFragments.isEmpty()) {
      if (checkLipidFragmentationRulesRating(speciesLevelFragments)) {
        IMolecularFormula lipidFormula = null;
        try {
          lipidFormula = (IMolecularFormula) speciesLevelAnnotation.getMolecularFormula().clone();
        } catch (CloneNotSupportedException e) {
          throw new RuntimeException(e);
        }
        ionizationType.ionizeFormula(lipidFormula);
        double precursorMz = FormulaUtils.calculateMzRatio(lipidFormula);
        Double msMsScore = MSMS_LIPID_TOOLS.calculateMsMsScore(massList, speciesLevelFragments,
            precursorMz, mzTolRangeMSMS);
        if (msMsScore >= minMsMsScore) {
          return new MatchedLipid(speciesLevelAnnotation, accurateMz, ionizationType,
              speciesLevelFragments, msMsScore);
        } else {
          return null;
        }
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  private boolean checkLipidFragmentationRulesRating(Set<LipidFragment> speciesLevelFragments) {
    long majorCount = speciesLevelFragments.stream().filter(
        fragment -> fragment.getLipidFragmentationRuleRating() == LipidFragmentationRuleRating.MAJOR).count();

    long minorCount = speciesLevelFragments.stream().filter(
        fragment -> fragment.getLipidFragmentationRuleRating() == LipidFragmentationRuleRating.MINOR).count();

    return majorCount > 0 || minorCount >= 2;
  }

}