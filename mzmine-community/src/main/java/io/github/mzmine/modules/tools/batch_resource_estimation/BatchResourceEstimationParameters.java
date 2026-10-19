package io.github.mzmine.modules.tools.batch_resource_estimation;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.util.files.ExtensionFilters;
import java.io.File;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class BatchResourceEstimationParameters extends SimpleParameterSet {

  public static final FileNameParameter batchFile = new FileNameParameter("Batch file", """
      The batch to estimate. Its first step must be the data import with all files of the dataset. \
      The batch runs on subsets of these files and the resources are extrapolated to all files.""",
      List.of(ExtensionFilters.MZ_BATCH), FileSelectionType.OPEN, false);

  public static final IntegerParameter smallestSubset = new IntegerParameter("Smallest subset",
      """
          Number of files in the smallest subset. Each further subset doubles the number of files. \
          Larger subsets give better estimates but take longer.""", 4, 1, null);

  public static final IntegerParameter subsetSizes = new IntegerParameter("Subset sizes", """
      Number of subsets that are extrapolated. A validation run with twice the files of the \
      largest subset follows and checks the extrapolation. The batch needs at least the files of \
      the validation run.""", 3, 2, 5);

  public BatchResourceEstimationParameters() {
    super(batchFile, smallestSubset, subsetSizes);
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
