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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
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
//@Disabled
public class ThermoRawImportTest {

  public static final List<String> fileNames = List.of( //
      "rawdatafiles/additional/astral.raw" //
//      , "rawdatafiles/additional/astral.raw" //
  );
  private static final Logger logger = Logger.getLogger(ThermoRawImportTest.class.getName());
  private static final Map<String, import_data.DataFileStats> stats = HashMap.newHashMap(
      fileNames.size());

  static {
    stats.put("astral.raw", new import_data.DataFileStats("astral.raw", 16099, 1116, 14983, 2124, 0,
        List.of(2010, 1917, 119, 102, 134, 333, 139, 155, 70, 69, 15, 68, 96),
        List.of(2.84714129859375E8, 2.702072534404297E8, 238569.5934753418, 242270.6945953369,
            174890.22714233398, 2.07111032328125E8, 336321.2731628418, 173020.20204162598,
            32911.07391357422, 61753.11619567871, 10827.591842651367, 108683.08026123047,
            88869.70237731934),
        List.of("CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED",
            "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED",
            "CENTROIDED"),
        List.of(358.3702392578125, 358.3701171875, 415.25384521484375, 283.1749572753906,
            279.09307861328125, 358.3681640625, 503.3338317871094, 256.16796875, 665.4675903320312,
            704.5429077148438, 104.1065444946289, 500.2733154296875, 395.3354187011719),
        List.of(1, 2, 11, 26, 51, 151, 201, 401, 601, 801, 1001, 1201, 1501),
        List.of("[200.0..2000.0]", "[200.0..2000.0]", "[50.0..1000.0]", "[50.0..1000.0]",
            "[50.0..1000.0]", "[200.0..2000.0]", "[50.0..1000.0]", "[50.0..1000.0]",
            "[50.0..1000.0]", "[50.0..1000.0]", "[50.0..1000.0]", "[50.0..1000.0]",
            "[50.0..1000.0]"),
        List.of(10.024f, 10.024f, 3.016f, 3.016f, 3.016f, 0.704f, 3.008f, 3.016f, 3.016f, 3.016f,
            3.016f, 3.016f, 3.016f), List.of(1, 1, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 2),
        List.of("+", "+", "+", "+", "+", "+", "+", "+", "+", "+", "+", "+", "+"),
        List.of(614.48828, 482.40613, 279.09372, 503.30707, 256.13345, 665.3869, 704.54504,
            1088.5831, 500.27399, 395.32794), List.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        List.of(0.0f, 0.0021996f, 0.014186934f, 0.017096933f, 0.021198934f, 0.035436265f,
            0.042543467f, 0.0805956f, 0.112928666f, 0.15107746f, 0.1795352f, 0.2077028f,
            0.24929826f), List.of(), List.of(), List.of(), List.of(), List.of()));
  }

  /**
   * Init MZmine core in headless mode with the options -r (keep running) and -m (keep in memory)
   */
  @BeforeAll
  public static void init() {
    logger.info("Getting project");
    try {
      MZmineTestUtil.importFiles(fileNames, 60);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }


  @AfterAll
  public static void tearDown() {
    //clean the project after this integration test
    MZmineTestUtil.cleanProject();
  }

  @Test
//    @Order(1)
//  @Disabled
  @DisplayName("Test data import without advanced parameters")
  void dataImportTest() {
    DataImportTestUtils.testDataStatistics(fileNames, stats);
  }

}
