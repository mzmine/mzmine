/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.visualization.rawdataoverviewims;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYBarRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogramsmoothing.PreviewMobilogram;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.CachedFrame;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.FrameHeatmapProvider;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.FrameSummedMobilogramProvider;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.FrameSummedSpectrumProvider;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.IonTraceProvider;
import io.github.mzmine.modules.visualization.rawdataoverviewims.providers.SingleSpectrumProvider;
import io.github.mzmine.parameters.parametertypes.DoubleComponent;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeComponent;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.impl.StorableFrame;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Set;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.title.TextTitle;

public class IMSRawDataOverviewPane extends BorderPane {

  private final GridPane chartPanel;
  private final GridPane controlsPanel;
  private final SimpleXYChart<FrameSummedMobilogramProvider> mobilogramChart;
  private final SimpleXYChart<FrameSummedSpectrumProvider> summedSpectrumChart;
  private final SimpleXYChart<SingleSpectrumProvider> singleSpectrumChart;
  private final SimpleXYZScatterPlot<FrameHeatmapProvider> heatmapChart;
  private final SimpleXYZScatterPlot<IonTraceProvider> ionTraceChart;
  private final TICPlot ticChart;

  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat mobilityFormat;
  private final NumberFormat intensityFormat;
  private final UnitFormat unitFormat;

  private final MZTolerance mzTolerance;
  private final ObjectProperty<Frame> selectedFrame;
  private final ObjectProperty<MobilityScan> selectedMobilityScan;
  private final DoubleProperty selectedMz;
  private final Stroke markerStroke = new BasicStroke(1.0f);
  private ListView<Range<Double>> mobilogramRangesList;
  private CachedFrame cachedFrame;
  private Color markerColor;
  private IMSRawDataFile rawDataFile;
  private int selectedMobilogramDatasetIndex;
  private double frameNoiseLevel;
  private double mobilityScanNoiseLevel;

  /**
   * Creates a BorderPane layout.
   */
  public IMSRawDataOverviewPane() {
    this(0, 0);
  }

  public IMSRawDataOverviewPane(final double frameNoiseLevel, final double mobilityScanNoiseLevel) {
    super();
    chartPanel = new GridPane();
    controlsPanel = new GridPane();
    selectedMobilogramDatasetIndex = -1;
    this.frameNoiseLevel = frameNoiseLevel;
    this.mobilityScanNoiseLevel = mobilityScanNoiseLevel;

    ColumnConstraints constraints = new ColumnConstraints(300, Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY, Priority.ALWAYS, HPos.CENTER, true);
    RowConstraints rowConstraints = new RowConstraints(300, Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY, Priority.ALWAYS, VPos.CENTER, true);

    chartPanel.getColumnConstraints()
        .addAll(constraints, constraints, new ColumnConstraints(), constraints);
    chartPanel.getRowConstraints().addAll(rowConstraints, rowConstraints, rowConstraints);

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    setCenter(chartPanel);
    initControlPanel();

    selectedFrame = new SimpleObjectProperty<>();
    selectedFrame.addListener((observable, oldValue, newValue) -> onSelectedFrameChanged());

    mobilogramChart = new SimpleXYChart<>("Mobilogram chart");
    summedSpectrumChart = new SimpleXYChart<>("Summed frame spectrum");
    singleSpectrumChart = new SimpleXYChart<>("Mobility scan");
    heatmapChart = new SimpleXYZScatterPlot<>("Frame heatmap");
    ionTraceChart = new SimpleXYZScatterPlot<>("Ion trace chart");
    ticChart = new TICPlot();
    initCharts();

    updateAxisLabels();
    chartPanel.add(new BorderPane(mobilogramChart), 0, 0);
    chartPanel.add(new BorderPane(heatmapChart), 1, 0);
    chartPanel.add(new Separator(Orientation.VERTICAL), 2, 0, 1, 2);
    chartPanel.add(controlsPanel, 3, 0);
    chartPanel.add(new BorderPane(singleSpectrumChart), 0, 1);
    chartPanel.add(new BorderPane(summedSpectrumChart), 1, 1);
    chartPanel.add(new BorderPane(ticChart), 3, 1);
    chartPanel.add(new BorderPane(ionTraceChart), 0, 2, 4, 1);

    selectedMz = new SimpleDoubleProperty();
    selectedMobilityScan = new SimpleObjectProperty<>();
    mzTolerance = new MZTolerance(0.008, 10);
    markerColor = MZmineCore.getConfiguration().getDefaultColorPalette().getNeutralColorAWT();
    initChartListeners();
    initSelectedValueListeners();
  }

  private void onSelectedFrameChanged() {
    clearAllCharts();
    if (selectedFrame.get() == null) {
      return;
    }
    try {
      cachedFrame = new CachedFrame((StorableFrame) selectedFrame.get(), frameNoiseLevel,
          mobilityScanNoiseLevel);
      heatmapChart.setDataset(new FrameHeatmapProvider(cachedFrame));
      mobilogramChart.addDataset(new FrameSummedMobilogramProvider(cachedFrame));
      summedSpectrumChart.addDataset(new FrameSummedSpectrumProvider(cachedFrame));
      ticChart.getXYPlot().clearDomainMarkers();
      ticChart.getXYPlot().addDomainMarker(
          new ValueMarker(selectedFrame.get().getRetentionTime(), markerColor, markerStroke));
      for (Range<Double> mzRange : mobilogramRangesList.getItems()) {
        Thread mobilogramCalc =
            new Thread(new Threads.BuildMobilogram(mzRange, Set.of(cachedFrame), this, false));
        mobilogramCalc.start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void updateAxisLabels() {
    String intensityLabel = unitFormat.format("Intensity", "cps");
    String mzLabel = "m/z";
    String mobilityLabel = (rawDataFile != null) ?
        unitFormat.format("Mobility (" + rawDataFile.getMobilityType().getAxisLabel() + ")",
            rawDataFile.getMobilityType().getUnit()) : "Mobility";
    mobilogramChart.setDomainAxisLabel(mobilityLabel);
    mobilogramChart.setDomainAxisNumberFormatOverride(mobilityFormat);
    mobilogramChart.setRangeAxisLabel(intensityLabel);
    mobilogramChart.setRangeAxisNumberFormatOverride(intensityFormat);
    summedSpectrumChart.setDomainAxisLabel(mzLabel);
    summedSpectrumChart.setDomainAxisNumberFormatOverride(mzFormat);
    summedSpectrumChart.setRangeAxisLabel(intensityLabel);
    summedSpectrumChart.setRangeAxisNumberFormatOverride(intensityFormat);
    singleSpectrumChart.setDomainAxisLabel(mzLabel);
    singleSpectrumChart.setDomainAxisNumberFormatOverride(mzFormat);
    singleSpectrumChart.setRangeAxisLabel(intensityLabel);
    singleSpectrumChart.setRangeAxisNumberFormatOverride(intensityFormat);
    heatmapChart.setDomainAxisLabel(mzLabel);
    heatmapChart.setDomainAxisNumberFormatOverride(mzFormat);
    heatmapChart.setRangeAxisLabel(mobilityLabel);
    heatmapChart.setRangeAxisNumberFormatOverride(mobilityFormat);
    ionTraceChart.setDomainAxisLabel(unitFormat.format("Retention time", "min"));
    ionTraceChart.setRangeAxisLabel(mobilityLabel);
  }

  private void initCharts() {
    final ColoredXYBarRenderer summedSpectrumRenderer = new ColoredXYBarRenderer(false);
    summedSpectrumRenderer
        .setDefaultItemLabelGenerator(summedSpectrumChart.getXYPlot().getRenderer()
            .getDefaultItemLabelGenerator());
    summedSpectrumRenderer.setDefaultToolTipGenerator(summedSpectrumChart.getXYPlot().getRenderer()
        .getDefaultToolTipGenerator());
    summedSpectrumChart.setDefaultRenderer(summedSpectrumRenderer);
    summedSpectrumChart.setShowCrosshair(false);

    mobilogramChart.getXYPlot().setOrientation(PlotOrientation.HORIZONTAL);
    mobilogramChart.getXYPlot().getRangeAxis().setInverted(true);
    mobilogramChart.setShowCrosshair(false);

    final ColoredXYBarRenderer singleSpectrumRenderer = new ColoredXYBarRenderer(false);
    singleSpectrumRenderer
        .setDefaultItemLabelGenerator(singleSpectrumChart.getXYPlot().getRenderer()
            .getDefaultItemLabelGenerator());
    singleSpectrumRenderer.setDefaultToolTipGenerator(singleSpectrumChart.getXYPlot().getRenderer()
        .getDefaultToolTipGenerator());
    singleSpectrumChart.setDefaultRenderer(singleSpectrumRenderer);
    singleSpectrumChart.setShowCrosshair(false);

    ionTraceChart.setShowCrosshair(false);
    heatmapChart.setShowCrosshair(false);
    ticChart.getXYPlot().setDomainCrosshairVisible(false);
    ticChart.getXYPlot().setRangeCrosshairVisible(false);
    ticChart.switchDataPointsVisible();
  }

  private void initControlPanel() {
    DoubleComponent frameNoiseLevelComponent = new DoubleComponent(100, 0d, Double.MAX_VALUE,
        intensityFormat, frameNoiseLevel);
    frameNoiseLevelComponent.setText(intensityFormat.format(frameNoiseLevel));
    DoubleComponent mobilityScanNoiseLevelComponent = new DoubleComponent(100, 0d, Double.MAX_VALUE,
        intensityFormat, mobilityScanNoiseLevel);
    mobilityScanNoiseLevelComponent.setText(intensityFormat.format(mobilityScanNoiseLevel));

    controlsPanel.setPadding(new Insets(5));
    controlsPanel.setVgap(5);
    controlsPanel.getColumnConstraints().addAll(new ColumnConstraints(150),
        new ColumnConstraints());
    controlsPanel.add(new Label("Mobility scan noise level"), 0, 0);
    controlsPanel.add(mobilityScanNoiseLevelComponent, 1, 0);
    controlsPanel.add(new Label("Frame noise level"), 0, 1);
    controlsPanel.add(frameNoiseLevelComponent, 1, 1);

    DoubleRangeComponent mobilogramRangeComp = new DoubleRangeComponent(mzFormat);
    mobilogramRangesList = new ListView<>(
        FXCollections.observableArrayList());
    mobilogramRangesList.setMaxHeight(150);
    mobilogramRangesList.setMaxWidth(240);
    mobilogramRangesList.setPrefWidth(240);
    mobilogramRangesList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    Button addMobilogramRange = new Button("Add mobilogram");
    addMobilogramRange.setOnAction(e -> {
      Range<Double> range = mobilogramRangeComp.getValue();
      if (range == null) {
        return;
      }
      mobilogramRangesList.getItems().add(range);
    });

    Button removeMobilogramRange = new Button("Remove mobilogram");
    removeMobilogramRange.setOnAction(e -> mobilogramRangesList.getItems()
        .remove(mobilogramRangesList.getSelectionModel().getSelectedItem()));
    mobilogramRangesList.setCellFactory(param -> new ListCell<>() {
      @Override
      protected void updateItem(Range<Double> item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setGraphic(null);
          return;
        }
        setText(mzFormat.format(item.lowerEndpoint()) + " - " + mzFormat
            .format(item.upperEndpoint()));
        setGraphic(null);
      }
    });

    Button update = new Button("Update");
    update.setOnAction(e -> {
      try {
        frameNoiseLevel =
            Double.parseDouble(frameNoiseLevelComponent.getText());
        mobilityScanNoiseLevel =
            Double.parseDouble(mobilityScanNoiseLevelComponent.getText());
        frameNoiseLevelComponent.setText(intensityFormat.format(frameNoiseLevel));
        mobilityScanNoiseLevelComponent.setText(intensityFormat.format(mobilityScanNoiseLevel));
        onSelectedFrameChanged();

      } catch (NullPointerException | NumberFormatException ex) {
        ex.printStackTrace();
      }
    });

//    controlsPanel.add(new Separator(Orientation.HORIZONTAL), 0, 2, 2, 1);
    controlsPanel.add(mobilogramRangesList, 1, 3, 1, 1);
    controlsPanel.add(new Label("Range:"), 0, 4);
    controlsPanel.add(mobilogramRangeComp, 1, 4);
    FlowPane buttons = new FlowPane(addMobilogramRange, removeMobilogramRange, update);
    buttons.setHgap(5);
    buttons.setAlignment(Pos.CENTER);
    controlsPanel.add(buttons, 0, 5, 2, 1);
  }

  private void initChartListeners() {
    mobilogramChart.cursorPositionProperty().addListener(((observable, oldValue,
        newValue) -> selectedMobilityScan
        .set(cachedFrame.getSortedMobilityScans().get(newValue.getValueIndex()))));
    singleSpectrumChart.cursorPositionProperty().addListener(((observable, oldValue, newValue) ->
        selectedMz.set(newValue.getDomainValue())));
    summedSpectrumChart.cursorPositionProperty().addListener(((observable, oldValue, newValue) ->
        selectedMz.set(newValue.getDomainValue())));
    heatmapChart.cursorPositionProperty().addListener(
        ((observable, oldValue, newValue) -> selectedMz.set(newValue.getDomainValue())));
    ticChart.cursorPositionProperty()
        .addListener(((observable, oldValue, newValue) -> setSelectedFrame(rawDataFile.getFrame(
            newValue.getScanNumber()))));
  }

  private void initSelectedValueListeners() {
    selectedMobilityScan.addListener(((observable, oldValue, newValue) -> {
      singleSpectrumChart.removeAllDatasets();
      singleSpectrumChart.addDataset(new SingleSpectrumProvider(selectedMobilityScan.get()));
      updateValueMarkers();
    }));

    selectedMz.addListener(((observable, oldValue, newValue) -> {
      if (selectedMobilogramDatasetIndex != -1) {
        mobilogramChart.removeDataSet(selectedMobilogramDatasetIndex);
      }
      Thread mobilogramCalc =
          new Thread(new Threads.BuildMobilogram(mzTolerance.getToleranceRange(selectedMz.get()),
              Set.of(cachedFrame), this, true));
      mobilogramCalc.start();
      ionTraceChart.setDataset(new IonTraceProvider(rawDataFile,
          mzTolerance.getToleranceRange(selectedMz.get()), rawDataFile.getDataRTRange(1),
          mobilityScanNoiseLevel));
      updateValueMarkers();
    }));
  }

  public int addMobilogramToChart(PreviewMobilogram previewMobilogram,
      boolean isSelectedMobilogram) {
    ColoredXYDataset dataset = new ColoredXYDataset(previewMobilogram);
    if (isSelectedMobilogram) {
      dataset
          .setColor(MZmineCore.getConfiguration().getDefaultColorPalette().getPositiveColorAWT());
    } else {
      dataset.setColor(MZmineCore.getConfiguration().getDefaultColorPalette().getNextColorAWT());
    }
    int datasetIndex = mobilogramChart.addDataset(dataset);
    if (isSelectedMobilogram) {
      selectedMobilogramDatasetIndex = datasetIndex;
    }
    updateValueMarkers();
    return datasetIndex;
  }

  private void updateValueMarkers() {
    if (selectedMobilityScan.get() != null) {
      mobilogramChart.getXYPlot().clearDomainMarkers();
      mobilogramChart.getXYPlot()
          .addDomainMarker(new ValueMarker(selectedMobilityScan.getValue().getMobility(),
              markerColor, markerStroke));
      heatmapChart.getXYPlot().clearRangeMarkers();
      heatmapChart.getXYPlot().addRangeMarker(
          new ValueMarker(selectedMobilityScan.getValue().getMobility(), markerColor,
              markerStroke));
    }
    if (selectedMz.getValue() != null) {
      summedSpectrumChart.getXYPlot().clearDomainMarkers();
      summedSpectrumChart.getXYPlot().addDomainMarker(new ValueMarker(selectedMz.get(),
          markerColor, markerStroke));
      singleSpectrumChart.getXYPlot().clearDomainMarkers();
      singleSpectrumChart.getXYPlot().addDomainMarker(new ValueMarker(selectedMz.get(),
          markerColor, markerStroke));
      heatmapChart.getXYPlot().clearDomainMarkers();
      heatmapChart.getXYPlot()
          .addDomainMarker(new ValueMarker(selectedMz.get(), markerColor, markerStroke));
    }
  }

  private void clearAllCharts() {
    mobilogramChart.removeAllDatasets();
    summedSpectrumChart.removeAllDatasets();
    heatmapChart.removeAllDatasets();
    singleSpectrumChart.removeAllDatasets();
  }

  public Frame getSelectedFrame() {
    return selectedFrame.get();
  }

  public void setSelectedFrame(Frame frame) {
    this.selectedFrame.set(frame);
  }

  public void setSelectedFrame(int frameId) {
    Frame frame = rawDataFile.getFrame(frameId);
    if (frame != null) {
      setSelectedFrame(frame);
    }
  }

  public ObjectProperty<Frame> selectedFrameProperty() {
    return selectedFrame;
  }

  public void setRawDataFile(RawDataFile rawDataFile) {
    if (!(rawDataFile instanceof IMSRawDataFile)) {
      return;
    }
    this.rawDataFile = (IMSRawDataFile) rawDataFile;
    ticChart.removeAllDataSets();
    ScanSelection selection = new ScanSelection(1);
    TICDataSet dataSet = new TICDataSet(rawDataFile, selection.getMatchingScans(rawDataFile),
        rawDataFile.getDataMZRange(), null);
    ticChart.addTICDataSet(dataSet, rawDataFile.getColorAWT());
    ticChart.getChart().setTitle(new TextTitle("TIC - " + rawDataFile.getName()));
    updateAxisLabels();
    setSelectedFrame(1);
  }
}
