/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.filter_interestingfeaturefinder.visualization;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonMobilogramTimeSeriesToRtMobilityHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotRenderer;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.javafx.FxColorUtil;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Rectangle;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ui.RectangleEdge;

public class MultiImsTraceVisualiser extends BorderPane {

  private static final Logger logger = Logger.getLogger(MultiImsTraceVisualiser.class.getName());

  private static final int HEATMAP_LEGEND_HEIGHT = 50;
  private static final double MIN_MOBILOGRAM_WIDTH = 250;

  private final SimpleXYZScatterPlot<IonMobilogramTimeSeriesToRtMobilityHeatmapProvider> traceChart;
  private final SimpleXYChart<IonTimeSeriesToXYProvider> ticChart;
  private final SimpleXYChart<SummedMobilogramXYProvider> mobilogramChart;
  private final Canvas traceLegendCanvas;

  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat mobilityFormat;
  private final NumberFormat intensityFormat;
  private final UnitFormat unitFormat;

  private final ListProperty<ModularFeature> featuresProperty;

  /**
   * Keeps track of the raw file. We only have to update the tic data set if we change raw file.
   */
  private final ObjectProperty<RawDataFile> rawFileProperty;

  private int ticDatasetIndex = 0;
  private int ticFeatureDatasetIndex = 0;


  public MultiImsTraceVisualiser() {
    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();

    traceChart = new SimpleXYZScatterPlot<>("Ion mobility trace");
    ticChart = new SimpleXYChart<>("Feature shape");
    mobilogramChart = new SimpleXYChart<>("Summed mobilogram");
    traceLegendCanvas = new Canvas();

    featuresProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    featuresProperty
        .addListener((ListChangeListener<? super ModularFeature>) c -> onFeaturesChanged(c));
    rawFileProperty = new SimpleObjectProperty<>();
    rawFileProperty
        .addListener(((observable, oldValue, newValue) -> onRawFileChanged(oldValue, newValue)));

    initCharts();
    updateAxisLabels();
  }

  public MultiImsTraceVisualiser(@Nullable final Collection<ModularFeature> features) {
    this();
    featuresProperty.clear();
    if (features != null) {
      featuresProperty.addAll(features);
    }
  }

  private void onRawFileChanged(RawDataFile oldValue, RawDataFile newFile) {
    assert Platform.isFxApplicationThread();

    if (oldValue != newFile) {
      ticChart.removeDataSet(ticDatasetIndex, false);
    }
    if(newFile == null) {
      ticChart.getChart().fireChartChanged();
      return;
    }

    Range<Double> bpcMzRange = newFile.getDataMZRange();
    if (!getFeatures().isEmpty()) {
      double min = getFeatures().stream()
          .mapToDouble(f -> f.getRawDataPointsMZRange().lowerEndpoint()).min()
          .orElseGet(() -> newFile.getDataMZRange().lowerEndpoint());
      double max = getFeatures().stream()
          .mapToDouble(f -> f.getRawDataPointsMZRange().lowerEndpoint()).min()
          .orElseGet(() -> newFile.getDataMZRange().upperEndpoint());
      bpcMzRange = Range.closed(min, max);
    }

    final TICDataSet dataSet = new TICDataSet(newFile, newFile.getScanNumbers(1), bpcMzRange, null,
        TICPlotType.BASEPEAK);
    TICPlotRenderer renderer = new TICPlotRenderer();
    renderer.setSeriesPaint(0,
        MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT());
    renderer.setDefaultShapesVisible(false);
//    dataSet.setCustomSeriesKey(
//        "BPC " + mzFormat.format(getFeature().getRawDataPointsMZRange().lowerEndpoint()) + " - "
//            + mzFormat.format(getFeature().getRawDataPointsMZRange().upperEndpoint()));
    ticDatasetIndex = ticChart.addDataset(dataSet, renderer);
  }

  private void onFeaturesChanged(Change<? extends ModularFeature> change) {
    assert Platform.isFxApplicationThread();

    clearFeatureFromCharts();
    final ObservableList<? extends ModularFeature> features = change.getList();
    if (features.isEmpty()) {
      return;
    }

    // show bpc for one raw file
    rawFileProperty.set(features.get(0).getRawDataFile());
    updateAxisLabels();

    for (ModularFeature feature : features) {
      var clr = MZmineCore.getConfiguration().getDefaultColorPalette().getNextColor();
      if (clr.equals(javafx.scene.paint.Color.BLACK)) {
        clr = MZmineCore.getConfiguration().getDefaultColorPalette().getNextColor();
      }

      final var ionTrace = new IonMobilogramTimeSeriesToRtMobilityHeatmapProvider(
          (IonMobilogramTimeSeries) feature.getFeatureData(), FeatureUtils.featureToString(feature),
          clr, true);
      traceChart.addDataset(ionTrace);

      final ColoredXYDataset mobilogram = new ColoredXYDataset(
          new SummedMobilogramXYProvider(feature, true), RunOption.THIS_THREAD);
      mobilogram.setColor(FxColorUtil.fxColorToAWT(clr));
      mobilogramChart.addDataset(mobilogram, new ColoredXYLineRenderer());

      final ColoredXYDataset dataSet = new ColoredXYDataset(new IonTimeSeriesToXYProvider(feature),
          RunOption.THIS_THREAD);
      dataSet.setColor(FxColorUtil.fxColorToAWT(clr));
      ticFeatureDatasetIndex = ticChart.addDataset(dataSet, new ColoredXYLineRenderer());
    }
    traceChart.getXYPlot().getRangeAxis().setAutoRange(true);
    traceChart.getXYPlot().getDomainAxis().setAutoRange(true);
  }

  private void clearFeatureFromCharts() {
    traceChart.removeAllDatasets();
    mobilogramChart.removeAllDatasets();
    ticChart.removeDataSet(ticFeatureDatasetIndex, false);
  }

  private void updateAxisLabels() {
    final String intensityLabel = unitFormat.format("Intensity", "a.u.");
    String mobilityLabel = "Mobility";
    if (rawFileProperty.get() instanceof IMSRawDataFile file) {
      mobilityLabel = file.getMobilityType().getAxisLabel();
    }
    mobilogramChart.setRangeAxisLabel(mobilityLabel);
    mobilogramChart.setRangeAxisNumberFormatOverride(mobilityFormat);
    mobilogramChart.setDomainAxisLabel(intensityLabel);
    mobilogramChart.setDomainAxisNumberFormatOverride(intensityFormat);
    traceChart.setDomainAxisLabel(unitFormat.format("Retention time", "min"));
    traceChart.setRangeAxisLabel(mobilityLabel);
    traceChart.setDomainAxisNumberFormatOverride(rtFormat);
    traceChart.setRangeAxisNumberFormatOverride(mobilityFormat);
    traceChart.setLegendNumberFormatOverride(intensityFormat);
    ticChart.setDomainAxisLabel(unitFormat.format("Retention time", "min"));
    ticChart.setDomainAxisNumberFormatOverride(rtFormat);
    ticChart.setRangeAxisLabel(unitFormat.format("Intensity", "a.u."));
    ticChart.setRangeAxisNumberFormatOverride(intensityFormat);
  }

  private void initCharts() {
    mobilogramChart.getXYPlot().getDomainAxis().setInverted(true);
    mobilogramChart.setShowCrosshair(false);
    mobilogramChart.setLegendItemsVisible(false);
    NumberAxis axis = (NumberAxis) mobilogramChart.getXYPlot().getRangeAxis();
    axis.setAutoRangeMinimumSize(0.2);
    axis.setAutoRangeIncludesZero(false);
    axis.setAutoRangeStickyZero(false);
    mobilogramChart.setMinHeight(300);
    mobilogramChart.setMinWidth(MIN_MOBILOGRAM_WIDTH);

    mobilogramChart.addDatasetChangeListener(e -> {
      mobilogramChart.getXYPlot().getRangeAxis().setAutoRange(true);
      mobilogramChart.getXYPlot().getDomainAxis().setAutoRange(true);
    });

    traceChart.setShowCrosshair(false);
    traceChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    traceChart.setDefaultPaintscaleLocation(RectangleEdge.BOTTOM);
    traceChart.setMinHeight(500);
    traceChart.setMinWidth(500);
    traceLegendCanvas.setHeight(HEATMAP_LEGEND_HEIGHT);
    traceLegendCanvas.setWidth(500);
    traceChart.setLegendCanvas(traceLegendCanvas);
    BorderPane.setAlignment(traceLegendCanvas, Pos.TOP_RIGHT);
    traceChart.addDatasetChangeListener(e -> {
      traceChart.getXYPlot().getRangeAxis().setAutoRange(true);
      traceChart.getXYPlot().getDomainAxis().setAutoRange(true);
    });

    ticChart.getXYPlot().setDomainCrosshairVisible(false);
    ticChart.getXYPlot().setRangeCrosshairVisible(false);
    ticChart.setMinHeight(200);

    ticChart.addDatasetChangeListener(e -> {
      ticChart.getXYPlot().getRangeAxis().setAutoRange(true);
      ticChart.getXYPlot().getDomainAxis().setAutoRange(true);
    });

    ChartGroup rtGroup = new ChartGroup(false, false, true, false);
    rtGroup.add(new ChartViewWrapper(ticChart));
    rtGroup.add(new ChartViewWrapper(traceChart));

    ChartGroup mobilityGroup = new ChartGroup(false, false, false, true);
    mobilityGroup.add(new ChartViewWrapper(traceChart));
    mobilityGroup.add(new ChartViewWrapper(mobilogramChart));

    BorderPane ticWrap = new BorderPane(ticChart, null, null, null,
        new Rectangle(MIN_MOBILOGRAM_WIDTH, 1, javafx.scene.paint.Color.TRANSPARENT));

    setCenter(traceChart);
    setTop(ticWrap);
    setLeft(mobilogramChart);
    setBottom(traceLegendCanvas);
  }

  public List<ModularFeature> getFeatures() {
    return featuresProperty.get();
  }

  public ListProperty<ModularFeature> featureProperty() {
    return featuresProperty;
  }

  public void setFeatures(List<ModularFeature> features) {
    featuresProperty.clear();
    featuresProperty.addAll(features);
  }

}
