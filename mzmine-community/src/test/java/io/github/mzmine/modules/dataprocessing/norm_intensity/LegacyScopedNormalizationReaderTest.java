/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.modules.tools.PlaceholderModule;
import io.github.mzmine.modules.tools.PlaceholderModuleParameters;
import io.github.mzmine.util.XMLUtils;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class LegacyScopedNormalizationReaderTest {

  @Test
  void migratesCanonicalLegacySummaryAndResavesWithoutRequestPayloads() throws Exception {
    final Document document = XMLUtils.newDocument();
    final Element parameters = document.createElement("parameters");
    document.appendChild(parameters);
    appendStringParameter(parameters, "Name suffix", "legacy norm");
    appendStringParameter(parameters, "Source revision", "snapshot-17");
    appendStringParameter(parameters, "Reviewed factors", "s1=1.5;s2=0.75");
    final Element summaryElement = appendParameter(parameters, "Normalization functions");
    final NormalizationFunctionsParameter summaryParameter = new NormalizationFunctionsParameter();
    summaryParameter.setValue(new IntensityNormalizationSummary(List.of(
        new RawFileNormalizationFunction(new RawDataFilePlaceholder("first", null),
            LocalDateTime.of(2026, 1, 1, 0, 0), new FactorNormalizationFunction(1.5d)),
        new RawFileNormalizationFunction(new RawDataFilePlaceholder("second", null),
            LocalDateTime.of(2026, 1, 1, 0, 1), new FactorNormalizationFunction(.75d)))));
    summaryParameter.saveValueToXML(summaryElement);

    final IntensityNormalizerParameters migrated = LegacyScopedNormalizationReader.migrate(parameters);

    assertEquals("legacy norm", migrated.getValue(IntensityNormalizerParameters.suffix));
    assertTrue(migrated.getValue(IntensityNormalizerParameters.hiddenReviewedFunctions));
    final IntensityNormalizationSummary summary = migrated.getValue(
        IntensityNormalizerParameters.hiddenNormalizationSummary);
    assertEquals(2, summary.size());
    assertEquals(1.5d, summary.get(0).getNormalizationFactor(0d, 0f));
    assertTrue(summary.messages().getLast().contains("snapshot-17"));

    final Element reSaved = document.createElement("resaved");
    parameters.appendChild(reSaved);
    migrated.saveValuesToXML(reSaved);
    final String xml = XMLUtils.nodeToString(reSaved);
    assertFalse(xml.contains("Reviewed factors"));
    assertFalse(xml.contains("Reviewed row factors"));
    assertFalse(xml.contains("Unavailable row IDs"));
    assertTrue(xml.contains("Normalization functions"));
  }

  @Test
  void rejectsRequestOnlyLegacyPayloadWithoutRawFileBindings() throws Exception {
    final Document document = XMLUtils.newDocument();
    final Element parameters = document.createElement("parameters");
    document.appendChild(parameters);
    appendStringParameter(parameters, "Reviewed row factors", "3@s1=1.5;s2=0.75");
    appendStringParameter(parameters, "Unavailable row IDs", "4");

    final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> LegacyScopedNormalizationReader.migrate(parameters));
    assertTrue(exception.getMessage().contains("raw-file bindings"));
  }

  @Test
  void appliedMethodUsesWarningPlaceholderForUnboundLegacyRequest() throws Exception {
    final Document document = XMLUtils.newDocument();
    final Element appliedMethod = document.createElement("appliedMethod");
    document.appendChild(appliedMethod);
    final Element description = document.createElement("description");
    description.setTextContent("legacy scoped normalization");
    appliedMethod.appendChild(description);
    final Element module = document.createElement("module");
    module.setAttribute("class", LegacyScopedNormalizationReader.LEGACY_MODULE_CLASS);
    module.setAttribute("date", "2026-01-01T00:00:00Z");
    appliedMethod.appendChild(module);
    final Element parameters = document.createElement("parameters");
    appliedMethod.appendChild(parameters);
    appendStringParameter(parameters, "Reviewed factors", "s1=1.5");

    final SimpleFeatureListAppliedMethod loaded = SimpleFeatureListAppliedMethod.loadValueFromXML(
        appliedMethod);

    assertTrue(loaded.getModule() instanceof PlaceholderModule);
    assertTrue(loaded.getParameters().getValue(PlaceholderModuleParameters.oldModuleName)
        .contains("Cannot safely migrate legacy scoped normalization"));
  }

  private static void appendStringParameter(final Element parent, final String name,
      final String value) {
    appendParameter(parent, name).setTextContent(value);
  }

  private static Element appendParameter(final Element parent, final String name) {
    final Element parameter = parent.getOwnerDocument().createElement(
        SimpleParameterSet.parameterElement);
    parameter.setAttribute(SimpleParameterSet.nameAttribute, name);
    parent.appendChild(parameter);
    return parameter;
  }
}
