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

    if (lipidClass.getChainTypes().length == 0) {
      hasNoChain = true;
    } else {
      for (LipidChainType type : lipidClass.getChainTypes()) {
        if (type.equals(LipidChainType.ALKYL_CHAIN)) {
          hasAlkylChain = true;
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
    return new SpeciesLevelAnnotation(lipidClass, annotation, molecularFormula, numberOfCarbons,
        numberOfDBEs);
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
    return new MolecularSpeciesLevelAnnotation(lipidClass, annotation, molecularFormula,
        lipidChains);
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
    return new MolecularSpeciesLevelAnnotation(lipidClass, annotation, molecularFormula,
        lipidChains);
  }


  // lipid synthesis
  public IMolecularFormula synthesisLipidMolecularFormula(String lipidBackbone, int chainLength,
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
      lipidBackboneFormula =
          doChainTypeSpecificSynthesis(chainTypes[i], lipidBackboneFormula, chainFormula);
    }
    return lipidBackboneFormula;
  }

  // Chemical reactions
  private IMolecularFormula doChainTypeSpecificSynthesis(LipidChainType type,
      IMolecularFormula lipidBackbone, IMolecularFormula chainFormula) {
    switch (type) {
      case ACYL_CHAIN:
        return doEsterBonding(lipidBackbone, chainFormula);
      case ALKYL_CHAIN:
        return doEtherBonding(lipidBackbone, chainFormula);
      default:
        return null;
    }
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
