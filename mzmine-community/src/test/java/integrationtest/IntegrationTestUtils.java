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
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularModule;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularParameters;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.modules.tools.output_compare_csv.CheckResult;
import io.github.mzmine.modules.tools.output_compare_csv.CheckResult.Severity;
import io.github.mzmine.modules.tools.output_compare_csv.CompareModularCsvParameters;
import io.github.mzmine.modules.tools.output_compare_csv.CompareModularCsvTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.XMLUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.xml.parsers.ParserConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

public class IntegrationTestUtils {

  /**
   * Executes a batch comparison process using the provided batch file.
   *
   * @param batchFile                 the URL of the batch file to be executed
   * @param baseCsvFile               the URL of the base CSV file for comparison
   * @param tempDir                   the temporary directory for intermediate files during
   *                                  execution
   * @param overrideDataFiles         optional array of URLs for data files that override batch
   *                                  defaults (null or empty means no overrides)
   * @param overrideSpectralLibraries optional array of URLs for spectral library files that
   *                                  override batch defaults (null or empty means no overrides)
   * @return a list of CheckResult objects containing the comparison results; the list is empty if
   * all comparisons are successful
   */
  public static List<CheckResult> runBatchCompareToCsv(URL batchFile, URL baseCsvFile, File tempDir,
      @Nullable URL[] overrideDataFiles, @Nullable URL[] overrideSpectralLibraries) {

    return runBatchCompareToCsv(urlToFile(batchFile), urlToFile(baseCsvFile), tempDir,
        overrideDataFiles != null ? Arrays.stream(overrideDataFiles).filter(Objects::nonNull)
            .map(IntegrationTestUtils::urlToFile).toArray(File[]::new) : null,
        overrideSpectralLibraries != null ? Arrays.stream(overrideSpectralLibraries)
            .filter(Objects::nonNull).map(IntegrationTestUtils::urlToFile).toArray(File[]::new)
            : null);
  }

  @NotNull
  private static File urlToFile(@Nullable final URL url) {
    if (url == null) {
      throw new RuntimeException("URL is null");
    }
    return new File(url.getFile());
  }

  public static List<CheckResult> runBatchCompareToCsv(File batchFile, File baseCsvFile,
      File tempDir, @Nullable File @Nullable [] overrideDataFiles,
      File[] overrideSpectralLibraries) {

    final String batchFileName = batchFile.getName();
    final File csvExportFile = runBatchGetExportedCsv(batchFile, tempDir, overrideDataFiles,
        overrideSpectralLibraries);

    return getCsvComparisonResults(baseCsvFile, csvExportFile, batchFileName);
  }

  /**
   * Runs a batch and returns the file of the exported modular csv file. This method can be used if
   * multiple comparisons shall be done, e.g., a purposefully failing test.
   */
  public static @NotNull File runBatchGetExportedCsv(URL batchFile, File tempDir,
      @Nullable URL @Nullable [] overrideDataFiles, URL[] overrideSpectralLibraries) {
    return runBatchGetExportedCsv(urlToFile(batchFile), tempDir,
        overrideDataFiles != null ? Arrays.stream(overrideDataFiles).filter(Objects::nonNull)
            .map(IntegrationTestUtils::urlToFile).toArray(File[]::new) : null,
        overrideSpectralLibraries != null ? Arrays.stream(overrideSpectralLibraries)
            .filter(Objects::nonNull).map(IntegrationTestUtils::urlToFile).toArray(File[]::new)
            : null);
  }

  /**
   * Runs a batch and returns the file of the exported modular csv file. This method can be used if
   * multiple comparisons shall be done, e.g., a purposefully failing test.
   */
  public static @NotNull File runBatchGetExportedCsv(File batchFile, File tempDir,
      @Nullable File @Nullable [] overrideDataFiles, File[] overrideSpectralLibraries) {

    final MZmineProject project = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(project);
    final String batchFileName = batchFile.getName();

    final BatchQueue queue = loadBatchFromFile(batchFile);
    final File csvExportFile = addOrModifyModularCsvExportStep(batchFileName, tempDir, queue);

    final BatchTask batchTask = BatchModeModule.runBatch(queue, project, overrideDataFiles, null,
        overrideSpectralLibraries, null, Instant.now());

    if (batchTask == null || batchTask.getStatus() != TaskStatus.FINISHED) {
      throw new RuntimeException(
          "Batch task for batch file %s did not finish".formatted(batchFileName));
    }
    return csvExportFile;
  }

  /**
   * Loads a batch mode from a batch file or throws an exception.
   */
  public static @NotNull BatchQueue loadBatchFromFile(File batchFile) {
    final List<String> batchLoadErrors = new ArrayList<>();
    final BatchQueue queue;
    try {
      queue = BatchQueue.loadFromXml(XMLUtils.load(batchFile).getDocumentElement(), batchLoadErrors,
          true);
    } catch (ParserConfigurationException | IOException | SAXException e) {
      throw new RuntimeException("Error while loading batch file" + e);
    }
    return queue;
  }

  public static List<@NotNull CheckResult> getCsvComparisonResults(URL expectedResultsFile,
      File batchExportedFile, String batchFileName) {
    return getCsvComparisonResults(urlToFile(expectedResultsFile), batchExportedFile,
        batchFileName);
  }

  /**
   * Compares two CSV files and returns a list of checks with the comparison results.
   *
   * @param expectedResultsFile the expected results CSV file to be used as the base for comparison
   * @param batchExportedFile   the exported CSV file to compare against the expected results
   * @param batchFileName       the name of the batch file associated with this comparison, used for
   *                            error reporting
   * @return a list of CheckResult with the level warning or error. All checks were successful if
   * the list is empty.
   */
  @NotNull
  public static List<@NotNull CheckResult> getCsvComparisonResults(File expectedResultsFile,
      File batchExportedFile, String batchFileName) {
    CompareModularCsvParameters param = (CompareModularCsvParameters) new CompareModularCsvParameters().cloneParameterSet();
    param.setParameter(CompareModularCsvParameters.baseFile, expectedResultsFile);
    param.setParameter(CompareModularCsvParameters.compareFile, batchExportedFile);
    param.setParameter(CompareModularCsvParameters.filterLevel, Severity.WARN);
    param.setParameter(CompareModularCsvParameters.outFile, false);

    final CompareModularCsvTask comparisonTask = new CompareModularCsvTask(Instant.now(), param);
    comparisonTask.run();
    if (comparisonTask.getStatus() != TaskStatus.FINISHED) {
      return List.of(CheckResult.create("Comparison failed", Severity.ERROR,
          "Comparison check task did not finish for batch file %s".formatted(batchFileName)));
    }
    return comparisonTask.getChecks();
  }

  /**
   * Adds or modifies a modular CSV export step in the provided batch queue. If the last step in the
   * queue is already a CSV export step, its parameters are updated. Otherwise, a new CSV export
   * step is added to the queue with default parameters. The CSV export file is automatically named
   * based on the provided batch name and a random UUID.
   *
   * @param batchName the name of the batch, used to construct the name of the CSV export file.
   * @param tempDir   the temporary directory where the CSV export file will be created.
   * @param queue     the batch queue to which the modular CSV export step will be added or
   *                  modified.
   * @return the file representing the created CSV export file.
   */
  public static @NotNull File addOrModifyModularCsvExportStep(String batchName, File tempDir,
      BatchQueue queue) {
    final MZmineProcessingStep<MZmineProcessingModule> last = queue.getLast();
    final File csvExportFile = new File(tempDir,
        "modular_export_%s_%s.csv".formatted(batchName, UUID.randomUUID().toString()));
    if (last.getModule().getName().equals(CSVExportModularModule.MODULE_NAME)) {
      final ParameterSet parameters = last.getParameterSet();
      parameters.setParameter(CSVExportModularParameters.filename, csvExportFile);
    } else {
      final ParameterSet parameters = new CSVExportModularParameters().cloneParameterSet();
      parameters.setParameter(CSVExportModularParameters.filename, csvExportFile);
      parameters.setParameter(CSVExportModularParameters.filter, FeatureListRowsFilter.ALL);
      parameters.setParameter(CSVExportModularParameters.omitEmptyColumns, true);
      parameters.setParameter(CSVExportModularParameters.idSeparator, ";");
      parameters.setParameter(CSVExportModularParameters.featureLists,
          new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
      queue.add(
          new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(CSVExportModularModule.class),
              parameters));
    }
    csvExportFile.deleteOnExit();
    return csvExportFile;
  }
}
