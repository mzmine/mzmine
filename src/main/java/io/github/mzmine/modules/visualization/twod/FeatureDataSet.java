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

package io.github.mzmine.modules.visualization.twod;

import java.util.Vector;
import org.jfree.data.xy.AbstractXYDataset;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;

/**
 * Picked peaks data set
 */
class FeatureDataSet extends AbstractXYDataset {

  private static final long serialVersionUID = 1L;

  private FeatureList featureList;

  private Feature features[];

  private FeatureDataPoint dataPoints[][];

  FeatureDataSet(RawDataFile dataFile, FeatureList featureList, Range<Float> rtRange,
      Range<Double> mzRange) {

    this.featureList = featureList;

    Vector<Feature> processedFeatures = new Vector<Feature>(1024, 1024);
    Vector<FeatureDataPoint[]> processedFeatureDataPoints =
        new Vector<FeatureDataPoint[]>(1024, 1024);
    Vector<FeatureDataPoint> thisFeatureDataPoints = new Vector<FeatureDataPoint>();

    Feature allFeatures[] = featureList.getFeatures(dataFile).toArray(Feature[]::new);

    for (Feature feature : allFeatures) {

      if (feature instanceof ModularFeature) {
        IonTimeSeries<? extends Scan> featureData = ((ModularFeature) feature).getFeatureData();
        featureData.getSpectra().forEach(scan -> {
          float rt = scan.getRetentionTime();
          if (rtRange.contains(rt) && mzRange.contains(featureData.getIntensityForSpectrum(scan))) {
            FeatureDataPoint newDP = new FeatureDataPoint(scan, rt, new SimpleDataPoint(
                featureData.getMzForSpectrum(scan), featureData.getIntensityForSpectrum(scan)));
            thisFeatureDataPoints.add(newDP);
          }
        });

      } else {
        feature.getScanNumbers().forEach(scan -> {
          DataPoint dp = feature.getDataPoint(scan);
          if (dp != null) {
            float rt = scan.getRetentionTime();
            if (rtRange.contains(rt) && mzRange.contains(dp.getMZ())) {
              FeatureDataPoint newDP = new FeatureDataPoint(scan, rt, dp);
              thisFeatureDataPoints.add(newDP);
            }
          }
        });
      }

      if (thisFeatureDataPoints.size() > 0) {
        FeatureDataPoint dpArray[] = thisFeatureDataPoints.toArray(new FeatureDataPoint[0]);
        processedFeatures.add(feature);
        processedFeatureDataPoints.add(dpArray);
        thisFeatureDataPoints.clear();
      }
    }

    features = processedFeatures.toArray(new Feature[0]);
    dataPoints = processedFeatureDataPoints.toArray(new FeatureDataPoint[0][]);

  }

  @Override
  public int getSeriesCount() {
    return dataPoints.length;
  }

  @Override
  public Comparable<?> getSeriesKey(int series) {
    return series;
  }

  @Override
  public int getItemCount(int series) {
    return dataPoints[series].length;
  }

  public FeatureList getFeatureList() {
    return featureList;
  }

  public Feature getFeature(int series) {
    return features[series];
  }

  public FeatureDataPoint getDataPoint(int series, int item) {
    return dataPoints[series][item];
  }

  @Override
  public Number getX(int series, int item) {
    return dataPoints[series][item].getRT();
  }

  @Override
  public Number getY(int series, int item) {
    return dataPoints[series][item].getMZ();
  }

}
