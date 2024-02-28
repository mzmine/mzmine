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

package import_data;

import io.github.mzmine.main.MZmineCore;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import testutils.MZmineTestUtil;

public class DataImportTestGenerator {

  private static final Logger logger = Logger.getLogger(DataImportTestGenerator.class.getName());

  /**
   * Call this method to regenerate the test data results - only if this data really has changed or
   * the structure of {@link DataFileStats} has changed
   */
  public static void main(String[] args) {
    // DEFINE PATH HERE FOR RESOURCES
    String path = "D:\\git\\mzmine3\\src\\test\\resources\\";

    List<AbstractDataImportTest> tests = List.of( //
        new MzMLImportTest() //
        , new ImzMLImportTest() //
        , new ThermoRawImportTest() //
        , new TimsTofImportTest() //
    );
//    tests = List.of(new MzMLImportTest());

    for (var entry : tests) {
      try {
        var clazz = entry.getClass();
        var files = entry.getFileNames();
        List<DataFileStats> stats = importAnalyzeFiles(files, entry);

        DataFileStatsIO.writeJson(path, clazz, stats);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    System.exit(0);
  }

  private static List<DataFileStats> importAnalyzeFiles(final List<String> files,
      final AbstractDataImportTest test) throws InterruptedException {
    logger.info("Exporting data for: " + test.getClass().getName());
    MZmineTestUtil.importFiles(files, 60);

    var stats = MZmineTestUtil.streamDataFiles(files).map(DataFileStats::extract)
        .collect(Collectors.toCollection(ArrayList::new));
    // now import spectra with advanced settings
    MZmineCore.getProjectManager().clearProject();

    // advanced
    var advancedImport = test.createAdvancedImportSettings();
    if (advancedImport != null) {
      MZmineTestUtil.importFiles(files, 60, advancedImport);
      var advancedStats = MZmineTestUtil.streamDataFiles(files).map(DataFileStats::extract)
          .toList();
      stats.addAll(advancedStats);
      MZmineCore.getProjectManager().clearProject();
    }

    return stats;
  }

}
