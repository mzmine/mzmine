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
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.gui.chartbasics.listener.RegionSelectionListener;
import io.github.mzmine.gui.chartbasics.simplechart.RegionSelectionWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZPieDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.MassSpectrumProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PieXYZDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYZValueProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.ScanBPCProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.RowToCCSMzHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.RowToMobilityMzHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedIntensityMobilitySeriesToMobilityMzHeatmapProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYZPieRenderer;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.filter_mobilitymzregionextraction.MobilityMzRegionExtractionModule;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.ValueMarker;

/**
 * @author https://github.com/SteffenHeu
 */
public class IMSMobilityMzPlot extends BorderPane {

  private final SimpleXYChart<IonTimeSeriesToXYProvider> ticChart;
  private final Map<ModularFeature, SingleIMSFeatureVisualiserPane> featureVisualisersMap;
  private final SimpleXYZScatterPlot<RowToMobilityMzHeatmapProvider> heatmap;
  private final RegionSelectionWrapper<SimpleXYZScatterPlot<?>> selectionWrapper;
  private final ScrollPane scrollPane;
  private final VBox content;

  private final NumberFormat rtFormat;
  private final NumberFormat mzFormat;
  private final NumberFormat mobilityFormat;
  private final NumberFormat intensityFormat;
  private final NumberFormat ccsFormat;
  private final UnitFormat unitFormat;

  private final Stroke markerStroke = new BasicStroke(1f);
  private final Paint markerColor = MZmineCore.getConfiguration().getDefaultColorPalette()
      .getPositiveColorAWT();

  private final ObjectProperty<Scan> selectedScan;
  private final BooleanProperty useCCS;
  private final List<RawDataFile> rawDataFiles; // raw data files in the ticChart
  private boolean isCtrlPressed = false;

  private boolean useMobilograms = false;

  private Collection<ModularFeatureListRow> features;

  public IMSMobilityMzPlot() {
    super();
    getStylesheets().addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    getStyleClass().add(".region-match-chart-bg");

    ticChart = new SimpleXYChart<>();
    heatmap = new SimpleXYZScatterPlot<>();
    featureVisualisersMap = new LinkedHashMap<>();
    rawDataFiles = new ArrayList<>();
    useCCS = new SimpleBooleanProperty();

    rtFormat = MZmineCore.getConfiguration().getRTFormat();
    mzFormat = MZmineCore.getConfiguration().getMZFormat();
    mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    ccsFormat = MZmineCore.getConfiguration().getCCSFormat();

    initCharts();

    selectedScan = new SimpleObjectProperty<>();
    selectedScan.addListener((observable, oldValue, newValue) -> updateValueMarkers(newValue));

    scrollPane = new ScrollPane();
    scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
    content = new VBox();
    content.maxWidthProperty().bind(scrollPane.widthProperty().subtract(10));
    content.minWidthProperty().bind(scrollPane.widthProperty().subtract(10));
    scrollPane.setContent(content);

    BorderPane selectedFeaturesPane = new BorderPane();
    selectedFeaturesPane.setTop(ticChart);
    selectedFeaturesPane.setCenter(scrollPane);
    selectedFeaturesPane.setMinWidth(500);
    selectionWrapper = new RegionSelectionWrapper<>(heatmap);
    setRight(selectedFeaturesPane);
    setCenter(selectionWrapper);

    initChartListeners();
    initSelectionPane();
  }

  private void initCharts() {
    heatmap.setDomainAxisLabel("m/z");
    heatmap.setDomainAxisNumberFormatOverride(mzFormat);
    heatmap.setRangeAxisLabel("Mobility");
    heatmap.setRangeAxisNumberFormatOverride(mobilityFormat);
    heatmap.setLegendAxisLabel(unitFormat.format("Intensity", "a.u."));
    heatmap.setLegendNumberFormatOverride(intensityFormat);
    heatmap.setDefaultRenderer(new ColoredXYZPieRenderer());
    heatmap.getXYPlot()
        .setBackgroundPaint(MZmineCore.getConfiguration().isDarkMode() ? Color.BLACK : Color.WHITE);
    heatmap.getXYPlot().setDomainCrosshairPaint(Color.LIGHT_GRAY);
    heatmap.getXYPlot().setRangeCrosshairPaint(Color.LIGHT_GRAY);
    ticChart.setDomainAxisLabel(unitFormat.format("Retention time", "min"));
    ticChart.setDomainAxisNumberFormatOverride(rtFormat);
    ticChart.setRangeAxisNumberFormatOverride(intensityFormat);
    ticChart.setRangeAxisLabel(unitFormat.format("Intensity", "counts"));
    ticChart.setShowCrosshair(false);
    ticChart.getChart().setTitle("Extracted ion chromatograms");
    ticChart.setMinHeight(250);
  }

  private void initSelectionPane() {
    final Button btnExtractRegions = new Button("Extract regions");
    final TextField tfSuffix = new TextField();
    tfSuffix.setPromptText("suffix");

    final GridPane selectionControls = selectionWrapper.getSelectionControls();

    btnExtractRegions.setDisable(true);
    btnExtractRegions
        .setOnAction(e -> Platform.runLater(() -> {
          List<List<Point2D>> regions = selectionWrapper.getFinishedRegionsAsListOfPointLists();
          MobilityMzRegionExtractionModule.runExtractionForFeatureList(
              features.stream().findFirst().get().getFeatureList(), regions,
              Objects.requireNonNullElse(tfSuffix.getText(), "extracted"),
              useCCS.get() ? PlotType.CCS : PlotType.MOBILITY);
        }));

    selectionWrapper.getFinishedRegionListeners()
        .addListener((ListChangeListener<RegionSelectionListener>) c -> {
          c.next();
          btnExtractRegions.setDisable(c.getList().isEmpty());
        });

    selectionControls.add(btnExtractRegions, 4, 0);
    selectionControls.add(tfSuffix, 5, 0);

    final ComboBox<PlotType> cbPlotType = new ComboBox<>(
        FXCollections.observableList(List.of(PlotType.values())));
    cbPlotType.setValue(useCCS.get() ? PlotType.CCS : PlotType.MOBILITY);
    cbPlotType.valueProperty()
        .addListener((observable, oldValue, newValue) -> useCCS.set(newValue == PlotType.CCS));
    selectionControls.add(cbPlotType, 6, 0);

    useCCS.addListener((observable, oldValue, newValue) -> {
      if (features != null && !features.isEmpty()) {
        heatmap.removeAllDatasets();
        setFeatures(features, cbPlotType.getValue(), useMobilograms);
      }
    });
  }

  public void setFeatures(@Nullable Collection<ModularFeatureListRow> features, PlotType plotType,
      boolean useMobilograms) {
    assert Platform.isFxApplicationThread();
    this.useMobilograms = useMobilograms;

    featureVisualisersMap.clear();
    content.getChildren().clear();
    ticChart.removeAllDatasets();

    if (features == null || features.isEmpty()) {
      this.features = Collections.emptyList();
      return;
    }
    this.features = features;

    boolean anyNonIms = features.stream().flatMap(row -> row.getRawDataFiles().stream()).distinct()
        .anyMatch(f -> !(f instanceof IMSRawDataFile));
    if (anyNonIms) {
      throw new IllegalArgumentException(
          "Cannot visualize non-ion mobility spectrometry files in an IMS visualizer");
    }
    final IMSRawDataFile file = (IMSRawDataFile) features.iterator().next().getBestFeature()
        .getRawDataFile();

    if ((plotType == PlotType.MOBILITY)) {
      heatmap.setRangeAxisLabel(file.getMobilityType().getAxisLabel());
      heatmap.setRangeAxisNumberFormatOverride(mobilityFormat);
      heatmap.setDomainAxisLabel("m/z");

      /*if (numFiles == 1) {
        Set<? extends Scan> frames = features.stream()
            .flatMap(row -> row.getBestFeature().getScanNumbers().stream())
            .collect(Collectors.toSet());
        final ColoredXYZDataset frameDataSet = new ColoredXYZDataset(
            new MergedFrameHeatmapProvider((Collection<Frame>) frames, new MZTolerance(0.008, 13),
                5));
        frameDataSet.statusProperty().addListener((observable, oldValue, newValue) -> {
          if(newValue == TaskStatus.FINISHED) {
            ColoredXYSmallBlockRenderer renderer = new ColoredXYSmallBlockRenderer();
            renderer.setBlockHeight(frameDataSet.getBoxHeight());
            renderer.setBlockWidth(frameDataSet.getBoxWidth());
            renderer.setPaintScale(frameDataSet.getPaintScale());
            Platform.runLater(() -> heatmap.addDataset(frameDataSet, renderer));
          }
        });
      }*/
    } else {
      heatmap.setRangeAxisLabel(new CCSType().getHeaderString());
      heatmap.setRangeAxisNumberFormatOverride(ccsFormat);
      heatmap.setDomainAxisLabel("Da");
    }

    final CalculateDatasetsTask calc = new CalculateDatasetsTask(features, plotType,
        useMobilograms);
    MZmineCore.getTaskController().addTask(calc);
    calc.addTaskStatusListener(((task, newStatus, oldStatus) -> {
      if (newStatus == TaskStatus.FINISHED) {
        MZmineCore.runLater(() ->{
          heatmap.addDatasetsAndRenderers(calc.getDatasetsRenderers());
          if(calc.getDatasetsRenderers().size() > 1) {
            heatmap.setLegendPaintScale(calc.getPaintScale());
          }
          heatmap.updateLegend();
        });
      }
    }));
  }

  public void addFeatureToRightSide(ModularFeature feature) {
    if (!rawDataFiles.contains(feature.getRawDataFile())) {
      ticChart.addDataset(new ColoredXYDataset(
          new ScanBPCProvider(feature.getRawDataFile().getScanNumbers(1))));
      rawDataFiles.add(feature.getRawDataFile());
    }
    ticChart.addDataset(new IonTimeSeriesToXYProvider(feature.getFeatureData(),
        FeatureUtils.featureToString(feature),
        new SimpleObjectProperty<>(
            MZmineCore.getConfiguration().getDefaultColorPalette().getNextColor())));

    SingleIMSFeatureVisualiserPane featureVisualiserPane = new SingleIMSFeatureVisualiserPane(
        feature);

    featureVisualiserPane.getHeatmapChart().cursorPositionProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (newValue.getDataset() instanceof ColoredXYDataset dataset) {
            if (dataset.getValueProvider() instanceof MassSpectrumProvider<?> spectrumProvider) {
              MassSpectrum spectrum = spectrumProvider.getSpectrum(newValue.getValueIndex());
              if (spectrum instanceof Scan) {
                selectedScan.set((Scan) spectrum);
              }
            }
          }
        });

    featureVisualiserPane.selectedMobilityScanProperty()
        .addListener((observable, oldValue, newValue) -> selectedScan.set(newValue.getFrame()));
    featureVisualisersMap.put(feature, featureVisualiserPane);
    content.getChildren().add(featureVisualiserPane);
    content.getChildren().add(new Separator(Orientation.HORIZONTAL));
  }

  private void updateValueMarkers(Scan newValue) {
    ticChart.getXYPlot().clearDomainMarkers();
    final ValueMarker newMarker = new ValueMarker(newValue.getRetentionTime(), markerColor,
        markerStroke);
    ticChart.getXYPlot().addDomainMarker(newMarker);
    featureVisualisersMap.values().forEach(vis -> {
      vis.getHeatmapChart().getXYPlot().clearDomainMarkers();
      vis.getHeatmapChart().getXYPlot().addDomainMarker(newMarker);
    });
  }

  private void initChartListeners() {
    heatmap.cursorPositionProperty().addListener(((observable, oldValue, newValue) -> {
      if (newValue.getDataset() instanceof ColoredXYZPieDataset<?> ds) {
        final PieXYZDataProvider<?> prov = ds.getPieDataProvider();
        // if the mz + mobility of a feature was used to generate the plot
        if (prov instanceof RowToMobilityMzHeatmapProvider) {
          final ModularFeatureListRow row = ((RowToMobilityMzHeatmapProvider) prov).getSourceRows()
              .get(newValue.getValueIndex());
          if (row != null && row.getBestFeature() != null) {
            if (!isCtrlPressed) {
              clearRightSide();
            }
            var f = row.getBestFeature();
            addFeatureToRightSide(f);
          }
        }
        // if the mz + css of a feature was used to generate the plot
        else if (prov instanceof RowToCCSMzHeatmapProvider) {
          ModularFeatureListRow row = ((RowToCCSMzHeatmapProvider) prov).getSourceRows().get(
              newValue.getValueIndex());
          if (row != null && row.getBestFeature() != null) {
            if (!isCtrlPressed) {
              clearRightSide();
            }
            addFeatureToRightSide(row.getBestFeature());
          }
        }
      }
      // if mobilograms were used to generate the plot
      else if (newValue.getDataset() instanceof ColoredXYDataset) {
        XYZValueProvider prov = ((ColoredXYZDataset) newValue.getDataset()).getXyzValueProvider();
        // if mobilograms were shown
        if (prov instanceof SummedIntensityMobilitySeriesToMobilityMzHeatmapProvider) {
          ModularFeature f = ((SummedIntensityMobilitySeriesToMobilityMzHeatmapProvider) prov)
              .getSourceFeature();
          if (f != null) {
            if (!isCtrlPressed) {
              clearRightSide();
            }
            addFeatureToRightSide(f);
          }
        }
      }
    }));

    ticChart.cursorPositionProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue.getDataset() instanceof ColoredXYDataset dataset) {
        if (dataset.getValueProvider() instanceof MassSpectrumProvider spectrumProvider) {
          MassSpectrum spectrum = spectrumProvider.getSpectrum(newValue.getValueIndex());
          if (spectrum instanceof Scan) {
            selectedScan.set((Scan) spectrum);
          }
        }
      }
    });
  }

  private void clearRightSide() {
    content.getChildren().clear();
    featureVisualisersMap.clear();
    ticChart.removeAllDatasets();
    rawDataFiles.clear();
  }

}
