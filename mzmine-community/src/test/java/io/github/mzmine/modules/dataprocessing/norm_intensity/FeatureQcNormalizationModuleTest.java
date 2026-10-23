/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createFeature;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class FeatureQcNormalizationModuleTest {

  @Test
  void nativeParametersPreserveFixedSmoothingAcrossXmlRoundtrip() throws Exception {
    final var parameters = (FeatureQcNormalizationParameters) new FeatureQcNormalizationParameters()
        .cloneParameterSet();
    parameters.setParameter(FeatureQcNormalizationParameters.automaticSmoothing, false);
    parameters.setParameter(FeatureQcNormalizationParameters.bandwidth, .6d);
    parameters.setParameter(FeatureQcNormalizationParameters.polynomialDegree, 2);
    parameters.setParameter(FeatureQcNormalizationParameters.initialQcsToExclude, 3);
    final String xml = ParameterUtils.saveValuesToXMLString(parameters);
    final var loaded = (FeatureQcNormalizationParameters) new FeatureQcNormalizationParameters()
        .cloneParameterSet();
    ParameterUtils.loadValuesFromXMLString(loaded, xml);
    assertEquals(parameters.settings(), loaded.settings());
    assertFalse(loaded.settings().automaticSmoothing());
  }

  @Test
  void implicitAndExplicitSingleBatchProduceExactSameFunctions() {
    final Fixture fixture = fixture();
    final FeatureQcNormalizationModule module = new FeatureQcNormalizationModule();
    final IntensityNormalizationSearchableSummary implicit = calculate(module, fixture,
        List.of(new SamplesBatch(fixture.files)));
    final IntensityNormalizationSearchableSummary explicit = calculate(module, fixture,
        List.of(new SamplesBatch(fixture.files, "named")));

    for (final RawDataFile file : fixture.files) {
      assertEquals(implicit.get(file).getNormalizationFactor(fixture.list.getRow(0), 100d, 5f),
          explicit.get(file).getNormalizationFactor(fixture.list.getRow(0), 100d, 5f));
      assertInstanceOf(FeatureRowNormalizationFunction.class, implicit.get(file));
    }
    assertEquals(1, implicit.featureQc().size());
    assertTrue(implicit.featureQc().getFirst().diagnostics().settings().automaticSmoothing());
  }

  @Test
  void keepsScalarBatchMetricsUnsetAndAccountsForPartialBatchAvailability() {
    final Fixture fixture = fixture();
    final ModularFeatureListRow partial = new ModularFeatureListRow(fixture.list, 2);
    for (int index = 0; index < 5; index++) {
      final RawDataFile file = fixture.files.get(index);
      partial.addFeature(file, createFeature(fixture.list, file, 50f), false);
    }
    partial.applyRowBindings();
    fixture.list.addRow(partial);

    final SamplesBatch first = new SamplesBatch(fixture.files.subList(0, 5), "A");
    final SamplesBatch second = new SamplesBatch(fixture.files.subList(5, 10), "B");
    final IntensityNormalizationSearchableSummary summary = calculate(new FeatureQcNormalizationModule(),
        fixture, List.of(first, second));

    assertTrue(Double.isNaN(first.getMedianReferenceNormMetric()));
    assertTrue(Double.isNaN(second.getMedianReferenceNormMetric()));
    for (int index = 0; index < 5; index++) {
      assertTrue(summary.get(fixture.files.get(index)).getNormalizationFactor(partial, 100d, 5f)
          .isPresent());
    }
    for (int index = 5; index < 10; index++) {
      assertTrue(summary.get(fixture.files.get(index)).getNormalizationFactor(partial, 100d, 5f)
          .isEmpty());
    }
    assertEquals(FeatureQcUnavailableReason.BATCH_INSUFFICIENT_USABLE_QCS.name(),
        ((FeatureRowNormalizationFunction) summary.get(fixture.files.get(5))).unavailableReasons().get(2));
  }

  @Test
  void reportsCompletedFilesForAllSkippedRows() {
    final Fixture fixture = fixture(false);
    final ModularFeatureListRow skipped = new ModularFeatureListRow(fixture.list, 2);
    skipped.addFeature(fixture.files.getFirst(), createFeature(fixture.list, fixture.files.getFirst(), 5f),
        false);
    skipped.applyRowBindings();
    fixture.list.addRow(skipped);
    final AtomicInteger completed = new AtomicInteger();

    final IntensityNormalizationSearchableSummary summary = calculate(new FeatureQcNormalizationModule(),
        fixture, List.of(new SamplesBatch(fixture.files)), completed::addAndGet);

    assertEquals(fixture.files.size(), completed.get());
    assertFalse(summary.featureQc().getFirst().diagnostics().availability().hasCorrectablePairs());
    assertEquals(FeatureQcUnavailableReason.GLOBAL_MISSING_QC_THRESHOLD,
        summary.featureQc().getFirst().diagnostics().rows().get(2).unavailableReason());
  }

  private static IntensityNormalizationSearchableSummary calculate(
      final FeatureQcNormalizationModule module, final Fixture fixture,
      final List<SamplesBatch> batches) {
    return calculate(module, fixture, batches, ignored -> { });
  }

  private static IntensityNormalizationSearchableSummary calculate(
      final FeatureQcNormalizationModule module, final Fixture fixture,
      final List<SamplesBatch> batches, final java.util.function.IntConsumer completed) {
    final IntensityNormalizationSearchableSummary summary = new IntensityNormalizationSearchableSummary(
        fixture.files.size());
    module.createAllNormalizationFunctionsToSummary(summary, fixture.list, batches,
        new MetadataTable(), parameters(), new FeatureQcNormalizationParameters(), () -> false,
        completed);
    return summary;
  }

  private static ParameterSet parameters() {
    return IntensityNormalizerParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS), "feature-qc", null,
        NormalizationType.NoNormalization, null, NormalizationType.FeatureQcLoess,
        new FeatureQcNormalizationParameters(), null, AbundanceMeasure.Area,
        OriginalFeatureListOption.PROCESS_IN_PLACE, null);
  }

  private static Fixture fixture() {
    return fixture(true);
  }

  private static Fixture fixture(final boolean addCompleteRow) {
    final List<RawDataFile> files = new ArrayList<>();
    for (int index = 0; index < 10; index++) {
      files.add(createRawFile("qc_" + index, LocalDateTime.of(2026, 1, 1, 10, 0).plusMinutes(index)));
    }
    final ModularFeatureList list = new ModularFeatureList("feature-qc", null, files);
    if (addCompleteRow) {
      final ModularFeatureListRow row = new ModularFeatureListRow(list, 1);
      for (int index = 0; index < files.size(); index++) {
        row.addFeature(files.get(index), createFeature(list, files.get(index),
            index < 5 ? 10f : 20f), false);
      }
      row.applyRowBindings();
      list.addRow(row);
    }
    return new Fixture(list, files);
  }

  private record Fixture(ModularFeatureList list, List<RawDataFile> files) {
  }
}
