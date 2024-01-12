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
public class ImzMLImportTest {

  public static final List<String> fileNames = List.of( //
      "rawdatafiles/additional/Example_Continuous.imzML" //
      , "rawdatafiles/additional/Example_Processed.imzML" //
      , "rawdatafiles/additional/image-centroid.imzML" //
  );
  private static final Logger logger = Logger.getLogger(ImzMLImportTest.class.getName());
  private static final Map<String, import_data.DataFileStats> stats = HashMap.newHashMap(
      fileNames.size());

  static {
    stats.put("Example_Continuous.imzML",
        new import_data.DataFileStats("Example_Continuous.imzML", 9, 9, 0, 8399, 0,
            List.of(8399, 8399), List.of(121.85039039868471, 182.31835420101888),
            List.of("CENTROIDED", "CENTROIDED"), List.of(152.9166717529297, 153.0833282470703),
            List.of(1, 2), List.of("[100.08333587646484..799.9166870117188]",
            "[100.08333587646484..799.9166870117188]"), List.of(), List.of(1, 1), List.of("-", "-"),
            List.of(), List.of(), List.of(0.0f, 0.0f), List.of(), List.of(), List.of(), List.of()));
    stats.put("Example_Processed.imzML",
        new import_data.DataFileStats("Example_Processed.imzML", 9, 9, 0, 8399, 0,
            List.of(8399, 8399), List.of(121.85039039868471, 182.31835420101888),
            List.of("CENTROIDED", "CENTROIDED"), List.of(152.9166717529297, 153.0833282470703),
            List.of(1, 2), List.of("[100.08333587646484..799.9166870117188]",
            "[100.08333587646484..799.9166870117188]"), List.of(), List.of(1, 1), List.of("-", "-"),
            List.of(), List.of(), List.of(0.0f, 0.0f), List.of(), List.of(), List.of(), List.of()));
    stats.put("image-centroid.imzML",
        new import_data.DataFileStats("image-centroid.imzML", 19430, 19430, 0, 607, 0,
            List.of(131, 157, 136, 131, 148, 116, 118, 111, 106, 111, 112, 115, 247),
            List.of(15136.91810131073, 15063.774250984192, 12043.561094284058, 11443.903260231018,
                11809.77567577362, 10311.32480430603, 13443.687370300293, 10620.759039878845,
                7142.334212303162, 11323.101006507874, 8217.41294002533, 10518.95936870575,
                17901.835989952087),
            List.of("CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED",
                "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED",
                "CENTROIDED", "CENTROIDED"),
            List.of(311.1688537597656, 311.1687927246094, 311.1688232421875, 311.1687927246094,
                311.16888427734375, 311.1687927246094, 311.1687927246094, 311.168701171875,
                311.1687316894531, 311.168701171875, 311.16888427734375, 311.1689147949219,
                311.1688232421875),
            List.of(1, 2, 11, 26, 51, 151, 201, 401, 601, 801, 1001, 1201, 1501),
            List.of("[200.3735809326172..839.069580078125]",
                "[200.92076110839844..947.716552734375]", "[205.15940856933594..926.9614868164062]",
                "[200.87295532226562..998.8631591796875]",
                "[205.15945434570312..962.2263793945312]",
                "[203.55264282226562..979.5468139648438]",
                "[203.61184692382812..976.7540893554688]", "[202.1475830078125..911.9891967773438]",
                "[200.45022583007812..999.07373046875]", "[200.72457885742188..962.9036254882812]",
                "[200.56150817871094..977.6541748046875]",
                "[205.15943908691406..995.9910278320312]",
                "[200.42050170898438..983.4232788085938]"), List.of(),
            List.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
            List.of("-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-", "-"), List.of(),
            List.of(),
            List.of(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
            List.of(), List.of(), List.of(), List.of()));
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
  @DisplayName("Test data import of imzXML without advanced parameters")
  void dataImportTest() {
    DataImportTestUtils.testDataStatistics(fileNames, stats);
  }

}
