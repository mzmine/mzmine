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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Analyzes MS1 and MS2 signals from feature lists to compare MS1 signals with MS2 fragment scans.
 */
class CommonMs1Ms2FragmentsAnalysisTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(
      CommonMs1Ms2FragmentsAnalysisTask.class.getName());
  private final List<FeatureList> featureLists;
  private final boolean useMassList;
  private final MZTolerance tolerance;
  private final Boolean considerAdductsAndCo;
  private final Boolean considerIsotopes;

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
  public CommonMs1Ms2FragmentsAnalysisTask(MZmineProject project, List<FeatureList> featureLists,
      ParameterSet parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.featureLists = featureLists;
    this.totalItems = featureLists.stream().mapToInt(FeatureList::getNumberOfRows).sum();
    this.useMassList = parameters.getValue(CommonMs1Ms2FragmentsAnalysisParameters.scanDataType)
        == ScanDataType.MASS_LIST;
    this.tolerance = parameters.getValue(CommonMs1Ms2FragmentsAnalysisParameters.tolerance);
    // TODO not implemented yet
    this.considerAdductsAndCo = parameters.getValue(
        CommonMs1Ms2FragmentsAnalysisParameters.considerAdductsAndCo);
    this.considerIsotopes = parameters.getValue(
        CommonMs1Ms2FragmentsAnalysisParameters.considerIsotopes);
  }

  private static double calcSumIntensity(List<UniqueSignal> signals) {
    return signals.stream().mapToDouble(UniqueSignal::sumIntensity).sum();
  }

  @Override
  protected void process() {
    List<GroupedSignalScans> groupedScans = gatherSpectra();
    logger.info("Collected spectra - now starting to analyze the grouped scans.");
  }

  /**
   * Gathers spectra from feature lists.
   *
   * @return A list of grouped signal scans.
   */
  private List<GroupedSignalScans> gatherSpectra() {
    return featureLists.stream()
        .flatMap(featureList -> featureList.getRows().stream().map(this::createGroupedSignalScans))
        .collect(Collectors.toList());
  }

  /**
   * Creates grouped signal scans for a given row.
   *
   * @param row The feature list row.
   * @return The grouped signal scans.
   */
  private GroupedSignalScans createGroupedSignalScans(FeatureListRow row) {
    List<Scan> ms1Scans = new ArrayList<>();
    List<Scan> ms2Scans = new ArrayList<>();
    List<Scan> allPrecursorsMs2Scans = new ArrayList<>();

    for (ModularFeature feature : row.getFeatures()) {
      try {
        Map<String, List<Scan>> categorizedScans = categorizeScans(feature);

        ms1Scans.addAll(categorizedScans.getOrDefault("ms1Scans", new ArrayList<>()));
        ms2Scans.addAll(categorizedScans.getOrDefault("ms2Scans", new ArrayList<>()));
        allPrecursorsMs2Scans.addAll(
            categorizedScans.getOrDefault("ms2ScansAllPrecursors", new ArrayList<>()));

        SignalsAnalysisResult analysisResult = analyzeSignals(ms1Scans, ms2Scans,
            allPrecursorsMs2Scans, tolerance, row.getAverageMZ());
        row.set(InSourceFragmentsAnalysisType.class, analysisResult.results);

      } catch (Exception ex) {
        logger.log(Level.WARNING, "Error processing feature: " + ex.getMessage(), ex);
      }
    }
    return new GroupedSignalScans(row, ms1Scans, ms2Scans, allPrecursorsMs2Scans);
  }

  /**
   * Categorizes MS1 and MS2 scans for a given feature.
   *
   * @param feature The feature to process.
   * @return A map of categorized scans.
   */
  private Map<String, List<Scan>> categorizeScans(Feature feature) {
    List<Scan> ms1Scans = new ArrayList<>();
    List<Scan> ms2Scans = new ArrayList<>();
    List<Scan> allPrecursorsMs2Scans = new ArrayList<>();

    Scan representativeMs1 = feature.getRepresentativeScan();
    if (representativeMs1 != null) {
      ms1Scans.add(representativeMs1);

      List<Scan> fragmentScans = feature.getAllMS2FragmentScans();
      ms2Scans.addAll(fragmentScans);

      // TODO: This could be a parameter to get it over the whole file instead
      Range<Float> rawRtRange = feature.getRawDataPointsRTRange();
      RawDataFile raw = feature.getRawDataFile();
      DataPoint[] dataPoints = ScanUtils.extractDataPoints(representativeMs1, useMassList);

      for (DataPoint dp : dataPoints) {
        double ms1SignalMz = dp.getMZ();
        Scan[] broadFragmentScans = findAllMS2FragmentScans(raw, rawRtRange,
            tolerance.getToleranceRange(ms1SignalMz));
        allPrecursorsMs2Scans.addAll(Arrays.asList(broadFragmentScans));
      }
    }

    Map<String, List<Scan>> result = new HashMap<>();
    result.put("ms1Scans", ms1Scans);
    result.put("ms2Scans", ms2Scans);
    result.put("ms2ScansAllPrecursors", allPrecursorsMs2Scans);

    return result;
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
   * Analyzes unique signals between MS1 and MS2 scans.
   *
   * @param ms1Scans              The MS1 scans.
   * @param ms2Scans              The MS2 scans.
   * @param allPrecursorsMs2Scans The MS2 scans from all precursors.
   * @param tolerance             The MZ tolerance.
   * @param precursorMz           The precursor MZ value.
   * @return The analysis result.
   */
  private SignalsAnalysisResult analyzeSignals(List<Scan> ms1Scans, List<Scan> ms2Scans,
      List<Scan> allPrecursorsMs2Scans, MZTolerance tolerance, Double precursorMz) {

    // Step 1: Analyze MS1 signals
    // require signal to be in 90% of MS1 scans
    int minMs1Scans = (int) Math.ceil(ms1Scans.size() * 0.9);
    var ms1SignalRangeMap = filterMap(collectUniqueSignals(ms1Scans, tolerance), minMs1Scans);
    List<UniqueSignal> ms1Signals = new ArrayList<>(ms1SignalRangeMap.asMapOfRanges().values());

    // Step 2a: Analyze MS2 signals for all precursors
    var ms2SignalRangeMapAllPrecursors = collectUniqueSignals(allPrecursorsMs2Scans, tolerance);
    List<UniqueSignal> ms2SignalsAllPrecursors = mapToList(ms2SignalRangeMapAllPrecursors);
    List<UniqueSignal> ms1SignalMatchesMs2AllPrecursors = findMatches(ms1Signals,
        ms2SignalRangeMapAllPrecursors);
    List<UniqueSignal> ms2SignalMatchesMs1AllPrecursors = findMatches(ms2SignalsAllPrecursors,
        ms1SignalRangeMap);

    // Step 2b: Analyze MS2 signals for classically associated precursors
    var ms2SignalRangeMap = collectUniqueSignals(ms2Scans, tolerance);
    List<UniqueSignal> ms2Signals = mapToList(ms2SignalRangeMap);
    List<UniqueSignal> ms1SignalMatchesMs2 = findMatches(ms1Signals, ms2SignalRangeMap);
    List<UniqueSignal> ms2SignalMatchesMs1 = findMatches(ms2Signals, ms1SignalRangeMap);

    // Step 3: Calculate percentages and totals
    Set<Double> precursorMzSet = ms1SignalMatchesMs2.stream().map(UniqueSignal::mz)
        .collect(Collectors.toSet());
    List<UniqueSignal> ms1FragmentedSignalMatchesMs2AllPrecursors = ms1Signals.stream()
        .filter(signal -> precursorMzSet.contains(signal.mz()))
        .filter(signal -> ms2SignalRangeMapAllPrecursors.get(signal.mz()) != null).toList();

    List<UniqueSignal> ms1FragmentedSignalMatchesMs2 = ms1Signals.stream()
        .filter(signal -> precursorMzSet.contains(signal.mz()))
        .filter(signal -> ms2SignalRangeMap.get(signal.mz()) != null).toList();

    // Step 4: Calculate percentages and totals
    double ms1IntensityTotal = calcSumIntensity(ms1Signals);
    double ms1IntensityFragmentedPercent =
        calcSumIntensity(findUniquePrecursors(ms1SignalRangeMap, allPrecursorsMs2Scans))
            / ms1IntensityTotal;
    double ms1IntensityCommon = calcSumIntensity(ms1SignalMatchesMs2);
    double ms1IntensityCommonAllPrecursors = calcSumIntensity(ms1SignalMatchesMs2AllPrecursors);
    double ms1IntensityCommonPercent = ms1IntensityCommon / ms1IntensityTotal;
    double ms1IntensityCommonPercentAllPrecursors =
        ms1IntensityCommonAllPrecursors / ms1IntensityTotal;

    int ms1SignalsTotal = ms1Signals.size();
    int ms1SignalsFragmented = findUniquePrecursors(ms1SignalRangeMap,
        allPrecursorsMs2Scans).size();
    double ms1SignalsFragmentedPercent = (double) ms1SignalsFragmented / ms1SignalsTotal;

    int ms2SignalsAllPrecursorsTotal = ms2SignalsAllPrecursors.size();
    int signalsCommonAllPrecursors = ms1SignalMatchesMs2AllPrecursors.size();
    double ms1SignalsCommonPercentAllPrecursors =
        (double) signalsCommonAllPrecursors / ms1SignalsTotal;
    double ms2SignalsCommonPercentAllPrecursors =
        (double) ms2SignalMatchesMs1AllPrecursors.size() / ms2SignalsAllPrecursorsTotal;
    double ms2IntensityCommonAllPrecursors = calcSumIntensity(ms2SignalMatchesMs1AllPrecursors);
    double ms2IntensityTotalAllPrecursors = calcSumIntensity(ms2SignalsAllPrecursors);
    double ms2IntensityCommonPercentAllPrecursors =
        ms2IntensityCommonAllPrecursors / ms2IntensityTotalAllPrecursors;

    int ms2SignalsTotal = ms2Signals.size();
    int signalsCommon = ms1SignalMatchesMs2.size();
    double ms1SignalsCommonPercent = (double) signalsCommon / ms1SignalsTotal;
    double ms2SignalsCommonPercent = (double) ms2SignalMatchesMs1.size() / ms2SignalsTotal;
    double ms2IntensityCommon = calcSumIntensity(ms2SignalMatchesMs1);
    double ms2IntensityTotal = calcSumIntensity(ms2Signals);
    double ms2IntensityCommonPercent = ms2IntensityCommon / ms2IntensityTotal;

    // Step 5: Determine ISF likelihood
    double ms1SignalsFragmentedLikelyISFPercent =
        (double) ms1FragmentedSignalMatchesMs2.size() / ms1SignalsFragmented;
    List<Double> likelyISFPrecursorMzs = ms1FragmentedSignalMatchesMs2.stream()
        .map(UniqueSignal::mz).toList();
    boolean isLikelyISF = likelyISFPrecursorMzs.stream()
        .anyMatch(mz -> tolerance.checkWithinTolerance(precursorMz, mz));

    // Step 6: Create results object
    InSourceFragmentAnalysisResults results = new InSourceFragmentAnalysisResults(isLikelyISF,
        ms1SignalsFragmentedLikelyISFPercent, signalsCommon, signalsCommonAllPrecursors,
        ms1SignalsTotal, ms2SignalsAllPrecursorsTotal, ms1SignalsFragmented,
        ms1SignalsFragmentedPercent, ms1IntensityFragmentedPercent, ms1SignalsCommonPercent,
        ms1IntensityCommonPercentAllPrecursors, ms1IntensityCommonPercent, ms2SignalsTotal,
        ms2SignalsCommonPercent, ms2IntensityCommonPercentAllPrecursors, ms2IntensityCommonPercent);

    return new SignalsAnalysisResult(results);
  }

  /**
   * Finds unique signals in signals that match with the signals in the provided signal map.
   */
  private List<UniqueSignal> findMatches(List<UniqueSignal> signals,
      RangeMap<Double, UniqueSignal> signalRangeMap) {
    return signals.stream().filter(signal -> signalRangeMap.get(signal.mz()) != null)
        .collect(Collectors.toList());
  }


  private List<UniqueSignal> mapToList(final RangeMap<Double, UniqueSignal> map) {
    return new ArrayList<>(map.asMapOfRanges().values());
  }

  /**
   * Filters the signal map based on a minimum number of scans.
   *
   * @param uniqueMs1Map The signal map.
   * @param minSamples   The minimum number of scans.
   * @return The filtered signal map.
   */
  private @NotNull RangeMap<Double, UniqueSignal> filterMap(
      final RangeMap<Double, UniqueSignal> uniqueMs1Map, final int minSamples) {
    RangeMap<Double, UniqueSignal> unique = TreeRangeMap.create();

    uniqueMs1Map.asMapOfRanges().values().stream()
        .filter(signal -> signal.numberOfScans() >= minSamples)
        .forEach(signal -> unique.put(tolerance.getToleranceRange(signal.mz()), signal));

    return unique;
  }

  /**
   * Collects unique signals from a list of scans.
   *
   * @param scans     The list of scans.
   * @param tolerance The MZ tolerance.
   * @return The range map of unique signals.
   */
  private RangeMap<Double, UniqueSignal> collectUniqueSignals(List<Scan> scans,
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
   * Finds unique precursor signals within a list of MS2 scans.
   *
   * @param ms1SignalRangeMap The MS1 signal map.
   * @param ms2Scans          The MS2 scans.
   * @return The list of unique MS1 signals with MS2 fragments.
   */
  private List<UniqueSignal> findUniquePrecursors(RangeMap<Double, UniqueSignal> ms1SignalRangeMap,
      List<Scan> ms2Scans) {
    return ms2Scans.stream().map(Scan::getPrecursorMz).filter(Objects::nonNull)
        .map(ms1SignalRangeMap::get).filter(Objects::nonNull).distinct().toList();
  }

  private record SignalsAnalysisResult(InSourceFragmentAnalysisResults results) {

  }
}
