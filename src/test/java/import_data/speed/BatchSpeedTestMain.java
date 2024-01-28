/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import import_data.local.Dom250Samples;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.batchmode.timing.StepTimeMeasurement;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import testutils.TaskResult;
import testutils.TaskResult.ERROR;
import testutils.TaskResult.FINISHED;
import testutils.TaskResult.TIMEOUT;

/**
 * Speed test to log the time required to import files. Just define List of String that point to
 * resources files or absolute paths and add a test to the main method. The results will be appended
 * to {@link #speedTestFile} define a full path here as it will otherwise be relative to
 * build/target path.
 */
public class BatchSpeedTestMain {

  static final List<String> thermo = List.of("rawdatafiles/additional/astral.raw");
  static final List<String> dom = List.of("""
      rawdatafiles/DOM_a.mzML
      rawdatafiles/DOM_a_invalid_chars.mzML
      rawdatafiles/DOM_a_invalid_header.mzML
          """.split("\n"));
  private static final Logger logger = Logger.getLogger(BatchSpeedTestMain.class.getName());
  public static String speedTestFile = "D:\\git\\mzmine3\\src\\test\\java\\import_data\\speed\\speed.jsonlines";

  public static void main(String[] args) {
    String RAM = "16GB";
    // keep running and all in memory
    String inMemory = "all";
//    String inMemory = "none";
    MZmineCore.main(new String[]{"-r", "-m", inMemory});

    try {
      String description = STR."inMemory=\{inMemory}, RAM=\{RAM}";
      List<String> dom = Dom250Samples.DOM;
      List<String> dom8 = Dom250Samples.DOM.subList(0, 8);
      List<String> dom32 = Dom250Samples.DOM.subList(0, 32);
      List<String> dom64 = Dom250Samples.DOM.subList(0, 64);
      List<String> dom100 = Dom250Samples.DOM.subList(0, 100);
      List<String> dom200 = Dom250Samples.DOM.subList(0, 200);

      for (int i = 0; i < 3; i++) {
        runBatch(description, dom8, Dom250Samples.batchFile, speedTestFile);
        runBatch(description, dom32, Dom250Samples.batchFile, speedTestFile);
        runBatch(description, dom64, Dom250Samples.batchFile, speedTestFile);
        runBatch(description, dom100, Dom250Samples.batchFile, speedTestFile);
        runBatch(description, dom200, Dom250Samples.batchFile, speedTestFile);
      }

      System.exit(0);

    } catch (InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
    System.exit(1);
  }

  private static void runBatch(String description, final List<String> files, final String batchFile,
      final String speedTestFile) throws InterruptedException, IOException {

    try {
      File jsonFile = FileAndPathUtil.getRealFilePath(new File(speedTestFile), ".jsonlines");
      File tsvFile = FileAndPathUtil.getRealFilePath(new File(speedTestFile), ".tsv");

      FileAndPathUtil.createDirectory(tsvFile.getParentFile());

      List<StepTimeMeasurement> finished = runBatch(files, batchFile);

      boolean exists = tsvFile.exists();
      try (var jsonWriter = Files.newBufferedWriter(jsonFile.toPath(), StandardCharsets.UTF_8,
          StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {
        try (var tsvWriter = Files.newBufferedWriter(tsvFile.toPath(), StandardCharsets.UTF_8,
            StandardOpenOption.APPEND, StandardOpenOption.CREATE)) {

          var tsvMapper = new CsvMapper();
          var schema = tsvMapper.schemaFor(SpeedMeasurement.class).withUseHeader(!exists);
          ObjectWriter tsvObjectWriter = tsvMapper.writer(schema);

          ObjectMapper jsonMapper = new ObjectMapper();

          for (final StepTimeMeasurement step : finished) {
            double seconds = step.duration().toMillis() / 1000.0;
            var sm = new SpeedMeasurement(step.name(), new File(batchFile).getName(), description,
                files.size(), seconds);

            String tsv = tsvObjectWriter.writeValueAsString(sm);
            tsvWriter.append(tsv);

            String str = jsonMapper.writeValueAsString(sm);
            jsonWriter.append(str).append('\n');
          }
        }
      }
    } catch (Exception ex) {
      logger.info(
          STR."Failed batch \{description} for \{batchFile} with \{files.size()} files. Will continue with next task.");
    }

    MZmineCore.getProjectManager().clearProject();
  }

  public static List<StepTimeMeasurement> runBatch(final List<String> fileNames, String batchFile) {
    File[] files = fileNames.stream().map(BatchSpeedTestMain::getFileOrResource)
        .toArray(File[]::new);

    File batch = getFileOrResource(batchFile);

    BatchTask task = BatchModeModule.runBatch(MZmineCore.getProject(), batch, files, new File[0],
        Instant.now());

    TaskResult finished = switch (task.getStatus()) {
      case WAITING, PROCESSING, CANCELED -> new TIMEOUT(BatchModeModule.class);
      case FINISHED -> new FINISHED(BatchModeModule.class, 0);
      case ERROR -> new ERROR(BatchModeModule.class);
    };
    Assertions.assertInstanceOf(FINISHED.class, finished, finished.description());

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
