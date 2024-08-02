/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.javafx.SortableFeatureComboBox;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ParameterDialogWithFeaturePreview extends ParameterSetupDialogWithPreview {

  protected final NumberFormats formats = ConfigService.getGuiFormats();
  private final SimpleXYChart<? extends PlotXYDataProvider> chart;
  private final DialogController controller = new DialogController();
  private SortableFeatureComboBox featureBox;

  public ParameterDialogWithFeaturePreview(boolean valueCheckRequired, ParameterSet parameters,
      Region message) {
    super(valueCheckRequired, parameters, message);
    chart = createChart();
    addPreviewPane();
  }

  public ParameterDialogWithFeaturePreview(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);
    chart = createChart();
    addPreviewPane();
  }

  protected abstract @NotNull SimpleXYChart<PlotXYDataProvider> createChart();

  @Override
  protected void parametersChanged() {
    super.parametersChanged();
    updatePreview();
  }

  /**
   * Called if the parameters or the selected feature change.
   */
  private void updatePreview() {
    updateParameterSetFromComponents();

    var task = new FxUpdateTask<>("update baseline preview", new Object()) {
      private List<DatasetAndRenderer> datasetAndRenderers;

      @Override
      public String getTaskDescription() {
        return "Updating preview";
      }

      @Override
      public double getFinishedPercentage() {
        return 0;
      }

      @Override
      protected void process() {
        datasetAndRenderers = calculateNewDatasets(featureBox.getSelectedFeature());
      }

      @Override
      protected void updateGuiModel() {
        updateChart(datasetAndRenderers, chart);
      }
    };

    controller.onTaskThreadDelayed(task);
  }

  private void addPreviewPane() {
    previewWrapperPane.setCenter(chart);

    ObservableList<FeatureList> flists = FXCollections.observableArrayList(
        ProjectService.getProjectManager().getCurrentProject().getCurrentFeatureLists());

    ComboBox<FeatureList> flistBox = new ComboBox<>(flists);
    featureBox = new SortableFeatureComboBox();

    GridPane controls = new GridPane(FxLayout.DEFAULT_SPACE, FxLayout.DEFAULT_SPACE);
    controls.add(FxLabels.newLabel("Feature list: "), 0, 0);
    controls.add(FxLabels.newLabel("Feature: "), 0, 1);
    controls.add(flistBox, 1, 0);
    controls.add(featureBox, 1, 1);

    flistBox.valueProperty().addListener((_, _, flist) -> {
      featureBox.setItems(flist.getFeatures(flist.getRawDataFile(0)));
    });

    featureBox.selectedFeatureProperty().addListener((_, _, _) -> updatePreview());

    previewWrapperPane.setBottom(controls);
  }

  /**
   * @param datasets The new datasets.
   * @param chart    The chart of the preview.
   */
  protected abstract void updateChart(@NotNull List<DatasetAndRenderer> datasets,
      @NotNull final SimpleXYChart<? extends PlotXYDataProvider> chart);

  /**
   * Calculate the new datasets after the feature changed. Parameters are update. This calculation
   * is run on a separate thread. The datasets are added to the chart in
   * {@link ParameterDialogWithFeaturePreview#updateChart(List, SimpleXYChart)} )}.
   */
  protected abstract @NotNull List<@NotNull DatasetAndRenderer> calculateNewDatasets(
      @Nullable final Feature feature);
}
