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

package io.github.mzmine.util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IntensitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.MobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.scans.ScanUtils;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IonMobilityUtils {

  private static final double isotopeDistance = IsotopePatternCalculator.THIRTHEEN_C_DISTANCE;
  private static final MZTolerance isotopeTol = new MZTolerance(0.003, 10);

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
      if (frame.getMobilityRange().isEmpty() || frame.getMobilities().size() <= 1) {
        continue;
      }
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

    return buildMobilogramForMzRange(frame.getMobilityScans(), mzRange, type, storage);
  }

  /**
   * Builds a mobilogram for the given mz range in the frame. Should only be used for previews and
   * visualisations, less perfomant than a ims feature detector.
   *
   * @param mobilityScans The mobility scans. must belong to a single frame and sorted by ascending
   *                      mobility scan number.
   * @param mzRange       The mz/Range of the mobilogram
   * @param type          basepeak or tic (summed)
   * @param storage       The storage to use
   * @return The built mobilogram.
   */
  public static IonMobilitySeries buildMobilogramForMzRange(
      @NotNull final List<MobilityScan> mobilityScans, @NotNull final Range<Double> mzRange,
      @NotNull final MobilogramType type, @Nullable final MemoryMapStorage storage) {

    final int numScans = mobilityScans.size();
    final double rangeCenter = RangeUtils.rangeCenter(mzRange);

    final double[] intensities = new double[numScans];
    final double[] mzs = new double[numScans];

    final int maxNumDataPoints =
        numScans != 0 ? mobilityScans.get(0).getFrame().getMaxMobilityScanRawDataPoints() : 0;

    final double[] intensitiesBuffer = new double[maxNumDataPoints];
    final double[] mzsBuffer = new double[maxNumDataPoints];

    for (int i = 0; i < numScans; i++) {
      final MobilityScan scan = mobilityScans.get(i);
      scan.getMzValues(mzsBuffer);
      scan.getIntensityValues(intensitiesBuffer);

      switch (type) {
        case BASE_PEAK -> {
          DataPoint bp = ScanUtils.findBasePeak(mzsBuffer, intensitiesBuffer, mzRange,
              scan.getNumberOfDataPoints());
          if (bp != null) {
            mzs[i] = bp.getMZ();
            intensities[i] = bp.getIntensity();
          }
        }
        case TIC -> {
          mzs[i] = rangeCenter;
          intensities[i] = ScanUtils.calculateTIC(mzsBuffer, intensitiesBuffer, mzRange,
              scan.getNumberOfDataPoints());
        }
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
   *               sorted by descending mobility.
   *               {@link io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries}
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

    final float startMobility = (float) MathUtils.twoPointGetXForY(series.getMobility(before),
        series.getIntensity(before),
        series.getMobility(Math.min(before + 1, series.getNumberOfValues() - 1)),
        series.getIntensity(Math.min(before + 1, series.getNumberOfValues() - 1)), halfIntensity);

    final float endMobility = (float) MathUtils.twoPointGetXForY(
        series.getMobility(Math.max(after - 1, 0)), series.getIntensity(Math.max(after - 1, 0)),
        series.getMobility(after), series.getIntensity(after), halfIntensity);

//    logger.finest(() -> "Determined FWHM from " + startMobility + " to " + endMobility);
    return Range.closed(Math.min(startMobility, endMobility), Math.max(startMobility, endMobility));
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
   * Sums up the number of values of each {@link IonMobilitySeries} in the given
   * {@link IonMobilogramTimeSeries}.
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
   * @return The maximum number of data points or null if there is no
   * {@link IonMobilogramTimeSeries}.
   */
  public static Integer getMaxNumTraceDatapoints(FeatureListRow row) {
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

  /**
   * Calculates a spectral purity around a specific m/z. The purity is calculated as the quotient of
   * intensities in the isolation window with regard to mobility and m/z. The
   * {@link MobilityScanDataAccess} must have selected the frame to evaluate. The mobility scan will
   * be set to the first using {@link MobilityScanDataAccess#resetMobilityScan()}. If no data points
   * are found in the isolation window a score of 0 will be returned.
   *
   * @param precursorMz          The precursor to isolate.
   * @param access               A data access.
   * @param mzRange              The mzRange to isolate.
   * @param mobilityRange        The mobility range to isolate.
   * @param sumPrecursorIsotopes If true, isotope intensities are summed to the precursor
   * @return Accumulated precursor intensity divided by intensity of all ions in the isolation
   * window. 0 if no intensities are found.
   */
  public static double getPurityInMzAndMobilityRange(final double precursorMz,
      @NotNull final MobilityScanDataAccess access, @NotNull final Range<Double> mzRange,
      @NotNull final Range<Float> mobilityRange, boolean sumPrecursorIsotopes) {

    double precursorIntensity = 0d;
    double isolationWindowTIC = 0d;

    access.resetMobilityScan();
    while (access.hasNextMobilityScan()) {
      access.nextMobilityScan();

      if (access.getNumberOfDataPoints() == 0 || !mobilityRange.contains(
          (float) access.getMobility())) {
        continue;
      }

      final int closestIndex = access.binarySearch(precursorMz, DefaultTo.CLOSEST_VALUE);
      if (closestIndex == -1) {
        continue;
      }

      final double precursorMzInScan = access.getMzValue(closestIndex);
      if (mzRange.contains(precursorMzInScan)) {
        precursorIntensity += access.getIntensityValue(closestIndex);
        isolationWindowTIC += access.getIntensityValue(closestIndex);
      }

      for (int i = closestIndex - 1; i > 0; i--) {
        if (mzRange.contains(access.getMzValue(i))) {
          isolationWindowTIC += access.getIntensityValue(i);
          if (sumPrecursorIsotopes && isPotentialIsotope(precursorMzInScan, access.getMzValue(i),
              isotopeTol)) {
            precursorIntensity += access.getIntensityValue(i);
          }
        } else {
          break;
        }
      }

      for (int i = closestIndex + 1; i < access.getNumberOfDataPoints(); i++) {
        if (mzRange.contains(access.getMzValue(i))) {
          isolationWindowTIC += access.getIntensityValue(i);
          if (sumPrecursorIsotopes && isPotentialIsotope(precursorMzInScan, access.getMzValue(i),
              isotopeTol)) {
            precursorIntensity += access.getIntensityValue(i);
          }
        } else {
          break;
        }
      }
    }

    return Double.compare(isolationWindowTIC, 0d) > 0 ? precursorIntensity / isolationWindowTIC
        : 0d;
  }

  public static double getPurityInMzRange(final double precursorMz,
      @NotNull final MobilityScanDataAccess access, @NotNull final Range<Double> mzRange,
      @NotNull final MobilityScan scan, boolean sumPrecursorIsotopes) {

    double precursorIntensity = 0d;
    double isolationWindowTIC = 0d;

    access.resetMobilityScan();

    if (access.jumpToMobilityScan(scan) == null || access.getNumberOfDataPoints() == 0) {
      return 0d;
    }

    final int closestIndex = access.binarySearch(precursorMz, DefaultTo.CLOSEST_VALUE);
    if (closestIndex == -1) {
      return 0d;
    }

    final double precursorMzInScan = access.getMzValue(closestIndex);
    if (mzRange.contains(precursorMzInScan)) {
      precursorIntensity += access.getIntensityValue(closestIndex);
      isolationWindowTIC += access.getIntensityValue(closestIndex);
    }

    for (int i = closestIndex - 1; i > 0; i--) {
      if (mzRange.contains(access.getMzValue(i))) {
        isolationWindowTIC += access.getIntensityValue(i);
        if (sumPrecursorIsotopes && isPotentialIsotope(precursorMzInScan, access.getMzValue(i),
            isotopeTol)) {
          precursorIntensity += access.getIntensityValue(i);
        }
      } else {
        break;
      }
    }

    for (int i = closestIndex + 1; i < access.getNumberOfDataPoints(); i++) {
      if (mzRange.contains(access.getMzValue(i))) {
        isolationWindowTIC += access.getIntensityValue(i);
        if (sumPrecursorIsotopes && isPotentialIsotope(precursorMzInScan, access.getMzValue(i),
            isotopeTol)) {
          precursorIntensity += access.getIntensityValue(i);
        }
      } else {
        break;
      }
    }

    return isolationWindowTIC > 0d ? precursorIntensity / isolationWindowTIC : 0d;
  }

  /**
   * @param mobilogram          The mobilogram.
   * @param normalizationFactor The factor to divide intensities by. null = resulting max intensity
   *                            will be normalized to 1.
   * @return The normalized mobilogram.
   */
  public static SummedIntensityMobilitySeries normalizeMobilogram(
      final SummedIntensityMobilitySeries mobilogram, @Nullable Double normalizationFactor) {
    double[] newIntensities = new double[mobilogram.getNumberOfValues()];
    double[] newMobilities = new double[mobilogram.getNumberOfValues()];
    mobilogram.getMobilityValues(newMobilities);
    mobilogram.getIntensityValues(newIntensities);

    final double max = normalizationFactor != null ? normalizationFactor
        : Arrays.stream(newIntensities).max().orElse(1d);
    for (int i = 0; i < newIntensities.length; i++) {
      newIntensities[i] /= max;
    }
    return new SummedIntensityMobilitySeries(null, newMobilities, newIntensities);
  }

  public static boolean isPotentialIsotope(final double monoisotopicMz, final double mzToCheck,
      final MZTolerance tolerance) {
    final int num13c = (int) Math.round(mzToCheck - monoisotopicMz);
    return tolerance.checkWithinTolerance(monoisotopicMz + num13c * isotopeDistance, mzToCheck);
  }

  public static MobilityScan getMobilityScanForMobility(final Frame frame, final double mobility) {
    final int index = switch (frame.getMobilityType()) {
      case TIMS -> { // reverse order for tims, invert the binary search.
        final int numScans = frame.getNumberOfMobilityScans();
        int i = BinarySearch.binarySearch(mobility, DefaultTo.CLOSEST_VALUE, numScans,
            j -> frame.getMobilities().getDouble(numScans - j));
        yield numScans - i;
      }
      default -> BinarySearch.binarySearch(mobility, DefaultTo.CLOSEST_VALUE,
          frame.getNumberOfMobilityScans(),
          i -> frame.getMobilities().getDouble(i));
    };

    return frame.getMobilityScan(index);
  }

  public enum MobilogramType {
    BASE_PEAK, TIC;

    public String shortString() {
      return switch (this) {
        case BASE_PEAK -> "BPM";
        case TIC -> "TIM";
      };
    }
  }
}
