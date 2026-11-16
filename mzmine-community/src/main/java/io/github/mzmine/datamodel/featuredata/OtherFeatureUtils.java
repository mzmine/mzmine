/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import io.github.mzmine.datamodel.features.types.MsMsInfoType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.types.otherdectectors.ChromatogramTypeType;
import io.github.mzmine.datamodel.features.types.otherdectectors.RawTraceType;
import io.github.mzmine.datamodel.impl.DDAMsMsInfoImpl;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.otherdetectors.OtherDataFile;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeriesData;
import io.github.mzmine.datamodel.otherdetectors.SimpleOtherTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.TraceKey;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import io.github.mzmine.util.MemoryMapStorage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OtherFeatureUtils {

  private OtherFeatureUtils() {

  }

  public static OtherFeature shiftRtAxis(MemoryMapStorage storage, @Nonnull OtherFeature feature,
      float shift) {
    final OtherTimeSeries timeSeries = feature.getFeatureData();

    double[] intensities = new double[timeSeries.getNumberOfValues()];
    timeSeries.getIntensityValues(intensities);

    float[] rts = new float[intensities.length];
    for (int i = 0; i < intensities.length; i++) {
      rts[i] = timeSeries.getRetentionTime(i) + shift;
    }

    final SimpleOtherTimeSeries shifted = new SimpleOtherTimeSeries(storage, rts, intensities,
        timeSeries.getName(), timeSeries.getTimeSeriesData());

    return feature.createSubFeature(shifted);
  }

  public static OtherFeature bin(MemoryMapStorage storage, @Nonnull OtherFeature feature,
      int width) {
    final OtherTimeSeries timeSeries = feature.getFeatureData();

    double[] intensities = new double[timeSeries.getNumberOfValues()];
    timeSeries.getIntensityValues(intensities);

    DoubleArrayList newIntensities = new DoubleArrayList();
    FloatArrayList newRts = new FloatArrayList();
    for (int i = 0; i < intensities.length; i += width) {
      double summedIntensity = 0;
      float summedRt = 0;
      int lastJ = 0;
      for (int j = 0; j < width && i + j < intensities.length; j++) {
        summedIntensity += intensities[i + j];
        summedRt += timeSeries.getRetentionTime(i + j);
        lastJ = j;
      }
      newIntensities.add(summedIntensity / (lastJ + 1));
      newRts.add(summedRt / (lastJ + 1));
    }

    final SimpleOtherTimeSeries shifted = new SimpleOtherTimeSeries(storage, newRts.toFloatArray(),
        newIntensities.toDoubleArray(), timeSeries.getName(), timeSeries.getTimeSeriesData());

    return feature.createSubFeature(shifted);
  }

  /**
   *
   * @param q1      The q1 mass. Set as precursor mass of the
   *                {@link io.github.mzmine.datamodel.msms.DDAMsMsInfo} in the
   *                {@link MsMsInfoType}.
   * @param q3      the Q3 set mass. Set as {@link MZType} of the {@link OtherFeature}.
   * @param method  The activation method or null.
   * @param energy  The activation energy or null.
   * @param feature The other feature.
   *
   * @Note When the {@link OtherFeature} is converted to a
   * {@link io.github.mzmine.datamodel.features.Feature} in the MrmToScansTask, the q1 mz is set as mz of the feature.
   */
  public static void applyMrmInfo(double q1, double q3, @Nullable ActivationMethod method,
      @Nullable Float energy, @NotNull OtherFeature feature) {
    feature.set(ChromatogramTypeType.class, ChromatogramType.MRM_SRM);
    feature.set(MZType.class, q3);

    final DDAMsMsInfoImpl msmsInfo = new DDAMsMsInfoImpl(q1, null, energy, null, null, 2, method,
        null);
    feature.set(MsMsInfoType.class, List.of(msmsInfo));
  }

  /**
   * @param feature a raw trace, a preprocessed trace or a processed feature
   * @return the raw trace the feature was derived from, the feature itself if it is a raw trace
   */
  @NotNull
  public static OtherFeature getRawTrace(@NotNull final OtherFeature feature) {
    // a raw trace has no raw trace reference itself
    return Objects.requireNonNullElse(feature.get(RawTraceType.class), feature);
  }

  /**
   * @param feature a raw trace, a preprocessed trace or a processed feature
   * @return the preprocessed trace of the feature's raw trace, or the raw trace if it was not
   * preprocessed
   */
  @NotNull
  public static OtherFeature getPreprocessedOrRawTrace(@NotNull final OtherFeature feature) {
    final OtherFeature rawTrace = getRawTrace(feature);
    final OtherTimeSeriesData data = rawTrace.getOtherDataFile().getOtherTimeSeriesData();
    final OtherFeature preprocessed =
        data != null ? data.getPreProcessedFeatureForTrace(rawTrace) : null;
    return preprocessed != null ? preprocessed : rawTrace;
  }

  /**
   * Finds the trace with a {@link TraceKey} in a raw data file, e.g., the trace of an aligned row
   * in a blank without a peak. Processed features keep the name of the preprocessed trace they were
   * resolved from, which may differ from the raw trace name (e.g., baseline correction adds a
   * suffix), so the keys of both traces are compared.
   *
   * @param file the raw data file
   * @param key  the trace key, e.g., of an aligned row
   * @return the preprocessed trace (or the raw trace if it was not preprocessed) that matches the
   * key, null if the file has no such trace
   */
  @Nullable
  public static OtherFeature findTraceByKey(@NotNull final RawDataFile file,
      @NotNull final TraceKey key) {
    for (final OtherDataFile otherFile : file.getOtherDataFiles()) {
      final OtherTimeSeriesData data = otherFile.getOtherTimeSeriesData();
      if (data == null) {
        continue;
      }
      for (final OtherFeature rawTrace : data.getRawTraces()) {
        final OtherFeature trace = getPreprocessedOrRawTrace(rawTrace);
        if (key.equals(TraceKey.of(trace)) || key.equals(TraceKey.of(rawTrace))) {
          return trace;
        }
      }
    }
    return null;
  }
}
