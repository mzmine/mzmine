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

package io.github.mzmine.modules.dataprocessing.id_lipidid.utils;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.MolecularSpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.SpeciesLevelAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.ILipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.ILipidChain;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.util.FormulaUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class LipidFactory {

  private static final Logger logger = Logger.getLogger(LipidFactory.class.getName());
  private static final LipidChainFactory LIPID_CHAIN_FACTORY = new LipidChainFactory();

  public SpeciesLevelAnnotation buildSpeciesLevelLipid(ILipidClass lipidClass, int numberOfCarbons,
      int numberOfDBEs, int numberOfAdditionalOxygens) {
    int numberOfOxygens =
        Arrays.stream(lipidClass.getChainTypes()).mapToInt(LipidChainType::getFixNumberOfOxygens)
            .sum() + numberOfAdditionalOxygens;
    String annotation = buildAnnotation(lipidClass, numberOfCarbons, numberOfDBEs, numberOfOxygens);
    if (annotation == null) {
      return null;
    }
    IMolecularFormula molecularFormula = synthesisLipidMolecularFormula(
        lipidClass.getBackBoneFormula(), numberOfCarbons, numberOfDBEs, numberOfAdditionalOxygens,
        lipidClass.getChainTypes());
    if (molecularFormula != null) {
      return new SpeciesLevelAnnotation(lipidClass, annotation, molecularFormula, numberOfCarbons,
          numberOfDBEs, numberOfOxygens);
    } else {
      return null;
    }
  }

  @Nullable
  public static String buildAnnotation(ILipidClass lipidClass, int numberOfCarbons,
      int numberOfDBEs, int numberOfOxygens) {
    if (lipidClass.getCoreClass() != null) {
      switch (lipidClass.getCoreClass()) {

        case FATTYACYLS -> {
          return buildGlyceroAndGlycerophospholipidSpeciesAnnotation(lipidClass, numberOfCarbons,
              numberOfDBEs, numberOfOxygens);
        }
        case GLYCEROLIPIDS -> {
          return buildGlyceroAndGlycerophospholipidSpeciesAnnotation(lipidClass, numberOfCarbons,
              numberOfDBEs, numberOfOxygens);
        }
        case GLYCEROPHOSPHOLIPIDS -> {
          return buildGlyceroAndGlycerophospholipidSpeciesAnnotation(lipidClass, numberOfCarbons,
              numberOfDBEs, numberOfOxygens);
        }
        case SPHINGOLIPIDS -> {
          return buildSphingolipidSpeciesAnnotation(lipidClass, numberOfCarbons, numberOfDBEs,
              numberOfOxygens);
        }
        case STEROLLIPIDS -> {
          return buildGlyceroAndGlycerophospholipidSpeciesAnnotation(lipidClass, numberOfCarbons,
              numberOfDBEs, numberOfOxygens);
        }
        case PRENOLLIPIDS -> {
          return "TODO Lipid Annotation formate";
        }
        case SACCHAROLIPIDS -> {
          return "TODO Lipid Annotation formate";
        }
        case POLYKETIDES -> {
          return "TODO Lipid Annotation formate";
        }
      }
    }
    return "No Annotation";
  }

  @Nullable
  private static String buildGlyceroAndGlycerophospholipidSpeciesAnnotation(ILipidClass lipidClass,
      int numberOfCarbons, int numberOfDBEs, int numberOfOxygens) {
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

    if (numberOfOxygens == 1) {
      annotation = annotation + ";O";
    } else if (numberOfOxygens > 0) {
      annotation = annotation + ";O" + numberOfOxygens;
    }
    return annotation;
  }

  @Nullable
  private static String buildSphingolipidSpeciesAnnotation(ILipidClass lipidClass,
      int numberOfCarbons, int numberOfDBEs, int numberOfOxygens) {
    String annotation;

    if (numberOfCarbons < lipidClass.getChainTypes().length) {
      return null;
    }
    annotation = lipidClass.getAbbr() + " " + numberOfCarbons + ':' + numberOfDBEs;
    if (numberOfOxygens == 1) {
      annotation = annotation + ";O";
    } else if (numberOfOxygens > 0) {
      annotation = annotation + ";O" + numberOfOxygens;
    }
    return annotation;
  }

  public MolecularSpeciesLevelAnnotation buildMolecularSpeciesLevelLipid(ILipidClass lipidClass,
      int[] numberOfCarbons, int[] numberOfDBEs, int[] numberOfAdditionalOxygens) {
    List<ILipidChain> lipidChains = new ArrayList<>();
    LipidChainType[] lipidChainTypes = lipidClass.getChainTypes();
    for (int i = 0; i < lipidChainTypes.length; i++) {
      lipidChains.add(LIPID_CHAIN_FACTORY.buildLipidChain(lipidChainTypes[i], numberOfCarbons[i],
          numberOfDBEs[i]));
    }
    int totalNumberOfCarbons = lipidChains.stream().mapToInt(ILipidChain::getNumberOfCarbons).sum();
    int totalNumberOfDBEs = lipidChains.stream().mapToInt(ILipidChain::getNumberOfDBEs).sum();
    int totalNumberOfAdditionalOxygens = Arrays.stream(numberOfAdditionalOxygens).sum();
    String annotation =
        lipidClass.getAbbr() + " " + LIPID_CHAIN_FACTORY.connectLipidChainAnnotations(lipidChains);
    IMolecularFormula molecularFormula = synthesisLipidMolecularFormula(
        lipidClass.getBackBoneFormula(), totalNumberOfCarbons, totalNumberOfDBEs,
        totalNumberOfAdditionalOxygens, lipidClass.getChainTypes());
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

    //Additional oxygens must be handled in chains for MolecularSpeciesLevel
    IMolecularFormula molecularFormula = synthesisLipidMolecularFormula(
        lipidClass.getBackBoneFormula(), totalNumberOfCarbons, totalNumberOfDBEs, 0,
        lipidClass.getChainTypes());
    if (molecularFormula != null) {
      return new MolecularSpeciesLevelAnnotation(lipidClass, annotation, molecularFormula,
          lipidChains);
    } else {
      return null;
    }
  }


  // lipid synthesis
  private IMolecularFormula synthesisLipidMolecularFormula(String lipidBackbone, int chainLength,
      int chainDoubleBonds, int numberOfAdditionalOxygens, LipidChainType[] chainTypes) {

    IMolecularFormula lipidFormula = null;
    IMolecularFormula lipidBackboneFormula = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        lipidBackbone);

    int numberOfCarbonsPerChain = chainLength / chainTypes.length;
    int restCarbons = chainLength % chainTypes.length;
    int numberOfDoubleBondsPerChain = chainDoubleBonds / chainTypes.length;
    int restDoubleBonds = chainDoubleBonds % chainTypes.length;

    // build chains
    for (int i = 0; i < chainTypes.length; i++) {

      // add rests to last chain
      if (i == chainTypes.length - 1) {
        numberOfCarbonsPerChain = numberOfCarbonsPerChain + restCarbons;
        numberOfDoubleBondsPerChain = numberOfDoubleBondsPerChain + restDoubleBonds;
      }
      IMolecularFormula chainFormula = LIPID_CHAIN_FACTORY.buildLipidChainFormula(chainTypes[i],
          numberOfCarbonsPerChain, numberOfDoubleBondsPerChain);
      if (chainFormula == null) {
        return null;
      }
      lipidBackboneFormula = doChainTypeSpecificSynthesis(chainTypes[i], lipidBackboneFormula,
          chainFormula);
      if (lipidBackboneFormula == null) {
        return null;
      }
    }
    // add additional oxygens
    if (numberOfAdditionalOxygens != 0) {
      lipidFormula = FormulaUtils.addFormula(lipidBackboneFormula,
          FormulaUtils.createMajorIsotopeMolFormulaWithCharge(numberOfAdditionalOxygens + "O"));
    } else {
      lipidFormula = lipidBackboneFormula;
    }
    return lipidFormula;
  }

  // Chemical reactions
  @Nullable
  private IMolecularFormula doChainTypeSpecificSynthesis(@NotNull LipidChainType type,
      @NotNull IMolecularFormula lipidBackbone, @NotNull IMolecularFormula chainFormula) {
    return switch (type) {
      case ACYL_CHAIN -> doEsterBonding(lipidBackbone, chainFormula);
      case ACYL_MONO_HYDROXY_CHAIN -> doEsterBonding(lipidBackbone, chainFormula);
      case TWO_ACYL_CHAINS_COMBINED -> doEsterBonding(lipidBackbone, chainFormula);
      case ALKYL_CHAIN -> doEtherBonding(lipidBackbone, chainFormula);
      case AMID_CHAIN -> doAmidBonding(lipidBackbone, chainFormula);
      case AMID_MONO_HYDROXY_CHAIN -> doAmidBonding(lipidBackbone, chainFormula);
      case SPHINGOLIPID_MONO_HYDROXY_BACKBONE_CHAIN ->
          doSphingolipidBonding(lipidBackbone, chainFormula);
      case SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN ->
          doSphingolipidBonding(lipidBackbone, chainFormula);
      case SPHINGOLIPID_TRI_HYDROXY_BACKBONE_CHAIN ->
          doSphingolipidBonding(lipidBackbone, chainFormula);
    };
  }

  // create ester bonding
  @Nullable
  private IMolecularFormula doEsterBonding(@NotNull IMolecularFormula backboneFormula,
      @NotNull IMolecularFormula chainFormula) {
    return combineFormulasRemoveLast(backboneFormula, chainFormula, "H2O");
  }

  // create ether bonding
  @Nullable
  private IMolecularFormula doEtherBonding(@NotNull IMolecularFormula backboneFormula,
      @NotNull IMolecularFormula chainFormula) {
    return combineFormulasRemoveLast(backboneFormula, chainFormula, "H2");
  }

  @Nullable
  private IMolecularFormula doAmidBonding(@NotNull IMolecularFormula backboneFormula,
      @NotNull IMolecularFormula chainFormula) {
    return combineFormulasRemoveLast(backboneFormula, chainFormula, "H2");
  }

  @Nullable
  private IMolecularFormula doSphingolipidBonding(@NotNull IMolecularFormula backboneFormula,
      @NotNull IMolecularFormula chainFormula) {
    //remove H and Sphingolipid backbone (C3H8N + H) atoms from Formula
    return combineFormulasRemoveLast(backboneFormula, chainFormula, "C3H9N");
  }

  /**
   * Bonds backbone and chain and releases the secondary product of the condensation reaction.
   *
   * @return null if the secondary product cannot be removed from the bonded formula, e.g. because
   * the chain or backbone formula is too small
   */
  @Nullable
  private IMolecularFormula combineFormulasRemoveLast(@NotNull IMolecularFormula backboneFormula,
      @NotNull IMolecularFormula chainFormula, @NotNull String toRemove) {
    final IMolecularFormula product = FormulaUtils.addFormula(backboneFormula, chainFormula);
    final Optional<IMolecularFormula> result = FormulaUtils.subtractFormula(product,
        FormulaUtils.createMajorIsotopeMolFormulaWithCharge(toRemove));
    if (result.isEmpty()) {
      logSynthesisFailure(product, toRemove);
      return null;
    }
    return result.get();
  }

  private static void logSynthesisFailure(final @NotNull IMolecularFormula product,
      final @NotNull String secondaryProduct) {
    logger.finest(() -> "Cannot synthesize lipid formula: %s cannot be removed from %s".formatted(
        secondaryProduct, FormulaUtils.getFormulaString(product)));
  }

}
