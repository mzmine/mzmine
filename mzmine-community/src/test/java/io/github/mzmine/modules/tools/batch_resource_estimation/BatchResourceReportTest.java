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

package io.github.mzmine.modules.tools.batch_resource_estimation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.batchmode.timing.HeapMeasurementQuality;
import io.github.mzmine.modules.batchmode.timing.StepMeasurement;
import io.github.mzmine.util.CSVParsingUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BatchResourceReportTest {

  @TempDir
  Path directory;

  private static SubsetRun run(final int files) {
    return new SubsetRun(files, files, 0.1, 0, files * 10, List.of(
        new StepMeasurement(1, "Aligner", files, String.valueOf(0.1 * files), 0.2 * files,
            0.15 * files, 0, 0, 0, 1, 0.05 * files,
            files == 8 ? HeapMeasurementQuality.PARTIAL_GC : HeapMeasurementQuality.FULL_GC)));
  }

  private static BatchResourceModel model(final boolean extended) {
    return BatchResourceModel.fit(List.of(run(4), run(8), run(16)), run(32),
        extended ? List.of(run(64)) : List.of(), List.of(StepScalingMode.SCALES_WITH_FILES), 100,
        4);
  }

  @Test
  void exportsAllRunsAndReplacesTheReportAfterAnAdditionalRun() throws Exception {
    final Path output = directory.resolve("statistics.tsv");
    final BatchResourceEstimationParameters parameters = new BatchResourceEstimationParameters();
    parameters.setParameter(BatchResourceEstimationParameters.statisticsFile, true,
        output.toFile());
    BatchResourceReport.exportStatistics(parameters, model(false));
    final List<String> first = CSVParsingUtils.readData(output.toFile(), "\t").stream()
        .map(row -> String.join("\t", row)).toList();
    final String header = "runType\tfiles\tthreads\talignedFeatureRows\tstep\tmodule\tdependsOn"
        + "\tpeakRamGB\tramReliability\tretainedRamGB\ttemporaryStorageGB\tprocessingSeconds";
    assertEquals(header, first.getFirst());
    assertEquals(7, first.size());
    assertTrue(first.get(1).startsWith("Subset (measured)\t4\t4\t40\t1\tAligner\t"));
    assertTrue(first.get(1).contains("\tReliable\t"));
    assertTrue(first.get(2).contains("\tUnreliable\t"));
    assertTrue(first.get(4).startsWith("Validation (measured)\t32\t"));
    assertTrue(first.get(5).startsWith("Validation (estimated)\t32\t"));
    assertTrue(first.getLast().startsWith("Full batch (estimated)\t100\t"));
    final String[] prediction = first.getLast().split("\t");
    assertEquals(model(false).predict(100).steps().getFirst().heapGB(),
        Double.parseDouble(prediction[7]));

    Files.writeString(output, "stale trailing content\n", StandardOpenOption.APPEND);
    BatchResourceReport.exportStatistics(parameters, model(true));
    final List<String> updated = CSVParsingUtils.readData(output.toFile(), "\t").stream()
        .map(row -> String.join("\t", row)).toList();
    assertEquals(8, updated.size());
    assertEquals(1, updated.stream().filter(header::equals).count());
    assertTrue(updated.get(6).startsWith("Additional run (measured)\t64\t"));
    assertTrue(updated.getLast().startsWith("Full batch (estimated)\t100\t"));
    assertFalse(Files.readString(output).contains("stale trailing content"));
    assertEquals(1,
        updated.stream().filter(line -> line.startsWith("Full batch (estimated)\t")).count());
  }

  @Test
  void disabledExportLeavesAnExistingFileUntouched() throws Exception {
    final Path output = directory.resolve("statistics.tsv");
    Files.writeString(output, "existing report");
    final BatchResourceEstimationParameters parameters = new BatchResourceEstimationParameters();
    parameters.setParameter(BatchResourceEstimationParameters.statisticsFile, false,
        output.toFile());
    BatchResourceReport.exportStatistics(parameters, model(false));
    assertEquals("existing report", Files.readString(output));
  }

  @Test
  void exportFailureIsReportedToTheCallingTask() {
    final BatchResourceEstimationParameters parameters = new BatchResourceEstimationParameters();
    parameters.setParameter(BatchResourceEstimationParameters.statisticsFile, true,
        directory.toFile());
    assertThrows(IOException.class,
        () -> BatchResourceReport.exportStatistics(parameters, model(false)));
  }
}
