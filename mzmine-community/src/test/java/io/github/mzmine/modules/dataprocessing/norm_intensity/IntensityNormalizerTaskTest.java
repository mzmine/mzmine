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
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createFactorParameters;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

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
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS), "norm",
        NormalizationType.AverageIntensity, createFactorParameters(), measure, handleOriginal,
        List.of());
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
    addRow(featureList, 1, qc1, 2f, qc2, 1f);
    addRow(featureList, 2, qc1, 4f, qc2, 1f);

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

    // factor(qc_1)=1.0: heights stay the same
    assertEquals(2f, featureNormalizedHeight(out, 0, qc1), 1e-5f);
    assertEquals(4f, featureNormalizedHeight(out, 1, qc1), 1e-5f);

    // factor(qc_2)=3.0: heights tripled
    assertEquals(3f, featureNormalizedHeight(out, 0, qc2), 1e-5f);
    assertEquals(3f, featureNormalizedHeight(out, 1, qc2), 1e-5f);
  }

  @Test
  void run_normalizesAreasCorrectly_withAverageNormalization() {
    // createFeature sets height == area, so area-based normalization yields the same factors
    final RunResult result = runTask(AbundanceMeasure.Area, OriginalFeatureListOption.KEEP);

    assertEquals(TaskStatus.FINISHED, result.task.getStatus());
    final ModularFeatureList out = result.outputList();
    final RawDataFileImpl qc1 = (RawDataFileImpl) out.getRawDataFile(0);
    final RawDataFileImpl qc2 = (RawDataFileImpl) out.getRawDataFile(1);

    assertEquals(2f, featureNormalizedArea(out, 0, qc1), 1e-5f);
    assertEquals(4f, featureNormalizedArea(out, 1, qc1), 1e-5f);
    assertEquals(3f, featureNormalizedArea(out, 0, qc2), 1e-5f);
    assertEquals(3f, featureNormalizedArea(out, 1, qc2), 1e-5f);
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

    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFeatureList(featureList);

    final IntensityNormalizerTask task = new IntensityNormalizerTask(project, featureList,
        buildParams(AbundanceMeasure.Height, OriginalFeatureListOption.KEEP), null, Instant.now());
    task.run();

    assertEquals(TaskStatus.ERROR, task.getStatus());
  }

  // --- helpers ---

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
