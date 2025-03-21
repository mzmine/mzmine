/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package integrationtest;

import io.github.mzmine.project.ProjectService;
import java.io.File;
import java.net.URL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import testutils.MZmineTestUtil;

@DisabledOnOs(OS.MAC)
public class IntegrationTests {

  /**
   * Tests must be run sequentially and mzmine core must be initialised.
   */
  @BeforeAll
  static void initMzmine() {
    MZmineTestUtil.startMzmineCore();
    ProjectService.getProjectManager().clearProject();
  }

  @AfterEach
  void clearProject() {
    ProjectService.getProjectManager().clearProject();
  }

  @Test
  void testSmallLcMsBatch(@TempDir File tempDir) {
    final URL batchFile = IntegrationTests.class.getClassLoader().getResource(
        "rawdatafiles/integration_tests/workshop_dataset/workshop_dataset_integration_test.mzbatch");
    final URL qc4 = IntegrationTests.class.getClassLoader().getResource(
        "rawdatafiles/integration_tests/workshop_dataset/171103_PMA_TK_QC_04-4to5min.mzML");
    final URL qc5 = IntegrationTests.class.getClassLoader().getResource(
        "rawdatafiles/integration_tests/workshop_dataset/171103_PMA_TK_QC_05-4to5min.mzML");
    final URL expectedResults = IntegrationTests.class.getClassLoader()
        .getResource("rawdatafiles/integration_tests/workshop_dataset/expected_results.csv");
    final URL expectedError = IntegrationTests.class.getClassLoader()
        .getResource("rawdatafiles/integration_tests/workshop_dataset/expected_results_error.csv");

    final URL massbank = IntegrationTests.class.getClassLoader()
        .getResource("spectral_libraries/integration_tests/massbank_nist_for_tests.msp");
    final URL mona = IntegrationTests.class.getClassLoader()
        .getResource("spectral_libraries/integration_tests/MoNA-export-LC-MS-MS_Spectra.json");

    final File results = IntegrationTestUtils.runBatchGetExportedCsv(batchFile, tempDir,
        new URL[]{qc4, qc5}, new URL[]{massbank, mona});

    Assertions.assertTrue(IntegrationTestUtils.getCsvComparisonResults(expectedResults, results,
        new File(batchFile.getFile()).getName()).isEmpty());

    Assertions.assertEquals(IntegrationTestUtils.getCsvComparisonResults(expectedError, results,
        new File(batchFile.getFile()).getName()).size(), 40);
  }

}
