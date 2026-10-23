/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.addRow;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createFeatureIntensityParameters;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

class IntensityNormalizerTaskTest {

  // File names starting with "qc_" are auto-detected as SampleType.QC via SampleType.ofString,
  // so no metadata table setup is needed. Both files are reference files, which means the
  // interpolation code path in IntensityNormalizerTask.run() is never entered, and
  // ProjectService.getMetadata() is called but the empty singleton metadata is harmless.

  /**
   * Builds a parameter set for average intensity normalization with the given abundance measure.
   */
  private static ParameterSet buildParams(AbundanceMeasure measure,
      OriginalFeatureListOption handleOriginal) {
    return IntensityNormalizerParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS), "norm", null,
        NormalizationType.NoNormalization, null, NormalizationType.ByFeatureIntensity,
        createFeatureIntensityParameters(FeatureIntensityNormalizationMode.AVERAGE), null, measure,
        handleOriginal, null);
  }

  /**
   * Creates a project with two QC files, two rows, runs the task and returns the output list.
   * <p>
   * Layout: qc_1 heights/areas: 2, 4  → average = 3 qc_2 heights/areas: 1, 1  → average = 1
   * maxMetric = 3 → factor(qc_1) = 1.0, factor(qc_2) = 3.0
   */
  private static RunResult runTask(AbundanceMeasure measure,
      OriginalFeatureListOption handleOriginal) {
    final RawDataFileImpl qc1 = createRawFile("qc_1", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl qc2 = createRawFile("qc_2", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, qc1, qc2);
    addRow(featureList, 1, qc1, 2f, qc2, 8f);
    addRow(featureList, 2, qc1, 6f, qc2, 8f);

    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFeatureList(featureList);

    final IntensityNormalizerTask task = new IntensityNormalizerTask(project, featureList,
        buildParams(measure, handleOriginal), null, Instant.now());
    task.run();

    return new RunResult(project, featureList, task);
  }

  @Test
  void run_normalizesHeightsCorrectly_withAverageNormalization() {
    final RunResult result = runTask(AbundanceMeasure.Height, OriginalFeatureListOption.KEEP);

    assertEquals(TaskStatus.FINISHED, result.task.getStatus());
    final ModularFeatureList out = result.outputList();
    final RawDataFileImpl qc1 = (RawDataFileImpl) out.getRawDataFile(0);
    final RawDataFileImpl qc2 = (RawDataFileImpl) out.getRawDataFile(1);

    // factor(qc_1)= average is 4 and across samples median is 6
    double f = 6d / 4d;
    assertEquals(featureHeight(out, qc1, 0) * f, featureNormalizedHeight(out, 0, qc1), 1e-5f);
    assertEquals(featureHeight(out, qc1, 1) * f, featureNormalizedHeight(out, 1, qc1), 1e-5f);

    // factor(qc_2)= average is 8 and samples median is 8
    f = 6d / 8d;
    assertEquals(featureHeight(out, qc2, 0) * f, featureNormalizedHeight(out, 0, qc2), 1e-5f);
    assertEquals(featureHeight(out, qc2, 1) * f, featureNormalizedHeight(out, 1, qc2), 1e-5f);
  }

  private static Float featureHeight(ModularFeatureList out, RawDataFileImpl qc1, int row) {
    return out.getFeature(row, qc1).getHeight();
  }

  private static Float featureArea(ModularFeatureList out, RawDataFileImpl qc1, int row) {
    return out.getFeature(row, qc1).getArea();
  }

  @Test
  void run_normalizesAreasCorrectly_withAverageNormalization() {
    // createFeature sets height == area, so area-based normalization yields the same factors
    final RunResult result = runTask(AbundanceMeasure.Area, OriginalFeatureListOption.KEEP);

    assertEquals(TaskStatus.FINISHED, result.task.getStatus());
    final ModularFeatureList out = result.outputList();
    final RawDataFileImpl qc1 = (RawDataFileImpl) out.getRawDataFile(0);
    final RawDataFileImpl qc2 = (RawDataFileImpl) out.getRawDataFile(1);

    // factor(qc_1)= average is 4 and across samples median is 6
    double f = 6d / 4d;
    assertEquals(featureArea(out, qc1, 0) * f, featureNormalizedArea(out, 0, qc1), 1e-5f);
    assertEquals(featureArea(out, qc1, 1) * f, featureNormalizedArea(out, 1, qc1), 1e-5f);
    f = 6d / 8d;
    assertEquals(featureArea(out, qc2, 0) * f, featureNormalizedArea(out, 0, qc2), 1e-5f);
    assertEquals(featureArea(out, qc2, 1) * f, featureNormalizedArea(out, 1, qc2), 1e-5f);
  }

  @Test
  void run_outputFeatureListHasBothNormalizedTypes() {
    final RunResult result = runTask(AbundanceMeasure.Height, OriginalFeatureListOption.KEEP);
    final ModularFeatureList out = result.outputList();
    final RawDataFileImpl qc2 = (RawDataFileImpl) out.getRawDataFile(1);

    final ModularFeature feature = (ModularFeature) out.getRow(0).getFeature(qc2);
    assertNotNull(feature.get(NormalizedHeightType.class),
        "NormalizedHeightType must be set on output feature");
    assertNotNull(feature.get(NormalizedAreaType.class),
        "NormalizedAreaType must be set on output feature");
  }

  @Test
  void run_outputFeatureListHasCorrectSuffixedName() {
    final RunResult result = runTask(AbundanceMeasure.Height, OriginalFeatureListOption.KEEP);
    assertEquals("flist norm", result.outputList().getName());
  }

  @Test
  void run_outputFeatureListHasAppliedMethod() {
    final RunResult result = runTask(AbundanceMeasure.Height, OriginalFeatureListOption.KEEP);
    assertEquals(1, result.outputList().getAppliedMethods().size());
    assertEquals(IntensityNormalizerModule.class,
        result.outputList().getAppliedMethods().get(0).getModule().getClass());
  }

  @Test
  void run_keepHandling_addsBothFeatureListsToProject() {
    final RunResult result = runTask(AbundanceMeasure.Height, OriginalFeatureListOption.KEEP);
    assertEquals(2, result.project.getCurrentFeatureLists().size());
  }

  @Test
  void run_removeHandling_replacesOriginalFeatureListInProject() {
    final RunResult result = runTask(AbundanceMeasure.Height, OriginalFeatureListOption.REMOVE);
    assertEquals(1, result.project.getCurrentFeatureLists().size());
    assertEquals("flist norm", result.project.getCurrentFeatureLists().get(0).getName());
  }

  @Test
  void run_errorsWhenNoReferenceSamplesFound() {
    // Files named "sample_1" are auto-detected as SampleType.SAMPLE, not QC.
    // FactorNormalizationModuleParameters filters for QC → empty list → task must error.
    final RawDataFileImpl s1 = createRawFile("sample_1", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl s2 = createRawFile("sample_2", LocalDateTime.of(2026, 1, 1, 10, 5));

    final ModularFeatureList featureList = new ModularFeatureList("flist", null, s1, s2);
    addRow(featureList, 1, s1, 2f, s2, 1f);
    featureList.addFeatureType(DataTypes.get(NormalizedHeightType.class));
    featureList.addFeatureType(DataTypes.get(NormalizedAreaType.class));
    featureList.streamFeatures().forEach(feature -> {
      feature.set(NormalizedHeightType.class, 31f);
      feature.set(NormalizedAreaType.class, 41f);
    });
    featureList.setPreferences(featureList.getPreferences().withActiveIntensityNormalization(
        new IntensityNormalizationSummary(List.of(
            new RawFileNormalizationFunction(s1, new FactorNormalizationFunction(2d))))));

    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFeatureList(featureList);

    final IntensityNormalizerTask task = new IntensityNormalizerTask(project, featureList,
        buildParams(AbundanceMeasure.Height, OriginalFeatureListOption.KEEP), null, Instant.now());
    task.run();

    assertEquals(TaskStatus.ERROR, task.getStatus());
    assertTrue(featureList.streamFeatures().allMatch(feature ->
        Float.valueOf(31f).equals(feature.get(NormalizedHeightType.class))));
    assertEquals(1, featureList.getPreferences().getActiveIntensityNormalization().functions().size());
  }

  @Test
  void processInPlaceCancellationBeforeRunPreservesExistingNormalization() {
    final RawDataFileImpl qc1 = createRawFile("qc_1", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl qc2 = createRawFile("qc_2", LocalDateTime.of(2026, 1, 1, 10, 5));
    final ModularFeatureList source = new ModularFeatureList("flist", null, qc1, qc2);
    addRow(source, 1, qc1, 2f, qc2, 8f);
    addRow(source, 2, qc1, 6f, qc2, 8f);
    source.addFeatureType(DataTypes.get(NormalizedHeightType.class));
    source.addFeatureType(DataTypes.get(NormalizedAreaType.class));
    source.streamFeatures().forEach(feature -> {
      feature.set(NormalizedHeightType.class, 77f);
      feature.set(NormalizedAreaType.class, 88f);
    });
    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFeatureList(source);
    final IntensityNormalizerTask task = new IntensityNormalizerTask(project, source,
        buildParams(AbundanceMeasure.Height, OriginalFeatureListOption.PROCESS_IN_PLACE), null,
        Instant.now());
    task.cancel();

    task.run();

    assertEquals(TaskStatus.CANCELED, task.getStatus());
    assertTrue(source.streamFeatures().allMatch(feature ->
        Float.valueOf(77f).equals(feature.get(NormalizedHeightType.class))));
    assertTrue(source.streamFeatures().allMatch(feature ->
        Float.valueOf(88f).equals(feature.get(NormalizedAreaType.class))));
    assertEquals(2f, source.getFeature(0, qc1).getHeight());
    assertEquals(8f, source.getFeature(0, qc2).getHeight());
    assertEquals(0, source.getAppliedMethods().size());
    assertEquals(1, project.getCurrentFeatureLists().size());
  }

  @Test
  void cancellationImmediatelyBeforeApplicationPreservesValuesPreferencesAndHistory() {
    final ModularFeatureList source = previouslyNormalizedList();
    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFeatureList(source);
    final ParameterSet parameters = spy(buildParams(AbundanceMeasure.Height,
        OriginalFeatureListOption.PROCESS_IN_PLACE));
    final IntensityNormalizerTask task = new IntensityNormalizerTask(project, source, parameters,
        null, Instant.now());
    final AtomicBoolean functionsCalculated = new AtomicBoolean();
    // History is prepared after all functions are calculated, just before application.
    doAnswer(invocation -> {
      assertPreviousNormalization(source);
      functionsCalculated.set(true);
      task.cancel();
      return invocation.callRealMethod();
    }).when(parameters).cloneParameterSet(true);

    task.run();

    assertTrue(functionsCalculated.get());
    assertEquals(TaskStatus.CANCELED, task.getStatus());
    assertPreviousNormalization(source);
    assertEquals(List.of(source), project.getCurrentFeatureLists());
    assertEquals("flist", source.getName());
  }

  @ParameterizedTest
  @EnumSource(OriginalFeatureListOption.class)
  void cancellationDuringApplicationCompletesValuesAndMetadataWithoutPublishing(
      @NotNull final OriginalFeatureListOption handling) {
    final ModularFeatureList source = previouslyNormalizedList();
    source.getFeature(1, source.getRawDataFile(1)).setArea(Float.NaN);
    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFeatureList(source);
    final IntensityNormalizerTask task = new IntensityNormalizerTask(project, source,
        buildParams(AbundanceMeasure.Height, handling), null, Instant.now());
    final AtomicBoolean canceledDuringApplication = new AtomicBoolean();
    final AtomicReference<ModularFeatureList> destination = new AtomicReference<>();
    if (handling.isProcessInPlace()) {
      destination.set(source);
      cancelOnFirstNormalizedValue(source, task, canceledDuringApplication);
    }

    // Observe the real output copy before it becomes visible in the project.
    try (final MockedStatic<FeatureListUtils> copies = mockStatic(FeatureListUtils.class,
        CALLS_REAL_METHODS)) {
      copies.when(() -> FeatureListUtils.createCopy(source, "norm", null, true))
          .thenAnswer(invocation -> {
            final ModularFeatureList copy = (ModularFeatureList) invocation.callRealMethod();
            assertPreviousNormalization(copy);
            destination.set(copy);
            cancelOnFirstNormalizedValue(copy, task, canceledDuringApplication);
            return copy;
          });
      task.run();
    }

    assertTrue(canceledDuringApplication.get());
    assertEquals(TaskStatus.CANCELED, task.getStatus());
    assertEquals(List.of(source), project.getCurrentFeatureLists());
    assertEquals("flist", source.getName());
    final ModularFeatureList output = destination.get();
    assertNotNull(output);
    if (handling.isProcessInPlace()) {
      assertSame(source, output);
    } else {
      assertNotSame(source, output);
      assertPreviousNormalization(source);
    }

    final float[][] expected = {{3f, 6f}, {9f, 6f}};
    final float[][] raw = {{2f, 8f}, {6f, 8f}};
    for (int row = 0; row < 2; row++) {
      for (int file = 0; file < 2; file++) {
        final ModularFeature feature = output.getFeature(row, output.getRawDataFile(file));
        assertEquals(raw[row][file], feature.getHeight());
        assertEquals(expected[row][file], feature.get(NormalizedHeightType.class));
        if (row == 1 && file == 1) {
          assertNull(feature.getArea());
          assertNull(feature.get(NormalizedAreaType.class));
        } else {
          assertEquals(raw[row][file], feature.getArea());
          assertEquals(expected[row][file], feature.get(NormalizedAreaType.class));
        }
      }
    }
    assertEquals(2, output.getAppliedMethods().size());
    assertEquals(Instant.EPOCH, output.getAppliedMethods().getFirst().getModuleCallDate());
    assertEquals(task.getModuleCallDate(), output.getAppliedMethods().getLast().getModuleCallDate());
    final IntensityNormalizationSummary active = output.getPreferences()
        .getActiveIntensityNormalization();
    final IntensityNormalizationSummary history = output.getAppliedMethods().getLast()
        .getParameters().getValue(IntensityNormalizerParameters.hiddenNormalizationSummary);
    assertEquals(active.functions(), history.functions());
    assertEquals(new FactorNormalizationFunction(1.5d), output.getPreferences()
        .getActiveNormalizationFunction(output.getRawDataFile(0)).orElseThrow());
    assertEquals(new FactorNormalizationFunction(.75d), output.getPreferences()
        .getActiveNormalizationFunction(output.getRawDataFile(1)).orElseThrow());
  }

  // --- helpers ---

  private static @NotNull ModularFeatureList previouslyNormalizedList() {
    final RawDataFileImpl qc1 = createRawFile("qc_1", LocalDateTime.of(2026, 1, 1, 10, 0));
    final RawDataFileImpl qc2 = createRawFile("qc_2", LocalDateTime.of(2026, 1, 1, 10, 5));
    final ModularFeatureList source = new ModularFeatureList("flist", null, qc1, qc2);
    addRow(source, 1, qc1, 2f, qc2, 8f);
    addRow(source, 2, qc1, 6f, qc2, 8f);
    source.streamFeatures().forEach(feature -> {
      feature.set(NormalizedHeightType.class, 77f);
      feature.set(NormalizedAreaType.class, 88f);
    });
    final IntensityNormalizationSummary previous = new IntensityNormalizationSummary(List.of(
        new RawFileNormalizationFunction(qc1, new FactorNormalizationFunction(9d)),
        new RawFileNormalizationFunction(qc2, new FactorNormalizationFunction(9d))));
    source.setPreferences(source.getPreferences().withActiveIntensityNormalization(previous));
    final ParameterSet previousParameters = buildParams(AbundanceMeasure.Height,
        OriginalFeatureListOption.PROCESS_IN_PLACE);
    previousParameters.setParameter(IntensityNormalizerParameters.hiddenNormalizationSummary,
        previous);
    source.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(
        IntensityNormalizerModule.class, previousParameters, Instant.EPOCH));
    return source;
  }

  private static void assertPreviousNormalization(@NotNull final ModularFeatureList list) {
    list.streamFeatures().forEach(feature -> {
      assertEquals(77f, feature.get(NormalizedHeightType.class));
      assertEquals(88f, feature.get(NormalizedAreaType.class));
    });
    assertEquals(1, list.getAppliedMethods().size());
    assertEquals(Instant.EPOCH, list.getAppliedMethods().getFirst().getModuleCallDate());
    assertEquals(2, list.getPreferences().getActiveIntensityNormalization().functions().size());
    for (final var file : list.getRawDataFiles()) {
      assertEquals(new FactorNormalizationFunction(9d), list.getPreferences()
          .getActiveNormalizationFunction(file).orElseThrow());
    }
  }

  private static void cancelOnFirstNormalizedValue(@NotNull final ModularFeatureList list,
      @NotNull final IntensityNormalizerTask task, @NotNull final AtomicBoolean canceled) {
    list.addFeatureTypeValueListener(DataTypes.get(NormalizedAreaType.class),
        (model, type, oldValue, newValue) -> {
          if (newValue != null && canceled.compareAndSet(false, true)) {
            task.cancel();
          }
        });
  }

  private static float featureNormalizedHeight(ModularFeatureList list, int rowIndex,
      RawDataFileImpl file) {
    final ModularFeature feature = (ModularFeature) list.getRow(rowIndex).getFeature(file);
    assertNotNull(feature);
    final Float value = feature.get(NormalizedHeightType.class);
    assertNotNull(value);
    return value;
  }

  private static float featureNormalizedArea(ModularFeatureList list, int rowIndex,
      RawDataFileImpl file) {
    final ModularFeature feature = (ModularFeature) list.getRow(rowIndex).getFeature(file);
    assertNotNull(feature);
    final Float value = feature.get(NormalizedAreaType.class);
    assertNotNull(value);
    return value;
  }

  private record RunResult(MZmineProjectImpl project, ModularFeatureList original,
                           IntensityNormalizerTask task) {

    ModularFeatureList outputList() {
      return (ModularFeatureList) project.getCurrentFeatureLists().stream()
          .filter(f -> f != original).findFirst()
          .orElseThrow(() -> new AssertionError("No output feature list found in project"));
    }
  }
}
