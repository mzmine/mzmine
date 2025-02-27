package io.github.mzmine.modules.dataprocessing.align_gc;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.PseudoSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.features.types.numbers.GcAlignMissingNumFeaturesType;
import io.github.mzmine.datamodel.features.types.numbers.GcAlignShiftedNumFeaturesType;
import io.github.mzmine.modules.dataprocessing.align_common.FeatureAlignmentPostProcessor;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.scans.SpectraMerging;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GCConsensusAlignerPostProcessor implements FeatureAlignmentPostProcessor {

  private static final Logger logger = Logger.getLogger(
      GCConsensusAlignerPostProcessor.class.getName());
  private final AtomicInteger shiftedFeatures = new AtomicInteger(0);
  private final MZTolerance mzTol;

  public GCConsensusAlignerPostProcessor(final MZTolerance mzTol) {
    this.mzTol = mzTol;
  }

  /**
   * Set the same mz feature to all samples in all rows
   */
  @Override
  public void handlePostAlignment(final ModularFeatureList flist) {
    // debugging info types - kept for now so that users may provide more info for debugging
    flist.addRowType(DataTypes.get(GcAlignMissingNumFeaturesType.class));
    flist.addRowType(DataTypes.get(GcAlignShiftedNumFeaturesType.class));

    int missing = flist.parallelStream().mapToInt(row -> setConsensusMainFeature(flist, row)).sum();
    logger.fine("""
        Done with setting consensus main features in GC alignment. /
        There were %d main signals that had no corresponding raw data signal in some samples. /
        Those features were excluded from alignment.
        Also there were %d signals that were shifted to the closest signal that was within 2x of the mz tolerance.
        This may point to mz shifts between samples or this was just a missing signal in a sample with a close enough other signal.""".formatted(
        missing, shiftedFeatures.get()));
  }

  /**
   * Find a consensus main feature (mz) for all samples of this row. This will be the mz signal
   * found in most {@link PseudoSpectrum} of GC-EI-MS spectra and the highest abundance across
   * samples.
   *
   * @return the number of ignored features that were excluded from alignment because a signal was
   * missing
   */
  private int setConsensusMainFeature(final ModularFeatureList flist, final FeatureListRow row) {
    final List<AlignedDataPoint> mostCommonDataPoints = countCommonSignalsInFragmentSpectra(row);

    AlignedDataPoint best = mostCommonDataPoints.getFirst();
    final double meanMz = best.getAverageMz();
    final var mzTolRange = mzTol.getToleranceRange(meanMz);
    final double mzTolRangeLength = RangeUtils.rangeLength(mzTolRange);

    int shifted = 0;
    List<ModularFeature> featuresToAdd = new ArrayList<>();
    for (final ModularFeature oldFeature : row.getFeatures()) {
      if (mzTolRange.contains(oldFeature.getMZ())) {
        featuresToAdd.add(oldFeature); // just add the old feature
        continue;
      }
      // extract new for features with different mz
      var newFeature = extractNewFeature(flist, oldFeature, mzTolRange);

      if (newFeature == null) {
        // No data found for this signal in this raw data file - maybe range was too narrow?
        // try to shift to the closest signal in max scan and then use a range around this
        newFeature = tryRecenterMzToClosestSignal(flist, oldFeature, meanMz, mzTolRangeLength);
        if (newFeature != null) {
          shifted++;
        }
      }

      // finally add
      if (newFeature != null) {
        featuresToAdd.add(newFeature);
      }
    }

    shiftedFeatures.addAndGet(shifted);
    // remove all features
    int missing = row.getNumberOfFeatures() - featuresToAdd.size();
    row.clearFeatures(false);

    // finally replace features
    for (final ModularFeature nf : featuresToAdd) {
      // do not update bindings - this is done later in the alignment
      row.addFeature(nf.getRawDataFile(), nf, false);
    }

    // debugging info types - kept for now so that users may provide more info for debugging
    row.set(GcAlignMissingNumFeaturesType.class, missing);
    row.set(GcAlignShiftedNumFeaturesType.class, shifted);

    return missing;
  }

  /**
   * Extract a new feature within an mzTolRange and with the same scans as an old feature
   */
  public @Nullable ModularFeature extractNewFeature(final ModularFeatureList flist,
      final ModularFeature feature, final Range<Double> mzTolRange) {
    // mz mismatch, because GC retains a random m/z as a representative for a feature (deconvoluted pseudo spectrum)
    RawDataFile dataFile = feature.getRawDataFile();
    // there might be missing data points / gaps in feature.getScanNumbers
    // therefore extract from raw file in RT range
    List<? extends Scan> seletedScans = flist.getSeletedScans(feature.getRawDataFile());
    seletedScans = BinarySearch.indexRange(feature.getRawDataPointsRTRange(), seletedScans,
        Scan::getRetentionTime).sublist(seletedScans);
    IonTimeSeries<Scan> ionTimeSeries = IonTimeSeriesUtils.extractIonTimeSeries(dataFile,
        seletedScans, mzTolRange, feature.getRawDataPointsRTRange(),
        dataFile.getMemoryMapStorage());

    final ModularFeature newFeature;
    newFeature = new ModularFeature(flist, feature);
    newFeature.set(FeatureDataType.class, ionTimeSeries);
    FeatureDataUtils.recalculateIonSeriesDependingTypes(newFeature);
    if (newFeature.getHeight() > 0) {
      return newFeature;
    }
    return null;
  }

  /**
   * @return list of aligned data points sorted by decreasing number of detections and intensity
   */
  private @NotNull List<AlignedDataPoint> countCommonSignalsInFragmentSpectra(
      final FeatureListRow row) {
    // count how many features actually contain a signal at specific mz values
    final RangeMap<Double, AlignedDataPoint> signalCounter = countMostIntenseSignalPerSampleFeature(
        row);

    // first is the one with the highest sum intensity across all samples
    List<AlignedDataPoint> mostCommonDataPoints = signalCounter.asMapOfRanges().values().stream()
        .sorted(Comparator.comparing(AlignedDataPoint::numDetections)
            .thenComparing(AlignedDataPoint::sumIntensity).reversed()).toList();
    return mostCommonDataPoints;
  }

  private @NotNull RangeMap<Double, AlignedDataPoint> countMostIntenseSignalPerSampleFeature(
      final FeatureListRow row) {

    RangeMap<Double, AlignedDataPoint> signalCounter = TreeRangeMap.create();
    // start with most abundant data point in all scans
    // list of spectra as list of data points
    prepareMostIntenseFragmentScans(row).stream()
        .sorted(Comparator.comparing(RawFileDataPoint::getIntensity, Comparator.reverseOrder()))
        .forEach(fdp -> {
          final double mz = fdp.getMZ();
          final RawDataFile rawFile = fdp.rawFile();
          final AlignedDataPoint alignedDp = signalCounter.get(mz);
          if (alignedDp != null) {
            final Map<RawDataFile, DataPoint> consensusMap = alignedDp.mostAbundantDetection();
            // only put a new data point if this is the first dp for this sample (feature)
            consensusMap.computeIfAbsent(rawFile, _ -> fdp);
          } else {
            // start counter - use smaller range in case of overlaps
            var mzRange = SpectraMerging.createNewNonOverlappingRange(signalCounter,
                mzTol.getToleranceRange(mz));
            Map<RawDataFile, DataPoint> detected = HashMap.newHashMap(row.getNumberOfFeatures());
            detected.put(rawFile, fdp);
            signalCounter.put(mzRange, new AlignedDataPoint(mzRange, detected));
          }
        });
    return signalCounter;
  }

  private static @NotNull List<RawFileDataPoint> prepareMostIntenseFragmentScans(
      final FeatureListRow row) {
    return row.streamFeatures().<RawFileDataPoint>mapMulti((f, consumer) -> {
      final RawDataFile raw = f.getRawDataFile();

      final Scan scan = f.getMostIntenseFragmentScan();
      for (int i = 0; i < scan.getNumberOfDataPoints(); i++) {
        consumer.accept(new RawFileDataPoint(raw, scan.getMzValue(i), scan.getIntensityValue(i)));
      }
    }).toList();
  }

  private ModularFeature tryRecenterMzToClosestSignal(final ModularFeatureList flist,
      final ModularFeature oldFeature, final double meanMz, final double maxAllowedMzDistance) {
    ModularFeature newFeature;
    var scan = oldFeature.getRepresentativeScan();
    int index = scan.binarySearch(meanMz, DefaultTo.CLOSEST_VALUE);
    final double closestMz = scan.getMzValue(index);
    // allow 2x tolerance as the shift to one side
    // then center on this new mz to try and extract new feature
    if (Math.abs(closestMz - meanMz) < maxAllowedMzDistance) {
      newFeature = extractNewFeature(flist, oldFeature, mzTol.getToleranceRange(closestMz));
      if (newFeature != null) {
        // this time we found a feature with signals
        return newFeature;
      }
//      else {
      // otherwise just ignore this feature and do not align it?
      // TODO other option would be to align feature and just set a different intensity and mz?
      // not sure because the recalculate ion time series dependent types would then change values again
//      }
    }
    return null;
  }

  private record RawFileDataPoint(RawDataFile rawFile, double mz, double intensity) implements DataPoint {

    @Override
    public double getMZ() {
      return mz;
    }

    @Override
    public double getIntensity() {
      return intensity;
    }
  }

  /**
   * @param initialMz             the range used to align data points
   * @param mostAbundantDetection most abundant data point in mz range
   */
  private record AlignedDataPoint(@NotNull Range<Double> initialMz,
                                  @NotNull Map<RawDataFile, DataPoint> mostAbundantDetection) {

    public int numDetections() {
      return mostAbundantDetection.size();
    }

    public double sumIntensity() {
      double sum = 0;
      for (final DataPoint dp : mostAbundantDetection.values()) {
        sum += dp.getIntensity();
      }
      return sum;
    }

    public double getAverageMz() {
      return detectedPoints().stream().mapToDouble(DataPoint::getMZ).average().orElse(0d);
    }

    public Collection<DataPoint> detectedPoints() {
      return mostAbundantDetection.values();
    }
  }

}
