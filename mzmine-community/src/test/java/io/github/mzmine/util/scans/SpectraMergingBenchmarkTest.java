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

package io.github.mzmine.util.scans;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import testutils.MZmineTestUtil;
import testutils.TaskResult;

/**
 * Compares the speed of the legacy guava based
 * {@link SpectraMerging#calculatedMergedMzsAndIntensitiesLegacy} and the range map based
 * {@link SpectraMerging#calculatedMergedMzsAndIntensities} by merging increasing numbers of
 * consecutive MS1 scans.
 * <p>
 * gradlew :mzmine-community:benchmark --tests
 * "io.github.mzmine.util.scans.SpectraMergingBenchmarkTest"
 */
@Tag("benchmark")
@TestInstance(Lifecycle.PER_CLASS)
@Disabled
class SpectraMergingBenchmarkTest {

  private static final Logger logger = Logger.getLogger(
      SpectraMergingBenchmarkTest.class.getName());

  private static final List<String> FILES = List.of("rawdatafiles/DOM_a.mzML",
      "rawdatafiles/DOM_b.mzXML");
  private static final int[] NUM_SCANS = {2, 5, 10, 25, 50, 100, Integer.MAX_VALUE};
  private static final MZTolerance TOLERANCE = SpectraMerging.defaultMs1MergeTol;

  private static final int WARMUP_RUNS = 3;
  private static final int MIN_RUNS = 3;
  private static final int MAX_RUNS = 20;
  private static final long MAX_NANOS_PER_CASE = 2_000_000_000L;

  /**
   * @return the sorted run times in nanoseconds
   */
  @NotNull
  private static long[] measure(@NotNull final Runnable merge) {
    for (int i = 0; i < WARMUP_RUNS; i++) {
      merge.run();
    }
    final long[] nanos = new long[MAX_RUNS];
    long total = 0;
    int runs = 0;
    while (runs < MAX_RUNS && (runs < MIN_RUNS || total < MAX_NANOS_PER_CASE)) {
      final long start = System.nanoTime();
      merge.run();
      nanos[runs] = System.nanoTime() - start;
      total += nanos[runs];
      runs++;
    }
    final long[] result = Arrays.copyOf(nanos, runs);
    Arrays.sort(result);
    return result;
  }

  private static double median(@NotNull final long[] sorted) {
    final int mid = sorted.length / 2;
    return sorted.length % 2 == 1 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2d;
  }

  private static double toMs(final double nanos) {
    return nanos / 1_000_000d;
  }

  @BeforeAll
  void importFiles() throws InterruptedException {
    MZmineTestUtil.startMzmineCore();
    MZmineTestUtil.cleanProject();
    final TaskResult result = MZmineTestUtil.importFiles(FILES, 360);
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, result, result.description());
  }

  @SuppressWarnings("deprecation")
  @Test
  void benchmarkMerging() {
    final RawDataFile[] files = ProjectService.getProject().getDataFiles();
    Arrays.sort(files, Comparator.comparing(RawDataFile::getName));
    Assertions.assertEquals(FILES.size(), files.length);

    final List<String> rows = new ArrayList<>();
    rows.add(
        "%-14s %6s %9s %8s | %12s %12s | %12s %12s %8s".formatted("file", "scans", "dps", "signals",
            "legacy med", "legacy min", "map med", "map min", "speedup"));

    for (final RawDataFile file : files) {
      final List<Scan> ms1Scans = file.getScanNumbers(1);
      for (final int requestedScans : NUM_SCANS) {
        final List<Scan> scans = ms1Scans.subList(0, Math.min(requestedScans, ms1Scans.size()));
        final int numDataPoints = scans.stream().mapToInt(Scan::getNumberOfDataPoints).sum();

        final double[][] legacy = SpectraMerging.calculatedMergedMzsAndIntensitiesLegacy(scans,
            TOLERANCE, IntensityMergingType.SUMMED, SpectraMerging.DEFAULT_CENTER_FUNCTION, null,
            null, null);
        final double[][] rangeMap = SpectraMerging.calculatedMergedMzsAndIntensities(scans,
            TOLERANCE, IntensityMergingType.SUMMED, SpectraMerging.DEFAULT_CENTER_FUNCTION, null,
            null, null);
        final String caseName = file.getName() + " " + scans.size() + " scans";
        Assertions.assertArrayEquals(legacy[0], rangeMap[0], caseName + " range map m/z");
        Assertions.assertArrayEquals(legacy[1], rangeMap[1], caseName + " range map intensity");

        final long[] legacyNanos = measure(
            () -> SpectraMerging.calculatedMergedMzsAndIntensitiesLegacy(scans, TOLERANCE,
                IntensityMergingType.SUMMED, SpectraMerging.DEFAULT_CENTER_FUNCTION, null, null,
                null));
        final long[] rangeMapNanos = measure(
            () -> SpectraMerging.calculatedMergedMzsAndIntensities(scans, TOLERANCE,
                IntensityMergingType.SUMMED, SpectraMerging.DEFAULT_CENTER_FUNCTION, null, null,
                null));

        final double legacyMedian = median(legacyNanos);
        final double rangeMapMedian = median(rangeMapNanos);
        rows.add(
            "%-14s %6d %9d %8d | %12.3f %12.3f | %12.3f %12.3f %7.1fx".formatted(file.getName(),
                scans.size(), numDataPoints, legacy[0].length, toMs(legacyMedian),
                toMs(legacyNanos[0]), toMs(rangeMapMedian), toMs(rangeMapNanos[0]),
                legacyMedian / rangeMapMedian));

        if (scans.size() == ms1Scans.size()) {
          break;
        }
      }
    }

    logger.info(
        "SpectraMerging benchmark, times in ms, tolerance " + TOLERANCE + "\n" + String.join("\n",
            rows));
  }
}
