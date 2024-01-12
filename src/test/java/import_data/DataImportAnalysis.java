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
import testutils.MZmineTestUtil;

public class DataImportAnalysis {

  private static final Logger logger = Logger.getLogger(DataImportAnalysis.class.getName());

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

  public static void main(String[] args) {

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
        DataImportAnalysis.printDatasetStats(files);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
