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

/*
 * This module is going to prepare all the data sets required by ion-mobility and it's
 * visualizations(ims)
 *
 */
package io.github.mzmine.modules.visualization.ims.imsvisualizer;
/*
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerParameters;
import io.github.mzmine.parameters.ParameterSet;

public class DataFactory {

  private final Scan[] scans;
  private final Range<Double> mzRange;
  private Double[] mobilityretentionTimeMobility;
  private Double[] intensityretentionTimeMobility;
  private Double[] retentionTimeretentionTimeMobility;

  private Double[] intensityretentionTimeIntensity;
  private Double[] retentionTimeretentionTimeIntensity;

  private Double[] mobilityMzMobility;
  private Double[] mzMzMobility;
  private Double[] intensityMzMobility;

  private Double[] intensityIntensityMobility;
  private Double[] mobilityIntensityMobility;

  public Map<Double, Double> scanMobilityMap = new HashMap<>();
  public Map<Float, Double> scanRetentionTimeMap = new HashMap<>();
  private Float selectedRetentionTime;
  private final int scanSize;
  private final ImsVisualizerTask imsTask;
  List<Float> rtIntensity = new ArrayList<>();


  public DataFactory(ParameterSet param, float rt, ImsVisualizerTask imsTask) {
    RawDataFile[] dataFiles =
        param.getParameter(ImsVisualizerParameters.dataFiles).getValue().getMatchingRawDataFiles();

    scans = param.getParameter(ImsVisualizerParameters.scanSelection).getValue()
        .getMatchingScans(dataFiles[0]);

    mzRange = param.getParameter(ImsVisualizerParameters.mzRange).getValue();

    this.selectedRetentionTime = rt;
    this.scanSize = scans.length;
    this.imsTask = imsTask;

    // if there is not selected any retention time.select the rt for max intensity.
    if (selectedRetentionTime == 0.0) {
      prepareDataAtSelectedRT();
    }

    // prepare data's for the ims frame.( mz-mobility heatmap and intensity mobility)
    updateFrameData(selectedRetentionTime);

    preparertMobility();

    prepareIntensityRetentionTime();
  }

  void prepareIntensityRetentionTime() {
    intensityretentionTimeIntensity = new Double[rtIntensity.size()];
    retentionTimeretentionTimeIntensity = new Double[rtIntensity.size()];

    // get the unique retention time.
    for (int i = 0; i < rtIntensity.size(); i++) {
      retentionTimeretentionTimeIntensity[i] = rtIntensity.get(i).doubleValue();
    }
    /*
     * scanRetentionTimeMap : Here scanRetentionTimeMap contains retentionTime as key and values as
     * the sum of all the intensity at that that retentionTime.
     */
/*
import java.util.ArrayList;for (int i = 0; i < rtIntensity.size(); i++) {

      if (scanRetentionTimeMap.get(rtIntensity.get(i)) != null) {
        intensityretentionTimeIntensity[i] = scanRetentionTimeMap.get(rtIntensity.get(i));
      }
    }
  }

  public void prepareDataAtSelectedRT() {
    double maxIntensity = 0.0;
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

    imsTask.setSelectedRetentionTime(selectedRetentionTime);
  }

  public void preparertMobility() {

    // data for the retention time - heatMap plot.
    retentionTimeretentionTimeMobility = new Double[scanSize];
    mobilityretentionTimeMobility = new Double[scanSize];
    intensityretentionTimeMobility = new Double[scanSize];

    for (int i = 0; i < scanSize; i++) {
      if (i == 0) {
        rtIntensity.add(scans[i].getRetentionTime());
      } else {
        if (scans[i].getRetentionTime() != scans[i - 1].getRetentionTime()) {
          rtIntensity.add(scans[i].getRetentionTime());
        }
      }
      Float rt = scans[i].getRetentionTime();
      retentionTimeretentionTimeMobility[i] = rt.doubleValue();
      mobilityretentionTimeMobility[i] = scans[i].getMobility();
      double intensitySum = 0;
      DataPoint dataPoint[] = scans[i].getDataPointsByMass(mzRange);
      for (int j = 0; j < dataPoint.length; j++) {
        intensityretentionTimeMobility[i] += dataPoint[j].getIntensity();
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
  }

  public void updateFrameData(double selectedRetentionTime) {

    class MzMobilityFields {
      public double mobility;
      public double mz;
      public double intensity;

      public MzMobilityFields(double mobility, double mz, double intensity) {

        this.mobility = mobility;
        this.mz = mz;
        this.intensity = intensity;
      }
    }

    class IntensityMobilityFields {
      public double mobility;
      public double intensity;

      public IntensityMobilityFields(double mobility, double intensity) {
        this.intensity = intensity;
        this.mobility = mobility;
      }
    }

    List<Scan> selectedScans = new ArrayList<>();
    // ims frame.
    ArrayList<MzMobilityFields> mzMobilityFields = new ArrayList<>();
    ArrayList<IntensityMobilityFields> intensityMobilityFields = new ArrayList<>();
    for (int i = 0; i < scanSize; i++) {
      if (scans[i].getRetentionTime() == selectedRetentionTime) {
        selectedScans.add(scans[i]);
        double intensitySum = 0;
        DataPoint dataPoint[] = scans[i].getDataPointsByMass(mzRange);
        for (int j = 0; j < dataPoint.length; j++) {
          intensitySum += dataPoint[j].getIntensity();
          mzMobilityFields.add(new MzMobilityFields(scans[i].getMobility(), dataPoint[j].getMZ(),
              dataPoint[j].getIntensity()));
        }
        intensityMobilityFields
            .add(new IntensityMobilityFields(scans[i].getMobility(), intensitySum));
      }
    }

    mobilityMzMobility = new Double[mzMobilityFields.size()];
    mzMzMobility = new Double[mzMobilityFields.size()];
    intensityMzMobility = new Double[mzMobilityFields.size()];

    for (int i = 0; i < mzMobilityFields.size(); i++) {
      mobilityMzMobility[i] = mzMobilityFields.get(i).mobility;
      mzMzMobility[i] = mzMobilityFields.get(i).mz;
      intensityMzMobility[i] = mzMobilityFields.get(i).intensity;
    }

    intensityIntensityMobility = new Double[intensityMobilityFields.size()];
    mobilityIntensityMobility = new Double[intensityMobilityFields.size()];
    for (int i = 0; i < intensityMobilityFields.size(); i++) {
      intensityIntensityMobility[i] = intensityMobilityFields.get(i).intensity;
      mobilityIntensityMobility[i] = intensityMobilityFields.get(i).mobility;
    }

    imsTask.setSelectedScans(selectedScans);
  }

  public Double[] getMobilityretentionTimeMobility() {
    return mobilityretentionTimeMobility;
  }

  public Double[] getRetentionTimeretentionTimeIntensity() {
    return retentionTimeretentionTimeIntensity;
  }

  public Double[] getMobilityMzMobility() {
    return mobilityMzMobility;
  }

  public Double[] getMobilityIntensityMobility() {
    return mobilityIntensityMobility;
  }

  public Double[] getIntensityretentionTimeMobility() {
    return intensityretentionTimeMobility;
  }

  public Double[] getIntensityretentionTimeIntensity() {
    return intensityretentionTimeIntensity;
  }

  public Double[] getRetentionTimeretentionTimeMobility() {
    return retentionTimeretentionTimeMobility;
  }

  public Scan[] getScans() {
    return scans;
  }

  public Range getmzRange() {
    return mzRange;
  }

  public Double[] getMzMzMobility() {
    return mzMzMobility;
  }

  public Double[] getIntensityMzMobility() {
    return intensityMzMobility;
  }

  public Double[] getIntensityIntensityMobility() {
    return intensityIntensityMobility;
  }

}
*/
