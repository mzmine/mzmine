/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.visualization.mzhistogram.chart;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineRuntimeException;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.modules.visualization.mzhistogram.MZDistributionHistoParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.RangeUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

/**
 * Enhanced version. Use arrows to jump to the next or previous distribution
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class HistogramTab extends MZmineTab implements ActionListener {

  //private final Scene mainScene;
  private final BorderPane mainPane;
  protected HistogramPanel histo;
  private CheckBox cbKeepSameXaxis;
  private RawDataFile dataFile;

  // scan counter
  private int processedScans, totalScans;

  // parameters
  private ScanSelection scanSelection;
  private Scan[] scans;
  private Range<Double> mzRange;
  private Range<Float> rtRange;
  private Boolean useRTRange;
  private boolean useMobilityScans;
  private double binWidth;

  private HistogramData data;

  /**
   * Create the dialog. Auto detect binWidth
   *
   * @wbp.parser.constructor
   */
  //public HistogramTab(RawDataFile dataFile, String title, String xLabel, HistogramData data) {
  //  this(dataFile, title, xLabel, data, 0);
  //}

  /**
   * @param dataFile   rawDataFile
   * @param title      title
   * @param xLabel     xLabel
   * @param parameters parameters
   */
  public HistogramTab(RawDataFile dataFile, String title, String xLabel, ParameterSet parameters) {
    super(title, true, false);
    //setTitle(title);

    this.dataFile = dataFile;
    scanSelection = parameters.getParameter(MZDistributionHistoParameters.scanSelection).getValue();

    mzRange = parameters.getParameter(MZDistributionHistoParameters.mzRange).getValue();
    useRTRange = parameters.getParameter(MZDistributionHistoParameters.rtRange).getValue();
    if (useRTRange) {
      rtRange = RangeUtils
          .toFloatRange(parameters.getParameter(MZDistributionHistoParameters.rtRange)
              .getEmbeddedParameter().getValue());
    }
    binWidth = parameters.getParameter(MZDistributionHistoParameters.binWidth).getValue();
    useMobilityScans = parameters.getParameter(MZDistributionHistoParameters.useMobilityScans)
        .getValue();

    data = buildHistogramData(dataFile);

    mainPane = new BorderPane();
    //mainScene = new Scene(mainPane);

    // Use main CSS
    //mainScene.getStylesheets()
    //    .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

    histo = new HistogramPanel(xLabel, data, binWidth);

    //setMinWidth(1050);
    //setMinHeight(700);
    //setScene(mainScene);
    setContent(mainPane);

    mainPane.setCenter(histo);

    // Add the Windows menu
    //WindowsMenu.addWindowsMenu(mainScene);
    //addKeyBindings();
  }

  private void addKeyBindings() {
    FlowPane pnJump = new FlowPane();

    cbKeepSameXaxis = new CheckBox("keep same x-axis length");
    pnJump.getChildren().add(cbKeepSameXaxis);

    Button btnPrevious = new Button("<");
    btnPrevious.setTooltip(new Tooltip("Jump to previous distribution (use left arrow"));
    btnPrevious.setOnAction(e -> jumpToPrevFeature());
    pnJump.getChildren().add(btnPrevious);

    Button btnNext = new Button(">");
    btnNext.setTooltip(new Tooltip("Jump to previous distribution (use right arrow"));
    btnNext.setOnAction(e -> jumpToNextFeature());
    pnJump.getChildren().add(btnNext);
  }

  private HistogramData buildHistogramData(RawDataFile dataFile) {
    logger.info("Starting to build mz distribution histogram for " + dataFile);

    // all selected scans
    scans = scanSelection.getMatchingScans(dataFile);
    totalScans = scans.length;

    // histo data
    DoubleArrayList data = new DoubleArrayList();

    for (Scan scan : scans) {

      // retention time in range
      if (!useRTRange || rtRange.contains(scan.getRetentionTime())) {

        if (scan.getDataFile() instanceof IMSRawDataFile && useMobilityScans
            && scan instanceof Frame) {
          for (MobilityScan mobilityScan : ((Frame) scan).getMobilityScans()) {
            // go through all mass lists
            MassList massList = mobilityScan.getMassList();
            if (massList == null) {
              throw new NullPointerException("Scan " + dataFile + " #" + scan.getScanNumber()
                  + " does not have a mass list");
            }
            DataPoint mzValues[] = massList.getDataPoints();

            // insert all mz in order and count them
            Arrays.stream(mzValues).mapToDouble(dp -> dp.getMZ()).filter(mz -> mzRange.contains(mz))
                .forEach(mz -> data.add(mz));
          }
        } else {
          // go through all mass lists
          MassList massList = scan.getMassList();
          if (massList == null) {
            throw new NullPointerException("Scan " + dataFile + " #" + scan.getScanNumber()
                + " does not have a mass list");
          }
          DataPoint mzValues[] = massList.getDataPoints();

          // insert all mz in order and count them
          Arrays.stream(mzValues).mapToDouble(dp -> dp.getMZ()).filter(mz -> mzRange.contains(mz))
              .forEach(mz -> data.add(mz));
          processedScans++;
        }
      }
    }

    double[] dataArray = new double[data.size()];
    if (!data.isEmpty()) {
      // to array
      for (int i = 0; i < data.size(); i++) {
        dataArray[i] = data.get(i);
      }
    } else {
      throw new MZmineRuntimeException("Data was empty. Review your selected filters.");
    }

    return new HistogramData(dataArray);
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    final String command = event.getActionCommand();
    if ("PREVIOUS_PEAK".equals(command)) {
      jumpToPrevFeature();
    } else if ("NEXT_PEAK".equals(command)) {
      jumpToNextFeature();
    }
  }

  /**
   * tries to find the next local maximum to jump to the prev peak
   */
  private void jumpToPrevFeature() {
    XYPlot plot = getXYPlot();
    if (plot == null) {
      return;
    }

    XYDataset data = plot.getDataset(0);
    // get center of zoom
    ValueAxis x = plot.getDomainAxis();
    double mid = (x.getUpperBound() + x.getLowerBound()) / 2;

    boolean started = false;

    for (int i = data.getItemCount(0) - 1; i >= 1; i--) {
      double mz = data.getXValue(0, i);
      if (mz < mid) {
        // wait for y to be 0 to start the search for a new peak
        if (!started) {
          if (data.getYValue(0, i) == 0) {
            started = true;
          }
        } else {
          // intensity drops?
          if (data.getYValue(0, i - 1) != 0 && data.getYValue(0, i) >= 100
              && data.getYValue(0, i - 1) < data.getYValue(0, i)) {
            // peak found with max at i
            setZoomAroundFeatureAt(i);
            return;
          }
        }
      }
    }
  }

  /**
   * tries to find the next local maximum to jump to the prev peak
   */
  private void jumpToNextFeature() {
    XYPlot plot = getXYPlot();
    if (plot == null) {
      return;
    }

    XYDataset data = plot.getDataset(0);
    // get center of zoom
    ValueAxis x = plot.getDomainAxis();
    // mid of range
    double mid = (x.getUpperBound() + x.getLowerBound()) / 2;

    boolean started = false;

    for (int i = 0; i < data.getItemCount(0) - 1; i++) {
      double mz = data.getXValue(0, i);
      if (mz > mid) {
        // wait for y to be 0 to start the search for a new peak
        if (!started) {
          if (data.getYValue(0, i) == 0) {
            started = true;
          }
        } else {
          // intensity drops?
          if (data.getYValue(0, i + 1) != 0 && data.getYValue(0, i) >= 100
              && data.getYValue(0, i + 1) < data.getYValue(0, i)) {
            // peak found with max at i
            setZoomAroundFeatureAt(i);
            return;
          }
        }
      }
    }
  }

  /**
   * Set zoom factor around peak at data point i
   *
   * @param i
   */
  private void setZoomAroundFeatureAt(int i) {
    XYPlot plot = getXYPlot();
    if (plot == null) {
      return;
    }

    XYDataset data = plot.getDataset(0);

    // keep same domain axis range length
    boolean keepRange = cbKeepSameXaxis.isSelected();

    // find lower bound (where y=0)
    double lower = data.getXValue(0, i);
    for (int x = i; x >= 0; x--) {
      if (data.getYValue(0, x) == 0) {
        lower = data.getXValue(0, x);
        break;
      }
    }
    // find upper bound /where y=0)
    double upper = data.getXValue(0, i);
    for (int x = i; x < data.getItemCount(0); x++) {
      if (data.getYValue(0, x) == 0) {
        upper = data.getXValue(0, x);
        break;
      }
    }

    if (keepRange) {
      // set constant range zoom
      double length = plot.getDomainAxis().getRange().getLength();
      plot.getDomainAxis().setRangeAboutValue(data.getXValue(0, i), length);
    } else {
      // set range directly around peak
      plot.getDomainAxis().setRange(lower, upper);
    }

    // auto gaussian fit
    if (getHistoPanel().isGaussianFitEnabled()) {
      // find
      getHistoPanel().setGaussianFitRange(lower, upper);
    }

    // auto range y
    ChartLogicsFX.autoRangeAxis(getChartPanel());
  }

  private ChartViewer getChartPanel() {
    return getHistoPanel().getChartPanel();
  }

  private XYPlot getXYPlot() {
    ChartViewer chart = getHistoPanel().getChartPanel();
    if (chart != null) {
      return chart.getChart().getXYPlot();
    } else {
      return null;
    }
  }

  public CheckBox getCbKeepSameXaxis() {
    return cbKeepSameXaxis;
  }

  public HistogramPanel getHistoPanel() {
    return histo;
  }

  public int getTotalScans() {
    return totalScans;
  }

  public int getProcessedScans() {
    return processedScans;
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return new ArrayList<>(Collections.singletonList(dataFile));
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
    if (rawDataFiles == null || rawDataFiles.isEmpty()) {
      return;
    }

    // get first raw data file
    RawDataFile newFile = rawDataFiles.iterator().next();
    if (dataFile.equals(newFile)) {
      return;
    }

    HistogramData newData = buildHistogramData(newFile);
    histo.setData(newData, binWidth);

    dataFile = newFile;
    data = newData;
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(
      Collection<? extends FeatureList> featureLists) {

  }
}
