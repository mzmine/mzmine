/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.injection_time;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.generators.SimpleToolTipGenerator;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYShapeRenderer;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.SpectraMassListRenderer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.massdefect.MassDefectFilter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.scene.layout.BorderPane;
import org.jetbrains.annotations.NotNull;

/**
 * This module plots the minimum signal intensities in mass lists against the injection time to
 * detect trends in trap based mass spectrometers with fill times. Usually Orbitrap analyzers seem
 * to follow a linear relation ship between (1/injection time & mass resolution) to noise level
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class InjectTimeAnalysisTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(InjectTimeAnalysisTask.class.getName());
  private final RawDataFile[] dataFiles;
  private final MassDefectFilter massDefectFilter;
  private final Boolean useMassDefect;
  private final Boolean useMobilityScans;
  private final Range<Double> mzRange;
  private final ScanSelection scanSelection;
  private final int minSignals;
  private final Double minIntensityFactor;
  private int totalScans;
  private long processedScans;

  public InjectTimeAnalysisTask(RawDataFile[] dataFile, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null
    this.dataFiles = dataFile;
    scanSelection = parameters.getValue(InjectTimeAnalysisParameters.scanSelection);
    mzRange = parameters.getValue(InjectTimeAnalysisParameters.mzRange);
    useMobilityScans = parameters.getValue(InjectTimeAnalysisParameters.useMobilityScans);
    minSignals = parameters.getValue(InjectTimeAnalysisParameters.minSignalsInScan);
    minIntensityFactor = parameters.getValue(InjectTimeAnalysisParameters.minIntensityFactor);

    useMassDefect = parameters.getValue(InjectTimeAnalysisParameters.massDefect);
    massDefectFilter = parameters.getParameter(InjectTimeAnalysisParameters.massDefect)
        .getEmbeddedParameter().getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Creating inject time analysis of " + Arrays.stream(dataFiles).map(Object::toString)
        .collect(Collectors.joining(","));
  }

  @Override
  public double getFinishedPercentage() {
    if (totalScans == 0) {
      return 0;
    } else {
      return (double) processedScans / totalScans;
    }
  }

  public RawDataFile[] getDataFiles() {
    return dataFiles;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    List<InjectData> data = buildData(dataFiles);
    data.sort(Comparator.comparingInt(InjectData::msLevel));

    if (data.isEmpty()) {
      logger.info("No data found");
      setStatus(TaskStatus.FINISHED);
      return;
    }

    SimpleXYChart<PlotXYDataProvider> chart = new SimpleXYChart<>("Inject time",
        "Lowest intensity");

    SimpleColorPalette colors = MZmineCore.getConfiguration().getDefaultColorPalette();
    List<ColoredXYDataset> datasets = data.stream().mapToInt(InjectData::msLevel).distinct()
        .sorted().mapToObj(msLevel -> new ColoredXYDataset(
            new InjectTimeDataProvider(msLevel, colors.getAWT(msLevel - 1), data),
            RunOption.THIS_THREAD)).toList();

    for (ColoredXYDataset dataset : datasets) {
      var defaultRenderer = new ColoredXYShapeRenderer();
      Color color = dataset.getAWTColor();
      defaultRenderer.setSeriesPaint(0, color);
      defaultRenderer.setSeriesToolTipGenerator(0, new SimpleToolTipGenerator());
      chart.addDataset(dataset, new SpectraMassListRenderer(color));

      chart.addRegression(dataset, 0);
    }

    MZmineCore.runLater(() -> {
      SimpleTab tab = new SimpleTab("Inject time");
      tab.setContent(new BorderPane(chart));
      MZmineCore.getDesktop().addTab(tab);
    });
    setStatus(TaskStatus.FINISHED);
    logger.info("Finished creating inject time scatter");
  }

  private List<InjectData> buildData(RawDataFile... dataFiles) {
    List<InjectData> data = new ArrayList<>();
    for (RawDataFile dataFile : dataFiles) {
      if (dataFile instanceof IMSRawDataFile ims && useMobilityScans) {
        MobilityScanDataAccess scanAccess = EfficientDataAccess.of(ims,
            MobilityScanDataType.CENTROID, scanSelection);
        totalScans = scanAccess.getNumberOfScans();
        while (scanAccess.nextFrame() != null) {
          while (scanAccess.nextMobilityScan() != null) {
            addAllDataPoints(scanAccess, data, scanAccess.getMobility());
          }
        }
      } else {
        ScanDataAccess scanAccess = EfficientDataAccess.of(dataFile, ScanDataType.CENTROID,
            scanSelection);
        totalScans = scanAccess.getNumberOfScans();
        while (scanAccess.nextScan() != null) {
          addAllDataPoints(scanAccess, data, -1d);
          processedScans++;
        }
      }
    }
    return data;
  }

  private void addAllDataPoints(Scan scan, List<InjectData> data, double mobility) {
    Float injectTime = scan.getInjectionTime();
    if (injectTime == null) {
      return;
    }

    int n = scan.getNumberOfDataPoints();
    if (n < minSignals) {
      return;
    }

    double mz = -1d;
    double lowestIntensity = 0d;
    double maxIntensity = 0d;

    for (int i = 0; i < n; i++) {
      double intensity = scan.getIntensityValue(i);
      double currentMz = scan.getMzValue(i);

      if (intensity > 0 && mzRange.contains(currentMz) && (!useMassDefect
          || massDefectFilter.contains(currentMz))) {
        if (mz < 0 || intensity < lowestIntensity) {
          mz = currentMz;
          lowestIntensity = intensity;
        }
        if (maxIntensity < intensity) {
          maxIntensity = intensity;
        }
      }
    }
    if (mz > 0 && maxIntensity / lowestIntensity >= minIntensityFactor) {
      data.add(new InjectData(injectTime, lowestIntensity, mz, scan.getMSLevel(), mobility));
    }
  }


}
