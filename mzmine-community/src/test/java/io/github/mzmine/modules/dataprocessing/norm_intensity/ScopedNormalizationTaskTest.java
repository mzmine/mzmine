package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.addRow;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ScopedNormalizationTaskTest {

  @Test
  void createsCopyWithExactFactorsAndPreservesRawSource() {
    final Fixture fixture = fixture();
    final var task = ScopedNormalizationModule.createReviewedTask(fixture.project, fixture.source,
        parameters(fixture.source), MemoryMapStorage.forFeatureList(), Instant.now(), () -> true);

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
    assertTrue(copy.getAppliedMethods().getFirst().getDescription().contains("source revision revision"));
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
    assertThrows(IllegalArgumentException.class,
        () -> ScopedNormalizationParameters.decodeRowFactors("1@s1=1;s1=2"));
    assertThrows(IllegalArgumentException.class,
        () -> ScopedNormalizationParameters.decodeRowFactors("1@s1=1;s2=2|1@s1=1;s2=2"));
  }

  @Test
  void factoriesClearOppositePayloadLeftInSharedParameterPrototypes() {
    final String oldFactors = ScopedNormalizationParameters.factors.getValue();
    final String oldRowFactors = ScopedNormalizationParameters.rowFactors.getValue();
    final String oldUnavailable = ScopedNormalizationParameters.unavailableRowIds.getValue();
    try {
      ScopedNormalizationParameters.rowFactors.setValue("1@s1=2;s2=0.5");
      ScopedNormalizationParameters.unavailableRowIds.setValue("2");
      final ScopedNormalizationParameters perFile = ScopedNormalizationParameters.create(
          new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS), "file", "revision",
          Map.of("s1", 2d));
      assertEquals("", perFile.getValue(ScopedNormalizationParameters.rowFactors));
      assertEquals("", perFile.getValue(ScopedNormalizationParameters.unavailableRowIds));

      ScopedNormalizationParameters.factors.setValue("s1=9");
      final ScopedNormalizationParameters perRow = ScopedNormalizationParameters.createRowFactors(
          new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS), "row", "revision",
          Map.of(1, Map.of("s1", 2d)), Set.of());
      assertEquals("", perRow.getValue(ScopedNormalizationParameters.factors));
    } finally {
      ScopedNormalizationParameters.factors.setValue(oldFactors);
      ScopedNormalizationParameters.rowFactors.setValue(oldRowFactors);
      ScopedNormalizationParameters.unavailableRowIds.setValue(oldUnavailable);
    }
  }

  @Test
  void appliesExactPerRowFactorsAndLeavesUnavailableRowsWithoutNormalizedValues() throws Exception {
    final Fixture fixture = twoRowFixture();
    final ModularFeature unavailableSource = (ModularFeature) fixture.source.getFeature(1, fixture.first);
    unavailableSource.set(NormalizedAreaType.class, 99f);
    unavailableSource.set(NormalizedHeightType.class, 88f);
    final Map<Integer, Map<String, Double>> factors = Map.of(1, Map.of("s1", 2d, "s2", .5d));
    final ScopedNormalizationParameters parameters = ScopedNormalizationParameters.createRowFactors(
        new FeatureListsSelection(fixture.source), "feature qc", "revision", factors, Set.of(2));
    final String xml = ParameterUtils.saveValuesToXMLString(parameters);
    final ScopedNormalizationParameters loaded = new ScopedNormalizationParameters();
    ParameterUtils.loadValuesFromXMLString(loaded, xml);
    assertEquals(factors, ScopedNormalizationParameters.decodeRowFactors(
        loaded.getValue(ScopedNormalizationParameters.rowFactors)));
    assertEquals(Set.of(2), ScopedNormalizationParameters.decodeUnavailableRowIds(
        loaded.getValue(ScopedNormalizationParameters.unavailableRowIds)));

    final var task = ScopedNormalizationModule.createReviewedTask(fixture.project, fixture.source,
        parameters, MemoryMapStorage.forFeatureList(), Instant.now(), () -> true);
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());
    final ModularFeatureList copy = (ModularFeatureList) fixture.project.getCurrentFeatureLists().get(1);
    assertEquals(20f, normalized(copy, fixture.first));
    assertEquals(10f, normalized(copy, fixture.second));
    assertEquals(20f, normalizedHeight(copy, fixture.first));
    assertEquals(10f, normalizedHeight(copy, fixture.second));
    assertNull(copy.getFeature(1, fixture.first).get(NormalizedAreaType.class));
    assertNull(copy.getFeature(1, fixture.first).get(NormalizedHeightType.class));
    assertEquals(5f, fixture.source.getFeature(1, fixture.first).getArea());
    assertEquals(99f, ((ModularFeature) fixture.source.getFeature(1, fixture.first)).get(NormalizedAreaType.class));
    final ScopedNormalizationParameters applied = (ScopedNormalizationParameters) copy.getAppliedMethods()
        .getFirst().getParameters();
    final FeatureRowNormalizationFunction firstFunction = assertInstanceOf(
        FeatureRowNormalizationFunction.class,
        applied.getValue(ScopedNormalizationParameters.hiddenNormalizationSummary).get(0));
    assertEquals(2d, firstFunction.getFactorForRow(1).orElseThrow());
    assertTrue(firstFunction.getFactorForRow(2).isEmpty());
    assertEquals(Set.of(2), firstFunction.unavailableRowIds());
    assertThrows(UnsupportedOperationException.class,
        () -> firstFunction.getNormalizationFactor(100d, 5f));
    final String appliedXml = ParameterUtils.saveValuesToXMLString(applied);
    final ScopedNormalizationParameters reloaded = new ScopedNormalizationParameters();
    ParameterUtils.loadValuesFromXMLString(reloaded, appliedXml);
    assertInstanceOf(FeatureRowNormalizationFunction.class,
        reloaded.getValue(ScopedNormalizationParameters.hiddenNormalizationSummary).get(0));
  }

  @Test
  void rejectsUnknownSamplesRowsAndIncompleteFeatureFactorCoverage() {
    final Fixture fixture = twoRowFixture();
    assertRowTaskError(fixture, Map.of(1, Map.of("s1", 1d, "s3", 1d)), Set.of(2));
    assertRowTaskError(fixture, Map.of(3, Map.of("s1", 1d, "s2", 1d)), Set.of(2));
    assertRowTaskError(fixture, Map.of(1, Map.of("s1", 1d, "s2", 1d)), Set.of());
  }

  @Test
  void cancellationBeforePublicationDoesNotAddFeatureSpecificCopy() {
    final Fixture fixture = twoRowFixture();
    final AtomicReference<Task> taskRef = new AtomicReference<>();
    final var task = ScopedNormalizationModule.createReviewedTask(fixture.project, fixture.source,
        ScopedNormalizationParameters.createRowFactors(new FeatureListsSelection(fixture.source), "feature qc",
            "revision", Map.of(1, Map.of("s1", 1d, "s2", 1d)), Set.of(2)),
        MemoryMapStorage.forFeatureList(), Instant.now(), () -> {
          if (taskRef.get() != null) taskRef.get().cancel();
          return true;
        });
    taskRef.set(task);
    task.run();

    assertEquals(TaskStatus.CANCELED, task.getStatus());
    assertEquals(1, fixture.project.getCurrentFeatureLists().size());
  }

  @Test
  void rowFactorsResolveByRowIdentityAndComposeWithoutCoordinateFallback() {
    final Fixture fixture = twoRowFixture();
    final FeatureRowNormalizationFunction rowFactors = new FeatureRowNormalizationFunction(
        Map.of(1, 2d, 2, .5d), Set.of());

    FeatureDataUtils.normalizeAbundances((ModularFeature) fixture.source.getFeature(0, fixture.first),
        rowFactors);
    FeatureDataUtils.normalizeAbundances((ModularFeature) fixture.source.getFeature(1, fixture.first),
        rowFactors);

    assertEquals(20f, normalized(fixture.source, fixture.first));
    assertEquals(2.5f, ((ModularFeature) fixture.source.getFeature(1, fixture.first))
        .get(NormalizedAreaType.class));
    final NormalizationFunction composite = NormalizationFunction.merge(rowFactors,
        new FactorNormalizationFunction(3d));
    assertEquals(6d, composite.getNormalizationFactor(fixture.source.findRowByID(1), 100d, 5f)
        .orElseThrow());
    assertEquals(1.5d, composite.getNormalizationFactor(fixture.source.findRowByID(2), 100d, 5f)
        .orElseThrow());
    assertTrue(composite.getNormalizationFactor(fixture.source.findRowByID(99), 100d, 5f)
        .isEmpty());
    final var newRow = addRow(fixture.source, 3, fixture.first, 7f, fixture.second, 9f);
    final ModularFeature newFeature = (ModularFeature) newRow.getFeature(fixture.first);
    newFeature.set(NormalizedAreaType.class, 99f);
    FeatureDataUtils.normalizeAbundances(newFeature, composite);
    assertNull(newFeature.get(NormalizedAreaType.class));
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
    final var task = ScopedNormalizationModule.createReviewedTask(fixture.project, fixture.source,
        parameters(fixture.source), MemoryMapStorage.forFeatureList(), Instant.now(), () -> false);

    task.run();

    assertEquals(TaskStatus.ERROR, task.getStatus());
    assertEquals(1, fixture.project.getCurrentFeatureLists().size());
  }

  @Test
  void canceledTaskDoesNotCreateACopy() {
    final Fixture fixture = fixture();
    final var task = ScopedNormalizationModule.createReviewedTask(fixture.project, fixture.source,
        parameters(fixture.source), MemoryMapStorage.forFeatureList(), Instant.now(), () -> true);

    task.cancel();
    task.run();

    assertEquals(TaskStatus.CANCELED, task.getStatus());
    assertEquals(1, fixture.project.getCurrentFeatureLists().size());
  }

  private static float normalized(final ModularFeatureList list, final RawDataFileImpl file) {
    return ((ModularFeature) list.getFeature(0, file)).get(NormalizedAreaType.class);
  }

  private static float normalizedHeight(final ModularFeatureList list, final RawDataFileImpl file) {
    return ((ModularFeature) list.getFeature(0, file)).get(NormalizedHeightType.class);
  }

  private static void assertRowTaskError(final Fixture fixture, final Map<Integer, Map<String, Double>> factors,
      final Set<Integer> unavailable) {
    assertThrows(IllegalArgumentException.class, () -> ScopedNormalizationModule.createReviewedTask(
        fixture.project, fixture.source, ScopedNormalizationParameters.createRowFactors(
            new FeatureListsSelection(fixture.source), "feature qc", "revision", factors, unavailable),
        MemoryMapStorage.forFeatureList(), Instant.now(), () -> true));
    assertEquals(1, fixture.project.getCurrentFeatureLists().size());
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

  private static Fixture twoRowFixture() {
    final Fixture fixture = fixture();
    addRow(fixture.source, 2, fixture.first, 5f, fixture.second, 8f);
    return fixture;
  }

  private record Fixture(MZmineProjectImpl project, ModularFeatureList source, RawDataFileImpl first,
                         RawDataFileImpl second) {
  }
}
