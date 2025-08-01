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

package import_data.speed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.batchmode.timing.StepTimeMeasurement;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

/**
 * Speed test to log the time required to import files. Just define List of String that point to
 * resources files or absolute paths and add a test to the main method. The results will be appended
 * to speedTestFile define a full path here as it will otherwise be relative to build/target path.
 * When working with local files - put those into the import_data/local folder and create a new run
 * script similar to the main method here
 * <p>
 * Be sure to specify VM options -Xms16g -Xmx16g or similar to start with fixed memory
 */
public class BatchSpeedTestMain {

  private static final Logger logger = Logger.getLogger(BatchSpeedTestMain.class.getName());

  public static void main(String[] args) {
    String speedTestFile = "D:\\speed.jsonlines";
    String description = "mzmine4.5";
    // keep running and all in memory
//    String inMemory = "all";
    String inMemory = "none";
    boolean headLess = false;
   
//    String batchFile = "rawdatafiles/test_batch_small.xml";
//    String batchFile = "D:\\tmp\\workshop_small.mzbatch";
    String batchFile = "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\speedtest_benchmark\\Orbitrap_QE_environmental_DOM_sea_water\\0_dom_500_mzmine4-4-52.mzbatch";
//    List<String> samples = List.of("rawdatafiles/DOM_a.mzML",
//        "rawdatafiles/DOM_a_invalid_chars.mzML", "rawdatafiles/DOM_a_invalid_header.mzML",
//        "rawdatafiles/DOM_b.mzXML", "rawdatafiles/DOM_b_invalid_header.mzXML");

    List<BatchSpeedJob> jobs = List.of(new BatchSpeedJob(description, 1, batchFile, null));

    startAndRunTests(speedTestFile, headLess, inMemory, jobs);
  }

  public static void startAndRunTests(final String outFile, final boolean headLess,
      final String inMemory, List<BatchSpeedJob> jobs) {
    try (var executor = Executors.newScheduledThreadPool(2)) {

      executor.schedule(() -> {
        try {
          if (headLess) {
            MZmineCore.main(new String[]{"-r", "-m", inMemory});
          } else {
            MZmineCore.main(new String[]{"-m", inMemory});
          }
        } catch (Exception ex) {
        }
      }, 0, TimeUnit.SECONDS);
      long delay = headLess ? 1 : 6;
      executor.schedule(() -> BatchSpeedTestMain.testSpeed(outFile, headLess, inMemory, jobs),
          delay, TimeUnit.SECONDS);
    }
  }

  private static void testSpeed(final String outFile, final boolean headLess, final String inMemory,
      List<BatchSpeedJob> jobs) {
    try {
      int[] iterations = new int[jobs.size()];
      while (true) {
        boolean allDone = true;
        for (int i = 0; i < jobs.size(); i++) {
          var job = jobs.get(i);
          if (iterations[i] < job.iterations()) {
            iterations[i]++;
            allDone = false;

            String description =
                "inMemory=" + inMemory + ", " + job.description() + " " + (headLess ? "headless"
                    : "GUI");
            runBatch(description, job.files(), job.batchFile(), outFile);
          }
        }
        if (allDone) {
          break;
        }
      }

      System.exit(0);

    } catch (InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
    System.exit(1);
  }

  private static void runBatch(String description, @Nullable final List<String> files,
      final String batchFile, final String outFile) throws InterruptedException, IOException {

    System.gc();
    try {
      File jsonFile = FileAndPathUtil.getRealFilePath(new File(outFile), ".jsonlines");
      File tsvFile = FileAndPathUtil.getRealFilePath(new File(outFile), ".csv");

      FileAndPathUtil.createDirectory(tsvFile.getParentFile());

      List<StepTimeMeasurement> finished = runBatch(files, batchFile);

      boolean exists = tsvFile.exists();
      try (var jsonWriter = Files.newBufferedWriter(jsonFile.toPath(), StandardCharsets.UTF_8,
          StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
        try (var tsvWriter = Files.newBufferedWriter(tsvFile.toPath(), StandardCharsets.UTF_8,
            StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {

          logger.info("Exporting files to " + tsvFile.getAbsolutePath());

          var tsvMapper = new CsvMapper();
          var schema = tsvMapper.schemaFor(SpeedMeasurement.class).withUseHeader(!exists);
          ObjectWriter tsvObjectWriter = tsvMapper.writer(schema);

          ObjectMapper jsonMapper = new ObjectMapper();

          for (final StepTimeMeasurement step : finished) {
            double seconds = step.secondsToFinish();
            var nFiles =
                files == null ? ProjectService.getProject().getNumberOfDataFiles() : files.size();
            var sm = new SpeedMeasurement(step.name(), new File(batchFile).getName(), description,
                nFiles, seconds, step.usedHeapGB());

            String tsv = tsvObjectWriter.writeValueAsString(sm);
            tsvWriter.append(tsv);
            // disable header
            tsvObjectWriter = tsvMapper.writer(schema.withUseHeader(false));

            String str = jsonMapper.writeValueAsString(sm);
            jsonWriter.append(str).append('\n');
          }
        }
      }
    } catch (Exception ex) {
      logger.info(
          "Failed batch " + description + " for " + batchFile + " with " + (files == null ? "x"
              : files.size()) + " files. Will continue with next task.");
    }

    ProjectService.getProjectManager().clearProject();
  }

  public static List<StepTimeMeasurement> runBatch(@Nullable final List<String> fileNames,
      String batchFile) {
    File[] files = fileNames == null ? null
        : fileNames.stream().map(BatchSpeedTestMain::getFileOrResource).toArray(File[]::new);

    File batch = getFileOrResource(batchFile);

    BatchTask task = BatchModeModule.runBatchFile(ProjectService.getProject(), batch, files, null, null,
        null, Instant.now());

    Assertions.assertEquals(TaskStatus.FINISHED, task.getStatus());

    return task.getStepTimes();
  }

  @NotNull
  private static File getFileOrResource(final String name) {
    var file = new File(name);
    if (file.exists()) {
      return file;
    }
    return new File(BatchSpeedTestMain.class.getClassLoader().getResource(name).getFile());
  }

}
