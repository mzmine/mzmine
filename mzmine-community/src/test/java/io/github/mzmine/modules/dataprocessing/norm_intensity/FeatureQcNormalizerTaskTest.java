/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createFeature;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DoubleMetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.combowithinput.FieldSeparator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FeatureQcNormalizerTaskTest {

  @TempDir
  Path tempDir;

  @AfterEach
  void clearProject() {
    ProjectService.getProjectManager().clearProject();
  }

  @Test
  void processInPlaceRequestUsesTheOrdinaryNativeLifecycle() {
    final Fixture fixture = fixture(true);
    final IntensityNormalizerTask task = new IntensityNormalizerTask(fixture.project, fixture.source,
        parameters(null), null, Instant.now());

    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());
    assertEquals(1, fixture.project.getCurrentFeatureLists().size());
    assertEquals(1d, task.getFinishedPercentage(), 1e-12);
    final ModularFeatureList output = output(fixture);
    final IntensityNormalizationSummary summary = IntensityNormalizerModule
        .getNormalizationFunctionsOfLatestCall(output);
    assertNotNull(summary.functions().getFirst().function());
    final IntensityNormalizationSummary appliedSummary = appliedSummary(output);
    assertEquals(1, appliedSummary.featureQc().size());
    assertEquals(FeatureQcSettings.defaults(), appliedSummary.featureQc().getFirst().diagnostics().settings());
  }

  @Test
  void namedSingleBatchMatchesImplicitTaskResult() {
    final Fixture implicit = fixture(true);
    new IntensityNormalizerTask(implicit.project, implicit.source, parameters(null), null, Instant.now()).run();
    final ModularFeatureList implicitOutput = output(implicit);

    final Fixture explicit = fixture(true);
    final String columnName = "feature_qc_task_batch";
    final StringMetadataColumn batch = new StringMetadataColumn(columnName, "test batch");
    explicit.project.getProjectMetadata().addColumn(batch);
    for (final RawDataFile file : explicit.files) explicit.project.getProjectMetadata().setValue(batch, file, "A");
    new IntensityNormalizerTask(explicit.project, explicit.source, parameters(columnName), null,
        Instant.now()).run();
    final ModularFeatureList explicitOutput = output(explicit);

    for (int index = 0; index < implicit.files.size(); index++) {
      final RawDataFile implicitFile = implicit.files.get(index);
      final RawDataFile explicitFile = explicit.files.get(index);
      assertEquals(((ModularFeature) implicitOutput.getFeature(0, implicitFile)).get(NormalizedAreaType.class),
          ((ModularFeature) explicitOutput.getFeature(0, explicitFile)).get(NormalizedAreaType.class),
          1e-6f);
    }
  }

  @Test
  void allSkippedFeatureQcCompletesWithWarning() {
    final Fixture fixture = fixture(false);
    fixture.source.streamFeatures().forEach(feature -> {
      feature.set(NormalizedAreaType.class, 88f);
      feature.set(NormalizedHeightType.class, 77f);
    });
    final IntensityNormalizerTask task = new IntensityNormalizerTask(fixture.project, fixture.source,
        parameters(null), null, Instant.now());

    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());
    assertEquals(1d, task.getFinishedPercentage(), 1e-12);
    final IntensityNormalizationSummary summary = IntensityNormalizerModule
        .getNormalizationFunctionsOfLatestCall(output(fixture));
    assertNotNull(summary);
    final IntensityNormalizationSummary appliedSummary = appliedSummary(output(fixture));
    assertFalse(appliedSummary.messages().isEmpty());
    assertFalse(appliedSummary.featureQc().getFirst().diagnostics().availability().hasCorrectablePairs());
    output(fixture).streamFeatures().forEach(feature -> {
      assertNull(feature.get(NormalizedAreaType.class));
      assertNull(feature.get(NormalizedHeightType.class));
    });
  }

  @Test
  void metadataAndInternalStandardAreComposedBeforeFixedQcRlsc()
      throws Exception {
    final MZmineProjectImpl project = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(project);
    final List<RawDataFile> files = new ArrayList<>();
    for (int index = 0; index < 10; index++) {
      files.add(createRawFile("qc_compose_" + index,
          LocalDateTime.of(2026, 1, 2, 10, 0).plusMinutes(index)));
    }
    final ModularFeatureList source = new ModularFeatureList("feature-qc-compose", null, files);
    final ModularFeatureListRow standard = new ModularFeatureListRow(source, 1);
    final ModularFeatureListRow analyte = new ModularFeatureListRow(source, 2);
    for (int index = 0; index < files.size(); index++) {
      final float analyteArea = index < 5 ? 40f : 80f;
      standard.addFeature(files.get(index), createFeature(source, files.get(index), 1f, 1f,
          200d, 5f, null), false);
      analyte.addFeature(files.get(index), createFeature(source, files.get(index), analyteArea,
          analyteArea, 100d, 5f, null), false);
    }
    standard.applyRowBindings();
    analyte.applyRowBindings();
    source.addRow(standard);
    source.addRow(analyte);
    project.addFeatureList(source);
    final StringMetadataColumn batch = new StringMetadataColumn("feature_qc_compose_batch", "batch");
    final DoubleMetadataColumn dilution = new DoubleMetadataColumn("feature_qc_compose_dilution", "dilution");
    project.getProjectMetadata().addColumn(batch);
    project.getProjectMetadata().addColumn(dilution);
    for (int index = 0; index < files.size(); index++) {
      project.getProjectMetadata().setValue(batch, files.get(index), index < 5 ? "A" : "B");
      project.getProjectMetadata().setValue(dilution, files.get(index), index < 5 ? 2d : 4d);
    }
    final File standards = tempDir.resolve("standards.csv").toFile();
    Files.writeString(standards.toPath(), "mz,rt,name\n200,5,internal\n");
    final ParameterSet internalStandard = StandardCompoundNormalizationTypeParameters.create(
        List.of(SampleType.values()), StandardUsageType.Nearest, 1d, standards, FieldSeparator.COMMA,
        new MZTolerance(.25, 0d), new RTTolerance(.25f, RTTolerance.Unit.MINUTES),
        new MobilityTolerance(.25f), StandardCompoundNormalizationMode.REQUIRE_N_SAMPLES);
    final FeatureQcNormalizationParameters featureQcParameters =
        (FeatureQcNormalizationParameters) new FeatureQcNormalizationParameters().cloneParameterSet();
    featureQcParameters.setParameter(FeatureQcNormalizationParameters.automaticSmoothing, false);
    final ParameterSet parameters = IntensityNormalizerParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS), "feature-qc-compose",
        new MetadataNormalizationConfig("feature_qc_compose_dilution", MetadataNormalizationConfig.Mode.divide),
        NormalizationType.StandardCompounds, internalStandard, NormalizationType.FeatureQcLoess,
        featureQcParameters, "feature_qc_compose_batch", AbundanceMeasure.Area,
        OriginalFeatureListOption.KEEP, null);

    final IntensityNormalizerTask task = new IntensityNormalizerTask(project, source, parameters, null,
        Instant.now());
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());
    final ModularFeatureList output = (ModularFeatureList) project.getCurrentFeatureLists().stream()
        .filter(list -> list != source).findFirst().orElseThrow();
    // Metadata and internal standards produce effective values of 15 and 30. Fixed QC-RLSC
    // preserves each batch's QC mean, exactly as automatic QC-RLSC does.
    for (int i = 0; i < files.size(); i++) {
      assertEquals(i < 5 ? 15f : 30f,
          ((ModularFeature) output.findRowByID(2).getFeature(files.get(i)))
              .get(NormalizedAreaType.class), 1e-3f);
    }
  }

  private static ParameterSet parameters(final String batchColumn) {
    return IntensityNormalizerParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS), "feature-qc-task", null,
        NormalizationType.NoNormalization, null, NormalizationType.FeatureQcLoess,
        new FeatureQcNormalizationParameters(), batchColumn, AbundanceMeasure.Area,
        OriginalFeatureListOption.PROCESS_IN_PLACE, null);
  }

  private static Fixture fixture(final boolean complete) {
    final MZmineProjectImpl project = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(project);
    final List<RawDataFile> files = new ArrayList<>();
    for (int index = 0; index < 6; index++) {
      files.add(createRawFile("qc_feature_" + index,
          LocalDateTime.of(2026, 1, 1, 10, 0).plusMinutes(index)));
    }
    final ModularFeatureList source = new ModularFeatureList("feature-qc-task", null, files);
    final ModularFeatureListRow row = new ModularFeatureListRow(source, 1);
    for (int index = 0; index < (complete ? files.size() : 1); index++) {
      row.addFeature(files.get(index), createFeature(source, files.get(index), 100f), false);
    }
    row.applyRowBindings();
    source.addRow(row);
    project.addFeatureList(source);
    return new Fixture(project, source, files);
  }

  private static ModularFeatureList output(final Fixture fixture) {
    return (ModularFeatureList) fixture.project.getCurrentFeatureLists().stream()
        .filter(list -> list != fixture.source).findFirst().orElse(fixture.source);
  }

  private static IntensityNormalizationSummary appliedSummary(final ModularFeatureList featureList) {
    return featureList.getAppliedMethods().getLast().getParameters().getValue(
        IntensityNormalizerParameters.hiddenNormalizationSummary);
  }

  private record Fixture(MZmineProjectImpl project, ModularFeatureList source, List<RawDataFile> files) {
  }
}
