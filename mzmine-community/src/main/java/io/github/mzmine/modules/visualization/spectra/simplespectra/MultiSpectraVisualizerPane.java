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

package io.github.mzmine.modules.visualization.spectra.simplespectra;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.features.FeatureRawMobilogramProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.SummedMobilogramXYProvider;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.chromatogram.TICVisualizerTab;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.color.ColorUtils;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.BasicStroke;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;

/**
 * Window to show all MS/MS scans of a feature list row
 *
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class MultiSpectraVisualizerPane extends BorderPane {

  private static final long serialVersionUID = 1L;
  private final NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
  private final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private final NumberFormat mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
  private final NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
  private final UnitFormat unitFormat = MZmineCore.getConfiguration().getUnitFormat();
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final GridPane pnGrid;
  private final Label lbRaw;
  private List<RawDataFile> rawFiles;
  private FeatureListRow row;
  private RawDataFile activeRaw;

  /**
   * Shows best fragmentation scan raw data file first
   *
   * @param row
   */
  public MultiSpectraVisualizerPane(FeatureListRow row) {
    this(row, row.getMostIntenseFragmentScan().getDataFile());
  }

  public MultiSpectraVisualizerPane(FeatureListRow row, RawDataFile raw) {
    getStyleClass().add("region-match-chart-bg");

//    setExtendedState(JFrame.MAXIMIZED_BOTH);
    setMinSize(800, 600);

    pnGrid = new GridPane();
    var colCon = new ColumnConstraints();
    colCon.setFillWidth(true);
    pnGrid.getColumnConstraints().add(colCon);
    // any number of rows
//    pnGrid.setLayout(new GridLayout(0, 1, 0, 25));
//    pnGrid.setAutoscrolls(true);
    pnGrid.setVgap(25);

    ScrollPane scrollPane = new ScrollPane(pnGrid);
    scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    scrollPane.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
    setCenter(scrollPane);

    FlowPane pnMenu = new FlowPane();
    pnMenu.setAlignment(Pos.TOP_LEFT);
    pnMenu.setVgap(0);
    setTop(pnMenu);

    Button nextRaw = new Button("next");
    nextRaw.setOnAction(e -> nextRaw());
    Button prevRaw = new Button("prev");
    prevRaw.setOnAction(e -> prevRaw());
    pnMenu.getChildren().addAll(prevRaw, nextRaw);

    lbRaw = new Label();
    pnMenu.getChildren().add(lbRaw);

    Label lbRawTotalWithFragmentation = new Label();
    pnMenu.getChildren().add(lbRawTotalWithFragmentation);

    int n = 0;
    for (Feature f : row.getFeatures()) {
      if (f.getMostIntenseFragmentScan() != null) {
        n++;
      }
    }
    lbRawTotalWithFragmentation.setText("(total raw:" + n + ")");

    // add charts
    setData(row, raw);

//    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setVisible(true);
  }

  /**
   * next raw file with peak and MSMS
   */
  private void nextRaw() {
    logger.log(Level.INFO, "All MS/MS scans window: next raw file");
    int n = indexOfRaw(activeRaw) + 1;
    while (!setRawFileAndShow(rawFiles.get(n)) && n + 1 < rawFiles.size()) {
      n++;
    }
  }

  /**
   * Previous raw file with peak and MSMS
   */
  private void prevRaw() {
    logger.log(Level.INFO, "All MS/MS scans window: previous raw file");
    int n = indexOfRaw(activeRaw) - 1;
    while (!setRawFileAndShow(rawFiles.get(n)) && n - 1 >= 0) {
      n--;
    }
  }

  /**
   * Set data and create charts
   *
   * @param row
   * @param raw
   */
  public void setData(FeatureListRow row, RawDataFile raw) {
    rawFiles = row.getRawDataFiles();
    this.row = row;

    if(row.getFeature(raw) != null) {
      setRawFileAndShow(raw);
    } else {
      setRawFileAndShow(row.getBestFeature().getRawDataFile());
    }
  }

  /**
   * Set the raw data file and create all chromatograms and MS2 spectra
   *
   * @param raw
   * @return true if row has peak with MS2 spectrum in RawDataFile raw
   */
  public boolean setRawFileAndShow(RawDataFile raw) {
    Feature peak = row.getFeature(raw);
    if(peak == null && row.getRawDataFiles().size() == 1 && row.getBestFeature() != null) {
      peak = row.getBestFeature();
    }
    // no peak / no ms2 - return false
    if (peak == null || peak.getAllMS2FragmentScans() == null
        || peak.getAllMS2FragmentScans().size() == 0) {
      return false;
    }

    this.activeRaw = raw;
    // clear
    pnGrid.getChildren().clear();

    List<Scan> numbers = peak.getAllMS2FragmentScans();
    int i = 0;
    for (Scan scan : numbers) {
      BorderPane pn = addSpectra(scan);
      pn.minWidthProperty().bind(widthProperty().subtract(30));
      pnGrid.add(pn, 0, i++);
    }

    int n = indexOfRaw(raw);
    lbRaw.setText(n + ": " + raw.getName());
    logger.finest(
        "All MS/MS scans window: Added " + numbers.size() + " spectra of raw file " + n + ": "
            + raw.getName());
    // show
//    pnGrid.revalidate();
//    pnGrid.repaint();
    return true;
  }

  private int indexOfRaw(RawDataFile raw) {
    return rawFiles.indexOf(raw);
  }

  private BorderPane addSpectra(Scan scan) {
    BorderPane panel = new BorderPane();
    panel.setPrefHeight(600);
    // Split pane for eic plot (top) and spectrum (bottom)
    SplitPane bottomPane = new SplitPane();
    bottomPane.setOrientation(Orientation.VERTICAL);

    // Create EIC plot
    // labels for TIC visualizer
    Map<Feature, String> labelsMap = new HashMap<Feature, String>(0);

    ModularFeature peak = (ModularFeature) row.getFeature(activeRaw);

    // scan selection
    ScanSelection scanSelection = new ScanSelection(1, activeRaw.getDataRTRange(1));

    // mz range
    Range<Double> mzRange = null;
    mzRange = peak.getRawDataPointsMZRange();
    // optimize output by extending the range
    double upper = mzRange.upperEndpoint();
    double lower = mzRange.lowerEndpoint();
    double fiveppm = (upper * 5E-6);
    mzRange = Range.closed(lower - fiveppm, upper + fiveppm);

    // labels
    labelsMap.put(peak, peak.toString());

    // get EIC window
    TICVisualizerTab window = new TICVisualizerTab(new RawDataFile[]{activeRaw}, // raw
        TICPlotType.BASEPEAK, // plot type
        scanSelection, // scan selection
        mzRange, // mz range
        null,
        // new Feature[] {peak}, // selected features
        labelsMap, null); // labels

    // get EIC Plot
    TICPlot ticPlot = window.getTICPlot();
    // ticPlot.setPreferredSize(new Dimension(600, 200));
    ticPlot.getChart().getLegend().setVisible(false);

    final SimpleColorPalette palette = MZmineCore.getConfiguration().getDefaultColorPalette();

    // add a retention time Marker to the EIC
    ValueMarker marker = new ValueMarker(scan.getRetentionTime());
    marker.setPaint(palette.getPositiveColorAWT());
    marker.setStroke(new BasicStroke(3.0f));

    XYPlot plot = (XYPlot) ticPlot.getChart().getPlot();
    plot.addDomainMarker(marker);

    SplitPane ticAndMobilogram = new SplitPane();
    ticAndMobilogram.setOrientation(Orientation.HORIZONTAL);
    ticAndMobilogram.getItems().add(ticPlot);

    if (peak.getFeatureData() instanceof IonMobilogramTimeSeries series
        && peak.getMostIntenseFragmentScan() instanceof MergedMsMsSpectrum mergedMsMs) {
      SimpleXYChart<PlotXYDataProvider> mobilogramChart = createMobilogramChart(peak, mzRange,
          palette, series, mergedMsMs);
      ticAndMobilogram.getItems().add(mobilogramChart);
    }

    bottomPane.getItems().add(ticAndMobilogram);

    SplitPane spectrumPane = new SplitPane();
    spectrumPane.setOrientation(Orientation.HORIZONTAL);

    // get MS/MS spectra window
    SpectraVisualizerTab spectraTab = new SpectraVisualizerTab(activeRaw);
    spectraTab.loadRawData(scan);

    // get MS/MS spectra plot
    SpectraPlot spectrumPlot = spectraTab.getSpectrumPlot();
    spectrumPlot.getChart().getLegend().setVisible(false);
    spectrumPane.getItems().add(spectrumPlot);
    spectrumPane.getItems().add(spectraTab.getToolBar());
    bottomPane.getItems().add(spectrumPane);
    panel.setCenter(bottomPane);
    return panel;
  }

  @NotNull
  private SimpleXYChart<PlotXYDataProvider> createMobilogramChart(ModularFeature peak,
      Range<Double> mzRange, SimpleColorPalette palette, IonMobilogramTimeSeries series,
      MergedMsMsSpectrum mergedMsMs) {
    SimpleXYChart<PlotXYDataProvider> mobilogramChart = new SimpleXYChart<>();
    mobilogramChart.addDataset(new FeatureRawMobilogramProvider(peak, mzRange));
    mobilogramChart.addDataset(new SummedMobilogramXYProvider(series.getSummedMobilogram(),
        new SimpleObjectProperty<>(
            ColorUtils.getContrastPaletteColor(peak.getRawDataFile().getColor(), palette)),
        FeatureUtils.featureToString(peak)));
    mobilogramChart.setDomainAxisNumberFormatOverride(mobilityFormat);
    mobilogramChart.setRangeAxisNumberFormatOverride(intensityFormat);
    mobilogramChart.setDomainAxisLabel(peak.getMobilityUnit().getAxisLabel());
    mobilogramChart.setRangeAxisLabel(unitFormat.format("Summed intensity", "a.u."));

    var optMin = mergedMsMs.getSourceSpectra().stream()
        .mapToDouble(s -> ((MobilityScan) s).getMobility()).min();
    var optMax = mergedMsMs.getSourceSpectra().stream()
        .mapToDouble(s -> ((MobilityScan) s).getMobility()).max();
    if (optMin.isPresent() && optMax.isPresent()) {
      IntervalMarker msmsInterval = new IntervalMarker(optMin.getAsDouble(), optMax.getAsDouble(),
          palette.getPositiveColorAWT(), new BasicStroke(1f), palette.getPositiveColorAWT(),
          new BasicStroke(1f), 0.2f);
      mobilogramChart.getXYPlot().addDomainMarker(msmsInterval);
    }
    return mobilogramChart;
  }
}
