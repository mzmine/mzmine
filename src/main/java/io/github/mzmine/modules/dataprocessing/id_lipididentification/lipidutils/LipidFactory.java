/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils;

import java.util.ArrayList;
import java.util.List;
import org.openscience.cdk.interfaces.IMolecularFormula;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.lipidchain.ILipidChain;
import io.github.mzmine.util.FormulaUtils;

public class LipidFactory {

  private static final LipidChainFactory LIPID_CHAIN_FACTORY = new LipidChainFactory();

  public SpeciesLevelAnnotation buildSpeciesLevelLipid(ILipidClass lipidClass, int numberOfCarbons,
      int numberOfDBEs) {
    String annotation;
    boolean hasAlkylChain = false;
    boolean hasNoChain = false;
    if (numberOfCarbons < lipidClass.getChainTypes().length) {
      return null;
    }

    if (lipidClass.getChainTypes().length == 0) {
      hasNoChain = true;
    } else {
      for (LipidChainType type : lipidClass.getChainTypes()) {
        if (type.equals(LipidChainType.ALKYL_CHAIN)) {
          hasAlkylChain = true;
          break;
        }
      }
    }
    if (hasAlkylChain) {
      annotation = lipidClass.getAbbr() + " O-" + numberOfCarbons + ':' + numberOfDBEs;
    } else if (hasNoChain) {
      annotation = lipidClass.getAbbr();
    } else {
      annotation = lipidClass.getAbbr() + " " + numberOfCarbons + ':' + numberOfDBEs;
    }
    IMolecularFormula molecularFormula = synthesisLipidMolecularFormula(
        lipidClass.getBackBoneFormula(), numberOfCarbons, numberOfDBEs, lipidClass.getChainTypes());
    if (molecularFormula != null) {
      return new SpeciesLevelAnnotation(lipidClass, annotation, molecularFormula, numberOfCarbons,
          numberOfDBEs);
    } else {
      return null;
    }
  }

  public MolecularSpeciesLevelAnnotation buildMolecularSpeciesLevelLipid(ILipidClass lipidClass,
      int[] numberOfCarbons, int[] numberOfDBEs) {
    List<ILipidChain> lipidChains = new ArrayList<>();
    LipidChainType[] lipidChainTypes = lipidClass.getChainTypes();
    for (int i = 0; i < lipidChainTypes.length; i++) {
      lipidChains.add(LIPID_CHAIN_FACTORY.buildLipidChain(lipidChainTypes[i], numberOfCarbons[i],
          numberOfDBEs[i]));
    }
    int totalNumberOfCarbons = lipidChains.stream().mapToInt(ILipidChain::getNumberOfCarbons).sum();
    int totalNumberOfDBEs = lipidChains.stream().mapToInt(ILipidChain::getNumberOfDBEs).sum();
    String annotation =
        lipidClass.getAbbr() + " " + LIPID_CHAIN_FACTORY.connectLipidChainAnnotations(lipidChains);
    IMolecularFormula molecularFormula =
        synthesisLipidMolecularFormula(lipidClass.getBackBoneFormula(), totalNumberOfCarbons,
            totalNumberOfDBEs, lipidClass.getChainTypes());
    if (molecularFormula != null) {
      return new MolecularSpeciesLevelAnnotation(lipidClass, annotation, molecularFormula,
          lipidChains);
    } else {
      return null;
    }
  }

  public MolecularSpeciesLevelAnnotation buildMolecularSpeciesLevelLipidFromChains(
      ILipidClass lipidClass, List<ILipidChain> lipidChains) {
    int totalNumberOfCarbons = lipidChains.stream().mapToInt(ILipidChain::getNumberOfCarbons).sum();
    int totalNumberOfDBEs = lipidChains.stream().mapToInt(ILipidChain::getNumberOfDBEs).sum();
    String annotation =
        lipidClass.getAbbr() + " " + LIPID_CHAIN_FACTORY.connectLipidChainAnnotations(lipidChains);
    IMolecularFormula molecularFormula =
        synthesisLipidMolecularFormula(lipidClass.getBackBoneFormula(), totalNumberOfCarbons,
            totalNumberOfDBEs, lipidClass.getChainTypes());
    if (molecularFormula != null) {
      return new MolecularSpeciesLevelAnnotation(lipidClass, annotation, molecularFormula,
          lipidChains);
    } else {
      return null;
    }
  }


  // lipid synthesis
  private IMolecularFormula synthesisLipidMolecularFormula(String lipidBackbone, int chainLength,
      int chainDoubleBonds, LipidChainType[] chainTypes) {

    IMolecularFormula lipidBackboneFormula =
        FormulaUtils.createMajorIsotopeMolFormula(lipidBackbone);

    int numberOfCarbonsPerChain = chainLength / chainTypes.length;
    int restCarbons = chainLength % chainTypes.length;
    int numberOfDoubleBondsPerChain = chainDoubleBonds / chainTypes.length;
    int restDoubleBonds = chainDoubleBonds % chainTypes.length;

    // build chains
    for (int i = 0; i < chainTypes.length; i++) {

      // add rests to last chainBUILDER
      if (i == chainTypes.length - 1) {
        numberOfCarbonsPerChain = numberOfCarbonsPerChain + restCarbons;
        numberOfDoubleBondsPerChain = numberOfDoubleBondsPerChain + restDoubleBonds;
      }
      IMolecularFormula chainFormula = LIPID_CHAIN_FACTORY.buildLipidChainFormula(chainTypes[i],
          numberOfCarbonsPerChain, numberOfDoubleBondsPerChain);
      if (chainFormula == null) {
        return null;
      }
      lipidBackboneFormula =
          doChainTypeSpecificSynthesis(chainTypes[i], lipidBackboneFormula, chainFormula);
    }
    return lipidBackboneFormula;
  }

  // Chemical reactions
  private IMolecularFormula doChainTypeSpecificSynthesis(LipidChainType type,
      IMolecularFormula lipidBackbone, IMolecularFormula chainFormula) {
    return switch (type) {
      case ACYL_CHAIN -> doEsterBonding(lipidBackbone, chainFormula);
      case ALKYL_CHAIN -> doEtherBonding(lipidBackbone, chainFormula);
    };
  }

  // create ester bonding
  private IMolecularFormula doEsterBonding(IMolecularFormula backboneFormula,
      IMolecularFormula chainFormula) {
    IMolecularFormula secondaryProduct = FormulaUtils.createMajorIsotopeMolFormula("H2O");
    IMolecularFormula product = FormulaUtils.addFormula(backboneFormula, chainFormula);
    product = FormulaUtils.subtractFormula(product, secondaryProduct);
    return product;
  }

  // create ester bonding
  private IMolecularFormula doEtherBonding(IMolecularFormula backboneFormula,
      IMolecularFormula chainFormula) {
    IMolecularFormula secondaryProduct = FormulaUtils.createMajorIsotopeMolFormula("H2");
    IMolecularFormula product = FormulaUtils.addFormula(backboneFormula, chainFormula);
    product = FormulaUtils.subtractFormula(product, secondaryProduct);
    return product;
  }


}
