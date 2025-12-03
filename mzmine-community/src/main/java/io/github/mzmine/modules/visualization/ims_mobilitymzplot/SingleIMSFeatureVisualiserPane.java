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

package io.github.mzmine.modules.visualization.ims_mobilitymzplot;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonMobilogramTimeSeriesToRtMobilityHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.SingleSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.Color;
import java.text.NumberFormat;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.util.StringConverter;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.ui.RectangleEdge;

public class SingleIMSFeatureVisualiserPane extends GridPane {

  private final SimpleXYZScatterPlot<IonMobilogramTimeSeriesToRtMobilityHeatmapProvider> heatmapChart;
  private final SimpleXYChart<SingleSpectrumProvider> msmsSpectrumChart;
  private final SimpleXYChart<SummedMobilogramXYProvider> mobilogramChart;

  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat mobilityFormat;
  private final NumberFormat intensityFormat;
  private final UnitFormat unitFormat;

  private final ObjectProperty<ModularFeature> feature = new SimpleObjectProperty<>();
  private final ObservableValue<String> featureString;
  private final ObjectProperty<MobilityScan> selectedMobilityScan;
  private ComboBox<Scan> fragmentScanSelection;

  public SingleIMSFeatureVisualiserPane() {
    this(null);
  }

  public SingleIMSFeatureVisualiserPane(@Nullable ModularFeature f) {
    super();

    getStylesheets().addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    feature.set(f);
    featureString = feature.map(FeatureUtils::featureToString).orElse("");

    this.heatmapChart = new SimpleXYZScatterPlot<>("Ion trace");
    this.msmsSpectrumChart = new SimpleXYChart<>("MS/MS");
    mobilogramChart = new SimpleXYChart<>("Extracted mobilogram", false);
    // add flipped chart gestures
    ChartGestureHandler.addStandardGestures(mobilogramChart, true);

    heatmapChart.getChartModel().titleProperty()
        .bind(featureString.map(fstr -> "Ion trace - " + fstr));
    msmsSpectrumChart.getChartModel().titleProperty()
        .bind(featureString.map(fstr -> "MS/MS - " + fstr));
    mobilogramChart.getChartModel().titleProperty()
        .bind(featureString.map(fstr -> "Extracted mobilogram - " + fstr));

    mobilogramChart.getXYPlot().setShowCursorCrosshair(false, true);

    heatmapChart.getXYPlot().rangeCursorValueProperty()
        .bindBidirectional(mobilogramChart.getXYPlot().rangeCursorValueProperty());

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    selectedMobilityScan = new SimpleObjectProperty<>();

    initCharts();

    initChartPanes();

    ColumnConstraints col0 = new ColumnConstraints(150);
    ColumnConstraints col1 = new ColumnConstraints(340);

    RowConstraints row0 = new RowConstraints(250);
    RowConstraints row3 = new RowConstraints(250);

    getRowConstraints().addAll(row0, row3);
    getColumnConstraints().addAll(col0, col1);

    // last update charts
    feature.subscribe(this::updateChartsForFeature);
  }

  private void updateChartsForFeature(ModularFeature feature) {
    if (feature == null) {
      heatmapChart.getXYPlot().removeAllDatasets();
      mobilogramChart.getXYPlot().removeAllDatasets();
      msmsSpectrumChart.getXYPlot().removeAllDatasets();
      return;
    }

    final MobilityType mobilityType = ((IMSRawDataFile) feature.getRawDataFile()).getMobilityType();
    heatmapChart.setRangeAxisLabel(mobilityType.getAxisLabel());
    mobilogramChart.setRangeAxisLabel(mobilityType.getAxisLabel());

    heatmapChart.setDataset(new IonMobilogramTimeSeriesToRtMobilityHeatmapProvider(feature));
    mobilogramChart.setDataset((new SummedMobilogramXYProvider(feature, true)));

    Scan msmsSpectrum = feature.getMostIntenseFragmentScan();
    // combobox
    fragmentScanSelection.setItems(FXCollections.observableList(feature.getAllMS2FragmentScans()));
    if (msmsSpectrum != null) {
      fragmentScanSelection.setValue(msmsSpectrum);
    }
  }

  private void initCharts() {

    heatmapChart.setDomainAxisLabel(unitFormat.format("Retention time", "min"));
    heatmapChart.setDomainAxisNumberFormatOverride(rtFormat);
    heatmapChart.setRangeAxisNumberFormatOverride(mobilityFormat);
    heatmapChart.setLegendAxisLabel(unitFormat.format("Intensity", "counts"));
    heatmapChart.setLegendNumberFormatOverride(intensityFormat);
    heatmapChart.setDefaultPaintscaleLocation(RectangleEdge.RIGHT);
    heatmapChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    heatmapChart.setShowCrosshair(true);
    heatmapChart.cursorPositionProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.getDataset() instanceof ColoredXYDataset) {
        ColoredXYDataset dataset = (ColoredXYDataset) newValue.getDataset();
        if (dataset.getValueProvider() instanceof MassSpectrumProvider) {
          MassSpectrumProvider spectrumProvider = (MassSpectrumProvider) dataset.getValueProvider();
          MassSpectrum spectrum = spectrumProvider.getSpectrum(newValue.getValueIndex());
          if (spectrum instanceof MobilityScan) {
            selectedMobilityScan.set((MobilityScan) spectrum);
          }
        }
      }
    });
    heatmapChart.addDatasetChangeListener(e -> {
      if (!(e.getDataset() instanceof ColoredXYDataset ds) || (ds.getStatus()
          != TaskStatus.FINISHED)) {
        return;
      }
      final Range<Double> domainValueRange = ((ColoredXYDataset) e.getDataset()).getDomainValueRange();
      heatmapChart.getXYPlot().getDomainAxis()
          .setRange(RangeUtils.guavaToJFree(RangeUtils.getPositiveRange(domainValueRange, 0.0001d)),
              false, true);
      final Range<Double> rangeValueRange = ((ColoredXYDataset) e.getDataset()).getRangeValueRange();
      heatmapChart.getXYPlot().getRangeAxis()
          .setRange(RangeUtils.guavaToJFree(RangeUtils.getPositiveRange(rangeValueRange, 0.0001d)),
              false, true);
    });
    NumberAxis axis = (NumberAxis) heatmapChart.getXYPlot().getRangeAxis();
    axis.setAutoRangeIncludesZero(false);
    axis.setAutoRangeStickyZero(false);
    axis.setVisible(false);

    axis = (NumberAxis) mobilogramChart.getXYPlot().getRangeAxis();
    axis.setAutoRange(true);
    axis.setAutoRangeIncludesZero(false);
    axis.setAutoRangeStickyZero(false);

    axis = (NumberAxis) mobilogramChart.getXYPlot().getDomainAxis();
    axis.setAutoRangeIncludesZero(true);
    axis.setAutoRangeStickyZero(true);
    axis.setAutoRange(true);

    msmsSpectrumChart.setDomainAxisNumberFormatOverride(mzFormat);
    msmsSpectrumChart.setDomainAxisLabel("m/z");
    msmsSpectrumChart.setRangeAxisLabel(unitFormat.format("Intensity", "a.u."));
    msmsSpectrumChart.setRangeAxisNumberFormatOverride(intensityFormat);
    msmsSpectrumChart.setShowCrosshair(false);
    msmsSpectrumChart.setDefaultRenderer(new ColoredXYBarRenderer(false));

    mobilogramChart.setDomainAxisNumberFormatOverride(intensityFormat);
    mobilogramChart.setDomainAxisLabel(unitFormat.format("Intensity", "a.u."));
    mobilogramChart.setRangeAxisNumberFormatOverride(mobilityFormat);
    mobilogramChart.getXYPlot().getDomainAxis().setInverted(true);
    mobilogramChart.getXYPlot().setShowCursorCrosshair(false, true);
    mobilogramChart.setLegendItemsVisible(false);
//    mobilogramChart.addDatasetChangeListener(l -> {
//      FxThread.runLater(() -> {
//        ChartLogicsFX.autoDomainAxis(mobilogramChart);
//        ChartLogicsFX.autoRangeAxis(mobilogramChart);
//        mobilogramChart.getXYPlot().getDomainAxis().setAutoRange(true);
//        mobilogramChart.getXYPlot().getRangeAxis().setAutoRange(true);
//        XYItemRenderer renderer = mobilogramChart.getXYPlot().getRenderer(0);
//        if (renderer != null) {
//          renderer.setDefaultSeriesVisibleInLegend(false);
//        }
//      });
//    });

    ChartGroup mobilityGroup = new ChartGroup(false, false, false, true);
    mobilityGroup.add(new ChartViewWrapper(mobilogramChart));
    mobilityGroup.add(new ChartViewWrapper(heatmapChart));
  }

  private void initChartPanes() {
//    Canvas legendCanvas = new Canvas();
//    heatmapChart.setLegendCanvas(legendCanvas);
//    legendCanvas.widthProperty().bind(heatmapChart.widthProperty());
//    legendCanvas.setHeight(60);

    add(new BorderPane(mobilogramChart), 0, 0);
    add(new BorderPane(heatmapChart), 1, 0);
//    add(new BorderPane(legendCanvas), 1, 1);

    fragmentScanSelection = new ComboBox<>();
    fragmentScanSelection.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue == null) {
        msmsSpectrumChart.removeAllDatasets();
        return;
      }

      msmsSpectrumChart.setDataset(new SingleSpectrumProvider(newValue));
    });
    fragmentScanSelection.setConverter(new StringConverter<Scan>() {
      @Override
      public String toString(Scan object) {
        if (object != null) {
          return ScanUtils.scanToString(object, true);
        }
        return null;
      }

      @Override
      public Scan fromString(String string) {
        return null;
      }
    });

    FlowPane controls = new FlowPane(new Label("Fragment spectrum: "));
    controls.setHgap(5);
    controls.getChildren().add(fragmentScanSelection);
    controls.setAlignment(Pos.TOP_CENTER);

    BorderPane spectrumPane = new BorderPane(msmsSpectrumChart);
    spectrumPane.setMinHeight(250);
    spectrumPane.setBottom(controls);
    add(spectrumPane, 0, 1, 2, 1);
  }

  public SimpleXYZScatterPlot<IonMobilogramTimeSeriesToRtMobilityHeatmapProvider> getHeatmapChart() {
    return heatmapChart;
  }

  public SimpleXYChart<SingleSpectrumProvider> getMsmsSpectrumChart() {
    return msmsSpectrumChart;
  }

  public SimpleXYChart<SummedMobilogramXYProvider> getMobilogramChart() {
    return mobilogramChart;
  }

  public MobilityScan getSelectedMobilityScan() {
    return selectedMobilityScan.get();
  }

  public void setSelectedMobilityScan(MobilityScan selectedMobilityScan) {
    this.selectedMobilityScan.set(selectedMobilityScan);
  }

  public ObjectProperty<MobilityScan> selectedMobilityScanProperty() {
    return selectedMobilityScan;
  }

  public void setFeature(ModularFeature feature) {
    this.feature.set(feature);
  }
}
