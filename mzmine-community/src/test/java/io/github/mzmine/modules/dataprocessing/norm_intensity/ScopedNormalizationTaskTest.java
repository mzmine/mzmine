package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.addRow;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScopedNormalizationTaskTest {

  @Test
  void createsCopyWithExactFactorsAndPreservesRawSource() {
    final Fixture fixture = fixture();
    final var task = new ScopedNormalizationTask(fixture.project, fixture.source, parameters(fixture.source),
        MemoryMapStorage.forFeatureList(), Instant.now(), () -> true);

    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());
    assertEquals(2, fixture.project.getCurrentFeatureLists().size());
    final ModularFeatureList copy = (ModularFeatureList) fixture.project.getCurrentFeatureLists().get(1);
    assertEquals(10f, fixture.source.getFeature(0, fixture.first).getArea());
    assertEquals(20f, fixture.source.getFeature(0, fixture.second).getArea());
    assertEquals(20f, normalized(copy, fixture.first));
    assertEquals(10f, normalized(copy, fixture.second));
    assertEquals(2d, IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(copy,
        fixture.first).orElseThrow().getNormalizationFactor(100d, 5f));
    assertEquals(1, copy.getAppliedMethods().size());
    assertEquals(ScopedNormalizationModule.class, copy.getAppliedMethods().getFirst().getModule().getClass());
  }

  @Test
  void persistsAndRejectsInvalidFactors() {
    final Map<String, Double> factors = new LinkedHashMap<>();
    factors.put("s1", 2d);
    factors.put("s2", 0.5d);
    assertEquals(factors, ScopedNormalizationParameters.decodeFactors(
        ScopedNormalizationParameters.encodeFactors(factors)));
    assertThrows(IllegalArgumentException.class, () -> ScopedNormalizationParameters.decodeFactors("s1=NaN"));
    assertThrows(IllegalArgumentException.class, () -> ScopedNormalizationParameters.decodeFactors("s1=1;s1=2"));
  }

  @Test
  void scopedHiddenSummarySurvivesParameterXmlRoundtrip() throws Exception {
    final ScopedNormalizationParameters parameters = new ScopedNormalizationParameters();
    parameters.setParameter(ScopedNormalizationParameters.featureLists,
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS));
    parameters.setParameter(ScopedNormalizationParameters.hiddenNormalizationSummary,
        new IntensityNormalizationSummary(List.of(new RawFileNormalizationFunction(
            createRawFile("roundtrip", LocalDateTime.of(2026, 1, 1, 1, 0)),
            new FactorNormalizationFunction(2d)))));
    final String xml = ParameterUtils.saveValuesToXMLString(parameters);
    final ScopedNormalizationParameters loaded = new ScopedNormalizationParameters();
    ParameterUtils.loadValuesFromXMLString(loaded, xml);

    assertEquals(2d, loaded.getValue(ScopedNormalizationParameters.hiddenNormalizationSummary)
        .get(0).getNormalizationFactor(100d, 5f));
  }

  @Test
  void scopedOperationSupersedesEarlierIntensityNormalizerForNewFeatures() {
    final RawDataFileImpl raw = createRawFile("latest", LocalDateTime.of(2026, 1, 1, 1, 0));
    final ModularFeatureList list = new ModularFeatureList("source", null, raw);
    final IntensityNormalizerParameters earlier = new IntensityNormalizerParameters();
    earlier.setParameter(IntensityNormalizerParameters.hiddenNormalizationSummary,
        new IntensityNormalizationSummary(List.of(new RawFileNormalizationFunction(raw,
            new FactorNormalizationFunction(3d)))));
    list.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(IntensityNormalizerModule.class,
        earlier, Instant.now()));
    final ScopedNormalizationParameters latest = new ScopedNormalizationParameters();
    latest.setParameter(ScopedNormalizationParameters.hiddenNormalizationSummary,
        new IntensityNormalizationSummary(List.of(new RawFileNormalizationFunction(raw,
            new FactorNormalizationFunction(2d)))));
    list.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(ScopedNormalizationModule.class,
        latest, Instant.now()));

    assertEquals(2d, IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(list, raw)
        .orElseThrow().getNormalizationFactor(100d, 5f));
  }

  @Test
  void staleGuardPreventsCopyPublication() {
    final Fixture fixture = fixture();
    final var task = new ScopedNormalizationTask(fixture.project, fixture.source, parameters(fixture.source),
        MemoryMapStorage.forFeatureList(), Instant.now(), () -> false);

    task.run();

    assertEquals(TaskStatus.ERROR, task.getStatus());
    assertEquals(1, fixture.project.getCurrentFeatureLists().size());
  }

  private static float normalized(final ModularFeatureList list, final RawDataFileImpl file) {
    return ((ModularFeature) list.getFeature(0, file)).get(NormalizedAreaType.class);
  }

  private static ScopedNormalizationParameters parameters(final ModularFeatureList source) {
    return ScopedNormalizationParameters.create(new FeatureListsSelection(source), "reviewed", "revision",
        Map.of("s1", 2d, "s2", 0.5d));
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
