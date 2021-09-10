/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.visualization.mzhistogram.chart;

import com.google.common.collect.Range;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.modules.visualization.mzhistogram.ScanMzHistogramParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.RangeUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.XYPlot;

/**
 * Enhanced version. Use arrows to jump to the next or previous distribution
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class HistogramTab extends MZmineTab {

  //private final Scene mainScene;
  private final BorderPane mainPane;
  protected HistogramPanel histo;
  private RawDataFile dataFile;

  // scan counter
  private int processedScans, totalScans;

  // parameters
  private final ScanSelection scanSelection;
  private Scan[] scans;
  private final Range<Double> mzRange;
  private Range<Float> rtRange;
  private final Boolean useRTRange;
  private final boolean useMobilityScans;
  private final double binWidth;

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
    scanSelection = parameters.getParameter(ScanMzHistogramParameters.scanSelection).getValue();

    mzRange = parameters.getParameter(ScanMzHistogramParameters.mzRange).getValue();
    useRTRange = parameters.getParameter(ScanMzHistogramParameters.rtRange).getValue();
    if (useRTRange) {
      rtRange = RangeUtils
          .toFloatRange(parameters.getParameter(ScanMzHistogramParameters.rtRange)
              .getEmbeddedParameter().getValue());
    }
    binWidth = parameters.getParameter(ScanMzHistogramParameters.binWidth).getValue();
    useMobilityScans = parameters.getParameter(ScanMzHistogramParameters.useMobilityScans)
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
            DataPoint[] mzValues = massList.getDataPoints();

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
          DataPoint[] mzValues = massList.getDataPoints();

          // insert all mz in order and count them
          Arrays.stream(mzValues).mapToDouble(dp -> dp.getMZ()).filter(mz -> mzRange.contains(mz))
              .forEach(mz -> data.add(mz));
          processedScans++;
        }
      }
    }

    if (!data.isEmpty()) {
      // to array
      return new HistogramData(data.toDoubleArray());
    } else {
      throw new MSDKRuntimeException("Data was empty. Review your selected filters.");
    }
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
