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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularTask;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.modules.io.projectload.ProjectLoaderParameters;
import io.github.mzmine.modules.io.projectload.ProjectOpeningTask;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import java.io.File;
import java.net.URL;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import testutils.MZmineTestUtil;

@ExtendWith(MockitoExtension.class)
public class IntegrationTests {

  /**
   * Tests must be run sequentially and mzmine core must be initialised.
   */
  @BeforeAll
  static void initMzmine() {
    MZmineTestUtil.startMzmineCore();
    ProjectService.getProjectManager().clearProject();
    ProjectService.getProject().clearSpectralLibrary();
  }

  @AfterEach
  void clearProject() {
    ProjectService.getProjectManager().clearProject();
    ProjectService.getProject().clearSpectralLibrary();
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

  @Test
  void testProjectLoad(@TempDir File tempDir) {
    final String resourcePath = "rawdatafiles/integration_tests/workshop_dataset/project.mzmine";
    final File resourceFile = IntegrationTestUtils.urlToFile(
        getClass().getClassLoader().getResource(resourcePath));
    final URL expectedResultsFromProcessing = IntegrationTests.class.getClassLoader()
        .getResource("rawdatafiles/integration_tests/workshop_dataset/expected_results.csv");
    final URL expectedResultsFromProjectLoad = IntegrationTests.class.getClassLoader().getResource(
        "rawdatafiles/integration_tests/workshop_dataset/expected_results_project.csv");

    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());

    var parameters = (ProjectLoaderParameters) new ProjectLoaderParameters().cloneParameterSet();
    parameters.setParameter(ProjectLoaderParameters.projectFile, resourceFile);
    ProjectOpeningTask task = new ProjectOpeningTask(parameters, Instant.now());
    task.run();

    final MZmineProject loadedProject = ProjectService.getProject();

    final FeatureList finalFlist = loadedProject.getCurrentFeatureLists().stream()
        .max(Comparator.comparingInt(fl -> fl.getName().length())).get();

    final File csvExportFile = new File(tempDir,
        "modular_export_%s_%s.csv".formatted(resourceFile.getName(), UUID.randomUUID().toString()));

    final CSVExportModularTask exportTask = new CSVExportModularTask(
        new ModularFeatureList[]{(ModularFeatureList) finalFlist}, csvExportFile, ",", ";",
        FeatureListRowsFilter.ALL, true, Instant.now());
    exportTask.run();

    // there should be the warning that the number of row types is not equal and 9 columns are missing
    Assertions.assertEquals(2,
        IntegrationTestUtils.getCsvComparisonResults(expectedResultsFromProcessing, csvExportFile,
            resourceFile.getName()).size());
    // saving and loading the project should be identical
    Assertions.assertEquals(0,
        IntegrationTestUtils.getCsvComparisonResults(expectedResultsFromProjectLoad, csvExportFile,
            resourceFile.getName()).size());
  }
}
