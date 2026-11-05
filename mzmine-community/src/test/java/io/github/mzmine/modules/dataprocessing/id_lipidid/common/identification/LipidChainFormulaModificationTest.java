package io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification;

import io.github.mzmine.util.FormulaUtils;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.interfaces.IMolecularFormula;

class LipidChainFormulaModificationTest {

  private static final IMolecularFormula CHAIN_18_1 = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
      "C18H34O2");

  @Test
  void plainFormulaIsAdded() {
    final LipidChainFormulaModification mod = LipidChainFormulaModification.parse("C3H5O");
    Assertions.assertNotNull(mod);
    assertFormula("C21H39O3", mod.addTo(CHAIN_18_1));
    assertFormula("C15H29O", mod.subtractFrom(CHAIN_18_1));
  }

  @Test
  void signedFormulaAddsAndRemoves() {
    final LipidChainFormulaModification mod = LipidChainFormulaModification.parse("+O2-H2");
    Assertions.assertNotNull(mod);
    assertFormula("C18H32O4", mod.addTo(CHAIN_18_1));
    // minus rules apply the inverse
    assertFormula("C18H36", mod.subtractFrom(CHAIN_18_1));
  }

  @Test
  void signedFormulaWithoutLeadingSignAndSpaces() {
    final LipidChainFormulaModification mod = LipidChainFormulaModification.parse(" O - H2 + N ");
    Assertions.assertNotNull(mod);
    assertFormula("C18H32NO3", mod.addTo(CHAIN_18_1));
  }

  @Test
  void removingMoreAtomsThanPresentIsEmpty() {
    final LipidChainFormulaModification mod = LipidChainFormulaModification.parse("+O-P");
    Assertions.assertNotNull(mod);
    Assertions.assertTrue(mod.addTo(CHAIN_18_1).isEmpty());
  }

  @Test
  void invalidFormulas() {
    Assertions.assertFalse(LipidChainFormulaModification.isValid(null));
    Assertions.assertFalse(LipidChainFormulaModification.isValid(" "));
    Assertions.assertFalse(LipidChainFormulaModification.isValid("+O2-Xy"));
    Assertions.assertFalse(LipidChainFormulaModification.isValid("+O2--H2"));
  }

  @Test
  void addToDoesNotChangeInput() {
    final LipidChainFormulaModification mod = LipidChainFormulaModification.parse("+O2-H2");
    Assertions.assertNotNull(mod);
    mod.addTo(CHAIN_18_1);
    Assertions.assertEquals("C18H34O2", FormulaUtils.getFormulaString(CHAIN_18_1, false));
  }

  @Test
  void validationAcceptsSignedFormulaOnlyForChainRules() {
    Assertions.assertTrue(LipidFragmentationRule.validate(
        LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, "+O2-H2").isOk());
    Assertions.assertTrue(LipidFragmentationRule.validate(
        LipidFragmentationRuleType.TWO_ACYLCHAINS_MINUS_FORMULA_FRAGMENT_NL, "C3H5O").isOk());
    Assertions.assertFalse(LipidFragmentationRule.validate(
        LipidFragmentationRuleType.ALKYLCHAIN_MINUS_FORMULA_FRAGMENT, "+O2-Xy").isOk());
  }

  @Test
  void allChainRulesHaveChainType() {
    for (final LipidFragmentationRuleType type : LipidFragmentationRuleType.values()) {
      final boolean isChainRule = !(type == LipidFragmentationRuleType.HEADGROUP_FRAGMENT
          || type == LipidFragmentationRuleType.HEADGROUP_FRAGMENT_NL
          || type == LipidFragmentationRuleType.PRECURSOR);
      Assertions.assertEquals(isChainRule, type.getChainType() != null, type.name());
    }
  }

  @Test
  void ruleTypeRoundTripsThroughTargetModificationObservation() {
    for (final LipidFragmentationRuleType type : LipidFragmentationRuleType.values()) {
      Assertions.assertEquals(type, LipidFragmentationRuleType.of(type.getTarget(),
          type.getModification(), type.getObservation()).orElse(null), type.name());
    }
    Assertions.assertTrue(LipidFragmentationRuleType.of(LipidRuleTarget.HEAD_GROUP,
        LipidRuleModification.PLUS_FORMULA, LipidRuleObservation.FRAGMENT_ION).isEmpty());
  }

  private static void assertFormula(String expected, Optional<IMolecularFormula> actual) {
    Assertions.assertTrue(actual.isPresent(), "Expected " + expected + " but was empty");
    Assertions.assertEquals(expected, FormulaUtils.getFormulaString(actual.get(), false));
  }
}
