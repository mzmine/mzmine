/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IntensityTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot.ChromatogramPlotController;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;

public class IntegrationPlotViewBuilder extends FxViewBuilder<IntegrationPlotModel> {

  private static final Logger logger = Logger.getLogger(IntegrationPlotViewBuilder.class.getName());

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

    pane.setFocusTraversable(true);
    pane.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
      pane.getCenter().requestFocus();
    });

    var chromPlot = model.getChromatogramPlot();

    model.currentTimeSeriesProperty().addListener((_, _, series) -> {
      chromPlot.clearDatasets();

      if (series != null) {
        final IntensityTimeSeriesToXYProvider provider = new IntensityTimeSeriesToXYProvider(series,
            getColorForSeries(series));
        chromPlot.addDataset(provider, new ColoredXYLineRenderer());

        var formats = ConfigService.getGuiFormats();
        final UnitFormat uf = formats.unitFormat();
        chromPlot.setDomainAxisLabel(getSeriesDomainLabel(series));
        chromPlot.setDomainAxisFormat(formats.rtFormat());

        chromPlot.setRangeAxisLabel(getSeriesRangeLabel(series));
        chromPlot.setRangeAxisFormat(formats.intensityFormat());
      }
    });

    addFeatureListeners(chromPlot);

    chromPlot.cursorPositionProperty().addListener((_, _, pos) -> {
      // ctrl while clicking allows boundary setting
      if (pos == null) {
        return;
      }

      // set selected feature
      model.getIntegratedFeatures().stream().filter(
              s -> s.getRetentionTime(0) < pos.getDomainValue()
                  && s.getRetentionTime(s.getNumberOfValues() - 1) > pos.getDomainValue()).findFirst()
          .ifPresent(model::setSelectedFeature);

      if (pos.getMouseEvent() != null && !(pos.getMouseEvent().isControlDown()
          || pos.getMouseEvent().isAltDown()) && model.getState() == State.NOT_INTEGRATING) {
        return;
      }

      // only handle regular click if no modifier is down.
      if (pos.getMouseEvent() == null || !(pos.getMouseEvent().isControlDown()
          || pos.getMouseEvent().isAltDown())) {
        if (model.getState() == State.SETTING_LEFT) {
          model.setCurrentStartTime(pos.getDomainValue());
        } else if (model.getState() == State.SETTING_RIGHT) {
          model.setCurrentEndTime(pos.getDomainValue());
        }
      } else if (pos.getMouseEvent() != null && pos.getMouseEvent().isControlDown()) {
        if (pos.getMouseEvent().isShiftDown()) { // ctrl + shift for right boundary
          model.setCurrentEndTime(pos.getDomainValue());
        } else {
          model.setCurrentStartTime(pos.getDomainValue());
        }
      } else if (pos.getMouseEvent() != null && pos.getMouseEvent().isAltDown()) {
        if (model.isCurrentIntegrationValid()) {
          onFinishPressed.run();
        }
      }
    });

    addMarkerListeners(chromPlot);

    final FlowPane buttonBar = createButtonBar();
    pane.setBottom(buttonBar);

    return pane;
  }

  private String getSeriesRangeLabel(IntensityTimeSeries series) {
    final NumberFormats formats = ConfigService.getGuiFormats();
    final UnitFormat uf = formats.unitFormat();

    return switch (series) {
      case OtherTimeSeries other ->
          uf.format(other.getOtherDataFile().getOtherTimeSeriesData().getTimeSeriesRangeLabel(),
              other.getOtherDataFile().getOtherTimeSeriesData().getTimeSeriesRangeUnit());
      default -> uf.format("Intensity", "a.u.");
    };
  }

  private String getSeriesDomainLabel(IntensityTimeSeries series) {
    final NumberFormats formats = ConfigService.getGuiFormats();
    final UnitFormat uf = formats.unitFormat();
    return switch (series) {
      case OtherTimeSeries other ->
          uf.format(other.getOtherDataFile().getOtherTimeSeriesData().getTimeSeriesDomainLabel(),
              other.getOtherDataFile().getOtherTimeSeriesData().getTimeSeriesDomainUnit());
      default -> uf.format("Retention time", "min");
    };
  }

  private Color getColorForSeries(IntensityTimeSeries series) {
    return switch (series) {
      case OtherTimeSeries other ->
          other.getOtherDataFile().getCorrespondingRawDataFile().getColorAWT();
      case IonTimeSeries<?> ion -> ion.getSpectrum(0).getDataFile().getColorAWT();
      case IonMobilitySeries mob -> mob.getSpectrum(0).getDataFile().getColorAWT();
      default -> ConfigService.getDefaultColorPalette().getMainColorAWT();
    };
  }

  private void addFeatureListeners(ChromatogramPlotController chromPlot) {
    model.integratedFeaturesProperty()
        .addListener((ListChangeListener<IntensityTimeSeries>) change -> {
          while (change.next()) {
            if (change.wasAdded()) {
              final List<? extends IntensityTimeSeries> added = change.getAddedSubList();
              final SimpleColorPalette palette = ConfigService.getDefaultColorPalette();
              final List<DatasetAndRenderer> datasets = added.stream().map(
                  feature -> new DatasetAndRenderer(new ColoredXYDataset(
                      new IntensityTimeSeriesToXYProvider(feature, palette.getNextColorAWT()),
                      RunOption.THIS_THREAD), new ColoredAreaShapeRenderer())).toList();
              chromPlot.addDatasets(datasets);
            }

            if (change.wasRemoved()) {
              final List<? extends IntensityTimeSeries> removed = change.getRemoved();
              chromPlot.getDatasetRenderers().keySet().stream().filter(
                      ds -> ds instanceof ColoredXYDataset cds
                          && cds.getValueProvider() instanceof IntensityTimeSeriesToXYProvider its
                          && removed.contains(its.getTimeSeries()))
                  .toList() // terminal operation prior to remove (concurrent mod otherwise)
                  .forEach(chromPlot::removeDataset);
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
        "Set the left boundary of a feature (CTRL + left click).", onSetLeftPressed);
    Button setRightBoundary = FxButtons.createButton("Set right", FxIcons.ARROW_RIGHT,
        "Set the right boundary of a feature. (CTRL + SHIFT + left click)", onSetRightPressed);
    Button finish = FxButtons.createButton("Finish feature", FxIcons.CHECK_CIRCLE,
        "Finish current integration and save the feature.", onFinishPressed);
    Button abortFeature = FxButtons.createButton("Abort feature", FxIcons.CANCEL,
        "Abort integration of the selected feature", onAbortPressed);
    Button editSelected = FxButtons.createButton("Edit feature", FxIcons.EDIT,
        "Edit the selected feature", onEditPressed);
    final FlowPane buttonBar = FxLayout.newFlowPane(setLeftBoundary, setRightBoundary, finish,
        abortFeature, editSelected);

    finish.disableProperty().bind(model.currentIntegrationValidProperty().not());
    editSelected.disableProperty().bind(
        Bindings.createBooleanBinding(() -> model.getSelectedFeature() == null,
            model.selectedFeatureProperty()));

    abortFeature.disableProperty().bind(Bindings.createBooleanBinding(
        () -> model.getCurrentStartTime() == null && model.getCurrentEndTime() == null,
        model.stateProperty(), model.currentStartTimeProperty(), model.currentEndTimeProperty()));

    return buttonBar;
  }
}
