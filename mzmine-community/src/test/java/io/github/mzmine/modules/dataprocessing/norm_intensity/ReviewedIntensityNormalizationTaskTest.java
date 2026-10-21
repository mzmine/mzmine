package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.addRow;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  void appliesFrozenPerRowFactorsAndLeavesUnavailableRowsWithoutNormalizedValues() {
    final Fixture fixture = fixture();
    addRow(fixture.source, 2, fixture.first, 30f, fixture.second, 40f);
    final IntensityNormalizationSummary summary = new IntensityNormalizationSummary(List.of(
        new RawFileNormalizationFunction(fixture.first,
            new FeatureRowNormalizationFunction(Map.of(1, 2d), Set.of(2))),
        new RawFileNormalizationFunction(fixture.second,
            new FeatureRowNormalizationFunction(Map.of(1, .5d), Set.of(2)))));
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
