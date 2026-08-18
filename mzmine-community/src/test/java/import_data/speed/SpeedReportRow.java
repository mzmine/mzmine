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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One measurement flattened for the HTML speed report. The {@link #series()} is the grouping of the
 * comparison (the description the user provided to qualify what was tested) while {@link #step()} and
 * {@link #name()} define the panel. Replicates come from the iterations of one or several runs.
 *
 * @param step        1-based step number, 0 for the whole batch. Together with the name this
 *                    separates steps that apply the same module twice in one batch.
 * @param name        module name of the step
 * @param phase       {@link SpeedTestPhase}, warmup rows are excluded by default in the report
 * @param iteration   1-based index within the phase
 * @param runId       groups the iterations of one JVM start
 * @param status      only {@code FINISHED} rows are plotted
 * @param environment human readable summary of the mzmine and JVM configuration, the report warns
 *                    when the compared series were measured with different environments
 * @see SpeedIterationStats the fields from {@code featureLists} on are measured once per iteration
 * and therefore repeated on every step row of that iteration
 */
public record SpeedReportRow(int step, @NotNull String name, @NotNull String series,
                             @NotNull String phase, int iteration, @NotNull String runId,
                             @NotNull String status, int files, double timeSeconds,
                             @Nullable Double gbRamUsed, @Nullable Double tempFilesCreated,
                             @Nullable Double reservedTempFileGB, @Nullable Double usedTempFileGB,
                             @Nullable Double liveTempFiles, @Nullable Double liveTempFileUsedGB,
                             int featureLists, @Nullable Double rows, @Nullable Double features,
                             @Nullable Double tempDirUsedGB, @Nullable Double peakHeapGB,
                             @Nullable Double gcCount, @Nullable Double gcTimeSeconds,
                             @NotNull String environment) {

}
