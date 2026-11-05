package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRule;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidRuleObservation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidRuleTarget;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.community.LipidLibraryCatalog;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.community.LipidLibraryClient;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.editor.LipidRuleRow;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.library.CustomLipidClassPreset;
import io.github.mzmine.util.io.JsonUtils;
import io.github.mzmine.util.presets.Preset;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CustomLipidClassLibraryTest {

  private static final CustomLipidClass PE = CustomLipidClassJson.toCustom(
      LipidClasses.DIACYLGLYCEROPHOSPHOETHANOLAMINES);

  @Test
  void presetRoundTripKeepsClass() throws Exception {
    final String json = JsonUtils.MAPPER.writeValueAsString(new CustomLipidClassPreset(PE));
    final Preset read = JsonUtils.MAPPER.readValue(json, Preset.class);
    final CustomLipidClassPreset preset = Assertions.assertInstanceOf(
        CustomLipidClassPreset.class, read);
    Assertions.assertEquals(CustomLipidClassJson.toJson(List.of(PE)),
        CustomLipidClassJson.toJson(List.of(preset.lipidClass())));
    Assertions.assertTrue(json.contains("\"lipidFragmentationRuleType\""),
        "Preset must store the class in the mzmine export format");
  }

  @Test
  void exportFormatIgnoresCommunityMetadata() {
    final String json = CustomLipidClassJson.toJson(List.of(PE));
    final String withMeta = json.replaceFirst("\\{",
        "{\"_meta\": {\"id\": \"pe\", \"authors\": [{\"name\": \"A\"}]},");
    final List<CustomLipidClass> classes = CustomLipidClassJson.fromJson(withMeta);
    Assertions.assertEquals(1, classes.size());
    Assertions.assertEquals("PE", classes.getFirst().getAbbr());
  }

  @Test
  void catalogParsesAndIgnoresUnknownFields() throws Exception {
    final String index = """
        {"formatVersion": 1, "generated": "2026-11-02", "enums": {}, "classes": [
          {"id": "tst", "source": "community", "name": "Test", "abbr": "TST",
           "coreClass": "GLYCEROLIPIDS", "mainClass": "DIRADYLGLYCEROLS",
           "chainTypes": ["ACYL_CHAIN"], "polarities": ["POSITIVE"],
           "authors": [{"name": "Jane Doe", "orcid": "0000"}], "tags": ["oxidized"],
           "minMzmineVersion": "999.0.0", "file": "classes/tst.json", "unknown": 1}]}
        """;
    final LipidLibraryCatalog catalog = JsonUtils.MAPPER.readValue(index,
        LipidLibraryCatalog.class);
    Assertions.assertEquals(1, catalog.classes().size());
    final var entry = catalog.classes().getFirst();
    Assertions.assertTrue(entry.isCommunity());
    Assertions.assertEquals("Jane Doe", entry.authorNames());
    Assertions.assertTrue(entry.searchText().contains("oxidized"));
    Assertions.assertTrue(entry.requiresNewerMzmine());
  }

  @Test
  void submissionUrlPrefillsTitle() {
    final String url = LipidLibraryClient.submissionUrl(PE);
    Assertions.assertTrue(url.startsWith(LipidLibraryClient.REPOSITORY_URL + "/issues/new"));
    Assertions.assertTrue(url.contains("title=%5BLipid+class%5D+PE+"), url);
  }

  @Test
  void ruleRowRoundTripsAllBuiltInRules() {
    for (final LipidClasses lipidClass : LipidClasses.values()) {
      for (final LipidFragmentationRule rule : lipidClass.getFragmentationRules()) {
        final LipidRuleRow row = LipidRuleRow.of(rule);
        Assertions.assertTrue(row.validate().isOk(),
            () -> lipidClass + " " + rule + ": " + row.validate().message());
        final LipidFragmentationRule back = row.toRule();
        Assertions.assertNotNull(back);
        Assertions.assertEquals(rule.getLipidFragmentationRuleType(),
            back.getLipidFragmentationRuleType());
        Assertions.assertEquals(rule.getIonizationType(), back.getIonizationType());
        Assertions.assertEquals(rule.getMolecularFormula().strip(), back.getMolecularFormula(),
            () -> lipidClass + " " + rule);
      }
    }
  }

  @Test
  void ruleRowPlusAndMinusFormulaOfChains() {
    final LipidRuleRow row = new LipidRuleRow();
    row.ionizationProperty().set(IonizationType.NEGATIVE_HYDROGEN);
    row.targetProperty().set(LipidRuleTarget.ACYL_CHAIN);
    Assertions.assertEquals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL,
        row.levelProperty().get(), "chain fragments identify chains");
    Assertions.assertEquals(LipidFragmentationRuleType.ACYLCHAIN_FRAGMENT,
        row.toRule().getLipidFragmentationRuleType(), "no plus or minus is the plain chain");

    // plus H and minus O in one rule
    row.plusFormulaProperty().set("H");
    row.minusFormulaProperty().set("O");
    Assertions.assertEquals("Acyl chain + H − O fragment", row.describe());
    LipidFragmentationRule rule = row.toRule();
    Assertions.assertNotNull(rule);
    Assertions.assertEquals(LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT,
        rule.getLipidFragmentationRuleType());
    Assertions.assertEquals("+H-O", rule.getMolecularFormula());

    // only minus stays a minus rule, readable by older mzmine versions
    row.plusFormulaProperty().set("");
    row.minusFormulaProperty().set("-H2O");
    rule = row.toRule();
    Assertions.assertEquals(LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT,
        rule.getLipidFragmentationRuleType());
    Assertions.assertEquals("H2O", rule.getMolecularFormula());

    row.minusFormulaProperty().set("Xy");
    Assertions.assertFalse(row.validate().isOk(), "unknown element");

    row.targetProperty().set(LipidRuleTarget.PRECURSOR);
    Assertions.assertEquals(LipidRuleObservation.FRAGMENT_ION, row.observationProperty().get());
  }

  @Test
  void ruleRowReadsStoredChainFormulas() {
    assertPlusMinus(LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, "C3H5O", "C3H5O",
        "");
    assertPlusMinus(LipidFragmentationRuleType.ACYLCHAIN_PLUS_FORMULA_FRAGMENT, "+H-O", "H", "O");
    assertPlusMinus(LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT, "H2O", "",
        "H2O");
    // a minus rule subtracts the whole signed formula
    assertPlusMinus(LipidFragmentationRuleType.ACYLCHAIN_MINUS_FORMULA_FRAGMENT, "+O-H2", "H2",
        "O");
  }

  private static void assertPlusMinus(LipidFragmentationRuleType type, String stored,
      String plus, String minus) {
    final LipidFragmentationRule rule = new LipidFragmentationRule(
        IonizationType.NEGATIVE_HYDROGEN.getPolarity(), IonizationType.NEGATIVE_HYDROGEN, type,
        LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL, stored);
    final LipidRuleRow row = LipidRuleRow.of(rule);
    Assertions.assertEquals(plus, row.plusFormulaProperty().get(), stored);
    Assertions.assertEquals(minus, row.minusFormulaProperty().get(), stored);
    Assertions.assertTrue(row.validate().isOk(), stored);
  }
}
