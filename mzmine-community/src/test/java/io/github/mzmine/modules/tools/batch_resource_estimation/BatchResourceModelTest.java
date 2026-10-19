package io.github.mzmine.modules.tools.batch_resource_estimation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.batchmode.timing.StepMeasurement;
import java.util.List;
import org.junit.jupiter.api.Test;

class BatchResourceModelTest {

  private static final int THREADS = 4;
  private static final double BASELINE = 0.3;
  private static final List<StepScalingMode> MODES = List.of(StepScalingMode.SCALES_WITH_THREADS,
      StepScalingMode.SCALES_WITH_FILES);

  /**
   * Retained heap 0.1 GB per file in both steps. The thread limited step needs 0.2 GB working
   * memory per parallel file, the alignment 0.02 * files^1.5.
   */
  private static SubsetRun syntheticRun(final int files) {
    return syntheticRun(files, 0.02 * Math.pow(files, 1.5));
  }

  private static SubsetRun syntheticRun(final int files, final double secondStepWorking) {
    return syntheticRun(files, secondStepWorking, 0);
  }

  /**
   * @param inStepPerFile heap per file of the thread limited step that is alive during the step
   *                      on top of the retained data, e.g. input and results at the same time
   */
  private static SubsetRun syntheticRun(final int files, final double secondStepWorking,
      final double inStepPerFile) {
    final double retained = BASELINE + 0.1 * files;
    final double threadWorking = 0.2 * Math.min(files, THREADS) + inStepPerFile * files;
    final double temp = 0.05 * files;
    return new SubsetRun(files, files, BASELINE, 0, 10 * files, List.of(
        measurement(1, "Chromatogram builder", 2 * Math.max(1, files / (double) THREADS),
            retained, retained + threadWorking, temp),
        measurement(2, "Join aligner", 0.5 * files, retained, retained + secondStepWorking,
            temp)));
  }

  private static StepMeasurement measurement(final int step, final String name,
      final double seconds, final double retained, final double livePeak, final double temp) {
    return new StepMeasurement(step, name, seconds, String.valueOf(retained), livePeak + 1,
        livePeak, 0, 0, 0, 1, temp);
  }

  private static List<SubsetRun> runs() {
    return List.of(syntheticRun(4), syntheticRun(8), syntheticRun(16));
  }

  private static BatchResourceModel model() {
    return BatchResourceModel.fit(runs(), MODES, 100, THREADS);
  }

  @Test
  void extrapolatesThreadLimitedAndFileScalingSteps() {
    final BatchResourcePrediction prediction = model().predict(100);

    final StepResourcePrediction chromatograms = prediction.steps().get(0);
    // working memory stays at the 4 parallel files, the retained data grows with all files
    assertEquals(BASELINE + 10 + 0.8, chromatograms.heapGB(), 0.01);
    assertEquals(BASELINE + 10, chromatograms.retainedHeapGB(), 0.01);
    assertEquals(2 * 25, chromatograms.seconds(), 0.1);

    final StepResourcePrediction alignment = prediction.steps().get(1);
    assertEquals(BASELINE + 10 + 0.02 * 1000, alignment.heapGB(), 0.05);
    assertEquals(5, alignment.liveTempGB(), 0.01);

    assertEquals("Join aligner", prediction.heapBottleneckStep());
    assertEquals(BASELINE + 10, prediction.retainedHeapGB(), 0.01);
    assertEquals(Math.ceil(alignment.heapGB() * BatchResourceModel.GC_HEADROOM_FACTOR),
        prediction.recommendedXmxGB());
    assertEquals(1000, prediction.rows());
  }

  @Test
  void validatesOnLargestSubset() {
    final BatchResourceModel model = model();
    assertEquals(List.of(4, 8, 16),
        model.measured().stream().map(BatchResourcePrediction::files).toList());
    final FitCheck check = model.fitCheck();
    assertNotNull(check);
    assertEquals(16, check.files());
    assertTrue(check.maxRelativeError() < 0.01, "error " + check.maxRelativeError());
  }

  @Test
  void validationRunMatchesExtrapolation() {
    final BatchResourceModel model = BatchResourceModel.fit(runs(), syntheticRun(32), MODES, 100,
        THREADS);
    final ModelValidation validation = model.validation();
    assertNotNull(validation);
    assertEquals(32, validation.measured().files());
    assertTrue(validation.totals().maxRelativeError() < 0.01,
        "error " + validation.totals().maxRelativeError());
    // the charts show the validation run on its own
    assertEquals(3, model.measured().size());
    final List<String> lines = BatchResourceReport.summaryLines(model, model.predict(100), 64,
        100);
    assertTrue(lines.stream().anyMatch(line -> line.startsWith("Validation run on 32 files")),
        String.join("\n", lines));
  }

  @Test
  void largestRunIsCheckedAndNextRunDoublesIt() {
    final BatchResourceModel model = BatchResourceModel.fit(runs(), syntheticRun(32), MODES, 100,
        THREADS);
    assertNotNull(model.latestCheck());
    assertEquals(32, model.latestCheck().files());
    assertEquals(64, model.nextRunFiles());

    // an additional run is checked against the prediction of all smaller runs
    final BatchResourceModel extended = BatchResourceModel.fit(runs(), syntheticRun(32),
        List.of(syntheticRun(64)), MODES, 100, THREADS);
    assertEquals(List.of(64),
        extended.extensions().stream().map(BatchResourcePrediction::files).toList());
    assertEquals(64, extended.latestCheck().files());
    assertTrue(extended.latestCheck().maxResourceError() < 0.01);
    assertEquals(100, extended.nextRunFiles());

    // the largest run already used all files of the batch
    assertNull(BatchResourceModel.fit(runs(), syntheticRun(32), MODES, 32, THREADS)
        .nextRunFiles());
  }

  @Test
  void fileScalingStepsUseFeatureMatrix() {
    // rows grow with 10 per file, the feature matrix with files^2, the alignment with files^1.5
    final StepScaling alignment = model().steps().get(1);
    assertEquals(0.75, alignment.working().exponent(), 1e-6);
    assertEquals(StepScaling.featureMatrixSize(100, 1000),
        StepScaling.workingX(StepScalingMode.SCALES_WITH_FILES, 100, 100, THREADS, 1000), 1e-12);
  }

  @Test
  void recommendsOnlyHigherXmx() {
    // peak heap 30.3 GB needs a -Xmx of 46 GB with the GC headroom
    final BatchResourcePrediction prediction = model().predict(100);
    assertTrue(BatchResourceReport.xmxRecommendation(prediction, 32).startsWith(
        "recommended -Xmx 46 GB"));
    assertTrue(BatchResourceReport.xmxRecommendation(prediction, 64).endsWith("is enough"));
  }

  @Test
  void flatteningStepIsExtrapolatedFromLargestRuns() {
    final BatchResourceModel model = flattening();
    // the validation run shows that the extrapolation overestimated the flattening step
    final FitCheck check = model.latestCheck();
    assertNotNull(check);
    assertTrue(check.predictedHeapGB() > check.measuredHeapGB());
    assertTrue(check.maxResourceError() > EstimationOutcome.MAX_RELIABLE_DEVIATION);
    // the final fit includes the validation run and keeps the working memory flat
    assertEquals(BASELINE + 10 + 1.6, model.predict(100).steps().get(1).heapGB(), 0.05);
  }

  /**
   * The working memory of the second step grows with the files up to 16 files and stays flat in
   * the validation run with 32 files: peak heap 11.9 GB for 100 files, -Xmx 18 GB, temp 5 GB
   */
  private static BatchResourceModel flattening() {
    final double perFile = 0.1;
    return BatchResourceModel.fit(List.of(syntheticRun(4, perFile * 4),
            syntheticRun(8, perFile * 8), syntheticRun(16, perFile * 16)),
        syntheticRun(32, perFile * 16), MODES, 100, THREADS);
  }

  @Test
  void threadLimitedStepSplitsWorkingMemoryAndInStepData() {
    // 0.05 GB per file of input and results are alive while the thread limited step runs
    final double perFile = 0.05;
    final List<SubsetRun> runs = List.of(syntheticRun(4, 0, perFile),
        syntheticRun(8, 0, perFile), syntheticRun(16, 0, perFile));
    final BatchResourceModel model = BatchResourceModel.fit(runs, MODES, 100, THREADS);

    final InStepMemory inStep = model.steps().getFirst().inStep();
    assertNotNull(inStep);
    assertEquals(0.2 * THREADS, inStep.parallelGB(), 0.01);
    assertEquals(perFile, inStep.perFileGB(), 0.001);
    // working memory of the 4 threads plus the in-step data of all files on the retained heap
    assertEquals(BASELINE + 10 + 0.8 + perFile * 100, model.predict(100).steps().getFirst()
        .heapGB(), 0.05);
    assertFalse(BatchResourceReport.stepsCsv(model.predict(100)).isBlank());
  }

  @Test
  void verdictCoversReliabilityAndResources() {
    // predicted exactly: peak heap 30.3 GB for 100 files, -Xmx 46 GB, temp 5 GB
    final BatchResourceModel reliable = BatchResourceModel.fit(runs(), syntheticRun(32), MODES,
        100, THREADS);
    // the validation run deviates from its prediction: -Xmx 18 GB, temp 5 GB
    final BatchResourceModel uncertain = flattening();

    assertEquals(EstimationVerdict.ENOUGH_RESOURCES, verdict(reliable, 64, 100));
    assertEquals(EstimationVerdict.ENOUGH_RESOURCES_UNCERTAIN, verdict(uncertain, 64, 100));

    // does not fit and the estimate is uncertain, a larger test run improves it
    final EstimationOutcome largerRun = EstimationOutcome.of(uncertain, uncertain.predict(100), 8,
        100);
    assertEquals(EstimationVerdict.LARGER_RUN_NEEDED, largerRun.verdict());
    assertEquals(64, largerRun.nextRunFiles());

    // does not fit and the estimate is reliable
    final EstimationOutcome notEnough = EstimationOutcome.of(reliable, reliable.predict(100), 8,
        4);
    assertEquals(EstimationVerdict.NOT_ENOUGH_RESOURCES, notEnough.verdict());
    assertNull(notEnough.nextRunFiles());
    assertTrue(notEnough.details().getLast().startsWith("Options: increase -Xmx"),
        String.join("\n", notEnough.details()));
    assertTrue(notEnough.details().getLast().contains("stricter feature detection"));

    // uncertain, but the largest run already used all files of the batch
    final BatchResourceModel allFilesUsed = BatchResourceModel.fit(List.of(syntheticRun(4, 0.4),
        syntheticRun(8, 0.8), syntheticRun(16, 1.6)), syntheticRun(32, 1.6), MODES, 32, THREADS);
    assertEquals(EstimationVerdict.NOT_ENOUGH_RESOURCES, verdict(allFilesUsed, 1, 100));
  }

  private static EstimationVerdict verdict(final BatchResourceModel model, final double maxHeapGB,
      final double tempDirFreeGB) {
    return EstimationOutcome.of(model, model.predict(model.totalFiles()), maxHeapGB,
        tempDirFreeGB).verdict();
  }
}
