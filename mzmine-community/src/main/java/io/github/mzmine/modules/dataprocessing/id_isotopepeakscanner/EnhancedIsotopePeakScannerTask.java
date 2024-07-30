package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MergedMassSpectrum;
import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.numbers.scores.IsotopePatternScoreType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.formula.IsotopePatternGenerator;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;


public class EnhancedIsotopePeakScannerTask extends AbstractTask {

  private static ModularFeatureList resultPeakList;
  private ParameterSet parameters;
  private MZmineProject project;
  private FeatureList peakList;
  private MZTolerance mzTolerance;
  private MobilityTolerance mobTolerance;
  private io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance rtTolerance;
  private double minPatternIntensity;
  private String element, suffix;
  private int charge;
  private PolarityType polarityType;
  private double minIsotopePatternScore;
  private boolean bestScores;
  private boolean onlyMonoisotopic;
  private boolean resolvedByMobility;
  private double minHeight;

  private final Map<RawDataFile, ScanDataAccess> dataAccessMap = new HashMap<>();


  private static final Logger logger = Logger.getLogger(
      EnhancedIsotopePeakScannerTask.class.getName());

//  /**
//   * Scanning for characteristic isotope pattern and their monoisotopic mass
//   *
//   * @param rows            apply to all rows
//   * @param mzTolerance     tolerance for signal matching
//   * @param minIntensity    minimum isotope intensity for prediction
//   */

  EnhancedIsotopePeakScannerTask(MZmineProject project, FeatureList peakList,
      ParameterSet parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.parameters = parameters;
    this.project = project;
    this.peakList = peakList;

    mzTolerance = parameters.getParameter(IsotopePeakScannerParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(IsotopePeakScannerParameters.rtTolerance).getValue();
    mobTolerance = parameters.getParameter(IsotopePeakScannerParameters.mobTolerance).getValue();
    minPatternIntensity = parameters.getParameter(IsotopePeakScannerParameters.minPatternIntensity)
        .getValue();
    element = parameters.getParameter(IsotopePeakScannerParameters.element).getValue();
    suffix = parameters.getParameter(IsotopePeakScannerParameters.suffix).getValue();
    charge = parameters.getParameter(IsotopePeakScannerParameters.charge).getValue();
    resultPeakList = new ModularFeatureList(peakList.getName() + " " + suffix,
        getMemoryMapStorage(), peakList.getRawDataFiles());
    minIsotopePatternScore = parameters.getParameter(
        IsotopePeakScannerParameters.minIsotopePatternScore).getValue();
    bestScores = parameters.getParameter(IsotopePeakScannerParameters.bestScores).getValue();
    onlyMonoisotopic = parameters.getParameter(IsotopePeakScannerParameters.onlyMonoisotopic)
        .getValue();
    resolvedByMobility = parameters.getParameter(IsotopePeakScannerParameters.resolvedByMobility)
        .getValue();
    minHeight = parameters.getParameter(IsotopePeakScannerParameters.minHeight).getValue();

    polarityType = (charge > 0) ? PolarityType.POSITIVE : PolarityType.NEGATIVE;
    charge = (charge < 0) ? charge * -1 : charge;
  }

  @Override
  public void run() {

    if (!checkParameters()) {
      return;
    }

    if (getPeakListPolarity(peakList) != polarityType) {
      logger.warning(
          "PeakList.polarityType does not match selected polarity. " + getPeakListPolarity(
              peakList).toString() + "!=" + polarityType.toString());
    }

    ObservableList<FeatureListRow> rows = peakList.getRows();
    PeakListHandler featureMap = new PeakListHandler();
    HashMap<Integer, IsotopePattern> resultingIsotopePattern = new HashMap<>();

    for (FeatureListRow row : rows) {
      featureMap.addRow(row);
    }

    // Scanning for characteristic isotope patterns for each element combination and charge.
    // Stored in "ResultMap" if the Similarity Score is higher than minScore.
    //In case of multiple scores for different charges, only the isotope pattern with the highest
    // score is stored as "detectedIsotopePattern.

    HashMap<Integer, IsotopePattern> calculatedIsotopePattern = new HashMap<>();
    HashMap<Integer, IsotopePattern> detectedIsotopePattern = new HashMap<>();
    ArrayList <FeatureListRow> rowsWithIPs = new ArrayList <>();
    ArrayList <FeatureListRow> rowsWithBestIPs = new ArrayList <>();
    HashMap<Integer, Double> scores = new HashMap<>();
    HashMap<Integer, Double> resultingScores = new HashMap<>();
    HashMap<Integer, Integer> numbersOfFoundIsotopes = new HashMap<>();
    HashMap<Integer, Integer> bestCharges = new HashMap<>();
    PeakListHandler resultMap = new PeakListHandler();
    PeakListHandler finalMap = new PeakListHandler();
    String[] elements;
    int[] charges = new int[charge];
    for (int i = 0; i < charges.length; i++) {
      charges[i] = i + 1;
    }
    if (element.contains(",")) {
      elements = element.split(",");
    } else {
      elements = new String[1];
      elements[0] = element;
    }
    for (String element : elements) {
      for (int charge : charges) {

        for (FeatureListRow row : rows) {

          DataPoint[] calculatedPatternDPs;
          try {
            calculatedPatternDPs = calculateIsotopePatternDataPoints(row, element,
                minPatternIntensity, mzTolerance, charge);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          calculatedIsotopePattern.put(row.getID(),
              new SimpleIsotopePattern(calculatedPatternDPs, charge, IsotopePatternStatus.DETECTED,
                  ""));

          DataPoint[] detectedPatternDPs = searchForIsotopePatternDataPoints(peakList, row,
              calculatedPatternDPs, mzTolerance, minHeight,
              calculatedIsotopePattern.get(row.getID()).getDataPointMZRange(), resolvedByMobility);
          IsotopePattern detectedPattern = new SimpleIsotopePattern(detectedPatternDPs, charge,
              IsotopePatternStatus.DETECTED, "");
          IsotopePatternScoring scoring = new IsotopePatternScoring();
          double score = scoring.calculateIsotopeScore(detectedPattern,
              calculatedIsotopePattern.get(row.getID()), minPatternIntensity, mzTolerance);

          if (score >= minIsotopePatternScore) {
            if (scores.get(row.getID()) != null && scores.get(row.getID()) > score){
              continue;
            }
            detectedIsotopePattern.put(row.getID(), detectedPattern);
            row.getBestFeature().setCharge(charge);
            rowsWithIPs.add(row);
            scores.put(row.getID(), score);
            numbersOfFoundIsotopes.put(row.getID(), giveTheNumberOfFoundIsotopes());
            bestCharges.put(row.getID(), charge);

          }
        }
      }
      //Reduction of the features to the monoisotopic signals, whereby the monoisotopic signal is
      // assumed to be the one with the highest SimilarityScore within the MZ range of the IsotopePattern.
      //A tolerance range of 0.01 was applied to avoid excluding isotopic patterns with very similar score values.
      if (onlyMonoisotopic) {
        for (FeatureListRow candidate : rowsWithIPs) {
          boolean bestScore = checkIfRowHasTheHighestIPSimilarityScore(rowsWithIPs,candidate,scores,
              detectedIsotopePattern.get(candidate.getID()).getDataPointMZRange(),rtTolerance,mobTolerance, detectedIsotopePattern);
          double candidateScore = scores.get(candidate.getID());

          if (bestScore) {
            if (resultMap.containsID(candidate.getID()) && resultingScores.get(candidate.getID()) > candidateScore) {
              continue;
            }
            resultMap.addRow(candidate);
            rowsWithBestIPs.add(candidate);
            resultingIsotopePattern.put(candidate.getID(), detectedIsotopePattern.get(candidate.getID()));
            resultingScores.put(candidate.getID(),scores.get(candidate.getID()));
          }
        }
      } else {

        for (FeatureListRow candidate : rowsWithIPs) {

          if (resultMap.containsID(candidate.getID()) && resultingScores.get(candidate.getID()) > scores.get(candidate.getID())) {
            continue;
          }
          resultMap.addRow(candidate);
          rowsWithBestIPs.add(candidate);
          resultingIsotopePattern.put(candidate.getID(), detectedIsotopePattern.get(candidate.getID()));
          resultingScores.put(candidate.getID(), scores.get(candidate.getID()));

        }
      }
      rowsWithIPs.clear();
      detectedIsotopePattern.clear();
      scores.clear();
    }

    // Reduction of features to those with the best similarity values of all considered element combinations.
    if (bestScores) {
      for (FeatureListRow candidate : rowsWithBestIPs) {
        boolean bestScoreOfAllIPs = checkIfRowHasTheHighestIPSimilarityScore(rowsWithBestIPs,
            candidate,resultingScores,resultingIsotopePattern.get(candidate.getID()).getDataPointMZRange(),rtTolerance,mobTolerance, resultingIsotopePattern );
        if (bestScoreOfAllIPs) {
          candidate.getBestFeature().setIsotopePattern(resultingIsotopePattern.get(candidate.getID()));
          finalMap.addRow(candidate);
        }
      }
    } else {
      finalMap = resultMap;
    }

    ArrayList<Integer> keys = finalMap.getAllKeys();
    for (Integer key : keys) {
      ModularFeatureListRow bestRow = new ModularFeatureListRow(resultPeakList, key,
          (ModularFeatureListRow) finalMap.getRowByID(key), true);
      bestRow.getBestFeature().setIsotopePattern(resultingIsotopePattern.get(key));
      bestRow.getBestFeature().setCharge(finalMap.getRowByID(key).getBestFeature().getCharge());

      float scoreFloat = resultingScores.get(key).floatValue();
      bestRow.getBestFeature().set(IsotopePatternScoreType.class, scoreFloat);
      resultPeakList.addRow(bestRow);
    }
    if (resultPeakList.getNumberOfRows() > 1) {
      addResultToProject(resultPeakList);
    } else {
      //message = "Element not found.";
    }
    setStatus(TaskStatus.FINISHED);
  }

  //
//  /**
//   * Apply isotope scoring to filter for monoisotopic masses.
//   *
//   * @param row             apply to this row
//   * @param mzTolerance     tolerance for signal matching
//   * @param minIntensity    minimum isotope intensity for prediction
//   * @param minIsotopeScore minimum isotope score to retain annotations
//   */


  //Calculation of the theoretical isotope pattern for the row
  //The m/z of the row is assumed to be monoisotopic mass.
  private IChemObjectBuilder builder = null;
  private IChemObjectBuilder builder2 = null;
  HashMap<String, org.openscience.cdk.formula.IsotopePattern> calculatedPatterns = new HashMap<>();
  HashMap<String, Double> majorIsotopeOfPattern = new HashMap<>();
  double majorIntensity;

  public DataPoint[] calculateIsotopePatternDataPoints(FeatureListRow row, String e,
      double minPatternIntensity, MZTolerance mzTolerance, int charge) throws IOException {
    if (!calculatedPatterns.containsKey(e)) {
      builder = SilentChemObjectBuilder.getInstance();
      IMolecularFormula elementFormula = MolecularFormulaManipulator.getMolecularFormula(e,
          builder);
      org.openscience.cdk.formula.IsotopePatternGenerator generator = new IsotopePatternGenerator(
          minPatternIntensity);
      generator.setMinResolution(mzTolerance.getMzTolerance());
      calculatedPatterns.put(e, generator.getIsotopes(elementFormula));
      builder2 = SilentChemObjectBuilder.getInstance();
      IMolecularFormula majorElementFormula = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(
          e, builder2);
      majorIsotopeOfPattern.put(e, MolecularFormulaManipulator.getMass(majorElementFormula));
    }
    org.openscience.cdk.formula.IsotopePattern pattern = calculatedPatterns.get(e);
    double[] massDiff = new double[pattern.getNumberOfIsotopes()];
    pattern.setCharge(charge);
    for (int i = 0; i < pattern.getNumberOfIsotopes(); i++) {
      massDiff[i] = (pattern.getIsotope(i).getMass() - majorIsotopeOfPattern.get(e)) / charge;
    }

    for (int i = 0; i < pattern.getNumberOfIsotopes(); i++) {
      if (mzTolerance.checkWithinTolerance(pattern.getIsotope(i).getMass(),
          majorIsotopeOfPattern.get(e))) {
        majorIntensity = pattern.getIsotope(i).getIntensity();
      }
    }
    DataPoint monoIsotope = new SimpleDataPoint(row.getAverageMZ(), row.getSumIntensity());

    DataPoint[] dp = new DataPoint[pattern.getNumberOfIsotopes()];
    for (int j = 0; j < pattern.getNumberOfIsotopes(); j++) {
      double calculatedMass = monoIsotope.getMZ() + massDiff[j];
      double calculatedIntensity =
          (pattern.getIsotope(j).getIntensity() / majorIntensity) * row.getBestFeature()
              .getHeight();
      dp[j] = new SimpleDataPoint(calculatedMass, calculatedIntensity);
    }
    return dp;
  }

  int isotopeCounter = 0;
  // Scanning for isotope signals in MS1Scan or MobilityScan. Takes the signal with the highest intensity within the mass range.
  public DataPoint[] searchForIsotopePatternDataPoints(FeatureList featureList, FeatureListRow row,
      DataPoint[] calculatedDataPoints, MZTolerance mzTolerance, double minHeight,
      Range<Double> mzRangeOfPattern, boolean resolvedMobility) {
    var ms1Scan = row.getBestFeature().getRepresentativeScan();

    final DataPoint[] ms1ScanPattern = new DataPoint[calculatedDataPoints.length];
    DataPoint[] detectedDps;
    RawDataFile raw = featureList.getRawDataFile(0);

    ScanDataAccess scans = dataAccessMap.computeIfAbsent(raw,
        r -> EfficientDataAccess.of(raw, ScanDataType.MASS_LIST, featureList.getSeletedScans(raw)));

    if (ms1Scan != null) {
      if (resolvedMobility) {
        ms1Scan = findBestScanOrMobilityScan(scans, Objects.requireNonNull(row.getFeature(raw)),
            mzTolerance,mobTolerance);
      }

      MassList massList = ms1Scan.getMassList();
      if (massList == null) {
        throw new MissingMassListException(ms1Scan);
      }

      DataPoint[] allData = ScanUtils.extractDataPoints(ms1Scan, true);

      if (getNumberOfMergedScans()>1) {
        if (mzTolerance.checkWithinTolerance(ms1Scan.getBasePeakMz(), row.getAverageMZ())) {
          double baseIntensity = ms1Scan.getBasePeakIntensity();
          DataPoint baseDP = new SimpleDataPoint(baseIntensity / getNumberOfMergedScans(), ms1Scan.getBasePeakMz());
          allData[ms1Scan.getBasePeakIndex()] = baseDP;
        }
      }
      detectedDps = ScanUtils.getDataPointsByMass(allData, mzRangeOfPattern);
    } else {
      return null;
    }

    for (int i = 0; i < calculatedDataPoints.length; i++) {
      DataPoint dp = calculatedDataPoints[i];
      for (DataPoint detectedDp : detectedDps) {
        if (mzTolerance.checkWithinTolerance(dp.getMZ(), detectedDp.getMZ())
            && detectedDp.getIntensity() > minHeight) {
          if (ms1ScanPattern[i] == null) {
            ms1ScanPattern[i] = detectedDp;
          } else if (detectedDp.getIntensity() > ms1ScanPattern[i].getIntensity()) {
            ms1ScanPattern[i] = detectedDp;
          }
        }
      }
    }
    for (int i = 0; i < ms1ScanPattern.length; i++) {
      DataPoint isotope = ms1ScanPattern[i];
      if (isotope == null) {
        SimpleDataPoint nullPoint = new SimpleDataPoint(calculatedDataPoints[i].getMZ(), 0);
        ms1ScanPattern[i] = nullPoint;
      }
      else{
        isotopeCounter +=1;
      }
    }
    return ms1ScanPattern;
  }

  public Integer giveTheNumberOfFoundIsotopes(){
    return isotopeCounter;
  }

  // Comparing the scores of all features within the RT range and MZ range of the candidate feature's IsotopePattern
// with the candidate's isotope pattern score, if the candidate has the highest score the value becomes true
  public Boolean checkIfRowHasTheHighestIPSimilarityScore(ArrayList <FeatureListRow> rows,
      FeatureListRow candidate, HashMap <Integer, Double> scores, Range <Double> DataPointMZRange,
      RTTolerance rtTolerance, MobilityTolerance mobTolerance, HashMap<Integer, IsotopePattern> resultingIsotopePattern) {
    for (FeatureListRow row : rows) {
      Range <Double> MZRangeOfRow = resultingIsotopePattern.get(row.getID()).getDataPointMZRange();
      if (rtTolerance.checkWithinTolerance(row.getAverageRT(),
          candidate.getAverageRT())&&
          checkMobility(mobTolerance,candidate,row)
          && (DataPointMZRange.contains(row.getAverageMZ())
          || MZRangeOfRow.contains(row.getAverageMZ()))) {
        if (scores.get(row.getID()) > (scores.get(candidate.getID())+ 0.01)) {
          return false;
        }
      }
    }
    return true;
  }


  private PolarityType getPeakListPolarity(FeatureList peakList) {
    return peakList.getRawDataFiles().stream()
        .map(raw -> raw.getDataPolarity().stream().findFirst().orElse(PolarityType.UNKNOWN))
        .findFirst().orElse(PolarityType.UNKNOWN);
  }

  /**
   * @return The {@link MemoryMapStorage} used to store results of this task (e.g. RawDataFiles,
   * MassLists, FeatureLists). May be null if results shall be stored in ram.
   */
  @Nullable
  public MemoryMapStorage getMemoryMapStorage() {
    return storage;
  }

  @Override
  public String getTaskDescription() {
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }


  public void addResultToProject(ModularFeatureList resultPeakList) {
    // Add new peakList to the project
    project.addFeatureList(resultPeakList);

    // Load previous applied methods
    for (FeatureListAppliedMethod proc : peakList.getAppliedMethods()) {
      resultPeakList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to peakList
    resultPeakList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("IsotopePeakScanner", IsotopePeakScannerModule.class,
            parameters, getModuleCallDate()));
  }


  private boolean checkParameters() {
    if (charge == 0) {
      setErrorMessage("Error: charge may not be 0!");
      setStatus(TaskStatus.ERROR);
      return false;
    }
//    if (!FormulaUtils.checkMolecularFormula(element)) {
//      setErrorMessage("Error: Invalid formula!");
//      setStatus(TaskStatus.ERROR);
//      return false;
//    }

    RawDataFile[] raws = peakList.getRawDataFiles().toArray(RawDataFile[]::new);
    boolean foundMassList = false;
    for (RawDataFile raw : raws) {
      ObservableList<Scan> scanNumbers = raw.getScans();
      for (Scan scan : scanNumbers) {
        MassList massList = scan.getMassList();
        if (massList != null) {
          foundMassList = true;
          break;
        }
      }
    }
    if (foundMassList == false) {
      setErrorMessage("Feature list \"" + peakList.getName() + "\" does not contain a mass list");
      setStatus(TaskStatus.ERROR);
      return false;
    }

    return true;
  }


  public static Boolean checkMobility(MobilityTolerance mobTolerance, FeatureListRow candidate,
      FeatureListRow row) {
    if (candidate.getAverageMobility() != null && row.getAverageMobility() != null) {
      return mobTolerance.checkWithinTolerance(candidate.getAverageMobility(),
          row.getAverageMobility());
    } else {
      return true;
    }
  }

  int numberOfScans = 1;

  private static Scan findBestScanOrMobilityScan(ScanDataAccess scans, Feature feature,
      MZTolerance mzTolerance, MobilityTolerance mobTolerance) {

    final Scan maxScan = feature.getRepresentativeScan();
    final int scanIndex = scans.indexOf(maxScan);
    scans.jumpToIndex(scanIndex);

    final boolean mobility = feature.getMobility() != null;
    final IonTimeSeries<? extends Scan> featureData = feature.getFeatureData();
    if (mobility && featureData instanceof IonMobilogramTimeSeries imsData) {
      MergedMassSpectrum mergedMobilityScan = null;
      //final Range <Float> mobilityRange = mobTolerance.getToleranceRange(feature.getMobility());
      final Range<Float> mobilityFWHM = IonMobilityUtils.getMobilityFWHM(imsData.getSummedMobilogram());
      final List<MobilityScan> mobilityScans = imsData.getMobilograms().stream()
          .flatMap(s -> (s.getSpectra().stream())).filter(m -> {
            assert mobilityFWHM != null;
            return mobilityFWHM.contains((float) m.getMobility());
          }).toList();
      if (!mobilityScans.isEmpty()) {
        mergedMobilityScan = SpectraMerging.mergeSpectra(mobilityScans, mzTolerance,
            MergingType.ALL_ENERGIES, null);
        return mergedMobilityScan;
      }
    }
    return scans;
  }
  private int getNumberOfMergedScans() {
    return numberOfScans;
  }

}
