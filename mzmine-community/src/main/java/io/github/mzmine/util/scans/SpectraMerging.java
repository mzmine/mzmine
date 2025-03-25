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

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.google.common.util.concurrent.AtomicDouble;
import gnu.trove.list.array.TDoubleArrayList;
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
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.impl.BuildingMobilityScan;
import io.github.mzmine.datamodel.impl.SimpleFrame;
import io.github.mzmine.datamodel.impl.SimpleMergedMassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleMergedMsMsSpectrum;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.datamodel.msms.PasefMsMsInfo;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderTask;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.maths.CenterFunction;
import io.github.mzmine.util.maths.CenterMeasure;
import io.github.mzmine.util.maths.Weighting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility methods to merge multiple spectra. Data points are sorted by intensity and grouped,
 * similar to ADAP chromatogram building {@link ModularADAPChromatogramBuilderTask}. Merging of data
 * points from the same spectrum is prevented by indexing the data points prior to sorting.
 *
 * @author https://github.com/SteffenHeu
 */
@SuppressWarnings("UnstableApiUsage")
public class SpectraMerging {

  public static final double EPSILON = 1E-15;

  public static final CenterMeasure DEFAULT_CENTER_MEASURE = CenterMeasure.AVG;
  public static final Weighting DEFAULT_WEIGHTING = Weighting.LINEAR;
  public static final CenterFunction DEFAULT_CENTER_FUNCTION = new CenterFunction(
      DEFAULT_CENTER_MEASURE, DEFAULT_WEIGHTING);

  // for merging IMS-TOF MS1 scans ~Steffen
  public static final MZTolerance defaultMs1MergeTol = new MZTolerance(0.005, 15);
  // for merging IMS-TOF MS2 scans ~Steffen
  public static final MZTolerance pasefMS2MergeTol = new MZTolerance(0.008, 25);

  public static final MZTolerance defaultMs2MergeTol = new MZTolerance(0.008, 25);

  private static final DataPointSorter sorter = new DataPointSorter(SortingProperty.Intensity,
      SortingDirection.Descending);
  private static final Logger logger = Logger.getLogger(SpectraMerging.class.getName());

  /**
   * Calculates merged intensities and mz values of all data points in the given spectrum. Ideally,
   * {@link MassList}s should be used so noise is filtered out by the user.
   *
   * @param source               The {@link MassSpectrum} source
   * @param tolerance            m/z tolerance to merge peaks.
   * @param intensityMergingType The way to calculate intensities (avg, sum, max)
   * @param mzCenterFunction     A function to center m/z values after merging.
   * @param <T>                  Any type of {@link MassSpectrum}
   * @param inputNoiseLevel      A noise level to use. May be null or 0 if no noise level shall be
   *                             used.
   * @param outputNoiseLevel     Minimum intensity to be achieved in the merged intensity. May be
   *                             null or 0.
   * @param minNumPeaks          Minimum number of times that a peak has to appear in one of the
   *                             source spectra to make it to the final merged spectrum.
   * @return double[2][] array, [0][] being the mzs, [1] being the intensities. Empty double[2][0]
   * if the source collection is empty.
   */
  public static <T extends MassSpectrum> double[][] calculatedMergedMzsAndIntensities(
      @NotNull final Collection<T> source, @NotNull final MZTolerance tolerance,
      @NotNull final SpectraMerging.IntensityMergingType intensityMergingType,
      @NotNull final CenterFunction mzCenterFunction, @Nullable final Double inputNoiseLevel,
      @Nullable final Double outputNoiseLevel, @Nullable final Integer minNumPeaks) {

    if (source.isEmpty()) {
      return new double[][]{new double[0], new double[0]};
    }

    final List<IndexedDataPoint> dataPoints = new ArrayList<>();
    // extract all data points in the mass spectrum
    final int numDp = source.stream().mapToInt(MassSpectrum::getNumberOfDataPoints).max()
        .getAsInt();
    final double[] rawMzs = new double[numDp];
    final double[] rawIntensities = new double[numDp];

    int index = 0;
    for (T spectrum : source) {
      spectrum.getMzValues(rawMzs);
      spectrum.getIntensityValues(rawIntensities);

      for (int i = 0; i < spectrum.getNumberOfDataPoints(); i++) {
        if (inputNoiseLevel == null || rawIntensities[i] > inputNoiseLevel) {
          final IndexedDataPoint dp = new IndexedDataPoint(rawMzs[i], rawIntensities[i], index);
          dataPoints.add(dp);
        }
      }
      index++;
    }

    dataPoints.sort(sorter);

    // set is sorted by the index of the datapoint, so we can quickly check the presence of the same index
    RangeMap<Double, SortedSet<IndexedDataPoint>> dataPointRanges = TreeRangeMap.create();

    for (IndexedDataPoint dp : dataPoints) {
      // todo hash map should be smarter, just put by index
      SortedSet<IndexedDataPoint> dplist = dataPointRanges.get(dp.getMZ());
      boolean containsIndex = false;

      // no entry -> make a new one
      if (dplist == null) {
        dplist = new TreeSet<>(Comparator.comparingInt(IndexedDataPoint::getIndex));
        Range<Double> range = createNewNonOverlappingRange(dataPointRanges,
            tolerance.getToleranceRange(dp.getMZ()));
        dataPointRanges.put(range, dplist);
      } else { // we have an entry, check if if we have the same index in there already
        if (dp.getIndex() > dplist.first().getIndex() && dp.getIndex() < dplist.last().getIndex()) {
          for (IndexedDataPoint indexedDataPoint : dplist) {
            if (dp.getIndex() == indexedDataPoint.getIndex()) {
              containsIndex = true;
              break;
            }
            if (dp.getIndex() > indexedDataPoint.getIndex()) {
              break;
            }
          }
        }
        // if an entry contains that index, make a new entry (this way multiple data points from a
        //  single scan will not be merged together)
        if (containsIndex) {
          dplist = new TreeSet<>(Comparator.comparingInt(IndexedDataPoint::getIndex));
          Range<Double> range = createNewNonOverlappingRange(dataPointRanges,
              tolerance.getToleranceRange(dp.getMZ()));
          dataPointRanges.put(range, dplist);
        }
      }

      // now add the datapoint to the set
      dplist.add(dp);
    }

    final int numDps = dataPointRanges.asMapOfRanges().size();
    final TDoubleArrayList newIntensities = new TDoubleArrayList(numDps);
    final TDoubleArrayList newMzs = new TDoubleArrayList(numDps);

    // now we got everything in place and have to calculate the new intensities and mzs
    for (Entry<Range<Double>, SortedSet<IndexedDataPoint>> entry : dataPointRanges.asMapOfRanges()
        .entrySet()) {
      if (minNumPeaks != null && entry.getValue().size() < minNumPeaks) {
        continue;
      }

      double[] mzs = entry.getValue().stream().mapToDouble(IndexedDataPoint::getMZ).toArray();
      double[] intensities = entry.getValue().stream().mapToDouble(IndexedDataPoint::getIntensity)
          .toArray();

      double newMz = mzCenterFunction.calcCenter(mzs, intensities);
      double newIntensity = switch (intensityMergingType) {
        case SUMMED -> Arrays.stream(intensities).sum();
        case MAXIMUM -> Arrays.stream(intensities).max().orElse(0d);
        case AVERAGE -> Arrays.stream(intensities).average().orElse(0d);
      };

      if (outputNoiseLevel == null || newIntensity > outputNoiseLevel) {
        newMzs.add(newMz);
        newIntensities.add(newIntensity);
      }
    }

    return new double[][]{newMzs.toArray(), newIntensities.toArray()};
  }

  /**
   * Creates a new non overlapping range for this range map. Ranges are created seamless, therefore
   * no gaps are introduced during this process.
   *
   * @param rangeMap
   * @param proposedRange The proposed range must not enclose a range in this map without
   *                      overlapping, otherwise the enclosed range will be deleted.
   * @return
   */
  public static Range<Double> createNewNonOverlappingRange(RangeMap<Double, ?> rangeMap,
      final Range<Double> proposedRange) {

    Entry<Range<Double>, ?> lowerEntry = rangeMap.getEntry(
        proposedRange.lowerBoundType() == BoundType.CLOSED ? proposedRange.lowerEndpoint()
            : proposedRange.lowerEndpoint() + EPSILON);

    Entry<Range<Double>, ?> upperEntry = rangeMap.getEntry(
        proposedRange.upperBoundType() == BoundType.CLOSED ? proposedRange.upperEndpoint()
            : proposedRange.upperEndpoint() - EPSILON);

    if (lowerEntry == null && upperEntry == null) {
      return proposedRange;
    }

    if (lowerEntry != null && proposedRange.intersection(lowerEntry.getKey()).isEmpty()
        && upperEntry == null) {
      return proposedRange;
    }

    if (upperEntry != null && proposedRange.intersection(upperEntry.getKey()).isEmpty()
        && lowerEntry == null) {
      return proposedRange;
    }

    if (upperEntry != null && lowerEntry != null && proposedRange.intersection(lowerEntry.getKey())
        .isEmpty() && proposedRange.intersection(upperEntry.getKey()).isEmpty()) {
      return proposedRange;
    }

    BoundType lowerBoundType = proposedRange.lowerBoundType();
    BoundType upperBoundType = proposedRange.upperBoundType();
    double lowerBound = proposedRange.lowerEndpoint();
    double upperBound = proposedRange.upperEndpoint();

    // check if the ranges actually overlap or if they are closed and open
    if (lowerEntry != null && !proposedRange.intersection(lowerEntry.getKey()).isEmpty()) {
      lowerBound = lowerEntry.getKey().upperEndpoint();
      lowerBoundType = BoundType.OPEN;
    }
    if (upperEntry != null && !proposedRange.intersection(upperEntry.getKey()).isEmpty()) {
      upperBound = upperEntry.getKey().lowerEndpoint();
      upperBoundType = BoundType.OPEN;
    }

    return createNewNonOverlappingRange(rangeMap,
        Range.range(lowerBound, lowerBoundType, upperBound, upperBoundType));
  }

  /**
   * Creates a merged MS/MS spectrum for a PASEF {@link PasefMsMsInfo}.
   *
   * @param info                     The MS/MS info to create a merged spectrum for
   * @param tolerance                The m/z tolerence to merge peaks from separate mobility scans
   *                                 with.
   * @param intensityMergingType     The merging type. Usually {@link IntensityMergingType#SUMMED}.
   * @param storage                  The storage to use or null.
   * @param mobilityRange            If the MS/MS shall only be created for a specific mobility
   *                                 range, e.g., in the case of isomeric features that have been
   *                                 resolved.
   * @param outputNoiseLevelAbs      Minimum intensity to be achieved in the merged intensity. May
   *                                 be null or 0.
   * @param outputNoiseLevelRelative minimum relative intensity in the merged spectrum may be null.
   * @return A {@link MergedMsMsSpectrum} or null the spectrum would not have any data points.
   */
  @Nullable
  public static MergedMsMsSpectrum getMergedMsMsSpectrumForPASEF(@NotNull final PasefMsMsInfo info,
      @NotNull final MZTolerance tolerance,
      @NotNull final SpectraMerging.IntensityMergingType intensityMergingType,
      @Nullable final MemoryMapStorage storage, @Nullable Range<Float> mobilityRange,
      @Nullable final Double outputNoiseLevelAbs, @Nullable Double outputNoiseLevelRelative,
      @Nullable final Integer minNumPeaks) {

    final Range<Integer> spectraNumbers = info.getSpectrumNumberRange();
    final Frame frame = info.getMsMsFrame();

    List<MobilityScan> mobilityScans = frame.getMobilityScans().stream().filter(
        ms -> spectraNumbers.contains(ms.getMobilityScanNumber()) && (mobilityRange == null
                                                                      || mobilityRange.contains(
            (float) ms.getMobility()))).collect(Collectors.toList());

    if (mobilityScans.isEmpty()) {
      return null;
    }

    final CenterFunction cf = DEFAULT_CENTER_FUNCTION;

    final List<MassList> massLists = mobilityScans.stream().map(MobilityScan::getMassList)
        .filter(Objects::nonNull).collect(Collectors.toList());

    if (massLists.isEmpty()) {
      logger.info(
          "Cannot calculate a merged MS/MS spectrum, because MS2 Scans do not contain a MassList.");
      return null;
    }

    double[][] merged = calculatedMergedMzsAndIntensities(massLists, tolerance,
        intensityMergingType, cf, null, outputNoiseLevelAbs, minNumPeaks);

    if (merged[0].length == 0) {
      return null;
    }

    if (outputNoiseLevelRelative != null) {
      double minRelative = Arrays.stream(merged[1]).max().orElse(0d) * outputNoiseLevelRelative;
      if (outputNoiseLevelAbs == null || (minRelative > outputNoiseLevelAbs)) {
        // if the min relative intensity is smaller than the absolute, we don't need to recalc here
        merged = calculatedMergedMzsAndIntensities(massLists, tolerance, intensityMergingType, cf,
            null, minRelative, minNumPeaks);
      }
    }

    final MsMsInfo copy = info.createCopy();
    copy.setMsMsScan(frame);
    return new SimpleMergedMsMsSpectrum(storage, merged[0], merged[1], copy, frame.getMSLevel(),
        mobilityScans, intensityMergingType, cf, MergingType.PASEF_SINGLE);
  }

  /**
   * Cannot return null as it's used for grouping
   *
   * @return the collision energy or -1 if null
   */
  private static float getCollisionEnergy(final Scan spec) {
    if (spec instanceof MergedMsMsSpectrum merged) {
      return merged.getCollisionEnergy();
    }
    MsMsInfo info = spec.getMsMsInfo();
    if (info != null) {
      return Objects.requireNonNullElse(info.getActivationEnergy(), -1f);
    }
    return -1f;
  }

  @Nullable
  public static MergedMassSpectrum extractSummedMobilityScan(@NotNull final ModularFeature f,
      @NotNull final MZTolerance tolerance, @NotNull final Range<Float> mobilityRange,
      @Nullable final MemoryMapStorage storage) {
    return extractSummedMobilityScan(f, tolerance, mobilityRange, Range.all(), storage);
  }

  @Nullable
  public static MergedMassSpectrum extractSummedMobilityScan(@NotNull final ModularFeature f,
      @NotNull final MZTolerance tolerance, @NotNull final Range<Float> mobilityRange,
      @NotNull final Range<Float> rtRange, @Nullable final MemoryMapStorage storage) {
    if (!(f.getFeatureData() instanceof IonMobilogramTimeSeries series)) {
      return null;
    }

    final List<MobilityScan> scans = series.getMobilograms().stream()
        .<MobilityScan>mapMulti((s, c) -> {
          for (var spectrum : s.getSpectra()) {
            if (mobilityRange.contains((float) spectrum.getMobility()) && rtRange.contains(
                spectrum.getRetentionTime())) {
              c.accept(spectrum);
            }
          }
        }).toList();

    // todo use mass lists over raw scans to merge (separate PR)

    final double[][] merged = calculatedMergedMzsAndIntensities(scans, tolerance,
        IntensityMergingType.SUMMED, DEFAULT_CENTER_FUNCTION, null, null, null);

    return new SimpleMergedMassSpectrum(storage, merged[0], merged[1], 1, scans,
        IntensityMergingType.SUMMED, DEFAULT_CENTER_FUNCTION, MergingType.ALL_ENERGIES);
  }

  /**
   * @return A summed spectrum with the given tolerances.
   */
  public static <T extends MassSpectrum> MergedMassSpectrum mergeSpectra(
      final @NotNull List<T> source, @NotNull final MZTolerance tolerance,
      final MergingType mergeType, @Nullable final MemoryMapStorage storage) {
    return mergeSpectra(source, tolerance, IntensityMergingType.SUMMED, mergeType,
        DEFAULT_CENTER_FUNCTION, storage);
  }

  public static <T extends MassSpectrum> MergedMassSpectrum mergeSpectra(
      final @NotNull List<T> source, @NotNull final MZTolerance tolerance,
      final MergingType mergeType, final IntensityMergingType intensityMergeType,
      @Nullable final MemoryMapStorage storage) {
    return mergeSpectra(source, tolerance, intensityMergeType, mergeType, DEFAULT_CENTER_FUNCTION,
        storage);
  }

  public static <T extends MassSpectrum> MergedMassSpectrum mergeSpectra(
      final @NotNull List<T> source, @NotNull final MZTolerance tolerance,
      IntensityMergingType intensityMergingType, MergingType mergeType,
      final CenterFunction centerFunction, @Nullable final MemoryMapStorage storage) {

    return mergeSpectra(source, tolerance, intensityMergingType, mergeType, null, null, null,
        centerFunction, storage);
  }

  public static <T extends MassSpectrum> MergedMassSpectrum mergeSpectra(
      final @NotNull List<T> source, @NotNull final MZTolerance tolerance,
      IntensityMergingType intensityMergingType, MergingType mergeType,
      @Nullable Double inputNoiseLevel, @Nullable Double outputNoiseLevel,
      @Nullable Integer minNumPeaks, CenterFunction centerFunction,
      @Nullable final MemoryMapStorage storage) {

    // if we have mass lists, use them to merge.
    final var spectra = source.stream().map(ScanUtils::getMassListOrThrow).toList();

    final double[][] mzIntensities = calculatedMergedMzsAndIntensities(spectra, tolerance,
        intensityMergingType, centerFunction, inputNoiseLevel, outputNoiseLevel, minNumPeaks);
    final int msLevel = source.stream().filter(s -> s instanceof Scan)
        .mapToInt(s -> ((Scan) s).getMSLevel()).min().orElse(1);

    if (msLevel > 1) {
      // Just use the one with the lowest MS level.
      // source scans have all MsMsInfos inside merged scan
      var copy = source.stream().map(SpectraMerging::getMsMsInfo).filter(Objects::nonNull)
          .min(Comparator.comparingInt(MsMsInfo::getMsLevel)).map(MsMsInfo::createCopy)
          .orElse(null);
      return new SimpleMergedMsMsSpectrum(storage, mzIntensities[0], mzIntensities[1], copy,
          msLevel, source, intensityMergingType, centerFunction, mergeType);
    }
    return new SimpleMergedMassSpectrum(storage, mzIntensities[0], mzIntensities[1], msLevel,
        source, intensityMergingType, centerFunction, mergeType);
  }

  public static @Nullable MsMsInfo getMsMsInfo(MassSpectrum spec) {
    if (spec instanceof Scan scan) {
      return scan.getMsMsInfo();
    } else {
      return null;
    }
  }

  public static Frame getMergedFrame(@Nullable final MemoryMapStorage storage,
      @NotNull final MZTolerance tolerance, @NotNull final Collection<Frame> frames,
      final int mobilityScanBin, @NotNull final IntensityMergingType intensityMergingType,
      @Nullable final Double inputNoiseLevel, @Nullable final Double outputNoiseLevelAbs,
      @Nullable final Integer minMobilityPeaks, @NotNull final AtomicDouble progress) {
    if (frames.isEmpty()) {
      throw new IllegalStateException("No frames in collection to be merged.");
    }

    final Frame aFrame = frames.stream().findAny().get();
    final int msLevel = aFrame.getMSLevel();
    final PolarityType polarityType = aFrame.getPolarity();
    float lowestRt = Float.MAX_VALUE;
    float highestRt = Float.MIN_VALUE;
    Range<Double> scanMzRange = aFrame.getScanningMZRange();

    // map all mobility scans that shall be merged together into the same list
    final Map<Integer, List<MobilityScan>> scanMap = new HashMap<>();
    for (final Frame frame : frames) {
      for (final MobilityScan mobilityScan : frame.getMobilityScans()) {
        final List<MobilityScan> mobilityScans = scanMap.computeIfAbsent(
            mobilityScan.getMobilityScanNumber() / mobilityScanBin, i -> new ArrayList<>());
        mobilityScans.add(mobilityScan);
      }

      if (frame.getRetentionTime() < lowestRt) {
        lowestRt = frame.getRetentionTime();
      }
      if (frame.getRetentionTime() > highestRt) {
        highestRt = frame.getRetentionTime();
      }
      if (!scanMzRange.equals(frame.getScanningMZRange()) && !scanMzRange.encloses(
          frame.getScanningMZRange())) {
        scanMzRange = scanMzRange.span(frame.getScanningMZRange());
      }

      if (msLevel != frame.getMSLevel()) {
        throw new AssertionError("Cannot merge frames of different MS levels");
      }
      if (polarityType != frame.getPolarity()) {
        throw new AssertionError("Cannot merge frames of different polarities");
      }
    }

    final CenterFunction cf = new CenterFunction(CenterMeasure.AVG,
        CenterFunction.DEFAULT_MZ_WEIGHTING);
    final IMSRawDataFile file = (IMSRawDataFile) frames.stream().findAny().get().getDataFile();

    final SimpleFrame frame = new SimpleFrame(file, -1, msLevel, (highestRt + lowestRt) / 2, null,
        null, MassSpectrumType.CENTROIDED, polarityType,
        String.format("Merged frame (%.2f-%.2f)", lowestRt, highestRt), scanMzRange,
        aFrame.getMobilityType(), null, null);

    final AtomicInteger processed = new AtomicInteger(0);
    final double totalFrames = scanMap.size();

    // create a merged spectrum for each mobility scan bin
    final List<BuildingMobilityScan> buildingMobilityScans = scanMap.entrySet().parallelStream()
        .map(entry -> {
          final List<? extends MassSpectrum> spectra;

          final List<MassList> massLists = entry.getValue().stream().map(MobilityScan::getMassList)
              .filter(Objects::nonNull).toList();
          if (massLists.isEmpty()) {
            spectra = entry.getValue();
          } else {
            if (massLists.size() != entry.getValue().size()) {
              throw new IllegalArgumentException(
                  "Not all mobility scans contain a mass list. Cannot merge Frames.");
            }
            spectra = massLists;
          }
          final double[][] mzIntensities = calculatedMergedMzsAndIntensities(spectra, tolerance,
              intensityMergingType, cf, inputNoiseLevel, outputNoiseLevelAbs, minMobilityPeaks);

          processed.getAndIncrement();
          progress.set(processed.get() / totalFrames);

          return new BuildingMobilityScan(entry.getKey(), mzIntensities[0], mzIntensities[1]);
        }).sorted(Comparator.comparingInt(BuildingMobilityScan::getMobilityScanNumber)).toList();

    final double[] mobilities = new double[scanMap.size()];
    int i = 0;
    for (Integer num : scanMap.keySet()) {
      mobilities[i] = aFrame.getMobilityScan(Math.min(num * mobilityScanBin + (mobilityScanBin / 2),
          aFrame.getNumberOfMobilityScans() - 1)).getMobility();
      i++;
    }

    frame.setMobilityScans(buildingMobilityScans, true);
    frame.setMobilities(mobilities);
    double[][] mergedSpectrum = calculatedMergedMzsAndIntensities(frames, tolerance,
        intensityMergingType, cf, null, null, null);
    frame.setDataPoints(mergedSpectrum[0], mergedSpectrum[1]);
    return frame;
  }

  public enum IntensityMergingType implements UniqueIdSupplier {
    SUMMED("Summed"), MAXIMUM("Maximum value"), AVERAGE("Average value");

    private final String label;

    IntensityMergingType(String label) {
      this.label = label;
    }

    public static IntensityMergingType parseOrElse(final String value,
        @Nullable final IntensityMergingType defaultValue) {
      return UniqueIdSupplier.parseOrElse(value, values(), defaultValue);
    }

    @Override
    public String toString() {
      return this.label;
    }

    public String getDescription() {
      return label + switch (this) {
        case SUMMED, AVERAGE ->
            " decreases the impact of random noise signals that are only present in some scans, however, it may over represent intense signals, because smaller signals may fall under the noise level in some scans, especially when acquiring scans with different fragmentation energies.";
        case MAXIMUM -> " retains the general amplitude of spectral data.";
      };
    }

    @Override
    public @NotNull String getUniqueID() {
      return switch (this) {
        case SUMMED -> "SUMMED";
        case MAXIMUM -> "MAXIMUM";
        case AVERAGE -> "AVERAGE";
      };
    }
  }
}
