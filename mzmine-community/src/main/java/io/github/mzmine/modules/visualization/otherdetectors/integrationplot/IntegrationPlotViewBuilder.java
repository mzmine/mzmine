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
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot.ChromatogramPlotController;
import io.github.mzmine.util.RangeUtils;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import org.jfree.data.Range;

public class IntegrationPlotViewBuilder extends FxViewBuilder<IntegrationPlotModel> {

  private static final Logger logger = Logger.getLogger(IntegrationPlotViewBuilder.class.getName());

  private final Runnable onSetLeftPressed;
  private final Runnable onSetRightPressed;
  private final Runnable onFinishPressed;
  private final Runnable onAbortPressed;
  private final Runnable onEditPressed;
  final Color peakAreaAllIdentical = new Color(255, 215, 0);
  final Color eicAllIdentical = Color.BLACK;


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

  private static String toStyleColor(Color awtColor) {
    return "rgba(%d,%d,%d,%.3f)".formatted(awtColor.getRed(), awtColor.getGreen(),
        awtColor.getBlue(), awtColor.getAlpha() / 255.0);
  }

  private static Color getBestLabelColorFromBackground(Color background) {
    // Use black or white text depending on which has better contrast with the background color.
    double luminance =
        (0.299 * background.getRed() + 0.587 * background.getGreen() + 0.114 * background.getBlue())
            / 255;
    return luminance > 0.5 ? Color.BLACK : Color.WHITE;
  }

  private void updateTitle(final Label titleLabel) {
    final boolean show = model.isShowTitle();
    final String title = model.getTitle();
    final boolean hasTitle = title != null && !title.isBlank();
    titleLabel.setText(hasTitle ? title : "");
    titleLabel.setVisible(show);
    titleLabel.setManaged(show);
  }

  @Override
  public Region build() {
    BorderPane pane = new BorderPane();
    final Region plotView = model.getChromatogramPlot().buildView();
    pane.setCenter(plotView);

    pane.setFocusTraversable(true);
    pane.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
      pane.getCenter().requestFocus();
    });

    var chromPlot = model.getChromatogramPlot();

    final Label titleLabel = new Label("-");
    titleLabel.getStyleClass().add("integration-dashboard-plot-title");
    titleLabel.setMaxWidth(Double.MAX_VALUE);
    titleLabel.setVisible(model.isShowTitle());
    titleLabel.setManaged(model.isShowTitle());
    pane.setTop(titleLabel);

    PropertyUtils.onChange(() -> this.updateTitle(titleLabel), model.titleProperty(), model.showTitleProperty());

    model.currentTimeSeriesProperty().addListener((_, _, series) -> {
      updateChromatogram(titleLabel, plotView, chromPlot, series);
      updateFeatures(chromPlot); // re-add features after full clear
    });

    model.useSampleColorProperty().addListener((_, _, _) -> {
      updateChromatogram(titleLabel, plotView, chromPlot, model.getCurrentTimeSeries());
      updateFeatures(chromPlot);
    });

    model.showAxisTitlesProperty().subscribe(show -> {
      chromPlot.setDomainAxisLabelVisible(show);
      chromPlot.setRangeAxisLabelVisible(show);
    });

    model.integratedFeaturesProperty()
        .addListener((ListChangeListener<IntensityTimeSeries>) change -> {
          updateFeatures(chromPlot);
        });

    model.additionalTimeSeriesDatasetsProperty().subscribe(this::updateAdditionalDatasets);

    chromPlot.cursorPositionProperty().addListener((_, _, pos) -> {
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
    buttonBar.setPadding(new Insets(1, 2, 1, 2));
    buttonBar.setHgap(2);
    buttonBar.setVgap(1);
    pane.setBottom(buttonBar);

    buttonBar.visibleProperty().bind(model.showControlsProperty());
    buttonBar.managedProperty().bind(buttonBar.visibleProperty());

    return pane;
  }

  private void updateFeatures(ChromatogramPlotController chromPlot) {
    // Remove only feature area datasets — do NOT remove the main chromatogram line renderer.
    // Filtering by renderer type is the safest way to distinguish them.
    chromPlot.getDatasetRenderers().entrySet().stream()
        .filter(e -> e.getValue() instanceof ColoredAreaShapeRenderer).map(Map.Entry::getKey)
        .toList().forEach(chromPlot::removeDataset);

    final List<IntensityTimeSeries> features = model.getIntegratedFeatures();
    if (features.isEmpty()) {
      return;
    }

    final IntensityTimeSeries mainSeries = model.getCurrentTimeSeries();
    final Color sampleColor = mainSeries != null ? getColorForSeries(mainSeries) : eicAllIdentical;

    final List<DatasetAndRenderer> datasets = features.stream().map(feature -> {
      final Color color = model.isUseSampleColor() ? sampleColor : peakAreaAllIdentical;
      final Color transparentColor = new Color(color.getRed(), color.getGreen(), color.getBlue(),
          192);
      return new DatasetAndRenderer(
          new ColoredXYDataset(new IntensityTimeSeriesToXYProvider(feature, transparentColor),
              RunOption.THIS_THREAD), new ColoredAreaShapeRenderer());
    }).toList();
    chromPlot.addDatasets(datasets);
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
    final String strSetLeft = "Set left";
    final String strSetRight = "Set right";
    final String strFinishFeature = "Finish feature";
    final String strAbortFeature = "Abort feature";
    final String strEditFeature = "Edit feature";

    final Button setLeftBoundary = FxButtons.createButton(strSetLeft, FxIcons.ARROW_LEFT,
        "Set the left boundary of a feature (CTRL + left click).", onSetLeftPressed);
    final Button setRightBoundary = FxButtons.createButton(strSetRight, FxIcons.ARROW_RIGHT,
        "Set the right boundary of a feature. (CTRL + SHIFT + left click)", onSetRightPressed);
    final Button finish = FxButtons.createButton(strFinishFeature, FxIcons.CHECK_CIRCLE,
        "Finish current integration and save the feature.", onFinishPressed);
    final Button abortFeature = FxButtons.createButton(strAbortFeature, FxIcons.CANCEL,
        "Abort integration of the selected feature", onAbortPressed);
    final Button editSelected = FxButtons.createButton(strEditFeature, FxIcons.EDIT,
        "Edit the selected feature", onEditPressed);

    final String smallStyle = "-fx-font-size: 9px; -fx-padding: 1 3 1 3;";
    setLeftBoundary.setStyle(smallStyle);
    setRightBoundary.setStyle(smallStyle);
    finish.setStyle(smallStyle);
    abortFeature.setStyle(smallStyle);
    editSelected.setStyle(smallStyle);

    final FlowPane buttonBar = FxLayout.newFlowPane(setLeftBoundary, setRightBoundary, finish,
        abortFeature, editSelected);

    finish.disableProperty().bind(model.currentIntegrationValidProperty().not());
    editSelected.disableProperty().bind(
        Bindings.createBooleanBinding(() -> model.getSelectedFeature() == null,
            model.selectedFeatureProperty()));

    model.stateProperty().subscribe(state -> {
      if (state == State.SETTING_LEFT) {
        setLeftBoundary.requestFocus();
      }
    });

    abortFeature.disableProperty().bind(Bindings.createBooleanBinding(
        () -> model.getCurrentStartTime() == null && model.getCurrentEndTime() == null,
        model.stateProperty(), model.currentStartTimeProperty(), model.currentEndTimeProperty()));

    // a text less option is useful for the integration dashboard
    model.useTextlessButtonsProperty().subscribe(textLess -> {
      setLeftBoundary.setText(textLess ? null : strSetLeft);
      setRightBoundary.setText(textLess ? null : strSetRight);
      finish.setText(textLess ? null : strFinishFeature);
      abortFeature.setText(textLess ? null : strAbortFeature);
      editSelected.setText(textLess ? null : strEditFeature);
    });

    return buttonBar;
  }

  private void updateAdditionalDatasets(List<ColoredXYDataset> oldDs,
      List<ColoredXYDataset> newDs) {
    if (newDs.isEmpty() && oldDs.isEmpty()) {
      return;
    }
    model.getChromatogramPlot().applyWithNotifyChanges(false, () -> {
      model.getChromatogramPlot().removeDatasets(oldDs);
      model.getChromatogramPlot().addDatasets(
          newDs.stream().map(ds -> new DatasetAndRenderer(ds, new ColoredXYLineRenderer()))
              .toList());
    });
  }

  private void updateChromatogram(Label titleLabel, Region plotView,
      ChromatogramPlotController chromPlot, IntensityTimeSeries series) {
    chromPlot.clearDatasets();

    if (series != null) {
      final Color sampleColor = getColorForSeries(series);
      final Color sampleNameColor = getBestLabelColorFromBackground(sampleColor);
      final Color eicColor = model.isUseSampleColor() ? sampleColor : eicAllIdentical;

      final IntensityTimeSeriesToXYProvider provider = new IntensityTimeSeriesToXYProvider(series,
          eicColor);
      chromPlot.addDataset(provider, new ColoredXYLineRenderer());

      // Use inline style (highest CSS priority) so the sample color is never overridden by
      // the application stylesheet.
      titleLabel.getStyleClass().clear();
      titleLabel.getStyleClass().add("integration-dashboard-plot-title");
      titleLabel.setStyle(
          "-fx-background-color: " + toStyleColor(sampleColor) + "; -fx-text-fill: " + toStyleColor(
              sampleNameColor) + ";");
      updateTitle(titleLabel);

      var formats = ConfigService.getGuiFormats();
      chromPlot.setDomainAxisLabel(getSeriesDomainLabel(series));
      chromPlot.setDomainAxisFormat(formats.rtFormat());

      chromPlot.setRangeAxisLabel(getSeriesRangeLabel(series));
      chromPlot.setRangeAxisFormat(formats.intensityFormat());

      if (series.getNumberOfValues() > 0) {
        final Range xRange = chromPlot.getDomainAxisRange();

        if (!RangeUtils.isJFreeRangeConnectedToGuavaRange(xRange,
            com.google.common.collect.Range.closed(series.getRetentionTime(0),
                series.getRetentionTime(series.getNumberOfValues() - 1)))) {
          chromPlot.applyAutoRangeToDomainAxis();
          chromPlot.applyAutoRangeToRangeAxis(); // only change y axis if we also change x axis.
        }
      }
    } else {
      // Clear sample-color background so a stale color from the previous series doesn't persist.
      titleLabel.setStyle(null);
      updateTitle(titleLabel);
    }
  }
}

