package lipidannotationtest;

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

public class LipidMsOneLevelTest {

  private static final LipidFactory LIPID_FACTORY = new LipidFactory();
  private static final NumberFormat NUMBER_FORMAT = new DecimalFormat("#.#####");

  @Test
  public void molecularFormulaLevelTestMG() {
    Double EXACT_MASS_MG_18_1 = FormulaUtils.calculateExactMass("C21H40O4");
    IMolecularFormula testSpeciesMG = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROLS, 18, 1).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_MG_18_1), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesMG, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestDG() {
    Double EXACT_MASS_DG_32_2 = FormulaUtils.calculateExactMass("C35H64O5");
    IMolecularFormula testSpeciesDG = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROLS, 32, 2).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_DG_32_2), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesDG, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestDG_O() {
    Double EXACT_MASS_DG_O_34_1 = FormulaUtils.calculateExactMass("C37H72O4");
    IMolecularFormula testSpeciesDG_O = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROLS, 34, 1).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_DG_O_34_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesDG_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestTG() {
    Double EXACT_MASS_TG_56_4 = FormulaUtils.calculateExactMass("C59H106O6");
    IMolecularFormula testSpeciesTG = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.TRIACYLGLYCEROLS, 56, 4).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_TG_56_4), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesTG, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestTG_O() {
    Double EXACT_MASS_TG_O_56_4 = FormulaUtils.calculateExactMass("C59H108O5");
    IMolecularFormula testSpeciesTG = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.ALKYLDIACYLGLYCEROLS, 56, 4).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_TG_O_56_4), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesTG, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestDGDG() {
    Double EXACT_MASS_DGDG_36_8 = FormulaUtils.calculateExactMass("C51H80O15");
    IMolecularFormula testSpeciesDGDG =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIGALACTOSYLDIACYLGLYCEROL, 36, 8)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_DGDG_36_8),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesDGDG,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestMGDG() {
    Double EXACT_MASS_MGDG_36_6 = FormulaUtils.calculateExactMass("C45H74O10");
    IMolecularFormula testSpeciesMGDG =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOGALACTOSYLDIACYLGLYCEROL, 36, 6)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_MGDG_36_6),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesMGDG,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestDGTS() {
    Double EXACT_MASS_DGTS_34_2 = FormulaUtils.calculateExactMass("C44H81NO7");
    IMolecularFormula testSpeciesDGTS =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROLTRIMETHYLHOMOSERIN, 34, 2)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_DGTS_34_2),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesDGTS,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestLDGTS() {
    Double EXACT_MASS_LDGTS_18_1 = FormulaUtils.calculateExactMass("C28H53O6N");
    IMolecularFormula testSpeciesLDGTS =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROLTRIMETHYLHOMOSERIN, 18, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LDGTS_18_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLDGTS,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestSQDG() {
    Double EXACT_MASS_SQDG_32_1 = FormulaUtils.calculateExactMass("C41H76O12S");
    IMolecularFormula testSpeciesSQDG =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.SULFOQUINOVOSYLDIACYLGLYCEROLS, 32, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_SQDG_32_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesSQDG,
            AtomContainerManipulator.MonoIsotopic)));
  }

  // Glycerophospholipids

  @Test
  public void molecularFormulaLevelTestPC_O() {
    Double EXACT_MASS_PC_O_34_1 = FormulaUtils.calculateExactMass("C42H84NO7P");
    IMolecularFormula testSpeciesPC_O =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOCHOLINES, 34, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PC_O_34_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPC_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestPE_O() {
    Double EXACT_MASS_PE_O_34_1 = FormulaUtils.calculateExactMass("C39H78NO7P");
    IMolecularFormula testSpeciesPE_O = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOETHANOLAMINES, 34, 1)
        .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PE_O_34_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPE_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestPG_O() {
    Double EXACT_MASS_PG_O_36_1 = FormulaUtils.calculateExactMass("C42H83O9P");
    IMolecularFormula testSpeciesPG_O =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOGLYCEROLS, 36, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PG_O_36_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPG_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestPS_O() {
    Double EXACT_MASS_PS_O_34_1 = FormulaUtils.calculateExactMass("C40H78NO9P");
    IMolecularFormula testSpeciesPS_O =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOSERINES, 34, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PS_O_34_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPS_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestPI_O() {
    Double EXACT_MASS_PI_O_36_1 = FormulaUtils.calculateExactMass("C45H87O12P");
    IMolecularFormula testSpeciesPI_O =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.ALKYLACYLGLYCEROPHOSPHOINOSITOLS, 36, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PI_O_36_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesPI_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestPA() {
    Double EXACT_MASS_PA_32_2 = FormulaUtils.calculateExactMass("C35H65O8P");
    IMolecularFormula testSpeciesPA = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHATES, 32, 2).getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PA_32_2), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesPA, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestPC() {
    Double EXACT_MASS_PC_32_2 = FormulaUtils.calculateExactMass("C40H76NO8P");
    IMolecularFormula testSpeciesPC =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES, 32, 2)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PC_32_2), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesPC, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestPE() {
    Double EXACT_MASS_PE_32_2 = FormulaUtils.calculateExactMass("C37H70NO8P");
    IMolecularFormula testSpeciesPE =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOETHANOLAMINES, 32, 2)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PE_32_2), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesPE, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestPG() {
    Double EXACT_MASS_PG_32_2 = FormulaUtils.calculateExactMass("C38H71O10P");
    IMolecularFormula testSpeciesPG =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROLS, 32, 2)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PG_32_2), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesPG, AtomContainerManipulator.MonoIsotopic)));
  }

  // TODO add BMP
  // @Test
  // public void molecularFormulaLevelTestBMP() {
  // Double EXACT_MASS_BMP_32_2 = FormulaUtils.calculateExactMass("C38H71O10P");
  // IMolecularFormula testSpeciesBMP =
  // LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.Monoacylglycerophospho}, 32, 32, 2, 2)
  // .getMolecularFormula();
  // assertEquals(NUMBER_FORMAT.format(EXACT_MASS_BMP_32_2,
  // NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesBMP,
  // AtomContainerManipulator.MonoIsotopic)));
  // }

  @Test
  public void molecularFormulaLevelTestCL() {
    Double EXACT_MASS_CL_72_8 = FormulaUtils.calculateExactMass("C81H142O17P2");
    IMolecularFormula testSpeciesCL = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOGLYCEROPHOSPHODIRADYLGLYCEROLS, 72,
            8)
        .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_CL_72_8), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesCL, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestPI() {
    Double EXACT_MASS_PI_32_2 = FormulaUtils.calculateExactMass("C41H75O13P");
    IMolecularFormula testSpeciesPI =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOINOSITOLS, 32, 2)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PI_32_2), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesPI, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestPS() {
    Double EXACT_MASS_PS_32_2 = FormulaUtils.calculateExactMass("C38H70NO10P");
    IMolecularFormula testSpeciesPS =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOSERINES, 32, 2)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_PS_32_2), NUMBER_FORMAT.format(
        MolecularFormulaManipulator.getMass(testSpeciesPS, AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestLPA() {
    Double EXACT_MASS_LPA_18_1 = FormulaUtils.calculateExactMass("C21H41O7P");
    IMolecularFormula testSpeciesLPA =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHATES, 18, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPA_18_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPA,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestLPC() {
    Double EXACT_MASS_LPC_18_1 = FormulaUtils.calculateExactMass("C26H52NO7P");
    IMolecularFormula testSpeciesLPC =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOCHOLINES, 18, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPC_18_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPC,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestLPE() {
    Double EXACT_MASS_LPE_18_1 = FormulaUtils.calculateExactMass("C23H46NO7P");
    IMolecularFormula testSpeciesLPE = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOETHANOLAMINES, 18, 1)
        .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPE_18_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPE,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestLPG() {
    Double EXACT_MASS_LPG_18_1 = FormulaUtils.calculateExactMass("C24H47O9P");
    IMolecularFormula testSpeciesLPG =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOGLYCEROLS, 18, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPG_18_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPG,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestLPI() {
    Double EXACT_MASS_LPI_18_1 = FormulaUtils.calculateExactMass("C27H51O12P");
    IMolecularFormula testSpeciesLPI =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOINOSITOLS, 18, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPI_18_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPI,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestLPS() {
    Double EXACT_MASS_LPS_18_0 = FormulaUtils.calculateExactMass("C24H48NO9P");
    IMolecularFormula testSpeciesLPI =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOACYLGLYCEROPHOSPHOSERINES, 18, 0)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPS_18_0),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPI,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestLPC_O() {
    Double EXACT_MASS_LPC_O_16_1 = FormulaUtils.calculateExactMass("C24H50NO6P");
    IMolecularFormula testSpeciesLPC_O =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOALKYLGLYCEROPHOSPHOCHOLINES, 16, 1)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPC_O_16_1),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPC_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestLPE_O() {
    Double EXACT_MASS_LPE_O_16_0 = FormulaUtils.calculateExactMass("C21H46NO6P");
    IMolecularFormula testSpeciesLPE_O = LIPID_FACTORY
        .buildSpeciesLevelLipid(LipidClasses.MONOALKYLGLYCEROPHOSPHOETHANOLAMINES, 16, 0)
        .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPE_O_16_0),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPE_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

  @Test
  public void molecularFormulaLevelTestLPG_O() {
    Double EXACT_MASS_LPG_O_16_0 = FormulaUtils.calculateExactMass("C22H47O8P");
    IMolecularFormula testSpeciesLPE_O =
        LIPID_FACTORY.buildSpeciesLevelLipid(LipidClasses.MONOALKYLGLYCEROPHOSPHOGLYCEROLS, 16, 0)
            .getMolecularFormula();
    assertEquals(NUMBER_FORMAT.format(EXACT_MASS_LPG_O_16_0),
        NUMBER_FORMAT.format(MolecularFormulaManipulator.getMass(testSpeciesLPE_O,
            AtomContainerManipulator.MonoIsotopic)));
  }

}
