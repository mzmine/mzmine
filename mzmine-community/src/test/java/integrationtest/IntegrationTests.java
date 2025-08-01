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

package integrationtest;

import java.io.File;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import testutils.MZmineTestUtil;

@ExtendWith(MockitoExtension.class)
public class IntegrationTests {

  private static final Logger logger = Logger.getLogger(IntegrationTests.class.getName());

  /**
   * Tests must be run sequentially and mzmine core must be initialised.
   */
  @BeforeAll
  static void initMzmine() {
    MZmineTestUtil.startMzmineCore();
    MZmineTestUtil.clearProjectAndLibraries();
  }

  @AfterEach
  void clearProject() {
    MZmineTestUtil.clearProjectAndLibraries();
  }

  @Test
  void testSmallLcMsBatchRemove(@TempDir File tempDir) {
    testSmallLcMsBatch(tempDir, "workshop_dataset_integration_test.mzbatch");
  }

  @Test
  void testSmallLcMsBatchKeep(@TempDir File tempDir) {
    testSmallLcMsBatch(tempDir, "workshop_dataset_integration_test_keep.mzbatch");
  }

  @Test
  void testSmallLcMsBatchInPlace(@TempDir File tempDir) {
    testSmallLcMsBatch(tempDir, "workshop_dataset_integration_test_process_in_place.mzbatch");
  }


  void testSmallLcMsBatch(File tempDir, String batchFile) {
    final File results = IntegrationTest.builder("rawdatafiles/integration_tests/workshop_dataset",
            batchFile).tempDir(tempDir)
        .rawFiles("171103_PMA_TK_QC_04-4to5min.mzML", "171103_PMA_TK_QC_05-4to5min.mzML")
        .specLibsFullPath("spectral_libraries/integration_tests/massbank_nist_for_tests.msp",
            "spectral_libraries/integration_tests/MoNA-export-LC-MS-MS_Spectra.json").build()
        .runBatchGetCsvFile();

    Assertions.assertTrue(IntegrationTestUtils.getCsvComparisonResults(
            "rawdatafiles/integration_tests/workshop_dataset/expected_results.csv", results, batchFile)
        .isEmpty());

    logger.info("Checking file with 42 known differences. Table below is expected:");
    Assertions.assertEquals(42, IntegrationTestUtils.getCsvComparisonResults(
        "rawdatafiles/integration_tests/workshop_dataset/expected_results_error.csv", results,
        batchFile).size());
  }

  @Test
  void testLcMsFullBatch(@TempDir File tempDir) {
    if (new File("D:\\OneDrive - mzio GmbH").exists()) {
      Assertions.assertEquals(0,
          IntegrationTest.builder("rawdatafiles/integration_tests/workshop_dataset",
              "workshop_dataset_full.mzbatch").tempDir(tempDir).build().runBatchGetCheckResults(
              "rawdatafiles/integration_tests/workshop_dataset/expected_results_full.csv").size());
    }
  }

  @Test
  @DisabledOnOs({OS.LINUX, OS.MAC})
    // windows paths don't work on linux/mac
  void testProjectLoadLcms(@TempDir File tempDir) {
    final String expectedResultsFromProcessing = "rawdatafiles/integration_tests/workshop_dataset/expected_results.csv";
    final String expectedResultsFromProjectLoad = "rawdatafiles/integration_tests/workshop_dataset/expected_results_project.csv";

    final File csvExportFile = IntegrationTestUtils.loadProjectExportFeatureList(tempDir,
        "rawdatafiles/integration_tests/workshop_dataset/project.mzmine");

    // there should be the warning that the number of row types is not equal and 9 columns are missing
    Assertions.assertEquals(2,
        IntegrationTestUtils.getCsvComparisonResults(expectedResultsFromProcessing, csvExportFile,
            "project_load_lcms").size());
    // saving and loading the project should be identical
    Assertions.assertEquals(0,
        IntegrationTestUtils.getCsvComparisonResults(expectedResultsFromProjectLoad, csvExportFile,
            "project_load_lcms").size());
  }

  @Test
  @DisabledOnOs({OS.LINUX, OS.MAC})
  void testDiTimsMs(@TempDir File tempDir) {
    Assertions.assertEquals(0,
        IntegrationTest.builder("rawdatafiles/integration_tests/timstof_ditimsms_pasef",
                "di_tims_ms.mzbatch") //
            .rawFilesFullPath("rawdatafiles/additional/lc-tims-ms-pasef-a.d").tempDir(tempDir)
            .build()//
            .runBatchGetCheckResults(
                "rawdatafiles/integration_tests/timstof_ditimsms_pasef/expected_results.csv")
            .size());
  }

  @Test
  void testGcTofMs(@TempDir File tempDir) {
    Assertions.assertEquals(0,
        IntegrationTest.builder("rawdatafiles/integration_tests/gc_tof_ms", "gc_tof.mzbatch")
            .rawFiles("019_KR8_20220715.mzML")
            .specLibsFullPath("spectral_libraries/integration_tests/GC_HRMS_Archeology.json")
            .tempDir(tempDir).build() //
            .runBatchGetCheckResults(
                "rawdatafiles/integration_tests/gc_tof_ms/expected_results.csv").size());
  }

  @Test
  void testMseMs(@TempDir File tempDir) {
    Assertions.assertEquals(0,
        IntegrationTest.builder("rawdatafiles/integration_tests/mse", "mse_batch.mzbatch")
            .tempDir(tempDir).rawFiles("mse_20180205_0125.mzML")
            .specLibsFullPath("spectral_libraries/integration_tests/massbank_eu_nist_for_mse.msp")
            .build()
            .runBatchGetCheckResults("rawdatafiles/integration_tests/mse/expected_results.csv")
            .size());
  }

  @Test
  @DisabledOnOs({OS.LINUX, OS.MAC})
  void testMseMsProject(@TempDir File tempDir) {
    final File exportedFlist = IntegrationTestUtils.loadProjectExportFeatureList(tempDir,
        "rawdatafiles/integration_tests/mse/mse_project.mzmine");

    Assertions.assertEquals(0, IntegrationTestUtils.getCsvComparisonResults(
        "rawdatafiles/integration_tests/mse/expected_results_project.csv", exportedFlist,
        "mse_project.mzmine").size());

    // expected_results_project_direct_batch.csv is the results of the project after batch processing
    // expected_results.csv changed a bit because the MSe processing changed from 10% to 1% intensity factor
    // just using the old project still with the old results
    Assertions.assertEquals(2, IntegrationTestUtils.getCsvComparisonResults(
        "rawdatafiles/integration_tests/mse/expected_results_project_direct_batch.csv",
        exportedFlist, "mse_project.mzmine").size());
  }

  @Test
  void testLibToFlist(@TempDir File tempDir) {
    Assertions.assertEquals(0,
        IntegrationTest.builder("rawdatafiles/integration_tests/library_to_flist",
                "lib_batch.mzbatch").tempDir(tempDir)
            .specLibsFullPath("spectral_libraries/integration_tests/lib_to_flist.json").build()
            .runBatchGetCheckResults(
                "rawdatafiles/integration_tests/library_to_flist/expected_results.csv").size());
  }

  @Test
  @DisabledOnOs({OS.LINUX, OS.MAC})
  void timsFullBatch(@TempDir File tempDir) {
    // only run the test on local machines
    if (!new File("D:\\OneDrive - mzio GmbH").exists()) {
      logger.info("Skipping tims full batch integration test.");
      return;
    }
    Assertions.assertEquals(0,
        IntegrationTest.builder("rawdatafiles/integration_tests/lc_tims", "lc_tims_local.mzbatch")
            .tempDir(tempDir).build()
            .runBatchGetCheckResults("rawdatafiles/integration_tests/lc_tims/expected_results.csv")
            .size());

//    ConfigService.getPreferences().setParameter(MZminePreferences.numOfThreads, 4);
//    final File first = IntegrationTest.builder("rawdatafiles/integration_tests/lc_tims",
//        "lc_tims_local_ims.mzbatch").tempDir(tempDir).build().runBatchGetCsvFile();
//    ProjectService.getProject().removeFeatureLists(ProjectService.getProject()
//        .getCurrentFeatureLists());
//    ConfigService.getPreferences().setParameter(MZminePreferences.numOfThreads, 2);
//    final File second = IntegrationTest.builder("rawdatafiles/integration_tests/lc_tims",
//        "lc_tims_local_ims.mzbatch").tempDir(tempDir).build().runBatchGetCsvFile();
//    final List<@NotNull CheckResult> results = IntegrationTestUtils.getCsvComparisonResults(first,
//        second, "ims");
//    Assertions.assertEquals(0, results.size());
  }

  @Test
  void testDiaPasef(@TempDir File tempDir) {
    // only run the test on local machines
    if (!new File("D:\\OneDrive - mzio GmbH").exists()) {
      logger.info("Skipping tims full batch integration test.");
      return;
    }
    Assertions.assertEquals(0, IntegrationTest.builder("rawdatafiles/integration_tests/diaPASEF",
            "dia_pasef_local.mzbatch").tempDir(tempDir).build()
        .runBatchGetCheckResults("rawdatafiles/integration_tests/diaPASEF/expected_results.csv")
        .size());
  }
}
