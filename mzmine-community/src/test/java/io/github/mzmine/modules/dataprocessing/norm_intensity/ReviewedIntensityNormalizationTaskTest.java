package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.addRow;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ReviewedIntensityNormalizationTaskTest {

  @Test
  void bindsFrozenFunctionsToTheirRawFileIdentitiesInsteadOfSummaryOrder() {
    final Fixture fixture = fixture();
    // The result order is deliberately reversed. Function identity, not table position, decides
    // which value is applied to each raw file.
    final IntensityNormalizationSummary summary = new IntensityNormalizationSummary(List.of(
        new RawFileNormalizationFunction(fixture.second, new FactorNormalizationFunction(0.5d)),
        new RawFileNormalizationFunction(fixture.first, new FactorNormalizationFunction(2d))));
    final Task task = IntensityNormalizerModule.createReviewedTask(fixture.project, fixture.source,
        "reviewed", "revision", summary, MemoryMapStorage.forFeatureList(), Instant.now(), () -> true);

    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());
    final ModularFeatureList copy = (ModularFeatureList) fixture.project.getCurrentFeatureLists().get(1);
    assertEquals(20f, normalizedArea(copy, fixture.first));
    assertEquals(10f, normalizedArea(copy, fixture.second));
    assertEquals(2d, IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(copy,
        fixture.first).orElseThrow().getNormalizationFactor(1d, 1f));
    assertEquals(0.5d, IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(copy,
        fixture.second).orElseThrow().getNormalizationFactor(1d, 1f));
    final var parameters = copy.getAppliedMethods().getFirst().getParameters();
    assertEquals("revision", parameters.getValue(IntensityNormalizerParameters.hiddenSourceRevision));
    assertEquals(NormalizationSourceRevision.capture(fixture.source,
        fixture.project.getProjectMetadata(), () -> false), parameters.getValue(
        IntensityNormalizerParameters.hiddenNativeSourceFingerprint));
  }

  @Test
  void overflowingNormalizedMeasurementRemainsMissingWithoutLosingTheOtherMeasure() {
    final Fixture fixture = fixture();
    final ModularFeature sourceFeature = (ModularFeature) fixture.source.getFeature(0, fixture.first);
    sourceFeature.setArea(Float.MAX_VALUE);
    sourceFeature.setHeight(10f);
    final IntensityNormalizationSummary summary = new IntensityNormalizationSummary(List.of(
        new RawFileNormalizationFunction(fixture.first, new FactorNormalizationFunction(2d)),
        new RawFileNormalizationFunction(fixture.second, new FactorNormalizationFunction(1d))));
    final Task task = IntensityNormalizerModule.createReviewedTask(fixture.project, fixture.source,
        "reviewed", "revision", summary, MemoryMapStorage.forFeatureList(), Instant.now(), () -> true);

    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());
    final ModularFeatureList copy = (ModularFeatureList) fixture.project.getCurrentFeatureLists().get(1);
    final ModularFeature normalized = (ModularFeature) copy.getFeature(0, fixture.first);
    assertNull(normalized.get(NormalizedAreaType.class));
    assertEquals(20f, normalized.get(NormalizedHeightType.class));
    assertEquals(Float.MAX_VALUE, sourceFeature.getArea());
  }

  @Test
  void staleReviewedSnapshotDoesNotPublishACopy() {
    final Fixture fixture = fixture();
    final IntensityNormalizationSummary summary = new IntensityNormalizationSummary(List.of(
        new RawFileNormalizationFunction(fixture.first, new FactorNormalizationFunction(2d)),
        new RawFileNormalizationFunction(fixture.second, new FactorNormalizationFunction(0.5d))));
    final Task task = IntensityNormalizerModule.createReviewedTask(fixture.project, fixture.source,
        "reviewed", "revision", summary, MemoryMapStorage.forFeatureList(), Instant.now(), () -> false);

    task.run();

    assertEquals(TaskStatus.ERROR, task.getStatus());
    assertEquals(1, fixture.project.getCurrentFeatureLists().size());
  }

  @Test
  void persistedFreshReviewedParametersReplayUnchangedSourceAfterMetadataVersionChanges()
      throws Exception {
    final Fixture fixture = fixture();
    final IntensityNormalizationSummary summary = new IntensityNormalizationSummary(List.of(
        new RawFileNormalizationFunction(fixture.first, new FactorNormalizationFunction(2d)),
        new RawFileNormalizationFunction(fixture.second, new FactorNormalizationFunction(.5d))));
    IntensityNormalizerModule.createReviewedTask(fixture.project, fixture.source, "reviewed",
        "reviewed-revision", summary, MemoryMapStorage.forFeatureList(), Instant.now(), () -> true)
        .run();
    final var freshParameters = fixture.project.getCurrentFeatureLists().get(1).getAppliedMethods()
        .getFirst().getParameters();
    final String xml = ParameterUtils.saveValuesToXMLString(freshParameters);
    final IntensityNormalizerParameters loaded = new IntensityNormalizerParameters();
    ParameterUtils.loadValuesFromXMLString(loaded, xml);

    // Metadata versions are transient; the restored table content is identical.
    final StringMetadataColumn transientColumn = new StringMetadataColumn("transient");
    fixture.project.getProjectMetadata().addColumn(transientColumn);
    fixture.project.getProjectMetadata().removeColumn(transientColumn);
    final Task replay = IntensityNormalizerTask.createReplayTask(fixture.project, fixture.source,
        "replayed", loaded.getValue(IntensityNormalizerParameters.hiddenSourceRevision),
        loaded.getValue(IntensityNormalizerParameters.hiddenNormalizationSummary),
        loaded.getValue(IntensityNormalizerParameters.hiddenNativeSourceFingerprint), loaded,
        IntensityNormalizerModule.class, MemoryMapStorage.forFeatureList(), Instant.now());

    replay.run();

    assertEquals(TaskStatus.FINISHED, replay.getStatus());
    assertEquals(3, fixture.project.getCurrentFeatureLists().size());
    final ModularFeatureList copy = (ModularFeatureList) fixture.project.getCurrentFeatureLists().get(2);
    assertEquals(20f, normalizedArea(copy, fixture.first));
    assertEquals(10f, normalizedArea(copy, fixture.second));
  }

  @Test
  void replayRejectsChangedNativeInputsBeforePublishing() {
    final Fixture fixture = fixture();
    final IntensityNormalizationSummary summary = new IntensityNormalizationSummary(List.of(
        new RawFileNormalizationFunction(fixture.first, new FactorNormalizationFunction(2d)),
        new RawFileNormalizationFunction(fixture.second, new FactorNormalizationFunction(.5d))));
    final String fingerprint = NormalizationSourceRevision.capture(fixture.source,
        fixture.project.getProjectMetadata(), () -> false);
    ((ModularFeature) fixture.source.getFeature(0, fixture.first)).setArea(99f);

    final Task task = IntensityNormalizerTask.createReplayTask(fixture.project, fixture.source,
        "reviewed", "reviewed-revision", summary, fingerprint, new IntensityNormalizerParameters(),
        IntensityNormalizerModule.class, MemoryMapStorage.forFeatureList(), Instant.now());
    task.run();

    assertEquals(TaskStatus.ERROR, task.getStatus());
    assertEquals(1, fixture.project.getCurrentFeatureLists().size());
  }

  @Test
  void replayWithoutPersistedFingerprintFailsClosed() {
    final Fixture fixture = fixture();
    final IntensityNormalizationSummary summary = new IntensityNormalizationSummary(List.of(
        new RawFileNormalizationFunction(fixture.first, new FactorNormalizationFunction(2d)),
        new RawFileNormalizationFunction(fixture.second, new FactorNormalizationFunction(.5d))));
    final Task task = IntensityNormalizerTask.createReplayTask(fixture.project, fixture.source,
        "reviewed", "reviewed-revision", summary, null, new IntensityNormalizerParameters(),
        IntensityNormalizerModule.class, MemoryMapStorage.forFeatureList(), Instant.now());
    task.run();

    assertEquals(TaskStatus.ERROR, task.getStatus());
    assertEquals(1, fixture.project.getCurrentFeatureLists().size());
  }

  @Test
  void snapshotChangingAfterFrozenApplicationDoesNotPublishAndLeavesSourceUntouched() {
    final Fixture fixture = fixture();
    final IntensityNormalizationSummary summary = new IntensityNormalizationSummary(List.of(
        new RawFileNormalizationFunction(fixture.first, new FactorNormalizationFunction(2d)),
        new RawFileNormalizationFunction(fixture.second, new FactorNormalizationFunction(.5d))));
    final AtomicInteger checks = new AtomicInteger();
    final Task task = IntensityNormalizerModule.createReviewedTask(fixture.project, fixture.source,
        "reviewed", "revision", summary, MemoryMapStorage.forFeatureList(), Instant.now(),
        () -> checks.incrementAndGet() == 1);

    task.run();

    assertEquals(TaskStatus.ERROR, task.getStatus());
    assertEquals(2, checks.get());
    assertEquals(1, fixture.project.getCurrentFeatureLists().size());
    assertNull(((ModularFeature) fixture.source.getFeature(0, fixture.first)).get(NormalizedAreaType.class));
    assertNull(((ModularFeature) fixture.source.getFeature(0, fixture.second)).get(NormalizedAreaType.class));
  }

  @Test
  void appliesFrozenPerRowFactorsAndLeavesUnavailableRowsWithoutNormalizedValues() {
    final Fixture fixture = fixture();
    addRow(fixture.source, 2, fixture.first, 30f, fixture.second, 40f);
    final IntensityNormalizationSummary summary = new IntensityNormalizationSummary(List.of(
        new RawFileNormalizationFunction(fixture.first,
            new FeatureRowNormalizationFunction(Map.of(1, 2d), Map.of(2, "missing QC"))),
        new RawFileNormalizationFunction(fixture.second,
            new FeatureRowNormalizationFunction(Map.of(1, .5d), Map.of(2, "missing QC")))));
    final Task task = IntensityNormalizerModule.createReviewedTask(fixture.project, fixture.source,
        "reviewed", "revision", summary, MemoryMapStorage.forFeatureList(), Instant.now(), () -> true);

    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());
    final ModularFeatureList copy = (ModularFeatureList) fixture.project.getCurrentFeatureLists().get(1);
    assertEquals(20f, normalizedArea(copy, fixture.first));
    assertEquals(10f, normalizedArea(copy, fixture.second));
    assertNull(((ModularFeature) copy.getFeature(1, fixture.first)).get(NormalizedAreaType.class));
    assertNull(((ModularFeature) copy.getFeature(1, fixture.second)).get(NormalizedAreaType.class));
  }

  @Test
  void appliesAFeatureQcFactorToAvailableAreaWithoutManufacturingMissingHeight() {
    final Fixture fixture = fixture();
    ((ModularFeature) fixture.source.getFeature(0, fixture.first)).setHeight(null);
    final IntensityNormalizationSummary summary = new IntensityNormalizationSummary(List.of(
        new RawFileNormalizationFunction(fixture.first,
            new FeatureRowNormalizationFunction(Map.of(1, 2d), Map.of())),
        new RawFileNormalizationFunction(fixture.second,
            new FeatureRowNormalizationFunction(Map.of(1, .5d), Map.of()))));
    final Task task = IntensityNormalizerModule.createReviewedTask(fixture.project, fixture.source,
        "reviewed", "revision", summary, MemoryMapStorage.forFeatureList(), Instant.now(), () -> true);

    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());
    final ModularFeature copy = (ModularFeature) ((ModularFeatureList) fixture.project
        .getCurrentFeatureLists().get(1)).getFeature(0, fixture.first);
    assertEquals(20f, copy.get(NormalizedAreaType.class));
    assertNull(copy.get(NormalizedHeightType.class));
  }

  private static float normalizedArea(final ModularFeatureList list, final RawDataFileImpl file) {
    return ((ModularFeature) list.getFeature(0, file)).get(NormalizedAreaType.class);
  }

  private static Fixture fixture() {
    final RawDataFileImpl first = createRawFile("sample_1", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl second = createRawFile("sample_2", LocalDateTime.of(2026, 1, 1, 10, 1));
    final ModularFeatureList source = new ModularFeatureList("source", null, first, second);
    addRow(source, 1, first, 10f, second, 20f);
    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFeatureList(source);
    return new Fixture(project, source, first, second);
  }

  private record Fixture(MZmineProjectImpl project, ModularFeatureList source, RawDataFileImpl first,
                         RawDataFileImpl second) {
  }
}
