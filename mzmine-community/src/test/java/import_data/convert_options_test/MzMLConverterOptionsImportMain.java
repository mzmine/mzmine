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

package import_data.convert_options_test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import import_data.AbstractDataImportTest;
import import_data.DataFileStats;
import import_data.DataFileStatsIO;
import import_data.DataImportTestUtils;
import import_data.MzMLImportTest;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import testutils.MZmineTestUtil;

/**
 * Test class that checks if all data files converted with different parameters will result in the
 * same data read (precision taken into account). The files are too big and not supplied.
 */
public class MzMLConverterOptionsImportMain {

  private static final Logger logger = Logger.getLogger(
      MzMLConverterOptionsImportMain.class.getName());

  public static List<String> getFileNames() {
    final String[] parameters = """
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

    // test resources folder but not supplied
    String gc = "rawdatafiles/speedtest/gc_orbi_profle/%s/gc_orbi_profile_a.mzML";

    return Arrays.stream(parameters).map(gc::formatted).toList();
  }

  public static void main(String[] args) {
    // keep running and all in memory
    MZmineCore.main(new String[]{"-r", "-m", "all"});

    try {
      dataImportTest();
      advancedDataImportTest();
    } catch (Exception e) {
      System.exit(1);
      logger.log(Level.SEVERE, "Data is different by conversion " + e.getMessage(), e);
    }
    System.exit(0);
  }

  public static void dataImportTest() throws InterruptedException {
    Map<String, DataFileStats> stats = DataFileStatsIO.readJson(MzMLImportTest.class);
    var files = getFileNames();
// import single files
    for (final String file : files) {
      var currentFiles = List.of(file);
      MZmineTestUtil.cleanProject();
      MZmineTestUtil.importFiles(currentFiles, 60);
      DataImportTestUtils.testDataStatistics(currentFiles, stats, false);
    }
  }

  public static void advancedDataImportTest() throws InterruptedException {
    var advanced = AbstractDataImportTest.createAdvancedImportSettings();
    Map<String, DataFileStats> stats = DataFileStatsIO.readJson(MzMLImportTest.class);
    var files = getFileNames();
// import single files
    for (final String file : files) {
      var currentFiles = List.of(file);
      MZmineTestUtil.cleanProject();
      MZmineTestUtil.importFiles(currentFiles, 60, advanced);
      DataImportTestUtils.testDataStatistics(currentFiles, stats, true);

      //
      for (final RawDataFile raw : MZmineCore.getProject().getDataFiles()) {
        String msg = " Error in " + raw.getName();
        for (final Scan scan : raw.getScans()) {
          // advanced sets mass list
          assertNotNull(scan.getMassList(), msg);
          assertEquals(scan.getNumberOfDataPoints(), scan.getMassList().getNumberOfDataPoints(),
              msg);
          if (scan.getNumberOfDataPoints() > 0 && scan.getMSLevel() == 1) {
            // MS1 scans were cropped
            assertTrue(scan.getMzValue(0) >= AbstractDataImportTest.lowestMz,
                scan.getMzValue(0) + " was higher than " + AbstractDataImportTest.lowestMz + msg);
          }
        }
      }
    }
  }

}
