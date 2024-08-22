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

package io.github.mzmine.modules.visualization.other_correlationdashboard;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.OtherFeatureDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.MzRangeChromatogramProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot.ChromatogramPlotController;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import io.github.mzmine.parameters.parametertypes.other_detectors.OtherRawOrProcessed;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.color.SimpleColorPalette;
import io.github.mzmine.util.javafx.OtherFeatureSelectionPane;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;

public class CorrelationDashboardViewBuilder extends FxViewBuilder<CorrelationDashboardModel> {

  private final FeatureTableFX featureTable = new FeatureTableFX();
  private Region uvPlot;
  private Region msPlot;
  private Region correlatedPlot;
  private SimpleColorPalette palette;
  private Color otherFeatureColor = palette.getNegativeColorAWT();

  public CorrelationDashboardViewBuilder(CorrelationDashboardModel model) {
    super(model);
  }

  private static @NotNull List<DatasetAndRenderer> getMsFeatureAndChromatogramDatasets(
      Feature feature, RawDataFile file) {
    final List<DatasetAndRenderer> datasets = new ArrayList<>();
    final IonTimeSeriesToXYProvider featureDataSet = new IonTimeSeriesToXYProvider(feature);
    datasets.add(new DatasetAndRenderer(featureDataSet, new ColoredAreaShapeRenderer()));

    final Range<Double> toleranceRange = new MZTolerance(0.005, 10).getToleranceRange(
        feature.getRawDataPointsMZRange());
    final MzRangeChromatogramProvider chromatogram = new MzRangeChromatogramProvider(toleranceRange,
        feature.getFeatureList().getSeletedScans(file),
        "Chromatogram m/z " + ConfigService.getGuiFormats().mz(toleranceRange), file.getColorAWT());

    datasets.add(new DatasetAndRenderer(chromatogram, new ColoredXYLineRenderer()));
    return datasets;
  }

  @Override
  public Region build() {

    uvPlot = model.getUvPlotController().buildView();
    msPlot = model.getMsPlotController().buildView();
    correlatedPlot = model.getCorrelationPlotController().buildView();

    final BorderPane plotsAndControls = new BorderPane();

    // plots and controls on tob and feature table at the bottom
    final SplitPane topBottomSplit = new SplitPane();
    topBottomSplit.setOrientation(Orientation.VERTICAL);
    topBottomSplit.getItems().addAll(plotsAndControls, featureTable);

    final VBox plots = FxLayout.newVBox(Insets.EMPTY, new BorderPane(uvPlot),
        new BorderPane(msPlot));
    plotsAndControls.setCenter(plots);

    DoubleComponent shiftCompoment = new DoubleComponent(10, Double.MIN_VALUE, Double.MAX_VALUE,
        ConfigService.getGuiFormats().rtFormat(), 0d);
    Bindings.bindBidirectional(shiftCompoment.getTextField().textProperty(),
        model.uvToMsRtOffsetProperty(), new StringConverter<>() {
          @Override
          public String toString(Number object) {
            return object != null ? object.toString() : "";
          }

          @Override
          public Number fromString(String string) {
            if (string == null) {
              return 0d;
            }
            string = string.replaceAll("[^0-9\\.]+", "").trim();
            return !string.isEmpty() ? Double.parseDouble(string) : 0;
          }
        });
    final HBox shiftBox = FxLayout.newHBox(FxLabels.newLabel("RT shift:"), shiftCompoment);

    OtherFeatureSelectionPane otherFeatureSelectionPane = new OtherFeatureSelectionPane(
        OtherRawOrProcessed.RAW);
    final VBox controlsAndCorrelation = FxLayout.newVBox(Insets.EMPTY, otherFeatureSelectionPane,
        shiftBox, new BorderPane(correlatedPlot));
    plotsAndControls.setRight(controlsAndCorrelation);

    featureTable.getSelectionModel().selectedItemProperty().addListener((_, _, _) -> {
      if (featureTable.getSelectedRow() == null) {
        model.setSelectedRow(null);
        return;
      }
      model.setSelectedRow(featureTable.getSelectedRow());
    });

    model.selectedRowProperty().addListener((_, _, _) -> updateMsPlot());
    model.selectedRawDataFileProperty().addListener((_, _, _) -> updateMsPlot());

    model.selectedOtherFeatureProperty().addListener((_, _, trace) -> {
      updateUvChart(trace);
    });

    model.selectedOtherFeatureProperty().addListener((_, _, _) -> updateCorrelationChart());
    model.selectedRowProperty().addListener((_, _, _) -> updateCorrelationChart());
    model.selectedRawDataFileProperty().addListener((_, _, _) -> updateCorrelationChart());

    return null;
  }

  /**
   * clears the uv plot and puts the raw trace and all features in that raw trace into the chart.
   */
  private void updateUvChart(OtherFeature trace) {
    model.getUvPlotController().clearDatasets();
    if (trace == null) {
      return;
    }

    final OtherTimeSeriesData data = trace.getOtherDataFile().getOtherTimeSeries();
    final List<OtherFeature> processed = data.getProcessedFeaturesForTrace(trace);

    palette = ConfigService.getDefaultColorPalette();
    List<DatasetAndRenderer> datasets = new ArrayList<>();
    datasets.add(new DatasetAndRenderer(new OtherFeatureDataProvider(trace, otherFeatureColor),
        new ColoredXYLineRenderer()));
    processed.forEach(p -> datasets.add(
        new DatasetAndRenderer(new OtherFeatureDataProvider(p, otherFeatureColor),
            new ColoredAreaShapeRenderer())));

    model.getUvPlotController().addDatasets(datasets);
  }

  /**
   * clears the plot and puts the selected feature and other trace into the chart.
   */
  private void updateCorrelationChart() {
    final ChromatogramPlotController controller = model.getCorrelationPlotController();
    controller.clearDatasets();

    final FeatureListRow row = model.getSelectedRow();
    final RawDataFile file = model.getSelectedRawDataFile();
    final OtherFeature other = model.getSelectedOtherFeature();

    final List<DatasetAndRenderer> datasets = new ArrayList<>();
    if (other != null) {
      datasets.add(new DatasetAndRenderer(new OtherFeatureDataProvider(other, otherFeatureColor),
          new ColoredXYLineRenderer()));
    }
    if (row != null && file != null && row.getFeature(file) != null) {
      datasets.add(new DatasetAndRenderer(new IonTimeSeriesToXYProvider(row.getFeature(file)),
          new ColoredAreaShapeRenderer()));
    }
    controller.addDatasets(datasets);
  }

  /**
   * clears the ms plot and updates it to the currently selected row and feature.
   */
  private void updateMsPlot() {
    model.getMsPlotController().clearDatasets();
    final RawDataFile file = model.getSelectedRawDataFile();
    final FeatureListRow row = model.getSelectedRow();
    if (row == null || file == null) {
      return;
    }
    final Feature feature = row.getFeature(file);
    if (feature == null) {
      return;
    }
    // todo update titles
    final List<DatasetAndRenderer> datasets = getMsFeatureAndChromatogramDatasets(feature, file);
    model.msPlotControllerProperty().get().addDatasets(datasets);
  }
}
