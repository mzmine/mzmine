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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.batchmode.LoadedBatchQueue;
import io.github.mzmine.modules.batchmode.timing.StepTimeMeasurement;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.JsonUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.xml.sax.SAXException;

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
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  public static void main(String[] args) {
    String speedTestFile = "D:\\speed.jsonlines";
    String description = "mzmine4.10.25_fastercopy";
    // keep running and all in memory
//    String inMemory = "all";
    String inMemory = "none";
    boolean headLess = false;
    boolean dataImportOnce = true;

//    String batchFile = "rawdatafiles/test_batch_small.xml";
//    String batchFile = "D:\\tmp\\workshop_small.mzbatch";
    String batchFile = "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\speedtest_benchmark\\Orbitrap_QE_environmental_DOM_sea_water\\0_dom_250_mzmine4-10-25-factor_speed.mzbatch";
//    List<String> samples = List.of("rawdatafiles/DOM_a.mzML",
//        "rawdatafiles/DOM_a_invalid_chars.mzML", "rawdatafiles/DOM_a_invalid_header.mzML",
//        "rawdatafiles/DOM_b.mzXML", "rawdatafiles/DOM_b_invalid_header.mzXML");

    List<BatchSpeedJob> jobs = List.of(new BatchSpeedJob(description, 5, batchFile, null));

    startAndRunTests(speedTestFile, headLess, inMemory, jobs, dataImportOnce);
  }

  public static void startAndRunTests(final String outFile, final boolean headLess,
      final String inMemory, List<BatchSpeedJob> jobs, boolean dataImportOnce) {
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
      executor.schedule(
          () -> BatchSpeedTestMain.testSpeed(outFile, headLess, inMemory, jobs, dataImportOnce),
          delay, TimeUnit.SECONDS);
    }
  }

  private static void testSpeed(final String outFile, final boolean headLess, final String inMemory,
      List<BatchSpeedJob> jobs, boolean dataImportOnce) {
    try {
      for (int i = 0; i < jobs.size(); i++) {
        var job = jobs.get(i);
        // may import data once and then do all iterations with the data
        final LoadedBatchQueue fullBatch = prepareBatch(job, dataImportOnce);

        for (int iteration = 0; iteration < job.iterations(); iteration++) {
          String description =
              "inMemory=" + inMemory + ", " + job.description() + " " + (headLess ? "headless"
                  : "GUI" + (dataImportOnce ? "import once" : ""));
          runBatch(job, description, fullBatch.newQueue(), outFile, dataImportOnce);
        }

        // clear whole project also libraries after each job
        ProjectService.getProjectManager().clearProject();
        ProjectService.getProject().clearSpectralLibrary();
      }

      System.exit(0);

    } catch (InterruptedException | IOException | ParserConfigurationException | SAXException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
    System.exit(1);
  }

  private static @NonNull LoadedBatchQueue prepareBatch(BatchSpeedJob job, boolean dataImportOnce)
      throws ParserConfigurationException, IOException, SAXException {
    final LoadedBatchQueue fullBatch = BatchQueue.loadFromFile(getFileOrResource(job.batchFile()));

    // versions might have changed
    if (!fullBatch.errorMessages().isEmpty()) {
      logger.log(Level.WARNING, "Warnings during batch file import:");
      for (final String errorMessage : fullBatch.errorMessages()) {
        logger.log(Level.WARNING, errorMessage);
      }
      // parameters have updated, we need to exit
      logger.log(Level.SEVERE,
          "Exiting because some parameter sets have been updated since the batch was "
              + "created. Please update the batch file by opening it in the GUI and try again.");
      System.exit(1);
    }

    if (dataImportOnce) {
      // remove data import step and just apply it once
      if (fullBatch.newQueue().getFirst().getModule() instanceof AllSpectralDataImportModule) {
        File[] files = job.files() == null ? null
            : job.files().stream().map(BatchSpeedTestMain::getFileOrResource).toArray(File[]::new);
        //
        final BatchQueue importData = new BatchQueue();
        // remove first step from full batch and run it once
        importData.add(fullBatch.newQueue().removeFirst());
        final BatchTask batchTask = BatchModeModule.runBatchQueue(importData,
            ProjectService.getProject(), files, null, null, null, Instant.now(), null, null);
        if (batchTask == null) {
          logger.severe("Data import once batch queue returned null");
          System.exit(1);
        } else if (batchTask.getStatus() != TaskStatus.FINISHED) {
          logger.severe(
              "Data import once batch queue finished with bad state " + batchTask.getStatus());
          System.exit(1);
        }
        final StepTimeMeasurement first = batchTask.getStepTimes().getFirst();
        // maybe add to csv?

        // use specific files for rest steps
        for (MZmineProcessingStep<MZmineProcessingModule> step : fullBatch.newQueue()) {
          step.getParameterSet().streamForClass(RawDataFilesParameter.class).forEach(
              rawParam -> rawParam.setValue(RawDataFilesSelectionType.SPECIFIC_FILES,
                  ProjectService.getProject().getDataFiles()));
        }
      } else {
        logger.severe(
            "First step needs to be AllSpectralDataImportModule for import data once option. or disable this option");
        System.exit(1);
      }
    }

    return fullBatch;
  }

  private static void runBatch(BatchSpeedJob job, String description, final BatchQueue batchQueue,
      final String outFile, boolean dataImportOnce) throws InterruptedException, IOException {

    System.gc();
    try {
      File jsonFile = FileAndPathUtil.getRealFilePath(new File(outFile), ".jsonlines");
      File tsvFile = FileAndPathUtil.getRealFilePath(new File(outFile), ".csv");

      FileAndPathUtil.createDirectory(tsvFile.getParentFile());

      List<StepTimeMeasurement> finished = runBatch(job.files(), batchQueue);

      boolean exists = tsvFile.exists();
      try (var jsonWriter = Files.newBufferedWriter(jsonFile.toPath(), StandardCharsets.UTF_8,
          StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
        try (var tsvWriter = Files.newBufferedWriter(tsvFile.toPath(), StandardCharsets.UTF_8,
            StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {

          logger.info("Exporting files to " + tsvFile.getAbsolutePath());

          var tsvMapper = CsvMapper.builder().addModule(new JavaTimeModule()).build();
          var schema = tsvMapper.schemaFor(SpeedMeasurement.class).withUseHeader(!exists);
          ObjectWriter tsvObjectWriter = tsvMapper.writer(schema);

          ObjectMapper jsonMapper = JsonUtils.MAPPER;

          final String now = LocalDate.now().format(DATE_FORMATTER);

          for (final StepTimeMeasurement step : finished) {
            double seconds = step.secondsToFinish();
            var nFiles = job.files() == null ? ProjectService.getProject().getNumberOfDataFiles()
                : job.files().size();
            var sm = new SpeedMeasurement(now, step.name(), job.getBatchFileName(), description,
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
      logger.log(Level.SEVERE,
          "Failed batch " + description + " for " + batchQueue + " with " + (job.files() == null
              ? "x" : job.files().size()) + " files. Will continue with next task. "
              + ex.getMessage(), ex);
    }

    if (!dataImportOnce) {
      // skip if data import once
      ProjectService.getProjectManager().clearProject();
    } else {
      ProjectService.getProject().clearFeatureLists();
    }
  }

  public static List<StepTimeMeasurement> runBatch(@Nullable final List<String> fileNames,
      BatchQueue batchQueue) {
    File[] files = fileNames == null ? null
        : fileNames.stream().map(BatchSpeedTestMain::getFileOrResource).toArray(File[]::new);

    BatchTask task = BatchModeModule.runBatchQueue(batchQueue, ProjectService.getProject(), files,
        null, null, null, Instant.now(), null, null);

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
