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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Combined per-batch-step measurement that merges {@link StepTimeMeasurement} (timing + heap) and
 * {@link StepStorageMeasurement} (temp file usage) into a single record, so both can be logged as
 * one CSV. Record components define the column order.
 *
 * @param step             1-based step number
 * @param name             module name of the step
 * @param secondsToFinish  wall-clock seconds the step took
 * @param usedHeapGB        used heap (GB) after the step, or null if not tracked
 * @param tempFilesCreated number of mapped temp files created during this step
 * @param reservedTempFileGB       nominal reserved space of files created during this step (GB)
 * @param usedTempFileGB           logical bytes written during this step (GB)
 * @param liveTempFiles        mapped files still alive at the end of the step (best-effort)
 * @param liveTempFileUsedGB       logical bytes of still-alive segments at the end of the step (GB,
 *                         best-effort)
 */
public record StepMeasurement(int step, String name, double secondsToFinish,
                              @Nullable String usedHeapGB, long tempFilesCreated, double reservedTempFileGB,
                              double usedTempFileGB, long liveTempFiles, double liveTempFileUsedGB) {

  public StepMeasurement(@NotNull final StepTimeMeasurement time,
      @NotNull final StepStorageMeasurement storage) {
    this(time.step(), time.name(), time.secondsToFinish(), time.usedHeapGB(),
        storage.filesCreatedInStep(), storage.reservedGBInStep(), storage.usedGBInStep(),
        storage.liveFiles(), storage.liveUsedGB());
  }
}
