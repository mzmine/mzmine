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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.ims.imsVisualizer;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerParameters;
import io.github.mzmine.parameters.ParameterSet;

import java.util.ArrayList;
import java.util.HashMap;

public class DataFactory {
  private RawDataFile dataFiles[];
  private Scan scans[];
  private Range<Double> mzRange;
  ArrayList<Double> mobility;
  ArrayList<Double> retentionTime;
  private double[] mobilityValues;
  private double[] intensityMobilityValues;
  private double[] intensityRetentionValues;
  private double[] rtHeatMapValues;
  private double[] mobilityHeatMapValues;
  private double[] intensityHeatMapValues;
  private double[] mzValues;
  private double[] retentionTimeValues;
  HashMap<Double, Double> scanMobilityMap;
  HashMap<Double, Double> scanretentionTimeMap;
  private int scanSize;

  public DataFactory(ParameterSet parameters) {
    dataFiles =
        parameters
            .getParameter(ImsVisualizerParameters.dataFiles)
            .getValue()
            .getMatchingRawDataFiles();

    scans =
        parameters
            .getParameter(ImsVisualizerParameters.scanSelection)
            .getValue()
            .getMatchingScans(dataFiles[0]);

    mzRange = parameters.getParameter(ImsVisualizerParameters.mzRange).getValue();

    mobility = new ArrayList<>();
    retentionTime = new ArrayList<>();
    scanMobilityMap = new HashMap<>();
    scanretentionTimeMap = new HashMap<>();
    scanSize = scans.length;

    // data for the retention time heatmap plot.
    rtHeatMapValues = new double[scanSize];
    mobilityHeatMapValues = new double[scanSize];
    intensityHeatMapValues = new double[scanSize];

    for (int i = 0; i < scanSize; i++) {
      // get all unique mobility and retentiontime scans.
      if (i == 0) {
        mobility.add(scans[i].getMobility());
        retentionTime.add(scans[i].getRetentionTime());
      } else {

        if (scans[i].getMobility() != scans[i - 1].getMobility()) {
          mobility.add(scans[i].getMobility());
        }
        if (scans[i].getRetentionTime() != scans[i - 1].getRetentionTime()) {
          retentionTime.add(scans[i].getRetentionTime());
        }
      }
      rtHeatMapValues[i] = scans[i].getRetentionTime();
      mobilityHeatMapValues[i] = scans[i].getMobility();
      double intensitySum = 0;
      DataPoint dataPoint[] = scans[i].getDataPointsByMass(mzRange);
      for (int j = 0; j < dataPoint.length; j++) {
        intensityHeatMapValues[i] += dataPoint[j].getIntensity();
        intensitySum += dataPoint[j].getIntensity();
      }

      if (scanMobilityMap.get(scans[i].getMobility()) == null) {
        scanMobilityMap.put(scans[i].getMobility(), intensitySum);
      } else {
        double getSum = scanMobilityMap.get(scans[i].getMobility());
        scanMobilityMap.put(scans[i].getMobility(), getSum + intensitySum);
      }

      if (scanretentionTimeMap.get(scans[i].getRetentionTime()) == null) {
        scanretentionTimeMap.put(scans[i].getRetentionTime(), intensitySum);
      } else {
        double getSum = scanretentionTimeMap.get(scans[i].getRetentionTime());
        scanretentionTimeMap.put(scans[i].getRetentionTime(), getSum + intensitySum);
      }
    }

    intensityMobilityValues = new double[mobility.size()];
    intensityRetentionValues = new double[retentionTime.size()];
    mobilityValues = new double[mobility.size()];
    retentionTimeValues = new double[retentionTime.size()];

    for (int i = 0; i < (int) mobility.size(); i++) {
      mobilityValues[i] = mobility.get(i);
    }
    for (int i = 0; i < retentionTime.size(); i++) {
      retentionTimeValues[i] = retentionTime.get(i);
    }

    for (int i = 0; i < mobility.size(); i++) {

      if (scanMobilityMap.get(mobility.get(i)) != null) {

        intensityMobilityValues[i] = scanMobilityMap.get(mobility.get(i));
      }
    }

    for (int i = 0; i < retentionTime.size(); i++) {

      if (scanretentionTimeMap.get(retentionTime.get(i)) != null) {
        intensityRetentionValues[i] = scanretentionTimeMap.get(retentionTime.get(i));
      }
    }
  }

  public double[] getMobilityValues() {
    return mobilityValues;
  }

  public double[] getRetentionTimeValues() {
    return retentionTimeValues;
  }

  public double[] getIntensityMobilityValues() {
    return intensityMobilityValues;
  }

  public double[] getIntensityRetentionValues() {
    return intensityRetentionValues;
  }

  // return rt-mobility heatmap data's.

  public double[] getRtHeatMapValues() {
    return rtHeatMapValues;
  }

  public double[] getIntensityHeatMapValues() {
    return intensityHeatMapValues;
  }

  public double[] getMobilityHeatMapValues() {
    return mobilityHeatMapValues;
  }

  public Scan[] getScans() {
    return scans;
  }
}
