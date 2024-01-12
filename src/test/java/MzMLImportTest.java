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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

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
  private static final List<String> fileNames = List.of( //
      "rawdatafiles/DOM_a.mzML", //
//      "rawdatafiles/DOM_a_invalid_header.mzML", //
//      "rawdatafiles/DOM_a_invalid_chars.mzML", //
      "rawdatafiles/DOM_b.mzXML" //
//      "rawdatafiles/DOM_b_invalid_header.mzXML" //
  );
  private static MZmineProject project;

  private static final Map<String, DataFileStats> stats = HashMap.newHashMap(fileNames.size());

  static {
    stats.put("DOM_a.mzML", new DataFileStats("DOM_a.mzML", 521, 87, 434, 2400, 0,
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
            8.5974f), List.of(), List.of(), List.of(), List.of()));
    stats.put("DOM_a_invalid_header.mzML",
        new DataFileStats("DOM_a_invalid_header.mzML", 521, 87, 434, 2400, 0,
            List.of(2125, 230, 253, 271, 234, 1889, 202, 50),
            List.of(5.333711600859375E8, 2055437.8270263672, 1550998.44140625, 2049773.3881835938,
                1743926.3271484375, 4.085587983691406E8, 894691.6715087891, 2306341.512084961),
            List.of("CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED",
                "CENTROIDED", "CENTROIDED", "CENTROIDED"),
            List.of(368.20648193359375, 333.1700439453125, 391.1732482910156, 361.16455078125,
                349.16412353515625, 395.3631896972656, 405.1899719238281, 85.02902221679688),
            List.of(2100, 2101, 2110, 2125, 2150, 2250, 2300, 2500),
            List.of("[152.070556640625..1481.020141601563]", "[79.054946899414..368.257934570313]",
                "[78.623321533203..408.309844970703]", "[79.054977416992..397.187133789063]",
                "[79.054985046387..349.202362060547]", "[151.075302124023..1404.502807617188]",
                "[79.054962158203..405.230224609375]", "[79.05485534668..207.156967163086]"),
            List.of(), List.of(1, 2, 2, 2, 2, 1, 2, 2),
            List.of("+", "+", "+", "+", "+", "+", "+", "+"),
            List.of(368.206455623197, 408.201080322266, 396.20120413401, 349.164581298828,
                405.190887451172, 207.137962510578), List.of(1, 1, 1, 1),
            List.of(7.2438664f, 7.248367f, 7.2802997f, 7.3303504f, 7.4139667f, 7.7484336f,
                7.9202833f, 8.5974f), List.of(), List.of(), List.of(), List.of()));
    stats.put("DOM_a_invalid_chars.mzML",
        new DataFileStats("DOM_a_invalid_chars.mzML", 521, 87, 434, 2400, 0,
            List.of(2125, 230, 253, 271, 234, 1889, 202, 50),
            List.of(5.333711600859375E8, 2055437.8270263672, 1550998.44140625, 2049773.3881835938,
                1743926.3271484375, 4.085587983691406E8, 894691.6715087891, 2306341.512084961),
            List.of("CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED",
                "CENTROIDED", "CENTROIDED", "CENTROIDED"),
            List.of(368.20648193359375, 333.1700439453125, 391.1732482910156, 361.16455078125,
                349.16412353515625, 395.3631896972656, 405.1899719238281, 85.02902221679688),
            List.of(2100, 2101, 2110, 2125, 2150, 2250, 2300, 2500),
            List.of("[152.070556640625..1481.020141601563]", "[79.054946899414..368.257934570313]",
                "[78.623321533203..408.309844970703]", "[79.054977416992..397.187133789063]",
                "[79.054985046387..349.202362060547]", "[151.075302124023..1404.502807617188]",
                "[79.054962158203..405.230224609375]", "[79.05485534668..207.156967163086]"),
            List.of(), List.of(1, 2, 2, 2, 2, 1, 2, 2),
            List.of("+", "+", "+", "+", "+", "+", "+", "+"),
            List.of(368.206455623197, 408.201080322266, 396.20120413401, 349.164581298828,
                405.190887451172, 207.137962510578), List.of(1, 1, 1, 1),
            List.of(7.2438664f, 7.248367f, 7.2802997f, 7.3303504f, 7.4139667f, 7.7484336f,
                7.9202833f, 8.5974f), List.of(), List.of(), List.of(), List.of()));
    stats.put("DOM_b.mzXML", new DataFileStats("DOM_b.mzXML", 521, 87, 434, 2410, 0,
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
            8.581467f), List.of(), List.of(), List.of(), List.of()));
    stats.put("DOM_b_invalid_header.mzXML",
        new DataFileStats("DOM_b_invalid_header.mzXML", 521, 87, 434, 2410, 0,
            List.of(269, 2039, 249, 2226, 229, 218, 184, 63),
            List.of(1741484.3294677734, 6.165148037929688E8, 1689558.2706298828,
                5.814518785332031E8, 2114246.5798339844, 1041696.2025146484, 1275887.1608886719,
                4367971.130493164),
            List.of("CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED", "CENTROIDED",
                "CENTROIDED", "CENTROIDED", "CENTROIDED"),
            List.of(391.1732177734375, 246.24269104003906, 373.164306640625, 305.17462158203125,
                349.16473388671875, 415.221435546875, 333.1698303222656, 219.1031036376953),
            List.of(2100, 2101, 2110, 2125, 2150, 2250, 2300, 2500),
            List.of("[79.0549087524414..447.3476257324219]",
                "[151.075439453125..1433.0494384765625]", "[79.05493927001953..408.3114318847656]",
                "[151.11146545410156..1409.50732421875]", "[79.05498504638672..367.1740417480469]",
                "[77.9360122680664..450.2566223144531]", "[75.44328308105469..368.29925537109375]",
                "[78.02328491210938..219.1871795654297]"), List.of(),
            List.of(2, 1, 2, 1, 2, 2, 2, 2), List.of("+", "+", "+", "+", "+", "+", "+", "+"),
            List.of(426.212162243691, 408.201249101897, 366.190806726003, 450.248413085938,
                368.206530266888, 219.187151122833), List.of(1, 1, 1, 0, 1, 1),
            List.of(7.2345333f, 7.236483f, 7.267833f, 7.3167667f, 7.4032335f, 7.739217f, 7.907817f,
                8.581467f), List.of(), List.of(), List.of(), List.of()));
  }

  /**
   * Init MZmine core in headless mode with the options -r (keep running) and -m (keep in memory)
   */
  @BeforeAll
  public static void init() {
    logger.info("Getting project");
    project = MZmineCore.getProjectManager().getCurrentProject();
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

  @Nullable
  private static RawDataFile getRawFromProject(final String name) {
    return project.getCurrentRawDataFiles().stream().filter(r -> r.getName().equals(name))
        .findFirst().orElse(null);
  }

  @NotNull
  private static Stream<RawDataFile> streamDataFiles(List<String> fileNames) {
    return fileNames.stream().map(n -> new File(n).getName())
        .map(MzMLImportTest::getRawFromProject);
  }

  @Test
//  @Disabled("Expected to be disabled and only used when raw data changes")
  @DisplayName("Analyze and extract test data")
  void analyzeDataFiles() {
    // analyze data files and extract test data

    String data = streamDataFiles(fileNames).map(name -> {
      var stats = DataFileStats.extract(name);
      var instance = stats.printInstance();
      return STR."stats.put(\"\{name}\", \{instance});";
    }).collect(Collectors.joining("\n"));

    System.out.println(STR."""
        #######################################################

        private static final Map<String, DataFileStats> stats = HashMap.newHashMap(fileNames.size());
        static {
          \{data}
        }

        #######################################################
        """);
  }

  @Test
//    @Order(1)
//  @Disabled
  @DisplayName("Test data import of mzML and mzXML without advanced parameters")
  void dataImportTest() {
    for (final String file : fileNames) {
      String name = new File(file).getName();
      var expected = stats.get(name);
      var actual = getRawFromProject(name);
      Assertions.assertNotNull(expected);
      Assertions.assertNotNull(actual);

      expected.test(actual);
    }
  }
}
