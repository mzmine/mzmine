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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IonMobilityUtils {

  private static final Logger logger = Logger.getLogger(IonMobilityUtils.class.getName());

  public static double getSmallestMobilityDelta(Frame frame) {
    double minDelta = Double.MAX_VALUE;

    List<MobilityScan> scans = frame.getMobilityScans();
    double lastMobility = scans.get(0).getMobility();
    for (int i = 1; i < scans.size(); i++) {
      final double currentMobility = scans.get(i).getMobility();
      final double delta = Math.abs(currentMobility - lastMobility);
      if (delta < minDelta) {
        minDelta = delta;
      }
      lastMobility = currentMobility;
    }
    return minDelta;
  }

  /**
   * @param file The raw data file
   * @return A map of frame -> mobility range, sorted with ascending frame id.
   */
  public static Map<Frame, Range<Double>> getUniqueMobilityRanges(
      @NotNull final IMSRawDataFile file) {
    Map<Frame, Range<Double>> ranges = new LinkedHashMap<>();
    for (Frame frame : file.getFrames()) {
      if (!ranges.containsValue(frame.getMobilityRange())) {
        ranges.put(frame, frame.getMobilityRange());
      }
    }
    return ranges;
  }

  public static boolean isRowWithinMzMobilityRegion(@NotNull ModularFeatureListRow row,
      @NotNull final Collection<Path2D> regions) {
    Float mobility = row.get(MobilityType.class);
    if (mobility != null) {
      Point2D point = new Point2D.Double(row.getAverageMZ(), mobility.doubleValue());
      for (Path2D region : regions) {
        if (region.contains(point)) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean isRowWithinMzCCSRegion(@NotNull ModularFeatureListRow feature,
      @NotNull final Collection<Path2D> regions) {
    Float ccs = feature.getAverageCCS();
    if (ccs != null) {
      Point2D point = new Point2D.Double(feature.getAverageMZ() * feature.getRowCharge(),
          ccs.doubleValue());
      for (Path2D region : regions) {
        if (region.contains(point)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Builds a mobilogram for the given mz range in the frame. Should only be used for previews and
   * visualisations, less perfomant than a ims feature detector.
   *
   * @param frame   The frame
   * @param mzRange The mz/Range of the mobilogram
   * @param type    basepeak or tic (summed)
   * @param storage The storage to use
   * @return The built mobilogram.
   */
  public static IonMobilitySeries buildMobilogramForMzRange(@NotNull final Frame frame,
      @NotNull final Range<Double> mzRange, @NotNull final MobilogramType type,
      @Nullable final MemoryMapStorage storage) {

    final int numScans = frame.getNumberOfMobilityScans();
    final double rangeCenter = RangeUtils.rangeCenter(mzRange);

    final double[] intensities = new double[frame.getNumberOfMobilityScans()];
    final double[] mzs = new double[frame.getNumberOfMobilityScans()];

    final List<MobilityScan> mobilityScans = frame.getMobilityScans();

    final int maxNumDataPoints = frame.getMaxMobilityScanRawDataPoints();

    final double[] intensitiesBuffer = new double[maxNumDataPoints];
    final double[] mzsBuffer = new double[maxNumDataPoints];

    for (int i = 0; i < numScans; i++) {
      final MobilityScan scan = mobilityScans.get(i);
      scan.getMzValues(mzsBuffer);
      scan.getIntensityValues(intensitiesBuffer);

      if (type == MobilogramType.BASE_PEAK) {
        DataPoint bp = ScanUtils.findBasePeak(mzsBuffer, intensitiesBuffer, mzRange,
            scan.getNumberOfDataPoints());
        if (bp != null) {
          mzs[i] = bp.getMZ();
          intensities[i] = bp.getIntensity();
        }
      } else if (type == MobilogramType.TIC) {
        mzs[i] = rangeCenter;
        intensities[i] = ScanUtils.calculateTIC(mzsBuffer, intensitiesBuffer, mzRange,
            scan.getNumberOfDataPoints());
      }
    }

    return new SimpleIonMobilitySeries(storage, mzs, intensities, mobilityScans);
  }

  /**
   * Extracts the mobility scan with the highest intensity this feature was detected in.
   *
   * @param f The feature.
   * @return The mobility scan. Null if this feature does not possess a mobility dimension.
   */
  @Nullable
  public static MobilityScan getBestMobilityScan(@NotNull final Feature f) {
    Scan bestScan = f.getRepresentativeScan();
    if (!(bestScan instanceof Frame bestFrame)) {
      return null;
    }

    final IonTimeSeries<? extends Scan> featureData = f.getFeatureData();
    if (!(featureData instanceof IonMobilogramTimeSeries trace)) {
      return null;
    }

    final IonMobilitySeries bestMobilogram = trace.getMobilogram(bestFrame);
    if (bestMobilogram == null) {
      return null;
    }

    MobilityScan bestMobilityScan = null;
    double maxIntensity = 0d;
    for (int i = 0; i < bestMobilogram.getNumberOfValues(); i++) {
      if (bestMobilogram.getIntensity(i) > maxIntensity) {
        maxIntensity = bestMobilogram.getIntensity(i);
        bestMobilityScan = bestMobilogram.getSpectrum(i);
      }
    }
    return bestMobilityScan;
  }

  /**
   * @param series The series. Sorted by ascending mobility. Note that raw {@link IonMobilitySeries}
   *               from {@link io.github.mzmine.datamodel.MobilityType#TIMS} measurements can be
   *               sorted by descending mobility. {@link io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries}
   *               are guaranteed to be sorted by ascending mobility.
   * @return The FWHM range or null.
   */
  public static <T extends IntensitySeries & MobilitySeries> Range<Float> getMobilityFWHM(
      T series) {
    final int mostIntenseIndex = FeatureDataUtils.getMostIntenseIndex(series);
    if (mostIntenseIndex == -1) {
      return null;
    }

    final double maxIntensity = series.getIntensity(mostIntenseIndex);
    final double halfIntensity = maxIntensity / 2;

    int before = 0;
    int after = series.getNumberOfValues() - 1;

    for (int i = 0; i < mostIntenseIndex; i++) {
      if (series.getIntensity(i) > halfIntensity) {
        before = Math.max(0, i - 1);
        break;
      }
    }

    for (int i = mostIntenseIndex; i < series.getNumberOfValues(); i++) {
      if (series.getIntensity(i) < halfIntensity) {
        after = i;
        break;
      }
    }

    final double startMobility = MathUtils.twoPointGetXForY(series.getMobility(before),
        series.getIntensity(before),
        series.getMobility(Math.min(before + 1, series.getNumberOfValues() - 1)),
        series.getIntensity(Math.min(before + 1, series.getNumberOfValues() - 1)), halfIntensity);

    final double endMobility = MathUtils.twoPointGetXForY(
        series.getMobility(Math.max(after - 1, 0)), series.getIntensity(Math.max(after - 1, 0)),
        series.getMobility(after), series.getIntensity(after), halfIntensity);

//    logger.finest(() -> "Determined FWHM from " + startMobility + " to " + endMobility);
    return Range.closed((float) startMobility, (float) endMobility);
  }

  /**
   * @param row The row.
   * @return The spanned mobility range for all features in this row. Null if there is no mobility
   * dimension.
   */
  public static Range<Float> getRowMobilityrange(ModularFeatureListRow row) {

    Range<Float> mobilityRange = null;
    for (ModularFeature feature : row.getFilesFeatures().values()) {
      if (mobilityRange == null) {
        mobilityRange = feature.getMobilityRange();
      } else {
        var featureRange = feature.getMobilityRange();
        if (featureRange != null) {
          mobilityRange = mobilityRange.span(featureRange);
        }
      }
    }
    return mobilityRange;
  }

  /**
   * Sums up the number of values of each {@link IonMobilitySeries} in the given {@link
   * IonMobilogramTimeSeries}.
   *
   * @param trace The ion mobility trace.
   * @return The number of data points.
   */
  public static int getTraceDatapoints(IonMobilogramTimeSeries trace) {
    int num = 0;
    for (IonMobilitySeries mobilogram : trace.getMobilograms()) {
      num += mobilogram.getNumberOfValues();
    }
    return num;
  }

  /**
   * Returns the maximum number of datapoints in {@link IonMobilogramTimeSeries} in this row.
   *
   * @param row The row.
   * @return The maximum number of data points or null if there is no {@link
   * IonMobilogramTimeSeries}.
   */
  public static Integer getMaxNumTraceDatapoints(ModularFeatureListRow row) {
    int max = row.streamFeatures()
        .filter(f -> f != null && f.getFeatureData() instanceof IonMobilogramTimeSeries)
        .mapToInt(f -> getTraceDatapoints((IonMobilogramTimeSeries) f.getFeatureData())).max()
        .orElse(-1);
    return max == -1 ? null : max;
  }

  /**
   * @param series The series.
   * @return The mobility values in the series.
   */
  public static <T extends IntensitySeries & MobilitySeries> double[] extractMobilities(
      @NotNull final T series) {
    final double[] mobilities = new double[series.getNumberOfValues()];
    extractMobilities(series, mobilities);
    return mobilities;
  }

  /**
   * @param series The series.
   * @param dst    A buffer to write the mobility values to.
   */
  public static <T extends IntensitySeries & MobilitySeries> void extractMobilities(
      @NotNull final T series, @NotNull final double[] dst) {
    assert series.getNumberOfValues() <= dst.length;

    if (series instanceof SummedIntensityMobilitySeries summed) {
      summed.getMobilityValues(dst);
    } else if (series instanceof BinningMobilogramDataAccess access) {
      System.arraycopy(access.getMobilityValues(), 0, dst, 0, access.getNumberOfValues());
    } else {
      for (int i = 0; i < dst.length; i++) {
        dst[i] = series.getMobility(i);
      }
    }
  }

  public enum MobilogramType {
    BASE_PEAK, TIC
  }

}
