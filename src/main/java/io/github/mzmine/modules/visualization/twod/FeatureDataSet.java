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
