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
import com.google.common.collect.Range;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import testutils.MZmineTestUtil;
import testutils.TaskResult;
import testutils.TaskResult.FINISHED;

/**
 * Speed test to log the time required to import files. Just define List of String that point to
 * resources files or absolute paths and add a test to the main method. The results will be appended
 * to {@link #speedTestFile} define a full path here as it will otherwise be relative to
 * build/target path.
 */
public class ImportSpeedTestMain {

  static final List<String> thermo = List.of("rawdatafiles/additional/astral.raw");
  static final List<String> gc = List.of(
      "D:\\Data\\convert_test\\speedtest\\gc_orbi_profle\\64_zlib_lin_int\\gc_orbi_profile_a.mzML");
  static final List<String> dom = List.of("""
      rawdatafiles/DOM_a.mzML
      rawdatafiles/DOM_a_invalid_chars.mzML
      rawdatafiles/DOM_a_invalid_header.mzML
      """.split("\n"));
  public static String speedTestFile = "D:\\git\\mzmine3\\mzmine-community\\src\\test\\java\\import_data\\speed\\speed.jsonlines";


  private static final Logger logger = Logger.getLogger(ImportSpeedTestMain.class.getName());

  public static void main(String[] args) {

    // keep running and all in memory
    MZmineCore.main(new String[]{"-r", "-m", "all"});

    try {
      var description = "mzml parser, advanced, RT filter 5min";

      for (int i = 0; i < 3; i++) {
        testImportSpeed("Robin, Import, Astral", description, gc, speedTestFile);
//        testImportSpeed("Robin, Import, Astral", description, thermo, speedTestFile);
//        testImportSpeed("Robin, import, dom", description, dom, speedTestFile);
      }

      System.exit(0);

    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    System.exit(1);
  }

  private static void testImportSpeed(final String name, final String description,
      final List<String> files, final String speedTestFile)
      throws InterruptedException, IOException {

    var selection = new ScanSelection(1, Range.closed(30f, 35f));
    var advanced = AdvancedSpectraImportParameters.create(
        MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL, 1E5, 1E3, Range.closed(100d, 1000d),
        selection, true);
    TaskResult finished = importFiles(files, 5 * 60, advanced);

    if (finished instanceof FINISHED f) {
      System.gc(); // better memory tracking
      var sm = new SpeedMeasurement(name, null, description, files.size(), f.getSeconds(),
          "%.2f".formatted(ConfigService.getConfiguration().getUsedMemoryGB()));
      appendToFile(speedTestFile, sm);
    }

    ProjectService.getProjectManager().clearProject();
  }

  private static void appendToFile(final String speedTestFile, final SpeedMeasurement sm)
      throws IOException {
    var file = new File(speedTestFile);

    try (var fileWriter = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
        WriterOptions.APPEND.toOpenOption())) {
      var jsonWriter = new ObjectMapper();
      String str = jsonWriter.writeValueAsString(sm);
      fileWriter.append(str).append('\n');
    }
  }


  public static TaskResult importFiles(final List<String> fileNames, long timeoutSeconds,
      @Nullable AdvancedSpectraImportParameters advanced) throws InterruptedException {
    File[] files = fileNames.stream().map(name -> {
      var file = new File(name);
      if (file.exists()) {
        return file;
      }
      return new File(ImportSpeedTestMain.class.getClassLoader().getResource(name).getFile());
    }).toArray(File[]::new);

    ParameterSet paramDataImport = AllSpectralDataImportParameters.create(true, files, null, null,
        advanced);

    logger.info("Testing data import of mzML and mzXML without advanced parameters");

    TaskResult finished = MZmineTestUtil.callModuleWithTimeout(timeoutSeconds,
        AllSpectralDataImportModule.class, paramDataImport);

    // should have finished by now
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, finished, finished.description());
    return finished;
  }

}
