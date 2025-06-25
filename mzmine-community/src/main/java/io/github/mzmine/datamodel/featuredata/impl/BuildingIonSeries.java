/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.datamodel.featuredata.impl;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.util.MemoryMapStorage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.lang.foreign.MemorySegment;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * Used to build chromatograms or images with a predefined number of scans that span the whole
 * range. Can be converted to an {@link IonTimeSeries}
 */
public class BuildingIonSeries implements IonSeries {

  private final double[] mzs;
  private final double[] intensities;
  private final MzMode mzMode;
  private final IntensityMode mode;
  private final int[] dataPoints;

  /**
   * @param numberOfScans all scans that this chromatogram spans (e.g., all MS1 positive mode)
   * @param mode          defines how to merge intensity values
   */
  public BuildingIonSeries(int numberOfScans, MzMode mzMode, IntensityMode mode) {
    mzs = new double[numberOfScans];
    intensities = new double[numberOfScans];
    dataPoints = new int[numberOfScans];
    this.mode = mode;
    this.mzMode = mzMode;
  }

  public double[] getIntensities() {
    return intensities;
  }

  public double[] getMzs() {
    return mzs;
  }

  public int[] getDataPoints() {
    return dataPoints;
  }

  public IntensityMode getMode() {
    return mode;
  }

  public boolean hasNonZeroData() {
    for (final double v : intensities) {
      if (v > 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param scans the original full list of scans that was used to create this chromatogram
   * @return an ion time series with only data points > 0 with 1 leading and trailing zero around
   * detected data points if in scan range
   */
  public IonTimeSeries<? extends Scan> toIonTimeSeriesWithLeadingAndTrailingZero(
      @Nullable MemoryMapStorage storage, final List<Scan> scans) {
    if (mzs.length == 0) {
      return IonTimeSeries.EMPTY;
    }

    DoubleArrayList fmzs = new DoubleArrayList();
    DoubleArrayList fintensities = new DoubleArrayList();
    List<Scan> fscans = new ArrayList<>();

    int wasZero = 0;
    boolean wasData = false;
    Scan last = null;
    for (int i = 0; i < scans.size(); i++) {
      if (intensities[i] == 0) {
        wasZero++;
        // add first zero after data
        if (wasZero == 1 && wasData) {
          last = addDpToIonTimeSeries(scans, fmzs, fintensities, fscans, i);
          wasData = false;
        }
      } else {
        // add one leading 0 if available
        if (wasZero > 0 && !Objects.equals(last, scans.get(i - 1))) {
          addDpToIonTimeSeries(scans, fmzs, fintensities, fscans, i - 1);
        }
        last = addDpToIonTimeSeries(scans, fmzs, fintensities, fscans, i);
        wasData = true;
        wasZero = 0;
      }
    }

    return new SimpleIonTimeSeries(storage, fmzs.toDoubleArray(), fintensities.toDoubleArray(),
        fscans);
  }

  private Scan addDpToIonTimeSeries(final List<Scan> scans, final DoubleArrayList fmzs,
      final DoubleArrayList fintensities, final List<Scan> fscans, final int i) {
    fmzs.add(mzs[i]);
    fintensities.add(intensities[i]);
    Scan scan = scans.get(i);
    fscans.add(scan);
    return scan;
  }

  /**
   * @param scans the original full list of scans that was used to create this chromatogram
   * @return an ion time series that spans all scans
   */
  public IonTimeSeries<? extends Scan> toFullIonTimeSeries(@Nullable MemoryMapStorage storage,
      final List<? extends Scan> scans) {
    return new SimpleIonTimeSeries(storage, mzs, intensities, scans);
  }

  /**
   * Add value and directly compute the new value
   *
   * @param scanIndex the index of the current scan in all selected scans
   * @param mz        the mz to add
   * @param intensity the intensity to add (based on the defined IntensityMode)
   * @return false if mode was {@link IntensityMode#HIGHEST} and the new signal was lower than the
   * already existing value
   */
  public boolean addValue(int scanIndex, double mz, double intensity) {
    boolean added = switch (mode) {
      case HIGHEST -> {
        if (intensity > intensities[scanIndex]) {
          intensities[scanIndex] = intensity;
          yield true;
        } else {
          yield false;
        }
      }
      case SUM -> {
        intensities[scanIndex] += intensity;
        dataPoints[scanIndex] += 1;
        yield true;
      }
      case MEAN -> {
        int n = dataPoints[scanIndex];
        intensities[scanIndex] = (intensities[scanIndex] * n + intensity) / (n + 1);
        dataPoints[scanIndex] += 1;
        yield true;
      }
    };
    if (!added) {
      return false;
    }

    switch (mzMode) {
      case HIGHEST_INTENSITY -> mzs[scanIndex] = mz;
      case MEAN -> {
        int n = dataPoints[scanIndex];
        mzs[scanIndex] = (mzs[scanIndex] * n + mz) / (n + 1);
      }
    }
    return true;
  }

  @Override
  public double[] getIntensityValues(final double[] dst) {
    throw new UnsupportedOperationException(
        "This method in building ion series should not be called. Call getIntensities instead");
  }

  @Override
  public double[] getMzValues(final double[] dst) {
    throw new UnsupportedOperationException(
        "This method in building ion series should not be called. Call getMzs instead");
  }

  @Override
  public MemorySegment getIntensityValueBuffer() {
    throw new UnsupportedOperationException("Only used for building");
  }

  @Override
  public IonSeries copy(final MemoryMapStorage storage) {
    throw new UnsupportedOperationException("Only used for building");
  }

  @Override
  public MemorySegment getMZValueBuffer() {
    throw new UnsupportedOperationException("Only used for building");
  }

  public enum IntensityMode {
    HIGHEST, SUM, MEAN;

    public static IntensityMode DEFAULT = HIGHEST;

    public static IntensityMode from(TICPlotType type) {
      return switch (type) {
        case TIC -> SUM;
        case BASEPEAK -> HIGHEST;
      };
    }

    public TICPlotType toPlotType() {
      return switch (this) {
        case SUM, MEAN -> TICPlotType.TIC;
        case HIGHEST -> TICPlotType.BASEPEAK;
      };
    }

  }

  public enum MzMode {
    HIGHEST_INTENSITY, MEAN;

    public static MzMode DEFAULT = HIGHEST_INTENSITY;

  }
}
