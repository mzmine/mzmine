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

package io.github.mzmine.modules.dataprocessing.filter_rowsfilter;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.statistics.FeaturesDataTable;
import io.github.mzmine.modules.dataanalysis.utils.StatisticUtils;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunctions;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AbundanceMeasureParameter;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.metadata.Metadata1GroupSelection;
import io.github.mzmine.parameters.parametertypes.metadata.Metadata1GroupSelectionParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.statistics.AbundanceDataTablePreparationConfig;
import io.github.mzmine.parameters.parametertypes.statistics.MissingValueImputationParameter;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RsdFilterParameters extends SimpleParameterSet {


  public static final AbundanceMeasureParameter abundanceMeasure = new AbundanceMeasureParameter();

  public static final MissingValueImputationParameter missingValueImputation = new MissingValueImputationParameter();

  public static final Metadata1GroupSelectionParameter grouping = new Metadata1GroupSelectionParameter(
      "Sample grouping", "Select the metadata group to calculate the CV for",
      new Metadata1GroupSelection(MetadataColumn.SAMPLE_TYPE_HEADER, SampleType.QC.toString()));

  public static final PercentParameter maxMissingValues = new PercentParameter(
      "Maximum missing values",
      "Maximum allowed percentage of missing values in the selected group.", 0.2, 0d, 1d);

  public static final PercentParameter maxCv = new PercentParameter("Maximum RSD",
      "Maximum allowed relative standard deviation (coefficient of variation) of a feature inside the selected group.",
      0.2, 0d, 10d);


  public static final BooleanParameter keepUndetected = new BooleanParameter("Keep undetected",
      "Keep features that were not detected in the specified group in the feature list.", false);

  /**
   * The raw data files selected by {@link #grouping} when this filter actually ran. This is
   * execution provenance rather than user input, so it is hidden from the setup dialog and remains
   * unset until a successful run records it in the applied method.
   */
  public static final HiddenParameter<RawDataFilesSelection> resolvedGroupDataFiles =
      new HiddenParameter<>(new RawDataFilesParameter("Resolved RSD group files", 0,
          Integer.MAX_VALUE, null));

  public RsdFilterParameters() {
    super(abundanceMeasure, missingValueImputation, grouping, maxMissingValues, maxCv,
        keepUndetected, resolvedGroupDataFiles);
  }

  public void setAll(AbundanceMeasure measure, ImputationFunctions imputation,
      double maxMissingSamples, double maxRSD, boolean keepUndetected,
      Metadata1GroupSelection grouping) {
    setParameter(abundanceMeasure, measure);
    setParameter(missingValueImputation, imputation);
    setParameter(maxMissingValues, maxMissingSamples);
    setParameter(maxCv, maxRSD);
    setParameter(RsdFilterParameters.grouping, grouping);
    setParameter(RsdFilterParameters.keepUndetected, keepUndetected);
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public @Nullable String getVersionMessage(int version) {
    return switch (version) {
      case 2 -> "Added missing value imputation";
      case 3 -> "Record the raw data files resolved for RSD filtering";
      default -> null;
    };
  }

  @Override
  public int getVersion() {
    return 3;
  }

  /**
   * Records the exact files that contributed to the RSD calculation. The specific-file selection
   * intentionally does not depend on future metadata edits.
   */
  public void recordResolvedGroupDataFiles(@NotNull final List<RawDataFile> rawDataFiles) {
    setParameter(resolvedGroupDataFiles,
        new RawDataFilesSelection(rawDataFiles.toArray(RawDataFile[]::new)));
  }

  /** Clears execution provenance when the optional RSD filter was not used. */
  public void clearResolvedGroupDataFiles() {
    setParameter(resolvedGroupDataFiles, null);
  }

  /**
   * Create a filter for a specific data table. Will automatically select only the grouped data
   * files
   */
  public RsdFilter createFilter(List<FeatureListRow> rows, List<RawDataFile> rawFiles) {
    // prepare data
    final AbundanceMeasure measure = getValue(abundanceMeasure);
    final ImputationFunctions missingImputation = getValue(missingValueImputation);
    final var config = new AbundanceDataTablePreparationConfig(measure, missingImputation);

    final FeaturesDataTable dataTable = StatisticUtils.extractAbundancesPrepareData(rows, rawFiles,
        config);

    // create filter
    final Metadata1GroupSelection group = getValue(RsdFilterParameters.grouping);
    final double maxMissing = getValue(RsdFilterParameters.maxMissingValues);
    final double maxCV = getValue(RsdFilterParameters.maxCv);
    final boolean keepUndedected = getValue(RsdFilterParameters.keepUndetected);

    // subset data table to the grouped data files
    final List<RawDataFile> groupFiles = group.getMatchingFiles(dataTable.getRawDataFiles());
    final FeaturesDataTable subsetTable = dataTable.subsetBySamples(groupFiles);

    return new RsdFilter(group, subsetTable, maxMissing, maxCV, keepUndedected);
  }

}
