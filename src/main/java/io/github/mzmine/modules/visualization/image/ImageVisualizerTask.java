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

package io.github.mzmine.modules.visualization.image;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYZScatterPlot;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.FeatureImageProvider;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.import_rawdata_imzml.ImagingParameters;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFXModule;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFXParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ImageVisualizerTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final ParameterSet parameters;
  private final ImagingRawDataFile rawDataFile;
  private final ImagingParameters imagingParameters;
  private final ScanSelection scanSelection;
  private final Range<Double> mzRange;

  private double pixelWidth;
  private double pixelHeight;
  private double progress = 0.0;
  private String taskDescription = "Building image";


  public ImageVisualizerTask(RawDataFile rawDataFile, ParameterSet parameters) {
    super(null, Instant.now());// date irrelevant
    this.parameters = parameters;
    this.rawDataFile = (ImagingRawDataFile) rawDataFile;
    this.imagingParameters = ((ImagingRawDataFile) rawDataFile).getImagingParam();
    this.scanSelection =
        parameters.getParameter(ImageVisualizerParameters.scanSelection).getValue();
    this.mzRange = parameters.getParameter(ImageVisualizerParameters.mzRange).getValue();
    setStatus(TaskStatus.WAITING);
  }

  @Override
  public String getTaskDescription() {
    return taskDescription;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (isCanceled()) {
      return;
    }
    progress = 0.0;
    List<DataPoint> imageDataPoints = extractAllDataPointsFromScans();
    SimpleIonTimeSeries timeSeries = extractIonTimeSeries(
        imageDataPoints);

    ModularFeature feature = new ModularFeature(
        new ModularFeatureList("Raw data feature", rawDataFile.getMemoryMapStorage(), rawDataFile),
        rawDataFile, timeSeries,
        FeatureStatus.DETECTED);

    FeatureImageProvider prov = new FeatureImageProvider(feature);
    ColoredXYZDataset ds = new ColoredXYZDataset(prov, RunOption.THIS_THREAD);

    SimpleXYZScatterPlot<FeatureImageProvider> chart = new SimpleXYZScatterPlot<>();
    chart.setRangeAxisLabel("µm");
    chart.setDomainAxisLabel("µm");

    final boolean hideAxes = MZmineCore.getConfiguration()
        .getModuleParameters(FeatureTableFXModule.class).getParameter(
            FeatureTableFXParameters.hideImageAxes).getValue();

    NumberAxis axis = (NumberAxis) chart.getXYPlot().getRangeAxis();
    chart.setDataset(ds);
    axis.setInverted(true);
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeIncludesZero(false);
    axis.setRange(new org.jfree.data.Range(0, imagingParameters.getLateralHeight()));
    axis.setVisible(!hideAxes);

    axis = (NumberAxis) chart.getXYPlot().getDomainAxis();
    axis.setAutoRangeStickyZero(false);
    axis.setAutoRangeIncludesZero(false);
    chart.getXYPlot().setDomainAxisLocation(AxisLocation.TOP_OR_RIGHT);
    axis.setRange(new org.jfree.data.Range(0, imagingParameters.getLateralWidth()));
    axis.setVisible(!hideAxes);

    final boolean lockOnAspectRatio = MZmineCore.getConfiguration()
        .getModuleParameters(FeatureTableFXModule.class).getParameter(
            FeatureTableFXParameters.lockImagesToAspectRatio).getValue();
    chart.getXYPlot().setBackgroundPaint(Color.BLACK);

    MZmineCore.runLater(() -> {
      ImageVisualizerTab newTab =
          new ImageVisualizerTab(chart, rawDataFile, imagingParameters);
      MZmineCore.getDesktop().addTab(newTab);
    });

    progress = 1.0;
    setStatus(TaskStatus.FINISHED);
  }

  @NotNull
  private SimpleIonTimeSeries extractIonTimeSeries(List<DataPoint> dataPoints) {
    Scan[] scans = scanSelection.getMatchingScans(rawDataFile);
    int numDp = dataPoints.size();
    double[] mzs = new double[numDp];
    double[] intensities = new double[numDp];
    List<Scan> scansList = new ArrayList<>();
    int i = 0;
    for (DataPoint dp : dataPoints) {
      mzs[i] = dp.getMZ();
      intensities[i] = dp.getIntensity();
      scansList.add(scans[i]);
      i++;
    }

    return new SimpleIonTimeSeries(null, mzs,
        intensities, scansList);
  }

  private List<DataPoint> extractAllDataPointsFromScans() {
    logger.info("Start data point extraction");
    taskDescription = "Get data points from scans";
    int processedScans = 1;
    List<DataPoint> allDataPoints = new ArrayList<>();
    Scan[] scans = scanSelection.getMatchingScans(rawDataFile);
    for (Scan scan : scans) {
      if (!(scan instanceof ImagingScan) || !scanSelection.matches(scan)) {
        continue;
      }
      double intensitySum = Arrays
          .stream(ScanUtils.selectDataPointsByMass(ScanUtils.extractDataPoints(scan), mzRange))
          .mapToDouble(DataPoint::getIntensity).sum();
      allDataPoints.add(new SimpleDataPoint(0.0, intensitySum));
      progress = (processedScans / (double) scans.length);
      processedScans++;
    }
    logger.info("Extracted " + allDataPoints.size() + " ims data points");
    return allDataPoints;
  }

}
