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
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import testutils.MZmineTestUtil;
import testutils.TaskResult;
import testutils.TaskResult.FINISHED;

public class ImportSpeedTestMain {

  static final List<String> orbiCentroid = List.of(//
      "rawdatafiles/speedtest/blood_skin/bld_a.mzML",
      "rawdatafiles/speedtest/blood_skin/skin_a.mzML",
      "rawdatafiles/speedtest/blood_skin/bld_b.mzML",
      "rawdatafiles/speedtest/blood_skin/skin_b.mzML",
//      "rawdatafiles/speedtest/blood_skin/bld_c.mzML",
//      "rawdatafiles/speedtest/blood_skin/skin_c.mzML",
      "rawdatafiles/speedtest/blood_skin/bld_d.mzML",
      "rawdatafiles/speedtest/blood_skin/skin_d.mzML"//
  );
  static final String[] parameters = """
      32_int
      32_zlib_lin
      32_zlib_lin_float
      32_zlib_lin_int
      64
      64_lin_int
      64_zlib_lin
      64_zlib_lin_float
      64_zlib_lin_int
      """.split("\n");
  private static final Logger logger = Logger.getLogger(ImportSpeedTestMain.class.getName());
  static StopWatch watch = StopWatch.create();

//  static final List<String> gcOrbiProfile = List.of(
//      "rawdatafiles/speedtest/gc_orbi_profile_200mb_a.mzML",
//      "rawdatafiles/speedtest/gc_orbi_profile_200mb_b.mzML");

  public static void main(String[] args) {

    // keep running and all in memory
    MZmineCore.main(new String[]{"-r", "-m", "all"});

    var speedTestFile = "D:\\git\\mzmine3\\src\\test\\java\\import_data\\speed\\speed.jsonlines";

    try {
//      var description = "String, Woodstox, direct bytes";
      var description = "String, Woodstox, ByteArrayInputStream";
      testImportSpeed("Import skinbld_centroid", description, orbiCentroid, speedTestFile);

      for (int i = 0; i < 3; i++) {
        String gc = "rawdatafiles/speedtest/gc_orbi_profle/%s/gc_orbi_profile_a.mzML";

        for (final String parameter : parameters) {
          var files = List.of(gc.formatted(parameter));
          testImportSpeed("Import " + parameter, description, files, speedTestFile);
        }
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
    TaskResult finished = importFiles(files, 5 * 60, null);

    if (finished instanceof FINISHED f) {
      var sm = new SpeedMeasurement(name, description, files.size(), f.getSeconds());
      appendToFile(speedTestFile, sm);
    }

    MZmineCore.getProjectManager().clearProject();
  }

  private static void appendToFile(final String speedTestFile, final SpeedMeasurement sm)
      throws IOException {
    var file = new File(speedTestFile);
    try (var fileWriter = new FileWriter(file, true)) {
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

    AllSpectralDataImportParameters paramDataImport = new AllSpectralDataImportParameters();
    paramDataImport.setParameter(AllSpectralDataImportParameters.fileNames, files);
    paramDataImport.setParameter(SpectralLibraryImportParameters.dataBaseFiles, new File[0]);
    paramDataImport.setParameter(AllSpectralDataImportParameters.advancedImport, advanced != null);
    if (advanced != null) {
      var advancedP = paramDataImport.getParameter(AllSpectralDataImportParameters.advancedImport);
      advancedP.setEmbeddedParameters(advanced);
    }

    logger.info("Testing data import of mzML and mzXML without advanced parameters");

    watch.reset();
    watch.start();
    TaskResult finished = MZmineTestUtil.callModuleWithTimeout(timeoutSeconds,
        AllSpectralDataImportModule.class, paramDataImport);

    // should have finished by now
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, finished, finished.description());
    return finished;
  }

}
