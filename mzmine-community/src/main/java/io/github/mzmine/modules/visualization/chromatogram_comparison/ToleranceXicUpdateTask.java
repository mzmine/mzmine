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

package io.github.mzmine.modules.visualization.chromatogram_comparison;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.modules.dataprocessing.featdet_extract_mz_ranges.ExtractMzRangesIonSeriesFunction;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.Task;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extracts the XIC of the XIC tolerance around the m/z of the selected group from the scans of the
 * comparison.
 */
class ToleranceXicUpdateTask extends FxUpdateTask<ChromatogramComparisonModel> {

  private final @Nullable ChromatogramGroup group;
  private final @Nullable RawDataFile file;
  private final @NotNull List<Scan> scans;
  private final boolean showXic;
  private final @NotNull MZTolerance tolerance;
  private @Nullable ToleranceXic xic;
  private double progress = 0;

  ToleranceXicUpdateTask(@NotNull ChromatogramComparisonModel model) {
    super("Update chromatogram comparison tolerance XIC", model);
    group = model.getSelectedGroup();
    file = model.getComparisonFile();
    scans = model.getComparisonScans();
    showXic = model.isShowXic();
    tolerance = model.getXicTolerance();
  }

  @Override
  public boolean checkPreConditions() {
    return showXic && group != null && file != null && !scans.isEmpty();
  }

  @Override
  public void onFailedPreCondition() {
    model.setToleranceXic(null);
  }

  @Override
  protected void process() {
    if (group == null || file == null) {
      return;
    }
    xic = extract(group, file, scans, tolerance, this);
    progress = 1;
  }

  /**
   * @param scans in the order of the raw data file
   * @return the XIC, null if canceled
   */
  static @Nullable ToleranceXic extract(@NotNull ChromatogramGroup group,
      @NotNull RawDataFile file, @NotNull List<Scan> scans, @NotNull MZTolerance tolerance,
      @Nullable Task parentTask) {
    // the builders use the mass lists
    final ScanDataType dataType =
        scans.stream().allMatch(scan -> scan.getMassList() != null) ? ScanDataType.MASS_LIST
            : ScanDataType.RAW;
    final BuildingIonSeries[] series = new ExtractMzRangesIonSeriesFunction(file, scans,
        List.of(tolerance.getToleranceRange(group.mz())), dataType, parentTask).get();
    if (series.length != 1) {
      return null;
    }
    return new ToleranceXic(group, tolerance,
        series[0].toIonTimeSeriesWithLeadingAndTrailingZero(null, scans));
  }

  @Override
  protected void updateGuiModel() {
    model.setToleranceXic(xic);
  }

  @Override
  public String getTaskDescription() {
    return "Extracting the XIC with %s".formatted(tolerance);
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }
}
