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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class SphingoMolecularSpeciesLevelMatchedLipidFactory implements
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
    Set<MatchedLipid> matchedMolecularSpeciesLevelAnnotations = new HashSet<>();
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
    Set<LipidFragment> detectedFragmentsWithChainInformation = detectedFragments.stream().filter(
        fragment -> fragment.getLipidFragmentInformationLevelType()
            .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)).collect(Collectors.toSet());
    List<ILipidChain> potentialChains = getChainsFromFragments(
        detectedFragmentsWithChainInformation);
    List<ILipidChain> backboneChains = getPotentialBackboneChainsFromPossibleChains(
        potentialChains);
    Set<MolecularSpeciesLevelAnnotation> predictedCompositions = new HashSet<>(
        predictCompositionsUsingBackbones(backboneChains, lipidAnnotation, totalNumberOfCAtoms,
            totalNumberOfDBEs, detectedFragmentsWithChainInformation));

    for (MolecularSpeciesLevelAnnotation molecularSpeciesLevelAnnotation : predictedCompositions) {
      if (checkChainTypesFitLipidClass(molecularSpeciesLevelAnnotation.getLipidChains(),
          lipidAnnotation.getLipidClass())) {
        Set<LipidFragment> fittingFragments = extractFragmentsForFittingChains(
            molecularSpeciesLevelAnnotation.getLipidChains(),
            detectedFragmentsWithChainInformation);
        if (!fittingFragments.isEmpty()) {
          MatchedLipid newMolecularSpeciesLevelMatch = buildNewMolecularSpeciesLevelMatch(
              fittingFragments, molecularSpeciesLevelAnnotation, accurateMz, massList, minMsMsScore,
              mzTolRangeMSMS, ionizationType);
          matchedMolecularSpeciesLevelAnnotations.add(newMolecularSpeciesLevelMatch);
        }
      }
    }
    return matchedMolecularSpeciesLevelAnnotations;
  }

  private Set<MolecularSpeciesLevelAnnotation> predictCompositionsUsingBackbones(
      List<ILipidChain> backboneChains, ILipidAnnotation lipidAnnotation, int totalNumberOfCAtoms,
      int totalNumberOfDBEs, Set<LipidFragment> detectedFragments) {
    Set<MolecularSpeciesLevelAnnotation> molecularSpeciesLevelAnnotations = new LinkedHashSet<>();
    int speciesChainNumber = lipidAnnotation.getLipidClass().getChainTypes().length;

    for (int i = 0; i < backboneChains.size(); i++) {
      int carbonBackbone = backboneChains.get(i).getNumberOfCarbons();
      int doubleBondEquivalentBackbone = backboneChains.get(i).getNumberOfDBEs();
      if (speciesChainNumber == 1 && carbonBackbone == totalNumberOfCAtoms
          && doubleBondEquivalentBackbone == totalNumberOfDBEs) {
        molecularSpeciesLevelAnnotations.add(
            LIPID_FACTORY.buildMolecularSpeciesLevelLipidFromChains(lipidAnnotation.getLipidClass(),
                List.of(backboneChains.get(i))));
      }
      if (speciesChainNumber >= 2) {

        // estimate none backbone chain
        int carbonTwo = totalNumberOfCAtoms - carbonBackbone;
        int doubleBondEquivalentTwo = totalNumberOfDBEs - doubleBondEquivalentBackbone;
        if (speciesChainNumber == 2 && doubleBondEquivalentTwo >= 0
            && carbonTwo / 2 >= doubleBondEquivalentTwo) {
          molecularSpeciesLevelAnnotations.add(
              LIPID_FACTORY.buildMolecularSpeciesLevelLipid(lipidAnnotation.getLipidClass(),
                  new int[]{carbonBackbone, carbonTwo},
                  new int[]{doubleBondEquivalentBackbone, doubleBondEquivalentTwo},
                  new int[]{0, 0}));
        }
      }
    }
    return molecularSpeciesLevelAnnotations;
  }

  private List<ILipidChain> getPotentialBackboneChainsFromPossibleChains(
      List<ILipidChain> potentialChains) {
    List<ILipidChain> potentialBackboneChains = new ArrayList<>();
    for (ILipidChain potentialChain : potentialChains) {
      if (potentialChain.getLipidChainType()
          .equals(LipidChainType.SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN)
          || potentialChain.getLipidChainType()
          .equals(LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN)
          || potentialChain.getLipidChainType()
          .equals(LipidChainType.SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN)) {
        potentialBackboneChains.add(potentialChain);
      }
    }
    return potentialBackboneChains;
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
            && lipidFragment.getNumberOfDBEs() == chain.getNumberOfDBEs()
            && lipidFragment.getLipidChainType() == chain.getLipidChainType()) {
          fittingFragments.add(lipidFragment);
        }
      }
    }
    return fittingFragments;
  }

  private MatchedLipid buildNewMolecularSpeciesLevelMatch(Set<LipidFragment> detectedFragments,
      ILipidAnnotation molecularSpeciesLevelAnnotation, Double accurateMz, DataPoint[] massList,
      double minMsMsScore, MZTolerance mzTolRangeMSMS, IonizationType ionizationType) {
    Double msMsScore = MSMS_LIPID_TOOLS.calculateMsMsScore(massList, detectedFragments,
        minMsMsScore, mzTolRangeMSMS);
    return new MatchedLipid(molecularSpeciesLevelAnnotation, accurateMz, ionizationType,
        detectedFragments, msMsScore);

  }
}
