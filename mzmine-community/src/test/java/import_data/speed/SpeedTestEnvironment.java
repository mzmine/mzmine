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

import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.io.SemverVersionReader;
import java.lang.management.ManagementFactory;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Everything about the machine, the JVM and the mzmine configuration that influences a speed
 * measurement. These settings are only documented in the results, they are never changed by the
 * harness - so two runs that should be compared need to use the same configuration and VM options.
 * The report can use these columns to spot runs that are not comparable.
 *
 * @param mzmineVersion       mzmine version of the running build
 * @param inMemory            the {@code -m} command line argument, see
 *                            {@link io.github.mzmine.util.MemoryMapStorage}
 * @param runGCafterBatchStep {@link MZminePreferences#runGCafterBatchStep}, influences the timing
 *                            and is required for the heap measurements to be meaningful
 * @param numOfThreads        {@link MZminePreferences#numOfThreads} as resolved to a number
 * @param availableProcessors processors visible to the JVM
 * @param maxHeapGB           {@code -Xmx} as seen by the JVM
 * @param javaVersion         runtime version of the JVM
 * @param memoryVmArgs        the {@code -X} and {@code -XX} VM options, other arguments are dropped
 *                            because they are usually IDE specific and not related to performance
 * @param osName              operating system name and version
 */
public record SpeedTestEnvironment(@NotNull String mzmineVersion, @NotNull String inMemory,
                                   boolean runGCafterBatchStep, int numOfThreads,
                                   int availableProcessors, double maxHeapGB,
                                   @NotNull String javaVersion, @NotNull String memoryVmArgs,
                                   @NotNull String osName) {

  @NotNull
  public static SpeedTestEnvironment detect(@NotNull final String inMemory) {
    final boolean gc = Objects.requireNonNullElse(
        ConfigService.getPreference(MZminePreferences.runGCafterBatchStep), false);
    // only the memory and GC related options matter for a comparison, the rest is IDE noise
    final String vmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
        .filter(arg -> arg.startsWith("-X")).collect(Collectors.joining(" "));

    return new SpeedTestEnvironment(SemverVersionReader.getMZmineVersion().toString(), inMemory, gc,
        ConfigService.getConfiguration().getNumOfThreads(),
        Runtime.getRuntime().availableProcessors(),
        Math.round(Runtime.getRuntime().maxMemory() / 1e7) / 100d, Runtime.version().toString(),
        vmArgs, System.getProperty("os.name") + " " + System.getProperty("os.version"));
  }

  /**
   * Human readable summary for the log and the report header.
   */
  @NotNull
  public String describe() {
    return String.join(", ", "mzmine " + mzmineVersion, "inMemory=" + inMemory,
        "runGCafterBatchStep=" + runGCafterBatchStep,
        "threads=" + numOfThreads + "/" + availableProcessors, "maxHeap=" + maxHeapGB + " GB",
        "java " + javaVersion, memoryVmArgs.isBlank() ? "no -X VM options" : memoryVmArgs, osName);
  }
}
