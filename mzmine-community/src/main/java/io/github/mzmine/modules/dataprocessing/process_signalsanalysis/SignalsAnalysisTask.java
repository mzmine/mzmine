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

package io.github.mzmine.modules.dataprocessing.process_signalsanalysis;

import static io.github.mzmine.util.DataPointUtils.removePrecursorMz;
import static io.github.mzmine.util.scans.ScanUtils.findAllMS2FragmentScans;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.analysis.InSourceFragmentsAnalysisType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The SignalsAnalysisTask is responsible for analyzing the signals from feature lists. The task
 * will be scheduled by the TaskController and progress is calculated from the
 * finishedItems/totalItems. It compares all MS1 signals present in the feature mass spectrum to the
 * MS2 signals present in all the fragment scans corresponding to precursors corresponding to MS1
 * signals.
 */
class SignalsAnalysisTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(SignalsAnalysisTask.class.getName());
  private final List<FeatureList> featureLists;
  private final boolean useMassList;
  private final MZTolerance tolerance;

  /**
   * Constructor to initialize the task with necessary parameters.
   *
   * @param project        The MZmine project.
   * @param featureLists   The feature lists to process.
   * @param parameters     User parameters.
   * @param storage        Optional memory map storage.
   * @param moduleCallDate The date the module was called.
   * @param moduleClass    The class of the calling module.
   */
  public SignalsAnalysisTask(MZmineProject project, List<FeatureList> featureLists,
      ParameterSet parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.featureLists = featureLists;
    this.totalItems = featureLists.stream().mapToInt(FeatureList::getNumberOfRows).sum();
    var scanDataType = parameters.getValue(SignalsAnalysisParameters.scanDataType);
    this.useMassList = scanDataType == ScanDataType.MASS_LIST;
    this.tolerance = parameters.getValue(SignalsAnalysisParameters.tolerance);
  }

  private static double calcSumIntensity(final List<UniqueSignal> ms1SignalMatchesMs2) {
    return ms1SignalMatchesMs2.stream().mapToDouble(UniqueSignal::sumIntensity).sum();
  }

  @Override
  protected void process() {
    List<GroupedSignalScans> groupedScans = collectSpectra();
    logger.info("Collected spectra - now starting to analyze the grouped scans.");
  }

  /**
   * Collects spectra from feature lists.
   *
   * @return A list of grouped signal scans.
   */
  private List<GroupedSignalScans> collectSpectra() {
    List<GroupedSignalScans> groupingResults = new ArrayList<>();
    for (FeatureList featureList : featureLists) {
      for (FeatureListRow row : featureList.getRows()) {
        GroupedSignalScans result = processRow(row);
        groupingResults.add(result);
      }
    }
    return groupingResults;
  }

  /**
   * Processes a single row from the feature list.
   *
   * @param row The feature list row.
   * @return The grouped signal scans.
   */
  private GroupedSignalScans processRow(FeatureListRow row) {
    List<Scan> ms1Scans = new ArrayList<>();
    List<Scan> ms2Scans = new ArrayList<>();
    for (final ModularFeature feature : row.getFeatures()) {
      try {
        List<Scan> exportedScans = processFeature(feature);
        for (Scan scan : exportedScans) {
          if (scan.getMSLevel() == 1) {
            ms1Scans.add(scan);
          } else if (scan.getMSLevel() == 2) {
            ms2Scans.add(scan);
          }
        }
      } catch (Exception ex) {
        logger.log(Level.WARNING, ex.getMessage(), ex);
      }
    }

    SignalsAnalysisResult analysisResult = countUniqueSignalsBetweenMs1AndMs2(ms1Scans, ms2Scans,
        tolerance, row.getAverageMZ());

    row.set(InSourceFragmentsAnalysisType.class, analysisResult.results);

    return new GroupedSignalScans(row, ms1Scans, ms2Scans);
  }

  /**
   * Processes a single feature.
   *
   * @param feature The feature to process.
   * @return The list of exported scans. The representative MS1 and all MS2 scans that match the
   * retention time range of this feature and any MS1 signal in best MS1
   */
  private List<Scan> processFeature(final Feature feature) {
    List<Scan> scansToExport = new ArrayList<>();
    Scan bestMs1 = feature.getRepresentativeScan();
    if (bestMs1 != null) {
      scansToExport.add(bestMs1);
      // TODO: This could be a parameter to get it over the whole file instead
      Range<Float> rawRtRange = feature.getRawDataPointsRTRange();
      RawDataFile raw = feature.getRawDataFile();
      DataPoint[] dataPoints = ScanUtils.extractDataPoints(bestMs1, useMassList);
      for (DataPoint dp : dataPoints) {
        double ms1Signal = dp.getMZ();
        Scan[] fragmentScansBroad = findAllMS2FragmentScans(raw, rawRtRange,
            tolerance.getToleranceRange(ms1Signal));
        for (Scan ms2 : fragmentScansBroad) {
          if (ms2 != null) {
            scansToExport.add(ms2);
          }
        }
      }
    }
    feature.getAllMS2FragmentScans().stream().filter(scan -> scan.getMSLevel() == 2)
        .forEach(scansToExport::add);
    return scansToExport.stream().distinct().toList();
  }

  @Override
  public String getTaskDescription() {
    return "Signals analysis task running on " + featureLists;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return featureLists;
  }

  /**
   * Counts unique signals between MS1 and MS2 scans.
   *
   * @param ms1Scans    The MS1 scans.
   * @param ms2Scans    The MS2 scans.
   * @param tolerance   The MZ tolerance.
   * @param precursorMz
   * @return The results of the signal count.
   */
  private SignalsAnalysisResult countUniqueSignalsBetweenMs1AndMs2(List<Scan> ms1Scans,
      List<Scan> ms2Scans, MZTolerance tolerance, final Double precursorMz) {
    // require signal to be in 90% of MS1 scans
    int minMs1Scans = (int) Math.ceil(ms1Scans.size() * 0.9);
    var ms1SignalMap = filterMap(collectUniqueSignalsFromScans(ms1Scans, tolerance), minMs1Scans);

    // in MS2 this may not be true if different energies used, so apply no filter
    var ms2SignalMap = collectUniqueSignalsFromScans(ms2Scans, tolerance);

    List<UniqueSignal> ms1Signals = mapToList(ms1SignalMap);

    List<UniqueSignal> ms1SignalMatchesMs2 = ms1Signals.stream()
        .filter(signal -> ms2SignalMap.get(signal.mz()) != null).toList();

    // TODO this is the MS2 scans of one row so all of them have the same m/z - maybe this should be done globally for all rows?
    // you could build a RangeMap<Double, UniqueSignal> of all rows with fragment spectra (precursor m/z)
    // before looping over all rows and pass it into this method
    // Or maybe we need to accumulate all MS2 fragment signals over all scans?
    List<UniqueSignal> ms1SignalMatchesMs2Precursors = filterUniquePrecursors(ms1SignalMap,
        ms2Scans);
    Set<Double> precursorMzSet = ms1SignalMatchesMs2Precursors.stream().map(UniqueSignal::mz)
        .collect(Collectors.toSet());
    List<UniqueSignal> ms1FragmentedSignalMatchesMs2 = ms1Signals.stream()
        .filter(signal -> precursorMzSet.contains(signal.mz()))
        .filter(signal -> ms2SignalMap.get(signal.mz()) != null).toList();

    double ms1IntensityTotal = calcSumIntensity(ms1Signals);
    double ms1IntensityFragmentedPercent =
        calcSumIntensity(ms1SignalMatchesMs2Precursors) / ms1IntensityTotal;
    double ms1IntensityMatched = calcSumIntensity(ms1SignalMatchesMs2);
    double ms1IntensityMatchedPercent = ms1IntensityMatched / ms1IntensityTotal;

    int ms1SignalsTotal = ms1Signals.size();
    int ms1SignalsFragmented = ms1SignalMatchesMs2Precursors.size();
    double ms1SignalsFragmentedPercent = ms1SignalsFragmented / (double) ms1SignalsTotal;
    int signalsMatched = ms1SignalMatchesMs2.size();
    double ms1SignalsMatchedPercent = signalsMatched / (double) ms1SignalsTotal;

    // for all precursor ions
    int ms2SignalsAllPrecursors = mapToList(ms2SignalMap).size();

    // for this precursor ion
    List<UniqueSignal> ms2Signals = mapToList(ms2SignalMap).stream()
        .filter(signal -> originatesFromPrecursorIon(signal, tolerance, precursorMz)).toList();
    List<UniqueSignal> ms2SignalMatchesMs1 = ms2Signals.stream()
        .filter(signal -> ms1SignalMap.get(signal.mz()) != null).toList();

    int ms2SignalsTotal = ms2Signals.size();
    double ms2SignalsMatchedPercent = signalsMatched / (double) ms2SignalsTotal;
    double ms2IntensityMatched = calcSumIntensity(ms2SignalMatchesMs1);
    double ms2IntensityTotal = calcSumIntensity(ms2Signals);
    double ms2IntensityMatchedPercent = ms2IntensityMatched / ms2IntensityTotal;

    /*
     * The precursor with the highest m/z will never be recognized as ISF.
     * Same if there is only one MS2 scan over the whole range.
     */
    double ms1SignalsFragmentedLikelyISFPercent =
        ms1FragmentedSignalMatchesMs2.size() / (double) ms1SignalsFragmented;

    // Create a list of ISF precursor m/z values
    List<Double> likelyISFPrecursorMzs = ms1FragmentedSignalMatchesMs2.stream()
        .map(UniqueSignal::mz).collect(Collectors.toList());

    //
    boolean isLikelyISF = likelyISFPrecursorMzs.stream()
        .anyMatch(mz -> tolerance.checkWithinTolerance(precursorMz, mz));

    InSourceFragmentAnalysisResults results = new InSourceFragmentAnalysisResults(isLikelyISF,
        ms1SignalsFragmentedLikelyISFPercent, signalsMatched, ms1SignalsTotal,
        ms2SignalsAllPrecursors, ms1SignalsFragmented, ms1SignalsFragmentedPercent,
        ms1IntensityFragmentedPercent, ms1SignalsMatchedPercent, ms1IntensityMatchedPercent,
        ms2SignalsTotal, ms2SignalsMatchedPercent, ms2IntensityMatchedPercent);

    return new SignalsAnalysisResult(results, likelyISFPrecursorMzs);
  }

  private static boolean originatesFromPrecursorIon(final UniqueSignal signal,
      final MZTolerance tolerance, final Double precursorMz) {
    return signal.streamPrecursorMzs()
        .anyMatch(mz -> tolerance.checkWithinTolerance(precursorMz, mz));
  }

  private List<UniqueSignal> mapToList(final RangeMap<Double, UniqueSignal> map) {
    return new ArrayList<>(map.asMapOfRanges().values());
  }

  private @NotNull RangeMap<Double, UniqueSignal> filterMap(
      final RangeMap<Double, UniqueSignal> uniqueMs1Map, final int minSamples) {
    RangeMap<Double, UniqueSignal> unique = TreeRangeMap.create();

    uniqueMs1Map.asMapOfRanges().values().stream()
        .filter(signal -> signal.numberOfScans() >= minSamples)
        .forEach(signal -> unique.put(tolerance.getToleranceRange(signal.mz()), signal));

    return unique;
  }

  /**
   * Collects unique dataPoints from scans.
   *
   * @param scans     The scans.
   * @param tolerance The MZ tolerance.
   * @return A set of unique dataPoints.
   */
  private RangeMap<Double, UniqueSignal> collectUniqueSignalsFromScans(List<Scan> scans,
      MZTolerance tolerance) {
    RangeMap<Double, UniqueSignal> unique = TreeRangeMap.create();

    for (Scan scan : scans) {
      DataPoint[] dataPoints = ScanUtils.extractDataPoints(scan, useMassList);
      if (scan.getMSLevel() > 1) {
        // TODO arbitrarily removing 1 around precursor for now
        dataPoints = removePrecursorMz(dataPoints, scan.getPrecursorMz(), 1);
      }
      // start with most abundant signals first as they are usually more important
      Arrays.stream(dataPoints).sorted(DataPointSorter.DEFAULT_INTENSITY).forEach(dp -> {
        var uniqueSignal = unique.get(dp.getMZ());
        if (uniqueSignal == null) {
          uniqueSignal = new UniqueSignal(dp, scan);
          unique.put(tolerance.getToleranceRange(dp.getMZ()), uniqueSignal);
        } else {
          uniqueSignal.add(dp, scan);
        }
      });
    }

    return unique;
  }

  /**
   * Filters unique precursors.
   *
   * @param ms1Signals The MS1 signals.
   * @param ms2Scans   The MS2 scans.
   * @return A list of unique MS1 signals that match MS2 precursors.
   */
  private List<UniqueSignal> filterUniquePrecursors(RangeMap<Double, UniqueSignal> ms1Signals,
      List<Scan> ms2Scans) {
    return ms2Scans.stream().map(Scan::getPrecursorMz).filter(Objects::nonNull)
        .map(precursormz -> ms1Signals.get(precursormz)).filter(Objects::nonNull).distinct()
        .toList();
  }

  private record SignalsAnalysisResult(InSourceFragmentAnalysisResults results,
                                       List<Double> likelyISFPrecursorMzs) {

  }

}
