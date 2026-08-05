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

package io.github.mzmine.modules.dataanalysis.volcanoplot;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.MissingValueType;
import io.github.mzmine.datamodel.statistics.FeaturesDataTable;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTest;
import io.github.mzmine.modules.dataanalysis.significance.RowSignificanceTestResult;
import io.github.mzmine.modules.dataanalysis.significance.UnivariateRowSignificanceTest;
import io.github.mzmine.parameters.parametertypes.statistics.UnivariateRowSignificanceTestConfig;
import io.github.mzmine.taskcontrol.progress.TotalFinishedItemsProgress;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Creates new datasets and updates the data model on FX thread, only if still the latest scheduled
 * task
 */
class VolcanoPlotUpdateTask extends FxUpdateTask<VolcanoPlotModel> {

  private static final Logger logger = Logger.getLogger(VolcanoPlotUpdateTask.class.getName());

  private final @Nullable FeaturesDataTable dataTable;
  private final @Nullable UnivariateRowSignificanceTestConfig testConfig;
  private final double pValue;
  private final TotalFinishedItemsProgress progress = new TotalFinishedItemsProgress();
  private @Nullable List<DatasetAndRenderer> temporaryDatasets;
  /**
   * Set if the plot cannot be computed or is incomplete. Reported to the user in
   * {@link #updateGuiModel()}
   */
  private @Nullable String statusMessage;

  VolcanoPlotUpdateTask(VolcanoPlotModel model) {
    super("volcanoplot_update", model);

    // only capture the inputs here. this constructor runs on the FX thread, so neither the
    // significance test (which copies the whole data table per group) nor its validation may happen
    // here. Exceptions would escape into the JavaFX listener dispatch and kill all further updates.
    dataTable = model.getFeatureDataTable();
    testConfig = model.getTest();
    pValue = model.getpValue();
    progress.setTotal(dataTable != null ? dataTable.getNumberOfFeatures() : 0);
  }

  @Override
  public boolean checkPreConditions() {
    return dataTable != null && testConfig != null;
  }

  @Override
  public void onFailedPreCondition() {
    // clear the plot and explain why, otherwise an empty chart is indistinguishable from a failure
    FxThread.runLater(() -> {
      model.setDatasets(List.of());
      model.setStatusMessage(
          "Select a feature list, a metadata column, and two groups to compute a volcano plot.");
    });
  }

  @Override
  protected void process() {
    if (!checkPreConditions()) {
      return;
    }

    // creating the test resolves the metadata groups and splits the data table - may fail
    final RowSignificanceTest test;
    try {
      test = testConfig.toValidConfig(dataTable);
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Cannot compute volcano plot: " + ex.getMessage(), ex);
      statusMessage = "Cannot compute volcano plot: " + ex.getMessage();
      return;
    }
    if (test == null) {
      statusMessage = """
          Cannot compute volcano plot. Metadata column "%s" with groups "%s" and "%s" is not a valid selection. See the log for details.""".formatted(
          testConfig.column(), testConfig.groupA(), testConfig.groupB());
      return;
    }
    if (!(test instanceof UnivariateRowSignificanceTest<?> ttest)) {
      statusMessage = "The volcano plot requires a univariate significance test.";
      return;
    }

    // use the rows of the data table, not of the feature list. The test operates on subsets of this
    // table, so only these rows are guaranteed to resolve to an abundance index.
    final List<FeatureListRow> rows = dataTable.getFeatureListRows();
    List<RowSignificanceTestResult> rowSignificanceTestResults = new ArrayList<>(rows.size());
    for (final FeatureListRow row : rows) {
      if (isCanceled()) {
        return;
      }
      RowSignificanceTestResult result = test.test(row);
      if (result != null) {
        rowSignificanceTestResults.add(result);
      }
      progress.getAndIncrement();
    }

    final Map<DataType<?>, List<RowSignificanceTestResult>> dataTypeMap = DataTypeUtils.groupByBestDataType(
        rowSignificanceTestResults, RowSignificanceTestResult::row, true,
        CompoundAnnotationUtils.annotationTypePriority.toArray(DataType[]::new));

    final SimpleColorPalette colors = ConfigService.getConfiguration().getDefaultColorPalette()
        .clone(true);
    temporaryDatasets = new ArrayList<>();
    // rows without a computable p value (e.g. constant abundances in both groups) have no
    // -log10(p) to plot. Count them instead of dropping them silently.
    int missingPValues = 0;

    for (Entry<DataType<?>, List<RowSignificanceTestResult>> entry : dataTypeMap.entrySet()) {

      final DataType<?> type = entry.getKey();
      final List<RowSignificanceTestResult> testResults = entry.getValue();

      final List<RowSignificanceTestResult> significantRows = testResults.stream()
          .filter(result -> result.pValue() < pValue).toList();
      final List<RowSignificanceTestResult> insignificantRows = testResults.stream()
          .filter(result -> result.pValue() >= pValue).toList();
      missingPValues += testResults.size() - significantRows.size() - insignificantRows.size();

      final Color color = colors.getNextColorAWT();
      if (!significantRows.isEmpty()) {
        var provider = new VolcanoDatasetProvider(ttest, significantRows, color,
            (type.equals(DataTypes.get(MissingValueType.class)) ? "unknown"
                : type.getHeaderString()) + " (p < " + pValue + ")");
        temporaryDatasets.add(
            new DatasetAndRenderer(new ColoredXYZDataset(provider, RunOption.THIS_THREAD),
                new ColoredXYShapeRenderer(false, ColoredXYShapeRenderer.defaultShape, true)));
      }
      // NOT significant
      if (!insignificantRows.isEmpty()) {
        var provider = new VolcanoDatasetProvider(ttest, insignificantRows, color,
            (type.equals(DataTypes.get(MissingValueType.class)) ? "unknown"
                : type.getHeaderString()) + " (p ≥ " + pValue + ")");
        temporaryDatasets.add(
            new DatasetAndRenderer(new ColoredXYDataset(provider, RunOption.THIS_THREAD),
                new ColoredXYShapeRenderer(true, ColoredXYShapeRenderer.defaultShape, true)));
      }
    }

    if (missingPValues > 0) {
      statusMessage = """
          %d of %d features have no p-value and are not shown. This usually means the abundances are constant within both groups - try a different missing value imputation.""".formatted(
          missingPValues, rows.size());
    }
  }

  @Override
  protected void updateGuiModel() {
    if (temporaryDatasets == null && !isFinished()) {
      // cancelled before any result was produced - keep the current plot
      return;
    }
    // process may have aborted with a reason, then temporaryDatasets is null and the plot is cleared
    model.setDatasets(temporaryDatasets != null ? temporaryDatasets : List.of());
    model.setStatusMessage(statusMessage);
  }

  @Override
  public String getTaskDescription() {
    return "Updating volcano plot";
  }

  @Override
  public double getFinishedPercentage() {
    return progress.progress();
  }


}
