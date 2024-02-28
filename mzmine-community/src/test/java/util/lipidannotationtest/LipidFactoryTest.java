package util.lipidannotationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainFactory;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.lipidchain.LipidChainType;
import io.github.mzmine.util.FormulaUtils;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public class LipidFactoryTest {

  private static final LipidChainFactory LIPID_CHAIN_FACTORY = new LipidChainFactory();
  private static final NumberFormat NUMBER_FORMAT = new DecimalFormat("#.#####");

  @Test
  void testAcylChain() {
    testLipidChainFormula(LipidChainType.ACYL_CHAIN, 18, 1, "C18H34O2");
  }

  @Test
  void testAcylMonoHydroxyChain() {
    testLipidChainFormula(LipidChainType.ACYL_MONO_HYDROXY_CHAIN, 14, 0, "C14H28O3");
  }

  @Test
  void testAlkylChain() {
    testLipidChainFormula(LipidChainType.ALKYL_CHAIN, 12, 0, "C12H26");
  }

  @Test
  void testTwoAcylChainsCombined() {
    testLipidChainFormula(LipidChainType.TWO_ACYL_CHAINS_COMBINED, 18, 1, "C18H34O2");
  }

  @Test
  void testAmidChain() {
    testLipidChainFormula(LipidChainType.AMID_CHAIN, 18, 1, "C18H35NO ");
  }

  @Test
  void testAmidMonoHydroxyChain() {
    testLipidChainFormula(LipidChainType.AMID_MONO_HYDROXY_CHAIN, 18, 1, "C18H35NO2 ");
  }

  @Test
  void testSphingolipidDiHydroxyBackboneChain() {
    //Sphinganine
    testLipidChainFormula(LipidChainType.SPHINGOLIPID_DI_HYDROXY_BACKBONE_CHAIN, 18, 0,
        "C18H39NO2");
  }

  @Test
  void testLipidPolarityAndIonNotation() {
    LipidClasses[] lipidClasses = LipidClasses.values();
    for (LipidClasses lipidClass : lipidClasses) {
      LipidFragmentationRule[] lipidClassFragmentationRules = lipidClass.getFragmentationRules();
      for (LipidFragmentationRule lipidFragmentationRule : lipidClassFragmentationRules) {
        PolarityType polarityType = lipidFragmentationRule.getPolarityType();
        PolarityType polarityTypeFromIonNotation = lipidFragmentationRule.getIonizationType()
            .getPolarity();
        assertEquals(polarityTypeFromIonNotation, polarityType,
            lipidClass.getAbbr() + " " + lipidClass.getName() + " "
                + lipidFragmentationRule.toString() + " detected polarity is " + polarityType
                + " but should be " + polarityTypeFromIonNotation);
      }
    }
  }

  private void testLipidChainFormula(LipidChainType chainType, int carbonAtoms, int doubleBonds,
      String formulaString) {
    Double expectedMass = FormulaUtils.calculateExactMass(formulaString);
    IMolecularFormula testChain = LIPID_CHAIN_FACTORY.buildLipidChainFormula(chainType, carbonAtoms,
        doubleBonds);
    assertEquals(NUMBER_FORMAT.format(expectedMass), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testChain, AtomContainerManipulator.MonoIsotopic)));
  }

}
