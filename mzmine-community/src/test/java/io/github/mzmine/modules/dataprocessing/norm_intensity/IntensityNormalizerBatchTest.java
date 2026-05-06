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

import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createFeatureIntensityParameters;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.toFeatureSelections;
import static io.github.mzmine.util.FeatureListTestUtils.addRow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DoubleMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link IntensityNormalizerTask} focusing on batch-aware multi-step normalization.
 *
 * <p>All test files are named "qc_*" so they are auto-detected as {@link SampleType#QC} without
 * requiring a metadata table entry for sample type.
 *
 * <p>{@link ProjectService#getMetadata()} returns the JVM-singleton metadata table. Tests add and
 * clean up columns there via {@link BeforeEach}/{@link AfterEach}.
 */
class IntensityNormalizerBatchTest {

  /**
   * Unique column name for batch grouping — avoids clashing with other tests.
   */
  private static final String BATCH_COL = "batch_norm_test";
  /**
   * Unique column name for dilution factors — avoids clashing with other tests.
   */
  private static final String DILUTION_COL = "dilution_norm_test";

  private static final LocalDateTime T0 = LocalDateTime.of(2026, 1, 1, 10, 0);
  private static final Duration FIVE_MIN = Duration.ofMinutes(5);

  private MetadataTable metadata;
  private StringMetadataColumn batchCol;
  private DoubleMetadataColumn dilutionCol;

  private int samples = 0;
  // internal standard row
  private ModularFeatureListRow isRow;

  private @NotNull LocalDateTime nextDate() {
    return T0.plus(FIVE_MIN.multipliedBy(samples++));
  }

  final RawDataFileImpl qcA1 = createRawFile("A1_qc", nextDate());
  final RawDataFileImpl qcA2 = createRawFile("A2_qc", nextDate());
  // should only use qcA2 for interpolation if batches are active
  final RawDataFileImpl sampleA3 = createRawFile("A3_sample", nextDate());
  final RawDataFileImpl qcB1 = createRawFile("B1_qc", nextDate());
  final RawDataFileImpl qcB2 = createRawFile("B2_qc", nextDate());

  // important to define raw data file order here to match the row intensities later
  // feature list constructor sorts raw data files differently
  final List<RawDataFile> allFiles = List.of(qcA1, qcA2, sampleA3, qcB1, qcB2);
  final ModularFeatureList flist = new ModularFeatureList("fl_batch", null, allFiles);

  final MZmineProjectImpl project = new MZmineProjectImpl();

  private double bFactor = 10;
  private double aFactor = 2;


  @BeforeEach
  void setUp() {
    ProjectService.getProjectManager().setCurrentProject(project);
    metadata = ProjectService.getMetadata();
    batchCol = new StringMetadataColumn(BATCH_COL, "Test batch grouping column");
    dilutionCol = new DoubleMetadataColumn(DILUTION_COL, "Test dilution factor column");
    metadata.addColumn(batchCol);
    metadata.addColumn(dilutionCol);

    // IS row: added first (row ID 1)
    isRow = addRow(flist, 1, allFiles, List.of(1f, 1.1f, 1.2f, 1.4f, 1.6f));
    addRow(flist, 2, allFiles, List.of(10f, 20f, 12f, 30f, 60f));
    addRow(flist, 3, allFiles, List.of(10f, 20f, 12f, 30f, 60f));

    // Batch metadata
    metadata.setValue(batchCol, qcA1, "A");
    metadata.setValue(batchCol, qcA2, "A");
    metadata.setValue(batchCol, sampleA3, "A");
    metadata.setValue(batchCol, qcB1, "B");
    metadata.setValue(batchCol, qcB2, "B");

    for (RawDataFile f : allFiles) {
      double factor = f.getName().startsWith("B") ? bFactor : aFactor;
      metadata.setValue(dilutionCol, f, factor);
    }

    // use different factor for sample A3 to see that this is applied
    metadata.setValue(dilutionCol, sampleA3, 4d);

    project.addFeatureList(flist);
  }

  @AfterEach
  void tearDown() {
//    metadata.removeColumn(batchCol);
//    metadata.removeColumn(dilutionCol);
    ProjectService.getProjectManager().clearProject();
  }

  // ── Test 1: batch correction only ───────────────────────────────────────────────────────────────

  /**
   * Four QC files split into two batches. Intra-batch QC correction + inter-batch alignment must
   * bring every file to the global QC median.
   *
   * <p>Layout (both feature rows have the same abundance per file):
   *
   * <pre>
   *   File    Batch  Abundance
   *   qc_A1   A      10
   *   qc_A2   A      20   → batch-A median metric = 15
   *   qc_B1   B      30
   *   qc_B2   B      60   → batch-B median metric = 45
   *   global inter-batch median = median(15, 45) = 30
   * </pre>
   *
   * <p>Expected composite factors and normalized heights:
   *
   * <pre>
   *   qc_A1: intra=1.5 × inter=2.0   = 3.0  → 10 × 3.0 = 30
   *   qc_A2: intra=0.75 × inter=2.0  = 1.5  → 20 × 1.5 = 30
   *   qc_B1: intra=1.5 × inter=0.667 = 1.0  → 30 × 1.0 = 30
   *   qc_B2: intra=0.75 × inter=0.667= 0.75 → 60 × 0.5 = 30
   * </pre>
   */
  @Test
  void run_withBatchCorrection_normalizesAllBatchesToGlobalMedian() {
    // not needed here makes calc of values harder
    removeInternalStandardRow();

    // step 1 (metadata) OFF, step 2 (IS) OFF, step 3 (QC/batch) ON
    final ParameterSet params = IntensityNormalizerParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS), "norm_simple",
        /*metadataNorm=*/ null, NormalizationType.NoNormalization, /*isParams=*/ null,
        NormalizationType.ByFeatureIntensity,
        createFeatureIntensityParameters(FeatureIntensityNormalizationMode.MEDIAN), BATCH_COL,
        AbundanceMeasure.Height, OriginalFeatureListOption.KEEP, null);

    final IntensityNormalizerTask task = new IntensityNormalizerTask(project, flist, params, null,
        Instant.now());
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());

    final ModularFeatureList out = outputList(project, "norm_simple");

    for (int row = 0; row < flist.getNumberOfRows(); row++) {
      // batch correction should have normalized sampleA3 only by the closest qcA2 not B1
      // 24*1.5 = 36
      assertEquals(18, featureNormalizedHeight(out, row, sampleA3), 1e-4f,
          "row %d sampleA3".formatted(row));
      //
      assertEquals(30.0f, featureNormalizedHeight(out, row, qcA1), 1e-4f,
          "row %d qcA1".formatted(row));
      assertEquals(30.0f, featureNormalizedHeight(out, row, qcA2), 1e-4f,
          "row %d qcA2".formatted(row));
      assertEquals(30.0f, featureNormalizedHeight(out, row, qcB1), 1e-4f,
          "row %d qcB1".formatted(row));
      assertEquals(30.0f, featureNormalizedHeight(out, row, qcB2), 1e-4f,
          "row %d qcB2".formatted(row));
    }
  }

  @Test
  void run_withBatchCorrection_and_metadata_factor() {
    // not needed here makes calc of values harder
    removeInternalStandardRow();

    // step 1 (metadata) OFF, step 2 (IS) OFF, step 3 (QC/batch) ON
    final ParameterSet params = IntensityNormalizerParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS),
        "norm_batch_and_metadata", getMetadataNormalizationConfig(),
        NormalizationType.NoNormalization, /*isParams=*/ null, NormalizationType.ByFeatureIntensity,
        createFeatureIntensityParameters(FeatureIntensityNormalizationMode.MEDIAN), BATCH_COL,
        AbundanceMeasure.Height, OriginalFeatureListOption.KEEP, null);

    final IntensityNormalizerTask task = new IntensityNormalizerTask(project, flist, params, null,
        Instant.now());
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());

    final ModularFeatureList out = outputList(project, "norm_batch_and_metadata");

    for (int row = 0; row < flist.getNumberOfRows(); row++) {
      // batch correction should have normalized sampleA3 only by the closest qcA2 not B1
      assertEquals(1.8, featureNormalizedHeight(out, row, sampleA3), 1e-4f,
          "row %d sampleA3".formatted(row));
      // all the same because 2 rows with same values are then normalized to the same median
      assertEquals(6, featureNormalizedHeight(out, row, qcA1), 1e-4f,
          "row %d qcA1".formatted(row));
      assertEquals(6, featureNormalizedHeight(out, row, qcA2), 1e-4f,
          "row %d qcA2".formatted(row));
      assertEquals(6, featureNormalizedHeight(out, row, qcB1), 1e-4f,
          "row %d qcB1".formatted(row));
      assertEquals(6, featureNormalizedHeight(out, row, qcB2), 1e-4f,
          "row %d qcB2".formatted(row));
    }
  }

  @Test
  void run_withoutBatchCorrectionSimple() {
    // not needed here makes calc of values harder
    removeInternalStandardRow();

    // step 1 (metadata) OFF, step 2 (IS) OFF, step 3 (QC/batch) ON
    final ParameterSet params = IntensityNormalizerParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS), "norm_noBatches",
        /*metadataNorm=*/ null, NormalizationType.NoNormalization, /*isParams=*/ null,
        NormalizationType.ByFeatureIntensity,
        createFeatureIntensityParameters(FeatureIntensityNormalizationMode.MEDIAN), null,
        AbundanceMeasure.Height, OriginalFeatureListOption.KEEP, null);

    final IntensityNormalizerTask task = new IntensityNormalizerTask(project, flist, params, null,
        Instant.now());
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());

    final ModularFeatureList out = outputList(project, "norm_noBatches");

    for (int row = 0; row < flist.getNumberOfRows(); row++) {
      // batch correction should have normalized sampleA3 only by the closest qcA2 not B1
      assertEquals(12.5f, featureNormalizedHeight(out, row, sampleA3), 1e-4f,
          "row %d sampleA3".formatted(row));
      //
      assertEquals(25f, featureNormalizedHeight(out, row, qcA1), 1e-4f,
          "row %d qcA1".formatted(row));
      assertEquals(25f, featureNormalizedHeight(out, row, qcA2), 1e-4f,
          "row %d qcA2".formatted(row));
      assertEquals(25f, featureNormalizedHeight(out, row, qcB1), 1e-4f,
          "row %d qcB1".formatted(row));
      assertEquals(25f, featureNormalizedHeight(out, row, qcB2), 1e-4f,
          "row %d qcB2".formatted(row));
    }
  }

  private void removeInternalStandardRow() {
    flist.removeRow(0);
  }

  // ── Test 2: all three normalization steps + batch correction ────────────────────────────────────

  /**
   * Exercises all three normalization steps together with batch correction:
   *
   * <ol>
   *   <li>Step 1 – metadata pre-normalization: dilution factor = 2 for A and 10 for B divide mode
   *   <li>Step 2 – IS correction: one IS row with abundance = 1.0 for all files → IS factor =
   *       1/1.0 = 1.0 (no change to relative differences).
   *   <li>Step 3 – QC drift correction + inter-batch alignment (same 4-file, 2-batch layout as
   *       test 1).
   * </ol>
   *
   * <p>Because the metadata factor (0.5) and IS factor (1.0) are applied uniformly, the QC step
   * computes metrics from effective abundances that are all halved. The intra- and inter-batch
   * correction factors have the same ratios as in test 1, but the global target shifts to 15 (= 30
   * × 0.5). All regular rows must therefore normalize to 15.0.
   */
  @Test
  void run_withAllStepsAndBatchCorrection_normalizesRegularRowsToExpectedValue() {

    // Step 2: IS normalization via StandardCompounds (isRow)
    final ParameterSet isParams = StandardCompoundNormalizationTypeParameters.create(
        List.of(SampleType.values()), StandardUsageType.Nearest, 1.0d,
        toFeatureSelections(isRow), /*requireAllStandards=*/ false);

    // Step 3: QC drift correction (MEDIAN) + batch correction
    final ParameterSet qcParams = createFeatureIntensityParameters(
        FeatureIntensityNormalizationMode.MEDIAN);

    final ParameterSet params = IntensityNormalizerParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS), "norm_allsteps",
        getMetadataNormalizationConfig(), NormalizationType.StandardCompounds, isParams,
        NormalizationType.ByFeatureIntensity, qcParams, BATCH_COL, AbundanceMeasure.Height,
        OriginalFeatureListOption.KEEP, null);

    final IntensityNormalizerTask task = new IntensityNormalizerTask(project, flist, params, null,
        Instant.now());
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus(), "Task must finish without errors");

    final ModularFeatureList out = outputList(project, "norm_allsteps");

    // normalized intensities
    double[] row0 = new double[]{2.1777596473693848, 1.1977678537368774, 1.1977678537368774,
        1.0162878036499023, 0.580735981464386};
    double[] row1 = new double[]{21.777597427368164, 21.777597427368164, 11.977678298950195, 21.777597427368164, 21.777597427368164};

    for (int i = 0; i < allFiles.size(); i++) {
      final RawDataFile file = allFiles.get(i);
        final float norm0 = featureNormalizedHeight(out, 0, file);
        final float norm1 = featureNormalizedHeight(out, 1, file);
      assertEquals(row0[i], norm0, 1e-3f, "isNorm should be equal");
      assertEquals(row1[i], norm1, 1e-3f, "isNorm should be equal");
    }

    // The IS row (index 0) has different normalized values — just verify it is set and positive.
    for (RawDataFile file : allFiles) {
      final ModularFeature isFeature = (ModularFeature) out.getRow(0).getFeature(file);
      assertNotNull(isFeature);
      final Float isNorm = isFeature.get(NormalizedHeightType.class);
      assertNotNull(isNorm, "IS row must have a normalized height for " + file.getName());
      assertTrue(isNorm > 0f, "IS row normalized height must be positive for " + file.getName());
    }
  }

  private static @NotNull MetadataNormalizationConfig getMetadataNormalizationConfig() {
    return new MetadataNormalizationConfig(DILUTION_COL, MetadataNormalizationConfig.Mode.divide);
  }

  // ── helpers ──────────────────────────────────────────────────────────────────────────────────────

  private static float featureNormalizedHeight(ModularFeatureList list, int rowIndex,
      RawDataFile file) {
    final ModularFeature feature = (ModularFeature) list.getRow(rowIndex).getFeature(file);
    assertNotNull(feature,
        "Feature must exist for row %d, file %s".formatted(rowIndex, file.getName()));
    final Float value = feature.get(NormalizedHeightType.class);
    assertNotNull(value,
        "NormalizedHeightType must be set for row %d, file %s".formatted(rowIndex, file.getName()));
    return value;
  }

  private static ModularFeatureList outputList(MZmineProjectImpl project, String suffix) {
    return (ModularFeatureList) project.getCurrentFeatureLists().stream()
        .filter(f -> f.getName().endsWith(suffix)).findFirst()
        .orElseThrow(() -> new AssertionError("No output feature list found in project"));
  }
}
