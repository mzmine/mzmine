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

package util.lipidannotationtest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;
import io.github.mzmine.util.FormulaUtils;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IMolecularFormula;

class LipidMsOneLevelTest {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();
  private static final NumberFormat NUMBER_FORMAT = new DecimalFormat("#.#####");

  private static void testLipid(String formula, LipidClasses freefattyacids, int numberOfCarbons,
      int numberOfDBEs) {
    Double mzExact = FormulaUtils.calculateExactMass(formula);
    IMolecularFormula testSpeciesMG = LIPID_FACTORY.buildSpeciesLevelLipid(freefattyacids,
        numberOfCarbons, numberOfDBEs, 0).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(mzExact), NUMBER_FORMAT.format(FormulaUtils.getMonoisotopicMass(testSpeciesMG)));
  }

  //Fatty Acyls
  @Test
  void molecularFormulaLevelTestFA() {
    testLipid("C16H32O2", LipidClasses.FREEFATTYACIDS, 16, 0);
  }

  @Test
  void molecularFormulaLevelTestFA_O() {
    testLipid("C18H30O3", LipidClasses.OXIDIZEDFREEFATTYACIDS, 18, 3);
  }

  @Test
  void molecularFormulaLevelTestFAHFA() {
    testLipid("C34H60O4", LipidClasses.FATTYACIDESTOLIDES, 34, 3);
  }

  @Test
  void molecularFormulaLevelTestCAR() {
    testLipid("C17H29NO4", LipidClasses.FATTYACYLCARNITINES, 10, 2);
  }

  //Glycerolipids
  @Test
  void molecularFormulaLevelTestMG() {
    testLipid("C21H40O4", LipidClasses.MONOACYLGLYCEROLS, 18, 1);
  }

  @Test
  void molecularFormulaLevelTestMG_O() {
    testLipid("C21H42O3", LipidClasses.MONOALKYLGLYCEROLS, 18, 1);
  }

  @Test
  void molecularFormulaLevelTestDG() {
    testLipid("C35H64O5", LipidClasses.DIACYLGLYCEROLS, 32, 2);
  }

  @Test
  void molecularFormulaLevelTestDG_O() {
    testLipid("C37H72O4", LipidClasses.ALKYLACYLGLYCEROLS, 34, 1);
  }

  @Test
  void molecularFormulaLevelTestDG_dO() {
    testLipid("C43H70O3", LipidClasses.DIALKYLGLYCEROLS, 40, 9);
  }

  @Test
  void molecularFormulaLevelTestTG() {
    testLipid("C59H106O6", LipidClasses.TRIACYLGLYCEROLS, 56, 4);
  }

  @Test
  void molecularFormulaLevelTestTG_O() {
    testLipid("C59H108O5", LipidClasses.ALKYLDIACYLGLYCEROLS, 56, 4);
  }

  @Test
  void molecularFormulaLevelTestDGTS() {
    testLipid("C44H81NO7", LipidClasses.DIACYLGLYCEROLTRIMETHYLHOMOSERIN, 34, 2);
  }

  @Test
  void molecularFormulaLevelTestLDGTS() {
    testLipid("C28H53O6N", LipidClasses.MONOACYLGLYCEROLTRIMETHYLHOMOSERIN, 18, 1);
  }

  @Test
  void molecularFormulaLevelTestMGDG() {
    testLipid("C45H74O10", LipidClasses.MONOGALACTOSYLDIACYLGLYCEROL, 36, 6);
  }

  @Test
  void molecularFormulaLevelTestDGDG() {
    testLipid("C51H80O15", LipidClasses.DIGALACTOSYLDIACYLGLYCEROL, 36, 8);
  }

  @Test
  void molecularFormulaLevelTestSQDG() {
    testLipid("C41H76O12S", LipidClasses.SULFOQUINOVOSYLDIACYLGLYCEROLS, 32, 1);
  }

  @Test
  void molecularFormulaLevelTestSQMG() {
    testLipid("C25H46O11S", LipidClasses.SULFOQUINOVOSYLMONOACYLGLYCEROLS, 16, 1);
  }

  // Glycerophospholipids

  @Test
  void molecularFormulaLevelTestPC() {
    testLipid("C40H76NO8P", LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES, 32, 2);
  }

  @Test
  void molecularFormulaLevelTestPC_O() {
    testLipid("C42H84NO7P", LipidClasses.ALKYLACYLGLYCEROPHOSPHOCHOLINES, 34, 1);
  }

  @Test
  void molecularFormulaLevelTestPC_dO() {
    testLipid("C48H82NO6P", LipidClasses.DIALKYLGLYCEROPHOSPHOCHOLINES, 40, 9);
  }

  @Test
  void molecularFormulaLevelTestLPC() {
    testLipid("C26H52NO7P", LipidClasses.MONOACYLGLYCEROPHOSPHOCHOLINES, 18, 1);
  }


  @Test
  void molecularFormulaLevelTestLPC_O() {
    testLipid("C24H50NO6P", LipidClasses.MONOALKYLGLYCEROPHOSPHOCHOLINES, 16, 1);
  }

  @Test
  void molecularFormulaLevelTestPE() {
    testLipid("C37H70NO8P", LipidClasses.DIACYLGLYCEROPHOSPHOETHANOLAMINES, 32, 2);
  }

  @Test
  void molecularFormulaLevelTestPE_O() {
    testLipid("C39H78NO7P", LipidClasses.ALKYLACYLGLYCEROPHOSPHOETHANOLAMINES, 34, 1);
  }

  @Test
  void molecularFormulaLevelTestPE_dO() {
    testLipid("C45H76NO6P", LipidClasses.DIALKYLGLYCEROPHOSPHOETHANOLAMINES, 40, 9);
  }

  @Test
  void molecularFormulaLevelTestLPE() {
    testLipid("C23H46NO7P", LipidClasses.MONOACYLGLYCEROPHOSPHOETHANOLAMINES, 18, 1);
  }

  @Test
  void molecularFormulaLevelTestLPE_O() {
    testLipid("C21H46NO6P", LipidClasses.MONOALKYLGLYCEROPHOSPHOETHANOLAMINES, 16, 0);
  }

  @Test
  void molecularFormulaLevelTestLNAPE() {
    testLipid("C39H74NO8P", LipidClasses.NACYLLYSOPHOSPHATIDYLETHANOLAMINE, 34, 2);
  }

  @Test
  void molecularFormulaLevelTestPS() {
    testLipid("C38H70NO10P", LipidClasses.DIACYLGLYCEROPHOSPHOSERINES, 32, 2);
  }

  @Test
  void molecularFormulaLevelTestPS_O() {
    testLipid("C40H78NO9P", LipidClasses.ALKYLACYLGLYCEROPHOSPHOSERINES, 34, 1);
  }

  @Test
  void molecularFormulaLevelTestLPS() {
    testLipid("C24H48NO9P", LipidClasses.MONOACYLGLYCEROPHOSPHOSERINES, 18, 0);
  }

  @Test
  void molecularFormulaLevelTestLPS_O() {
    testLipid("C22H46NO8P", LipidClasses.MONOALKYLGLYCEROPHOSPHOSERINES, 16, 0);
  }

  @Test
  void molecularFormulaLevelTestPG() {
    testLipid("C38H71O10P", LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROLS, 32, 2);
  }

  @Test
  void molecularFormulaLevelTestPG_O() {
    testLipid("C42H83O9P", LipidClasses.ALKYLACYLGLYCEROPHOSPHOGLYCEROLS, 36, 1);
  }

  @Test
  void molecularFormulaLevelTestPG_dO() {
    testLipid("C44H73O8P", LipidClasses.DIALKYLGLYCEROPHOSPHOGLYCEROLS, 38, 9);
  }

  @Test
  void molecularFormulaLevelTestLPG() {
    testLipid("C24H47O9P", LipidClasses.MONOACYLGLYCEROPHOSPHOGLYCEROLS, 18, 1);
  }

  @Test
  void molecularFormulaLevelTestLPG_O() {
    testLipid("C22H47O8P", LipidClasses.MONOALKYLGLYCEROPHOSPHOGLYCEROLS, 16, 0);
  }

  @Test
  void molecularFormulaLevelTestBMP() {
    testLipid("C38H71O10P", LipidClasses.MONOACYLGLYCEROPHOSPHOMONORADYLGLYCEROLS, 32, 2);
  }

  @Test
  void molecularFormulaLevelTestPI() {
    testLipid("C41H75O13P", LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 32, 2);
  }

  @Test
  void molecularFormulaLevelTestPI_O() {
    testLipid("C45H87O12P", LipidClasses.ALKYLACYLGLYCEROPHOSPHOINOSITOLS, 36, 1);
  }

  @Test
  void molecularFormulaLevelTestPI_dO() {
    testLipid("C50H101O11P", LipidClasses.DIALKYLGLYCEROPHOSPHOINOSITOLS, 41, 0);
  }

  @Test
  void molecularFormulaLevelTestLPI() {
    testLipid("C27H51O12P", LipidClasses.MONOACYLGLYCEROPHOSPHOINOSITOLS, 18, 1);
  }

  @Test
  void molecularFormulaLevelTestLPI_O() {
    testLipid("C29H59O11P", LipidClasses.MONOALKYLGLYCEROPHOSPHOINOSITOLS, 20, 0);
  }

  @Test
  void molecularFormulaLevelTestPA() {
    testLipid("C35H65O8P", LipidClasses.DIACYLGLYCEROPHOSPHATES, 32, 2);
  }

  @Test
  void molecularFormulaLevelTestPA_O() {
    testLipid("C35H69O7P", LipidClasses.ALKYLACYLGLYCEROPHOSPHATES, 32, 1);
  }

  @Test
  void molecularFormulaLevelTestLPA() {
    testLipid("C21H41O7P", LipidClasses.MONOACYLGLYCEROPHOSPHATES, 18, 1);
  }

  @Test
  void molecularFormulaLevelTestLPA_O() {
    testLipid("C23H49O6P", LipidClasses.MONOALKYLGLYCEROPHOSPHATES, 20, 0);
  }

  @Test
  void molecularFormulaLevelTestCL() {
    testLipid("C81H142O17P2", LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS, 72,
        8);
  }

  @Test
  void molecularFormulaLevelTestSPB_18_1_3O() {
    testLipid("C18H37NO3", LipidClasses.PHYTOSPHINGANINESANDPHYTOSPHINGOSINES, 18, 1);
  }

  @Test
  void molecularFormulaLevelTestSPB_18_1_2O() {
    testLipid("C18H37NO2", LipidClasses.SPHINGANINESANDSPHINGOSINES, 18, 1);
  }

  @Test
  void molecularFormulaLevelTestSM36_1_O2() {
    testLipid("C41H83N2O6P", LipidClasses.CERAMIDEPHOSPHOCHOLINES, 36, 1);
  }

  @Test
  void molecularFormulaLevelTestSM() {
    testLipid("C37H75N2O6P", LipidClasses.CERAMIDEPHOSPHOCHOLINES, 32, 1);
  }

  @Test
  void molecularFormulaLevelTestCer3O() {
    testLipid("C34H69NO4", LipidClasses.CERAMIDEANDDIHYDROCERAMIDEHYDROXYFATTYACID, 34, 0);
  }

  @Test
  void molecularFormulaLevelTestCerP() {
    testLipid("C30H60NO6P", LipidClasses.CERAMIDEPHOSPHATES, 30, 1);
  }

  @Test
  void molecularFormulaLevelTestCer_O3() {
    testLipid("C38H77NO4", LipidClasses.NACYLFOURHYDROXYPHINGANINES, 38, 0);
  }

  @Test
  void molecularFormulaLevelTestCer_O2() {
    testLipid("C32H63NO3", LipidClasses.NACYLSPHINGOSINESANDNACYLSPHINGANINES, 32, 1);
  }

  @Test
  void molecularFormulaLevelTestCer_O4() {
    testLipid("C44H89NO5", LipidClasses.PHYTOCERAMIDEHYDROXYFATTYACID, 44, 0);
  }

  @Test
  void molecularFormulaLevelTestHexCer() {
    testLipid("C40H79NO8", LipidClasses.HEXOSYLCERAMIDES, 34, 0);
  }


  @Test
  void molecularFormulaLevelTestHex2Cer() {
    testLipid("C46H87NO13", LipidClasses.DIHEXOSYLCERAMIDES, 34, 1);
  }

  @Test
  void molecularFormulaLevelTestHex3Cer() {
    testLipid("C52H97NO18", LipidClasses.TRIHEXOSYLCERAMIDES, 34, 1);
  }

  @Test
  void molecularFormulaLevelTestHexCerO3() {
    testLipid("C40H77NO9", LipidClasses.HEXOSYLCERAMIDEHYDROXYFATTYACID, 34, 1);
  }

  @Test
  void molecularFormulaLevelTestHexCerO4() {
    testLipid("C41H77NO10", LipidClasses.HEXOSYLCERAMIDEHYDROXYFATTYACIDPHYTOSPHINGOSINE, 35, 2);
  }

  @Test
  void molecularFormulaLevelTestSM_O3() {
    testLipid("C39H79N2O7P", LipidClasses.OXIDIZEDCERAMIDEPHOSPHOCHOLINES, 34, 1);
  }

  //Sterol lipids
  @Test
  void molecularFormulaLevelTestCE_16_2() {
    testLipid("C43H72O2", LipidClasses.CHOLESTEROLESTERS, 16, 2);
  }

}
