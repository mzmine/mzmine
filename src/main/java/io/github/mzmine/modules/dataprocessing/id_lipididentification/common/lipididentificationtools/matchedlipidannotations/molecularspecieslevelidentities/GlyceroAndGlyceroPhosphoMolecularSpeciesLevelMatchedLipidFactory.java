package io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.molecularspecieslevelidentities;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipididentificationtools.matchedlipidannotations.specieslevellipidmatches.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidChainFactory;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.common.lipidutils.LipidFactory;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class GlyceroAndGlyceroPhosphoMolecularSpeciesLevelMatchedLipidFactory implements
    IMolecularSpeciesLevelMatchedLipidFactory {

  private static final MSMSLipidTools MSMS_LIPID_TOOLS = new MSMSLipidTools();
  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  @Override
  public MatchedLipid validateMolecularSpeciesLevelAnnotation(double accurateMz,
      ILipidAnnotation speciesLevelAnnotation, Set<LipidFragment> annotatedFragments,
      DataPoint[] massList, double minMsMsScore, MZTolerance mzTolRangeMSMS,
      IonizationType ionizationType) {
    if (!annotatedFragments.isEmpty()) {
      IMolecularFormula lipidFormula = null;
      try {
        lipidFormula = (IMolecularFormula) speciesLevelAnnotation.getMolecularFormula().clone();
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
      ionizationType.ionizeFormula(lipidFormula);
      double precursorMz = FormulaUtils.calculateMzRatio(lipidFormula);
      Double msMsScore = MSMS_LIPID_TOOLS.calculateMsMsScore(massList, annotatedFragments,
          precursorMz, mzTolRangeMSMS);
      if (msMsScore >= minMsMsScore) {
        return new MatchedLipid(speciesLevelAnnotation, accurateMz, ionizationType,
            annotatedFragments, msMsScore);
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  @Override
  public Set<MatchedLipid> predictMolecularSpeciesLevelMatches(Set<LipidFragment> detectedFragments,
      ILipidAnnotation lipidAnnotation, Double accurateMz, DataPoint[] massList,
      double minMsMsScore, MZTolerance mzTolRangeMSMS, IonizationType ionizationType) {
    Set<LipidFragment> detectedFragmentsWithChainInformation = detectedFragments.stream().filter(
        fragment -> fragment.getLipidFragmentInformationLevelType()
            .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)).collect(Collectors.toSet());
    List<ILipidChain> chains = getChainsFromFragments(detectedFragmentsWithChainInformation);
    Set<MatchedLipid> matchedMolecularSpeciesLevelAnnotations = new HashSet<>();

    // get number of total C atoms, double bonds and number of chains
    int totalNumberOfCAtoms = 0;
    int totalNumberOfDBEs = 0;
    if (lipidAnnotation instanceof SpeciesLevelAnnotation) {
      totalNumberOfCAtoms = ((SpeciesLevelAnnotation) lipidAnnotation).getNumberOfCarbons();
      totalNumberOfDBEs = ((SpeciesLevelAnnotation) lipidAnnotation).getNumberOfDBEs();
    }
    int chainsInLipid = lipidAnnotation.getLipidClass().getChainTypes().length;

    for (int i = 0; i < chains.size(); i++) {
      int carbonOne = chains.get(i).getNumberOfCarbons();
      int dbeOne = chains.get(i).getNumberOfDBEs();
      if (chainsInLipid == 1 && carbonOne == totalNumberOfCAtoms && dbeOne == totalNumberOfDBEs) {
        List<ILipidChain> predictedChains = new ArrayList<>();
        predictedChains.add(chains.get(i));
        if (checkChainTypesFitLipidClass(predictedChains, lipidAnnotation.getLipidClass())) {
          Set<LipidFragment> fittingFragments = extractFragmentsForFittingChains(predictedChains,
              detectedFragmentsWithChainInformation);
          matchedMolecularSpeciesLevelAnnotations.add(
              buildNewMolecularSpeciesLevelMatch(fittingFragments, lipidAnnotation, accurateMz,
                  massList, predictedChains, minMsMsScore, mzTolRangeMSMS, ionizationType));
        }
      }
      if (chainsInLipid >= 2) {
        for (int j = 0; j < chains.size(); j++) {
          int carbonTwo = chains.get(j).getNumberOfCarbons();
          int dbeTwo = chains.get(j).getNumberOfDBEs();
          if (chainsInLipid == 2 && carbonOne + carbonTwo == totalNumberOfCAtoms
              && dbeOne + dbeTwo == totalNumberOfDBEs) {
            List<ILipidChain> predictedChains = new ArrayList<>();
            predictedChains.add(chains.get(i));
            predictedChains.add(chains.get(j));
            if (checkChainTypesFitLipidClass(predictedChains, lipidAnnotation.getLipidClass())) {
              Set<LipidFragment> fittingFragments = extractFragmentsForFittingChains(
                  predictedChains, detectedFragmentsWithChainInformation);
              matchedMolecularSpeciesLevelAnnotations.add(
                  buildNewMolecularSpeciesLevelMatch(fittingFragments, lipidAnnotation, accurateMz,
                      massList, predictedChains, minMsMsScore, mzTolRangeMSMS, ionizationType));
            }
          }
          if (chainsInLipid >= 3) {
            for (int k = 0; k < chains.size(); k++) {
              int carbonThree = chains.get(k).getNumberOfCarbons();
              int dbeThree = chains.get(k).getNumberOfDBEs();
              if (chainsInLipid == 3 && carbonOne + carbonTwo + carbonThree == totalNumberOfCAtoms
                  && dbeOne + dbeTwo + dbeThree == totalNumberOfDBEs) {
                List<ILipidChain> predictedChains = new ArrayList<>();
                predictedChains.add(chains.get(i));
                predictedChains.add(chains.get(j));
                predictedChains.add(chains.get(k));
                if (checkChainTypesFitLipidClass(predictedChains,
                    lipidAnnotation.getLipidClass())) {
                  Set<LipidFragment> fittingFragments = extractFragmentsForFittingChains(
                      predictedChains, detectedFragmentsWithChainInformation);
                  matchedMolecularSpeciesLevelAnnotations.add(
                      buildNewMolecularSpeciesLevelMatch(fittingFragments, lipidAnnotation,
                          accurateMz, massList, predictedChains, minMsMsScore, mzTolRangeMSMS,
                          ionizationType));
                }
              }
              if (chainsInLipid >= 4) {
                for (int l = 0; l < chains.size(); l++) {
                  int carbonFour = chains.get(l).getNumberOfCarbons();
                  int dbeFour = chains.get(l).getNumberOfDBEs();
                  if (chainsInLipid == 4
                      && carbonOne + carbonTwo + carbonThree + carbonFour == totalNumberOfCAtoms
                      && dbeOne + dbeTwo + dbeThree + dbeFour == totalNumberOfDBEs) {
                    List<ILipidChain> predictedChains = new ArrayList<>();
                    predictedChains.add(chains.get(i));
                    predictedChains.add(chains.get(j));
                    predictedChains.add(chains.get(k));
                    predictedChains.add(chains.get(l));
                    if (checkChainTypesFitLipidClass(predictedChains,
                        lipidAnnotation.getLipidClass())) {
                      Set<LipidFragment> fittingFragments = extractFragmentsForFittingChains(
                          predictedChains, detectedFragmentsWithChainInformation);
                      matchedMolecularSpeciesLevelAnnotations.add(
                          buildNewMolecularSpeciesLevelMatch(fittingFragments, lipidAnnotation,
                              accurateMz, massList, predictedChains, minMsMsScore, mzTolRangeMSMS,
                              ionizationType));
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    return matchedMolecularSpeciesLevelAnnotations;
  }

  private List<ILipidChain> getChainsFromFragments(Set<LipidFragment> detectedFragments) {
    LipidChainFactory chainFactory = new LipidChainFactory();
    List<ILipidChain> chains = new ArrayList<>();
    for (LipidFragment lipidFragment : detectedFragments) {
      if (lipidFragment.getLipidChainType() != null && lipidFragment.getChainLength() != null
          && lipidFragment.getNumberOfDBEs() != null) {
        ILipidChain lipidChain = chainFactory.buildLipidChain(lipidFragment.getLipidChainType(),
            lipidFragment.getChainLength(), lipidFragment.getNumberOfDBEs());
        if (lipidChain != null) {
          chains.add(lipidChain);
        }
      }
    }
    return chains.stream().distinct().collect(Collectors.toList());
  }

  private boolean checkChainTypesFitLipidClass(List<ILipidChain> chains, ILipidClass lipidClass) {
    List<LipidChainType> lipidClassChainTypes = Arrays.asList(lipidClass.getChainTypes());
    List<LipidChainType> chainTypes = chains.stream().map(ILipidChain::getLipidChainType)
        .collect(Collectors.toList());
    Collections.sort(lipidClassChainTypes);
    Collections.sort(chainTypes);
    return lipidClassChainTypes.equals(chainTypes);
  }

  private Set<LipidFragment> extractFragmentsForFittingChains(List<ILipidChain> predictedChains,
      Set<LipidFragment> detectedFragmentsWithChainInformation) {
    Set<LipidFragment> fittingFragments = new HashSet<>();
    for (LipidFragment lipidFragment : detectedFragmentsWithChainInformation) {
      for (ILipidChain chain : predictedChains) {
        if (lipidFragment != null && chain != null
            && lipidFragment.getChainLength() == chain.getNumberOfCarbons()
            && lipidFragment.getNumberOfDBEs() == chain.getNumberOfDBEs()) {
          fittingFragments.add(lipidFragment);
        }
      }
    }
    return fittingFragments;
  }

  private MatchedLipid buildNewMolecularSpeciesLevelMatch(Set<LipidFragment> detectedFragments,
      ILipidAnnotation lipidAnnotation, Double accurateMz, DataPoint[] massList,
      List<ILipidChain> predictedChains, double minMsMsScore, MZTolerance mzTolRangeMSMS,
      IonizationType ionizationType) {
    ILipidAnnotation molecularSpeciesLevelAnnotation = LIPID_FACTORY.buildMolecularSpeciesLevelLipidFromChains(
        lipidAnnotation.getLipidClass(), predictedChains);
    Double msMsScore = MSMS_LIPID_TOOLS.calculateMsMsScore(massList, detectedFragments,
        minMsMsScore, mzTolRangeMSMS);
    return new MatchedLipid(molecularSpeciesLevelAnnotation, accurateMz, ionizationType,
        detectedFragments, msMsScore);

  }
}
