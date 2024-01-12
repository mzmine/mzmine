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

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import testutils.MZmineTestUtil;

/**
 * {@link Lifecycle#PER_CLASS} creates only one test instance of this class and executes everything
 * in sequence. As we are using data import, chromatogram building, ... Only with this option the
 * init (@BeforeAll) and tearDown method are not static.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
@TestInstance(Lifecycle.PER_CLASS)
//@TestMethodOrder(OrderAnnotation.class)
//@Disabled("Only used manually to generate list of files")
public class DataImportAnalysisTest {

  private static final Logger logger = Logger.getLogger(DataImportAnalysisTest.class.getName());

  /**
   * Init MZmine core in headless mode with the options -r (keep running) and -m (keep in memory)
   */
  @BeforeAll
  public static void init() {
    logger.info("Getting project");
  }


  @AfterAll
  public static void tearDown() {
    //clean the project after this integration test
    MZmineTestUtil.cleanProject();
  }

  public static void printDatasetStats(final List<String> fileNames) {
    String data = MZmineTestUtil.streamDataFiles(fileNames).map(raw -> {
      var stats = DataFileStats.extract(raw);
      var instance = stats.printInstance();
      return STR."stats.put(\"\{raw.getName()}\", \{instance});";
    }).collect(Collectors.joining("\n"));

    System.out.println(STR."""
        #######################################################

        private static final Map<String, import_data.DataFileStats> stats = HashMap.newHashMap(fileNames.size());
        static {
          \{data}
        }

        #######################################################
        """);
  }

  @Test
//  @Disabled("Expected to be disabled and only used when raw data changes")
  @DisplayName("Analyze and extract test data")
  void analyzeDataFiles() {
    // analyze data files and extract test data
    var tests = Map.of("mzml", MzMLImportTest.fileNames, //
        "imzml", ImzMLImportTest.fileNames, //
        "thermo", ThermoRawImportTest.fileNames //
    );

    for (var entry : tests.entrySet()) {
      try {
        var format = entry.getKey();
        var files = entry.getValue();
        MZmineTestUtil.importFiles(files, 60);

        logger.info("Exporting data for: " + format);
        DataImportAnalysisTest.printDatasetStats(files);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
