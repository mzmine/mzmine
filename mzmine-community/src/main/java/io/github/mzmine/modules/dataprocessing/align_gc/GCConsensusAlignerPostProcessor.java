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
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.datamodel.features.types.numbers.GcAlignMissingNumFeaturesType;
import io.github.mzmine.datamodel.features.types.numbers.GcAlignShiftedNumFeaturesType;
import io.github.mzmine.modules.dataprocessing.align_common.FeatureAlignmentPostProcessor;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import java.util.ArrayList;
import java.util.Arrays;
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
    int missing = flist.parallelStream().mapToInt(row -> setConsensusMainFeature(flist, row)).sum();
    logger.fine("""
        Done with setting consensus main features in GC alignment. /
        There were %d main signals that had no corresponding raw data signal in some samples. /
        Those features were excluded from alignment.
        Also there were %d signals that were shifted to the closest signal to that was within 2x of the mz tolerance.
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
    final List<Map<ModularFeature, DataPoint>> mostCommonDataPoints = countCommonSignalsInFragmentSpectra(
        row);

    Map<ModularFeature, DataPoint> best = mostCommonDataPoints.getFirst();
    final double meanMz = best.values().stream().mapToDouble(DataPoint::getMZ).average().orElse(0d);
    final var mzTolRange = mzTol.getToleranceRange(meanMz);
    final double mzTolRangeLength = RangeUtils.rangeLength(mzTolRange);

    int shifted = 0;
    List<ModularFeature> featuresToReplace = new ArrayList<>();
    for (final ModularFeature oldFeature : row.getFeatures()) {
      if (mzTolRange.contains(oldFeature.getMZ())) {
        featuresToReplace.add(oldFeature); // just add the old feature
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
        featuresToReplace.add(newFeature);
      }
    }

    shiftedFeatures.addAndGet(shifted);
    // remove all features
    int missing = row.getNumberOfFeatures() - featuresToReplace.size();
    row.clearFeatures(false);

    // finally replace features
    for (final ModularFeature nf : featuresToReplace) {
      // do not update bindings - this is done later in the alignment
      row.addFeature(nf.getRawDataFile(), nf, false);
    }

    // TODO remove temporary debugging info types
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
    IonTimeSeries<Scan> ionTimeSeries = IonTimeSeriesUtils.extractIonTimeSeries(dataFile,
        feature.getScanNumbers(), mzTolRange, feature.getRawDataPointsRTRange(),
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

  private @NotNull List<Map<ModularFeature, DataPoint>> countCommonSignalsInFragmentSpectra(
      final FeatureListRow row) {
    // list of spectra as list of data points
    List<List<FeatureDataPoint>> spectra = prepareMostIntenseFragmentScans(row);

    // count how many features actually contain a signal at specific mz values
    final RangeMap<Double, Map<ModularFeature, DataPoint>> signalCounter = countMostIntenseSignalPerSampleFeature(
        row, spectra);

    // first is the one with the highest sum intensity across all samples
    List<Map<ModularFeature, DataPoint>> mostCommonDataPoints = signalCounter.asMapOfRanges()
        .values().stream().sorted(
            Comparator.comparing((Map<ModularFeature, DataPoint> t) -> t.size()).thenComparing(
                map -> map.values().stream().mapToDouble(DataPoint::getIntensity).sum()).reversed())
        .toList();
    return mostCommonDataPoints;
  }

  private @NotNull RangeMap<Double, Map<ModularFeature, DataPoint>> countMostIntenseSignalPerSampleFeature(
      final FeatureListRow row, final List<List<FeatureDataPoint>> spectra) {
    RangeMap<Double, Map<ModularFeature, DataPoint>> signalCounter = TreeRangeMap.create();
    // start with most abundant data point in all scans
    spectra.stream().flatMap(List::stream)
        .sorted(Comparator.comparing(fdp -> fdp.dp().getIntensity(), Comparator.reverseOrder()))
        .forEach(fdp -> {
          double mz = fdp.dp().getMZ();
          var feature = fdp.feature();
          var consensusMap = signalCounter.get(mz);
          if (consensusMap != null) {
            // only put a new data point if this is the first dp for this sample (feature)
            consensusMap.computeIfAbsent(feature, _ -> fdp.dp());
          } else {
            // start counter - use smaller range in case of overlaps
            var mzRange = SpectraMerging.createNewNonOverlappingRange(signalCounter,
                mzTol.getToleranceRange(mz));
            HashMap<ModularFeature, DataPoint> detected = HashMap.newHashMap(
                row.getNumberOfFeatures());
            detected.put(feature, fdp.dp());
            signalCounter.put(mzRange, detected);
          }
        });
    return signalCounter;
  }

  private static @NotNull List<List<FeatureDataPoint>> prepareMostIntenseFragmentScans(
      final FeatureListRow row) {
    return row.streamFeatures().map(f -> {
      var dps = ScanUtils.extractDataPoints(f.getMostIntenseFragmentScan());
      return Arrays.stream(dps).map(dp -> new FeatureDataPoint(f, dp)).toList();
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
      if (newFeature!=null) {
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

  private record FeatureDataPoint(ModularFeature feature, DataPoint dp) {

  }
}
