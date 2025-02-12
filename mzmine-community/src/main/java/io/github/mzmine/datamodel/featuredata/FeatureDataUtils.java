/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.featuredata;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.impl.StorageUtils;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.features.types.numbers.FwhmType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.TailingFactorType;
import io.github.mzmine.datamodel.features.types.otherdectectors.ChromatogramTypeType;
import io.github.mzmine.datamodel.features.types.otherdectectors.OtherFileType;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used to uniformly calculate feature describing values by the series stored in
 * {@link ModularFeature#getFeatureData()} (and it's super/extending classes).
 *
 * @author https://github.com/SteffenHeu
 */
public class FeatureDataUtils {

  /**
   * The default {@link CenterMeasure} for weighting and calculating feature m/z values.
   */
  public static final CenterMeasure DEFAULT_CENTER_MEASURE = CenterMeasure.AVG;
  public static final Weighting DEFAULT_WEIGHTING = Weighting.LINEAR;
  public static final CenterFunction DEFAULT_CENTER_FUNCTION = new CenterFunction(
      DEFAULT_CENTER_MEASURE, DEFAULT_WEIGHTING);

  private static final Logger logger = Logger.getLogger(FeatureDataUtils.class.getName());

  /**
   * The Rt range of the series.
   *
   * @param series The series, sorted ascending.
   * @return The RT range or null, if there are no scans in the series.
   */
  @Nullable
  public static Range<Float> getRtRange(IntensityTimeSeries series) {
    final int numValues = series.getNumberOfValues();
    return numValues == 0 ? null
        : Range.closed(series.getRetentionTime(0), series.getRetentionTime(numValues - 1));
  }

  /**
   * @param series The m/z series
   * @return m/z range of the given series. For {@link IonMobilogramTimeSeries}, the underlying
   * {@link IonMobilitySeries} are investigated. Null if no range can be specified.
   */
  @Nullable
  public static Range<Double> getMzRange(MzSeries series) {
    double min = Double.MAX_VALUE;
    double max = Double.NEGATIVE_INFINITY;

    if (series instanceof IonMobilogramTimeSeries ionTrace) {
      for (IonMobilitySeries mobilogram : ionTrace.getMobilograms()) {
        for (int i = 0; i < mobilogram.getNumberOfValues(); i++) {
          final double mz = mobilogram.getMZ(i);
          // we add flanking 0 intensities with 0d mz during building, don't count those
          if (mz < min && Double.compare(mz, 0d) > 0) {
            min = mz;
          }
          if (mz > max) {
            max = mz;
          }
        }
      }
    } else {
      if (series.getNumberOfValues() == 1) {
        return Range.singleton(series.getMZ(0));
      }

      for (int i = 0; i < series.getNumberOfValues(); i++) {
        final double mz = series.getMZ(i);
        // we add flanking 0 intesities with 0d mz during building, don't count those
        if (mz < min && Double.compare(mz, 0d) > 0) {
          min = mz;
        }
        if (mz > max) {
          max = mz;
        }
      }
    }
    if (Double.compare(min, max) == 0) {
      return Range.singleton(min);
    }
    return min < max ? Range.closed(min, max) : null;
  }

  /**
   * @param series The intensity series.
   * @return The intensity range of the series. Null if no range can be specified.
   */
  public static Range<Float> getIntensityRange(IntensitySeries series) {
    double min = Double.MAX_VALUE;
    double max = Double.NEGATIVE_INFINITY;

    if (series.getNumberOfValues() == 1) {
      return Range.singleton((float) series.getIntensity(0));
    }

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
    if (min == max) {
      return Range.singleton((float) min);
    }
    return min < max ? Range.closed((float) min, (float) max) : null;
  }

  /**
   * Caclualtes the highest point of the intensity series. Usually, this method is not needed
   * because {@link #getIntensityRange(IntensitySeries)} returns more information.
   */
  public static float getHeight(IntensitySeries series) {
    final Range<Float> range = getIntensityRange(series);
    if (range == null) {
      return 0f;
    }
    return range.upperEndpoint();
  }

  /**
   * @param series The series. Ascending or descending mobility.
   * @return The mobility range. Null if no range can be specified.
   */
  @Nullable
  public static Range<Float> getMobilityRange(MobilitySeries series) {
    if (series.getNumberOfValues() == 0) {
      return null;
    }
    return Range.singleton((float) series.getMobility(0))
        .span(Range.singleton((float) series.getMobility(series.getNumberOfValues() - 1)));
  }

  /**
   * @param series The series.
   * @return The index of the highest intensity. May be -1 if no intensity could be found (-> series
   * empty).
   */
  public static int getMostIntenseIndex(IntensitySeries series) {
    int maxIndex = -1;
    double maxIntensity = Double.NEGATIVE_INFINITY;

    for (int i = 0; i < series.getNumberOfValues(); i++) {
      final double intensity = series.getIntensity(i);
      if (intensity > maxIntensity) {
        maxIndex = i;
        maxIntensity = intensity;
      }
    }
    return maxIndex;
  }

  /**
   * @return The most intense spectrum in the series or null.
   */
  @Nullable
  public static MassSpectrum getMostIntenseSpectrum(
      IonSpectrumSeries<? extends MassSpectrum> series) {
    final int maxIndex = getMostIntenseIndex(series);
    return maxIndex != -1 ? series.getSpectrum(maxIndex) : null;
  }

  /**
   * @return The most intense scan in the series or null.
   */
  @Nullable
  public static Scan getMostIntenseScan(IonTimeSeries<? extends Scan> series) {
    return (Scan) getMostIntenseSpectrum(series);
  }

  /**
   * @param series The series.
   * @return The area of the given series in retention time dimension (= for
   * {@link IonMobilogramTimeSeries}, the intensities in one frame are summed.).
   */
  public static float calculateArea(IntensityTimeSeries series) {
    if (series.getNumberOfValues() <= 1) {
      return 0f;
    }
    float area = 0f;
    double lastIntensity = series.getIntensity(0);
    float lastRT = series.getRetentionTime(0);
    for (int i = 1; i < series.getNumberOfValues(); i++) {
      final double thisIntensity = series.getIntensity(i);
      final float thisRT = series.getRetentionTime(i);
      area += (thisRT - lastRT) * ((float) (thisIntensity + lastIntensity)) / 2.0;
      lastIntensity = thisIntensity;
      lastRT = thisRT;
    }
    return area;
  }

  /**
   * Calculates the m/z of the given series.
   *
   * @param series The series.
   * @param cf     The center function ({@link #DEFAULT_CENTER_FUNCTION} default)
   * @return The m/z value
   */
  public static double calculateCenterMz(@NotNull final IonSeries series,
      @NotNull final CenterFunction cf) {
    double[][] data = DataPointUtils.getDataPointsAsDoubleArray(series.getMZValueBuffer(),
        series.getIntensityValueBuffer());
    return cf.calcCenter(data[0], data[1]);
  }

  /**
   * Calculates the m/z of the given series.
   *
   * @param series The series.
   * @param cf     The center function ({@link #DEFAULT_CENTER_FUNCTION} default)
   * @return The m/z value
   */
  public static double calculateCenterMz(@NotNull final IonSeries series,
      @NotNull final CenterFunction cf, int startInclusive, int endInclusive) {
    return cf.calcCenter(
        StorageUtils.copyOfRangeDouble(series.getMZValueBuffer(), startInclusive, endInclusive + 1),
        StorageUtils.copyOfRangeDouble(series.getIntensityValueBuffer(), startInclusive,
            endInclusive + 1));
  }

  /**
   * Calculates the all feature for the given feature.
   *
   * @param feature The feature.
   */
  public static void recalculateIonSeriesDependingTypes(@NotNull final ModularFeature feature) {
    recalculateIonSeriesDependingTypes(feature, DEFAULT_CENTER_FUNCTION, true);
  }

  public static void recalculateIntensityTimeSeriesDependingTypes(@NotNull OtherFeature feature) {
    final OtherTimeSeries featureData = feature.getFeatureData();
    if (featureData == null) {
      return;
    }

    feature.set(OtherFileType.class, featureData.getOtherDataFile());
    feature.set(ChromatogramTypeType.class, featureData.getChromatoogramType());

    final int mostIntenseIndex = getMostIntenseIndex(featureData);
    var intensityRange = getIntensityRange(featureData);
    feature.set(IntensityRangeType.class, intensityRange);
    feature.set(AreaType.class, calculateArea(featureData));
    if (mostIntenseIndex >= 0) {
      feature.set(HeightType.class, (float) featureData.getIntensity(mostIntenseIndex));
      feature.set(RTType.class, featureData.getRetentionTime(mostIntenseIndex));
    }
    feature.set(RTRangeType.class, getRtRange(featureData));
  }

  /**
   * @param feature          The feature
   * @param mzCenterFunction Center function for m/z calculation. Default =
   *                         {@link FeatureDataUtils#DEFAULT_CENTER_FUNCTION}
   * @param calcQuality      specifies if quality parameters (FWHM, asymmetry, tailing) shall be
   *                         calculated.
   */
  public static void recalculateIonSeriesDependingTypes(@NotNull final ModularFeature feature,
      @NotNull final CenterFunction mzCenterFunction, boolean calcQuality) {
    final IonTimeSeries<? extends Scan> featureData = feature.getFeatureData();
    if (featureData == null) {
      return;
    }
    final Range<Float> intensityRange = FeatureDataUtils.getIntensityRange(featureData);
    final Range<Double> mzRange = FeatureDataUtils.getMzRange(featureData);
    final Range<Float> rtRange = FeatureDataUtils.getRtRange(featureData);
    final Scan mostIntenseSpectrum = FeatureDataUtils.getMostIntenseScan(featureData);
    final float area = FeatureDataUtils.calculateArea(featureData);

    feature.set(AreaType.class, area);
    feature.set(MZRangeType.class, mzRange);
    feature.set(RTRangeType.class, rtRange);
    feature.set(IntensityRangeType.class, intensityRange);
    feature.setRepresentativeScan(mostIntenseSpectrum);
    feature.setHeight(intensityRange != null ? intensityRange.upperEndpoint() : 0f);
    feature.setRT(mostIntenseSpectrum != null ? mostIntenseSpectrum.getRetentionTime() : Float.NaN);
    feature.setMZ(calculateCenterMz(featureData, mzCenterFunction));

    if (featureData instanceof IonMobilogramTimeSeries imts) {
      final SummedIntensityMobilitySeries summedMobilogram = imts.getSummedMobilogram();
      feature.setMobilityRange(getMobilityRange(summedMobilogram));
      feature.setMobility(calculateMobility(summedMobilogram));
      feature.setMobilityUnit(((IMSRawDataFile) feature.getRawDataFile()).getMobilityType());
    }

    if (calcQuality) {
      calculateQualityParameters(feature);
    }
  }

  /**
   * @param series the series
   * @param <T>    Any series extending {@link IntensitySeries} and {@link MobilitySeries}.
   * @return The mobility value (highest intensity).
   */
  public static <T extends IntensitySeries & MobilitySeries> float calculateMobility(T series) {
    int mostIntenseMobilityScanIndex = -1;
    double intensity = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < series.getNumberOfValues(); i++) {
      double currentIntensity = series.getIntensity(i);
      if (currentIntensity > intensity) {
        intensity = currentIntensity;
        mostIntenseMobilityScanIndex = i;
      }
    }

    if (Double.compare(intensity, Double.NEGATIVE_INFINITY) == 0) {
      logger.info(() -> "Mobility cannot be specified for: " + series);
      return Float.NaN;
    } else {
      return (float) series.getMobility(mostIntenseMobilityScanIndex);
    }
  }

  public static double getSmallestMzDelta(@NotNull final MzSeries series) {
    double smallestDelta = Double.POSITIVE_INFINITY;

    if (series instanceof IonMobilogramTimeSeries ims) {
      int maxValues = ims.getMobilograms().stream().mapToInt(IonMobilitySeries::getNumberOfValues)
          .max().orElse(0);

      double[] mzBuffer = new double[maxValues];
      for (IonMobilitySeries mobilogram : ims.getMobilograms()) {
        mobilogram.getMzValues(mzBuffer);
        final double delta = ArrayUtils.smallestDelta(mzBuffer, mobilogram.getNumberOfValues());
        if (delta < smallestDelta) {
          smallestDelta = delta;
        }
      }
    } else {
      double[] mzBuffer = new double[series.getNumberOfValues()];
      series.getMzValues(mzBuffer);
      smallestDelta = ArrayUtils.smallestDelta(mzBuffer);
    }

    return smallestDelta;
  }

  public static double getSmallestRtDelta(@NotNull final TimeSeries series) {
    if (series.getNumberOfValues() <= 1) {
      return 0d;
    }
    double smallestDelta = Double.POSITIVE_INFINITY;

    for (int i = 1; i < series.getNumberOfValues(); i++) {
      final float delta = Math.abs(series.getRetentionTime(i) - series.getRetentionTime(i - 1));
      smallestDelta = Math.min(delta, smallestDelta);
    }

    return smallestDelta;
  }

  private static void calculateQualityParameters(@NotNull ModularFeature feature) {
    float fwhm = QualityParameters.calculateFWHM(feature);
    if (!Float.isNaN(fwhm)) {
      feature.set(FwhmType.class, fwhm);
    }
    float tf = QualityParameters.calculateTailingFactor(feature);
    if (!Float.isNaN(tf)) {
      feature.set(TailingFactorType.class, tf);
    }
    float af = QualityParameters.calculateAsymmetryFactor(feature);
    if (!Float.isNaN(af)) {
      feature.set(AsymmetryFactorType.class, af);
    }
  }
}
