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

package io.github.mzmine.modules.dataprocessing.id_ion_type;

import static io.github.mzmine.util.scans.ScanUtils.findAllMSnFragmentScans;

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
  private final MZTolerance toleranceMsn;

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
    this.toleranceMsn = parameters.getValue(IonTypeAnalysisParameters.toleranceMsn);
  }


  /**
   * Formats values from a list of UniqueSignals into a comma-separated string, rounding each value
   * to the specified number of decimal places.
   *
   * @param signals         The list of UniqueSignal objects.
   * @param decimalPlaces   The number of decimal places to format to.
   * @param formatIntensity A boolean flag to indicate whether to format intensity values (true) or
   *                        m/z values (false).
   * @return A comma-separated string of formatted intensity values.
   */
  public static String formatValuesToString(List<UniqueSignal> signals, int decimalPlaces,
      boolean formatIntensity) {
    String formatString = "%." + decimalPlaces + "f";

    return signals.stream().flatMap(uniqueSignal -> {
      // Choose to stream intensity values or m/z values based on the formatIntensity flag
      return formatIntensity ? uniqueSignal.getIntensityValues().stream()
          : uniqueSignal.getMzValues().stream();
    }).map(value -> String.format(formatString, value)).collect(Collectors.joining(", "));
  }

  /**
   * Formats values from a RangeMap of UniqueSignals into a comma-separated string, rounding each
   * value to the specified number of decimal places.
   *
   * @param signals         The RangeMap of UniqueSignal objects.
   * @param decimalPlaces   The number of decimal places to format to.
   * @param formatIntensity A boolean flag to indicate whether to format intensity values (true) or
   *                        m/z values (false).
   * @return A comma-separated string of formatted intensity or m/z values.
   */
  public static String formatValuesToString(RangeMap<Double, UniqueSignal> signals,
      int decimalPlaces, boolean formatIntensity) {
    String formatString = "%." + decimalPlaces + "f";
    return signals.asMapOfRanges().values().stream().flatMap(uniqueSignal -> {
      // Choose to stream intensity values or m/z values based on the formatIntensity flag
      return formatIntensity ? uniqueSignal.getIntensityValues().stream()
          : uniqueSignal.getMzValues().stream();
    }).map(value -> String.format(formatString, value)).collect(Collectors.joining(", "));
  }


  @Override
  protected void process() {
    int isotopeMaxCharge = 2;  // TODO fix for now

    // Precompute isotope mass differences
    DoubleArrayList[] isoMzDiffsForCharge = IsotopesUtils.getIsotopesMzDiffsForCharge(
        Arrays.asList(new Element("C"), new Element("S"), new Element("Cl"), new Element("Br")),
        isotopeMaxCharge);

    // Precompute max isotope MZ differences
    double[] maxIsoMzDiff = new double[isotopeMaxCharge];
    for (int i = 0; i < isotopeMaxCharge; i++) {
      for (double diff : isoMzDiffsForCharge[i]) {
        maxIsoMzDiff[i] = Math.max(maxIsoMzDiff[i], diff);
      }
      maxIsoMzDiff[i] += 10 * toleranceMs1.getMzToleranceForMass(maxIsoMzDiff[i]);
    }

    List<GroupedSignalScans> groupedScans = gatherSpectra(isoMzDiffsForCharge, maxIsoMzDiff);
    logger.info("Collected spectra - now starting to analyze the grouped scans.");
  }

  /**
   * Gathers spectra from feature lists.
   *
   * @return A list of grouped signal scans.
   */
  private List<GroupedSignalScans> gatherSpectra(DoubleArrayList[] isoMzDiffsForCharge,
      double[] maxIsoMzDiff) {
    return featureLists.stream().flatMap(featureList -> featureList.getRows().stream()
            .map(row -> createGroupedSignalScans(row, isoMzDiffsForCharge, maxIsoMzDiff))).distinct()
        .collect(Collectors.toList());
  }

  /**
   * Creates grouped signal scans for a given row.
   *
   * @param row The feature list row.
   * @return The grouped signal scans.
   */
  private GroupedSignalScans createGroupedSignalScans(FeatureListRow row,
      DoubleArrayList[] isoMzDiffsForCharge, double[] maxIsoMzDiff) {
    List<Scan> ms1Scans = new ArrayList<>();
    List<Scan> allPrecursorsMsnScans = new ArrayList<>();
    Set<DataPoint> isotopeSet = new HashSet<>();
    Set<DataPoint> adductsAndCoSet = new HashSet<>();

    for (ModularFeature feature : row.getFeatures()) {
      try {
        Scan representativeMs1 = feature.getRepresentativeScan();
        if (representativeMs1 != null) {
          ms1Scans.add(representativeMs1);

          Range<Float> rawRtRange = feature.getRawDataPointsRTRange();
          Range<Integer> msLevelRange = Range.closed(2, 5); // TODO change this default
          RawDataFile raw = feature.getRawDataFile();
          DataPoint[] dataPoints = ScanUtils.extractDataPoints(representativeMs1, useMassList);

          for (DataPoint dp : dataPoints) {
            double ms1SignalMz = dp.getMZ();
            Scan[] broadFragmentScans = findAllMSnFragmentScans(raw, msLevelRange, rawRtRange,
                toleranceMsn.getToleranceRange(ms1SignalMz));
            allPrecursorsMsnScans.addAll(Arrays.asList(broadFragmentScans));
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
      SignalsAnalysisResult analysisResult = analyzeSignals(row.getMZRange(), ms1Scans,
          allPrecursorsMsnScans, toleranceMs1, toleranceMsn, isotopeAndAdducts.adducts(),
          isotopeAndAdducts.isotopes());
      row.set(IonTypeAnalysisType.class, analysisResult.results);
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Error processing row: " + ex.getMessage(), ex);
    }
    return new GroupedSignalScans(row, ms1Scans, allPrecursorsMsnScans);
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
            if (isValidIsotopicPattern(foundIsotopes)) {
              isotopeSet.addAll(foundIsotopes);
            }
            findAndAddMassDifferences(dataPoints, dataPoint, adductsAndCoSet);
          }
        }
      }
    }
  }

  /**
   * Checks if the isotopic pattern is valid by ensuring that the intensity of each isotope is
   * significantly lower than the previous one, with customizable ratios for each step.
   *
   * @param isotopes The list of isotopes to check.
   * @return true if the pattern is valid, false otherwise.
   */
  private boolean isValidIsotopicPattern(List<DataPoint> isotopes) {
    // At least three isotopes are needed to form a pattern
    if (isotopes.size() < 3) {
      return false;
    }

    // Iterate through the isotopes to check m/z differences and intensity
    for (int i = 0; i < isotopes.size() - 1; i++) {
      double mzDiff = Math.abs(isotopes.get(i + 1).getMZ() - isotopes.get(i).getMZ());
      double intensityFirst = isotopes.get(i).getIntensity();
      double intensitySecond = isotopes.get(i + 1).getIntensity();

      // Do not allow missing isotopes (m/z difference should not exceed 1.03)
      if (mzDiff > 1.04) {
        return false;
      }

      // Do not allow M + 1 to be higher than M (logic valid up to ~~C75)
      if (mzDiff < 0.98) {
        if (i + 2 < isotopes.size()) {
          double intensityThird = isotopes.get(i + 2).getIntensity();
          if (intensityThird >= intensityFirst) {
            return false;
          }
        }
        continue;
      }
      if (intensitySecond >= intensityFirst) {
        return false;
      }
    }
    // TODO: Implement better logic
    // If all checks passed, return true
    return true;
  }

  private void findAndAddMassDifferences(DataPoint[] dataPoints, DataPoint target,
      Set<DataPoint> adductsAndCoSet) {

    // COMMENT: For now the names are not used, but in case for the future
    // Most occurring mass diffs taken from 10.1021/acs.analchem.4c00966
    Object[][] knownMassDifferences = { //
        {67.9874, "sodium formate"}, //
        {0.5017, "12C 13C double charge"}, //
        {21.9819, "H Na"}, //
        {135.9748, "2 sodium formate"}, //
        {0.3345, "12C 13C triple charge"}, //
        {57.9586, "NaCl"}, //
        {46.0055, "formic acid"}, //
        {15.9739, "Na K"} //
    };

    double proton = 1.007276;
    double targetMZ = target.getMZ();

    // Calculate the M specific differences
    double single_proton = targetMZ + proton;
    double diff_dimer_single = 2 * targetMZ + proton - single_proton;
    double diff_single_double = single_proton - (targetMZ + 2 * proton) / 2;

    // Adding M specific differences with names
    ArrayList<Object[]> updatedMassDifferences = new ArrayList<>(
        Arrays.asList(knownMassDifferences));
    updatedMassDifferences.add(new Object[]{diff_dimer_single, "dimer - single"});
    updatedMassDifferences.add(new Object[]{diff_single_double, "single - double"});

    for (Object[] massDiffPair : updatedMassDifferences) {
      double massDiff = (double) massDiffPair[0];
      // String diffName = (String) massDiffPair[1];
      boolean hasMatchingPoint = false;
      for (DataPoint point : dataPoints) {
        double diffToPoint = Math.abs(point.getMZ() - targetMZ - massDiff);
        if (diffToPoint <= toleranceMs1.getMzToleranceForMass(massDiff)) {
          adductsAndCoSet.add(point);
          hasMatchingPoint = true;
        }
      }
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
   * @param mzRange               The mzRange of the row.
   * @param ms1Scans              The MS1 scans.
   * @param allPrecursorsMsnScans The MSn scans from all precursors.
   * @param toleranceMs1          The tolerance in MS1.
   * @param toleranceMsn          The tolerance in MS1.
   * @param adductsAndCoList      The adducts and co list
   * @param isotopeList           The isotopes list.
   * @return The analysis result.
   */
  private SignalsAnalysisResult analyzeSignals(Range<Double> mzRange, List<Scan> ms1Scans,
      List<Scan> allPrecursorsMsnScans, MZTolerance toleranceMs1, MZTolerance toleranceMsn,
      List<DataPoint> adductsAndCoList, List<DataPoint> isotopeList) {

    // Step 1: Collect signals
    // MS1
    int minMs1Scans = (int) Math.ceil(ms1Scans.size() * 0.9);  // Require signal in 90% of scans
    RangeMap<Double, UniqueSignal> ms1SignalRangeMap = filterMap(
        collectUniqueSignals(ms1Scans, toleranceMs1), minMs1Scans);
    List<UniqueSignal> ms1Signals = mapToListNonOverlapping(ms1SignalRangeMap);
    // MSn (all precursors)
    RangeMap<Double, UniqueSignal> ms2SignalRangeMap = collectUniqueSignals(allPrecursorsMsnScans,
        toleranceMsn);
    List<UniqueSignal> ms2SignalsAllPrecursors = mapToListNonOverlapping(ms2SignalRangeMap);
    //MSn (row precursor)
    var rowMsnScans = filterScansByMzRange(allPrecursorsMsnScans, mzRange);
    var rowMs2SignalRangeMap = collectUniqueSignals(rowMsnScans, toleranceMsn);
    List<UniqueSignal> ms2SignalsRow = mapToListNonOverlapping(rowMs2SignalRangeMap);

    // Step 2: Find unique precursors
    RangeMap<Double, UniqueSignal> uniquePrecursorsSignals = findUniquePrecursors(ms1SignalRangeMap,
        allPrecursorsMsnScans);
    RangeMap<Double, UniqueSignal> ms2SignalMatchesMs1AllPrecursors = findUniqueMatches(
        ms2SignalsAllPrecursors, ms1SignalRangeMap);
    RangeMap<Double, UniqueSignal> ms2SignalMatchesMs1Row = findUniqueMatches(ms2SignalsRow,
        ms1SignalRangeMap);

    // Step 3: Analyze isotopes, adducts, and co
    RangeMap<Double, UniqueSignal> adductsAndCoSignalMap = collectUniqueSignalsFromDataPoints(
        adductsAndCoList, toleranceMs1);
    RangeMap<Double, UniqueSignal> isotopesSignalsMap = collectUniqueSignalsFromDataPoints(
        isotopeList, toleranceMs1);
    RangeMap<Double, UniqueSignal> ms1SignalsAdductsAndCo = findUniqueMatches(ms1Signals,
        adductsAndCoSignalMap);
    RangeMap<Double, UniqueSignal> ms1SignalsIsotopes = findUniqueMatches(ms1Signals,
        isotopesSignalsMap);

    // Step 4: Analyze MS2 signals for all precursors
    RangeMap<Double, UniqueSignal> commonSignals = findUniqueMatches(ms1Signals, ms2SignalRangeMap);
    RangeMap<Double, UniqueSignal> commonSignalsRow = findUniqueMatches(ms1Signals,
        rowMs2SignalRangeMap);

    // Step 5: Keep a trace of the common datapoints
    String ms1Mzs = formatValuesToString(ms1SignalRangeMap, 3, false);
    String ms1Intensities = formatValuesToString(ms1SignalRangeMap, 0, true);
    String msnMzsRow = formatValuesToString(ms2SignalsRow, 3, false);
    String msnMzsScan = formatValuesToString(ms2SignalsAllPrecursors, 3, false);
    String msnIntensities = formatValuesToString(ms2SignalsAllPrecursors, 0, true);
    String commonMzsRow = formatValuesToString(commonSignalsRow, 3, false);
    String commonMzsScan = formatValuesToString(commonSignals, 3, false);
    String commonMzsRowMsn = formatValuesToString(ms2SignalMatchesMs1Row, 3, false);
    String commonMzsScanMsn = formatValuesToString(ms2SignalMatchesMs1AllPrecursors, 3, false);
    String fragmentedMzs = formatValuesToString(uniquePrecursorsSignals, 3, false);
    String adductsMzs = formatValuesToString(ms1SignalsAdductsAndCo, 3, false);
    String isotopesMzs = formatValuesToString(ms1SignalsIsotopes, 3, false);

    // Step 6: Create results object
    IonTypeAnalysisResults results = new IonTypeAnalysisResults( //
        ms1Mzs, //
        ms1Intensities, //
        msnMzsRow, //
        msnMzsScan, //
        msnIntensities, //
        commonMzsRow, //
        commonMzsScan, //
        commonMzsRowMsn, //
        commonMzsScanMsn, //
        fragmentedMzs, //
        adductsMzs, //
        isotopesMzs);

    return new SignalsAnalysisResult(results);
  }

  /**
   * Finds unique signals in `signals` that match with the signals in the provided `signalRangeMap`
   * and returns a RangeMap of matching signals.
   *
   * @param signals        The list of UniqueSignal objects to search for matches.
   * @param signalRangeMap The RangeMap of signals to match against.
   * @return A RangeMap of unique signals that have matching entries in the signalRangeMap.
   */
  private RangeMap<Double, UniqueSignal> findUniqueMatches(List<UniqueSignal> signals,
      RangeMap<Double, UniqueSignal> signalRangeMap) {
    RangeMap<Double, UniqueSignal> matchingSignals = TreeRangeMap.create();
    for (UniqueSignal signal : signals) {
      UniqueSignal match = signalRangeMap.get(signal.mz());
      if (match != null) {
        Range<Double> mzRange = toleranceMs1.getToleranceRange(signal.mz());
        if (matchingSignals.get(mzRange.lowerEndpoint()) == null) {
          matchingSignals.put(mzRange, signal);
        }
      }
    }
    return matchingSignals;
  }

  /**
   * Converts the RangeMap to a List of UniqueSignal, ensuring that the ranges are non-overlapping.
   */
  private List<UniqueSignal> mapToListNonOverlapping(final RangeMap<Double, UniqueSignal> map) {
    List<UniqueSignal> uniqueSignals = new ArrayList<>();
    Set<Range<Double>> seenRanges = new HashSet<>();
    for (UniqueSignal signal : map.asMapOfRanges().values()) {
      Range<Double> mzRange = toleranceMs1.getToleranceRange(signal.mz());
      // TODO merge if overlapping later
      if (isNonOverlapping(seenRanges, mzRange)) {
        uniqueSignals.add(signal);
        seenRanges.add(mzRange);
      }
    }
    return uniqueSignals;
  }

  /**
   * Checks if the provided mzRange overlaps with any of the already seen ranges.
   */
  private boolean isNonOverlapping(Set<Range<Double>> seenRanges, Range<Double> mzRange) {
    for (Range<Double> existingRange : seenRanges) {
      if (existingRange.isConnected(mzRange) && !existingRange.intersection(mzRange).isEmpty()) {
        return false;
      }
    }
    return true;
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
        .filter(signal -> signal.numberOfScans() >= minSamples).forEach(signal -> {
          Range<Double> mzRange = toleranceMs1.getToleranceRange(signal.mz());
          if (unique.get(mzRange.lowerEndpoint()) == null) {
            unique.put(mzRange, signal);
          }
        });
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
    Set<Range<Double>> seenRanges = new HashSet<>();
    for (Scan scan : scans) {
      DataPoint[] dataPoints = ScanUtils.extractDataPoints(scan, useMassList);
      // Arrays.sort(dataPoints, DataPointSorter.DEFAULT_INTENSITY);
      List<DataPoint> filteredDataPoints;
      Integer charge = scan.getPrecursorCharge();
      Double precursorMz = scan.getPrecursorMz();
      double toleranceMz = tolerance.getMzTolerance();

      if (precursorMz != null) {
        double minMzThreshold = precursorMz - toleranceMz;
        double maxMzThreshold = precursorMz + toleranceMz;

        if (charge == null || charge > 1) {
          // For charge > 1 (or null), remove only data points within the precursor range
          filteredDataPoints = Arrays.stream(dataPoints).parallel()
              .filter(dp -> dp.getMZ() < minMzThreshold || dp.getMZ() > maxMzThreshold)
              .collect(Collectors.toList());
        } else {
          // For charge <= 1, remove data points above the precursor m/z minus tolerance
          filteredDataPoints = Arrays.stream(dataPoints).parallel()
              .filter(dp -> dp.getMZ() <= minMzThreshold).collect(Collectors.toList());
        }
      } else {
        filteredDataPoints = Arrays.asList(dataPoints);
      }
      for (DataPoint dp : filteredDataPoints) {
        Range<Double> mzRange = tolerance.getToleranceRange(dp.getMZ());
        // TODO merge if overlapping later
        if (isNonOverlapping(seenRanges, mzRange)) {
          UniqueSignal existingSignal = unique.get(mzRange.lowerEndpoint());
          if (existingSignal == null) {
            unique.put(mzRange, new UniqueSignal(dp, scan));
          } else {
            existingSignal.add(dp, scan);
          }
          seenRanges.add(mzRange);
        }
      }
    }
    return unique;
  }

  /**
   * Collects unique signals from a list of data points, ensuring each tolerance range is unique.
   *
   * @param dataPoints The list of data points.
   * @param tolerance  The MZ tolerance to apply for range creation.
   * @return A RangeMap of unique signals with distinct tolerance ranges.
   */
  private RangeMap<Double, UniqueSignal> collectUniqueSignalsFromDataPoints(
      List<DataPoint> dataPoints, MZTolerance tolerance) {
    RangeMap<Double, UniqueSignal> unique = TreeRangeMap.create();
    Set<Range<Double>> seenRanges = new HashSet<>();
    dataPoints.stream().sorted(DataPointSorter.DEFAULT_INTENSITY).forEach(dp -> {
      Range<Double> mzRange = tolerance.getToleranceRange(dp.getMZ());
      // TODO merge if overlapping later
      if (isNonOverlapping(seenRanges, mzRange)) {
        unique.put(mzRange, new UniqueSignal(dp, null));
        seenRanges.add(mzRange);
      }
    });
    return unique;
  }

  /**
   * Filters a list of scans to keep only those whose precursor m/z values match the specified
   * range.
   *
   * @param scans   The list of scans to filter.
   * @param mzRange The desired m/z range for filtering precursor signals.
   * @return A list of scans with precursor m/z values within the specified range.
   */
  private List<Scan> filterScansByMzRange(List<Scan> scans, Range<Double> mzRange) {
    return scans.stream().filter(scan -> {
      Double precursorMz = scan.getPrecursorMz();
      return precursorMz != null && mzRange.contains(precursorMz);
    }).toList();
  }

  /**
   * Finds unique precursor signals within a list of MSn scans and returns them as a RangeMap.
   *
   * @param ms1SignalRangeMap The MS1 signal map.
   * @param msnScans          The MSn scans.
   * @return A RangeMap of unique MS1 signals that are fragmented.
   */
  private RangeMap<Double, UniqueSignal> findUniquePrecursors(
      RangeMap<Double, UniqueSignal> ms1SignalRangeMap, List<Scan> msnScans) {
    RangeMap<Double, UniqueSignal> uniquePrecursors = TreeRangeMap.create();
    Set<Range<Double>> addedRanges = new HashSet<>();
    msnScans.stream().map(Scan::getPrecursorMz).filter(Objects::nonNull).forEach(precursorMz -> {
      UniqueSignal match = ms1SignalRangeMap.get(precursorMz);
      if (match != null) {
        Range<Double> mzRange = toleranceMs1.getToleranceRange(precursorMz);
        // TODO take the one with highest intensity if overlapping later
        if (isNonOverlapping(addedRanges, mzRange)) {
          uniquePrecursors.put(mzRange, match);
          addedRanges.add(mzRange);
        }
      }
    });
    return uniquePrecursors;
  }

  private record SignalsAnalysisResult(IonTypeAnalysisResults results) {

  }

  public record IsotopeAndAdducts(List<DataPoint> isotopes, List<DataPoint> adducts) {

  }
}
