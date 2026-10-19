/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.tools_autoparam.runphases;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.SimpleRange.SimpleFloatRange;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.RawDataPreparation;
import io.github.mzmine.project.ProjectService;
import java.io.File;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import testutils.MZmineTestUtil;

/**
 * Regression test of {@link RunPhaseDetection} on real data. The expected ranges were checked
 * manually on the TIC, base peak, background ion and pump traces. Only runs where the example data
 * folder is available.
 */
@TestInstance(Lifecycle.PER_CLASS)
class RunPhaseRegressionTest {

  private static final File EXAMPLE_DATA = new File(
      "D:/OneDrive - mzio GmbH/Example data - Documents");
  // minutes
  private static final double TOLERANCE = 0.02;

  /**
   * file relative to the example data folder, expected start and end in minutes
   */
  static @NotNull Stream<Arguments> files() {
    return Stream.of(
        // decision: small files only, the timsTOF calibrant plug files run out of the default test
        // heap. Pressure trace as lower bound, re-equilibration confirmed by G
        Arguments.of("Agilent/Agilent 6546_Zamboni/mzML/BEH30mm_5min_LipidMix_1.mzML", 0.00, 3.75),
        // void and a salt peak at the end of the HILIC wash, no pump traces
        Arguments.of("other_tools/masscube/Experimental_data_benchmarking/8_experimental_data/"
            + "raw_data/NIST_Feces_Orbitrap_HILIC_negative.mzML", 0.19, 8.22));
  }

  @BeforeAll
  void initialize() {
    Assumptions.assumeTrue(EXAMPLE_DATA.isDirectory(),
        "example data not available: " + EXAMPLE_DATA);
    MZmineTestUtil.startMzmineCore();
  }

  @ParameterizedTest
  @MethodSource("files")
  void effectiveRtRange(@NotNull String path, double start, double end) {
    final File file = new File(EXAMPLE_DATA, path);
    Assumptions.assumeTrue(file.exists(), "file not available: " + file);
    try {
      final List<RawDataFile> raws = RawDataPreparation.importFilesBlocking(new File[]{file}, null);
      Assertions.assertEquals(1, raws.size(), "import failed: " + file);
      final RawDataFile raw = raws.getFirst();
      final SimpleFloatRange range = RunPhaseDetection.detect(raw, raw.getScanNumbers(1));
      Assertions.assertNotNull(range);
      Assertions.assertEquals(start, range.lower(), TOLERANCE, "start of " + file.getName());
      Assertions.assertEquals(end, range.upper(), TOLERANCE, "end of " + file.getName());
    } finally {
      ProjectService.getProjectManager().clearProject();
    }
  }
}
