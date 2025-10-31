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
import java.util.Comparator;
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
    double[] maxIsoMzDiff = computeMaxIsoMzDiff(isoMzDiffsForCharge, isotopeMaxCharge);

    List<GroupedSignalScans> groupedScans = gatherSpectra(isoMzDiffsForCharge, maxIsoMzDiff);
    logger.info("Collected spectra - now starting to analyze the grouped scans.");
  }

  private double[] computeMaxIsoMzDiff(DoubleArrayList[] isoMzDiffsForCharge,
      int isotopeMaxCharge) {
    double[] maxIsoMzDiff = new double[isotopeMaxCharge];
    for (int i = 0; i < isotopeMaxCharge; i++) {
      for (double diff : isoMzDiffsForCharge[i]) {
        maxIsoMzDiff[i] = Math.max(maxIsoMzDiff[i], diff);
      }
      maxIsoMzDiff[i] += 10 * toleranceMs1.getMzToleranceForMass(maxIsoMzDiff[i]);
    }
    return maxIsoMzDiff;
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
    IsotopeAndAdducts matches = new IsotopeAndAdducts();

    for (ModularFeature feature : row.getFeatures()) {
      try {
        processFeature(feature, ms1Scans, allPrecursorsMsnScans, matches, isoMzDiffsForCharge,
            maxIsoMzDiff);
      } catch (Exception ex) {
        logger.log(Level.WARNING, "Error gathering scans for feature: " + ex.getMessage(), ex);
      }
    }

    try {
      SignalsAnalysisResult analysisResult = analyzeSignals(row.getMZRange(), ms1Scans,
          allPrecursorsMsnScans, toleranceMs1, toleranceMsn, matches);
      row.set(IonTypeAnalysisType.class, analysisResult.results);
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Error processing row: " + ex.getMessage(), ex);
    }

    return new GroupedSignalScans(row, ms1Scans, allPrecursorsMsnScans);
  }

  private void processFeature(ModularFeature feature, List<Scan> ms1Scans,
      List<Scan> allPrecursorsMsnScans, IsotopeAndAdducts matches,
      DoubleArrayList[] isoMzDiffsForCharge, double[] maxIsoMzDiff) {

    Scan representativeMs1 = feature.getRepresentativeScan();
    if (representativeMs1 != null) {
      ms1Scans.add(representativeMs1);
      Range<Float> rawRtRange = feature.getRawDataPointsRTRange();
      // TODO change this default
      Range<Integer> msLevelRange = Range.closed(2, 5);
      RawDataFile raw = feature.getRawDataFile();
      DataPoint[] dataPoints = ScanUtils.extractDataPoints(representativeMs1, useMassList);

      // Find all MSn fragment scans related to the data points in the representative scan
      List<Scan> broadFragmentScans = Arrays.stream(dataPoints).map(
              dp -> findAllMSnFragmentScans(raw, msLevelRange, rawRtRange,
                  toleranceMsn.getToleranceRange(dp.getMZ()))).flatMap(Arrays::stream).distinct()
          .toList();
      allPrecursorsMsnScans.addAll(broadFragmentScans);
      processIsotopesAndAdductsForFeature(feature, matches, isoMzDiffsForCharge, maxIsoMzDiff);
    }
  }

  private void processIsotopesAndAdductsForFeature(Feature feature, IsotopeAndAdducts result,
      DoubleArrayList[] isoMzDiffsForCharge, double[] maxIsoMzDiff) {

    Scan representativeScan = feature.getRepresentativeScan();
    if (representativeScan != null && representativeScan.getMSLevel() == 1) {
      DataPoint[] dataPoints = ScanUtils.extractDataPoints(representativeScan, useMassList);
      if (dataPoints.length > 0) {
        Arrays.sort(dataPoints, DataPointSorter.DEFAULT_INTENSITY);

        // Process isotopes
        for (int i = 0; i < isoMzDiffsForCharge.length; i++) {
          processIsotopes(dataPoints, isoMzDiffsForCharge[i], maxIsoMzDiff[i], result,
              representativeScan);
        }

        // Process adducts
        for (DataPoint dataPoint : dataPoints) {
          processAdducts(dataPoints, dataPoint, result);
        }

//        result.getMatches().forEach((dataPoint, matchList) -> {
//          matchList.forEach(match -> {
//            if (match instanceof IsotopeMatch isotopeMatch) {
//              System.out.printf("MZ: %.4f, Isotopes: [", isotopeMatch.getDataPoint().getMZ());
//              isotopeMatch.getIsotopes().forEach(iso -> System.out.printf(" %.4f", iso.getMZ()));
//              System.out.println(" ]");
//            }
//          });
//          result.getAdductMatches().getOrDefault(dataPoint, new ArrayList<>())
//              .forEach(adductMatch -> {
//                System.out.printf("MZ: %.4f, Adducts: [", dataPoint.getMZ());
//                adductMatch.getAdducts()
//                    .forEach(adduct -> System.out.printf(" %.4f", adduct.getMZ()));
//                adductMatch.getAdductNames().forEach(name -> System.out.printf(" %s", name));
//                System.out.println(" ]");
//              });
//        });
      }
    }
  }

  private void processIsotopes(DataPoint[] dataPoints, DoubleArrayList chargeDiffs, double maxDiff,
      IsotopeAndAdducts result, Scan representativeScan) {
    Map<Set<DataPoint>, DataPoint> isotopePatterns = new HashMap<>();

    for (DataPoint dataPoint : dataPoints) {
      List<DataPoint> foundIsotopes = IsotopesUtils.findIsotopesInScan(chargeDiffs, maxDiff,
          toleranceMs1, representativeScan, dataPoint);

      if (isValidIsotopicPattern(foundIsotopes)) {
        Set<DataPoint> fullPattern = new HashSet<>(foundIsotopes);

        // Check if we already have a pattern with the same isotopes
        if (isotopePatterns.containsKey(fullPattern)) {
          DataPoint existingIsotope = isotopePatterns.get(fullPattern);
          // If the current isotope has a higher intensity than the stored one, replace it
          if (dataPoint.getIntensity() > existingIsotope.getIntensity()) {
            isotopePatterns.put(fullPattern, dataPoint);
          }
        } else {
          isotopePatterns.put(fullPattern, dataPoint);
        }
      }
    }

    for (Map.Entry<Set<DataPoint>, DataPoint> entry : isotopePatterns.entrySet()) {
      DataPoint intenseIsotope = entry.getValue(); // The most intense isotope
      Set<DataPoint> fullPattern = entry.getKey(); // The set of isotopes

      // Exclude the current isotope from the list of isotopic peaks
      List<DataPoint> sortedIsotopes = fullPattern.stream()
          .filter(iso -> !iso.equals(intenseIsotope))
          .sorted(Comparator.comparingDouble(DataPoint::getMZ)).collect(Collectors.toList());
      IsotopeMatch match = new IsotopeMatch(intenseIsotope, sortedIsotopes);

      result.addMatch(intenseIsotope, match);
    }
  }

  private void processAdducts(DataPoint[] dataPoints, DataPoint target, IsotopeAndAdducts result) {
    // Most occurring mass diffs taken from 10.1021/acs.analchem.4c00966
    Object[][] knownMassDifferences = { //
        // {1.0034, "12C and 13C"}, // Not specific enough
        // {67.9874, "sodium formate"}, // This one could also be a loss combined to an adduct
        // {1.9971, "37Cl and 35Cl"}, // Not specific enough
        {0.5017, "12C 13C double charge"}, //
        {21.9819, "H Na"}, //
        // {18.0106, "H2O"}, // Loss
        // {44.0262, "C2H4O"}, // Loss
        // {135.9748, "2 sodium formate"},  // This one could also be a loss combined to an adduct
        // {14.0157, "CH2"}, // Loss
        // { 2.0157, "2H"}, // Not specific enough
        {0.3345, "12C 13C triple charge"}, //
        // {26.0157, "C2H2"}, // Loss
        // {1.0009, "7Li and 6Li"}, // Not specific enough
        {57.9586, "NaCl"}, //
        // {46.0055, "formic acid"}, // This one could also be a loss
        {15.9739, "Na K"} //
        // {28.0313, "C2H4"} // Loss
        // {2.0067, "3C + 13C − 12C + 12C "} // Not specific enough
    };

    double proton = 1.007276;
    double targetMZ = target.getMZ();

    // Calculate M-specific differences
    double singleProton = targetMZ + proton;
    double diffDimerSingle = 2 * targetMZ + proton - singleProton;
    double diffSingleDouble = singleProton - (targetMZ + 2 * proton) / 2;

    ArrayList<Object[]> updatedMassDifferences = new ArrayList<>(
        Arrays.asList(knownMassDifferences));
    updatedMassDifferences.add(new Object[]{diffDimerSingle, "dimer - single"});
    updatedMassDifferences.add(new Object[]{diffSingleDouble, "single - double"});

    for (Object[] massDiffPair : updatedMassDifferences) {
      double massDiff = (double) massDiffPair[0];
      String adductName = (String) massDiffPair[1];
      double expectedMZ = targetMZ + massDiff;

      for (DataPoint point : dataPoints) {
        double pointMz = point.getMZ();
        if (Math.abs(pointMz - expectedMZ) <= toleranceMs1.getMzToleranceForMass(pointMz)) {
          List<DataPoint> adducts = new ArrayList<>();
          List<String> adductNames = new ArrayList<>();
          adducts.add(point);
          adductNames.add(adductName);
          AdductMatch match = new AdductMatch(target, adducts, adductNames);
          result.addAdductMatch(target, match);
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

    double neutron = 1.008666;

    for (int i = 0; i < isotopes.size() - 1; i++) {
      double mzFirst = isotopes.get(i).getMZ();
      double mzSecond = isotopes.get(i + 1).getMZ();
      double mzDiff = Math.abs(mzSecond - mzFirst);

      double intensityFirst = isotopes.get(i).getIntensity();
      double intensitySecond = isotopes.get(i + 1).getIntensity();

      // Get the mass tolerance based on the average m/z of the pair
      double avgMz = (mzFirst + mzSecond) / 2.0;
      double mzTolerance = toleranceMs1.getMzToleranceForMass(avgMz);

      // Do not allow missing isotopes (m/z difference should not exceed expected range)
      if (mzDiff > neutron + mzTolerance) {
        return false;
      }

      // Check for M + 1 isotope intensity (valid up to ~~C75)
      if (mzDiff < neutron - mzTolerance) {
        if (i + 2 < isotopes.size()) {
          double intensityThird = isotopes.get(i + 2).getIntensity();
          if (intensityThird >= intensityFirst) {
            return false;
          }
        }
        continue;
      }

      // Intensity of M + 1 should not be higher than M
      if (intensitySecond >= intensityFirst) {
        return false;
      }
    }
    // TODO: Implement better logic
    // If all checks passed, return true
    return true;
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
   * @param toleranceMsn          The tolerance in MSn.
   * @param matches               The matches for adducts and isotopes.
   * @return The analysis result.
   */
  private SignalsAnalysisResult analyzeSignals(Range<Double> mzRange, List<Scan> ms1Scans,
      List<Scan> allPrecursorsMsnScans, MZTolerance toleranceMs1, MZTolerance toleranceMsn,
      IsotopeAndAdducts matches) {

    // Step 1: Collect unique signals for MS1 and MSn scans
    int minMs1Scans = (int) Math.ceil(ms1Scans.size() * 0.9);
    var ms1SignalMap = filterMap(collectUniqueSignals(ms1Scans, toleranceMs1), minMs1Scans);
    List<UniqueSignal> ms1Signals = mapToListNonOverlapping(ms1SignalMap);

    var ms2SignalMapAllPrecursors = collectUniqueSignals(allPrecursorsMsnScans, toleranceMsn);
    List<UniqueSignal> ms2SignalsAllPrecursors = mapToListNonOverlapping(ms2SignalMapAllPrecursors);

    var rowMsnScans = filterScansByMzRange(allPrecursorsMsnScans, mzRange);
    var ms2SignalMapRow = collectUniqueSignals(rowMsnScans, toleranceMsn);
    List<UniqueSignal> ms2SignalsRow = mapToListNonOverlapping(ms2SignalMapRow);

    // Step 2: Identify unique precursor signals and matching signals between MS1 and MSn scans
    var uniquePrecursorSignals = findUniquePrecursors(ms1SignalMap, allPrecursorsMsnScans);
    var ms2MatchesAllPrecursors = findUniqueMatches(ms2SignalsAllPrecursors, ms1SignalMap);
    var ms2MatchesRow = findUniqueMatches(ms2SignalsRow, ms1SignalMap);

    // Step 3: Analyze isotopes and adducts using the unified matches structure
    var adductsSignalMap = collectUniqueSignalsFromDataPoints(matches.getAdducts(), toleranceMs1);
    var isotopesSignalMap = collectUniqueSignalsFromDataPoints(matches.getIsotopes(), toleranceMs1);

    // Step 4: Find common signals between MS1 and MSn scans
    var commonSignals = findUniqueMatches(ms1Signals, ms2SignalMapAllPrecursors);
    var commonSignalsRow = findUniqueMatches(ms1Signals, ms2SignalMapRow);

    // Step 5: Extract and format signal data for analysis results
    IonTypeAnalysisResults results = new IonTypeAnalysisResults(
        formatValuesToString(ms1SignalMap, 3, false), formatValuesToString(ms1SignalMap, 0, true),
        formatValuesToString(ms2SignalsRow, 3, false),
        formatValuesToString(ms2SignalsAllPrecursors, 3, false),
        formatValuesToString(ms2SignalsAllPrecursors, 0, true),
        formatValuesToString(commonSignalsRow, 3, false),
        formatValuesToString(commonSignals, 3, false),
        formatValuesToString(ms2MatchesRow, 3, false),
        formatValuesToString(ms2MatchesAllPrecursors, 3, false),
        formatValuesToString(uniquePrecursorSignals, 3, false),
        formatValuesToString(adductsSignalMap, 3, false),
        formatValuesToString(isotopesSignalMap, 3, false));

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
      List<DataPoint> filteredDataPoints = new ArrayList<>();
      Integer charge = scan.getPrecursorCharge();
      Double precursorMz = scan.getPrecursorMz();

      for (DataPoint dp : dataPoints) {
        double mz = dp.getMZ();
        if (precursorMz != null) {
          Range<Double> toleranceRange = tolerance.getToleranceRange(precursorMz);
          double minMz = toleranceRange.lowerEndpoint();
          double maxMz = toleranceRange.upperEndpoint();
          if ((charge == null || charge > 1) && (mz < minMz || mz > maxMz)) {
            // For charge > 1 (or null), remove only data points within the precursor range
            filteredDataPoints.add(dp);
          } else if ((charge != null && charge <= 1) && (mz < minMz)) {
            // For charge <= 1, remove data points above the precursor m/z minus tolerance
            filteredDataPoints.add(dp);
          }
        } else {
          // Nothing if no precursor
          filteredDataPoints.add(dp);
        }
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

  public interface MatchType {

    String getTypeName();

    DataPoint getDataPoint();
  }

  private record SignalsAnalysisResult(IonTypeAnalysisResults results) {

  }

  public class IsotopeAndAdducts {

    private final Map<DataPoint, List<MatchType>> matches;  // To store both isotopes and adducts
    private final Map<DataPoint, List<AdductMatch>> adductMatches;  // For adducts specifically

    public IsotopeAndAdducts() {
      this.matches = new HashMap<>();
      this.adductMatches = new HashMap<>();
    }

    public void addMatch(DataPoint key, MatchType match) {
      matches.computeIfAbsent(key, _ -> new ArrayList<>()).add(match);
    }

    public void addAdductMatch(DataPoint key, AdductMatch match) {
      adductMatches.computeIfAbsent(key, _ -> new ArrayList<>()).add(match);
    }

    public Map<DataPoint, List<MatchType>> getMatches() {
      return matches;
    }

    public Map<DataPoint, List<AdductMatch>> getAdductMatches() {
      return adductMatches;
    }

    public List<DataPoint> getAdducts() {
      return adductMatches.entrySet().stream().flatMap(entry -> entry.getValue().stream())
          .map(AdductMatch::getDataPoint).collect(Collectors.toList());
    }

    public List<DataPoint> getIsotopes() {
      return matches.entrySet().stream().flatMap(entry -> entry.getValue().stream())
          .filter(match -> match instanceof IsotopeMatch)
          .flatMap(match -> ((IsotopeMatch) match).getIsotopes().stream())
          .collect(Collectors.toList());
    }
  }


  public class AdductMatch implements MatchType {

    private final DataPoint dataPoint; // The current MZ
    private final List<DataPoint> adducts; // List of associated adducts
    private final List<String> adductNames; // List of associated names

    public AdductMatch(DataPoint dataPoint, List<DataPoint> adducts, List<String> adductNames) {
      this.dataPoint = dataPoint;
      this.adducts = adducts;
      this.adductNames = adductNames;
    }

    @Override
    public String getTypeName() {
      return "Adduct";
    }

    @Override
    public DataPoint getDataPoint() {
      return dataPoint;
    }

    public List<DataPoint> getAdducts() {
      return adducts;
    }

    public List<String> getAdductNames() {
      return adductNames;
    }
  }


  public class IsotopeMatch implements MatchType {

    private final DataPoint currentIsotope; // The current MZ
    private final List<DataPoint> isotopes; // List of associated isotopes

    public IsotopeMatch(DataPoint currentIsotope, List<DataPoint> isotopes) {
      this.currentIsotope = currentIsotope;
      this.isotopes = isotopes;
    }

    @Override
    public String getTypeName() {
      return "Isotope";
    }

    @Override
    public DataPoint getDataPoint() {
      return currentIsotope;
    }

    public List<DataPoint> getIsotopes() {
      return isotopes;
    }
  }

}
