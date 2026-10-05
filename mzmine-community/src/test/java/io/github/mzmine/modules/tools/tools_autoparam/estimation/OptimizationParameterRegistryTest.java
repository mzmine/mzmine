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

package io.github.mzmine.modules.tools.tools_autoparam.estimation;

import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ApplicationScope;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.estimation.domain.SearchScale;
import io.github.mzmine.modules.tools.tools_autoparam.optimizer.ParameterDefinitionCheckListParameter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class OptimizationParameterRegistryTest {

  @Test
  void idsAreUniqueAndAvailableWithoutLoadingOptionalModules() {
    final List<ParameterDefinition<?>> definitions = OptimizationParameterRegistry.allSolutions();
    Assertions.assertEquals(definitions.size(),
        definitions.stream().map(ParameterDefinition::id).distinct().count());
  }

  @Test
  void wizardPartDistinguishesTheSameParameterName() {
    final WizardParameterDefinition<Double> original = OptimizationParameterRegistry.MINIMUM_FEATURE_HEIGHT;
    final WizardParameterDefinition<Double> otherPart = new WizardParameterDefinition<>(
        original.name(), WizardPart.IMS, original.parameter(), original.role(), original.presets(),
        original.estimator());
    Assertions.assertNotEquals(original.id(), otherPart.id());
    Assertions.assertNotEquals(original, otherPart);
  }

  @Test
  void batchIdsIncludeModuleAndApplicationScope() {
    final BatchParameterDefinition<Double> first = OptimizationParameterRegistry.TOP_TO_EDGE;
    final BatchParameterDefinition<Double> all = new BatchParameterDefinition<>(first.name(),
        MinimumSearchFeatureResolverModule.class, MinimumSearchFeatureResolverParameters.MIN_RATIO,
        ApplicationScope.ALL, first.role(), first.presets(), first.estimator());
    Assertions.assertEquals(
        "batch/" + MinimumSearchFeatureResolverModule.class.getName() + "/FIRST/"
            + MinimumSearchFeatureResolverParameters.MIN_RATIO.getName(), first.id());
    Assertions.assertNotEquals(first.id(), all.id());
    Assertions.assertNotEquals(first, all);
  }

  @Test
  void selectionXmlUsesStableIdsAndSurvivesDisplayNameChanges() throws Exception {
    final WizardParameterDefinition<Double> original = OptimizationParameterRegistry.MINIMUM_FEATURE_HEIGHT;
    final WizardParameterDefinition<Double> renamed = new WizardParameterDefinition<>(
        "Renamed intensity", original.part(), original.parameter(), original.role(),
        original.presets(), original.estimator());
    final ParameterDefinitionCheckListParameter saved = new ParameterDefinitionCheckListParameter(
        "Selection", "", List.of(original), List.of(original));
    final Element xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        .createElement("selection");
    saved.saveValueToXML(xml);
    Assertions.assertEquals("wizard/MS/" + original.parameter().getName(), xml.getTextContent());
    final ParameterDefinitionCheckListParameter loaded = new ParameterDefinitionCheckListParameter(
        "Selection", "", List.of(renamed), List.of());
    loaded.loadValueFromXML(xml);
    Assertions.assertSame(renamed, loaded.getValue().getFirst());
  }

  @Test
  void emptySelectionXmlDoesNotRestoreDefaults() throws Exception {
    final ParameterDefinitionCheckListParameter parameter = new ParameterDefinitionCheckListParameter(
        "Selection", "", OptimizationParameterRegistry.allSolutions(),
        OptimizationParameterRegistry.defaultSolutions());
    parameter.loadValueFromXML(
        DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
            .createElement("selection"));
    Assertions.assertTrue(parameter.getValue().isEmpty());
  }

  @Test
  void defaultSelectionExcludesOnlyOptionalWaveletParameters() {
    final List<String> allNames = OptimizationParameterRegistry.allSolutions().stream()
        .map(ParameterDefinition::name).toList();
    final List<String> defaultNames = OptimizationParameterRegistry.defaultSolutions().stream()
        .map(ParameterDefinition::name).toList();

    Assertions.assertTrue(allNames.contains("Wavelet SNR threshold"));
    Assertions.assertFalse(defaultNames.stream().anyMatch(name -> name.startsWith("Wavelet ")));
    Assertions.assertTrue(defaultNames.contains("Min height"));
    Assertions.assertTrue(defaultNames.contains("Top-to-edge ratio"));
    Assertions.assertEquals(allNames.size() - 3, defaultNames.size());
  }

  @Test
  void sequenceContributesOnlyParametersForItsResolver() {
    final WizardSequence sequence = new WizardSequence();
    sequence.set(WizardPart.ION_INTERFACE, IonInterfaceWizardParameterFactory.LC_WAVELET.create());

    final List<String> names = OptimizationParameterRegistry.forSequence(sequence).stream()
        .map(ParameterDefinition::name).toList();

    Assertions.assertEquals(
        List.of("Crop retention time", "Inter sample RT tolerance", "Min consecutive",
            "RT correction", "Wavelet SNR threshold", "Wavelet baseline method",
            "Wavelet noise calculation"), names);
    Assertions.assertFalse(names.contains("Top-to-edge ratio"));
  }

  @Test
  void presetsWithoutChromatographyOrMobilityContributeNoSuchParameters() {
    // the direct infusion, GC-EI and no-IMS presets share parameter names with LC and IMS presets
    final WizardSequence direct = new WizardSequence();
    direct.set(WizardPart.ION_INTERFACE,
        IonInterfaceWizardParameterFactory.DIRECT_INFUSION.create());
    direct.set(WizardPart.IMS, IonMobilityWizardParameterFactory.NO_IMS.create());
    Assertions.assertEquals(List.of(), OptimizationParameterRegistry.forSequence(direct));

    final WizardSequence gcEi = new WizardSequence();
    gcEi.set(WizardPart.ION_INTERFACE, IonInterfaceWizardParameterFactory.GC_EI.create());
    Assertions.assertEquals(List.of("FWHM", "Inter sample RT tolerance", "Min consecutive"),
        OptimizationParameterRegistry.forSequence(gcEi).stream().map(ParameterDefinition::name)
            .toList());
  }

  @Test
  void estimateOnlyDefinitionsApplyButAreNeverOffered() {
    final List<ParameterDefinition<?>> estimateOnly = OptimizationParameterRegistry.forSequence(
            ParameterEstimationTestData.sequence()).stream()
        .filter(definition -> definition.role() == OptimizationRole.ESTIMATE_ONLY).toList();
    Assertions.assertFalse(estimateOnly.isEmpty());
    Assertions.assertTrue(
        estimateOnly.stream().noneMatch(OptimizationParameterRegistry.allSolutions()::contains));
  }

  @Test
  void parametersDeclareTheirSearchScale() {
    final ParameterEstimationContext context = ParameterEstimationTestData.context(
        ParameterEstimationTestData.sequence());
    final Map<String, SearchScale> scales = OptimizationParameterRegistry.forSequence(
        context.sequence()).stream().collect(Collectors.toMap(ParameterDefinition::name,
        definition -> definition.prepare(context).searchDomain().searchScale()));

    Assertions.assertEquals(SearchScale.LINEAR, scales.get("Inter sample RT tolerance"));
    Assertions.assertEquals(SearchScale.LOGARITHMIC, scales.get("Min height"));
    Assertions.assertEquals(SearchScale.LINEAR, scales.get("Min consecutive"));
    Assertions.assertEquals(SearchScale.LINEAR, scales.get("Chrom. Threshold"));
  }
}
