package io.github.mzmine.datamodel.features.settings;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.dataprocessing.norm_intensity.FactorNormalizationFunction;
import io.github.mzmine.modules.dataprocessing.norm_intensity.IntensityNormalizationSummary;
import io.github.mzmine.modules.dataprocessing.norm_intensity.IntensityNormalizerModule;
import io.github.mzmine.modules.dataprocessing.norm_intensity.RawFileNormalizationFunction;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class FeatureListSettingsTest {

  private static Element newSettingsElement() throws ParserConfigurationException {
    final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .newDocument();
    final Element element = document.createElement(CONST.XML_FLIST_SETTINGS_ELEMENT);
    document.appendChild(element);
    return element;
  }

  private static FeatureListSettings saveAndLoad(final FeatureListSettings settings)
      throws ParserConfigurationException {
    final Element element = newSettingsElement();
    settings.saveToXML(element);
    return FeatureListSettings.loadFromXML(element);
  }

  private static IntensityNormalizationSummary factorSummary(final RawDataFileImpl raw) {
    return new IntensityNormalizationSummary(
        List.of(new RawFileNormalizationFunction(raw, new FactorNormalizationFunction(2d))));
  }

  @Test
  void testNormalizationRoundTrip() throws ParserConfigurationException {
    final RawDataFileImpl raw = new RawDataFileImpl("sample", null, null);
    final FeatureListSettings loaded = saveAndLoad(
        FeatureListSettings.DEFAULT.withNormalization(factorSummary(raw)));

    Assertions.assertNotNull(loaded);
    Assertions.assertEquals(1, loaded.normalization().functions().size());
    Assertions.assertEquals(2d,
        loaded.normalization().get(0).getNormalizationFactor(100d, 5f), 1e-12);
  }

  @Test
  void testClearedNormalizationStaysCleared() throws ParserConfigurationException {
    // an existing settings element without normalization is not an old project
    final FeatureListSettings loaded = saveAndLoad(FeatureListSettings.DEFAULT);
    Assertions.assertEquals(FeatureListSettings.DEFAULT, loaded);
  }

  @Test
  void testMissingElementIsNull() {
    // null signals a project saved before settings were introduced
    Assertions.assertNull(FeatureListSettings.loadFromXML(null));
  }

  @Test
  void testEmptySummaryIsNoNormalization() {
    Assertions.assertNull(
        FeatureListSettings.DEFAULT.withNormalization(IntensityNormalizationSummary.EMPTY)
            .normalization());
  }

  @Test
  void testLoadedFunctionsMatchRawFileOfFeatureList() throws ParserConfigurationException {
    // the project loads new raw data file instances, matched by the placeholder
    final RawDataFileImpl serializedRaw = new RawDataFileImpl("same-name", null, null);
    final RawDataFileImpl listRaw = new RawDataFileImpl("same-name", null, null);
    final FeatureListSettings loaded = saveAndLoad(
        FeatureListSettings.DEFAULT.withNormalization(factorSummary(serializedRaw)));
    final ModularFeatureList list = new ModularFeatureList("list", null, listRaw);

    list.setSettings(loaded);

    Assertions.assertTrue(
        IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(list, listRaw)
            .isPresent());
    Assertions.assertEquals(1, loaded.normalization().toSearchableSummary(List.of(listRaw)).size());
  }
}
