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
public class MzMLImportTest {

  private static final Logger logger = Logger.getLogger(MzMLImportTest.class.getName());
  public static final List<String> fileNames = List.of( //
      "rawdatafiles/DOM_a.mzML", //
//      "rawdatafiles/DOM_a_invalid_header.mzML", //
//      "rawdatafiles/DOM_a_invalid_chars.mzML", //
      "rawdatafiles/DOM_b.mzXML" //
//      "rawdatafiles/DOM_b_invalid_header.mzXML" //
      , "rawdatafiles/additional/orbi_idx_msn.mzML" //
      , "rawdatafiles/additional/gc_orbi.mzML" //
      , "rawdatafiles/additional/gc_orbi_profil.mzML" //
  );

  private static final Map<String, DataFileStats> stats = HashMap.newHashMap(fileNames.size());

  static {
    var domA = new DataFileStats("DOM_a.mzML", 521, 87, 434, 2400, 0,
        List.of(2125, 230, 253, 271, 234, 1889, 202, 50),
        List.of(5.333711600859375E8, 2055437.8270263672, 1550998.44140625, 2049773.3881835938,
            1743926.3271484375, 4.085587983691406E8, 894691.6715087891, 2306341.512084961),
        List.of("CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED",
            "CENTROIDED", "CENTROIDED"),
        List.of(368.20648193359375, 333.1700439453125, 391.1732482910156, 361.16455078125,
            349.16412353515625, 395.3631896972656, 405.1899719238281, 85.02902221679688),
        List.of(2100, 2101, 2110, 2125, 2150, 2250, 2300, 2500),
        List.of("[152.070556640625..1481.020141601563]", "[79.054946899414..368.257934570313]",
            "[78.623321533203..408.309844970703]", "[79.054977416992..397.187133789063]",
            "[79.054985046387..349.202362060547]", "[151.075302124023..1404.502807617188]",
            "[79.054962158203..405.230224609375]", "[79.05485534668..207.156967163086]"), List.of(),
        List.of(1, 2, 2, 2, 2, 1, 2, 2), List.of("+", "+", "+", "+", "+", "+", "+", "+"),
        List.of(368.206455623197, 408.201080322266, 396.20120413401, 349.164581298828,
            405.190887451172, 207.137962510578), List.of(1, 1, 1, 1),
        List.of(7.2438664f, 7.248367f, 7.2802997f, 7.3303504f, 7.4139667f, 7.7484336f, 7.9202833f,
            8.5974f), List.of(), List.of(), List.of(), List.of(), List.of());
    var domB = new DataFileStats("DOM_b.mzXML", 521, 87, 434, 2410, 0,
        List.of(269, 2039, 249, 2226, 229, 218, 184, 63),
        List.of(1741484.3294677734, 6.165148037929688E8, 1689558.2706298828, 5.814518785332031E8,
            2114246.5798339844, 1041696.2025146484, 1275887.1608886719, 4367971.130493164),
        List.of("CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED",
            "CENTROIDED", "CENTROIDED"),
        List.of(391.1732177734375, 246.24269104003906, 373.164306640625, 305.17462158203125,
            349.16473388671875, 415.221435546875, 333.1698303222656, 219.1031036376953),
        List.of(2100, 2101, 2110, 2125, 2150, 2250, 2300, 2500),
        List.of("[79.0549087524414..447.3476257324219]", "[151.075439453125..1433.0494384765625]",
            "[79.05493927001953..408.3114318847656]", "[151.11146545410156..1409.50732421875]",
            "[79.05498504638672..367.1740417480469]", "[77.9360122680664..450.2566223144531]",
            "[75.44328308105469..368.29925537109375]", "[78.02328491210938..219.1871795654297]"),
        List.of(), List.of(2, 1, 2, 1, 2, 2, 2, 2), List.of("+", "+", "+", "+", "+", "+", "+", "+"),
        List.of(426.212162243691, 408.201249101897, 366.190806726003, 450.248413085938,
            368.206530266888, 219.187151122833), List.of(1, 1, 1, 0, 1, 1),
        List.of(7.2345333f, 7.236483f, 7.267833f, 7.3167667f, 7.4032335f, 7.739217f, 7.907817f,
            8.581467f), List.of(), List.of(), List.of(), List.of(), List.of());
    stats.put("DOM_a.mzML", domA);
    stats.put("DOM_a_invalid_header.mzML", domA);
    stats.put("DOM_a_invalid_chars.mzML", domA);
    stats.put("DOM_b.mzXML", domB);
    stats.put("DOM_b_invalid_header.mzXML", domB);
    stats.put("orbi_idx_msn.mzML",
        new import_data.DataFileStats("orbi_idx_msn.mzML", 887, 427, 45, 1098, 0,
            List.of(550, 562, 548, 552, 564, 743, 31, 55, 26, 529),
            List.of(3.3155126E7, 3.431942E7, 3.4495409E7, 3.6225665E7, 3.2717563E7, 3.6315817E7,
                122989.0, 144137.0, 1048756.0, 3.9967299E7),
            List.of("CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED",
                "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED"),
            List.of(157.03514099121094, 157.03512573242188, 157.03514099121094, 157.03506469726562,
                157.03501892089844, 157.03509521484375, 77.03856658935547, 226.0628204345703,
                299.0557861328125, 157.03509521484375),
            List.of(1, 2, 11, 26, 51, 151, 201, 401, 601, 801),
            List.of("[115.0..2000.0]", "[115.0..2000.0]", "[115.0..2000.0]", "[115.0..2000.0]",
                "[115.0..2000.0]", "[115.0..2000.0]", "[40.0..114.0]", "[40.0..265.0]",
                "[40.0..460.0]", "[115.0..2000.0]"),
            List.of(50.0f, 50.0f, 50.0f, 50.0f, 50.0f, 50.0f, 200.0f, 200.0f, 50.0f, 50.0f),
            List.of(1, 1, 1, 1, 1, 1, 4, 4, 2, 1),
            List.of("+", "+", "+", "+", "+", "+", "+", "+", "+", "+"),
            List.of(103.054351806641, 254.0576171875, 449.108489990234), List.of(1),
            List.of(0.0010631501f, 0.0044698855f, 0.035379827f, 0.0870163f, 0.17286651f,
                0.51696837f, 0.7151894f, 1.3292525f, 2.0071716f, 2.7059631f), List.of(), List.of(),
            List.of(), List.of(), List.of()));
    stats.put("gc_orbi.mzML", new import_data.DataFileStats("gc_orbi.mzML", 13066, 13066, 0, 837, 0,
        List.of(35, 54, 479, 507, 415, 254, 244, 230, 212, 230, 248, 236, 235),
        List.of(23257.0, 34254.0, 1556096.0, 1702222.0, 1117247.0, 365574.0, 363432.0, 336990.0,
            325145.0, 339793.0, 348200.0, 336657.0, 336062.0),
        List.of("CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED",
            "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED",
            "CENTROIDED"),
        List.of(245.4212646320936, 245.42103578610931, 91.05431368094558, 91.05429075907789,
            91.0542984308835, 91.05433659436612, 91.05427548400505, 91.05430604041939,
            91.0543288919483, 91.05429841282174, 91.05427551715736, 91.05428317478724,
            91.05436705293228),
        List.of(1, 2, 11, 26, 51, 151, 201, 401, 601, 801, 1001, 1201, 1501),
        List.of("[60.0..900.0]", "[60.0..900.0]", "[60.0..900.0]", "[60.0..900.0]", "[60.0..900.0]",
            "[60.0..900.0]", "[60.0..900.0]", "[60.0..900.0]", "[60.0..900.0]", "[60.0..900.0]",
            "[60.0..900.0]", "[60.0..900.0]", "[60.0..900.0]"),
        List.of(200.0f, 200.0f, 200.0f, 200.0f, 200.0f, 200.0f, 200.0f, 200.0f, 200.0f, 200.0f,
            200.0f, 200.0f, 200.0f), List.of(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
        List.of("+", "+", "+", "+", "+", "+", "+", "+", "+", "+", "+", "+", "+"), List.of(),
        List.of(),
        List.of(8.006503f, 8.0104885f, 8.046366f, 8.106157f, 8.205812f, 8.604424f, 8.803738f,
            9.60093f, 10.398119f, 11.195321f, 11.992539f, 12.789728f, 13.985539f), List.of(),
        List.of(), List.of(), List.of(), List.of()));
    stats.put("gc_orbi_profil.mzML",
        new import_data.DataFileStats("gc_orbi_profil.mzML", 21, 21, 0, 5412, 0,
            List.of(3888, 3706, 5213),
            List.of(1673612.983516693, 1787071.453713894, 8877841.729095459),
            List.of("PROFILE", "PROFILE", "PROFILE"),
            List.of(81.0699404490411, 69.06986635151571, 213.18512844783922),
            List.of(5920, 5921, 5930), List.of("[60.0..900.0]", "[60.0..900.0]", "[60.0..900.0]"),
            List.of(200.0f, 200.0f, 200.0f), List.of(1, 1, 1), List.of("+", "+", "+"), List.of(),
            List.of(), List.of(31.5997f, 31.603699f, 31.63958f), List.of(), List.of(), List.of(),
            List.of(), List.of()));
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
  @DisplayName("Test data import of mzML and mzXML without advanced parameters")
  void dataImportTest() {
    DataImportTestUtils.testDataStatistics(fileNames, stats);
  }

}
