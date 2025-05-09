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

package io.github.mzmine.modules.dataprocessing.isolab_targeted;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.MobilityScanDataType;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.MobilityScanDataAccess;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.RtRelativeErrorType;
import io.github.mzmine.modules.dataprocessing.featdet_targeted.OverlappingCompoundAnnotation;
import io.github.mzmine.modules.dataprocessing.featdet_targeted.TargetedFeatureDetectionParameters;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.Gap;
import io.github.mzmine.modules.dataprocessing.gapfill_peakfinder.multithreaded.ImsGap;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.CSVParsingUtils.CompoundDbLoadResult;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.Element;
import org.openscience.cdk.config.IsotopeFactory;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

public class IsotopeLabelingTargetedTask extends AbstractTask {

  protected static final Range<Double> doubleInfiniteRange = Range.closed(Double.NEGATIVE_INFINITY,
      Double.POSITIVE_INFINITY);
  protected static final Range<Float> floatInfiniteRange = Range.closed(Float.NEGATIVE_INFINITY,
      Float.POSITIVE_INFINITY);

  private final BinningMobilogramDataAccess mobilogramBinning;
  private final ScanSelection scanSelection;
  private final List<Scan> matchingScans;
  private final IonNetworkLibrary ionLibrary;

  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private final MZmineProject project;
  private final RawDataFile dataFile;
  private final String suffix;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final MobilityTolerance mobTol;
  private final double intTolerance;
  private final ParameterSet parameters;
  private final int totalScans;
  private final File featureListFile;
  private final String fieldSeparator;
  private final int minDataPoints = 5;
  private final List<Element> tracedElements;
  private FeatureList processedFeatureList;
  private int processedScans;
  private int ID = 1;
  private CompoundDbLoadResult compoundResultLabeled;
  private Map<String, Map<String, double[]>> dataIsotopes; // Isotopic data cache

  // Isotope data class to store information about an isotope
  private static class IsotopeData {

    private final String element;
    private final int massNumber;
    private final double exactMass;
    private final double massShift;

    public IsotopeData(String element, int massNumber, double exactMass, double massShift) {
      this.element = element;
      this.massNumber = massNumber;
      this.exactMass = exactMass;
      this.massShift = massShift;
    }

    @Override
    public String toString() {
      return massNumber + element;
    }
  }

  // Map to store parsed isotope data
  private final Map<String, List<IsotopeData>> elementIsotopes = new HashMap<>();

  IsotopeLabelingTargetedTask(MZmineProject project, ParameterSet parameters, RawDataFile dataFile,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.parameters = parameters;

    suffix = parameters.getParameter(IsotopeLabelingTargetedParameters.suffix).getValue();
    scanSelection = parameters.getParameter(IsotopeLabelingTargetedParameters.scanSelection)
        .getValue();
    featureListFile = parameters.getParameter(IsotopeLabelingTargetedParameters.featureListFile)
        .getValue();
    fieldSeparator = parameters.getParameter(IsotopeLabelingTargetedParameters.fieldSeparator)
        .getValue();
    intTolerance = parameters.getParameter(IsotopeLabelingTargetedParameters.intTolerance)
        .getValue();
    mzTolerance = parameters.getParameter(IsotopeLabelingTargetedParameters.mzTolerance).getValue();
    rtTolerance = parameters.getEmbeddedParameterValueIfSelectedOrElseGet(
        IsotopeLabelingTargetedParameters.rtTolerance, () -> null);
    mobTol = parameters.getEmbeddedParameterValueIfSelectedOrElseGet(
        IsotopeLabelingTargetedParameters.mobilityTolerance, () -> null);
    tracedElements = parameters.getParameter(IsotopeLabelingTargetedParameters.elements).getValue();

    final boolean useIonLibrary = parameters.getValue(IsotopeLabelingTargetedParameters.ionLibrary);
    ionLibrary = useIonLibrary ? new IonNetworkLibrary(
        (IonLibraryParameterSet) parameters.getEmbeddedParameterValue(
            IsotopeLabelingTargetedParameters.ionLibrary)) : null;

    if (dataFile instanceof IMSRawDataFile imsRawDataFile) {
      mobilogramBinning = new BinningMobilogramDataAccess(imsRawDataFile,
          BinningMobilogramDataAccess.getRecommendedBinWidth(imsRawDataFile));
    } else {
      mobilogramBinning = null;
    }

    matchingScans = scanSelection.getMatchingScans(dataFile.getScans());
    totalScans = matchingScans.size();

    this.dataFile = dataFile;

    // Initialize isotopic data
    this.dataIsotopes = getDefaultIsotopicData();

    // Initialize isotope data for each tracked element
    for (Element element : tracedElements) {
      parseIsotopeData(element.getSymbol());
    }
  }

  /**
   * Parse isotope data for a specific element, loading mass differences between isotopes
   *
   * @param elementSymbol Element symbol to load isotope data for
   */
  private void parseIsotopeData(String elementSymbol) {
    logger.info("Parsing isotope data for element: " + elementSymbol);

    // Check if data is already loaded
    if (elementIsotopes.containsKey(elementSymbol)) {
      return;
    }

    // Get isotope data for this element
    if (!dataIsotopes.containsKey(elementSymbol)) {
      // Try to load data specifically for this element
      Map<String, Map<String, double[]>> specificData = loadSpecificElementIsotopeData(
          elementSymbol);
      if (specificData.containsKey(elementSymbol)) {
        dataIsotopes.put(elementSymbol, specificData.get(elementSymbol));
      } else {
        logger.warning("Could not load isotope data for element: " + elementSymbol);
        return;
      }
    }

    Map<String, double[]> elementData = dataIsotopes.get(elementSymbol);
    double[] masses = elementData.get("mass");

    if (masses.length < 2) {
      logger.warning(
          "Element " + elementSymbol + " has only one isotope, cannot calculate mass shifts");
      return;
    }

    // For each isotope, create an IsotopeData object with mass shift from the base isotope
    List<IsotopeData> isotopes = new ArrayList<>();
    double baseMass = masses[0]; // Most abundant isotope (typically the lightest)

    try {
      // Access CDK IsotopeFactory to get mass numbers
      IsotopeFactory isotopeFactory = Isotopes.getInstance();
      IIsotope[] cdkIsotopes = isotopeFactory.getIsotopes(elementSymbol);

      // Sort isotopes by mass
      Arrays.sort(cdkIsotopes, Comparator.comparingDouble(IIsotope::getExactMass));

      // Match masses with mass numbers
      for (int i = 0; i < masses.length; i++) {
        double mass = masses[i];
        double massShift = mass - baseMass;

        // Find matching isotope in CDK data
        int massNumber = findMassNumber(cdkIsotopes, mass);

        isotopes.add(new IsotopeData(elementSymbol, massNumber, mass, massShift));
        logger.fine(
            "Added isotope: " + massNumber + elementSymbol + " with mass " + mass + " and shift "
                + massShift);
      }

    } catch (IOException e) {
      logger.warning("Error accessing CDK isotope data: " + e.getMessage());

      // Fallback: estimate mass numbers based on masses
      for (int i = 0; i < masses.length; i++) {
        double mass = masses[i];
        double massShift = mass - baseMass;
        // Estimate mass number as rounded mass value
        int massNumber = (int) Math.round(mass);

        isotopes.add(new IsotopeData(elementSymbol, massNumber, mass, massShift));
        logger.fine("Added estimated isotope: " + massNumber + elementSymbol + " with mass " + mass
            + " and shift " + massShift);
      }
    }

    elementIsotopes.put(elementSymbol, isotopes);
  }

  /**
   * Find the mass number for a given mass by finding the closest match in the CDK isotope data
   *
   * @param cdkIsotopes Array of isotopes from CDK
   * @param mass        Mass to find
   * @return Mass number of the isotope
   */
  private int findMassNumber(IIsotope[] cdkIsotopes, double mass) {
    double minDiff = Double.MAX_VALUE;
    int bestMassNumber = 0;

    for (IIsotope isotope : cdkIsotopes) {
      double diff = Math.abs(isotope.getExactMass() - mass);
      if (diff < minDiff) {
        minDiff = diff;
        bestMassNumber = isotope.getMassNumber();
      }
    }

    return bestMassNumber;
  }

  /**
   * Retrieves isotopic data for common elements using CDK's IsotopeFactory. This method is adapted
   * from MetaboliteCorrectorFactory.
   *
   * @return A map containing isotopic data (abundance and mass) for each element
   */
  private Map<String, Map<String, double[]>> getDefaultIsotopicData() {
    logger.info("Loading isotopic data from CDK.");

    Map<String, Map<String, double[]>> isotopes = new HashMap<>();

    try {
      // Initialize CDK's IsotopeFactory
      IsotopeFactory isotopeFactory = Isotopes.getInstance();

      // Define elements of interest
      String[] elements = {"C", "H", "N", "P", "O", "S", "Si"};

      for (String element : elements) {
        // Get all isotopes for the current element
        IIsotope[] elementIsotopes = isotopeFactory.getIsotopes(element);

        // Filter and collect isotopes with abundance > 0
        List<IIsotope> validIsotopes = new ArrayList<>();
        for (IIsotope isotope : elementIsotopes) {
          if (isotope.getNaturalAbundance() != null && isotope.getNaturalAbundance() > 0.0) {
            validIsotopes.add(isotope);
          }
        }

        // Sort isotopes by mass number
        Collections.sort(validIsotopes, Comparator.comparingInt(IIsotope::getMassNumber));

        // Skip if no isotopes with abundance data were found
        if (validIsotopes.isEmpty()) {
          logger.warning("No isotopes with abundance data found for element: " + element);
          continue;
        }

        // Create arrays for abundance and mass
        double[] abundances = new double[validIsotopes.size()];
        double[] masses = new double[validIsotopes.size()];

        // Fill arrays with data
        for (int i = 0; i < validIsotopes.size(); i++) {
          IIsotope isotope = validIsotopes.get(i);
          // CDK stores abundance as percentage (0-100), convert to fraction (0-1)
          abundances[i] = isotope.getNaturalAbundance() / 100.0;
          masses[i] = isotope.getExactMass();
        }

        // Store data in the map
        isotopes.put(element, new HashMap<>());
        isotopes.get(element).put("abundance", abundances);
        isotopes.get(element).put("mass", masses);

        logger.info(
            "Loaded isotopic data for " + element + ": " + validIsotopes.size() + " isotopes");
      }

      logger.info("Successfully loaded isotopic data from CDK.");
      return isotopes;

    } catch (IOException e) {
      logger.warning("Error loading isotope data from CDK: " + e.getMessage());
      // Fallback to hardcoded data if CDK fails
      return getFallbackIsotopicData();
    } catch (Exception e) {
      logger.warning("Unexpected error when loading isotope data from CDK: " + e.getMessage());
      // Fallback if any other error occurs
      return getFallbackIsotopicData();
    }
  }

  /**
   * Provides hardcoded isotopic data as a fallback when CDK is unavailable. This method is adapted
   * from MetaboliteCorrectorFactory.
   */
  private Map<String, Map<String, double[]>> getFallbackIsotopicData() {
    logger.info("Loading fallback isotopic data.");

    Map<String, Map<String, double[]>> isotopes = new HashMap<>();

    // Carbon isotopes
    isotopes.put("C", new HashMap<>());
    isotopes.get("C").put("abundance", new double[]{0.9893, 0.0107});
    isotopes.get("C").put("mass", new double[]{12.0, 13.003354835});

    // Hydrogen isotopes
    isotopes.put("H", new HashMap<>());
    isotopes.get("H").put("abundance", new double[]{0.999885, 0.000115});
    isotopes.get("H").put("mass", new double[]{1.0078250322, 2.0141017781});

    // Nitrogen isotopes
    isotopes.put("N", new HashMap<>());
    isotopes.get("N").put("abundance", new double[]{0.99636, 0.00364});
    isotopes.get("N").put("mass", new double[]{14.003074004, 15.000108899});

    // Phosphorus isotopes
    isotopes.put("P", new HashMap<>());
    isotopes.get("P").put("abundance", new double[]{1.0});
    isotopes.get("P").put("mass", new double[]{30.973761998});

    // Oxygen isotopes
    isotopes.put("O", new HashMap<>());
    isotopes.get("O").put("abundance", new double[]{0.99757, 0.00038, 0.00205});
    isotopes.get("O").put("mass", new double[]{15.99491462, 16.999131757, 17.999159613});

    // Sulfur isotopes
    isotopes.put("S", new HashMap<>());
    isotopes.get("S").put("abundance", new double[]{0.9499, 0.0075, 0.0425, 0.0, 0.0001});
    isotopes.get("S")
        .put("mass", new double[]{31.972071174, 32.971458910, 33.9678670, 35.0, 35.967081});

    // Silicon isotopes
    isotopes.put("Si", new HashMap<>());
    isotopes.get("Si").put("abundance", new double[]{0.92223, 0.04685, 0.03092});
    isotopes.get("Si").put("mass", new double[]{27.976926535, 28.976494665, 29.9737701});

    logger.info("Loaded fallback isotopic data.");
    return isotopes;
  }

  /**
   * Load isotope data specifically for the given element. This method is adapted from
   * MetaboliteCorrectorFactory.
   *
   * @param element The chemical element to load data for
   * @return A map containing isotope data for the requested element
   */
  private Map<String, Map<String, double[]>> loadSpecificElementIsotopeData(String element) {
    logger.info("Loading specific isotope data for element: " + element);

    Map<String, Map<String, double[]>> isotopes = new HashMap<>();

    try {
      // Initialize CDK's IsotopeFactory
      IsotopeFactory isotopeFactory = Isotopes.getInstance();

      // Get all isotopes for the requested element
      IIsotope[] elementIsotopes = isotopeFactory.getIsotopes(element);

      if (elementIsotopes == null || elementIsotopes.length == 0) {
        logger.warning("No isotopes found for element: " + element);
        return isotopes; // Return empty map
      }

      // Filter and collect isotopes with abundance > 0
      List<IIsotope> validIsotopes = new ArrayList<>();
      for (IIsotope isotope : elementIsotopes) {
        if (isotope.getNaturalAbundance() != null && isotope.getNaturalAbundance() > 0.0) {
          validIsotopes.add(isotope);
        }
      }

      // Sort isotopes by mass number
      Collections.sort(validIsotopes, Comparator.comparingInt(IIsotope::getMassNumber));

      // Skip if no isotopes with abundance data were found
      if (validIsotopes.isEmpty()) {
        logger.warning("No isotopes with abundance data found for element: " + element);
        return isotopes; // Return empty map
      }

      // Create arrays for abundance and mass
      double[] abundances = new double[validIsotopes.size()];
      double[] masses = new double[validIsotopes.size()];

      // Fill arrays with data
      for (int i = 0; i < validIsotopes.size(); i++) {
        IIsotope isotope = validIsotopes.get(i);
        // CDK stores abundance as percentage (0-100), convert to fraction (0-1)
        abundances[i] = isotope.getNaturalAbundance() / 100.0;
        masses[i] = isotope.getExactMass();
      }

      // Store data in the map
      isotopes.put(element, new HashMap<>());
      isotopes.get(element).put("abundance", abundances);
      isotopes.get(element).put("mass", masses);

      logger.info("Loaded specific isotope data for " + element + ": " + validIsotopes.size()
          + " isotopes");

    } catch (IOException e) {
      logger.warning("Error loading specific isotope data for " + element + ": " + e.getMessage());
    } catch (Exception e) {
      logger.warning("Unexpected error when loading specific isotope data for " + element + ": "
          + e.getMessage());
    }

    return isotopes;
  }

  public static List<OverlappingCompoundAnnotation> findAndMergeOverlaps(
      final List<CompoundDBAnnotation> annotations, final MZTolerance mzTol,
      final RTTolerance rtTol, final MobilityTolerance mobTol) {

    // to check for rough overlaps within overlapping annotations. otherwise we don't need to check
    // the whole bunch.
    final MZTolerance doubleTolerance = new MZTolerance(mzTol.getMzTolerance() * 2,
        mzTol.getPpmTolerance() * 2);

    final List<OverlappingCompoundAnnotation> overlappingCompoundAnnotations = new ArrayList<>();
    final List<CompoundDBAnnotation> sortedAnnotations = new ArrayList(annotations);
    sortedAnnotations.sort(Comparator.comparingDouble(CompoundDBAnnotation::getPrecursorMZ));

    while (!sortedAnnotations.isEmpty()) {
      final CompoundDBAnnotation annotation = sortedAnnotations.remove(0);

      final Range<Double> doubleToleranceRange = doubleTolerance.getToleranceRange(
          annotation.getPrecursorMZ());
      final double upperBreakPoint = doubleToleranceRange.upperEndpoint();
      final double lowerBreakPoint = doubleToleranceRange.lowerEndpoint();

      final OverlappingCompoundAnnotation overlappingAnnotation = new OverlappingCompoundAnnotation(
          annotation, mzTol, rtTol, mobTol);

      // check against all remaining annotations
      if (sortedAnnotations.isEmpty()) {
        continue;
      }
      for (Iterator<CompoundDBAnnotation> iterator = sortedAnnotations.iterator();
          iterator.hasNext(); ) {
        final CompoundDBAnnotation sortedAnnotation = iterator.next();
        if (sortedAnnotation.getPrecursorMZ() > upperBreakPoint) {
          break;
        }

        if (overlappingAnnotation.offerAnnotation(sortedAnnotation)) {
          iterator.remove();
        }
      }

      // check against all already overlapping annotations
      for (int i = overlappingCompoundAnnotations.size() - 1; i > 0; i--) {
        final OverlappingCompoundAnnotation otherOverlaps = overlappingCompoundAnnotations.get(i);
        final List<CompoundDBAnnotation> oldOverlaps = otherOverlaps.getAnnotations();
        if (oldOverlaps.get(0).getPrecursorMZ() < lowerBreakPoint) {
          break;
        }

        for (CompoundDBAnnotation oldOverlap : oldOverlaps) {
          overlappingAnnotation.offerAnnotation(oldOverlap);
        }
      }

      overlappingCompoundAnnotations.add(overlappingAnnotation);
    }

    return overlappingCompoundAnnotations;
  }

  public void run() {

    setStatus(TaskStatus.PROCESSING);

    // Create new feature list
    processedFeatureList = new ModularFeatureList(dataFile.getName() + " " + suffix,
        getMemoryMapStorage(), dataFile);
    processedFeatureList.setSelectedScans(dataFile, matchingScans);

    CompoundDbLoadResult compoundResult = CSVParsingUtils.getAnnotationsFromCsvFile(featureListFile,
        fieldSeparator, parameters.getValue(IsotopeLabelingTargetedParameters.columns), ionLibrary);

    compoundResultLabeled = updateLabeledFeatureList(compoundResult);

    final List<OverlappingCompoundAnnotation> mergedAnnotations = findAndMergeOverlaps(
        compoundResultLabeled.annotations(), mzTolerance, rtTolerance, mobTol);
    logger.finest(() -> String.format("Merged %d raw annotations to %d overlapping annotations.",
        compoundResultLabeled.annotations().size(), mergedAnnotations.size()));
    if (mergedAnnotations == null || mergedAnnotations.isEmpty()) {
      setErrorMessage("Error while merging compound annotations. No annotations remaining.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    final List<Gap> gaps = new ArrayList<>();
    for (int row = 0; row < mergedAnnotations.size(); row++) {
      FeatureListRow newRow = new ModularFeatureListRow((ModularFeatureList) processedFeatureList,
          ID++);
      final OverlappingCompoundAnnotation mergedAnnotation = mergedAnnotations.get(row);

      final Range<Double> mzRange = mzTolerance.getToleranceRange(
          mergedAnnotation.evaluateMergedToleranceRange(mzTolerance));
      final Range<Float> rtRange = mergedAnnotation.evaluateMergedRtToleranceRange(rtTolerance);
      final Range<Float> mobRange = mergedAnnotation.evaluateMergedMobilityToleranceRange(mobTol);

      newRow.setCompoundAnnotations(mergedAnnotation.getAnnotations());

      final Gap newGap;
      if (dataFile instanceof IMSRawDataFile) {
        newGap = new ImsGap(newRow, dataFile, mzRange, rtRange, mobRange, intTolerance,
            mobilogramBinning, false);
        gaps.add(newGap);
      } else {
        newGap = new Gap(newRow, dataFile, mzRange, rtRange, intTolerance, false);
        gaps.add(newGap);
      }
    }

    // Canceled?
    if (isCanceled()) {
      return;
    }

    // Get all scans of this data file
    if (dataFile instanceof IMSRawDataFile imsFile) {
      if (!processImsFile(gaps, imsFile)) {
        return;
      }
    } else if (dataFile instanceof RawDataFile) {
      if (!processLcmsFile(gaps)) {
        return;
      }
    }

    // evaluate compound annotations
    for (FeatureListRow row : processedFeatureList.getRows()) {
      row.getCompoundAnnotations().forEach(a -> {
        a.put(MzPpmDifferenceType.class,
            (float) MathUtils.getPpmDiff(a.getPrecursorMZ(), row.getAverageMZ()));
        if (a.getRT() != null) {
          a.put(RtRelativeErrorType.class, (row.getAverageRT() - a.getRT()) / a.getRT());
        }
        a.setScore(a.calculateScore(row, mzTolerance, rtTolerance, mobTol, null));
      });
      row.getCompoundAnnotations()
          .sort(Comparator.comparingDouble(a -> a.getScore() != null ? a.getScore() : 0f));
    }

    // Append processed feature list to the project
    project.addFeatureList(processedFeatureList);

    dataFile.getAppliedMethods().forEach(m -> processedFeatureList.getAppliedMethods().add(m));
    // Add task description to peakList
    processedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Isotopologue peak detection ",
            IsotopeLabelingTargetedModule.class, parameters, getModuleCallDate()));

    logger.log(Level.INFO, "Finished targeted feature detection on {0}", this.dataFile);
    setStatus(TaskStatus.FINISHED);
  }

  private boolean processImsFile(List<? extends Gap> gaps, IMSRawDataFile imsFile) {
    final MobilityScanDataAccess access = new MobilityScanDataAccess(imsFile,
        MobilityScanDataType.MASS_LIST,
        (List<Frame>) processedFeatureList.getSeletedScans(imsFile));
    List<ImsGap> imsGaps = (List<ImsGap>) gaps;

    while (access.hasNextFrame()) {
      if (isCanceled()) {
        return false;
      }

      for (ImsGap gap : imsGaps) {
        access.resetMobilityScan();
        gap.offerNextScan(access);
      }
      processedScans++;
    }

    for (Gap gap : gaps) {
      final FeatureListRow row = gap.getFeatureListRow();
      if (gap.noMoreOffers(minDataPoints)) {
        processedFeatureList.addRow(row);
      }
    }
    return true;
  }

  private boolean processLcmsFile(List<Gap> gaps) {
    final ScanDataAccess access = EfficientDataAccess.of(dataFile, ScanDataType.MASS_LIST,
        matchingScans);

    while (access.hasNextScan()) {
      access.nextScan();
      // Canceled?
      if (isCanceled()) {
        return false;
      }

      // Feed this scan to all gaps
      for (Gap gap : gaps) {
        gap.offerNextScan(access);
      }

      processedScans++;
    }

    for (Gap gap : gaps) {
      // Finalize gaps
      final FeatureListRow row = gap.getFeatureListRow();
      if (gap.noMoreOffers()) {
        processedFeatureList.addRow(row);
      }
    }
    return true;
  }

  public double getFinishedPercentage() {
    if (totalScans == 0) {
      return 0;
    }
    return (double) processedScans / (double) totalScans;
  }

  /**
   * Get atom count for a specific element in a chemical formula
   *
   * @param formula The chemical formula to search in
   * @param element Element symbol to count
   * @return Number of atoms for the specified element
   */
  public static int getAtomCount(String formula, String element) {
    // Regex to find the specified element followed by zero or more digits
    Pattern pattern = Pattern.compile(element + "(\\d*)");
    Matcher matcher = pattern.matcher(formula);

    if (matcher.find()) {
      String number = matcher.group(1);  // Extract the part after the element symbol
      if (number.isEmpty()) {
        return 1;  // Default to 1 if no digits follow the element
      } else {
        return Integer.parseInt(number);  // Parse the number
      }
    } else {
      return 0;  // Element not found in the formula
    }
  }

  /**
   * Generate all possible isotopically labeled combinations for a compound
   *
   * @param compoundResult Original compound result with no isotopic labeling
   * @return New compound result with all isotopically labeled variants added
   */
  public CompoundDbLoadResult updateLabeledFeatureList(CompoundDbLoadResult compoundResult) {
    logger.info("Generating isotopically labeled compounds");
    List<String[]> labeledEntries = new ArrayList<>(64);

    for (CompoundDBAnnotation annotation : compoundResult.annotations()) {
      String formula = annotation.getFormula();

      // Add the original unlabeled compound first
      String[] row = new String[4];
      row[0] = annotation.getCompoundName();
      row[1] = String.valueOf(annotation.getPrecursorMZ());
      row[2] = String.valueOf(annotation.getRT());
      row[3] = annotation.getFormula();
      labeledEntries.add(row);

      // Process each traced element and generate labeled variants
      for (Element elementObj : tracedElements) {
        String element = elementObj.getSymbol();
        int atomCount = getAtomCount(formula, element);

        if (atomCount == 0) {
          logger.info("Element " + element + " not found in formula " + formula + ", skipping");
          continue;
        }

        // Make sure isotope data is loaded for this element
        parseIsotopeData(element);

        // Skip if no isotopes are available
        if (!elementIsotopes.containsKey(element) || elementIsotopes.get(element).size() <= 1) {
          logger.info("No isotope data available for " + element + ", skipping");
          continue;
        }

        // Get the list of isotopes for this element
        List<IsotopeData> isotopes = elementIsotopes.get(element);

        // Start from index 1 (first heavy isotope) as index 0 is the base isotope
        for (int isotopeIndex = 1; isotopeIndex < isotopes.size(); isotopeIndex++) {
          IsotopeData isotope = isotopes.get(isotopeIndex);

          // Create labeled variants with 1 to N atoms labeled
          for (int labelCount = 1; labelCount <= atomCount; labelCount++) {
            // Create a new chemical formula by replacing some atoms with their isotopes
            ChemicalFormula chemicalFormula = new ChemicalFormula(formula);
            chemicalFormula.modifyElementCount(element, -labelCount);
            chemicalFormula.addIsotope(element, labelCount, isotope.massNumber);
            String labeledFormula = chemicalFormula.reconstructFormula();

            // Calculate mass shift based on actual mass differences from isotope data
            double massShift = labelCount * isotope.massShift;

            // Create labeled compound name with isotope notation
            String labeledCompoundName =
                isotope.toString() + labelCount + "-" + annotation.getCompoundName();

            row = new String[4];
            row[0] = labeledCompoundName;
            row[1] = String.valueOf(annotation.getPrecursorMZ() + massShift);
            row[2] = String.valueOf(annotation.getRT());
            row[3] = labeledFormula;
            labeledEntries.add(row);

            logger.fine(
                "Added labeled compound: " + labeledCompoundName + " with formula " + labeledFormula
                    + " and mass shift " + massShift);
          }
        }
      }
    }

    // Write labeled entries to a temporary CSV file
    Path tempFile = null;
    try {
      tempFile = Files.createTempFile("isotope_labeling", ".csv");

      // Automatically delete the file when the program ends
      tempFile.toFile().deleteOnExit();

      // Writing data to the temporary file
      try (var writer = Files.newBufferedWriter(tempFile, StandardOpenOption.WRITE)) {
        writer.write(
            "mz" + fieldSeparator + "rt" + fieldSeparator + "formula" + fieldSeparator + "name");
        writer.newLine();
        for (int i = 1; i <= labeledEntries.size(); i++) {
          String[] entry = labeledEntries.get(i - 1);
          writer.write(
              entry[1] + fieldSeparator + entry[2] + fieldSeparator + entry[3] + fieldSeparator
                  + entry[0]);
          writer.newLine();
        }
      }

      // Log the number of entries
      logger.info("Created temporary file with " + labeledEntries.size() + " entries");

    } catch (IOException e) {
      logger.severe("Error creating temporary file: " + e.getMessage());
      e.printStackTrace();
    }

    // Read the temporary file to create the result
    File labeledEntriesFile = tempFile.toFile();
    final CompoundDbLoadResult labeledcompoundResult = CSVParsingUtils.getAnnotationsFromCsvFile(
        labeledEntriesFile, fieldSeparator,
        parameters.getValue(TargetedFeatureDetectionParameters.columns), ionLibrary);

    return labeledcompoundResult;
  }

  public String getTaskDescription() {
    return "Isotopologue peak detection " + this.dataFile;
  }
}