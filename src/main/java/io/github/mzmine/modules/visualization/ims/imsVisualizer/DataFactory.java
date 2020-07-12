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

/*
This module is going to prepare all the data sets required by ion-mobility and it's visualizations(ims)

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
  private ArrayList<Double> mobility;
  private ArrayList<Double> retentionTime;
  private double[] mobilityValues;
  private double[] intensityMobilityValues;
  private double[] intensityRetentionValues;
  private double[] rtHeatMapValues;
  private double[] mobilityHeatMapValues;
  private double[] intensityHeatMapValues;
  private double[] retentionTimeValues;
  private HashMap<Double, Double> scanMobilityMap;
  private HashMap<Double, Double> scanRetentionTimeMap;
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
    scanRetentionTimeMap = new HashMap<>();
    scanSize = scans.length;

    // data for the retention time - heatMap plot.
    rtHeatMapValues = new double[scanSize];
    mobilityHeatMapValues = new double[scanSize];
    intensityHeatMapValues = new double[scanSize];

    for (int i = 0; i < scanSize; i++) {
      // get all unique mobility and retentionTime scans.
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

      if (scanRetentionTimeMap.get(scans[i].getRetentionTime()) == null) {
        scanRetentionTimeMap.put(scans[i].getRetentionTime(), intensitySum);
      } else {
        double getSum = scanRetentionTimeMap.get(scans[i].getRetentionTime());
        scanRetentionTimeMap.put(scans[i].getRetentionTime(), getSum + intensitySum);
      }
    }

    intensityMobilityValues = new double[mobility.size()];
    intensityRetentionValues = new double[retentionTime.size()];
    mobilityValues = new double[mobility.size()];
    retentionTimeValues = new double[retentionTime.size()];

    // get the unique mobilities.
    for (int i = 0; i < mobility.size(); i++) {
      mobilityValues[i] = mobility.get(i);
    }
    // get the unique retention time.
    for (int i = 0; i < retentionTime.size(); i++) {
      retentionTimeValues[i] = retentionTime.get(i);
    }

    /* scanMobilityMap : Here scanMobilityMap contains mobility as key and values as the sum of all the intensity at
       that that mobility.

       scanRetentionTimeMap : Here scanRetentionTimeMap contains retentionTime as key and values as the sum of all the intensity at
       that that retentionTime.
    */
    for (int i = 0; i < mobility.size(); i++) {

      if (scanMobilityMap.get(mobility.get(i)) != null) {

        intensityMobilityValues[i] = scanMobilityMap.get(mobility.get(i));
      }
    }

    for (int i = 0; i < retentionTime.size(); i++) {

      if (scanRetentionTimeMap.get(retentionTime.get(i)) != null) {
        intensityRetentionValues[i] = scanRetentionTimeMap.get(retentionTime.get(i));
      }
    }
  }
  /*
  get all the unique mobilities in all scan
  */
  public double[] getMobilityValues() {
    return mobilityValues;
  }
  /*
   get the all unique retention times in all   scan
  */
  public double[] getRetentionTimeValues() {
    return retentionTimeValues;
  }
  /*
   get all the intensities value at unique mobilities.
  */
  public double[] getIntensityMobilityValues() {
    return intensityMobilityValues;
  }
  /*
   get the all the intensities values at unique retention time
  */
  public double[] getIntensityRetentionValues() {
    return intensityRetentionValues;
  }

  /*
  return retentionTime retentionTime-mobility heat map
  */
  public double[] getRtHeatMapValues() {
    return rtHeatMapValues;
  }
  /*
   Return intensities for retentionTime-mobility heat map.
  */
  public double[] getIntensityHeatMapValues() {
    return intensityHeatMapValues;
  }
  /*
   Return mobilities for retentionTime-mobility heat map.
  */
  public double[] getMobilityHeatMapValues() {
    return mobilityHeatMapValues;
  }

  /*
    return all the scans .
  */
  public Scan[] getScans() {
    return scans;
  }

  /*
    return the the seleted mz Range.
  */
  public Range getmzRange() {
    return mzRange;
  }
}
