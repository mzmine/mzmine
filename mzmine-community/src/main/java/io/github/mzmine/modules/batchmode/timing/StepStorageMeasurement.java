/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.batchmode.timing;

import io.github.mzmine.util.MemoryMapSnapshot;
import org.jetbrains.annotations.NotNull;

/**
 * Per-batch-step statistics about the memory-mapped temp files created by
 * {@link io.github.mzmine.util.MemoryMapStorage}. Collected in parallel to {@link StepTimeMeasurement}
 * by taking a {@link MemoryMapSnapshot} before and after each step.
 * <p>
 * The {@code *InStep} fields are deltas of the monotonic counters (exact). {@code liveTempFiles} and
 * {@code liveTempFileUsedGB} are the current state at the end of the step and are best-effort (they depend on
 * garbage collection of the underlying segments).
 *
 * @param step               1-based step number
 * @param name               module name of the step
 * @param filesCreatedInStep number of mapped temp files created during this step
 * @param reservedGBInStep   nominal reserved space of files created during this step (GB)
 * @param usedGBInStep       logical bytes written during this step (GB)
 * @param liveFiles          mapped files still alive at the end of the step (best-effort)
 * @param liveUsedGB         logical bytes of still-alive segments at the end of the step (GB,
 *                           best-effort)
 */
public record StepStorageMeasurement(int step, String name, long filesCreatedInStep,
                                     double reservedGBInStep, double usedGBInStep, long liveFiles,
                                     double liveUsedGB) {

  public StepStorageMeasurement(final int step, final String name,
      @NotNull final MemoryMapSnapshot before, @NotNull final MemoryMapSnapshot after) {
    this(step, name, after.totalFilesCreated() - before.totalFilesCreated(),
        bytesToGB(after.totalReservedBytes() - before.totalReservedBytes()),
        bytesToGB(after.totalUsedBytes() - before.totalUsedBytes()), after.liveFiles(),
        bytesToGB(after.liveUsedBytes()));
  }

  // round to 3 decimals (MB resolution) so the CsvWriter output stays clean, matching the
  // millisecond-derived 3-decimal formatting of StepTimeMeasurement
  private static double bytesToGB(final long bytes) {
    return Math.round(bytes / 1e6) / 1e3;
  }

  @Override
  public String toString() {
    return "Step %d: %s created %d temp files (reserved %.3f GB, used %.3f GB); live: %d files / %.3f GB".formatted(
        step, name, filesCreatedInStep, reservedGBInStep, usedGBInStep, liveFiles, liveUsedGB);
  }
}
