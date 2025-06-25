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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class GlyceroAndPhosphoMolecularSpeciesLevelMatchedLipidFactory implements
    IMolecularSpeciesLevelMatchedLipidFactory {

  private static final MSMSLipidTools MSMS_LIPID_TOOLS = new MSMSLipidTools();
  private static final LipidFactory LIPID_FACTORY = new LipidFactory();
  private static final LipidChainFactory LIPID_CHAIN_FACTORY = new LipidChainFactory();

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
    Set<LipidFragment> detectedFragmentsWithCombinedChainInformation = null;
    if (detectedFragmentsWithChainInformation.stream().anyMatch(
        lipidFragment -> lipidFragment.getLipidChainType()
            .equals(LipidChainType.TWO_ACYL_CHAINS_COMBINED))) {
      detectedFragmentsWithCombinedChainInformation = extractFragmentsWithCombinedChainInformation(
          detectedFragmentsWithChainInformation);
    }
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
                  massList, predictedChains, mzTolRangeMSMS, ionizationType));
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
                      massList, predictedChains, mzTolRangeMSMS, ionizationType));
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
                          accurateMz, massList, predictedChains, mzTolRangeMSMS,
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
                              accurateMz, massList, predictedChains, mzTolRangeMSMS,
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

    //ILS allows alkyl chain assumption based on detected acyl chain for ether lipids, only allowed for 2 chains in total
    LipidChainType[] chainTypesArray = lipidAnnotation.getLipidClass().getChainTypes();
    List<LipidChainType> chainTypes = new ArrayList<>();
    Collections.addAll(chainTypes, chainTypesArray);

    boolean containsAlkylChain = chainTypes.contains(LipidChainType.ALKYL_CHAIN);
    if (matchedMolecularSpeciesLevelAnnotations.isEmpty() && containsAlkylChain
        && chainsInLipid <= 2) {
      Set<MatchedLipid> matchedEtherLipids = constructPossibleEtherLipidMolecularSpeciesLevelAnnotations(
          detectedFragmentsWithChainInformation, chains, lipidAnnotation, accurateMz, massList,
          minMsMsScore, mzTolRangeMSMS, ionizationType, totalNumberOfCAtoms, totalNumberOfDBEs);
      matchedMolecularSpeciesLevelAnnotations.addAll(matchedEtherLipids);
    }

    // check for fragments with combined chains
    if (detectedFragmentsWithCombinedChainInformation != null
        && !detectedFragmentsWithCombinedChainInformation.isEmpty()) {
      handleCombinedChainFragments(lipidAnnotation, accurateMz, massList, minMsMsScore,
          mzTolRangeMSMS, ionizationType, detectedFragmentsWithChainInformation,
          detectedFragmentsWithCombinedChainInformation, matchedMolecularSpeciesLevelAnnotations,
          totalNumberOfCAtoms, totalNumberOfDBEs, chainTypesArray);
    }

    return matchedMolecularSpeciesLevelAnnotations;
  }

  private void handleCombinedChainFragments(ILipidAnnotation lipidAnnotation, Double accurateMz,
      DataPoint[] massList, double minMsMsScore, MZTolerance mzTolRangeMSMS,
      IonizationType ionizationType, Set<LipidFragment> detectedFragmentsWithChainInformation,
      Set<LipidFragment> detectedFragmentsWithCombinedChainInformation,
      Set<MatchedLipid> matchedMolecularSpeciesLevelAnnotations, int totalNumberOfCAtoms,
      int totalNumberOfDBEs, LipidChainType[] chainTypesArray) {
    addAnnotationSupportingFragments(matchedMolecularSpeciesLevelAnnotations,
        detectedFragmentsWithCombinedChainInformation);

    // if no annotations detected, check if combined chain annotations can be found
    if (matchedMolecularSpeciesLevelAnnotations.isEmpty() && Arrays.stream(
            lipidAnnotation.getLipidClass().getChainTypes())
        .allMatch(lipidChainType -> lipidChainType.equals(LipidChainType.ACYL_CHAIN))) {
      List<ILipidChain> combinedChainsFromFragments = getChainsFromFragments(
          detectedFragmentsWithCombinedChainInformation);
      int numberOfChains = chainTypesArray.length;
      if (numberOfChains == 4) {
        for (ILipidChain combinedChainOne : combinedChainsFromFragments) {
          for (ILipidChain combinedChainTwo : combinedChainsFromFragments) {
            int combinedCarbons =
                combinedChainOne.getNumberOfCarbons() + combinedChainTwo.getNumberOfCarbons();
            int combinedDbes =
                combinedChainOne.getNumberOfDBEs() + combinedChainTwo.getNumberOfDBEs();
            if (combinedCarbons == totalNumberOfCAtoms && combinedDbes == totalNumberOfDBEs) {
              List<ILipidChain> predictedChains = List.of(combinedChainOne, combinedChainTwo);
              Set<LipidFragment> extractFragmentsForFittingChains = extractFragmentsForFittingChains(
                  predictedChains, detectedFragmentsWithCombinedChainInformation);
              matchedMolecularSpeciesLevelAnnotations.add(
                  buildNewSpeciesLevelMatch(extractFragmentsForFittingChains, lipidAnnotation,
                      accurateMz, massList, predictedChains, mzTolRangeMSMS,
                      ionizationType));
            }
          }
        }
      }
    }
  }

  private void addAnnotationSupportingFragments(
      Set<MatchedLipid> matchedMolecularSpeciesLevelAnnotations,
      Set<LipidFragment> detectedFragmentsWithCombinedChainInformation) {
    for (MatchedLipid matchedLipid : matchedMolecularSpeciesLevelAnnotations) {
      if (matchedLipid.getLipidAnnotation() instanceof MolecularSpeciesLevelAnnotation lipidAnnotation) {
        List<ILipidChain> lipidChains = lipidAnnotation.getLipidChains();
        for (LipidFragment fragmentWithCombinedChainInformation : detectedFragmentsWithCombinedChainInformation) {
          for (ILipidChain lipidChainOne : lipidChains) {
            for (ILipidChain lipidChainTwo : lipidChains) {
              int carbons = lipidChainOne.getNumberOfCarbons() + lipidChainTwo.getNumberOfCarbons();
              int dbes = lipidChainOne.getNumberOfDBEs() + lipidChainTwo.getNumberOfDBEs();
              if (carbons == fragmentWithCombinedChainInformation.getChainLength()
                  && dbes == fragmentWithCombinedChainInformation.getNumberOfDBEs()) {
                matchedLipid.getMatchedFragments().add(fragmentWithCombinedChainInformation);
              }
            }
          }
        }
      }
    }
  }

  private Set<LipidFragment> extractFragmentsWithCombinedChainInformation(
      Set<LipidFragment> detectedFragmentsWithChainInformation) {
    Set<LipidFragment> detectedChainFragmentsWithCombinedChainInformation = new HashSet<>();
    Iterator<LipidFragment> iterator = detectedFragmentsWithChainInformation.iterator();
    while (iterator.hasNext()) {
      LipidFragment fragment = iterator.next();
      if (fragment.getLipidChainType().equals(LipidChainType.TWO_ACYL_CHAINS_COMBINED)) {
        detectedChainFragmentsWithCombinedChainInformation.add(fragment);
        iterator.remove();
      }
    }
    return detectedChainFragmentsWithCombinedChainInformation;
  }


  private Set<MatchedLipid> constructPossibleEtherLipidMolecularSpeciesLevelAnnotations(
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
        int carbonEstimateEtherChain = totalNumberOfCAtoms - chain.getNumberOfCarbons();
        int dbesEstimateEtherChain = totalNumberOfDBEs - chain.getNumberOfDBEs();
        if (carbonEstimateEtherChain > 0 && totalNumberOfDBEs >= 0) {
          ILipidChain alkylChain = LIPID_CHAIN_FACTORY.buildLipidChain(LipidChainType.ALKYL_CHAIN,
              carbonEstimateEtherChain, dbesEstimateEtherChain);
          if (alkylChain == null) {
            continue;
          }
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
      List<ILipidChain> predictedChains, MZTolerance mzTolRangeMSMS,
      IonizationType ionizationType) {
    ILipidAnnotation molecularSpeciesLevelAnnotation = LIPID_FACTORY.buildMolecularSpeciesLevelLipidFromChains(
        lipidAnnotation.getLipidClass(), predictedChains);
    if (molecularSpeciesLevelAnnotation != null) {
      IMolecularFormula lipidFormula = null;
      try {
        lipidFormula = (IMolecularFormula) lipidAnnotation.getMolecularFormula().clone();
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
      double precursorMz = FormulaUtils.calculateMzRatio(lipidFormula);
      Double msMsScore = MSMS_LIPID_TOOLS.calculateMsMsScore(massList, detectedFragments,
          precursorMz, mzTolRangeMSMS);
      return new MatchedLipid(molecularSpeciesLevelAnnotation, accurateMz, ionizationType,
          detectedFragments, msMsScore);
    } else {
      return null;
    }
  }

  private MatchedLipid buildNewSpeciesLevelMatch(Set<LipidFragment> detectedFragments,
      ILipidAnnotation lipidAnnotation, Double accurateMz, DataPoint[] massList,
      List<ILipidChain> predictedChains, MZTolerance mzTolRangeMSMS,
      IonizationType ionizationType) {
    ILipidAnnotation molecularSpeciesLevelAnnotation = LIPID_FACTORY.buildSpeciesLevelLipid(
        lipidAnnotation.getLipidClass(),
        predictedChains.stream().mapToInt(ILipidChain::getNumberOfCarbons).sum(),
        predictedChains.stream().mapToInt(ILipidChain::getNumberOfDBEs).sum(), 0);
    if (molecularSpeciesLevelAnnotation != null) {
      IMolecularFormula lipidFormula = null;
      try {
        lipidFormula = (IMolecularFormula) lipidAnnotation.getMolecularFormula().clone();
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
      double precursorMz = FormulaUtils.calculateMzRatio(lipidFormula);
      Double msMsScore = MSMS_LIPID_TOOLS.calculateMsMsScore(massList, detectedFragments,
          precursorMz, mzTolRangeMSMS);
      return new MatchedLipid(molecularSpeciesLevelAnnotation, accurateMz, ionizationType,
          detectedFragments, msMsScore);
    } else {
      return null;
    }
  }
}
