/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.featuredata;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import java.nio.DoubleBuffer;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;

public class FeatureDataUtils {

  private static final Logger logger = Logger.getLogger(FeatureDataUtils.class.getName());

  public static Range<Float> getRtRange(IonTimeSeries<? extends Scan> series) {
    final List<? extends Scan> scans = series.getSpectra();
    return Range
        .closed(scans.get(0).getRetentionTime(), scans.get(scans.size() - 1).getRetentionTime());
  }

  public static Range<Double> getMzRange(MzSeries series) {
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;

    if (series instanceof IonMobilogramTimeSeries ionTrace) {
      for (IonMobilitySeries mobilogram : ionTrace.getMobilograms()) {
        for (int i = 0; i < mobilogram.getNumberOfValues(); i++) {
          final double mz = mobilogram.getMZ(i);
          // we add flanking 0 intensities with 0d mz during building, don't count those
          if (mz < min && Double.compare(mz, 0d) == 1) {
            min = mz;
          }
          if (mz > max) {
            max = mz;
          }
        }
      }
    } else {
      for (int i = 0; i < series.getNumberOfValues(); i++) {
        final double mz = series.getMZ(i);
        // we add flanking 0 intesities with 0d mz during building, don't count those
        if (mz < min && Double.compare(mz, 0d) == 1) {
          min = mz;
        }
        if (mz > max) {
          max = mz;
        }
      }
    }
    return Range.closed(min, max);
  }

  public static Range<Float> getIntensityRange(IntensitySeries series) {
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;

    for (int i = 0; i < series.getNumberOfValues(); i++) {
      final double intensity = series.getIntensity(i);
      // we add flanking 0s during building, don't count those
      if (intensity < min && intensity > 0d) {
        min = intensity;
      }
      if (intensity > max) {
        max = intensity;
      }
    }
    return Range.closed((float) min, (float) max);
  }

  public static Range<Float> getMobilityRange(MobilitySeries series) {
    double min = Double.MAX_VALUE;
    double max = Double.MIN_VALUE;

    for (int i = 0; i < series.getNumberOfValues(); i++) {
      final double mz = series.getMobility(i);
      if (mz < min) {
        min = mz;
      }
      if (mz > max) {
        max = mz;
      }
    }
    return Range.closed((float) min, (float) max);
  }

  @Nullable
  public static MassSpectrum getMostIntenseSpectrum(
      IonSpectrumSeries<? extends MassSpectrum> series) {
    int maxIndex = -1;
    double maxIntensity = Double.MIN_VALUE;

    for (int i = 0; i < series.getNumberOfValues(); i++) {
      final double intensity = series.getIntensity(i);
      if (intensity > maxIntensity) {
        maxIndex = i;
        maxIntensity = intensity;
      }
    }
    return maxIndex != -1 ? series.getSpectrum(maxIndex) : null;
  }

  @Nullable
  public static Scan getMostIntenseScan(IonTimeSeries<? extends Scan> series) {
    return (Scan) getMostIntenseSpectrum(series);
  }

  public static float calculateArea(IonTimeSeries<? extends Scan> series) {
    if (series.getNumberOfValues() <= 1) {
      return 0f;
    }
    float area = 0f;
    DoubleBuffer intensities = series.getIntensityValues();
    List<? extends Scan> scans = series.getSpectra();
    double lastIntensity = intensities.get(0);
    float lastRT = scans.get(0).getRetentionTime();
    for (int i = 1; i < series.getNumberOfValues(); i++) {
      final double thisIntensity = intensities.get(i);
      final float thisRT = scans.get(i).getRetentionTime();
      area += (thisRT - lastRT) * (thisIntensity + lastIntensity) / 2.0 /* 60d*/;
      lastIntensity = thisIntensity;
      lastRT = thisRT;
    }
    return area;
  }

  public static double calculateMz(IonSeries series, CenterMeasure cm) {
    CenterFunction cf = new CenterFunction(cm, Weighting.LINEAR);
    double[][] data = DataPointUtils
        .getDataPointsAsDoubleArray(series.getMZValues(), series.getIntensityValues());
    return cf.calcCenter(data[0], data[1]);
  }

  public static void recalculateIonSeriesDependingTypes(ModularFeature feature) {
    recalculateIonSeriesDependingTypes(feature, CenterMeasure.AVG);
  }

  public static void recalculateIonSeriesDependingTypes(ModularFeature feature, CenterMeasure cm) {
    IonTimeSeries<? extends Scan> featureData = feature.getFeatureData();
    Range<Float> intensityRange = FeatureDataUtils.getIntensityRange(featureData);
    Range<Double> mzRange = FeatureDataUtils.getMzRange(featureData);
    Range<Float> rtRange = FeatureDataUtils.getRtRange(featureData);
    Scan mostIntenseSpectrum = FeatureDataUtils.getMostIntenseScan(featureData);
    float area = FeatureDataUtils.calculateArea(featureData);

    feature.set(AreaType.class, area);
    feature.set(MZRangeType.class, mzRange);
    feature.set(RTRangeType.class, rtRange);
    feature.set(IntensityRangeType.class, intensityRange);
    feature.setRepresentativeScan(mostIntenseSpectrum);
    feature.setHeight(intensityRange.upperEndpoint());
    feature.setRT(mostIntenseSpectrum.getRetentionTime());
    feature.setMZ(calculateMz(featureData, cm));

    if (featureData instanceof IonMobilogramTimeSeries) {
      SummedIntensityMobilitySeries summedMobilogram = ((IonMobilogramTimeSeries) featureData)
          .getSummedMobilogram();
      Range<Float> mobilityRange = getMobilityRange(summedMobilogram);
      feature.setMobilityRange(mobilityRange);

      int mostIntenseMobilityScanIndex = -1;
      double intensity = Double.MIN_VALUE;
      for (int i = 0; i < summedMobilogram.getNumberOfValues(); i++) {
        double currentIntensity = summedMobilogram.getIntensity(i);
        if (currentIntensity > intensity) {
          intensity = currentIntensity;
          mostIntenseMobilityScanIndex = i;
        }
      }
      if(Double.compare(intensity, Double.MIN_VALUE) == 0) {
        logger.info(() -> "Mobility cannot be specified for: " + summedMobilogram.print());
        feature.setMobility(Float.NaN);
      } else {
        feature.setMobility((float) summedMobilogram.getMobility(mostIntenseMobilityScanIndex));
      }
    }
    // todo recalc quality parameters
  }
}
