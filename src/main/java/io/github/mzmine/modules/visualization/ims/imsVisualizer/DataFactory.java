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
import io.github.mzmine.modules.visualization.ims.ImsVisualizerTask;
import io.github.mzmine.parameters.ParameterSet;

import java.util.ArrayList;
import java.util.HashMap;

public class DataFactory {

  private RawDataFile dataFiles[];
  private Scan scans[];
  private Range<Double> mzRange;
  private double[] mobility_retentionTimeMobility;
  private double[] intensity_retentionTimeMobility;
  private double[] retentionTime_retentionTimeMobility;

  private double[] intensity_retentionTimeIntensity;
  private double[] retentionTime_retentionTimeIntensity;

  private Double[] mobility_MzMobility;
  private Double[] mz_MzMobility;
  private Double[] intensity_MzMobility;

  private Double[] intensity_IntensityMobility;
  private Double[] mobility_IntensityMobility;

  public HashMap<Double, Double> scanMobilityMap = new HashMap<>();
  public HashMap<Double, Double> scanRetentionTimeMap = new HashMap<>();
  private Double selectedRetentionTime;
  private int scanSize;

  public DataFactory(ParameterSet parameters, double rt, ImsVisualizerTask imsVisualizerTask) {
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

    this.selectedRetentionTime = rt;
    scanSize = scans.length;

    // if there is not selected any retention time.select the rt for max intensity.
    double maxIntensity = 0.0;
    if (selectedRetentionTime == 0.0) {
      for (int i = 0; i < scanSize; i++) {

        DataPoint dataPoint[] = scans[i].getDataPointsByMass(mzRange);
        double intensitySum = 0;
        for (int j = 0; j < dataPoint.length; j++) {
          intensitySum += dataPoint[j].getIntensity();
        }

        if (maxIntensity < intensitySum) {
          maxIntensity = intensitySum;
          this.selectedRetentionTime = scans[i].getRetentionTime();
        }
      }

      imsVisualizerTask.setSelectedRetentionTime(selectedRetentionTime);
    }

    // prepare data's for the ims frame.( mz-mobility heatmap and intensity mobility)
    updateFrameData(selectedRetentionTime);

    // data for the retention time - heatMap plot.
    retentionTime_retentionTimeMobility = new double[scanSize];
    mobility_retentionTimeMobility = new double[scanSize];
    intensity_retentionTimeMobility = new double[scanSize];

    ArrayList<Double> rt_rtIntensity = new ArrayList<>();

    for (int i = 0; i < scanSize; i++) {
      if (i == 0) {
        rt_rtIntensity.add(scans[i].getRetentionTime());
      } else {
        if (scans[i].getRetentionTime() != scans[i - 1].getRetentionTime()) {
          rt_rtIntensity.add(scans[i].getRetentionTime());
        }
      }
      retentionTime_retentionTimeMobility[i] = scans[i].getRetentionTime();
      mobility_retentionTimeMobility[i] = scans[i].getMobility();
      double intensitySum = 0;
      DataPoint dataPoint[] = scans[i].getDataPointsByMass(mzRange);
      for (int j = 0; j < dataPoint.length; j++) {
        intensity_retentionTimeMobility[i] += dataPoint[j].getIntensity();
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

    intensity_retentionTimeIntensity = new double[rt_rtIntensity.size()];
    retentionTime_retentionTimeIntensity = new double[rt_rtIntensity.size()];

    // get the unique retention time.
    for (int i = 0; i < rt_rtIntensity.size(); i++) {
      retentionTime_retentionTimeIntensity[i] = rt_rtIntensity.get(i);
    }
    /*
       scanRetentionTimeMap : Here scanRetentionTimeMap contains retentionTime as key and values as the sum of all the intensity at
       that that retentionTime.
    */
    for (int i = 0; i < rt_rtIntensity.size(); i++) {

      if (scanRetentionTimeMap.get(rt_rtIntensity.get(i)) != null) {
        intensity_retentionTimeIntensity[i] = scanRetentionTimeMap.get(rt_rtIntensity.get(i));
      }
    }
  }

  public void updateFrameData(double selectedRetentionTime) {

    class MzMobilityFields {
      public double _mobility;
      public double _mz;
      public double _intensity;

      public MzMobilityFields(double _mobility, double _mz, double _intensity) {

        this._mobility = _mobility;
        this._mz = _mz;
        this._intensity = _intensity;
      }
    }

    class IntensityMobilityFields {
      public double _mobility;
      public double _intensity;

      public IntensityMobilityFields(double _mobility, double _intensity) {
        this._intensity = _intensity;
        this._mobility = _mobility;
      }
    }

    ArrayList<Scan>selectedScan = new ArrayList<>();
    // ims frame.
    ArrayList<MzMobilityFields> mzMobilityFields = new ArrayList<>();
    ArrayList<IntensityMobilityFields> intensityMobilityFields = new ArrayList<>();
    for (int i = 0; i < scanSize; i++) {
      if (scans[i].getRetentionTime() == selectedRetentionTime) {
          selectedScan.add(scans[i]);
        double intensitySum = 0;
        DataPoint dataPoint[] = scans[i].getDataPointsByMass(mzRange);
        for (int j = 0; j < dataPoint.length; j++) {
          intensitySum += dataPoint[j].getIntensity();
          mzMobilityFields.add(
              new MzMobilityFields(
                  scans[i].getMobility(), dataPoint[j].getMZ(), dataPoint[j].getIntensity()));
        }
        intensityMobilityFields.add(
            new IntensityMobilityFields(scans[i].getMobility(), intensitySum));
      }
    }

    mobility_MzMobility = new Double[mzMobilityFields.size()];
    mz_MzMobility = new Double[mzMobilityFields.size()];
    intensity_MzMobility = new Double[mzMobilityFields.size()];

    for (int i = 0; i < mzMobilityFields.size(); i++) {
      mobility_MzMobility[i] = mzMobilityFields.get(i)._mobility;
      mz_MzMobility[i] = mzMobilityFields.get(i)._mz;
      intensity_MzMobility[i] = mzMobilityFields.get(i)._intensity;
    }

    intensity_IntensityMobility = new Double[intensityMobilityFields.size()];
    mobility_IntensityMobility = new Double[intensityMobilityFields.size()];
    for (int i = 0; i < intensityMobilityFields.size(); i++) {
      intensity_IntensityMobility[i] = intensityMobilityFields.get(i)._intensity;
      mobility_IntensityMobility[i] = intensityMobilityFields.get(i)._mobility;
    }
  }
  /*
  get all the unique mobilities in all scan
  */
  public double[] getMobility_retentionTimeMobility() {
    return mobility_retentionTimeMobility;
  }

  /*
   Return mobilities for retentionTime-mobility heat map.
  */
  public double[] getRetentionTime_retentionTimeIntensity() {
    return retentionTime_retentionTimeIntensity;
  }

  public Double[] getMobility_MzMobility() {
    return mobility_MzMobility;
  }

  public Double[] getMobility_IntensityMobility() {
    return mobility_IntensityMobility;
  }

  /*
   get the all unique retention times in all   scan
  */
  public double[] getIntensity_retentionTimeMobility() {
    return intensity_retentionTimeMobility;
  }
  /*
   get all the intensities value at unique mobilities.
  */
  public double[] getIntensity_retentionTimeIntensity() {
    return intensity_retentionTimeIntensity;
  }
  /*
   get the all the intensities values at unique retention time
  */
  public double[] getRetentionTime_retentionTimeMobility() {
    return retentionTime_retentionTimeMobility;
  }

  /*
    return all the scans .
  */
  public Scan[] getScans() {
    return scans;
  }

  /*
    return the seleted mz Range.
  */
  public Range getmzRange() {
    return mzRange;
  }
  /*
    return the mobility.
  */
  public Double[] getMz_MzMobility() {
    return mz_MzMobility;
  }

  public Double[] getIntensity_MzMobility() {
    return intensity_MzMobility;
  }

  public Double[] getIntensity_IntensityMobility() {
    return intensity_IntensityMobility;
  }
}
