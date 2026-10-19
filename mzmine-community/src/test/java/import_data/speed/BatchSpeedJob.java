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

import java.io.File;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One batch that is applied {@code warmupIterations + iterations} times.
 *
 * @param description      string that qualifies what is tested, e.g. the branch name or the
 *                         optimization under test. The report groups the measurements by this.
 * @param warmupIterations iterations that run before the measured ones, so that JIT compilation and
 *                         file system caches do not end up in the compared numbers. They are still
 *                         exported, marked as {@link SpeedTestPhase#WARMUP}.
 * @param iterations       measured iterations, marked as {@link SpeedTestPhase#PRODUCTION}
 * @param batchFile        resource path or absolute path of the batch file
 * @param files            raw data files to import, or null to use the files defined in the batch
 * @param trackMemory      samples the peak heap and reads the garbage collector counters, see
 *                         {@link MemorySampler}. Off by default because it costs performance - run
 *                         it once to learn the peak heap and the GC behaviour, keep it off when
 *                         comparing speed.
 */
public record BatchSpeedJob(@NotNull String description, int warmupIterations, int iterations,
                            @NotNull String batchFile, @Nullable List<String> files,
                            boolean trackMemory) {

  public BatchSpeedJob(@NotNull final String description, final int warmupIterations,
      final int iterations, @NotNull final String batchFile, @Nullable final List<String> files) {
    this(description, warmupIterations, iterations, batchFile, files, false);
  }

  public BatchSpeedJob(@NotNull final String description, final int iterations,
      @NotNull final String batchFile, @Nullable final List<String> files) {
    this(description, 1, iterations, batchFile, files, false);
  }

  @NotNull
  public String getBatchFileName() {
    return new File(batchFile).getName();
  }

  /**
   * @param index overall iteration index, warmups come first
   */
  @NotNull
  public SpeedTestPhase phaseOf(final int index) {
    return index < warmupIterations ? SpeedTestPhase.WARMUP : SpeedTestPhase.PRODUCTION;
  }

  /**
   * @param index overall iteration index
   * @return the 1-based index within its phase
   */
  public int iterationInPhase(final int index) {
    return index < warmupIterations ? index + 1 : index - warmupIterations + 1;
  }

  public int totalIterations() {
    return warmupIterations + iterations;
  }
}
