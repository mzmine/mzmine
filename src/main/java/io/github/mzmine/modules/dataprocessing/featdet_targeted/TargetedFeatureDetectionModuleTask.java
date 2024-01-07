/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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
package io.github.mzmine.modules.dataprocessing.featdet_targeted;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class TargetedFeatureDetectionModuleTask extends AbstractTask {

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
  private FeatureList processedFeatureList;
  private final String suffix;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final MobilityTolerance mobTol;
  private final double intTolerance;
  private final ParameterSet parameters;
  private int processedScans;
  private final int totalScans;
  private final File featureListFile;
  private final char fieldSeparator;
  private final int finishedLines = 0;
  private int ID = 1;
  private final int minDataPoints = 5;

  TargetedFeatureDetectionModuleTask(MZmineProject project, ParameterSet parameters,
      RawDataFile dataFile, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.parameters = parameters;

    suffix = parameters.getParameter(TargetedFeatureDetectionParameters.suffix).getValue();
    scanSelection = parameters.getParameter(TargetedFeatureDetectionParameters.scanSelection)
        .getValue();
    featureListFile = parameters.getParameter(TargetedFeatureDetectionParameters.featureListFile)
        .getValue();
    fieldSeparator =
        parameters.getParameter(TargetedFeatureDetectionParameters.fieldSeparator).getValue()
            .equals("\\t") ? '\t'
            : parameters.getParameter(TargetedFeatureDetectionParameters.fieldSeparator).getValue()
                .charAt(0);
    intTolerance = parameters.getParameter(TargetedFeatureDetectionParameters.intTolerance)
        .getValue();
    mzTolerance = parameters.getParameter(TargetedFeatureDetectionParameters.mzTolerance)
        .getValue();
    rtTolerance = parameters.getEmbeddedParameterValueIfSelectedOrElseGet(
        TargetedFeatureDetectionParameters.rtTolerance, () -> null);
    mobTol = parameters.getEmbeddedParameterValueIfSelectedOrElseGet(
        TargetedFeatureDetectionParameters.mobilityTolerance, () -> null);

    final boolean useIonLibrary = parameters.getValue(
        TargetedFeatureDetectionParameters.ionLibrary);
    ionLibrary = useIonLibrary ? new IonNetworkLibrary(
        (IonLibraryParameterSet) parameters.getEmbeddedParameterValue(
            TargetedFeatureDetectionParameters.ionLibrary)) : null;

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

  public void run() {

    setStatus(TaskStatus.PROCESSING);

    // Calculate total number of scans in all files

    // Create new feature list
    processedFeatureList = new ModularFeatureList(dataFile.getName() + " " + suffix,
        getMemoryMapStorage(), dataFile);
    processedFeatureList.setSelectedScans(dataFile, matchingScans);

    final CompoundDbLoadResult compoundResult = CSVParsingUtils.getAnnotationsFromCsvFile(
        featureListFile, fieldSeparator,
        parameters.getValue(TargetedFeatureDetectionParameters.columns), ionLibrary);
    if (compoundResult.status() == TaskStatus.ERROR) {
      setErrorMessage(compoundResult.errorMessage());
      setStatus(TaskStatus.ERROR);
      return;
    }

    final List<OverlappingCompoundAnnotation> mergedAnnotations = findAndMergeOverlaps(
        compoundResult.annotations(), mzTolerance, rtTolerance, mobTol);
    logger.finest(() -> String.format("Merged %d raw annotations to %d overlapping annotations.",
        compoundResult.annotations().size(), mergedAnnotations.size()));

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
            TargetedFeatureDetectionModule.class, parameters, getModuleCallDate()));

    logger.log(Level.INFO, "Finished targeted feature detection on {0}", this.dataFile);
    setStatus(TaskStatus.FINISHED);
  }

  private boolean processImsFile(List<? extends Gap> gaps, IMSRawDataFile imsFile) {
    final MobilityScanDataAccess access = new MobilityScanDataAccess(imsFile,
        MobilityScanDataType.MASS_LIST, (List<Frame>) processedFeatureList.getSeletedScans(imsFile));
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

  public double getFinishedPercentage() {
    if (totalScans == 0) {
      return 0;
    }
    return (double) processedScans / (double) totalScans;
  }

  public String getTaskDescription() {
    return "Targeted feature detection " + this.dataFile;
  }
}
