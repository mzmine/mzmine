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

package io.github.mzmine.modules.visualization.ims_mobilitymzplot;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
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
import javafx.beans.property.SimpleObjectProperty;
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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.XYItemRenderer;
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

  private final ModularFeature feature;
  private final SimpleObjectProperty<MobilityScan> selectedMobilityScan;

  public SingleIMSFeatureVisualiserPane(ModularFeature f) {
    super();

    getStylesheets().addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    this.feature = f;
    String fstr = FeatureUtils.featureToString(f);
    this.heatmapChart = new SimpleXYZScatterPlot<>("Ion trace - " + fstr);
    this.msmsSpectrumChart = new SimpleXYChart<>("MS/MS - " + fstr);
    this.mobilogramChart = new SimpleXYChart<>("Extracted mobilogram");

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    selectedMobilityScan = new SimpleObjectProperty<>();

    initCharts();
    heatmapChart.setDataset(new IonMobilogramTimeSeriesToRtMobilityHeatmapProvider(feature));
    mobilogramChart.addDataset(new SummedMobilogramXYProvider(feature, true));

    Scan msmsSpectrum = feature.getMostIntenseFragmentScan();
    if (msmsSpectrum != null) {
      msmsSpectrumChart.addDataset(new SingleSpectrumProvider(msmsSpectrum));
    }

    initChartPanes();

    ColumnConstraints col0 = new ColumnConstraints(150);
    ColumnConstraints col1 = new ColumnConstraints(340);

    RowConstraints row0 = new RowConstraints(250);
    RowConstraints row3 = new RowConstraints(250);

    getRowConstraints().addAll(row0, row3);
    getColumnConstraints().addAll(col0, col1);
  }

  private void initCharts() {
    final MobilityType mobilityType = ((IMSRawDataFile) feature.getRawDataFile()).getMobilityType();

    heatmapChart.setDomainAxisLabel(unitFormat.format("Retention time", "min"));
    heatmapChart.setDomainAxisNumberFormatOverride(rtFormat);
    heatmapChart.setRangeAxisLabel(mobilityType.getAxisLabel());
    heatmapChart.setRangeAxisNumberFormatOverride(mobilityFormat);
    heatmapChart.setLegendAxisLabel(unitFormat.format("Intensity", "counts"));
    heatmapChart.setLegendNumberFormatOverride(intensityFormat);
    heatmapChart.setDefaultPaintscaleLocation(RectangleEdge.RIGHT);
    heatmapChart.getXYPlot().setBackgroundPaint(Color.BLACK);
    heatmapChart.setShowCrosshair(false);
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
      heatmapChart.getXYPlot().getDomainAxis().setRange(RangeUtils.guavaToJFree(
          RangeUtils.getPositiveRange(((ColoredXYDataset) e.getDataset()).getDomainValueRange(),
              0.0001d)), false, true);
      heatmapChart.getXYPlot().getRangeAxis().setRange(RangeUtils.guavaToJFree(
          RangeUtils.getPositiveRange(((ColoredXYDataset) e.getDataset()).getRangeValueRange(),
              0.0001d)), false, true);
    });
    NumberAxis axis = (NumberAxis) heatmapChart.getXYPlot().getRangeAxis();
    axis.setAutoRange(true);
    axis.setAutoRangeIncludesZero(false);
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeMinimumSize(0.005);
    axis.setVisible(false);

    msmsSpectrumChart.setDomainAxisNumberFormatOverride(mzFormat);
    msmsSpectrumChart.setDomainAxisLabel("m/z");
    msmsSpectrumChart.setRangeAxisLabel(unitFormat.format("Intensity", "a.u."));
    msmsSpectrumChart.setRangeAxisNumberFormatOverride(intensityFormat);
    msmsSpectrumChart.setShowCrosshair(false);
    msmsSpectrumChart.setDefaultRenderer(new ColoredXYBarRenderer(false));

    mobilogramChart.setDomainAxisNumberFormatOverride(intensityFormat);
    mobilogramChart.setDomainAxisLabel(unitFormat.format("Intensity", "a.u."));
    mobilogramChart.setRangeAxisLabel(mobilityType.getAxisLabel());
    mobilogramChart.setRangeAxisNumberFormatOverride(mobilityFormat);
    mobilogramChart.getXYPlot().getDomainAxis().setInverted(true);
    mobilogramChart.setShowCrosshair(false);
    mobilogramChart.setLegendItemsVisible(false);
    mobilogramChart.addDatasetChangeListener(l -> {
      MZmineCore.runLater(() -> {
        NumberAxis a = (NumberAxis) heatmapChart.getXYPlot().getRangeAxis();
        a.setAutoRangeIncludesZero(false);
        a.setAutoRangeStickyZero(false);
        a.setAutoRangeMinimumSize(0.0001);
        a.setAutoRange(true);
        XYItemRenderer renderer = mobilogramChart.getXYPlot().getRenderer(0);
        if (renderer != null) {
          renderer.setDefaultSeriesVisibleInLegend(false);
        }
      });
    });

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

    ComboBox<Scan> fragmentScanSelection = new ComboBox<>();
    fragmentScanSelection.setItems(FXCollections.observableList(feature.getAllMS2FragmentScans()));
    if (feature.getAllMS2FragmentScans() != null && feature.getMostIntenseFragmentScan() != null) {
      fragmentScanSelection.setValue(feature.getMostIntenseFragmentScan());
    }
    fragmentScanSelection.valueProperty().addListener((observable, oldValue, newValue) -> {
      msmsSpectrumChart.removeAllDatasets();
      msmsSpectrumChart.addDataset(new SingleSpectrumProvider(newValue));
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

  public SimpleObjectProperty<MobilityScan> selectedMobilityScanProperty() {
    return selectedMobilityScan;
  }
}
