/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.util.scans;

import static io.github.mzmine.util.spectraldb.entry.DBEntryField.MERGED_SPEC_TYPE;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.Objects.requireNonNullElse;

import com.google.common.collect.Range;
import com.google.common.util.concurrent.AtomicDouble;
import gnu.trove.list.array.TDoubleArrayList;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.PrecursorIonTree;
import io.github.mzmine.datamodel.PrecursorIonTreeNode;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.impl.MSnInfoImpl;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.msms.DDAMsMsInfo;
import io.github.mzmine.datamodel.msms.IonMobilityMsMsInfo;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.spectra.CachedMobilityScan;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceRangeMap;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.collections.IndexRange;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.merging.FloatGrouping;
import io.github.mzmine.util.scans.sorting.ScanSortMode;
import io.github.mzmine.util.scans.sorting.ScanSorter;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Scan related utilities
 */
public class ScanUtils {

  /**
   * tolerance to compute and combine precursor m/z
   */
  public static final int DEFAULT_PRECURSOR_MZ_TOLERANCE = 100;
  /**
   * Sort MassSpectra first by source file and then by scan number, nulls last
   */
  public static final Comparator<MassSpectrum> SCAN_SORTER_RAW_FILE_SCAN_NUMBER = Comparator.comparing(
          ScanUtils::getSourceFile, nullsLast(naturalOrder()))
      .thenComparingInt(ScanUtils::extractScanNumber);
  private static final Logger logger = Logger.getLogger(ScanUtils.class.getName());

  /**
   * Erase file format
   */
  @Nullable
  public static String eraseRawFileFormat(@Nullable String fileName) {
    return fileName == null ? null : FileAndPathUtil.eraseFormat(fileName);
  }

  /**
   * Source file of scan is defined of other MassSpectra may be undefined and return null
   */
  @Nullable
  public static String getSourceFile(@NotNull MassSpectrum scan) {
    return switch (scan) {
      case Scan s -> s.getDataFile().getFileName();
      case SpectralLibraryEntry e -> {
        var lib = e.getLibrary();
        yield lib == null ? e.getLibraryName() : lib.getPath().getName();
      }
      default -> null;
    };
  }

  /**
   * Source file of scan is defined of other MassSpectra may be undefined and return null
   */
  @Nullable
  public static RawDataFile getDataFile(@NotNull MassSpectrum scan) {
    return switch (scan) {
      case Scan s -> s.getDataFile();
      default -> null;
    };
  }

  /**
   * Common utility method to be used as Scan.toString() method in various Scan implementations
   *
   * @param scan Scan to be converted to String
   * @return String representation of the scan
   */
  public static @NotNull String scanToString(@NotNull Scan scan) {
    return scanToString(scan, false);
  }

  /**
   * Common utility method to be used as Scan.toString() method in various Scan implementations
   *
   * @param scan Scan to be converted to String
   * @return String representation of the scan
   */
  public static @NotNull String scanToString(@Nullable Scan scan,
      @NotNull Boolean includeFileName) {
    if (scan == null) {
      return "null";
    }

    StringBuilder buf = new StringBuilder();
    Format rtFormat = MZmineCore.getConfiguration().getRTFormat();
    Format mzFormat = MZmineCore.getConfiguration().getMZFormat();
    Format mobilityFormat = MZmineCore.getConfiguration().getMobilityFormat();
    UnitFormat unitFormat = MZmineCore.getConfiguration().getUnitFormat();
    if (includeFileName) {
      buf.append(scan.getDataFile().getName());
    }
    if (scan instanceof Frame) {
      buf.append("Frame");
    } else if (scan instanceof MobilityScan) {
      buf.append("Mobility scan");
    } else {
      buf.append("Scan");
    }
    buf.append(" #");
    buf.append(scan instanceof MobilityScan ? ((MobilityScan) scan).getMobilityScanNumber()
        : scan.getScanNumber());
    buf.append(" @");
    buf.append(rtFormat.format(scan.getRetentionTime())).append("min");

    if (scan instanceof MobilityScan ms) {
      buf.append(" ");
      buf.append(mobilityFormat.format(ms.getMobility()));
      buf.append(ms.getMobilityType().getUnit());
    }
    if (scan instanceof Frame f) {
      buf.append(" ");
      buf.append(mobilityFormat.format(f.getMobilityRange().lowerEndpoint()));
      buf.append("-");
      buf.append(mobilityFormat.format(f.getMobilityRange().upperEndpoint()));
      buf.append(f.getMobilityType().getUnit());
    }

    buf.append(" MS");
    buf.append(scan.getMSLevel());
    if (scan.getMSLevel() > 1 && !(scan instanceof CachedMobilityScan)
        && scan.getMsMsInfo() instanceof DDAMsMsInfo dda) {
      buf.append(" (").append(mzFormat.format(dda.getIsolationMz())).append(")");
    }

    final List<Float> CEs = extractCollisionEnergies(scan);
    if (!CEs.isEmpty()) {
      buf.append(", CE: ");
      buf.append(CEs.stream().sorted().map("%.1f"::formatted).collect(Collectors.joining(", ")));
    }

    switch (scan.getSpectrumType()) {
      case CENTROIDED -> buf.append(" c");
      case PROFILE -> buf.append(" p");
      case THRESHOLDED -> buf.append(" t");
    }

    buf.append(" ");
    buf.append(scan.getPolarity().asSingleChar());

    if (scan instanceof MergedMassSpectrum) {
      buf.append(" merged ");
      buf.append(((MergedMassSpectrum) scan).getSourceSpectra().size());
      buf.append(" spectra");
    }


    /*
     * if ((scan.getScanDefinition() != null) && (scan.getScanDefinition().length() > 0)) {
     * buf.append(" ("); buf.append(scan.getScanDefinition()); buf.append(")"); }
     */

    return buf.toString();
  }

  public static @Nullable Float extractCollisionEnergy(MassSpectrum spectrum) {
    return switch (spectrum) {
      case MobilityScan mob ->
          mob.getMsMsInfo() != null ? mob.getMsMsInfo().getActivationEnergy() : null;
      case Scan scan ->
          scan.getMsMsInfo() != null ? scan.getMsMsInfo().getActivationEnergy() : null;
      case SpectralLibraryEntry entry -> {
        final FloatArrayList list = entry.getOrElse(DBEntryField.COLLISION_ENERGY,
            FloatArrayList.of());
        if (!list.isEmpty()) {
          yield list.getLast(); // last is the collision energy of the last MSn scan step
        }
        yield null;
      }
      default -> null;
    };
  }

  /**
   * @param spectrum any mass spectrum like {@link MergedMassSpectrum}
   * @return a list of distinct collision energies of all scans. Flattens the tree of a
   * {@link MergedMassSpectrum} and returns stream of all
   * {@link ScanUtils#extractCollisionEnergy(MassSpectrum)}.
   */
  public static List<Float> extractCollisionEnergies(MassSpectrum spectrum) {
    return streamSourceScans(spectrum).map(ScanUtils::extractCollisionEnergy)
        .filter(Objects::nonNull).distinct().toList();
  }

  /**
   * MSn energies for merged spectra are quite complex
   * <p>
   * [MS2, MS3, MS4] and multiple energies in last level due to merging
   *
   * @return energies for each merged scan as [[MS2, MS3, MS4], [MS2, MS3, MS4]]. List<List<Float>>
   */
  public static @NotNull List<List<Float>> extractMSnCollisionEnergies(final MassSpectrum scan) {
    return ScanUtils.streamSourceScans(scan, Scan.class).map(s -> {
      if (s.getMsMsInfo() instanceof MSnInfoImpl msn) {
        List<DDAMsMsInfo> precursors = msn.getPrecursors();
        if (precursors.isEmpty()) {
          return null;
        }
        return precursors.stream().map(DDAMsMsInfo::getActivationEnergy).filter(Objects::nonNull)
            .toList();
      }
      return null;
    }).filter(Objects::nonNull).distinct().toList();
  }

  /**
   * DataPoint usage is discouraged when used to stare data. It can be used when sorting of data
   * points is needed etc.
   *
   * @return array of data points
   */
  @Deprecated
  @NotNull
  public static DataPoint[] extractDataPoints(MassSpectrum spectrum) {
    int size = spectrum.getNumberOfDataPoints();
    DataPoint[] result = new DataPoint[size];
    double[] mz = spectrum.getMzValues(new double[size]);
    double[] intensity = spectrum.getIntensityValues(new double[size]);
    for (int i = 0; i < size; i++) {
      result[i] = new SimpleDataPoint(mz[i], intensity[i]);
    }
    return result;
  }

  public static DataPoint[] normalizeSpectrum(@NotNull MassSpectrum spectrum,
      double normalizedValue) {
    Integer basePeakIndex = spectrum.getBasePeakIndex();
    if (basePeakIndex == null || basePeakIndex < 0) {
      return new DataPoint[0];
    }
    final double maxIntensity = spectrum.getIntensityValue(basePeakIndex);

    final int total = spectrum.getNumberOfDataPoints();
    DataPoint[] newDataPoints = new DataPoint[total];
    for (int i = 0; i < total; i++) {
      double mz = spectrum.getMzValue(i);
      double intensity = spectrum.getIntensityValue(i) / maxIntensity * normalizedValue;

      newDataPoints[i] = new SimpleDataPoint(mz, intensity);
    }
    return newDataPoints;
  }

  public static DataPoint[] normalizeSpectrum(MassSpectrum spec) {
    return normalizeSpectrum(spec, 1);
  }

  /**
   * Find a base peak of a given scan in a given m/z range
   *
   * @param scan    Scan to search
   * @param mzRange mz range to search in
   * @return data point containing base peak m/z and intensity
   */
  @Nullable
  public static DataPoint findBasePeak(@NotNull Scan scan, @NotNull Range<Double> mzRange) {
    final Double scanBasePeakMz = scan.getBasePeakMz();
    if (scanBasePeakMz != null && mzRange.contains(scanBasePeakMz)) {
      return new SimpleDataPoint(scanBasePeakMz,
          requireNonNullElse(scan.getBasePeakIntensity(), 0d));
    }

    final double lower = mzRange.lowerEndpoint();
    final double upper = mzRange.upperEndpoint();

    boolean found = false;
    double baseMz = 0d;
    double baseIntensity = 0d;

    final int startIndex = scan.binarySearch(lower, DefaultTo.GREATER_EQUALS);
    if (startIndex == -1) {
      return null;
    }
    for (int i = startIndex; i < scan.getNumberOfDataPoints(); i++) {
      double mz = scan.getMzValue(i);
      if (mz > upper) {
        break;
      }

      double intensity = scan.getIntensityValue(i);
      if (intensity > baseIntensity) {
        found = true;
        baseIntensity = intensity;
        baseMz = mz;
      }
    }
    return found ? new SimpleDataPoint(baseMz, baseIntensity) : null;
  }

  /**
   * @param numValues The number of values to be scanned.
   * @return The base peak or null
   */
  @Nullable
  public static DataPoint findBasePeak(@NotNull double[] mzs, @NotNull double[] intensities,
      @NotNull Range<Double> mzRange, final int numValues) {

    assert mzs.length == intensities.length;
    assert numValues <= mzs.length;

    double baseMz = 0d;
    double baseIntensity = 0d;
    final int startIndex = BinarySearch.binarySearch(mzRange.lowerEndpoint(),
        DefaultTo.GREATER_EQUALS, numValues, j -> mzs[j]);
    if (startIndex == -1) {
      return null;
    }
    for (int i = startIndex; i < numValues; i++) {
      final double mz = mzs[i];
      if (mz > mzRange.upperEndpoint()) {
        break;
      }
      final double intensity = intensities[i];
      if (intensity > baseIntensity) {
        baseIntensity = intensity;
        baseMz = mz;
      }
    }
    return Double.compare(baseMz, 0d) != 0 ? new SimpleDataPoint(baseMz, baseIntensity) : null;
  }

  /**
   * Calculate the total ion count of a scan within a given mass range.
   *
   * @param scan    the scan.
   * @param mzRange mass range.
   * @return the total ion count of the scan within the mass range.
   */
  public static double calculateTIC(Scan scan, Range<Double> mzRange) {
    final IndexRange indexRange = BinarySearch.indexRange(mzRange, scan.getNumberOfDataPoints(), scan::getMzValue);

    double tic = 0.0;
    for (int i = indexRange.min(); i < indexRange.maxExclusive(); i++) {
      tic += scan.getIntensityValue(i);
    }
    return tic;
  }

  /**
   * @param numValues The number of values to be scanned.
   * @return the tic summed intensity of all signals within range
   */
  public static double calculateTIC(@NotNull double[] mzs, @NotNull double[] intensities,
      @NotNull Range<Double> mzRange, final int numValues) {

    assert mzs.length == intensities.length;
    assert numValues <= mzs.length;

    double totalIntensity = 0d;

    for (int i = 0; i < numValues; i++) {
      final double mz = mzs[i];
      if (mz < mzRange.lowerEndpoint()) {
        continue;
      } else if (mz > mzRange.upperEndpoint()) {
        break;
      }
      totalIntensity += intensities[i];
    }

    return totalIntensity;
  }

  /**
   * Selects data points within given m/z range
   */
  public static DataPoint[] selectDataPointsByMass(DataPoint[] dataPoints, Range<Double> mzRange) {
    ArrayList<DataPoint> goodPoints = new ArrayList<>();
    for (DataPoint dp : dataPoints) {
      if (mzRange.contains(dp.getMZ())) {
        goodPoints.add(dp);
      }
    }
    return goodPoints.toArray(new DataPoint[0]);
  }

  /**
   * Selects data points with intensity >= given intensity
   */
  public static DataPoint[] selectDataPointsOverIntensity(DataPoint[] dataPoints,
      double minIntensity) {
    ArrayList<DataPoint> goodPoints = new ArrayList<>();
    for (DataPoint dp : dataPoints) {
      if (dp.getIntensity() >= minIntensity) {
        goodPoints.add(dp);
      }
    }
    return goodPoints.toArray(new DataPoint[0]);
  }

  /**
   * This method bins values on x-axis. Each bin is assigned biggest y-value of all values in the
   * same bin.
   *
   * @param x            X-coordinates of the data
   * @param y            Y-coordinates of the data
   * @param binRange     x coordinates of the left and right edge of the first bin
   * @param numberOfBins Number of bins
   * @param interpolate  If true, then empty bins will be filled with interpolation using other
   *                     bins
   * @param binningType  Type of binning (sum of all 'y' within a bin, max of 'y', min of 'y', avg
   *                     of 'y')
   * @return Values for each bin
   */
  public static double[] binValues(double[] x, double[] y, Range<Double> binRange, int numberOfBins,
      boolean interpolate, BinningType binningType) {

    Double[] binValues = new Double[numberOfBins];
    double binWidth = (binRange.upperEndpoint() - binRange.lowerEndpoint()) / numberOfBins;

    double beforeX = Double.MIN_VALUE;
    double beforeY = 0.0f;
    double afterX = Double.MAX_VALUE;
    double afterY = 0.0f;

    double[] noOfEntries = null;

    // Binnings
    for (int valueIndex = 0; valueIndex < x.length; valueIndex++) {

      // Before first bin?
      if ((x[valueIndex] - binRange.lowerEndpoint()) < 0) {
        if (x[valueIndex] > beforeX) {
          beforeX = x[valueIndex];
          beforeY = y[valueIndex];
        }
        continue;
      }

      // After last bin?
      if ((binRange.upperEndpoint() - x[valueIndex]) < 0) {
        if (x[valueIndex] < afterX) {
          afterX = x[valueIndex];
          afterY = y[valueIndex];
        }
        continue;
      }

      int binIndex = (int) ((x[valueIndex] - binRange.lowerEndpoint()) / binWidth);

      // in case x[valueIndex] is exactly lastBinStop, we would overflow
      // the array
      if (binIndex == binValues.length) {
        binIndex--;
      }

      switch (binningType) {
        case MAX:
          if (binValues[binIndex] == null) {
            binValues[binIndex] = y[valueIndex];
          } else {
            if (binValues[binIndex] < y[valueIndex]) {
              binValues[binIndex] = y[valueIndex];
            }
          }
          break;
        case MIN:
          if (binValues[binIndex] == null) {
            binValues[binIndex] = y[valueIndex];
          } else {
            if (binValues[binIndex] > y[valueIndex]) {
              binValues[binIndex] = y[valueIndex];
            }
          }
          break;
        case AVG:
          if (noOfEntries == null) {
            noOfEntries = new double[binValues.length];
          }
          if (binValues[binIndex] == null) {
            noOfEntries[binIndex] = 1;
            binValues[binIndex] = y[valueIndex];
          } else {
            noOfEntries[binIndex]++;
            binValues[binIndex] += y[valueIndex];
          }
          break;

        case SUM:
        default:
          if (binValues[binIndex] == null) {
            binValues[binIndex] = y[valueIndex];
          } else {
            binValues[binIndex] += y[valueIndex];
          }
          break;

      }

    }

    // calculate the AVG
    if (binningType.equals(BinningType.AVG)) {
      assert noOfEntries != null;
      for (int binIndex = 0; binIndex < binValues.length; binIndex++) {
        if (binValues[binIndex] != null) {
          binValues[binIndex] /= noOfEntries[binIndex];
        }
      }
    }

    // Interpolation
    if (interpolate) {

      for (int binIndex = 0; binIndex < binValues.length; binIndex++) {
        if (binValues[binIndex] == null) {

          // Find exisiting left neighbour
          double leftNeighbourValue = beforeY;
          int leftNeighbourBinIndex = (int) Math.floor(
              (beforeX - binRange.lowerEndpoint()) / binWidth);
          for (int anotherBinIndex = binIndex - 1; anotherBinIndex >= 0; anotherBinIndex--) {
            if (binValues[anotherBinIndex] != null) {
              leftNeighbourValue = binValues[anotherBinIndex];
              leftNeighbourBinIndex = anotherBinIndex;
              break;
            }
          }

          // Find existing right neighbour
          double rightNeighbourValue = afterY;
          int rightNeighbourBinIndex = (binValues.length - 1) + (int) Math.ceil(
              (afterX - binRange.upperEndpoint()) / binWidth);
          for (int anotherBinIndex = binIndex + 1; anotherBinIndex < binValues.length;
              anotherBinIndex++) {
            if (binValues[anotherBinIndex] != null) {
              rightNeighbourValue = binValues[anotherBinIndex];
              rightNeighbourBinIndex = anotherBinIndex;
              break;
            }
          }

          double slope = (rightNeighbourValue - leftNeighbourValue) / (rightNeighbourBinIndex
                                                                       - leftNeighbourBinIndex);
          binValues[binIndex] = leftNeighbourValue + slope * (binIndex - leftNeighbourBinIndex);

        }

      }

    }

    double[] res = new double[binValues.length];
    for (int binIndex = 0; binIndex < binValues.length; binIndex++) {
      res[binIndex] = binValues[binIndex] == null ? 0 : binValues[binIndex];
    }
    return res;

  }

  /**
   * sort the data points by their m/z value. This method should be called before using other search
   * methods to do binary search in logarithmic time.
   *
   * @param dataPoints spectrum that should be sorted
   */
  public static void sortDataPointsByMz(DataPoint[] dataPoints) {
    Arrays.sort(dataPoints, Comparator.comparingDouble(DataPoint::getMZ));
  }

  /**
   * Returns the index of the datapoint with lowest m/z within the given datapoints which is within
   * the given mass range
   *
   * @param dataPoints sorted(!) list of datapoints
   * @param mzRange    m/z range to search in
   * @return index of datapoint or -1, if no datapoint is in range
   */
  public static int findFirstFeatureWithin(DataPoint[] dataPoints, Range<Double> mzRange) {
    final int insertionPoint = Arrays.binarySearch(dataPoints,
        new SimpleDataPoint(mzRange.lowerEndpoint(), 0d),
        Comparator.comparingDouble(DataPoint::getMZ));
    if (insertionPoint < 0) {
      final int k = -insertionPoint - 1;
      if (k < dataPoints.length && mzRange.contains(dataPoints[k].getMZ())) {
        return k;
      } else {
        return -1;
      }
    } else {
      return insertionPoint;
    }
  }

  /**
   * Returns the index of the datapoint with largest m/z within the given datapoints which is within
   * the given mass range
   *
   * @param dataPoints sorted(!) list of datapoints
   * @param mzRange    m/z range to search in
   * @return index of datapoint or -1, if no datapoint is in range
   */
  public static int findLastFeatureWithin(DataPoint[] dataPoints, Range<Double> mzRange) {
    final int insertionPoint = Arrays.binarySearch(dataPoints,
        new SimpleDataPoint(mzRange.upperEndpoint(), 0d),
        Comparator.comparingDouble(DataPoint::getMZ));
    if (insertionPoint < 0) {
      final int k = -insertionPoint - 2;
      if (k >= 0 && mzRange.contains(dataPoints[k].getMZ())) {
        return k;
      } else {
        return -1;
      }
    } else {
      return insertionPoint;
    }
  }

  /**
   * Returns the index of the datapoint with highest intensity within the given datapoints which is
   * within the given mass range
   *
   * @param dataPoints sorted(!) list of datapoints
   * @param mzRange    m/z range to search in
   * @return index of datapoint or -1, if no datapoint is in range
   */
  public static int findMostIntenseFeatureWithin(DataPoint[] dataPoints, Range<Double> mzRange) {
    int k = findFirstFeatureWithin(dataPoints, mzRange);
    if (k < 0) {
      return -1;
    }
    int mostIntense = k;
    for (; k < dataPoints.length; ++k) {
      if (!mzRange.contains(dataPoints[k].getMZ())) {
        break;
      }
      if (dataPoints[k].getIntensity() > dataPoints[mostIntense].getIntensity()) {
        mostIntense = k;
      }
    }
    return mostIntense;
  }

  /**
   * Returns index of m/z value in a given array, which is closest to given value, limited by given
   * m/z tolerance. We assume the m/z array is sorted.
   *
   * @return index of best match, or -1 if no datapoint was found
   */
  public static int findClosestDatapoint(double key, double[] mzValues, double mzTolerance) {

    int index = Arrays.binarySearch(mzValues, key);

    if (index >= 0) {
      return index;
    }

    // Get "insertion point"
    index = (index * -1) - 1;

    // If key value is bigger than biggest m/z value in array
    if (index == mzValues.length) {
      index--;
    } else if (index > 0) {
      // Check insertion point value and previous one, see which one
      // is closer
      if (Math.abs(mzValues[index - 1] - key) < Math.abs(mzValues[index] - key)) {
        index--;
      }
    }

    // Check m/z tolerancee
    if (Math.abs(mzValues[index] - key) <= mzTolerance) {
      return index;
    }

    // Nothing was found
    return -1;

  }

  /**
   * Determines if the spectrum represented by given array of data points is centroided or
   * continuous (profile or thresholded). Profile spectra are easy to detect, because they contain
   * zero-intensity data points. However, distinguishing centroided from thresholded spectra is not
   * trivial. MZmine uses multiple checks for that purpose, as described in the code comments.
   */
  /*
   * Adapted from MSDK: https://github.com/msdk/msdk/blob/master/msdk-spectra/
   * msdk-spectra-spectrumtypedetection/src/main/java/io/github/
   * msdk/spectra/spectrumtypedetection/SpectrumTypeDetectionAlgorithm.java
   */
  @NotNull
  public static MassSpectrumType detectSpectrumType(@NotNull double[] mzValues,
      double[] intensityValues) {

    // If the spectrum has less than 5 data points, it should be centroided.
    if (mzValues.length < 5) {
      return MassSpectrumType.CENTROIDED;
    }

    int basePeakIndex = 0;
    boolean hasZeroDataPoint = false;

    // Go through the data points and find the highest one
    int size = mzValues.length;
    for (int i = 0; i < size; i++) {

      // Update the maxDataPointIndex accordingly
      if (intensityValues[i] > intensityValues[basePeakIndex]) {
        basePeakIndex = i;
      }

      if (intensityValues[i] == 0.0) {
        hasZeroDataPoint = true;
      }
    }

    final double scanMzSpan = mzValues[size - 1] - mzValues[0];

    // Find the all data points around the base peak that have intensity
    // above half maximum
    final double halfIntensity = intensityValues[basePeakIndex] / 2.0;
    int leftIndex = basePeakIndex;
    while ((leftIndex > 0) && intensityValues[leftIndex - 1] > halfIntensity) {
      leftIndex--;
    }
    int rightIndex = basePeakIndex;
    while ((rightIndex < size - 1) && intensityValues[rightIndex + 1] > halfIntensity) {
      rightIndex++;
    }
    final double mainFeatureMzSpan = mzValues[rightIndex] - mzValues[leftIndex];
    final int mainFeatureDataPointCount = rightIndex - leftIndex + 1;

    // If the main feature has less than 3 data points above half intensity, it
    // indicates a centroid spectrum. Further, if the m/z span of the main
    // feature is more than 0.1% of the scan m/z range, it also indicates a
    // centroid spectrum. These criteria are empirical and probably not
    // bulletproof. However, it works for all the test cases we have.
    if ((mainFeatureDataPointCount < 3) || (mainFeatureMzSpan > (scanMzSpan / 1000.0))) {
      return MassSpectrumType.CENTROIDED;
    } else {
      if (hasZeroDataPoint) {
        return MassSpectrumType.PROFILE;
      } else {
        return MassSpectrumType.THRESHOLDED;
      }
    }

  }

  /**
   * Finds all MS/MS scans on MS2 level within given retention time range and with precursor m/z
   * within given m/z range
   *
   * @return stream sorted by default sorting (highest TIC)
   */
  public static Stream<Scan> streamAllMS2FragmentScans(@NotNull RawDataFile dataFile,
      @Nullable Range<Float> rtRange, @NotNull Range<Double> mzRange) {
    return streamAllMS2FragmentScans(dataFile, rtRange, mzRange, FragmentScanSorter.DEFAULT_TIC);
  }

  /**
   * Finds all MS/MS scans on MS2 level within given retention time range and with precursor m/z
   * within given m/z range. Applies sorting if sorter is not null
   *
   * @param sorter sorted stream see {@link FragmentScanSorter}. Unsorted if null
   * @return sorted stream
   */
  public static Stream<Scan> streamAllMS2FragmentScans(@NotNull RawDataFile dataFile,
      @Nullable Range<Float> rtRange, @NotNull Range<Double> mzRange,
      @Nullable Comparator<Scan> sorter) {

    final Stream<Scan> stream = dataFile.getScanNumbers(2).stream()
        .filter(s -> matchesMS2Scan(s, rtRange, mzRange));
    return sorter == null ? stream : stream.sorted(sorter);
  }

  /**
   * Finds all MS/MS scans on MS2 level within given retention time range and with precursor m/z
   * within given m/z range
   */
  public static Scan[] findAllMS2FragmentScans(RawDataFile dataFile, Range<Float> rtRange,
      Range<Double> mzRange) {
    return streamAllMS2FragmentScans(dataFile, rtRange, mzRange).toArray(Scan[]::new);
  }

  /**
   * Checks if scan precursor mz and rt is in ranges
   *
   * @param s tested scan
   * @return true if scan precursor mz is in range and rt
   */
  public static boolean matchesMS2Scan(Scan s, Range<Float> rtRange, Range<Double> mzRange) {
    if (rtRange != null && !rtRange.contains(s.getRetentionTime())) {
      return false;
    }
    final Double precursorMz = s.getPrecursorMz();
    return precursorMz != null && mzRange.contains(precursorMz);
  }

  /**
   * @param msLevel 0 for all scans
   */
  public static Stream<Scan> streamScans(RawDataFile dataFile, int msLevel) {
    return dataFile.getScanNumbers(msLevel).stream();
  }

  @Nullable
  public static List<PasefMsMsInfo> findMsMsInfos(IMSRawDataFile imsRawDataFile,
      Range<Double> mzRange, Range<Float> rtRange) {
    List<PasefMsMsInfo> featureMsMsInfos = new ArrayList<>();
    Collection<? extends Frame> ms2Frames = imsRawDataFile.getFrames(2, rtRange);
    for (Frame frame : ms2Frames) {
      final List<PasefMsMsInfo> infos = frame.getImsMsMsInfos().stream()
          .filter(info -> info instanceof PasefMsMsInfo).map(info -> (PasefMsMsInfo) info).toList();
      for (PasefMsMsInfo msmsInfo : infos) {
        if (mzRange.contains(msmsInfo.getIsolationMz())) {
          featureMsMsInfos.add(msmsInfo);
        }
      }
    }
    if (featureMsMsInfos.isEmpty()) {
      return null;
    }
    return featureMsMsInfos;
  }

  /**
   * Find the highest data point in array
   */
  public static @NotNull DataPoint findTopDataPoint(@NotNull DataPoint[] dataPoints) {

    DataPoint topDP = null;

    for (DataPoint dp : dataPoints) {
      if ((topDP == null) || (dp.getIntensity() > topDP.getIntensity())) {
        topDP = dp;
      }
    }

    return topDP;
  }

  /**
   * Find the highest data point index in array
   */
  public static int findTopDataPoint(@NotNull double[] intensityValues) {

    int basePeak = 0;
    for (int i = 0; i < intensityValues.length; i++) {

      if (intensityValues[i] > intensityValues[basePeak]) {
        basePeak = i;
      }
    }
    return basePeak;
  }

  /**
   * Find the m/z range of the data points in the array. We assume there is at least one data point,
   * and the data points are sorted by m/z.
   */
  public static @NotNull Range<Double> findMzRange(@NotNull DataPoint[] dataPoints) {

    assert dataPoints.length > 0;

    double lowMz = dataPoints[0].getMZ();
    double highMz = lowMz;
    for (int i = 1; i < dataPoints.length; i++) {
      if (dataPoints[i].getMZ() < lowMz) {
        lowMz = dataPoints[i].getMZ();
        continue;
      }
      if (dataPoints[i].getMZ() > highMz) {
        highMz = dataPoints[i].getMZ();
      }
    }

    return Range.closed(lowMz, highMz);
  }

  /**
   * Find the m/z range of the data points in the array. We assume there is at least one data point,
   * and the data points are sorted by m/z.
   */
  public static @NotNull Range<Double> findMzRange(@NotNull double[] mzValues) {

    assert mzValues.length > 0;

    double lowMz = mzValues[0];
    double highMz = lowMz;
    for (int i = 1; i < mzValues.length; i++) {
      if (mzValues[i] < lowMz) {
        lowMz = mzValues[i];
        continue;
      }
      if (mzValues[i] > highMz) {
        highMz = mzValues[i];
      }
    }

    return Range.closed(lowMz, highMz);
  }

  /**
   * Find the RT range of given scans. We assume there is at least one scan.
   */
  public static @NotNull Range<Float> findRtRange(@NotNull Scan[] scans) {

    assert scans.length > 0;

    float lowRt = scans[0].getRetentionTime();
    float highRt = lowRt;
    for (int i = 1; i < scans.length; i++) {
      if (scans[i].getRetentionTime() < lowRt) {
        lowRt = scans[i].getRetentionTime();
        continue;
      }
      if (scans[i].getRetentionTime() > highRt) {
        highRt = scans[i].getRetentionTime();
      }
    }

    return Range.closed(lowRt, highRt);
  }

  public static byte[] encodeDataPointsToBytes(DataPoint[] dataPoints) {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    DataOutputStream featureStream = new DataOutputStream(byteStream);
    for (int i = 0; i < dataPoints.length; i++) {

      try {
        featureStream.writeDouble(dataPoints[i].getMZ());
        featureStream.writeDouble(dataPoints[i].getIntensity());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    byte[] featureBytes = byteStream.toByteArray();
    return featureBytes;
  }

  public static char[] encodeDataPointsBase64(DataPoint[] dataPoints) {
    byte[] featureBytes = encodeDataPointsToBytes(dataPoints);
    char[] encodedData = Base64.getEncoder().encodeToString(featureBytes).toCharArray();
    return encodedData;
  }

  public static DataPoint[] decodeDataPointsFromBytes(byte[] bytes) {
    // each double is 8 bytes and we need one for m/z and one for intensity
    int dpCount = bytes.length / 2 / 8;

    // make a data input stream
    ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
    DataInputStream featureStream = new DataInputStream(byteStream);

    DataPoint[] dataPoints = new DataPoint[dpCount];

    for (int i = 0; i < dataPoints.length; i++) {
      try {
        double mz = featureStream.readDouble();
        double intensity = featureStream.readDouble();
        dataPoints[i] = new SimpleDataPoint(mz, intensity);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return dataPoints;
  }

  public static DataPoint[] decodeDataPointsBase64(char[] encodedData) {
    byte[] bytes = Base64.getDecoder().decode(new String(encodedData));
    DataPoint[] dataPoints = decodeDataPointsFromBytes(bytes);
    return dataPoints;
  }

  public static Stream<Scan> streamAllFragmentScans(FeatureListRow row) {
    return row.getAllFragmentScans().stream();
  }

  /**
   * Sorted list (best first) of all MS2 fragmentation scans with n signals >= noiseLevel in the
   * specified or first massList, if none was specified
   *
   * @param row                all MS2 scans of all features in this row
   * @param noiseLevel
   * @param minNumberOfSignals
   * @param sort               the sorting property (best first, index=0)
   * @return
   */
  @NotNull
  public static List<Scan> listAllFragmentScans(FeatureListRow row, double noiseLevel,
      int minNumberOfSignals, ScanSortMode sort) throws MissingMassListException {
    List<Scan> scans = listAllFragmentScans(row, noiseLevel, minNumberOfSignals);
    // first entry is the best scan
    scans.sort(new ScanSorter(noiseLevel, sort).reversed());
    return scans;
  }

  /**
   * List of all MS2 fragmentation scans with n signals >= noiseLevel in the specified or first
   * massList, if none was specified
   *
   * @param row
   * @param noiseLevel
   * @param minNumberOfSignals
   * @return
   */
  @NotNull
  public static List<Scan> listAllFragmentScans(FeatureListRow row, double noiseLevel,
      int minNumberOfSignals) throws MissingMassListException {
    List<Scan> scans = row.getAllFragmentScans();
    return listAllScans(scans, noiseLevel, minNumberOfSignals);
  }

  /**
   * Sorted list of all MS1 {@link Feature#getRepresentativeScan()} of all features. scans with n
   * signals >= noiseLevel in the specified or first massList, if none was specified
   *
   * @param row                all representative MS1 scans of all features in this row
   * @param noiseLevel
   * @param minNumberOfSignals
   * @param sort               the sorting property (best first, index=0)
   * @return
   */
  @NotNull
  public static List<Scan> listAllMS1Scans(FeatureListRow row, double noiseLevel,
      int minNumberOfSignals, ScanSortMode sort) throws MissingMassListException {
    List<Scan> scans = listAllMS1Scans(row, noiseLevel, minNumberOfSignals);
    // first entry is the best scan
    scans.sort(Collections.reverseOrder(new ScanSorter(noiseLevel, sort)));
    return scans;
  }

  /**
   * List of all MS1 {@link Feature#getRepresentativeScan()} of all features. scans with n signals
   * >= noiseLevel in the specified or first massList, if none was specified
   *
   * @param row
   * @param noiseLevel
   * @param minNumberOfSignals
   * @return
   */
  @NotNull
  public static List<Scan> listAllMS1Scans(FeatureListRow row, double noiseLevel,
      int minNumberOfSignals) throws MissingMassListException {
    List<Scan> scans = getAllMostIntenseMS1Scans(row);
    return listAllScans(scans, noiseLevel, minNumberOfSignals);
  }

  /**
   * Array of all {@link Feature#getRepresentativeScan()} of all features
   *
   * @param row
   * @return
   */
  public static List<Scan> getAllMostIntenseMS1Scans(FeatureListRow row) {
    return row.getFeatures().stream().map(Feature::getRepresentativeScan).filter(Objects::nonNull)
        .toList();
  }

  /**
   * List of all scans with n signals >= noiseLevel in the specified or first massList, if none was
   * specified
   *
   * @param noiseLevel
   * @param minNumberOfSignals
   * @return
   */
  @NotNull
  public static List<Scan> listAllScans(List<Scan> scans, double noiseLevel, int minNumberOfSignals,
      ScanSortMode sort) throws MissingMassListException {
    List<Scan> filtered = listAllScans(scans, noiseLevel, minNumberOfSignals);
    // first entry is the best scan
    filtered.sort(Collections.reverseOrder(new ScanSorter(noiseLevel, sort)));
    return filtered;
  }

  /**
   * List of all scans with n signals >= noiseLevel in the specified or first massList, if none was
   * specified
   *
   * @param noiseLevel
   * @param minNumberOfSignals
   * @return
   */
  @NotNull
  public static List<Scan> listAllScans(List<Scan> scans, double noiseLevel, int minNumberOfSignals)
      throws MissingMassListException {
    List<Scan> filtered = new ArrayList<>();
    for (Scan scan : scans) {
      // find mass list: with name or first
      final MassList massList = scan.getMassList();
      if (massList == null) {
        throw new MissingMassListException(scan);
      }

      // minimum number of signals >= noiseLevel
      int signals = getNumberOfSignals(massList, noiseLevel);
      if (signals >= minNumberOfSignals) {
        filtered.add(scan);
      }
    }
    return filtered;
  }


  /**
   * Uses a default mz tolerance {@link SpectraMerging#defaultMs2MergeTol} to build trees. Rather
   * use {@link #getMSnFragmentTrees(RawDataFile, MZTolerance)} and define the tolerance matching
   * the dataset
   *
   * @param raw build trees from this file
   * @return list of trees
   */
  public static List<PrecursorIonTree> getMSnFragmentTrees(RawDataFile raw) {
    return getMSnFragmentTrees(raw, SpectraMerging.defaultMs2MergeTol);
  }

  public static List<PrecursorIonTree> getMSnFragmentTrees(RawDataFile raw, MZTolerance mzTol) {
    return getMSnFragmentTrees(raw, mzTol, null);
  }

  public static List<PrecursorIonTree> getMSnFragmentTrees(RawDataFile raw, MZTolerance mzTol,
      AtomicDouble progress) {
    return getMSnFragmentTrees(raw.getScans(), mzTol, progress);
  }


  /**
   * Uses a default mz tolerance {@link SpectraMerging#defaultMs2MergeTol} to build trees. Rather
   * use {@link #getMSnFragmentTrees(FeatureList, MZTolerance)} and define the tolerance matching
   * the dataset
   *
   * @param flist build trees from this feature list
   * @return list of trees
   */
  public static List<PrecursorIonTree> getMSnFragmentTrees(FeatureList flist) {
    return getMSnFragmentTrees(flist, SpectraMerging.defaultMs2MergeTol);
  }

  public static List<PrecursorIonTree> getMSnFragmentTrees(FeatureList flist, MZTolerance mzTol) {
    return flist.stream().flatMap(row -> getMSnFragmentTrees(row, mzTol).stream()).sorted()
        .toList();
  }

  public static List<PrecursorIonTree> getMSnFragmentTrees(FeatureListRow row, MZTolerance mzTol) {
    return getMSnFragmentTrees(row.getAllFragmentScans(), mzTol, null);
  }

  public static List<PrecursorIonTree> getMSnFragmentTrees(List<Scan> scans, MZTolerance mzTol,
      AtomicDouble progress) {
    if (scans == null || scans.isEmpty()) {
      return List.of();
    }

    // at any time in the flow there should only be the latest precursor with the same m/z
    MZToleranceRangeMap<PrecursorIonTreeNode> ms2Nodes = new MZToleranceRangeMap<>(mzTol);

    PrecursorIonTreeNode parent = null;
    final int totalScans = scans.size();

    for (Scan scan : scans) {
      if (scan.getMSLevel() <= 1) {
        continue;
      }
      // add MS2 scans to existing or create new
      if (scan.getMSLevel() == 2) {
        Double ms2PrecursorMz = scan.getPrecursorMz();
        if (ms2PrecursorMz != null) {
          PrecursorIonTreeNode node = ms2Nodes.computeIfAbsent(ms2PrecursorMz,
              (key) -> new PrecursorIonTreeNode(2, ms2PrecursorMz, null));
          node.addFragmentScan(scan);
        }
      } else if (scan.getMsMsInfo() instanceof MSnInfoImpl msn) {
        // add MSn scans to MS2 precursor
        Double ms2PrecursorMz = msn.getMS2PrecursorMz();
        if ((parent = ms2Nodes.get(ms2PrecursorMz)) != null) {
          boolean added = parent.addChildFragmentScan(scan, msn);
          if (!added) {
            logger.warning(
                () -> String.format("Scan#%d was not added to parent %.4f", scan.getScanNumber(),
                    ms2PrecursorMz));
          }
        } else {
          logger.warning(
              () -> String.format("Scan#%d: Cannot find MS2 precursor scan for m/z: %.4f",
                  scan.getScanNumber(), ms2PrecursorMz));
        }
        //
        if (progress != null) {
          progress.addAndGet(1d / totalScans);
        }
      }
    }
    // get all values
    List<PrecursorIonTree> result = ms2Nodes.asMapOfRanges().values().stream()
        .map(PrecursorIonTree::new).sorted().toList();
    // sort
    result.forEach(PrecursorIonTree::sort);
    return result;
  }

  private static long getMzKey(double precursorMZ) {
    return Math.round(precursorMZ * DEFAULT_PRECURSOR_MZ_TOLERANCE);
  }

  /**
   * Sum of intensity of all data points >= noiseLevel
   *
   * @param data
   * @param noiseLevel
   * @return
   */
  public static double getTIC(DataPoint[] data, double noiseLevel) {
    return Stream.of(data).mapToDouble(DataPoint::getIntensity).filter(i -> i >= noiseLevel).sum();
  }

  /**
   * threshold: keep data points >= noiseLevel
   *
   * @param data
   * @param noiseLevel
   * @return
   */
  public static DataPoint[] getFiltered(DataPoint[] data, double noiseLevel) {
    return Stream.of(data).filter(dp -> dp.getIntensity() >= noiseLevel).toArray(DataPoint[]::new);
  }

  /**
   * below threshold: keep data points < noiseLevel
   *
   * @param data
   * @param noiseLevel
   * @return
   */
  public static DataPoint[] getBelowThreshold(DataPoint[] data, double noiseLevel) {
    return Stream.of(data).filter(dp -> dp.getIntensity() < noiseLevel).toArray(DataPoint[]::new);
  }

  /**
   * Number of signals >=noiseLevel
   *
   * @param data
   * @param noiseLevel
   * @return
   */
  public static int getNumberOfSignals(DataPoint[] data, double noiseLevel) {
    int n = 0;
    for (DataPoint dp : data) {
      if (dp.getIntensity() >= noiseLevel) {
        n++;
      }
    }
    return n;
  }


  /**
   * Sum of intensity of all data points >= noiseLevel
   *
   * @param spec
   * @param noiseLevel
   * @return
   */
  public static double getTIC(MassSpectrum spec, double noiseLevel) {
    int size = spec.getNumberOfDataPoints();
    double sum = 0;
    for (int i = 0; i < size; i++) {
      double intensity = spec.getIntensityValue(i);
      if (intensity >= noiseLevel) {
        sum += intensity;
      }
    }
    return sum;
  }

  /**
   * Sum of intensities
   */
  public static double getTIC(MassSpectrum spec) {
    Double tic = spec.getTIC();
    if (tic != null) {
      return tic;
    }

    int size = spec.getNumberOfDataPoints();
    double sum = 0;
    for (int i = 0; i < size; i++) {
      double intensity = spec.getIntensityValue(i);
      sum += intensity;
    }
    return sum;
  }

  /**
   * Number of signals >=noiseLevel
   *
   * @param spec
   * @param noiseLevel
   * @return
   */
  public static int getNumberOfSignals(MassSpectrum spec, double noiseLevel) {
    int size = spec.getNumberOfDataPoints();
    int n = 0;
    for (int i = 0; i < size; i++) {
      if (spec.getIntensityValue(i) >= noiseLevel) {
        n++;
      }
    }
    return n;
  }

  /**
   * Finds the first MS1 scan preceding the given MS2 scan. If no such scan exists, returns null.
   */
  public static @Nullable Scan findPrecursorScan(@NotNull Scan scan) {
    final RawDataFile dataFile = scan.getDataFile();
    final List<Scan> scanNumbers = dataFile.getScans();

    int startIndex = scanNumbers.indexOf(scan);

    for (int i = startIndex; i >= 0; i--) {
      Scan s = scanNumbers.get(i);
      if (s.getMSLevel() == 1) {
        return s;
      }
    }

    // Didn't find any MS1 scan
    return null;
  }

  @Nullable
  public static Scan findPrecursorScanForMerged(@NotNull MergedMsMsSpectrum merged,
      MZTolerance mzTolerance) {
    final List<MassSpectrum> sourceSpectra = merged.getSourceSpectra();
    if (sourceSpectra.stream().allMatch(s -> s instanceof MobilityScan)) {
      // this was an IMS file, so scans have been merged

      final List<MobilityScan> mobilityScans = sourceSpectra.stream()
          .<MobilityScan>mapMulti((s, consumer) -> consumer.accept(((MobilityScan) s))).toList();
      final double lowestMobility = mobilityScans.stream().mapToDouble(MobilityScan::getMobility)
          .min().orElse(0d);
      final double highestMobility = mobilityScans.stream().mapToDouble(MobilityScan::getMobility)
          .max().orElse(mobilityScans.get(0).getFrame().getMobilityRange().upperEndpoint());
      final Range<Double> mobilityRange = Range.closed(lowestMobility, highestMobility);

      // get the MS2 frames - usually this should be one, but we also provide the option to merge
      // all MS/MS scans for a precursor together
      final List<Frame> ms2Frames = mobilityScans.stream().map(MobilityScan::getFrame).distinct()
          .toList();
      final List<Frame> ms1Frames = ms2Frames.stream()
          .map(scan -> (Frame) ScanUtils.findPrecursorScan(scan)).filter(Objects::nonNull).toList();

      final List<MobilityScan> ms1MobilityScans = ms1Frames.stream()
          .<MobilityScan>mapMulti((f, c) -> {
            for (var mobscan : f.getMobilityScans()) {
              if (mobilityRange.contains(mobscan.getMobility())) {
                c.accept(mobscan);
              }
            }
          }).toList();

      return SpectraMerging.mergeSpectra(ms1MobilityScans, mzTolerance, MergingType.ALL_ENERGIES,
          null);
    } else {
      logger.warning(() -> "Unknown merged spectrum type. Please contact the developers.");
      return null;
    }
  }

  /**
   * Finds the first MS1 scan succeeding the given MS2 scan. If no such scan exists, returns null.
   */
  @Nullable
  public static Scan findSucceedingPrecursorScan(@NotNull Scan scan) {
    assert scan != null;
    final RawDataFile dataFile = scan.getDataFile();
    final List<Scan> scanNumbers = dataFile.getScans();

    int startIndex = scanNumbers.indexOf(scan);

    for (int i = startIndex; i < scanNumbers.size(); i++) {
      Scan s = scanNumbers.get(i);
      if (s.getMSLevel() == 1) {
        return s;
      }
    }

    // Didn't find any MS1 scan
    return null;
  }

  @Nullable
  public static Scan findSucceedingPrecursorScanForMerged(@NotNull MergedMsMsSpectrum merged,
      MZTolerance mzTolerance) {
    final List<MassSpectrum> sourceSpectra = merged.getSourceSpectra();
    if (sourceSpectra.stream().allMatch(s -> s instanceof MobilityScan)) {
      // this was an IMS file, so scans have been merged

      final List<MobilityScan> mobilityScans = sourceSpectra.stream()
          .<MobilityScan>mapMulti((s, consumer) -> consumer.accept(((MobilityScan) s))).toList();
      final double lowestMobility = mobilityScans.stream().mapToDouble(MobilityScan::getMobility)
          .min().orElse(0d);
      final double highestMobility = mobilityScans.stream().mapToDouble(MobilityScan::getMobility)
          .max().orElse(mobilityScans.get(0).getFrame().getMobilityRange().upperEndpoint());
      final Range<Double> mobilityRange = Range.closed(lowestMobility, highestMobility);

      // get the MS2 frames - usually this should be one, but we also provide the option to merge
      // all MS/MS scans for a precursor together
      final List<Frame> ms2Frames = mobilityScans.stream().map(MobilityScan::getFrame).distinct()
          .toList();
      final List<Frame> ms1Frames = ms2Frames.stream()
          .map(scan -> (Frame) ScanUtils.findSucceedingPrecursorScan(scan)).filter(Objects::nonNull)
          .toList();

      final List<MobilityScan> ms1MobilityScans = ms1Frames.stream()
          .<MobilityScan>mapMulti((f, c) -> {
            for (var mobscan : f.getMobilityScans()) {
              if (mobilityRange.contains(mobscan.getMobility())) {
                c.accept(mobscan);
              }
            }
          }).toList();

      return SpectraMerging.mergeSpectra(ms1MobilityScans, mzTolerance, MergingType.ALL_ENERGIES,
          null);
    } else {
      logger.warning(() -> "Unknown merged spectrum type. Please contact the developers.");
      return null;
    }
  }

  /**
   * Selects best N MS/MS scans from a feature list row
   */
  public static @NotNull Collection<Scan> selectBestMS2Scans(@NotNull FeatureListRow row,
      @NotNull Integer topN) throws MissingMassListException {
    final @NotNull List<Scan> allMS2Scans = row.getAllFragmentScans();
    return selectBestMS2Scans(allMS2Scans, topN);
  }

  /**
   * Selects best N MS/MS scans from a collection of scans
   */
  public static @NotNull Collection<Scan> selectBestMS2Scans(@NotNull Collection<Scan> scans,
      @NotNull Integer topN) throws MissingMassListException {
    assert scans != null;
    assert topN != null;

    // Keeps MS2 scans sorted by decreasing quality
    // Filter top N scans into an immutable list
    return scans.stream().sorted(new ScanSorter(0, ScanSortMode.MAX_TIC)).limit(topN).toList();
  }

  /**
   * Move the mass window given by binRange across the spectrum, keep only the
   * numberOfFeaturesPerBin most intense features within the window. This is a very simple and
   * robust method to remove most noise in the spectrum without having to estimate any noise
   * intensity parameter.
   *
   * @param dataPoints             spectrum
   * @param binRange               sliding mass window. Is shifted in each step by its width.
   * @param numberOfFeaturesPerBin number of features to keep within the sliding mass window
   * @return
   */
  public static DataPoint[] extractMostIntenseFeaturesAcrossMassRange(DataPoint[] dataPoints,
      Range<Double> binRange, int numberOfFeaturesPerBin) {
    double offset = binRange.lowerEndpoint();
    final double width = binRange.upperEndpoint() - binRange.lowerEndpoint();
    final HashMap<Integer, List<DataPoint>> bins = new HashMap<>();
    for (DataPoint p : dataPoints) {
      final int bin = (int) Math.floor((p.getMZ() - offset) / width);
      if (bin >= 0) {
        bins.computeIfAbsent(bin, (x) -> new ArrayList<>()).add(p);
      }
    }
    final List<DataPoint> finalDataPoints = new ArrayList<>();
    for (Integer bin : bins.keySet()) {
      List<DataPoint> list = bins.get(bin);
      list.sort((u, v) -> Double.compare(v.getIntensity(), u.getIntensity()));
      for (int i = 0; i < Math.min(list.size(), numberOfFeaturesPerBin); ++i) {
        finalDataPoints.add(list.get(i));
      }
    }
    DataPoint[] spectrum = finalDataPoints.toArray(new DataPoint[0]);
    sortDataPointsByMz(spectrum);
    return spectrum;
  }

  /**
   * Generalization of the cosine similarity for high resolution. See Algorithmic Mass Spectrometry
   * by Sebastian Böcker, chapter 4.2 While the cosine similarity transforms the spectrum into a
   * finite dimensional vector, the probability product transforms it into a mixture of continuous
   * gaussians.
   * <p>
   * As for cosine similarity it is recommended to first take the square root of all feature
   * intensities, before calling this method.
   *
   * @param scanLeft                   the first spectrum
   * @param scanRight                  the second spectrum
   * @param expectedMassDeviationInPPM the width of the gaussians (corresponds to the expected mass
   *                                   deviation). Rather use a larger than a small value! Value is
   *                                   given in ppm and Dalton.
   * @param noiseLevel                 the lowest intensity for a feature to be considered
   * @param mzRange                    the m/z range in which the features are compared. use null
   *                                   for the whole spectrum
   */
  public static double probabilityProduct(DataPoint[] scanLeft, DataPoint[] scanRight,
      MZTolerance expectedMassDeviationInPPM, double noiseLevel, @Nullable Range<Double> mzRange) {
    double d = probabilityProductUnnormalized(scanLeft, scanRight, expectedMassDeviationInPPM,
        noiseLevel, mzRange);
    double l = probabilityProductUnnormalized(scanLeft, scanLeft, expectedMassDeviationInPPM,
        noiseLevel, mzRange);
    double r = probabilityProductUnnormalized(scanRight, scanRight, expectedMassDeviationInPPM,
        noiseLevel, mzRange);
    return d / Math.sqrt(l * r);

  }

  /**
   * Calculates the probability product without normalization. Usually, this method is only useful
   * if you plan to normalize the spectra (or value) yourself.
   *
   * @see #probabilityProduct(DataPoint[], DataPoint[], MZTolerance, double, Range)
   */
  public static double probabilityProductUnnormalized(DataPoint[] scanLeft, DataPoint[] scanRight,
      MZTolerance expectedMassDeviationInPPM, double noiseLevel, @Nullable Range<Double> mzRange) {
    int i, j;
    double score = 0d;
    final int nl, nr;// =left.length, nr=right.length;
    if (mzRange == null) {
      nl = scanLeft.length;
      nr = scanRight.length;
      i = 0;
      j = 0;
    } else {
      nl = findLastFeatureWithin(scanLeft, mzRange) + 1;
      nr = findLastFeatureWithin(scanRight, mzRange) + 1;
      i = findFirstFeatureWithin(scanLeft, mzRange);
      j = findFirstFeatureWithin(scanRight, mzRange);
      if (i < 0 || j < 0) {
        return 0d;
      }
    }
    // gaussians are set to zero above allowedDifference to speed up
    // computation
    final double allowedDifference = expectedMassDeviationInPPM.getMzToleranceForMass(1000d) * 5;
    while (i < nl && j < nr) {
      DataPoint lp = scanLeft[i];
      if (lp.getIntensity() < noiseLevel) {
        ++i;
        continue;
      }
      DataPoint rp = scanRight[j];
      if (rp.getIntensity() < noiseLevel) {
        ++j;
        continue;
      }
      final double difference = lp.getMZ() - rp.getMZ();
      if (Math.abs(difference) <= allowedDifference) {
        final double mzabs = expectedMassDeviationInPPM.getMzToleranceForMass(
            Math.round((lp.getMZ() + rp.getMZ()) / 2d));
        final double variance = mzabs * mzabs;
        double matchScore = probabilityProductScore(lp, rp, variance);
        score += matchScore;
        for (int k = i + 1; k < nl; ++k) {
          DataPoint lp2 = scanLeft[k];
          final double difference2 = lp2.getMZ() - rp.getMZ();
          if (Math.abs(difference2) <= allowedDifference) {
            matchScore = probabilityProductScore(lp2, rp, variance);
            score += matchScore;
          } else {
            break;
          }
        }
        for (int l = j + 1; l < nr; ++l) {
          DataPoint rp2 = scanRight[l];
          final double difference2 = lp.getMZ() - rp2.getMZ();
          if (Math.abs(difference2) <= allowedDifference) {
            matchScore = probabilityProductScore(lp, rp2, variance);
            score += matchScore;
          } else {
            break;
          }
        }
        ++i;
        ++j;
      } else if (difference > 0) {
        ++j;

      } else {
        ++i;
      }
    }
    return score;
  }

  /**
   * Calculates the product of the integrals of two gaussians centered in lp and rp with given
   * variance
   */
  private static double probabilityProductScore(DataPoint lp, DataPoint rp, double variance) {
    final double mzDiff = Math.abs(lp.getMZ() - rp.getMZ());
    final double constTerm = 1.0 / (Math.PI * variance * 4);
    final double propOverlap = constTerm * Math.exp(-(mzDiff * mzDiff) / (4 * variance));
    return (lp.getIntensity() * rp.getIntensity()) * propOverlap;
  }

  /**
   * Converts DataPoint ion mz to int.
   * <p>
   * Function adapted from module: adap.mspexport.
   *
   * @param dataPoints spectra to convert.
   * @param intMode    conversion method: MAX or SUM.
   * @return DataPoint array converted to integers.
   */
  public static DataPoint[] integerDataPoints(final DataPoint[] dataPoints,
      final IntegerMode intMode) {

    int size = dataPoints.length;

    Map<Double, Double> integerDataPoints = new HashMap<>();

    for (int i = 0; i < size; ++i) {
      double mz = Math.round(dataPoints[i].getMZ());
      double intensity = dataPoints[i].getIntensity();
      Double prevIntensity = integerDataPoints.get(mz);
      if (prevIntensity == null) {
        prevIntensity = 0.0;
      }

      switch (intMode) {
        case MAX:
          integerDataPoints.put(mz, prevIntensity + intensity);
          break;
        case SUM:
          integerDataPoints.put(mz, Math.max(prevIntensity, intensity));
          break;
      }
    }

    DataPoint[] result = new DataPoint[integerDataPoints.size()];
    int count = 0;
    for (Entry<Double, Double> e : integerDataPoints.entrySet()) {
      result[count++] = new SimpleDataPoint(e.getKey(), e.getValue());
    }

    return result;
  }

  /**
   * @param dataPoints Sorted (by mz, ascending) array of data points
   * @param mzRange
   * @return
   */
  @NotNull
  public static DataPoint[] getDataPointsByMass(@NotNull DataPoint[] dataPoints,
      @NotNull Range<Double> mzRange) {

    int startIndex, endIndex;
    for (startIndex = 0; startIndex < dataPoints.length; startIndex++) {
      if (dataPoints[startIndex].getMZ() >= mzRange.lowerEndpoint()) {
        break;
      }
    }

    for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) {
      if (dataPoints[endIndex].getMZ() > mzRange.upperEndpoint()) {
        break;
      }
    }

    DataPoint[] pointsWithinRange = new DataPoint[endIndex - startIndex];

    // Copy the relevant points
    System.arraycopy(dataPoints, startIndex, pointsWithinRange, 0, endIndex - startIndex);

    return pointsWithinRange;
  }

  /**
   * Most abundant n signals
   *
   * @param scan
   * @param n
   * @return
   */
  public static DataPoint[] getMostAbundantSignals(DataPoint[] scan, int n) {
    if (scan.length <= n) {
      return scan;
    } else {
      Arrays.sort(scan,
          new DataPointSorter(SortingProperty.Intensity, SortingDirection.Descending));
      return Arrays.copyOf(scan, n);
    }
  }

  /**
   * Calculates an array of neutral losses relative to the precursor mz
   *
   * @param dps         data points to be inverted
   * @param precursorMZ
   * @return neutral loss array
   */
  public static DataPoint[] getNeutralLossSpectrum(DataPoint[] dps, double precursorMZ) {
    return getNeutralLossSpectrum(Arrays.stream(dps), precursorMZ);
  }

  /**
   * Calculates an array of neutral losses relative to the precursor mz
   *
   * @param dps         data points to be inverted
   * @param precursorMZ
   * @return neutral loss array
   */
  public static DataPoint[] getNeutralLossSpectrum(List<DataPoint> dps, double precursorMZ) {
    return getNeutralLossSpectrum(dps.stream(), precursorMZ);
  }

  /**
   * Calculates an array of neutral losses relative to the precursor mz
   *
   * @param dps         data points to be inverted
   * @param precursorMZ
   * @return neutral loss array
   */
  public static DataPoint[] getNeutralLossSpectrum(Stream<DataPoint> dps, double precursorMZ) {
    return dps.map(d -> new SimpleDataPoint(precursorMZ - d.getMZ(), d.getIntensity()))
        .toArray(DataPoint[]::new);
  }

  /**
   * Remove all data points within mz and tolerance
   *
   * @param dps       original data points
   * @param mz        the filter center
   * @param tolerance the filter tolerance
   * @return a new filtered array
   */
  public static DataPoint[] removeSignals(DataPoint[] dps, double mz, MZTolerance tolerance) {
    Range<Double> range = tolerance.getToleranceRange(mz);
    return Arrays.stream(dps).filter(dp -> !range.contains(dp.getMZ())).toArray(DataPoint[]::new);
  }

  /**
   * Filters the raw mz + intensity data of a scan to remove neighbouring zeros.
   *
   * @param scan The scan, [0] are mzs, [1] are intensities
   * @return A multidimensional array with the filtered values. [0] are mzs, [1] are intensities.
   */
  public static double[][] removeExtraZeros(double[][] scan) {
    // remove all extra zeros
    final int numDp = scan[0].length;
    final TDoubleArrayList filteredMzs = new TDoubleArrayList();
    final TDoubleArrayList filteredIntensities = new TDoubleArrayList();
    filteredMzs.add(scan[0][0]);
    filteredIntensities.add(scan[1][0]);
    for (int i = 1; i < numDp - 1;
        i++) { // previous , this and next are zero --> do not add this data point
      if (scan[1][i - 1] != 0 || scan[1][i] != 0 || scan[1][i + 1] != 0) {
        filteredMzs.add(scan[0][i]);
        filteredIntensities.add(scan[1][i]);
      }
    }
    filteredMzs.add(scan[0][numDp - 1]);
    filteredIntensities.add(scan[1][numDp - 1]);

    //Convert the ArrayList to an array.
    double[][] filteredScan = new double[2][filteredMzs.size()];
    for (int i = 0; i < filteredMzs.size(); i++) {
      filteredScan[0][i] = filteredMzs.get(i);
      filteredScan[1][i] = filteredIntensities.get(i);
    }

    return filteredScan;
  }

  /**
   * Calculates the spectral entropy given by <p></p> S = -SUM_p(I_p * ln(I_p)) <p></p> p = peak
   * index in the spectrum.
   * <a href="https://www.nature.com/articles/s41592-021-01331-z#Sec9">Reference</a>>
   *
   * @return The normalized spectral entropy. {@link Double#POSITIVE_INFINITY} if there is no TIC or
   * no ions in the spectrum.
   */
  public static double getSpectralEntropy(@NotNull final MassSpectrum spectrum) {
    final Double tic = spectrum.getTIC();
    if (tic == null || tic <= 0 || spectrum.getNumberOfDataPoints() == 0) {
      return Double.POSITIVE_INFINITY;
    }

    double spectralEntropy = 0d;
    for (int i = 0; i < spectrum.getNumberOfDataPoints(); i++) {
      final double normalizedIntensity = spectrum.getIntensityValue(i) / tic;
      spectralEntropy += normalizedIntensity * Math.log(normalizedIntensity);
    }
    return -spectralEntropy;
  }

  /**
   * Calculates the spectral entropy given by <p></p> S = -SUM_p(I_p * ln(I_p)) <p></p> p = peak
   * index in the spectrum.
   * <a href="https://www.nature.com/articles/s41592-021-01331-z#Sec9">Reference</a>>
   *
   * @return The normalized spectral entropy. {@link Double#POSITIVE_INFINITY} if there is no TIC or
   * no ions in the spectrum.
   */
  public static double getSpectralEntropy(@NotNull final double[] intensities) {
    if (intensities.length == 0) {
      return Double.POSITIVE_INFINITY;
    }

    final Double tic = Arrays.stream(intensities).sum();
    if (tic <= 0) {
      return Double.POSITIVE_INFINITY;
    }

    double spectralEntropy = 0d;
    for (int i = 0; i < intensities.length; i++) {
      final double normalizedIntensity = intensities[i] / tic;
      spectralEntropy += normalizedIntensity * Math.log(normalizedIntensity);
    }
    return -spectralEntropy;
  }

  /**
   * @return The spectral entropy normalized to the number of signals in a spectrum.
   * @see #getSpectralEntropy(MassSpectrum)
   */
  public static double getNormalizedSpectralEntropy(@NotNull final MassSpectrum spectrum) {
    if (spectrum.getNumberOfDataPoints() == 0) {
      return Double.POSITIVE_INFINITY;
    }
    return getSpectralEntropy(spectrum) / Math.log(spectrum.getNumberOfDataPoints());
  }

  /**
   * @return The spectral entropy normalized to the number of signals in a spectrum.
   * @see #getSpectralEntropy(MassSpectrum)
   */
  public static double getNormalizedSpectralEntropy(@NotNull final double[] intensities) {
    if (intensities.length == 0) {
      return Double.POSITIVE_INFINITY;
    }
    return getSpectralEntropy(intensities) / Math.log(intensities.length);
  }

  /**
   * Calculates the weighted spectral entropy given by <p></p> S = -SUM_p(I_p * ln(I_p)) <p></p> p =
   * peak index in the spectrum.
   * <a href="https://www.nature.com/articles/s41592-021-01331-z#Sec9">Reference</a>>
   * For entropies S < 3, the intensities are reweighted.
   *
   * @return The weighted spectral entropy. {@link Double#POSITIVE_INFINITY} if there is no TIC or
   * no ions in the spectrum.
   */
  public static double getWeightedSpectralEntropy(@NotNull final MassSpectrum spectrum) {
    final double entropy = getSpectralEntropy(spectrum);
    if (entropy > 3) { // also matches Double.Positive_Infinity (= no ions)
      return entropy;
    }

    final double[] weightedIntensities = new double[spectrum.getNumberOfDataPoints()];
    double tic = 0d;
    for (int i = 0; i < spectrum.getNumberOfDataPoints(); i++) {
      weightedIntensities[i] = Math.pow(spectrum.getIntensityValue(i), 0.25 + entropy * 0.25);
      tic += weightedIntensities[i];
    }

    double spectralEntropy = 0d;
    for (int i = 0; i < weightedIntensities.length; i++) {
      final double normalizedIntensity = weightedIntensities[i] / tic;
      spectralEntropy += normalizedIntensity * Math.log(normalizedIntensity);
    }
    return -spectralEntropy;
  }

  /**
   * Calculates the weighted spectral entropy given by <p></p> S = -SUM_p(I_p * ln(I_p)) <p></p> p =
   * peak index in the spectrum.
   * <a href="https://www.nature.com/articles/s41592-021-01331-z#Sec9">Reference</a>>
   * For entropies S < 3, the intensities are reweighted.
   *
   * @return The weighted spectral entropy. {@link Double#POSITIVE_INFINITY} if there is no TIC or
   * no ions in the spectrum.
   */
  public static double getWeightedSpectralEntropy(@NotNull final double[] intensities) {
    final double entropy = getSpectralEntropy(intensities);
    if (entropy > 3) { // also matches Double.Positive_Infinity (= no ions)
      return entropy;
    }

    final double[] weightedIntensities = new double[intensities.length];
    double tic = 0d;
    for (int i = 0; i < intensities.length; i++) {
      weightedIntensities[i] = Math.pow(intensities[i], 0.25 + entropy * 0.25);
      tic += weightedIntensities[i];
    }

    double spectralEntropy = 0d;
    for (int i = 0; i < weightedIntensities.length; i++) {
      final double normalizedIntensity = weightedIntensities[i] / tic;
      spectralEntropy += normalizedIntensity * Math.log(normalizedIntensity);
    }
    return -spectralEntropy;
  }

  /**
   * @return The spectral entropy normalized to the number of signals in a spectrum.
   * @see #getWeightedSpectralEntropy(MassSpectrum)
   */
  public static double getNormalizedWeightedSpectralEntropy(@NotNull final MassSpectrum spectrum) {
    if (spectrum.getNumberOfDataPoints() == 0) {
      return Double.POSITIVE_INFINITY;
    }
    return getWeightedSpectralEntropy(spectrum) / Math.log(spectrum.getNumberOfDataPoints());
  }

  /**
   * @return The spectral entropy normalized to the number of signals in a spectrum.
   * @see #getWeightedSpectralEntropy(MassSpectrum)
   */
  public static double getNormalizedWeightedSpectralEntropy(@NotNull final double[] intensities) {
    if (intensities.length == 0) {
      return Double.POSITIVE_INFINITY;
    }
    return getWeightedSpectralEntropy(intensities) / Math.log(intensities.length);
  }

  /**
   * @param msms The spectrum
   * @return The lowest non-zero intensity or null if there are no data points..
   */
  @Nullable
  public static Double getLowestIntensity(@NotNull final MassSpectrum msms) {
    if (msms.getNumberOfDataPoints() == 0) {
      return null;
    }
    double minIntensity = Double.POSITIVE_INFINITY;
    for (int i = 0; i < msms.getNumberOfDataPoints(); i++) {
      final double intensity = msms.getIntensityValue(i);
      if (intensity < minIntensity && intensity > 0) {
        minIntensity = intensity;
      }
    }
    return minIntensity < Double.POSITIVE_INFINITY ? minIntensity : null;
  }

  /**
   * Split scans into lists for each sample name
   *
   * @param scans input list
   * @return map of sample name to scans
   */
  public static Map<String, List<Scan>> splitBySample(final List<Scan> scans) {
    return scans.stream().collect(
        Collectors.groupingBy(scan -> requireNonNullElse(getSourceFile(scan), "UNDEFINED_SAMPLE")));
  }

  /**
   * Split scans into lists for each fragmentation energy. Scans with undefined energy will be one
   * group and all scans with multiple energies will be one group. Usually the MSn levels are split
   * before
   *
   * @param scans input list
   * @return map of fragmention energy to scans
   */
  public static Map<FloatGrouping, List<Scan>> splitByFragmentationEnergy(
      final Collection<Scan> scans) {
    return scans.stream()
        .collect(Collectors.groupingBy(scan -> FloatGrouping.of(extractCollisionEnergies(scan))));
  }

  /**
   * Use scan or mass list
   *
   * @return the input scan or the corresponding mass list.
   * @throws MissingMassListException if mass list was demanded and is missing. user needs to apply
   *                                  mass detection
   */
  public static @NotNull MassSpectrum getMassListOrThrow(final MassSpectrum s)
      throws MissingMassListException {
    return getMassSpectrum(s, true);
  }

  /**
   * Use scan or mass list
   *
   * @return the input scan or the corresponding mass list.
   * @throws MissingMassListException if mass list was demanded and is missing. user needs to apply
   *                                  mass detection
   */
  public static @NotNull MassSpectrum getMassSpectrum(final MassSpectrum s,
      final boolean useMassList) throws MissingMassListException {
    if (!useMassList || !(s instanceof Scan scan)) {
      return s;
    }

    MassList masses = scan.getMassList();
    if (masses == null) {
      throw new MissingMassListException(scan);
    }
    return masses;
  }

  /**
   * Only use the array when needed. Best way to iterate scan data in a single thread is
   * {@link ScanDataAccess} by {@link EfficientDataAccess}. When sorting of data is needed use
   * {@link #extractDataPoints(MassSpectrum, boolean)} but discouraged for data storage in memory.
   *
   * @param scan        target scan
   * @param useMassList either use mass list or return the input scan
   * @return scan.getMasstList if input is a scan
   * @throws MissingMassListException if useMassList is true and no mass detection was applied,
   *                                  users need to apply mass detection to all scans
   */
  public static double[] getIntensityValues(final MassSpectrum scan, final boolean useMassList)
      throws MissingMassListException {
    return getMassSpectrum(scan, useMassList).getIntensityValues(new double[0]);
  }

  /**
   * Only use the array when needed. Best way to iterate scan data in a single thread is
   * {@link ScanDataAccess} by {@link EfficientDataAccess}. When sorting of data is needed use
   * {@link #extractDataPoints(MassSpectrum, boolean)} but discouraged for data storage in memory.
   *
   * @param scan        target scan
   * @param useMassList either use mass list or return the input scan
   * @return scan.getMasstList if input is a scan
   * @throws MissingMassListException if useMassList is true and no mass detection was applied,
   *                                  users need to apply mass detection to all scans
   */
  public static double[] getMzValues(final MassSpectrum scan, final boolean useMassList)
      throws MissingMassListException {
    return getMassSpectrum(scan, useMassList).getMzValues(new double[0]);
  }

  /**
   * Usage of datapoints is sometimes required so this method can be used when data needs to be
   * sorted etc. Otherwise use the double arrays for mz and intensity instead. Also look at
   * {@link EfficientDataAccess} for single threaded access to scans from one dataset.
   *
   * @param scan        the target scan
   * @param useMassList extract data from mass list
   * @return
   * @throws MissingMassListException users need to run mass detection before on this scan
   */
  @Deprecated
  @NotNull
  public static DataPoint[] extractDataPoints(final MassSpectrum scan, final boolean useMassList)
      throws MissingMassListException {
    return extractDataPoints(getMassSpectrum(scan, useMassList));
  }


  /**
   * multiplies the intensities with the injection time to denormalize the intensities. In trapped
   * MS, the injection time is used to divide the intensities to make them comparable. We use this
   * method to bring all intensities of MSn scans to the same levels before merging. And also for
   * noise level detection.
   *
   * @param scan        provides the data and inject time
   * @param useMassList use scan or scan.masslist intensities
   * @return original input if injectTime is unset or <=0 otherwise the array multiplied by
   * injection time - changes the array in place
   */
  public static double[] denormalizeIntensitiesMultiplyByInjectTime(final Scan scan,
      boolean useMassList) {
    final double[] intensities = getIntensityValues(scan, useMassList);
    Float injectTime = scan.getInjectionTime();
    return denormalizeIntensitiesMultiplyByInjectTime(intensities, injectTime);
  }

  /**
   * multiplies the intensities with the injection time to denormalize the intensities. In trapped
   * MS, the injection time is used to divide the intensities to make them comparable. We use this
   * method to bring all intensities of MSn scans to the same levels before merging. And also for
   * noise level detection.
   *
   * @param injectTime  used as a factor to denormalize spectra
   * @param intensities the array of intensities - original array is changed
   * @return original input if injectTime is unset or <=0 otherwise the array multiplied by
   * injection time - changes the array in place
   */
  public static double[] denormalizeIntensitiesMultiplyByInjectTime(final double[] intensities,
      Float injectTime) {
    if (injectTime == null || injectTime <= 0) {
      return intensities;
    }

    for (int i = 0; i < intensities.length; i++) {
      intensities[i] = intensities[i] * injectTime;
    }
    return intensities;
  }

  /**
   * For a single spectrum (not merged) this will return a single entry list with all the precursor
   * mz for this scan. If MS3 this may be something like [[400.1, 222.0]] for the MS2 and MS3
   * precursor mz.
   *
   * @return list for each source scan of merged spectrum or a single entry for a single scan with
   * all the MSn precursor m/z in MS level selection order 2, 3, 4, ...
   */
  @NotNull
  public static List<List<Double>> getMsnPrecursorMzs(final MassSpectrum scan) {
    return ScanUtils.streamSourceScans(scan, Scan.class).map(s -> {
      if (s.getMsMsInfo() instanceof MSnInfoImpl msn) {
        List<DDAMsMsInfo> precursors = msn.getPrecursors();
        if (precursors.isEmpty()) {
          return null;
        }
        return precursors.stream().map(DDAMsMsInfo::getIsolationMz).toList();
      }
      return null;
    }).filter(Objects::nonNull).distinct().toList();
  }

  /**
   * @return the precursor mz (in the case of MSn the last precursor mz)
   */
  @Nullable
  public static Double getPrecursorMz(final MassSpectrum scan) {
    return switch (scan) {
      case Scan sc -> sc.getPrecursorMz();
      case SpectralLibraryEntry sc -> sc.getPrecursorMz();
      case null, default -> null;
    };
  }

  public static PolarityType getPolarity(final MassSpectrum scan) {
    return switch (scan) {
      case Scan sc -> sc.getPolarity();
      case SpectralLibraryEntry sc -> sc.getPolarity();
      case null, default -> null;
    };
  }

  /**
   * MS level as in MS1 or MS2
   */
  public static Optional<Integer> getMsLevel(final MassSpectrum scan) {
    return switch (scan) {
      case Scan sc -> Optional.of(sc.getMSLevel());
      case SpectralLibraryEntry sc -> sc.getMsLevel();
      case null, default -> Optional.empty();
    };
  }

  /**
   * @return merging type of a merged spectrum or {@link MergingType#SINGLE_SCAN} as default
   */
  @NotNull
  public static MergingType getMergingType(final MassSpectrum scan) {
    return switch (scan) {
      case MergedMassSpectrum sc -> sc.getMergingType();
      case SpectralLibraryEntry sc -> {
        yield switch (sc.getField(MERGED_SPEC_TYPE).orElse(null)) {
          case MergingType t -> t;
          case String s -> MergingType.parseOrElse(s, MergingType.SINGLE_SCAN);
          case null, default -> MergingType.SINGLE_SCAN;
        };
      }
      default -> MergingType.SINGLE_SCAN;
    };
  }

  /**
   * @return the scan number or -1 if none
   */
  public static int extractScanNumber(MassSpectrum scan) {
    return switch (scan) {
      case Scan s -> s.getScanNumber();
      case SpectralLibraryEntry e -> {
        // could be a list of integers
        Object scanNumber = e.getField(DBEntryField.SCAN_NUMBER).orElse(null);
        // either integer
        // or list of integer
        // or string
        yield switch (scanNumber) {
          case List<?> list -> {
            // merged has multiple numbers - return -1 instead
            // if single number in list then return single number
            List<Integer> scanNumbers = list.stream().map(MathUtils::parseInt)
                .filter(Objects::nonNull).toList();
            yield scanNumbers.size() == 1 ? scanNumbers.getFirst() : -1;
          }
          case null -> -1;
          default -> requireNonNullElse(MathUtils.parseInt(scanNumber), -1);
        };
      }
      default -> -1;
    };
  }

  /**
   * @param spectrum any mass spectrum like {@link MergedMassSpectrum}
   * @return an IntStream of all scan numbers. Flattens the tree of a {@link MergedMassSpectrum} and
   * returns stream of all {@link Scan#getScanNumber()}. Empty stream if parent or source spectra
   * are not of type Scan
   */
  public static IntStream extractScanNumbers(MassSpectrum spectrum) {
    return streamSourceScans(spectrum, Scan.class).mapToInt(Scan::getScanNumber).distinct();
  }

  /**
   * Extracts all universal spectrum identifier USI from source scans. If spectrum is merged this
   * will return multiple source USI otherwise just a Stream of one element.
   */
  public static Stream<String> extractUSI(MassSpectrum spectrum, @Nullable String datasetID) {
    String baseUSI = "mzspec:" + (datasetID == null ? "DATASET_ID_PLACEHOLDER" : datasetID) + ":";
    return streamSourceScans(spectrum, Scan.class).map(scan -> scanToUSI(scan, baseUSI)).distinct();
  }

  /**
   * Extracts all universal spectrum identifier USI from source scans. If spectrum is merged this
   * will return multiple source USI otherwise just a Stream of one element.
   * <p>
   * This reduced USI ranges merge all USI from the same sample, so different scan numbers, into
   * number ranges like mzspec:DATASET:SAMPLE:1-5,9 would point to all scans from 1 to 5 and 9.
   * <p>
   * Tool compatibility is limited but this greatly reduces the size and redundancy in USI
   * representing scans from the same file
   */
  public static Stream<String> extractCompressedUSIRanges(MassSpectrum spectrum,
      @Nullable String datasetID) {
    Map<RawDataFile, List<Scan>> scansForFile = streamSourceScans(spectrum, Scan.class).collect(
        Collectors.groupingBy(Scan::getDataFile));

    return scansForFile.entrySet().stream().map((entry) -> {
      var raw = entry.getKey();
      var scans = entry.getValue();
      // combine all scans of this sample into range strings
      String scanNumberStr = extractIdStringForScansOfSingleFile(scans);

      String fileName = requireNonNullElse(eraseRawFileFormat(raw.getFileName()), "");
      return createUSI(datasetID, fileName, scanNumberStr);
    });
  }

  /**
   * Create USI for scan. This involves finding the scan number and source file name
   */
  @NotNull
  public static String scanToUSI(MassSpectrum scan, @Nullable String baseUSI) {
    return __scanToUSI(scan, baseUSI);
  }

  /**
   * Create USI for scan. This involves finding the scan number and source file name
   *
   * @param baseUSI usually mzspec:datasetID:
   */
  @NotNull
  private static String __scanToUSI(MassSpectrum scan, String baseUSI) {
    String fileName = getSourceFile(scan);
    int scanNumber = extractScanNumber(scan);
    // map to USI
    return baseUSI + fileName + ":" + scanNumber;
  }

  /**
   * Universal spectrum identifier
   *
   * @param datasetID  will use the provided dataset ID or "DATASET_ID_PLACEHOLDER" if null
   * @param rawFile    used as is. Caller may wants to remove the format first
   * @param scanNumber adds a scan number if not null otherwise this USI is raw data file level
   */
  public static String createUSI(@Nullable String datasetID, final @NotNull String rawFile,
      final @Nullable String scanNumber) {
    if (datasetID == null) {
      datasetID = "DATASET_ID_PLACEHOLDER";
    }
    if (scanNumber == null) {
      return "mzspec:%s:%s".formatted(datasetID, rawFile);
    } else {
      return "mzspec:%s:%s:%s".formatted(datasetID, rawFile, scanNumber);
    }
  }

  public static <T extends MassSpectrum> Stream<T> streamSourceScans(final MassSpectrum scan,
      Class<T> specClass) {
    return streamSourceScans(scan).filter(specClass::isInstance).map(specClass::cast);
  }

  /**
   * This maps a single scan into a stream or more important a {@link MergedMassSpectrum} tree of
   * source scans into a flat stream. The stream always contains the input scan as well.
   *
   * @param scan first parent scan
   * @return flat stream of all mass spectra, including the original scan and all source scans
   */
  public static Stream<MassSpectrum> streamSourceScans(final MassSpectrum scan) {
    if (!(scan instanceof MergedMassSpectrum merged)) {
      return Stream.of(scan);
    }

    return Stream.of(merged).mapMulti(ScanUtils::addAllChildren)
        .sorted(SCAN_SORTER_RAW_FILE_SCAN_NUMBER);
  }

  private static void addAllChildren(final MassSpectrum parent,
      final Consumer<MassSpectrum> consumer) {

    if (parent instanceof MergedMassSpectrum merged) {
      for (final MassSpectrum child : merged.getSourceSpectra()) {
        addAllChildren(child, consumer);
      }
    } else {
      // add single spectrum
      consumer.accept(parent);
    }
  }

  /**
   * @param scan Any kind of spectrum.
   * @return a string of the spectra in this scan.
   * <br></br>
   * regular scan: 4
   * <br></br>
   * merged scan with regular scans: 4-7,9
   * <br></br>
   * merged scan with mobility scans: 4[8-25],5[8-26],6[5-24,28]
   * <br></br>
   * merged scan with mixed: 4[8-25],5[8-26],6[5-24,28],7-9,11
   */
  public static String extractScanIdString(final Scan scan,
      final boolean includeFilenameForSingleFiles) {
    return switch (scan) {
      case MergedMassSpectrum merged -> {
        final Map<RawDataFile, List<Scan>> groupedByFile = merged.getSourceSpectra().stream()
            .filter(Scan.class::isInstance).map(Scan.class::cast)
            .collect(Collectors.groupingBy(Scan::getDataFile));
        final StringBuilder sb = new StringBuilder();
        for (Entry<RawDataFile, List<Scan>> fileEntry : groupedByFile.entrySet()) {
          final RawDataFile file = fileEntry.getKey();
          final List<Scan> allScans = fileEntry.getValue();
          final String fileStr = extractIdStringForScansOfSingleFile(allScans);

          if (groupedByFile.size() > 1 || includeFilenameForSingleFiles) {
            sb.append(file.getName());
            sb.append(":");
          }
          sb.append(fileStr);
          sb.append(";");
        }
        final String str = sb.toString();
        yield str.substring(0, str.length() - 1);
      }
      case Scan s -> "%d".formatted(s.getScanNumber());
    };
  }

  /**
   * @param allScans Scans of a single {@link RawDataFile}. May contain mobility scans or regular
   *                 scans. To handle merged spectra, use
   *                 {@link #extractScanIdString(Scan, boolean)}
   * @return Consecutive enumeration of scan numbers. for mobility scans: 6[7-14,16],7[8-18]. for
   * regular scans: 5-13,15.
   */
  public static @NotNull String extractIdStringForScansOfSingleFile(List<Scan> allScans) {
    final StringBuilder builder = new StringBuilder();
    final List<MobilityScan> mobScans = allScans.stream().filter(MobilityScan.class::isInstance)
        .map(MobilityScan.class::cast).toList();
    final Map<Frame, List<MobilityScan>> sortedByFrame = mobScans.stream()
        .sorted(Comparator.comparingInt(MobilityScan::getMobilityScanNumber))
        .collect(Collectors.groupingBy(MobilityScan::getFrame));
    final String imsString = extractIdStringForFramesAndMobilityScans(sortedByFrame);
    if (!imsString.isBlank()) {
      builder.append(imsString).append(",");
    }

    final String scanString = extractIdStringForRegularScans(allScans);
    if (!scanString.isBlank()) {
      builder.append(scanString).append(",");
    }
    final String fileStr = builder.toString();
    return fileStr.isBlank() ? "" : fileStr.substring(0, fileStr.length() - 1);
  }

  /**
   * @param allScans Only "regular" scans or frames, no mobility scans or merged spectra. Mobility
   *                 scans may be contained, but will be filtered out.
   * @return List of scans, e.g. "5-9,11".
   */
  private static @NotNull String extractIdStringForRegularScans(List<Scan> allScans) {
    var scanRanges = IndexRange.findRanges(
        allScans.stream().filter(s -> !(s instanceof MobilityScan)).map(Scan::getScanNumber)
            .toList());
    final String scanString = scanRanges.stream().map(IndexRange::toString)
        .collect(Collectors.joining(","));
    return scanString;
  }

  /**
   * @param sortedByFrame A map of mobility scans mapped to their respective frame.
   * @return A string <Frame>[mobscans],<Frame>[mobscans],... e.g. 6[5-30,32],7[8-20]
   */
  private static @NotNull String extractIdStringForFramesAndMobilityScans(
      Map<Frame, List<MobilityScan>> sortedByFrame) {
    final List<Entry<Frame, List<MobilityScan>>> sortedByFrameId = sortedByFrame.entrySet().stream()
        .sorted(Entry.comparingByKey()).toList();
    StringBuilder sbFile = new StringBuilder();
    for (Entry<Frame, List<MobilityScan>> frameEntry : sortedByFrameId) {
      final Frame frame = frameEntry.getKey();
      final List<MobilityScan> mobilityScans = frameEntry.getValue();
      final String frameStr = extractIdStringForMobilityScanNumbersOfFrame(frame, mobilityScans);
      sbFile.append(frameStr).append(",");
    }
    final String str = sbFile.toString();
    return str.isBlank() ? "" : str.substring(0, str.length() - 1);
  }

  /**
   * @param frame         The frame
   * @param mobilityScans The mobility scans, all belonging to the frame.
   * @return A string <FrameNumber>[Mobility scan numbers], e.g. 6[5-30,32]
   */
  private static @NotNull String extractIdStringForMobilityScanNumbersOfFrame(Frame frame,
      List<MobilityScan> mobilityScans) {
    if (mobilityScans.isEmpty()) {
      return "";
    }
    final List<IndexRange> ranges = IndexRange.findRanges(
        mobilityScans.stream().map(MobilityScan::getMobilityScanNumber).toList());
    return "%d[%s]".formatted(frame.getScanNumber(),
        ranges.stream().map(IndexRange::toString).collect(Collectors.joining(",")));
  }


  /**
   * @param imsCheckPrecursorMz Only applied to IMS Frames. Regular Scan data is usually already
   *                            filtered to match the selection. Uses precursor mz to filter out
   *                            spectra with mismatching isolation window. This is important for
   *                            timsTOF DIA workflows where one Frame contains multiple precursor
   *                            ions and MsMsInfos. Leave precursorMz as null to ignore this
   *                            filter.
   * @return A stream of all MsMsInfos in the given collection of scans. IMS frames will return all
   * isolations if there are multiple. This stream will not contain null entries and may be empty.
   */
  public static Stream<MsMsInfo> streamMsMsInfos(@Nullable Collection<? extends Scan> scans,
      final @Nullable Double imsCheckPrecursorMz) {
    if (scans == null) {
      return Stream.empty();
    }
    return scans.stream().mapMulti((s, downstream) -> {
      if (s instanceof Frame f) {
        // frames may contain multiple precursor isolations - check each and find the ones with matching isolation window
        for (final IonMobilityMsMsInfo imsInfo : f.getImsMsMsInfos()) {
          if (imsInfo != null) {
            final Range<Double> mzWindow = imsInfo.getIsolationWindow();
            if (imsCheckPrecursorMz != null && mzWindow != null && !mzWindow.contains(
                imsCheckPrecursorMz)) {
              continue;
            }
            downstream.accept(imsInfo);
          }
        }
      } else {
        final MsMsInfo info = s.getMsMsInfo();
        if (info != null) {
          // do not apply precursor mz filter
          // Scan is usually already filtered to match the precursor ion
          // filtering out based on the isolation window in MsMsInfo may be error prone / too strict
          // isolation is sometimes wider than the isolation window set in MsMsInfo
          downstream.accept(info);
        }
      }
    });
  }

  /**
   * Binning modes
   */
  public enum BinningType {
    SUM, MAX, MIN, AVG
  }


  /**
   * Integer conversion methods.
   */
  public enum IntegerMode {

    SUM("Merging mode: Sum"), MAX("Merging mode: Maximum");

    private final String intMode;

    IntegerMode(String intMode) {
      this.intMode = intMode;
    }

    @Override
    public String toString() {
      return this.intMode;
    }
  }


}
