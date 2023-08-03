package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import java.util.Set;
import java.util.stream.Collectors;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class SphingolipidSpeciesLevelMatchedLipidFactory implements
    ISpeciesLevelMatchedLipidFactory {

  private static final MSMSLipidTools MSMS_LIPID_TOOLS = new MSMSLipidTools();

  @Override
  public MatchedLipid validateSpeciesLevelAnnotation(double accurateMz,
      ILipidAnnotation speciesLevelAnnotation, Set<LipidFragment> annotatedFragments,
      DataPoint[] massList, double minMsMsScore, MZTolerance mzTolRangeMSMS,
      IonizationType ionizationType) {
    Set<LipidFragment> speciesLevelFragments = annotatedFragments.stream().filter(
        lipidFragment -> lipidFragment.getLipidFragmentInformationLevelType()
            .equals(LipidAnnotationLevel.SPECIES_LEVEL)).collect(Collectors.toSet());
    if (!speciesLevelFragments.isEmpty()) {
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
  }

}