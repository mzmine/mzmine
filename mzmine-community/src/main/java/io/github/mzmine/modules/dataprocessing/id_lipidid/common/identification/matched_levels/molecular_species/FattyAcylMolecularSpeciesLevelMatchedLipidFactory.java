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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.molecular_species;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.MSMSLipidTools;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.species_level.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class FattyAcylMolecularSpeciesLevelMatchedLipidFactory implements
    IMolecularSpeciesLevelMatchedLipidFactory {

  private static final MSMSLipidTools MSMS_LIPID_TOOLS = new MSMSLipidTools();
  private static final LipidFactory LIPID_FACTORY = new LipidFactory();

  @Override
  public MatchedLipid validateMolecularSpeciesLevelAnnotation(double accurateMz,
      ILipidAnnotation molecularSpeciesLevelAnnotation, Set<LipidFragment> annotatedFragments,
      DataPoint[] massList, double minMsMsScore, MZTolerance mzTolRangeMSMS,
      IonizationType ionizationType) {
    if (!annotatedFragments.isEmpty()) {
      IMolecularFormula lipidFormula = null;
      try {
        lipidFormula = (IMolecularFormula) molecularSpeciesLevelAnnotation.getMolecularFormula()
            .clone();
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
      ionizationType.ionizeFormula(lipidFormula);
      double precursorMz = FormulaUtils.calculateMzRatio(lipidFormula);
      Double msMsScore = MSMS_LIPID_TOOLS.calculateMsMsScore(massList, annotatedFragments,
          precursorMz, mzTolRangeMSMS);
      if (msMsScore >= minMsMsScore) {
        return new MatchedLipid(molecularSpeciesLevelAnnotation, accurateMz, ionizationType,
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
    } else if (lipidAnnotation instanceof MolecularSpeciesLevelAnnotation) {
      totalNumberOfCAtoms = ((MolecularSpeciesLevelAnnotation) lipidAnnotation).getLipidChains()
          .stream().mapToInt(ILipidChain::getNumberOfCarbons).sum();
      totalNumberOfDBEs = ((MolecularSpeciesLevelAnnotation) lipidAnnotation).getLipidChains()
          .stream().mapToInt(ILipidChain::getNumberOfDBEs).sum();
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

    //Fall back to Species Level if construction was not possible
    if (matchedMolecularSpeciesLevelAnnotations.isEmpty() && chainsInLipid <= 2) {
      Set<MatchedLipid> matchedEtherLipids = constructPossibleSpeciesLevelAnnotations(
          detectedFragmentsWithChainInformation, chains, lipidAnnotation, accurateMz, massList,
          minMsMsScore, mzTolRangeMSMS, ionizationType, totalNumberOfCAtoms, totalNumberOfDBEs);
      matchedMolecularSpeciesLevelAnnotations.addAll(matchedEtherLipids);
    }
    return matchedMolecularSpeciesLevelAnnotations;
  }

  private Set<MatchedLipid> constructPossibleSpeciesLevelAnnotations(
      Set<LipidFragment> detectedFragmentsWithChainInformation, List<ILipidChain> chains,
      ILipidAnnotation lipidAnnotation, Double accurateMz, DataPoint[] massList,
      double minMsMsScore, MZTolerance mzTolRangeMSMS, IonizationType ionizationType,
      int totalNumberOfCAtoms, int totalNumberOfDBEs) {
    Set<MatchedLipid> matchedLipids = new HashSet<>();
    Map<ILipidChain, Set<LipidFragment>> chainFragmentsMap = new HashMap<>();
    for (ILipidChain chain : chains) {
      Set<LipidFragment> lipidChainFragments = detectedFragmentsWithChainInformation.stream()
          .filter(
              lipidFragment -> lipidFragment.getLipidChainType().equals(chain.getLipidChainType())
                  && lipidFragment.getChainLength().equals(chain.getNumberOfCarbons())
                  && lipidFragment.getNumberOfDBEs().equals(chain.getNumberOfDBEs()))
          .collect(Collectors.toSet());
      if (!lipidChainFragments.isEmpty()) {
        chainFragmentsMap.put(chain, lipidChainFragments);
      }
    }
    for (Map.Entry<ILipidChain, Set<LipidFragment>> entry : chainFragmentsMap.entrySet()) {
      ILipidChain chain = entry.getKey();
      if (chain != null) {
        int carbonEstimateSecondChain = totalNumberOfCAtoms - chain.getNumberOfCarbons();
        if (carbonEstimateSecondChain > 0 && totalNumberOfDBEs >= 0) {
          //Fall back to Species Level annotation to avoid over annotation
          IMolecularFormula lipidFormula = null;
          try {
            lipidFormula = (IMolecularFormula) lipidAnnotation.getMolecularFormula().clone();
          } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
          }
          ionizationType.ionizeFormula(lipidFormula);
          double precursorMz = FormulaUtils.calculateMzRatio(lipidFormula);
          Double msMsScore = MSMS_LIPID_TOOLS.calculateMsMsScore(massList, entry.getValue(),
              precursorMz, mzTolRangeMSMS);
          if (msMsScore >= minMsMsScore) {
            matchedLipids.add(
                new MatchedLipid(lipidAnnotation, accurateMz, ionizationType, entry.getValue(),
                    msMsScore));
          }
        }
      }
    }
    return matchedLipids;
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
    if (molecularSpeciesLevelAnnotation != null) {
      Double msMsScore = MSMS_LIPID_TOOLS.calculateMsMsScore(massList, detectedFragments,
          minMsMsScore, mzTolRangeMSMS);
      return new MatchedLipid(molecularSpeciesLevelAnnotation, accurateMz, ionizationType,
          detectedFragments, msMsScore);
    } else {
      return null;
    }
  }
}
