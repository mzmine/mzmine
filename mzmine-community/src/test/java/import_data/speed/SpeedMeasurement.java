/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
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

package import_data.speed;

import io.github.mzmine.modules.batchmode.timing.StepMeasurement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One exported row: a single batch step (or the whole batch) of a single iteration. Flattened on
 * purpose - the record components define the csv column order and the json field names. Combines
 * everything {@link StepMeasurement} collects with the identity of the run ({@link #description()},
 * {@link #runId()}, {@link #phase()}, {@link #iteration()}) and the documented
 * {@link SpeedTestEnvironment}.
 *
 * @param runDate            ISO timestamp of the moment the iteration was exported
 * @param runId              same for all iterations of one JVM start, so that iterations of one run
 *                           can be grouped and drift within a run can be inspected
 * @param description        user provided string that qualifies what is tested, e.g. the branch or
 *                           the optimization under test. This is the grouping of the report.
 * @param batchFile          file name of the batch that was applied
 * @param phase              {@link SpeedTestPhase#WARMUP} iterations are exported but excluded from
 *                           the report by default
 * @param iteration          1-based index within the phase
 * @param status             final status of the batch, only {@code FINISHED} rows are trustworthy
 * @param files              number of raw data files the batch was applied to
 * @param step               1-based step number, 0 for the whole batch row
 * @param name               module name of the step, or
 *                           {@link io.github.mzmine.modules.batchmode.BatchTask#WHOLE_BATCH_NAME}
 * @param timeSeconds        wall clock seconds of the step
 * @param gbRamUsed          used heap (GB) after the step, only set when
 *                           {@link SpeedTestEnvironment#runGCafterBatchStep()} is enabled
 * @param tempFilesCreated   memory mapped temp files created during the step
 * @param reservedTempFileGB nominal reserved space of the temp files created during the step
 * @param usedTempFileGB     logical bytes written to temp files during the step
 * @param liveTempFiles      mapped files still alive at the end of the step (best effort, depends
 *                           on garbage collection)
 * @param liveTempFileUsedGB logical bytes of still alive segments at the end of the step (best
 *                           effort)
 * @see SpeedIterationStats for the columns that are measured per iteration and therefore repeated
 * on every row of that iteration: the feature list fingerprint, the temp directory disk space and
 * the optional memory tracking
 */
public record SpeedMeasurement(@NotNull String runDate, @NotNull String runId,
                               @NotNull String description, @NotNull String batchFile,
                               @NotNull SpeedTestPhase phase, int iteration, @NotNull String status,
                               int files, int step, @NotNull String name, double timeSeconds,
                               @Nullable String gbRamUsed, long tempFilesCreated,
                               double reservedTempFileGB, double usedTempFileGB, long liveTempFiles,
                               double liveTempFileUsedGB, int featureLists, int rows, int features,
                               double tempDirFreeGBBefore, double tempDirFreeGBAfter,
                               double tempDirUsedGB, @Nullable Double peakHeapGB,
                               @Nullable Long gcCount, @Nullable Double gcTimeSeconds,
                               @NotNull String mzmineVersion, @NotNull String inMemory,
                               boolean runGCafterBatchStep, int numOfThreads,
                               int availableProcessors, double maxHeapGB,
                               @NotNull String javaVersion, @NotNull String memoryVmArgs,
                               @NotNull String osName) {

  public SpeedMeasurement(@NotNull final String runDate, @NotNull final String runId,
      @NotNull final String description, @NotNull final String batchFile,
      @NotNull final SpeedTestPhase phase, final int iteration, @NotNull final String status,
      final int files, @NotNull final StepMeasurement measurement,
      @NotNull final SpeedIterationStats iterationStats, @NotNull final SpeedTestEnvironment env) {
    this(runDate, runId, description, batchFile, phase, iteration, status, files,
        measurement.step(), measurement.name(), measurement.secondsToFinish(),
        measurement.usedHeapGB(), measurement.tempFilesCreated(), measurement.reservedTempFileGB(),
        measurement.usedTempFileGB(), measurement.liveTempFiles(), measurement.liveTempFileUsedGB(),
        iterationStats.featureLists(), iterationStats.rows(), iterationStats.features(),
        iterationStats.tempDirFreeGBBefore(), iterationStats.tempDirFreeGBAfter(),
        iterationStats.tempDirUsedGB(), iterationStats.peakHeapGB(), iterationStats.gcCount(),
        iterationStats.gcTimeSeconds(), env.mzmineVersion(), env.inMemory(),
        env.runGCafterBatchStep(), env.numOfThreads(), env.availableProcessors(), env.maxHeapGB(),
        env.javaVersion(), env.memoryVmArgs(), env.osName());
  }
}
