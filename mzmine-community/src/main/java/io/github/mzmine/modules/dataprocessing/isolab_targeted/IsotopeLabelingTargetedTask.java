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
import io.github.mzmine.modules.dataprocessing.featdet_targeted.ChemicalFormula;
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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.Element;

class IsotopeLabelingTargetedTask extends AbstractTask {

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
  private final int finishedLines = 0;
  private final int minDataPoints = 5;
  private FeatureList processedFeatureList;
  private int processedScans;
  private int ID = 1;
  private CompoundDbLoadResult compoundResultLabeled;

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
    List<Element> elements = parameters.getParameter(IsotopeLabelingTargetedParameters.elements)
        .getValue();

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

    // Calculate total number of scans in all files

    // Create new feature list
    processedFeatureList = new ModularFeatureList(dataFile.getName() + " " + suffix,
        getMemoryMapStorage(), dataFile);
    processedFeatureList.setSelectedScans(dataFile, matchingScans);

    CompoundDbLoadResult compoundResult = CSVParsingUtils.getAnnotationsFromCsvFile(featureListFile,
        fieldSeparator, parameters.getValue(IsotopeLabelingTargetedParameters.columns), ionLibrary);

    compoundResultLabeled = updateLabeledFeatureList(compoundResult);
    //System.out.println("Labeled compound result: " + compoundResultLabeled);
    //System.out.println(
    //    "Labeled compound result annotations: " + compoundResultLabeled.annotations());
    //System.out.println(
    //    "Labeled compound formula in row 6: " + compoundResultLabeled.annotations().get(6)
    //        .getFormula().toString());
    //System.out.println(
    //    "Labeled compound name in row 6: " + compoundResultLabeled.annotations().get(6)
    //        .getCompoundName().toString());

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
        new SimpleFeatureListAppliedMethod("Targeted feature detection ",
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

      final Frame frame = access.nextFrame();
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

  public CompoundDbLoadResult updateLabeledFeatureList(CompoundDbLoadResult compoundResult) {
    List<String[]> labeledEntries = new ArrayList<>(64);
    for (CompoundDBAnnotation annotation : compoundResult.annotations()) {
      String element = parameters.getParameter(IsotopeLabelingTargetedParameters.elements)
          .getValue().get(0).getSymbol();
      String formula = annotation.getFormula();
      int atomCount = getAtomCount(formula, element);

      // Construct the base row
      String[] row = new String[4];
      row[0] = annotation.getCompoundName();
      row[1] = String.valueOf(annotation.getPrecursorMZ());
      row[2] = String.valueOf(annotation.getRT());
      row[3] = annotation.getFormula();
      labeledEntries.add(row);

      // Construct labeled rows
      for (int i = 1; i <= atomCount; i++) {

        // Modify the carbon count and add a labeled carbon
        ChemicalFormula chemicalFormula = new ChemicalFormula(annotation.getFormula());
        chemicalFormula.modifyElementCount("C", -i);
        chemicalFormula.addIsotope("C", i);
        String labeledFormula = chemicalFormula.reconstructFormula();

        String labeledCompoundName = "13C" + i + "-" + annotation.getCompoundName();
        row = new String[4];
        row[0] = labeledCompoundName;
        // To do: change the mass shift to the actual mass shift for each potential atom/isotope
        row[1] = String.valueOf(annotation.getPrecursorMZ() + (i * 1.00335));
        row[2] = String.valueOf(annotation.getRT());
        row[3] = labeledFormula;
        System.out.println("Entire row: ");
        for (String s : row) {
          System.out.println(s.toString());
        }
        labeledEntries.add(row);
      }
    }

    Path tempFile = null;
    try {
      tempFile = Files.createTempFile("example", ".csv");

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

    } catch (IOException e) {
      e.printStackTrace();
    }
    // read the temporary file
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
