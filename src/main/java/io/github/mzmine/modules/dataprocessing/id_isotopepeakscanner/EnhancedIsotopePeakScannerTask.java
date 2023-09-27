package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import com.alanmrace.jimzmlparser.mzml.ScanList;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MobilityScan;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonMobilitySeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.datamodel.featuredata.impl.IonMobilogramTimeSeriesFactory;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonMobilogramTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.MobilityUnitType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.scores.IsotopePatternScoreType;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.dataprocessing.featdet_mobilityscanmerger.MobilityScanMergerTask;
import io.github.mzmine.modules.dataprocessing.featdet_mobilogram_summing.MobilogramBinningTask;
import io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner.IsotopePeakScannerTask.RatingType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.IonMobilityUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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


  private static final Logger logger = Logger.getLogger(
      EnhancedIsotopePeakScannerTask.class.getName());

//  /**
//   * Scanning for characteristic isotope pattern and their monoisotopic mass
//   *
//   * @param rows            apply to all rows
//   * @param mzTolerance     tolerance for signal matching
//   * @param minIntensity    minimum isotope intensity for prediction
//   */

  EnhancedIsotopePeakScannerTask(MZmineProject project, FeatureList peakList, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.parameters = parameters;
    this.project = project;
    this.peakList = peakList;

    mzTolerance = parameters.getParameter(IsotopePeakScannerParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(IsotopePeakScannerParameters.rtTolerance).getValue();
    mobTolerance = parameters.getParameter(IsotopePeakScannerParameters.mobTolerance).getValue();
    minPatternIntensity = parameters.getParameter(IsotopePeakScannerParameters.minPatternIntensity).getValue();
    element = parameters.getParameter(IsotopePeakScannerParameters.element).getValue();
    suffix = parameters.getParameter(IsotopePeakScannerParameters.suffix).getValue();
    charge = parameters.getParameter(IsotopePeakScannerParameters.charge).getValue();
    resultPeakList = new ModularFeatureList(peakList.getName() + " " + suffix,
        getMemoryMapStorage(), peakList.getRawDataFiles());
    minIsotopePatternScore = parameters.getParameter(IsotopePeakScannerParameters.minIsotopePatternScore).getValue();
    bestScores = parameters.getParameter(IsotopePeakScannerParameters.bestScores).getValue();
    onlyMonoisotopic = parameters.getParameter(IsotopePeakScannerParameters.onlyMonoisotopic).getValue();
    resolvedByMobility = parameters.getParameter(IsotopePeakScannerParameters.resolvedByMobility).getValue();
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
      logger.warning("PeakList.polarityType does not match selected polarity. " + getPeakListPolarity(
              peakList).toString() + "!=" + polarityType.toString());
    }

    ObservableList<FeatureListRow> rows = peakList.getRows();
    PeakListHandler featureMap = new PeakListHandler();
    HashMap<Integer, IsotopePattern> foundDetectedIsotopePattern = new HashMap<>();

    for (FeatureListRow row : rows) {
      featureMap.addRow(row);
    }

    // Scanning for characteristic isotope patterns for each element combination and charge.
    // Stored in "ResultMap" if the Similarity Score is higher than minScore.
    //In case of multiple scores for different charges, only the isotope pattern with the highest
    // score is stored as "detectedIsotopePattern.

    HashMap<Integer, IsotopePattern> calculatedIsotopePattern = new HashMap<>();
    HashMap<Integer, IsotopePattern> detectedIsotopePattern = new HashMap<>();
    HashMap<Integer, Double> allScores = new HashMap<>();
    HashMap<Integer, Double> scores = new HashMap<>();
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
             calculatedIsotopePattern.get(row.getID()).getDataPointMZRange(), resolvedByMobility );
          IsotopePattern detectedPattern = new SimpleIsotopePattern(detectedPatternDPs, charge,
              IsotopePatternStatus.DETECTED, "");
          IsotopePatternScoring scoring = new IsotopePatternScoring();
          double score = scoring.calculateIsotopeScore(detectedPattern,
              calculatedIsotopePattern.get(row.getID()), minPatternIntensity, mzTolerance);

          if (score >= minIsotopePatternScore) {
            if (detectedIsotopePattern.containsKey(row.getID())
                && allScores.get(row.getID()) > score) {
              continue;
            }
            detectedIsotopePattern.put(row.getID(), detectedPattern);
            allScores.put(row.getID(), score);
          }
        }
      }
      //Reduction of the signals to the monoisotopic signals, where the monoisotopic signal is
      // assumed to be the one with the highest SimilarityScore within the MZ range of the IsotopePattern.
      //A tolerance range of 0.01 was applied to avoid excluding isotopic patterns with very similar score values.


if (onlyMonoisotopic ) {
  for (Integer candidat : detectedIsotopePattern.keySet()) {
    double candidatScore = allScores.get(candidat);
    Boolean bestScore = true;
    for (Integer rowID : detectedIsotopePattern.keySet()) {
      FeatureListRow actualRow = featureMap.getRowByID(rowID);
      if (rtTolerance.checkWithinTolerance(actualRow.getAverageRT(),
              featureMap.getRowByID(candidat).getAverageRT())&&
          checkMobility(mobTolerance,featureMap.getRowByID(candidat),actualRow)) {
        if (Objects.requireNonNull(detectedIsotopePattern.get(candidat).getDataPointMZRange()).contains(actualRow.getAverageMZ())) {
          if (allScores.get(rowID) > (candidatScore + 0.01)) {
            bestScore = false;
            break;
          }
        }
      }
    }
    if (bestScore) {
      if (resultMap.containsID(candidat) && scores.get(candidat) > candidatScore) {
        continue;
      }
      resultMap.addRow(featureMap.getRowByID(candidat));
      scores.put(candidat, allScores.get(candidat));
      foundDetectedIsotopePattern.put(candidat, detectedIsotopePattern.get(candidat));
    }
  }
}
else{
  for (Integer candidat : detectedIsotopePattern.keySet()) {
    resultMap.addRow(featureMap.getRowByID(candidat));
    scores.put(candidat, allScores.get(candidat));
    foundDetectedIsotopePattern.put(candidat, detectedIsotopePattern.get(candidat));}
  }

      detectedIsotopePattern.clear();
    }

    // Reduction of signals to those with the best similarity values of all considered element combinations.
    if (bestScores) {
      for (Integer candidat : foundDetectedIsotopePattern.keySet()) {
        double candidatScore = scores.get(candidat);
        Boolean bestScore = true;
        for (Integer rowID : foundDetectedIsotopePattern.keySet()) {
          FeatureListRow actualRow = resultMap.getRowByID(rowID);
          if (rtTolerance.checkWithinTolerance(actualRow.getAverageRT(),
              resultMap.getRowByID(candidat).getAverageRT())) {
            if (Objects.requireNonNull(
                foundDetectedIsotopePattern.get(candidat).getDataPointMZRange()).contains(actualRow.getAverageMZ())) {
                if (scores.get(rowID) > (candidatScore + 0.01)) {
                  bestScore = false;
                  break;
                }
              }
            }
          }
          if (bestScore) {
            resultMap.getRowByID(candidat).getBestFeature().setIsotopePattern(foundDetectedIsotopePattern.get(candidat));
            finalMap.addRow(resultMap.getRowByID(candidat));
          }
        }
      }
    else {
      finalMap = resultMap;
    }
    ArrayList<Integer> keys = finalMap.getAllKeys();
    for (Integer key : keys) {
      ModularFeatureListRow bestRow = new ModularFeatureListRow(resultPeakList, key, (ModularFeatureListRow) finalMap.getRowByID(key), true);
      bestRow.getBestFeature().setIsotopePattern(foundDetectedIsotopePattern.get(key));

      float scoreFloat = scores.get(key).floatValue();
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
     IMolecularFormula elementFormula = MolecularFormulaManipulator.getMolecularFormula(e, builder);
     org.openscience.cdk.formula.IsotopePatternGenerator generator = new IsotopePatternGenerator(
         minPatternIntensity);
     generator.setMinResolution(mzTolerance.getMzTolerance());
     calculatedPatterns.put(e, generator.getIsotopes(elementFormula));
     builder2 = SilentChemObjectBuilder.getInstance();
     IMolecularFormula majorElementFormula = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(
         e, builder2);
     majorIsotopeOfPattern.put(e,MolecularFormulaManipulator.getMass(majorElementFormula));
   }
    org.openscience.cdk.formula.IsotopePattern pattern = calculatedPatterns.get(e);
    double[] massDiff = new double[pattern.getNumberOfIsotopes()];
      pattern.setCharge(charge);
      for (int i = 0; i < pattern.getNumberOfIsotopes(); i++) {
        massDiff[i] = (pattern.getIsotope(i).getMass() - majorIsotopeOfPattern.get(e)) / charge;
      }

      for (int i = 0; i < pattern.getNumberOfIsotopes(); i++) {
        if (mzTolerance.checkWithinTolerance(pattern.getIsotope(i).getMass(), majorIsotopeOfPattern.get(e))) {
          majorIntensity = pattern.getIsotope(i).getIntensity();
        }
      }
      DataPoint monoIsotope = new SimpleDataPoint(row.getAverageMZ(), row.getSumIntensity());

    DataPoint[] dp = new DataPoint[pattern.getNumberOfIsotopes()];
    for (int j = 0; j < pattern.getNumberOfIsotopes(); j++) {
      double calculatedMass = monoIsotope.getMZ() + massDiff[j];
      double calculatedIntensity =
          (pattern.getIsotope(j).getIntensity() / majorIntensity) * row.getBestFeature().getHeight();
      dp[j] = new SimpleDataPoint(calculatedMass, calculatedIntensity);
    }
    return dp;
  }

// Scanning for isotope signals in MS1Scan or MobilityScan. Takes the signal with the highest intensity within the mass range.
  public static DataPoint[] searchForIsotopePatternDataPoints(FeatureList peakList, FeatureListRow row,
      DataPoint[] calculatedDataPoints, MZTolerance mzTolerance, double minHeight, Range <Double> mzRangeOfPattern, boolean resolvedMobility) {
    var ms1Scan = row.getBestFeature().getRepresentativeScan();

    final DataPoint[] ms1ScanPattern = new DataPoint[calculatedDataPoints.length];
    DataPoint[] detectedDps;
    RawDataFile raw = peakList.getRawDataFile(0);
    final ScanDataAccess scans = EfficientDataAccess.of(raw, ScanDataType.CENTROID, peakList.getSeletedScans(raw));

    final MobilityScanDataAccess mobScans = initMobilityScanDataAccess(peakList, raw, row);

    if (ms1Scan != null) {
      if (resolvedMobility) {
        ms1Scan = findBestScanOrMobilityScan(scans, mobScans, Objects.requireNonNull(row.getFeature(raw)), mzTolerance);
      }
      MassList ms1DefaultScan = ms1Scan.getMassList();
      if (ms1DefaultScan == null) {
        throw new MissingMassListException(ms1Scan);
      }
      DataPoint [] allData = new DataPoint [ms1Scan.getNumberOfDataPoints()];
      for (int i = 0; i < ms1Scan.getNumberOfDataPoints() ; i++) {
        allData [i] = new SimpleDataPoint(ms1Scan.getMzValue(i), ms1Scan.getIntensityValue(i));
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
    }
    return ms1ScanPattern;
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
    if (candidate.getAverageMobility()!= null &&row.getAverageMobility()!=null) {
      return mobTolerance.checkWithinTolerance(candidate.getAverageMobility(),
          row.getAverageMobility());
    } else {
      return true;
    }
  }

  private static Scan findBestScanOrMobilityScan(ScanDataAccess scans,
      MobilityScanDataAccess mobScans, Feature feature, MZTolerance mzTolerance) {

    final Scan maxScan = feature.getRepresentativeScan();
    final int scanIndex = scans.indexOf(maxScan);
    scans.jumpToIndex(scanIndex);

    final boolean mobility = feature.getMobility() != null;
    MobilityScan mobilityScan = null;
    if (mobility && mobScans != null) {
      final MobilityScan bestMobilityScan = IonMobilityUtils.getSummedMobilityScan(feature, mzTolerance);
      if (bestMobilityScan != null) {
        mobilityScan = mobScans.jumpToMobilityScan(bestMobilityScan);
      }
    }
    return mobilityScan != null ? mobScans : scans;
  }


  private static MobilityScanDataAccess initMobilityScanDataAccess(FeatureList peakList, RawDataFile raw,
      FeatureListRow row) {
    return raw instanceof IMSRawDataFile imsFile && row.getFeatureList().getFeatureTypes()
        .containsKey(MobilityUnitType.class) ? new MobilityScanDataAccess(imsFile,
        MobilityScanDataType.CENTROID, (List<Frame>) peakList.getSeletedScans(imsFile)) : null;
  }
}

