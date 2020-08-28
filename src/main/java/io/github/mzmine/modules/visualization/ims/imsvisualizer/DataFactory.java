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
package io.github.mzmine.modules.visualization.ims.imsvisualizer;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerParameters;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerTask;
import io.github.mzmine.parameters.ParameterSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataFactory {

    private final Scan[] scans;
    private final Range<Double> mzRange;
    private double[] mobilityretentionTimeMobility;
    private double[] intensityretentionTimeMobility;
    private double[] retentionTimeretentionTimeMobility;

    private double[] intensityretentionTimeIntensity;
    private double[] retentionTimeretentionTimeIntensity;

    private Double[] mobilityMzMobility;
    private Double[] mzMzMobility;
    private Double[] intensityMzMobility;

    private Double[] intensityIntensityMobility;
    private Double[] mobilityIntensityMobility;

    public Map<Double, Double> scanMobilityMap = new HashMap<>();
    public Map<Double, Double> scanRetentionTimeMap = new HashMap<>();
    private Double selectedRetentionTime;
    private final int scanSize;
    private final ImsVisualizerTask imsTask;
    List<Double> rtIntensity = new ArrayList<>();


    public DataFactory(ParameterSet param, double rt, ImsVisualizerTask imsTask) {
        RawDataFile[] dataFiles = param
                .getParameter(ImsVisualizerParameters.dataFiles)
                .getValue()
                .getMatchingRawDataFiles();

        scans =
                param
                        .getParameter(ImsVisualizerParameters.scanSelection)
                        .getValue()
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
        intensityretentionTimeIntensity = new double[rtIntensity.size()];
        retentionTimeretentionTimeIntensity = new double[rtIntensity.size()];

        // get the unique retention time.
        for (int i = 0; i < rtIntensity.size(); i++) {
            retentionTimeretentionTimeIntensity[i] = rtIntensity.get(i);
        }
    /*
       scanRetentionTimeMap : Here scanRetentionTimeMap contains retentionTime as key and values as the sum of all the intensity at
       that that retentionTime.
    */
        for (int i = 0; i < rtIntensity.size(); i++) {

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
        retentionTimeretentionTimeMobility = new double[scanSize];
        mobilityretentionTimeMobility = new double[scanSize];
        intensityretentionTimeMobility = new double[scanSize];

        for (int i = 0; i < scanSize; i++) {
            if (i == 0) {
                rtIntensity.add(scans[i].getRetentionTime());
            } else {
                if (scans[i].getRetentionTime() != scans[i - 1].getRetentionTime()) {
                    rtIntensity.add(scans[i].getRetentionTime());
                }
            }
            retentionTimeretentionTimeMobility[i] = scans[i].getRetentionTime();
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
                    mzMobilityFields.add(
                            new MzMobilityFields(
                                    scans[i].getMobility(), dataPoint[j].getMZ(), dataPoint[j].getIntensity()));
                }
                intensityMobilityFields.add(
                        new IntensityMobilityFields(scans[i].getMobility(), intensitySum));
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

    /*
    get all the unique mobilities in all scan
    */
    public double[] getMobilityretentionTimeMobility() {
        return mobilityretentionTimeMobility;
    }

    /*
     Return mobilities for retentionTime-mobility heat map.
    */
    public double[] getRetentionTimeretentionTimeIntensity() {
        return retentionTimeretentionTimeIntensity;
    }

    public Double[] getMobilityMzMobility() {
        return mobilityMzMobility;
    }

    public Double[] getMobilityIntensityMobility() {
        return mobilityIntensityMobility;
    }

    /*
     get the all unique retention times in all   scan
    */
    public double[] getIntensityretentionTimeMobility() {
        return intensityretentionTimeMobility;
    }

    /*
     get all the intensities value at unique mobilities.
    */
    public double[] getIntensityretentionTimeIntensity() {
        return intensityretentionTimeIntensity;
    }

    /*
     get the all the intensities values at unique retention time
    */
    public double[] getRetentionTimeretentionTimeMobility() {
        return retentionTimeretentionTimeMobility;
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
