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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipidutils.LipidFactory;
import io.github.mzmine.util.FormulaUtils;

class LipidMsOneLevelTest {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();
  private static final NumberFormat NUMBER_FORMAT = new DecimalFormat("#.#####");

  @Test
  void molecularFormulaLevelTestMG() {
    Double EXACT_MASS_MG_18_1 = FormulaUtils.calculateExactMass("C21H40O4");
    IMolecularFormula testSpeciesMG = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROLS, 18, 1).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_MG_18_1), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesMG, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestMG_O() {
    Double EXACT_MASS_MG_O_18_1 = FormulaUtils.calculateExactMass("C21H42O3");
    IMolecularFormula testSpeciesMG = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.MONOALKYLGLYCEROLS, 18, 1).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_MG_O_18_1), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesMG, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestDG() {
    Double EXACT_MASS_DG_32_2 = FormulaUtils.calculateExactMass("C35H64O5");
    IMolecularFormula testSpeciesDG = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROLS, 32, 2).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_DG_32_2), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesDG, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestDG_O() {
    Double EXACT_MASS_DG_O_34_1 = FormulaUtils.calculateExactMass("C37H72O4");
    IMolecularFormula testSpeciesDG_O = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROLS, 34, 1).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_DG_O_34_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesDG_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestDG_dO() {
    Double EXACT_MASS_DG_dO_40_9 = FormulaUtils.calculateExactMass("C43H70O3");
    IMolecularFormula testSpeciesDG_dO = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.DIALKYLGLYCEROLS, 40, 9).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_DG_dO_40_9),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesDG_dO,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestTG() {
    Double EXACT_MASS_TG_56_4 = FormulaUtils.calculateExactMass("C59H106O6");
    IMolecularFormula testSpeciesTG = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.TRIACYLGLYCEROLS, 56, 4).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_TG_56_4), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesTG, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestTG_O() {
    Double EXACT_MASS_TG_O_56_4 = FormulaUtils.calculateExactMass("C59H108O5");
    IMolecularFormula testSpeciesTG = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.ALKYLDIACYLGLYCEROLS, 56, 4).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_TG_O_56_4), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesTG, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestDGTS() {
    Double EXACT_MASS_DGTS_34_2 = FormulaUtils.calculateExactMass("C44H81NO7");
    IMolecularFormula testSpeciesDGTS =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROLTRIMETHYLHOMOSERIN, 34, 2)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_DGTS_34_2),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesDGTS,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestLDGTS() {
    Double EXACT_MASS_LDGTS_18_1 = FormulaUtils.calculateExactMass("C28H53O6N");
    IMolecularFormula testSpeciesLDGTS =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROLTRIMETHYLHOMOSERIN, 18, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LDGTS_18_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLDGTS,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestMGDG() {
    Double EXACT_MASS_MGDG_36_6 = FormulaUtils.calculateExactMass("C45H74O10");
    IMolecularFormula testSpeciesMGDG =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOGALACTOSYLDIACYLGLYCEROL, 36, 6)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_MGDG_36_6),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesMGDG,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestDGDG() {
    Double EXACT_MASS_DGDG_36_8 = FormulaUtils.calculateExactMass("C51H80O15");
    IMolecularFormula testSpeciesDGDG =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIGALACTOSYLDIACYLGLYCEROL, 36, 8)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_DGDG_36_8),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesDGDG,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestSQDG() {
    Double EXACT_MASS_SQDG_32_1 = FormulaUtils.calculateExactMass("C41H76O12S");
    IMolecularFormula testSpeciesSQDG =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.SULFOQUINOVOSYLDIACYLGLYCEROLS, 32, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_SQDG_32_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesSQDG,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestSQMG() {
    Double EXACT_MASS_SQMG_16_1 = FormulaUtils.calculateExactMass("C25H46O11S");
    IMolecularFormula testSpeciesSQMG =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.SULFOQUINOVOSYLMONOACYLGLYCEROLS, 16, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_SQMG_16_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesSQMG,
            AtomContainerManipulator.MonoIsotopic)));
  }

  // Glycerophospholipids

  @Test
  void molecularFormulaLevelTestPC() {
    Double EXACT_MASS_PC_32_2 = FormulaUtils.calculateExactMass("C40H76NO8P");
    IMolecularFormula testSpeciesPC =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES, 32, 2)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PC_32_2), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesPC, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPC_O() {
    Double EXACT_MASS_PC_O_34_1 = FormulaUtils.calculateExactMass("C42H84NO7P");
    IMolecularFormula testSpeciesPC_O =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOCHOLINES, 34, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PC_O_34_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPC_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPC_dO() {
    Double EXACT_MASS_PC_dO_40_9 = FormulaUtils.calculateExactMass("C48H82NO6P");
    IMolecularFormula testSpeciesPC_dO =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIALKYLGLYCEROPHOSPHOCHOLINES, 40, 9)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PC_dO_40_9),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPC_dO,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestLPC() {
    Double EXACT_MASS_LPC_18_1 = FormulaUtils.calculateExactMass("C26H52NO7P");
    IMolecularFormula testSpeciesLPC =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOCHOLINES, 18, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPC_18_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPC,
            AtomContainerManipulator.MonoIsotopic)));
  }


  @Test
  void molecularFormulaLevelTestLPC_O() {
    Double EXACT_MASS_LPC_O_16_1 = FormulaUtils.calculateExactMass("C24H50NO6P");
    IMolecularFormula testSpeciesLPC_O =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOALKYLGLYCEROPHOSPHOCHOLINES, 16, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPC_O_16_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPC_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPE() {
    Double EXACT_MASS_PE_32_2 = FormulaUtils.calculateExactMass("C37H70NO8P");
    IMolecularFormula testSpeciesPE =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOETHANOLAMINES, 32, 2)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PE_32_2), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesPE, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPE_O() {
    Double EXACT_MASS_PE_O_34_1 = FormulaUtils.calculateExactMass("C39H78NO7P");
    IMolecularFormula testSpeciesPE_O = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOETHANOLAMINES, 34, 1)
        .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PE_O_34_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPE_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPE_dO() {
    Double EXACT_MASS_PE_dO_40_9 = FormulaUtils.calculateExactMass("C45H76NO6P");
    IMolecularFormula testSpeciesPE_dO =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIALKYLGLYCEROPHOSPHOETHANOLAMINES, 40, 9)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PE_dO_40_9),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPE_dO,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestLPE() {
    Double EXACT_MASS_LPE_18_1 = FormulaUtils.calculateExactMass("C23H46NO7P");
    IMolecularFormula testSpeciesLPE = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOETHANOLAMINES, 18, 1)
        .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPE_18_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPE,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestLPE_O() {
    Double EXACT_MASS_LPE_O_16_0 = FormulaUtils.calculateExactMass("C21H46NO6P");
    IMolecularFormula testSpeciesLPE_O = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.MONOALKYLGLYCEROPHOSPHOETHANOLAMINES, 16, 0)
        .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPE_O_16_0),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPE_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPS() {
    Double EXACT_MASS_PS_32_2 = FormulaUtils.calculateExactMass("C38H70NO10P");
    IMolecularFormula testSpeciesPS =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOSERINES, 32, 2)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PS_32_2), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesPS, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPS_O() {
    Double EXACT_MASS_PS_O_34_1 = FormulaUtils.calculateExactMass("C40H78NO9P");
    IMolecularFormula testSpeciesPS_O =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOSERINES, 34, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PS_O_34_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPS_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestLPS() {
    Double EXACT_MASS_LPS_18_0 = FormulaUtils.calculateExactMass("C24H48NO9P");
    IMolecularFormula testSpeciesLPI =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOSERINES, 18, 0)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPS_18_0),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPI,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestLPS_O() {
    Double EXACT_MASS_LPS_16_0 = FormulaUtils.calculateExactMass("C22H46NO8P");
    IMolecularFormula testSpeciesLPI =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOALKYLGLYCEROPHOSPHOSERINES, 16, 0)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPS_16_0),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPI,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPG() {
    Double EXACT_MASS_PG_32_2 = FormulaUtils.calculateExactMass("C38H71O10P");
    IMolecularFormula testSpeciesPG =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROLS, 32, 2)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PG_32_2), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesPG, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPG_O() {
    Double EXACT_MASS_PG_O_36_1 = FormulaUtils.calculateExactMass("C42H83O9P");
    IMolecularFormula testSpeciesPG_O =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOGLYCEROLS, 36, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PG_O_36_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPG_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPG_dO() {
    Double EXACT_MASS_PG_dO_38_9 = FormulaUtils.calculateExactMass("C44H73O8P");
    IMolecularFormula testSpeciesPG_dO =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIALKYLGLYCEROPHOSPHOGLYCEROLS, 38, 9)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PG_dO_38_9),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPG_dO,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestLPG() {
    Double EXACT_MASS_LPG_18_1 = FormulaUtils.calculateExactMass("C24H47O9P");
    IMolecularFormula testSpeciesLPG =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOGLYCEROLS, 18, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPG_18_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPG,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestLPG_O() {
    Double EXACT_MASS_LPG_O_16_0 = FormulaUtils.calculateExactMass("C22H47O8P");
    IMolecularFormula testSpeciesLPE_O =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOALKYLGLYCEROPHOSPHOGLYCEROLS, 16, 0)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPG_O_16_0),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPE_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestBMP() {
    Double EXACT_MASS_BMP_32_2 = FormulaUtils.calculateExactMass("C38H71O10P");
    IMolecularFormula testSpeciesBMP = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOMONORADYLGLYCEROLS, 32, 2)
        .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_BMP_32_2),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesBMP,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPI() {
    Double EXACT_MASS_PI_32_2 = FormulaUtils.calculateExactMass("C41H75O13P");
    IMolecularFormula testSpeciesPI =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 32, 2)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PI_32_2), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesPI, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPI_O() {
    Double EXACT_MASS_PI_O_36_1 = FormulaUtils.calculateExactMass("C45H87O12P");
    IMolecularFormula testSpeciesPI_O =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOINOSITOLS, 36, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PI_O_36_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPI_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPI_dO() {
    Double EXACT_MASS_PI_dO_41_0 = FormulaUtils.calculateExactMass("C50H101O11P");
    IMolecularFormula testSpeciesPI_dO =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIALKYLGLYCEROPHOSPHOINOSITOLS, 41, 0)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PI_dO_41_0),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPI_dO,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestLPI() {
    Double EXACT_MASS_LPI_18_1 = FormulaUtils.calculateExactMass("C27H51O12P");
    IMolecularFormula testSpeciesLPI =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOINOSITOLS, 18, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPI_18_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPI,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestLPI_O() {
    Double EXACT_MASS_LPI_O_20_0 = FormulaUtils.calculateExactMass("C29H59O11P");
    IMolecularFormula testSpeciesLPI =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOALKYLGLYCEROPHOSPHOINOSITOLS, 20, 0)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPI_O_20_0),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPI,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPA() {
    Double EXACT_MASS_PA_32_2 = FormulaUtils.calculateExactMass("C35H65O8P");
    IMolecularFormula testSpeciesPA = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHATES, 32, 2).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PA_32_2), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesPA, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestPA_O() {
    Double EXACT_MASS_PA_O_32_1 = FormulaUtils.calculateExactMass("C35H69O7P");
    IMolecularFormula testSpeciesPA_O =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHATES, 32, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PA_O_32_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPA_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestLPA() {
    Double EXACT_MASS_LPA_18_1 = FormulaUtils.calculateExactMass("C21H41O7P");
    IMolecularFormula testSpeciesLPA =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHATES, 18, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPA_18_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPA,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestLPA_O() {
    Double EXACT_MASS_LPA_O_20_0 = FormulaUtils.calculateExactMass("C23H49O6P");
    IMolecularFormula testSpeciesLPA_O =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOALKYLGLYCEROPHOSPHATES, 20, 0)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPA_O_20_0),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPA_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  void molecularFormulaLevelTestCL() {
    Double EXACT_MASS_CL_72_8 = FormulaUtils.calculateExactMass("C81H142O17P2");
    IMolecularFormula testSpeciesCL = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS, 72,
            8)
        .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_CL_72_8), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesCL, AtomContainerManipulator.MonoIsotopic)));
  }

}
