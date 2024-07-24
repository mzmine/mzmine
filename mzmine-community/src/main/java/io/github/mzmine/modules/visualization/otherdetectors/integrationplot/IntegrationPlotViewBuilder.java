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

package io.github.mzmine.modules.visualization.otherdetectors.integrationplot;

import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.OtherFeatureProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.OtherTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot.ChromatogramPlotController;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;

public class IntegrationPlotViewBuilder extends FxViewBuilder<IntegrationPlotModel> {

  private final Runnable onSetLeftPressed;
  private final Runnable onSetRightPressed;
  private final Runnable onFinishPressed;
  private final Runnable onAbortPressed;
  private final Runnable onEditPressed;

  protected IntegrationPlotViewBuilder(IntegrationPlotModel model, Runnable onSetLeftPressed,
      Runnable onSetRightPressed, Runnable onFinishPressed, Runnable onAbortPressed,
      Runnable onEditPressed) {
    super(model);
    this.onSetLeftPressed = onSetLeftPressed;
    this.onSetRightPressed = onSetRightPressed;
    this.onFinishPressed = onFinishPressed;
    this.onAbortPressed = onAbortPressed;
    this.onEditPressed = onEditPressed;
  }

  @Override
  public Region build() {
    BorderPane pane = new BorderPane();
    pane.setCenter(model.getChromatogramPlot().buildView());

    var chromPlot = model.getChromatogramPlot();

    model.currentTimeSeriesProperty().addListener((_, _, series) -> {
      chromPlot.clearDatasets();

      if (series != null) {
        final OtherTimeSeriesToXYProvider provider = new OtherTimeSeriesToXYProvider(series);
        chromPlot.addDataset(provider, new ColoredXYLineRenderer());

        var formats = ConfigService.getGuiFormats();
        final UnitFormat uf = formats.unitFormat();
        chromPlot.setDomainAxisLabel(
            uf.format(series.getOtherDataFile().getOtherTimeSeries().getTimeSeriesDomainLabel(),
                series.getOtherDataFile().getOtherTimeSeries().getTimeSeriesDomainUnit()));
        chromPlot.setDomainAxisFormat(formats.rtFormat());

        chromPlot.setRangeAxisLabel(
            uf.format(series.getOtherDataFile().getOtherTimeSeries().getTimeSeriesRangeLabel(),
                series.getOtherDataFile().getOtherTimeSeries().getTimeSeriesRangeUnit()));
        chromPlot.setRangeAxisFormat(formats.intensityFormat());
      }
    });

    addFeatureListeners(chromPlot);

    chromPlot.cursorPositionProperty().addListener((_, _, pos) -> {
      if (!model.isIntegrating() || pos == null) {
        return;
      }

      // todo soemhow this is not displayed
      if (model.getNextBoundary() == Boundary.LEFT) {
        model.setCurrentStartTime(pos.getDomainValue());
      } else if (model.getNextBoundary() == Boundary.RIGHT) {
        model.setCurrentEndTime(pos.getDomainValue());
      }
    });

    addMarkerListeners(chromPlot);

    final FlowPane buttonBar = createButtonBar();
    pane.setBottom(buttonBar);

    return pane;
  }

  private void addFeatureListeners(ChromatogramPlotController chromPlot) {
    model.otherFeaturesProperty().addListener((ListChangeListener<OtherFeature>) change -> {
      while (change.next()) {
        if (change.wasAdded()) {
          final List<? extends OtherFeature> added = change.getAddedSubList();
          final SimpleColorPalette palette = ConfigService.getDefaultColorPalette();
          final List<DatasetAndRenderer> datasets = added.stream().map(
              feature -> new DatasetAndRenderer(
                  new ColoredXYDataset(new OtherFeatureProvider(feature, palette.getNextColorAWT()),
                      RunOption.THIS_THREAD), new ColoredAreaShapeRenderer())).toList();
          chromPlot.addDatasets(datasets);
        }

        if (change.wasRemoved()) {
          final List<? extends OtherFeature> removed = change.getRemoved();
          chromPlot.getDatasetRenderers().keySet().stream().filter(
              ds -> ds instanceof ColoredXYDataset cds
                  && cds.getValueProvider() instanceof OtherFeatureProvider ofs && removed.contains(
                  ofs.getFeature())).forEach(chromPlot::removeDataset);
        }
      }
    });
  }

  private void addMarkerListeners(ChromatogramPlotController chromPlot) {
    model.currentEndMarkerProperty().addListener((_, prevMarker, newMarker) -> {
      if (prevMarker != null) {
        chromPlot.removeDomainMarker(prevMarker);
      }
      if (newMarker != null) {
        chromPlot.addDomainMarker(newMarker);
      }
    });

    model.currentStartMarkerProperty().addListener((_, prevMarker, newMarker) -> {
      if (prevMarker != null) {
        chromPlot.removeDomainMarker(prevMarker);
      }
      if (newMarker != null) {
        chromPlot.addDomainMarker(newMarker);
      }
    });
  }

  private FlowPane createButtonBar() {
    Button setLeftBoundary = FxButtons.createButton("Set left", FxIcons.ARROW_LEFT,
        "Set the left boundary of a feature.", onSetLeftPressed);
    Button setRightBoundary = FxButtons.createButton("Set right", FxIcons.ARROW_RIGHT,
        "Set the right boundary of a feature.", onSetRightPressed);
    Button finish = FxButtons.createButton("Finish feature", FxIcons.CHECK_CIRCLE,
        "Finish current integration and save the feature", onFinishPressed);
    Button abortFeature = FxButtons.createButton("Abort feature", FxIcons.CANCEL,
        "Abort integration of the selected feature", onAbortPressed);
    Button editSelected = FxButtons.createButton("Edit feature", "Edit the selected feature",
        onEditPressed);
    final FlowPane buttonBar = FxLayout.newFlowPane(setLeftBoundary, setRightBoundary, finish,
        abortFeature, editSelected);

    finish.disableProperty().bind(model.currentIntegrationValidProperty().not());
    editSelected.disableProperty().bind(
        Bindings.createBooleanBinding(() -> model.getSelectedFeature() == null,
            model.selectedFeatureProperty()));
    abortFeature.disableProperty().bind(model.isIntegratingProperty().not());

    return buttonBar;
  }
}
