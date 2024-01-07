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
