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

package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.util.files.ExtensionFilters;
import io.mzio.mzmine.startup.EstimateResourcesSettings;
import java.io.File;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class BatchResourceEstimationParameters extends SimpleParameterSet {

  static final FileNameParameter batchFile = new FileNameParameter("Batch file", """
      The batch to estimate. Its first step must be the data import with all files of the dataset. \
      The batch runs on subsets of these files and the resources are extrapolated to all files.""",
      List.of(ExtensionFilters.MZ_BATCH), FileSelectionType.OPEN, false);

  static final IntegerParameter smallestSubset = new IntegerParameter("Smallest subset",
      """
          Number of files in the smallest subset. Each further subset doubles the number of files. \
          Larger subsets give better estimates but take longer.""", 4, 1, null);

  static final IntegerParameter subsetSizes = new IntegerParameter("Subset sizes", """
      Number of subsets that are extrapolated. A validation run with twice the files of the \
      largest subset follows and checks the extrapolation. The batch needs at least the files of \
      the validation run.""", 3, 2, 5);

  static final OptionalParameter<FileNameParameter> statisticsFile = new OptionalParameter<>(
      new FileNameParameter("Export statistics (TSV)", """
          Export per-step RAM, temporary storage, processing time, and RAM reliability for the \
          measured runs and the full-batch prediction. The file is overwritten after each \
          additional run with the complete updated statistics.""", List.of(ExtensionFilters.TSV),
          FileSelectionType.SAVE, false), false);

  public BatchResourceEstimationParameters() {
    super(batchFile, smallestSubset, subsetSizes, statisticsFile);
  }

  /**
   * @param file     batch to estimate
   * @param settings parsed CLI estimation settings
   * @return module parameters configured for the CLI run
   */
  static @NotNull BatchResourceEstimationParameters fromSettings(@NotNull final File file,
      @NotNull final EstimateResourcesSettings settings) {
    final BatchResourceEstimationParameters parameters = new BatchResourceEstimationParameters();
    parameters.setParameter(batchFile, file);
    parameters.setParameter(smallestSubset, settings.smallestSubset());
    parameters.setParameter(subsetSizes, settings.subsetSizes());
    parameters.setParameter(statisticsFile, settings.statisticsFile() != null,
        settings.statisticsFile());
    return parameters;
  }

  /**
   * Also checks that the batch has enough files for the subsets, so that the dialog stays open
   * instead of starting a run that cannot be done.
   */
  @Override
  public boolean checkParameterValues(@NotNull final Collection<String> errorMessages,
      final boolean skipRawDataAndFeatureListParameters) {
    if (!super.checkParameterValues(errorMessages, skipRawDataAndFeatureListParameters)) {
      return false;
    }
    final File file = getValue(batchFile);
    final Integer availableFiles = BatchResourceEstimationTask.countImportFiles(file);
    if (availableFiles == null) {
      errorMessages.add("Cannot read the batch file or its first step is not the data import: "
          + file.getAbsolutePath());
      return false;
    }
    final int requiredFiles = ResourceSubsetSelector.requiredFiles(getValue(smallestSubset),
        getValue(subsetSizes));
    if (availableFiles < requiredFiles) {
      errorMessages.add(
          BatchResourceEstimationTask.notEnoughFilesMessage(availableFiles, requiredFiles));
      return false;
    }
    return true;
  }
}
