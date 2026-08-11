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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.batchmode.LoadedBatchQueue;
import io.github.mzmine.modules.batchmode.timing.StepMeasurement;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

/**
 * Speed test to log the time required to apply a batch. Just define the batch (and optionally a list
 * of Strings that point to resource files or absolute paths) and add a test to the main method. The
 * results are appended to speedTestFile as csv and jsonlines - define a full path here as it will
 * otherwise be relative to build/target path. When working with local files - put those into the
 * import_data/local folder and create a new run script similar to the main method here.
 * {@link SpeedReportMain} turns the export into a html report that compares the runs.
 * <p>
 * Every job runs {@link BatchSpeedJob#warmupIterations()} warmup iterations before the measured
 * ones. Warmups are exported as well but marked as {@link SpeedTestPhase#WARMUP} so that the report
 * can drop them - they carry the JIT compilation and the cold file system caches.
 * <p>
 * All measurements that {@link BatchTask} collects are exported: time, used heap and the memory
 * mapped temp file statistics per step, plus a {@link BatchTask#WHOLE_BATCH_NAME} row. Per iteration
 * it also exports the {@link SpeedIterationStats}: the feature list result fingerprint, the temp
 * directory disk usage and, when {@link BatchSpeedJob#trackMemory()} is on, the peak heap and the
 * garbage collection.
 * <p>
 * Started either from the IDE (edit the values in the main) or through the gradle task
 * {@code gradlew batchSpeedTest -Pspeed.description=... -Pspeed.batch=...}, which fixes the heap to
 * 32 GB, or to {@code -Pspeed.heapGb=...} when given, so that runs on different branches stay
 * comparable. The task passes its configuration as {@code -Dmzmine.speed.*} system properties.
 * <p>
 * The harness never changes a preference. The performance relevant configuration
 * ({@link io.github.mzmine.gui.preferences.MZminePreferences#runGCafterBatchStep},
 * {@link io.github.mzmine.gui.preferences.MZminePreferences#numOfThreads}, the memory option, the VM
 * arguments, ...) is only documented in every exported row, see {@link SpeedTestEnvironment}. Two
 * runs are only comparable when those columns match, so keep the configuration and the VM options
 * identical when comparing branches.
 * <p>
 * Be sure to specify VM options -Xms16g -Xmx16g or similar to start with fixed memory. Note that the
 * used heap is only measured when runGCafterBatchStep is enabled in the preferences, which also
 * changes the timings - so enable or disable it for all compared runs.
 */
public class BatchSpeedTestMain {

  private static final Logger logger = Logger.getLogger(BatchSpeedTestMain.class.getName());
  private static final String PROPERTY_PREFIX = "mzmine.speed.";

  public static void main(String[] args) {
    String speedTestFile = property("out", "D:\\speed");
    // qualifies what is tested, e.g. the branch name or the optimization under test
    String description = property("description", "master4.10.26");
    // keep running and all in memory
//    String inMemory = "all";
    String inMemory = property("inMemory", "none");
    boolean headLess = booleanProperty("headless", false);
    boolean dataImportOnce = booleanProperty("importOnce", true);
    // opt in, the sampler costs performance
    boolean trackMemory = booleanProperty("trackMemory", false);
    int warmups = intProperty("warmups", 1);
    int iterations = intProperty("iterations", 4);

//    String batchFile = "rawdatafiles/test_batch_small.xml";
//    String batchFile = "D:\\tmp\\workshop_small.mzbatch";
//    String batchFile = property("batch", "D:\\Data\\batch\\test_small_microbes.mzbatch");
    String batchFile = property("batch",
        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\speedtest_benchmark\\Orbitrap_QE_environmental_DOM_sea_water\\0_dom_250_mzmine4-10-25-factor_speed.mzbatch");
//    List<String> samples = List.of("rawdatafiles/DOM_a.mzML",
//        "rawdatafiles/DOM_a_invalid_chars.mzML", "rawdatafiles/DOM_a_invalid_header.mzML",
//        "rawdatafiles/DOM_b.mzXML", "rawdatafiles/DOM_b_invalid_header.mzXML");

    List<BatchSpeedJob> jobs = List.of(
        new BatchSpeedJob(description, warmups, iterations, batchFile, null, trackMemory));

    startAndRunTests(speedTestFile, headLess, inMemory, jobs, dataImportOnce);
  }

  /**
   * The gradle task passes the configuration as {@code -Dmzmine.speed.*} system properties, the
   * hardcoded values above are the fallback when the main is started from the IDE.
   */
  @NotNull
  private static String property(@NotNull final String name, @NotNull final String defaultValue) {
    final String value = System.getProperty(PROPERTY_PREFIX + name);
    return value == null || value.isBlank() ? defaultValue : value.trim();
  }

  private static boolean booleanProperty(@NotNull final String name, final boolean defaultValue) {
    return Boolean.parseBoolean(property(name, String.valueOf(defaultValue)));
  }

  private static int intProperty(@NotNull final String name, final int defaultValue) {
    try {
      return Integer.parseInt(property(name, String.valueOf(defaultValue)));
    } catch (NumberFormatException e) {
      logger.warning("Not a number for " + PROPERTY_PREFIX + name + ", using " + defaultValue);
      return defaultValue;
    }
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
          logger.log(Level.SEVERE, "Failed to start mzmine: " + ex.getMessage(), ex);
        }
      }, 0, TimeUnit.SECONDS);
      long delay = headLess ? 1 : 6;
      executor.schedule(() -> BatchSpeedTestMain.testSpeed(outFile, inMemory, jobs, dataImportOnce),
          delay, TimeUnit.SECONDS);
    }
  }

  private static void testSpeed(final String outFile, final String inMemory,
      final List<BatchSpeedJob> jobs, final boolean dataImportOnce) {
    // one id for all iterations of this JVM, so that drift within a single run stays visible
    final String runId = UUID.randomUUID().toString().substring(0, 8);
    final SpeedTestEnvironment env = SpeedTestEnvironment.detect(inMemory);
    logger.info("Speed test run %s: %s".formatted(runId, env.describe()));

    try (var writer = new SpeedMeasurementWriter(new File(outFile))) {
      for (final BatchSpeedJob job : jobs) {
        // may import data once and then do all iterations with the data
        final LoadedBatchQueue fullBatch = prepareBatch(job, dataImportOnce, runId, env, writer);

        for (int iteration = 0; iteration < job.totalIterations(); iteration++) {
          // clone so that a batch changing its own parameters cannot affect the next iteration
          runBatch(job, runId, env, job.phaseOf(iteration), job.iterationInPhase(iteration),
              fullBatch.newQueue().clone(), writer, dataImportOnce);
        }

        // clear whole project also libraries after each job
        ProjectService.getProjectManager().clearProject();
        ProjectService.getProject().clearSpectralLibrary();
      }

      logger.info("Speed test run %s finished, results in %s".formatted(runId,
          writer.getCsvFile().getAbsolutePath()));
      System.exit(0);

    } catch (IOException | ParserConfigurationException | SAXException e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
    }
    System.exit(1);
  }

  private static @NotNull LoadedBatchQueue prepareBatch(final BatchSpeedJob job,
      final boolean dataImportOnce, final String runId, final SpeedTestEnvironment env,
      final SpeedMeasurementWriter writer)
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

    if (!dataImportOnce) {
      return fullBatch;
    }

    // remove data import step and just apply it once
    if (!(fullBatch.newQueue().getFirst().getModule() instanceof AllSpectralDataImportModule)) {
      logger.severe(
          "First step needs to be AllSpectralDataImportModule for import data once option. or disable this option");
      System.exit(1);
    }

    final File[] files = filesOf(job.files());
    //
    final BatchQueue importData = new BatchQueue();
    // remove first step from full batch and run it once
    importData.add(fullBatch.newQueue().removeFirst());
    final double tempDirFreeGB = SpeedIterationStats.tempDirFreeGB();
    final BatchTask batchTask = BatchModeModule.runBatchQueue(importData,
        ProjectService.getProject(), files, null, null, null, Instant.now(), null, null);
    if (batchTask == null) {
      logger.severe("Data import once batch queue returned null");
      System.exit(1);
      return fullBatch; // unreachable, keeps the compiler happy about the null check
    }
    if (batchTask.getStatus() != TaskStatus.FINISHED) {
      logger.severe(
          "Data import once batch queue finished with bad state " + batchTask.getStatus());
      System.exit(1);
    }

    // the import is not part of the compared iterations but still exported as warmup so that import
    // regressions stay visible. Its WHOLE BATCH row is dropped, it would only duplicate the step.
    final List<StepMeasurement> importMeasurements = batchTask.getStepMeasurements().stream()
        .filter(m -> !BatchTask.WHOLE_BATCH_NAME.equals(m.name())).toList();
    // no feature lists yet after the import, the fingerprint columns are 0 for these rows
    writer.append(toRows(job, runId, env, SpeedTestPhase.WARMUP, 1, TaskStatus.FINISHED.toString(),
        numberOfFiles(job, true), importMeasurements,
        SpeedIterationStats.after(tempDirFreeGB, null)));

    // use specific files for rest steps
    for (MZmineProcessingStep<MZmineProcessingModule> step : fullBatch.newQueue()) {
      step.getParameterSet().streamForClass(RawDataFilesParameter.class).forEach(
          rawParam -> rawParam.setValue(RawDataFilesSelectionType.SPECIFIC_FILES,
              ProjectService.getProject().getDataFiles()));
    }

    return fullBatch;
  }

  private static void runBatch(final BatchSpeedJob job, final String runId,
      final SpeedTestEnvironment env, final SpeedTestPhase phase, final int iteration,
      final BatchQueue batchQueue, final SpeedMeasurementWriter writer,
      final boolean dataImportOnce) {

    logger.info("Speed test %s: %s iteration %d of job %s".formatted(runId, phase, iteration,
        job.description()));
    System.gc();
    try {
      // disk space of the temp dir volume before the batch, the difference shows what it consumed
      final double tempDirFreeGB = SpeedIterationStats.tempDirFreeGB();
      final MemorySampler sampler = job.trackMemory() ? MemorySampler.start() : null;

      // with dataImportOnce the import step was already applied and removed - do not override the
      // files again as there is no import step left to change
      final BatchTask task = BatchModeModule.runBatchQueue(batchQueue, ProjectService.getProject(),
          dataImportOnce ? null : filesOf(job.files()), null, null, null, Instant.now(), null,
          null);

      final MemoryMeasurement memory = sampler == null ? null : sampler.stop();
      // reads the newest feature list, so measure before the project is cleared below
      final SpeedIterationStats iterationStats = SpeedIterationStats.after(tempDirFreeGB, memory);

      if (task == null) {
        logger.severe("Batch queue returned null, no measurements for this iteration");
      } else {
        if (task.getStatus() != TaskStatus.FINISHED) {
          // still exported, the status column marks the iteration as unusable
          logger.severe(
              "Batch finished with bad state " + task.getStatus() + ": " + task.getErrorMessage());
        }
        logger.info(
            "Iteration result: %d feature lists, newest has %d rows and %d features".formatted(
                iterationStats.featureLists(), iterationStats.rows(), iterationStats.features()));
        writer.append(toRows(job, runId, env, phase, iteration, task.getStatus().toString(),
            numberOfFiles(job, dataImportOnce), task.getStepMeasurements(), iterationStats));
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE,
          "Failed batch %s %s iteration %d. Will continue with the next iteration. %s".formatted(
              job.description(), phase, iteration, ex.getMessage()), ex);
    }

    if (!dataImportOnce) {
      // skip if data import once
      ProjectService.getProjectManager().clearProject();
    } else {
      ProjectService.getProject().clearFeatureLists();
    }
  }

  @NotNull
  private static List<SpeedMeasurement> toRows(final BatchSpeedJob job, final String runId,
      final SpeedTestEnvironment env, final SpeedTestPhase phase, final int iteration,
      final String status, final int files, final List<StepMeasurement> measurements,
      final SpeedIterationStats iterationStats) {
    final String now = Instant.now().toString();
    final List<SpeedMeasurement> rows = new ArrayList<>(measurements.size());
    for (final StepMeasurement measurement : measurements) {
      rows.add(new SpeedMeasurement(now, runId, job.description(), job.getBatchFileName(), phase,
          iteration, status, files, measurement, iterationStats, env));
    }
    return rows;
  }

  /**
   * With dataImportOnce the files were already imported, so the project knows the real number.
   */
  private static int numberOfFiles(final BatchSpeedJob job, final boolean dataImportOnce) {
    if (dataImportOnce || job.files() == null) {
      return ProjectService.getProject().getNumberOfDataFiles();
    }
    return job.files().size();
  }

  private static @Nullable File @Nullable [] filesOf(@Nullable final List<String> fileNames) {
    return fileNames == null ? null
        : fileNames.stream().map(BatchSpeedTestMain::getFileOrResource).toArray(File[]::new);
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
