package util.lipidannotationtest;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipididentificationtools.ChainTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LipidChainToolsTest {

  private static final ChainTools CHAIN_TOOLS = new ChainTools();

  @Test
  void testFattyAcidFormulaCalculation() {
    String fattyAcidFormula = CHAIN_TOOLS.calculateFattyAcidFormula(18, 2);
    String correctFattyAcidFormula = "C18H32O2";
    Assertions.assertEquals(fattyAcidFormula, correctFattyAcidFormula,
        fattyAcidFormula + " is not the correct formula for FA 18:2. Correct formula is: "
            + correctFattyAcidFormula);
  }

  @Test
  void testHydroCarbonFormulaCalculation() {
    String fattyAcidFormula = CHAIN_TOOLS.calculateHydroCarbonFormula(18, 0);
    String correctFattyAcidFormula = "C18H38";
    Assertions.assertEquals(fattyAcidFormula, correctFattyAcidFormula,
        fattyAcidFormula + " is not the correct formula for Alkane 18:0. Correct formula is: "
            + correctFattyAcidFormula);
  }

  @Test
  void testChainLengthFromFormula() {
    String fattyAcidFormula = "C18H32O2";
    int correctChainLength = 18;
    Assertions.assertEquals(correctChainLength,
        CHAIN_TOOLS.getChainLengthFromFormula(fattyAcidFormula),
        fattyAcidFormula + " is not the correct chain length. Correct chain length is: "
            + correctChainLength);
  }

  @Test
  void testDoubleBondsFromFormula() {
    String fattyAcidFormula = "C18H32O2";
    int correctNumberOfDoubleBonds = 2;
    Assertions.assertEquals(correctNumberOfDoubleBonds,
        CHAIN_TOOLS.getNumberOfDoubleBondsFromFormula(fattyAcidFormula), fattyAcidFormula
            + " is not the correct number of double bonds. Correct number of double bonds is: "
            + correctNumberOfDoubleBonds);
  }

}
