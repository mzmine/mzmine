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

package io.github.mzmine.modules.tools.qualityparameters;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.features.types.numbers.FwhmType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.TailingFactorType;
import io.github.mzmine.util.DataPointUtils;
import java.util.List;

/**
 * Calculates quality parameters for each peak in a feature list: - Full width at half maximum
 * (FWHM) - Tailing Factor - Asymmetry factor
 */
public class QualityParameters {

  public static void calculateAndSetModularQualityParameters(ModularFeatureList flist) {
    // add quality columns to flist - feature columns
    flist.addFeatureType(new FwhmType(), new AsymmetryFactorType(), new TailingFactorType());

    flist.streamFeatures().forEach(peak -> {
      Float height = peak.getHeight();
      Float rt = peak.getRT();

      List<Scan> scanNumbers = peak.getScanNumbers();
      RawDataFile dataFile = peak.getRawDataFile();
      IonTimeSeries<? extends Scan> dps = peak.getFeatureData();
      if (height == null || rt == null || dataFile == null
          || scanNumbers.isEmpty() || dps.getNumberOfValues() < 3) {
        return;
      }

      if (dps.getNumberOfValues() < 3) {
        return;
      }

      Range<Float> rtRange = peak.get(RTRangeType.class);
      if (rtRange == null) {
        rtRange = Range.singleton(rt);
      }

      height = peak.getHeight();
      rt = peak.getRT();
      double[] intensities = DataPointUtils.getDoubleBufferAsArray(dps.getIntensityValueBuffer());

      // FWHM
      double[] rtValues =
          peakFindRTs(height / 2.0, rt, scanNumbers, intensities, dataFile,
              rtRange);
      Double fwhm = rtValues[1] - rtValues[0];
      if (fwhm <= 0 || Double.isNaN(fwhm) || Double.isInfinite(fwhm)) {
        fwhm = null;
      }
      if (fwhm != null) {
        peak.set(FwhmType.class, (fwhm.floatValue()));
      }

      // Tailing Factor - TF
      double[] rtValues2 =
          peakFindRTs(height * 0.05, rt, scanNumbers, intensities,
              dataFile, rtRange);
      Double tf = (rtValues2[1] - rtValues2[0]) / (2 * (rt - rtValues2[0]));
      if (tf <= 0 || Double.isNaN(tf) || Double.isInfinite(tf)) {
        tf = null;
      }
      if (tf != null) {
        peak.set(TailingFactorType.class, (tf.floatValue()));
      }

      // Asymmetry factor - AF
      double[] rtValues3 =
          peakFindRTs(height * 0.1, rt, scanNumbers, intensities, dataFile,
              rtRange);
      Double af = (rtValues3[1] - rt) / (rt - rtValues3[0]);
      if (af <= 0 || Double.isNaN(af) || Double.isInfinite(af)) {
        af = null;
      }
      if (af != null) {
        peak.set(AsymmetryFactorType.class, (af.floatValue()));
      }
    });
  }

  public static float calculateFWHM(Feature feature) {
    if (feature == null) {
      return Float.NaN;
    }
    Float height = feature.getHeight();
    Float rt = feature.getRT();

    List<Scan> scanNumbers = feature.getScanNumbers();
    RawDataFile dataFile = feature.getRawDataFile();
    double[] intensities = DataPointUtils
        .getDoubleBufferAsArray(feature.getFeatureData().getIntensityValueBuffer());
    if (height == null || rt == null || dataFile == null
        || scanNumbers.isEmpty() || intensities.length == 0) {
      throw new IllegalArgumentException("Modular feature values are not initialized.");
    }

    if (intensities.length < 3) {
      return Float.NaN;
    }

    Range<Float> rtRange = feature.getRawDataPointsRTRange();
    if (rtRange == null) {
      rtRange = Range.singleton(rt);
    }

    height = feature.getHeight();
    rt = feature.getRT();

    // FWHM
    double[] rtValues =
        peakFindRTs(height / 2.0, rt, scanNumbers, intensities, dataFile, rtRange);
    if (rtValues.length < 2) {
      return Float.NaN;
    }
    double fwhm = rtValues[1] - rtValues[0];
    if (fwhm <= 0 || Double.isInfinite(fwhm)) {
      return Float.NaN;
    }

    return (float) fwhm;
  }

  public static float calculateTailingFactor(Feature feature) {
    if (feature == null) {
      return Float.NaN;
    }
    Float height = feature.getHeight();
    Float rt = feature.getRT();

    List<Scan> scanNumbers = feature.getScanNumbers();
    RawDataFile dataFile = feature.getRawDataFile();
    double[] intensities = DataPointUtils
        .getDoubleBufferAsArray(feature.getFeatureData().getIntensityValueBuffer());

    if (height == null || rt == null || dataFile == null
        || scanNumbers.isEmpty() || intensities.length == 0) {
      throw new IllegalArgumentException("Modular feature values are not initialized.");
    }

    if (intensities.length < 3) {
      return Float.NaN;
    }

    Range<Float> rtRange = feature.getRawDataPointsRTRange();
    if (rtRange == null) {
      rtRange = Range.singleton(rt);
    }

    height = feature.getHeight();
    rt = feature.getRT();

    // Tailing Factor - TF
    double[] rtValues =
        peakFindRTs(height * 0.05, rt, scanNumbers, intensities, dataFile, rtRange);
    double tf = (rtValues[1] - rtValues[0]) / (2 * (rt - rtValues[0]));
    if (tf <= 0 || Double.isInfinite(tf)) {
      return Float.NaN;
    }

    return (float) tf;
  }

  public static float calculateAsymmetryFactor(Feature feature) {
    if (feature == null) {
      return Float.NaN;
    }
    Float height = feature.getHeight();
    Float rt = feature.getRT();

    List<Scan> scanNumbers = feature.getScanNumbers();
    RawDataFile dataFile = feature.getRawDataFile();
    double[] intensities = DataPointUtils
        .getDoubleBufferAsArray(feature.getFeatureData().getIntensityValueBuffer());
    if (height == null || rt == null || dataFile == null
        || scanNumbers.isEmpty() || intensities.length == 0) {
      throw new IllegalArgumentException("Modular feature values are not initialized.");
    }
    if (intensities.length < 3) {
      return Float.NaN;
    }

    Range<Float> rtRange = feature.getRawDataPointsRTRange();
    if (rtRange == null) {
      rtRange = Range.singleton(rt);
    }

    height = feature.getHeight();
    rt = feature.getRT();

    // Asymmetry factor - AF
    double[] rtValues =
        peakFindRTs(height * 0.1, rt, scanNumbers, intensities, dataFile, rtRange);
    double af = (rtValues[1] - rt) / (rt - rtValues[0]);
    if (af <= 0 || Double.isInfinite(af)) {
      af = Double.NaN;
    }

    return (float) af;
  }

  private static double[] peakFindRTs(double intensity, float featureRT, List<Scan> scanNumbers,
      List<DataPoint> dps, RawDataFile dataFile, Range<Float> rtRange) {
    return peakFindRTs(intensity, featureRT,
        scanNumbers, dps.toArray(DataPoint[]::new), //
        dataFile, //
        Range.closed(rtRange.lowerEndpoint().doubleValue(), rtRange.upperEndpoint().doubleValue()));
  }

  private static double[] peakFindRTs(double intensity, double featureRT, List<Scan> scanNumbers,
      DataPoint[] dps, RawDataFile dataFile, Range<Double> rtRange) {

    assert scanNumbers != null;
    assert dps != null;
    assert dataFile != null;
    assert rtRange != null;

    double x1 = 0, x2 = 0, x3 = 0, x4 = 0, y1 = 0, y2 = 0, y3 = 0, y4 = 0;
    double lastDiff1 = intensity;
    double lastDiff2 = intensity;
    double currentDiff;
    double currentRT;

    // Find the data points closet to input intensity on both side of the
    // peak apex
    DataPoint lastDP = dps[0];
    double lastRT = scanNumbers.get(0).getRetentionTime();
    DataPoint dp = dps[1];
    double rt = scanNumbers.get(1).getRetentionTime();
    for (int i = 1; i < scanNumbers.size() - 1; i++) {
      DataPoint nextDP = dps[i + 1];
      double nextRT = scanNumbers.get(i + 1).getRetentionTime();

      if (dp != null) {
        currentDiff = Math.abs(intensity - dp.getIntensity());
        currentRT = scanNumbers.get(i).getRetentionTime();
        if (currentDiff < lastDiff1 && currentDiff > 0 && currentRT <= featureRT
            && nextDP != null) {
          x1 = rt;
          y1 = dp.getIntensity();
          x2 = nextRT;
          y2 = nextDP.getIntensity();
          lastDiff1 = currentDiff;
        } else if (currentDiff < lastDiff2 && currentDiff > 0 && currentRT >= featureRT
                   && lastDP != null) {
          x3 = lastRT;
          y3 = lastDP.getIntensity();
          x4 = rt;
          y4 = dp.getIntensity();
          lastDiff2 = currentDiff;
        }
      }
      // set to next
      lastDP = dp;
      lastRT = rt;
      dp = nextDP;
      rt = nextRT;
    }

    // Calculate RT value for input intensity based on linear regression
    double slope, intercept, rt1, rt2;
    if (y1 > 0) {
      slope = (y2 - y1) / (x2 - x1);
      intercept = y1 - (slope * x1);
      rt1 = (intensity - intercept) / slope;
    } else if (x2 > 0) { // Straight drop of peak to 0 intensity
      rt1 = x2;
    } else {
      rt1 = rtRange.lowerEndpoint();
    }
    if (y4 > 0) {
      slope = (y4 - y3) / (x4 - x3);
      intercept = y3 - (slope * x3);
      rt2 = (intensity - intercept) / slope;
    } else if (x3 > 0) { // Straight drop of peak to 0 intensity
      rt2 = x3;
    } else {
      rt2 = rtRange.upperEndpoint();
    }

    return new double[]{rt1, rt2};
  }

  private static double[] peakFindRTs(double intensity, double featureRT, List<Scan> scanNumbers,
      double[] intensities, RawDataFile dataFile, Range<Float> rtRange) {

    assert scanNumbers != null;
    assert intensities != null;
    assert dataFile != null;
    assert rtRange != null;

    double y1 = 0, y2 = 0, y3 = 0, y4 = 0;
    float x1 = 0, x2 = 0, x3 = 0, x4 = 0;
    double lastDiff1 = intensity;
    double lastDiff2 = intensity;
    double currentDiff;
    float currentRT;

    if (intensities.length < 2) {
      return new double[]{featureRT};
    }

    // Find the data points closet to input intensity on both side of the
    // peak apex
//    DataPoint lastDP = dps[0];
    double lastIntensity = intensities[0];
    float lastRT = scanNumbers.get(0).getRetentionTime();
//    DataPoint dp = dps[1];
    double currentIntensity = intensities[1];
    float rt = scanNumbers.get(1).getRetentionTime();
    for (int i = 1; i < scanNumbers.size() - 1; i++) {
//      DataPoint nextDP = dps[i + 1];
      double nextIntensity = intensities[i + 1];
      float nextRT = scanNumbers.get(i + 1).getRetentionTime();

//      if (dp != null) {
      currentDiff = Math.abs(intensity - currentIntensity);
      currentRT = scanNumbers.get(i).getRetentionTime();
      if (currentDiff < lastDiff1 && currentDiff > 0 && currentRT <= featureRT
        /*&& nextDP != null*/) {
        x1 = rt;
        y1 = currentIntensity;
        x2 = nextRT;
        y2 = nextIntensity;
        lastDiff1 = currentDiff;
      } else if (currentDiff < lastDiff2 && currentDiff > 0 && currentRT >= featureRT
        /* && lastDP != null*/) {
        x3 = lastRT;
        y3 = lastIntensity;
        x4 = rt;
        y4 = currentIntensity;
        lastDiff2 = currentDiff;
      }
//      }
      // set to next
//      lastDP = dp;
      lastIntensity = currentIntensity;
      lastRT = rt;
//      dp = nextDP;
      currentIntensity = nextIntensity;
      rt = nextRT;
    }

    // Calculate RT value for input intensity based on linear regression
    double slope, intercept, rt1, rt2;
    if (y1 > 0) {
      slope = (y2 - y1) / (x2 - x1);
      intercept = y1 - (slope * x1);
      rt1 = (intensity - intercept) / slope;
    } else if (x2 > 0) { // Straight drop of peak to 0 intensity
      rt1 = x2;
    } else {
      rt1 = rtRange.lowerEndpoint();
    }
    if (y4 > 0) {
      slope = (y4 - y3) / (x4 - x3);
      intercept = y3 - (slope * x3);
      rt2 = (intensity - intercept) / slope;
    } else if (x3 > 0) { // Straight drop of peak to 0 intensity
      rt2 = x3;
    } else {
      rt2 = rtRange.upperEndpoint();
    }

    return new double[]{rt1, rt2};
  }
}
