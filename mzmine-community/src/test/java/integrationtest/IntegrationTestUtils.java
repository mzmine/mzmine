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
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchModeModule;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularModule;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularParameters;
import io.github.mzmine.modules.io.export_features_csv.CSVExportModularTask;
import io.github.mzmine.modules.io.export_features_gnps.fbmn.FeatureListRowsFilter;
import io.github.mzmine.modules.io.projectload.ProjectLoaderParameters;
import io.github.mzmine.modules.io.projectload.ProjectOpeningTask;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

public class IntegrationTestUtils {

  private static final Logger logger = Logger.getLogger(IntegrationTestUtils.class.getName());

  @NotNull
  public static File urlToFile(@Nullable final URL url) {
    if (url == null) {
      throw new RuntimeException("URL is null");
    }
    return new File(url.getFile());
  }

  /**
   * Executes a batch comparison process using the provided batch file.
   *
   * @param batchFile                 the relative path string of the batch file to be executed
   * @param baseCsvFile               the relative path string  of the base CSV file for comparison
   * @param tempDir                   the temporary directory for intermediate files during
   *                                  execution
   * @param overrideDataFiles         optional array of relative path strings  for data files that
   *                                  override batch defaults (null or empty means no overrides)
   * @param overrideSpectralLibraries optional array of relative path strings  for spectral library
   *                                  files that override batch defaults (null or empty means no
   *                                  overrides)
   * @return a list of CheckResult objects containing the comparison results; the list is empty if
   * all comparisons are successful
   */
  public static List<CheckResult> runBatchCompareToCsv(String batchFile, String baseCsvFile,
      File tempDir, @Nullable String @Nullable [] overrideDataFiles,
      @Nullable String @Nullable [] overrideSpectralLibraries) {

    final URL batch = IntegrationTestUtils.class.getClassLoader().getResource(batchFile);
    final URL expectedResultsUrl = IntegrationTestUtils.class.getClassLoader()
        .getResource(baseCsvFile);

    final @Nullable File @Nullable [] files =
        overrideDataFiles != null ? Arrays.stream(overrideDataFiles).filter(Objects::nonNull)
            .map(str -> IntegrationTestUtils.class.getClassLoader().getResource(str))
            .filter(Objects::nonNull).map(IntegrationTestUtils::urlToFile).toArray(File[]::new)
            : null;

    final @Nullable File @Nullable [] libraries =
        overrideSpectralLibraries != null ? Arrays.stream(overrideSpectralLibraries)
            .filter(Objects::nonNull)
            .map(str -> IntegrationTestUtils.class.getClassLoader().getResource(str))
            .filter(Objects::nonNull).map(IntegrationTestUtils::urlToFile).toArray(File[]::new)
            : null;

    if (Objects.requireNonNullElse(files, new File[0]).length != Objects.requireNonNullElse(
        overrideDataFiles, new File[0]).length) {
      throw new RuntimeException("Not all data files were found");
    }

    if (Objects.requireNonNullElse(libraries, new File[0]).length != Objects.requireNonNullElse(
        overrideSpectralLibraries, new File[0]).length) {
      throw new RuntimeException("Not all libraries were found");
    }

    return runBatchCompareToCsv(urlToFile(batch), urlToFile(expectedResultsUrl), tempDir, files,
        libraries);
  }

  /**
   * Executes a batch comparison process using the provided batch file.
   *
   * @param batchFile                 the batch file to be executed
   * @param baseCsvFile               the base CSV file for comparison
   * @param tempDir                   the temporary directory for intermediate files during
   *                                  execution
   * @param overrideDataFiles         optional array of files for data files (null or empty means no
   *                                  overrides)
   * @param overrideSpectralLibraries optional array of spectral library files that override batch
   *                                  defaults (null or empty means no overrides)
   * @return a list of CheckResult objects containing the comparison results; the list is empty if
   * all comparisons are successful
   */
  public static List<CheckResult> runBatchCompareToCsv(File batchFile, File baseCsvFile,
      File tempDir, @Nullable File @Nullable [] overrideDataFiles,
      @Nullable File @Nullable [] overrideSpectralLibraries) {

    final String batchFileName = batchFile.getName();
    final File csvExportFile = runBatchGetExportedCsv(batchFile, tempDir, overrideDataFiles,
        overrideSpectralLibraries);

    return getCsvComparisonResults(baseCsvFile, csvExportFile, batchFileName);
  }


  public static List<CheckResult> runBatchCompareToCsv(@NotNull final IntegrationTest test,
      String expectedResultsFullPath) {
    final URL resource = IntegrationTestUtils.class.getClassLoader().getResource(expectedResultsFullPath);
    return runBatchCompareToCsv(test.batchFile(),
        urlToFile(resource), test.tempDir(),
        test.rawFiles(), test.specLibs());
  }

  /**
   * Runs a batch and returns the file of the exported modular csv file. This method can be used if
   * multiple comparisons shall be done, e.g., a purposefully failing test.
   */
  public static @NotNull File runBatchGetExportedCsv(@NotNull String batchFile,
      @NotNull File tempDir, @Nullable String @Nullable [] overrideDataFiles,
      @Nullable String @Nullable [] overrideSpectralLibraries) {

    final URL batch = IntegrationTestUtils.class.getClassLoader().getResource(batchFile);
    final @Nullable File @Nullable [] files =
        overrideDataFiles != null ? Arrays.stream(overrideDataFiles).filter(Objects::nonNull)
            .map(str -> IntegrationTestUtils.class.getClassLoader().getResource(str))
            .filter(Objects::nonNull).map(IntegrationTestUtils::urlToFile).toArray(File[]::new)
            : null;

    final @Nullable File @Nullable [] libraries =
        overrideSpectralLibraries != null ? Arrays.stream(overrideSpectralLibraries)
            .filter(Objects::nonNull)
            .map(str -> IntegrationTestUtils.class.getClassLoader().getResource(str))
            .filter(Objects::nonNull).map(IntegrationTestUtils::urlToFile).toArray(File[]::new)
            : null;

    if (Objects.requireNonNullElse(files, new File[0]).length != Objects.requireNonNullElse(
        overrideDataFiles, new File[0]).length) {
      throw new RuntimeException("Not all data files were found");
    }

    if (Objects.requireNonNullElse(libraries, new File[0]).length != Objects.requireNonNullElse(
        overrideSpectralLibraries, new File[0]).length) {
      throw new RuntimeException("Not all libraries were found");
    }

    return runBatchGetExportedCsv(urlToFile(batch), tempDir, files, libraries);
  }

  public static File runBatchGetExportedCsv(@NotNull final IntegrationTest test) {
    return runBatchGetExportedCsv(test.batchFile(), test.tempDir(), test.rawFiles(),
        test.specLibs());
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

    final BatchTask batchTask = BatchModeModule.runBatchQueue(queue, project, overrideDataFiles,
        null, overrideSpectralLibraries, null, Instant.now());

    checkImportedFilesAndLibraries(overrideDataFiles, overrideSpectralLibraries, batchFileName,
        project);

    if (batchTask == null || batchTask.getStatus() != TaskStatus.FINISHED) {
      throw new RuntimeException(
          "Batch task for batch file %s did not finish".formatted(batchFileName));
    }
    return csvExportFile;
  }

  private static void checkImportedFilesAndLibraries(@Nullable File @Nullable [] overrideDataFiles,
      @Nullable File @Nullable [] overrideSpectralLibraries, @NotNull String batchFileName,
      @NotNull MZmineProject project) {
    logger.info("Batch task for file %s finished. Batch resulted in %d feature lists.".formatted(
        batchFileName, project.getNumberOfFeatureLists()));
    if (overrideDataFiles != null && overrideDataFiles.length != project.getNumberOfDataFiles()) {
      throw new RuntimeException(
          "Batch task for file %s loaded %d/%d files.".formatted(batchFileName,
              overrideDataFiles.length, project.getNumberOfDataFiles()));
    }
    if (overrideSpectralLibraries != null
        && overrideSpectralLibraries.length != project.getNumberOfLibraries()) {
      throw new RuntimeException(
          "Batch task for file %s loaded %d/%d spectral libraries.".formatted(batchFileName,
              overrideSpectralLibraries.length, project.getCurrentSpectralLibraries().size()));
    }
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

  /**
   * @param expectedResultsFullPath path string to the expected results.
   * @param batchExportedFile   the exported csv file.
   * @param batchFileName       the batch name for logging.
   * @return A list of check results. empty if everything was ok.
   */
  public static List<@NotNull CheckResult> getCsvComparisonResults(
      @NotNull String expectedResultsFullPath, File batchExportedFile, String batchFileName) {

    return getCsvComparisonResults(
        urlToFile(IntegrationTestUtils.class.getClassLoader().getResource(expectedResultsFullPath)),
        batchExportedFile, batchFileName);
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
      final ParameterSet parameters = CSVExportModularParameters.create(csvExportFile,
          FeatureListRowsFilter.ALL, true, ";", ",",
          new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS));
      queue.add(
          new MZmineProcessingStepImpl<>(MZmineCore.getModuleInstance(CSVExportModularModule.class),
              parameters));
    }
    csvExportFile.deleteOnExit();
    return csvExportFile;
  }

  /**
   * @param projectFile relative file path to the project in the resources folder.
   * @return The exported feature list.
   */
  static @NotNull File loadProjectExportFeatureList(File tempDir, String projectFile) {
    final URL resource = IntegrationTestUtils.class.getClassLoader().getResource(projectFile);
    return loadProjectExportFeatureList(tempDir, urlToFile(resource));
  }

  static @NotNull File loadProjectExportFeatureList(File tempDir, File projectFile) {
    // clear old porject
    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());

    var parameters = (ProjectLoaderParameters) new ProjectLoaderParameters().cloneParameterSet();
    parameters.setParameter(ProjectLoaderParameters.projectFile, projectFile);
    ProjectOpeningTask task = new ProjectOpeningTask(parameters, Instant.now());
    task.run();

    // new project created in project opening task
    final MZmineProject loadedProject = ProjectService.getProject();

    final FeatureList finalFlist = loadedProject.getCurrentFeatureLists().stream()
        .max(Comparator.comparingInt(fl -> fl.getName().length())).get();

    final File csvExportFile = new File(tempDir,
        "modular_export_%s_%s.csv".formatted(projectFile.getName(), UUID.randomUUID().toString()));

    final CSVExportModularTask exportTask = new CSVExportModularTask(
        new ModularFeatureList[]{(ModularFeatureList) finalFlist}, csvExportFile, ",", ";",
        FeatureListRowsFilter.ALL, true, Instant.now());
    exportTask.run();
    return csvExportFile;
  }
}
