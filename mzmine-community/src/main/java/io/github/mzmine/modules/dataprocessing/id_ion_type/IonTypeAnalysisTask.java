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

package io.github.mzmine.modules.dataprocessing.id_ion_type;

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
import io.github.mzmine.datamodel.features.types.analysis.IonTypeAnalysisType;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.Element;

/**
 * Analyzes MS1 and MS2 signals from feature lists to compare MS1 signals with MS2 fragment scans.
 */
class IonTypeAnalysisTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(IonTypeAnalysisTask.class.getName());
  private final List<FeatureList> featureLists;
  private final boolean useMassList;
  private final MZTolerance toleranceMs1;
  private final MZTolerance toleranceMs2;

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
  public IonTypeAnalysisTask(MZmineProject project, List<FeatureList> featureLists,
      ParameterSet parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.featureLists = featureLists;
    this.totalItems = featureLists.stream().mapToInt(FeatureList::getNumberOfRows).sum();
    this.useMassList =
        parameters.getValue(IonTypeAnalysisParameters.scanDataType) == ScanDataType.MASS_LIST;
    this.toleranceMs1 = parameters.getValue(IonTypeAnalysisParameters.toleranceMs1);
    this.toleranceMs2 = parameters.getValue(IonTypeAnalysisParameters.toleranceMs2);
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
        .distinct().collect(Collectors.toList());
  }

  /**
   * Creates grouped signal scans for a given row.
   *
   * @param row The feature list row.
   * @return The grouped signal scans.
   */
  private GroupedSignalScans createGroupedSignalScans(FeatureListRow row) {
    List<Scan> ms1Scans = new ArrayList<>();
    List<Scan> allPrecursorsMs2Scans = new ArrayList<>();
    Set<DataPoint> isotopeSet = new HashSet<>();
    Set<DataPoint> adductsAndCoSet = new HashSet<>();
    int isotopeMaxCharge = 2; // Consider making this a constant or parameterized value

    // Precompute isotope mass differences
    DoubleArrayList[] isoMzDiffsForCharge = IsotopesUtils.getIsotopesMzDiffsForCharge(
        Arrays.asList(new Element("H"), new Element("C"), new Element("N"), new Element("O"),
            new Element("S"), new Element("F"), new Element("Cl"), new Element("Br"),
            new Element("I")), isotopeMaxCharge);

    // Precompute max isotope MZ differences
    double[] maxIsoMzDiff = new double[isotopeMaxCharge];
    for (int i = 0; i < isotopeMaxCharge; i++) {
      for (double diff : isoMzDiffsForCharge[i]) {
        maxIsoMzDiff[i] = Math.max(maxIsoMzDiff[i], diff);
      }
      maxIsoMzDiff[i] += 10 * toleranceMs1.getMzToleranceForMass(maxIsoMzDiff[i]);
    }

    for (ModularFeature feature : row.getFeatures()) {
      try {
        Scan representativeMs1 = feature.getRepresentativeScan();
        if (representativeMs1 != null) {
          ms1Scans.add(representativeMs1);

          Range<Float> rawRtRange = feature.getRawDataPointsRTRange();
          RawDataFile raw = feature.getRawDataFile();
          DataPoint[] dataPoints = ScanUtils.extractDataPoints(representativeMs1, useMassList);

          for (DataPoint dp : dataPoints) {
            double ms1SignalMz = dp.getMZ();
            Scan[] broadFragmentScans = findAllMS2FragmentScans(raw, rawRtRange,
                toleranceMs2.getToleranceRange(ms1SignalMz));
            allPrecursorsMs2Scans.addAll(Arrays.asList(broadFragmentScans));
          }
          processIsotopesAndAdductsForFeature(feature, isotopeSet, adductsAndCoSet,
              isoMzDiffsForCharge, maxIsoMzDiff);
        }
      } catch (Exception ex) {
        logger.log(Level.WARNING, "Error gathering scans for feature: " + ex.getMessage(), ex);
      }
    }

    try {
      IsotopeAndAdducts isotopeAndAdducts = new IsotopeAndAdducts(new ArrayList<>(isotopeSet),
          new ArrayList<>(adductsAndCoSet));
      SignalsAnalysisResult analysisResult = analyzeSignals(ms1Scans, allPrecursorsMs2Scans,
          toleranceMs1, toleranceMs2, isotopeAndAdducts.getAdducts(),
          isotopeAndAdducts.getIsotopes());
      row.set(IonTypeAnalysisType.class, analysisResult.results);

    } catch (Exception ex) {
      logger.log(Level.WARNING, "Error processing row: " + ex.getMessage(), ex);
    }
    return new GroupedSignalScans(row, ms1Scans, allPrecursorsMs2Scans);
  }

  private void processIsotopesAndAdductsForFeature(Feature feature, Set<DataPoint> isotopeSet,
      Set<DataPoint> adductsAndCoSet, DoubleArrayList[] isoMzDiffsForCharge,
      double[] maxIsoMzDiff) {
    Scan representativeScan = feature.getRepresentativeScan();
    if (representativeScan != null && representativeScan.getMSLevel() == 1) {
      DataPoint[] dataPoints = ScanUtils.extractDataPoints(representativeScan, useMassList);

      if (dataPoints.length > 0) {
        Arrays.sort(dataPoints, DataPointSorter.DEFAULT_INTENSITY);
        for (int i = 0; i < isoMzDiffsForCharge.length; i++) {
          DoubleArrayList currentChargeDiffs = isoMzDiffsForCharge[i];
          double currentMaxDiff = maxIsoMzDiff[i];

          for (DataPoint dataPoint : dataPoints) {
            List<DataPoint> foundIsotopes = IsotopesUtils.findIsotopesInScan(currentChargeDiffs,
                currentMaxDiff, toleranceMs1, representativeScan, dataPoint);
            // Only add it if we have evidence for it (so more than 1 ion)
            if (foundIsotopes.size() > 1) {
              isotopeSet.addAll(foundIsotopes);
            }

            findAndAddMassDifferences(dataPoints, dataPoint, adductsAndCoSet);
          }
        }
      }
    }
  }

  private void findAndAddMassDifferences(DataPoint[] dataPoints, DataPoint target,
      Set<DataPoint> adductsAndCoSet) {
    double[] knownMassDifferences = { //
        // Most occurring mass diffs taken from 10.1021/acs.analchem.4c00966
        67.9874, // sodium formate
        0.5017,  // double charge C
        21.9819, // H Na
        57.9586, // NaCl
        46.0055, // formic acid
        15.9739, // Na K
        // TODO Add dataPoint specific diffs later (like 2M, etc.)
    };
    double targetMZ = target.getMZ();

    for (double massDiff : knownMassDifferences) {
      boolean hasMatchingPoint = false; // Flag to track if a matching point is found

      for (DataPoint point : dataPoints) {
        if (Math.abs(point.getMZ() - targetMZ - massDiff) <= toleranceMs1.getMzToleranceForMass(
            massDiff)) {
          adductsAndCoSet.add(point);
          hasMatchingPoint = true; // Set flag to true if a match is found
        }
      }
      // Add the target to the set if at least one matching point was found
      if (hasMatchingPoint) {
        adductsAndCoSet.add(target);
      }
    }
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
   * @param allPrecursorsMs2Scans The MS2 scans from all precursors.
   * @param toleranceMs1          The tolerance in MS1.
   * @param toleranceMs2          The tolerance in MS1.
   * @param adductsAndCoList      The adducts and co list
   * @param isotopeList           The isotopes list.
   * @return The analysis result.
   */
  private SignalsAnalysisResult analyzeSignals(List<Scan> ms1Scans,
      List<Scan> allPrecursorsMs2Scans, MZTolerance toleranceMs1, MZTolerance toleranceMs2,
      List<DataPoint> adductsAndCoList, List<DataPoint> isotopeList) {

    // Step 1: Collect signals
    // MS1
    int minMs1Scans = (int) Math.ceil(ms1Scans.size() * 0.9);  // Require signal in 90% of scans
    var ms1SignalRangeMap = filterMap(collectUniqueSignals(ms1Scans, toleranceMs1), minMs1Scans);
    List<UniqueSignal> ms1Signals = new ArrayList<>(ms1SignalRangeMap.asMapOfRanges().values());
    // MS2 (all precursors)
    var ms2SignalRangeMap = collectUniqueSignals(allPrecursorsMs2Scans, toleranceMs2);
    List<UniqueSignal> ms2SignalsAllPrecursors = mapToList(ms2SignalRangeMap);

    // Step 2: Find unique precursors
    List<UniqueSignal> uniquePrecursorsSignals = findUniquePrecursors(ms1SignalRangeMap,
        allPrecursorsMs2Scans);
    List<UniqueSignal> ms2SignalMatchesMs1AllPrecursors = findMatches(ms2SignalsAllPrecursors,
        ms1SignalRangeMap);

    // Step 3: Analyze isotopes, adducts, and co
    var adductsAndCoSignalMap = collectSignalsFromDataPoints(adductsAndCoList, toleranceMs1);
    var isotopesSignalsMap = collectSignalsFromDataPoints(isotopeList, toleranceMs1);
    List<UniqueSignal> ms1SignalsAdductsAndCo = findMatches(ms1Signals, adductsAndCoSignalMap);
    List<UniqueSignal> ms1SignalsIsotopes = findMatches(ms1Signals, isotopesSignalsMap);

    // Step 4: Analyze MS2 signals for all precursors
    List<UniqueSignal> commonSignals = findMatches(ms1Signals, ms2SignalRangeMap);

    // Step 5: Get intersects
    List<UniqueSignal> adductsICommon = findMatches(ms1SignalsAdductsAndCo, ms2SignalRangeMap);
    List<UniqueSignal> adductsIIsotopes = findMatches(ms1SignalsAdductsAndCo, isotopesSignalsMap);
    List<UniqueSignal> isotopesICommon = findMatches(ms1SignalsIsotopes, ms2SignalRangeMap);
    List<UniqueSignal> explainedISet = findMatches(adductsICommon, isotopesSignalsMap);

    // Filter unexplained signals
    List<UniqueSignal> ms1SignalsUnexplained = filterSignals(ms1Signals,
        union(ms1SignalsAdductsAndCo, union(ms1SignalsIsotopes, commonSignals)));

    // Step 6: Get summed intensities
    double commonMs1IntensityAllPrecursors = calcSumIntensity(commonSignals);
    double ms1Intensity = calcSumIntensity(ms1Signals);
    double ms2IntensityAllPrecursors = calcSumIntensity(ms2SignalsAllPrecursors);
    double ms1IntensityFragmented = calcSumIntensity(uniquePrecursorsSignals);
    double ms1IntensityUnexplained = calcSumIntensity(ms1SignalsUnexplained);
    double commonMs2IntensityAllPrecursors = calcSumIntensity(ms2SignalMatchesMs1AllPrecursors);

    // Step 7: Get signals sizes
    int ms1SignalsCount = ms1Signals.size();
    int ms1SignalsAdductsAndCoCount = ms1SignalsAdductsAndCo.size();
    int ms1SignalsIsotopesCount = ms1SignalsIsotopes.size();
    int ms1SignalsFragmentedCount = uniquePrecursorsSignals.size();
    int ms1SignalsUnexplainedCount = ms1SignalsUnexplained.size();
    int ms2SignalsAllPrecursorsCount = ms2SignalsAllPrecursors.size();
    int commonSignalsCount = commonSignals.size();

    int adductsOnlyCount =
        ms1SignalsAdductsAndCoCount - adductsICommon.size() - adductsIIsotopes.size()
            + explainedISet.size();
    int isotopesOnlyCount =
        ms1SignalsIsotopesCount - adductsIIsotopes.size() - isotopesICommon.size()
            + explainedISet.size();
    int commonOnlyCount =
        commonSignalsCount - adductsICommon.size() - isotopesICommon.size() + explainedISet.size();
    int aICCount = adductsICommon.size() - explainedISet.size();
    int aIICount = adductsIIsotopes.size() - explainedISet.size();
    int iICCount = isotopesICommon.size() - explainedISet.size();
    int explainedICount = explainedISet.size();

    // Step 8: Calculate intensities percentages
    double ms1FragmentedIntensityPercent = ms1IntensityFragmented / ms1Intensity;
    double ms1UnexplainedIntensityPercent = ms1IntensityUnexplained / ms1Intensity;
    double ms1CommonIntensityPercent = commonMs1IntensityAllPrecursors / ms1Intensity;
    double ms2CommonIntensityPercent = commonMs2IntensityAllPrecursors / ms2IntensityAllPrecursors;

    // Step 9: Create results object
    IonTypeAnalysisResults results = new IonTypeAnalysisResults(commonSignalsCount, ms1SignalsCount,
        ms2SignalsAllPrecursorsCount, ms1SignalsAdductsAndCoCount, ms1SignalsIsotopesCount,
        ms1SignalsFragmentedCount, ms1SignalsUnexplainedCount, ms1FragmentedIntensityPercent,
        ms1CommonIntensityPercent, ms2CommonIntensityPercent, ms1UnexplainedIntensityPercent,
        adductsOnlyCount, isotopesOnlyCount, commonOnlyCount, aICCount, aIICount, iICCount,
        explainedICount);

    return new SignalsAnalysisResult(results);
  }

  /**
   * Filters out some signals.
   */
  private List<UniqueSignal> filterSignals(List<UniqueSignal> signals,
      List<UniqueSignal> signalsToRemove) {
    return signals.stream().filter(signal -> !signalsToRemove.contains(signal)).distinct()
        .collect(Collectors.toList());
  }

  /**
   * Returns the union of two lists, retaining all unique elements present in either list.
   */
  private List<UniqueSignal> union(List<UniqueSignal> signals1, List<UniqueSignal> signals2) {
    return Stream.concat(signals1.stream(), signals2.stream()).distinct()
        .collect(Collectors.toList());
  }

  /**
   * Finds unique signals in signals that match with the signals in the provided signal map.
   */
  private List<UniqueSignal> findMatches(List<UniqueSignal> signals,
      RangeMap<Double, UniqueSignal> signalRangeMap) {
    return signals.stream().filter(signal -> signalRangeMap.get(signal.mz()) != null).distinct()
        .collect(Collectors.toList());
  }

  /**
   * Converts the RangeMap to a Set of UniqueSignal
   */
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
        .forEach(signal -> unique.put(toleranceMs1.getToleranceRange(signal.mz()), signal));

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
      Arrays.sort(dataPoints, DataPointSorter.DEFAULT_INTENSITY);
      for (DataPoint dp : dataPoints) {
        Range<Double> mzRange = tolerance.getToleranceRange(dp.getMZ());
        UniqueSignal signal = unique.get(dp.getMZ());

        if (signal == null) {
          unique.put(mzRange, new UniqueSignal(dp, scan));
        } else {
          signal.add(dp, scan);
        }
      }
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

  private record SignalsAnalysisResult(IonTypeAnalysisResults results) {

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
