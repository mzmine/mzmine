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
import io.github.mzmine.util.IsotopesUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.ScanUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.Element;

/**
 * Analyzes MS1 and MS2 signals from feature lists to compare MS1 signals with MS2 fragment scans.
 */
class CommonMs1Ms2FragmentsAnalysisTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(
      CommonMs1Ms2FragmentsAnalysisTask.class.getName());
  private final List<FeatureList> featureLists;
  private final boolean useMassList;
  private final MZTolerance tolerance;
  private final Boolean removeAdductsAndCo;
  private final Boolean removeIsotopes;

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
    this.removeAdductsAndCo = parameters.getValue(
        CommonMs1Ms2FragmentsAnalysisParameters.removeAdductsAndCo);
    this.removeIsotopes = parameters.getValue(
        CommonMs1Ms2FragmentsAnalysisParameters.removeIsotopes);
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

      } catch (Exception ex) {
        logger.log(Level.WARNING, "Error categorizing scans for feature: " + ex.getMessage(), ex);
      }
    }

    try {
      IsotopeAndAdducts isotopeAndAdducts = processIsotopesAndAdducts(row);

      SignalsAnalysisResult analysisResult = analyzeSignals(ms1Scans, ms2Scans,
          allPrecursorsMs2Scans, tolerance, row.getAverageMZ(), isotopeAndAdducts.getAdducts(),
          isotopeAndAdducts.getIsotopes(), removeAdductsAndCo, removeIsotopes);
      row.set(InSourceFragmentsAnalysisType.class, analysisResult.results);

    } catch (Exception ex) {
      logger.log(Level.WARNING, "Error processing row: " + ex.getMessage(), ex);
    }
    return new GroupedSignalScans(row, ms1Scans, ms2Scans, allPrecursorsMs2Scans);
  }

  private IsotopeAndAdducts processIsotopesAndAdducts(FeatureListRow row) {
    Set<DataPoint> isotopeSet = new HashSet<>();
    Set<DataPoint> adductsAndCoSet = new HashSet<>();
    int isotopeMaxCharge = 2;

    DoubleArrayList[] isoMzDiffsForCharge = IsotopesUtils.getIsotopesMzDiffsForCharge(
        Arrays.asList(new Element("H"), new Element("C"), new Element("N"), new Element("O"),
            new Element("S"), new Element("F"), new Element("Cl"), new Element("Br")),
        isotopeMaxCharge);

    double[] maxIsoMzDiff = new double[isotopeMaxCharge];
    for (int i = 0; i < isotopeMaxCharge; i++) {
      for (double diff : isoMzDiffsForCharge[i]) {
        maxIsoMzDiff[i] = Math.max(maxIsoMzDiff[i], diff);
      }
      maxIsoMzDiff[i] += 10 * tolerance.getMzToleranceForMass(maxIsoMzDiff[i]);
    }

    for (Feature feature : row.getFeatures()) {
      Scan representativeScan = feature.getRepresentativeScan();
      if (representativeScan != null && representativeScan.getMSLevel() == 1) {
        List<DataPoint> dataPoints = Arrays.asList(
            ScanUtils.extractDataPoints(representativeScan, useMassList));

        if (!dataPoints.isEmpty()) {
          for (int i = 0; i < isotopeMaxCharge; i++) {
            final DoubleArrayList currentChargeDiffs = isoMzDiffsForCharge[i];
            final double currentMaxDiff = maxIsoMzDiff[i];

            for (DataPoint dataPoint : dataPoints) {
              List<DataPoint> foundIsotopes = IsotopesUtils.findIsotopesInScan(currentChargeDiffs,
                  currentMaxDiff, tolerance, representativeScan, dataPoint);

              double threshold = dataPoint.getMZ() + 0.2;
              foundIsotopes.removeIf(found -> found.getMZ() <= threshold);

              isotopeSet.addAll(foundIsotopes);

              // Most occurring mass diffs taken from 10.1021/acs.analchem.4c00966
              double[] knownMassDifferences = {67.9874, // sodium formate
                  0.5017,  //double charge C
                  21.9819, // H Na
                  57.9586, // NaCl
                  46.0055, // formic acid
                  15.9739, // Na K
              };

              // TODO Add dataPoint specific diffs later

              for (double massDiff : knownMassDifferences) {
                List<DataPoint> foundAdductsAndCo = findMassDifferences(dataPoints, dataPoint,
                    massDiff);
                adductsAndCoSet.addAll(foundAdductsAndCo);
              }
            }
          }
        }
      }
    }
    return new IsotopeAndAdducts(new ArrayList<>(isotopeSet), new ArrayList<>(adductsAndCoSet));
  }

  private List<DataPoint> findMassDifferences(List<DataPoint> dataPoints, DataPoint target,
      double massDiff) {
    List<DataPoint> matchingPoints = new ArrayList<>();
    double targetMZ = target.getMZ();

    for (DataPoint point : dataPoints) {
      double diff = point.getMZ() - targetMZ;
      if (Math.abs(diff - massDiff) <= tolerance.getMzToleranceForMass(massDiff)) {
        matchingPoints.add(point);
      }
    }

    return matchingPoints;
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
   * @param adductsAndCoList      The adducts and co list
   * @param isotopeList           The isotopes list.
   * @param removeAdductsAndCo    Boolean to remove adducts and co from count.
   * @param removeIsotopes        Boolean to remove isotopes from count.
   * @return The analysis result.
   */
  private SignalsAnalysisResult analyzeSignals(List<Scan> ms1Scans, List<Scan> ms2Scans,
      List<Scan> allPrecursorsMs2Scans, MZTolerance tolerance, Double precursorMz,
      List<DataPoint> adductsAndCoList, List<DataPoint> isotopeList, boolean removeAdductsAndCo,
      boolean removeIsotopes) {

    // Step 1: Analyze MS1 signals
    int minMs1Scans = (int) Math.ceil(ms1Scans.size() * 0.9);  // Require signal in 90% of scans
    var ms1SignalRangeMap = filterMap(collectUniqueSignals(ms1Scans, tolerance), minMs1Scans);
    List<UniqueSignal> ms1Signals = new ArrayList<>(ms1SignalRangeMap.asMapOfRanges().values());
    var adductsAndCoSignalMap = collectSignalsFromDataPoints(adductsAndCoList, tolerance);
    var isotopesSignalsMap = collectSignalsFromDataPoints(isotopeList, tolerance);
    List<UniqueSignal> ms1SignalsAdductsAndCo = findMatches(ms1Signals, adductsAndCoSignalMap);
    List<UniqueSignal> ms1SignalsIsotopes = findMatches(ms1Signals, isotopesSignalsMap);
    if (removeAdductsAndCo) {
      Set<UniqueSignal> adductsAndCoSet = new HashSet<>(ms1SignalsAdductsAndCo);
      List<UniqueSignal> filteredMs1Signals = ms1Signals.stream()
          .filter(signal -> !adductsAndCoSet.contains(signal)).collect(Collectors.toList());
      ms1Signals = filteredMs1Signals;
    }
    if (removeIsotopes) {
      Set<UniqueSignal> isotopesSet = new HashSet<>(ms1SignalsIsotopes);
      List<UniqueSignal> filteredMs1Signals = ms1Signals.stream()
          .filter(signal -> !isotopesSet.contains(signal)).collect(Collectors.toList());
      ms1Signals = filteredMs1Signals;
    }

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
    int ms1SignalsAdductsAndCoTotal = ms1SignalsAdductsAndCo.size();
    int ms1SignalsIsotopesTotal = ms1SignalsIsotopes.size();
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
        ms1SignalsTotal, ms2SignalsAllPrecursorsTotal, ms1SignalsAdductsAndCoTotal,
        ms1SignalsIsotopesTotal, ms1SignalsFragmented, ms1SignalsFragmentedPercent,
        ms1IntensityFragmentedPercent, ms1SignalsCommonPercent,
        ms1IntensityCommonPercentAllPrecursors, ms1IntensityCommonPercent, ms2SignalsTotal,
        ms2SignalsCommonPercent, ms2IntensityCommonPercentAllPrecursors, ms2IntensityCommonPercent);

    return new SignalsAnalysisResult(results);
  }

  private boolean isWithinTolerance(DataPoint dp1, DataPoint dp2, MZTolerance tolerance) {
    double mzTolerance = tolerance.getMzTolerance();
    return Math.abs(dp1.getMZ() - dp2.getMZ()) <= mzTolerance;
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
      collectSignalsFromScan(scan, unique, tolerance);
    }

    return unique;
  }

  /**
   * Collects signals from a DataPoint list and updates a newly created RangeMap.
   *
   * @param dataPoints The list of DataPoints.
   * @param tolerance  The MZ tolerance.
   * @return A RangeMap populated with unique signals.
   */
  private RangeMap<Double, UniqueSignal> collectSignalsFromDataPoints(List<DataPoint> dataPoints,
      MZTolerance tolerance) {
    RangeMap<Double, UniqueSignal> unique = TreeRangeMap.create();

    dataPoints.stream().sorted(DataPointSorter.DEFAULT_INTENSITY).forEach(dp -> {
      var uniqueSignal = unique.get(dp.getMZ());
      if (uniqueSignal == null) {
        uniqueSignal = new UniqueSignal(dp, null);
        unique.put(tolerance.getToleranceRange(dp.getMZ()), uniqueSignal);
      } else {
        uniqueSignal.add(dp, null);
      }
    });

    return unique;
  }

  /**
   * Collects signals from a single scan and adds them to the unique signal map.
   *
   * @param scan      The scan to extract signals from.
   * @param unique    The RangeMap of unique signals to populate.
   * @param tolerance The MZ tolerance.
   */
  private void collectSignalsFromScan(Scan scan, RangeMap<Double, UniqueSignal> unique,
      MZTolerance tolerance) {
    DataPoint[] dataPoints = ScanUtils.extractDataPoints(scan, useMassList);
    if (scan.getMSLevel() > 1) {
      dataPoints = removePrecursorMz(dataPoints, scan.getPrecursorMz(), 1);
    }
    RangeMap<Double, UniqueSignal> scanUniqueSignals = collectSignalsFromDataPoints(
        Arrays.asList(dataPoints), tolerance);
    scanUniqueSignals.asMapOfRanges().forEach(unique::put);
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

  public class IsotopeAndAdducts {

    private final List<DataPoint> isotopes;
    private final List<DataPoint> adducts;

    public IsotopeAndAdducts(List<DataPoint> isotopes, List<DataPoint> adducts) {
      this.isotopes = isotopes;
      this.adducts = adducts;
    }

    public List<DataPoint> getIsotopes() {
      return isotopes;
    }

    public List<DataPoint> getAdducts() {
      return adducts;
    }
  }
}
