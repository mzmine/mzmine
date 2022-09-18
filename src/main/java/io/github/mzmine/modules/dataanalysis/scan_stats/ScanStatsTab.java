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

package io.github.mzmine.modules.dataanalysis.scan_stats;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.control.TextArea;
import org.jetbrains.annotations.NotNull;

/**
 * This module collects simple statistics on the raw data files, scans, data points
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class ScanStatsTab extends SimpleTab {

  public ScanStatsTab(ParameterSet parameters) {
    super("Scan stats");
    RawDataFile[] raws = parameters.getValue(ScanStatsParameters.raws).getMatchingRawDataFiles();
    ScanSelection scanSelection = parameters.getValue(ScanStatsParameters.scanSelection);

    List<RawFileStats> data = analyzeDataFiles(raws, scanSelection);

    StringBuilder builder = new StringBuilder("n data files").append(raws.length).append("\n\n");
    builder.append(RawFileStats.getTitle()).append("\n");
    for (RawFileStats d : data) {
      builder.append(d.toString()).append("\n");
    }
    // show as tab separated text
    TextArea text = new TextArea(builder.toString());
    setContent(text);
  }

  @NotNull
  private List<RawFileStats> analyzeDataFiles(RawDataFile[] raws, ScanSelection scanSelection) {
    List<RawFileStats> data = Arrays.stream(raws).parallel().map(raw -> {
      Scan[] scans = scanSelection.getMatchingScans(raw);

      int allScans = raw.getNumOfScans();
      int selectedScans = scans.length;
      long mobilityScans = Arrays.stream(scans).filter(s -> s instanceof Frame).map(s -> (Frame) s)
          .mapToLong(Frame::getNumberOfMobilityScans).sum();
      long dataPoints = Arrays.stream(scans).mapToLong(this::getDataPointsGreaterZero).sum();
      long dataPointsInMobilityScans = Arrays.stream(scans).filter(s -> s instanceof Frame)
          .map(s -> (Frame) s).map(Frame::getMobilityScans).flatMap(List::stream)
          .mapToLong(this::getDataPointsGreaterZero).sum();

      return new RawFileStats(raw.getName(), allScans, selectedScans, dataPoints, mobilityScans,
          dataPointsInMobilityScans);
    }).collect(Collectors.toCollection(ArrayList::new));

    // sum stats
    int allScans = 0;
    int selectedScans = 0;
    long dataPoints = 0;
    long mobilityScans = 0;
    long dataPointsInMobilityScans = 0;

    for (RawFileStats d : data) {
      allScans += d.allScans;
      selectedScans += d.selectedScans;
      dataPoints += d.dataPoints;
      mobilityScans += d.mobilityScans;
      dataPointsInMobilityScans += d.dataPointsInMobilityScans;
    }
    int n = raws.length;
    data.add(0, new RawFileStats("All files " + raws.length, allScans, selectedScans, dataPoints,
        mobilityScans, dataPointsInMobilityScans));
    data.add(0, new RawFileStats("Mean for files " + raws.length, allScans / n, selectedScans / n,
        dataPoints / n, mobilityScans / n, dataPointsInMobilityScans / n));
    return data;
  }

  private int getDataPointsGreaterZero(Scan scan) {
    int n = 0;
    for (int i = 0; i < scan.getNumberOfDataPoints(); i++) {
      if (scan.getIntensityValue(i) > 0) {
        n++;
      }
    }
    return n;
  }


  private record RawFileStats(String name, int allScans, int selectedScans, long dataPoints,
                              long mobilityScans, long dataPointsInMobilityScans) {

    public static String getTitle() {
      return Arrays.stream(RawFileStats.class.getRecordComponents()).map(RecordComponent::getName)
          .collect(Collectors.joining("\t"));
    }

    @Override
    public String toString() {
      return Arrays.stream(RawFileStats.class.getRecordComponents())
          .map(RecordComponent::getAccessor).map(accessor -> {
            try {
              return accessor.invoke(this);
            } catch (IllegalAccessException e) {
              throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
              throw new RuntimeException(e);
            }
          }).map(String::valueOf).collect(Collectors.joining("\t"));
    }
  }

}
