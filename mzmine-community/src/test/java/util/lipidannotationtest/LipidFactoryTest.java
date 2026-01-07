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
