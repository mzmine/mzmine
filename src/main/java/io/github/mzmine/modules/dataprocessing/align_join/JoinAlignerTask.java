/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.align_join;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JoinAlignerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(JoinAlignerTask.class.getName());
  private final MZmineProject project;
  private final AtomicInteger alignedRows = new AtomicInteger(0);
  private final String featureListName;
  private final MZTolerance mzTolerance;
  private final RTTolerance rtTolerance;
  private final MobilityTolerance mobilityTolerance;
  private final double mzWeight;
  private final double rtWeight;
  private final double mobilityWeight;
  private final boolean sameIDRequired;
  private final boolean sameChargeRequired;
  private final boolean compareIsotopePattern;
  private final boolean compareSpectraSimilarity;
  private final ParameterSet parameters;
  private final boolean compareMobility;
  private final Double minIsotopeScore;
  private final Double isotopeNoiseLevel;
  private final MZTolerance isotopeMZTolerance;
  /**
   * All feature lists except the base list
   */
  private final List<FeatureList> featureLists;
  private ModularFeatureList alignedFeatureList;
  // Processed rows counter
  private int totalRows;
  // ID counter for the new peaklist
  private int iteration = 1;
  // fields for spectra similarity
  private MZmineProcessingStep<SpectralSimilarityFunction> simFunction;
  private int msLevel;

  public JoinAlignerTask(MZmineProject project, ParameterSet parameters,
      @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.parameters = parameters;

    featureLists = Arrays.stream(parameters.getParameter(JoinAlignerParameters.peakLists).getValue()
        .getMatchingFeatureLists()).map(flist -> (FeatureList) flist).toList();

    featureListName = parameters.getParameter(JoinAlignerParameters.peakListName).getValue();

    mzTolerance = parameters.getParameter(JoinAlignerParameters.MZTolerance).getValue();
    rtTolerance = parameters.getParameter(JoinAlignerParameters.RTTolerance).getValue();

    compareMobility = parameters.getParameter(JoinAlignerParameters.mobilityTolerance).getValue();
    mobilityTolerance = parameters.getParameter(JoinAlignerParameters.mobilityTolerance)
        .getEmbeddedParameter().getValue();

    mzWeight = parameters.getParameter(JoinAlignerParameters.MZWeight).getValue();
    rtWeight = parameters.getParameter(JoinAlignerParameters.RTWeight).getValue();
    mobilityWeight = parameters.getParameter(JoinAlignerParameters.mobilityWeight).getValue();

    sameChargeRequired = parameters.getParameter(JoinAlignerParameters.SameChargeRequired)
        .getValue();

    sameIDRequired = parameters.getParameter(JoinAlignerParameters.SameIDRequired).getValue();
    compareIsotopePattern = parameters.getParameter(JoinAlignerParameters.compareIsotopePattern)
        .getValue();
    final ParameterSet isoParam = parameters.getParameter(
        JoinAlignerParameters.compareIsotopePattern).getEmbeddedParameters();

    if (compareIsotopePattern) {
      minIsotopeScore = isoParam.getValue(
          IsotopePatternScoreParameters.isotopePatternScoreThreshold);
      isotopeNoiseLevel = isoParam.getValue(IsotopePatternScoreParameters.isotopeNoiseLevel);
      isotopeMZTolerance = isoParam.getValue(IsotopePatternScoreParameters.mzTolerance);
    } else {
      minIsotopeScore = null;
      isotopeNoiseLevel = null;
      isotopeMZTolerance = null;
    }

    compareSpectraSimilarity = parameters.getParameter(
        JoinAlignerParameters.compareSpectraSimilarity).getValue();

    if (compareSpectraSimilarity) {
      simFunction = parameters.getParameter(JoinAlignerParameters.compareSpectraSimilarity)
          .getEmbeddedParameters()
          .getParameter(JoinAlignerSpectraSimilarityScoreParameters.similarityFunction).getValue();
      msLevel = parameters.getParameter(JoinAlignerParameters.compareSpectraSimilarity)
          .getEmbeddedParameters().getParameter(JoinAlignerSpectraSimilarityScoreParameters.msLevel)
          .getValue();
    }
  }

  @Override
  public String getTaskDescription() {
    return "Join aligner, " + featureListName + " (" + featureLists.size() + " feature lists)";
  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0f;
    }
    return alignedFeatureList != null ? (alignedFeatureList.getNumberOfRows() + alignedRows.get())
                                        / (double) totalRows : 0d;
  }

  @Override
  public void run() {

    if ((mzWeight == 0) && (rtWeight == 0)) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Cannot run alignment, all the weight parameters are zero");
      return;
    }

    setStatus(TaskStatus.PROCESSING);
    logger.info(
        () -> "Running parallel join aligner on " + featureLists.size() + " feature lists.");

    // Remember how many rows we need to process. Each row will be processed
    // twice, first for score calculation, second for actual alignment.
    for (FeatureList list : featureLists) {
      totalRows += list.getNumberOfRows();
    }

    // Collect all data files
    final List<RawDataFile> allDataFiles = FeatureListUtils.getAllDataFiles(featureLists);
    if (allDataFiles == null) {
      return;
    }

    // Create a new aligned feature list based on the baseList and renumber IDs
    alignedFeatureList = new ModularFeatureList(featureListName, getMemoryMapStorage(),
        allDataFiles);
    FeatureListUtils.transferRowTypes(alignedFeatureList, featureLists);
    FeatureListUtils.transferSelectedScans(alignedFeatureList, featureLists);
    final AtomicInteger newRowID = new AtomicInteger(1);

    // list all rows for each feature list
    final List<List<FeatureListRow>> allRows = new ArrayList<>(featureLists.size());

    for (var flist : featureLists) {
      allRows.add(new ArrayList<>(flist.getRows()));
    }

    final FeatureListRowSorter rowsMzAscending = new FeatureListRowSorter(SortingProperty.MZ,
        SortingDirection.Ascending);

    // still contains rows from unaligned feature lists
    while (!allRows.isEmpty()) {
      // sort remaining unaligned rows by size (feature list with highest number of unaligned rows)
      allRows.sort((a, b) -> Integer.compare(b.size(), a.size())); // reverse sort

      // remove next feature list's rows
      // select the next base feature list with max number of rows
      final List<FeatureListRow> nextUnalignedFeatureList = allRows.remove(0);
      if (nextUnalignedFeatureList.isEmpty()) {
        logger.finer(() -> String.format(
            "End of join aligner reached. %d feature lists had no unaligned rows left",
            allRows.size()));
        break; // end loop
      }

      // create new rows, used as base for next alignment iteration, and later added to feature list
      List<FeatureListRow> nextBaseRows = new ArrayList<>(nextUnalignedFeatureList.size());
      for (var unalignedRow : nextUnalignedFeatureList) {
        nextBaseRows.add(new ModularFeatureListRow(alignedFeatureList, newRowID.getAndIncrement(),
            (ModularFeatureListRow) unalignedRow, true));
      }
      nextBaseRows.sort(rowsMzAscending);

      // align all remaining feature lists onto the feature list with max(row number) = nextBaseRows
      if (!allRows.isEmpty()) {
        alignRowsOnBaseRows(allRows, nextBaseRows);
      }

      // add all new base rows
      for (var row : nextBaseRows) {
        alignedFeatureList.addRow(row);
      }
      iteration++;
    }

    // sort by RT and reset IDs
    FeatureListUtils.sortByDefaultRT(alignedFeatureList, true);

    // update row bindings
    alignedFeatureList.parallelStream().filter(row -> row.getNumberOfFeatures() > 1)
        .forEach(FeatureListRow::applyRowBindings);

    alignedFeatureList.getAppliedMethods().addAll(featureLists.get(0).getAppliedMethods());
    // Add task description to peakList
    alignedFeatureList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Join aligner", JoinAlignerModule.class, parameters,
            getModuleCallDate()));
    // Add new aligned feature list to the project {
    project.addFeatureList(alignedFeatureList);

    if (parameters.getValue(JoinAlignerParameters.handleOriginal)
        == OriginalFeatureListOption.REMOVE) {
      project.removeFeatureLists(featureLists);
    }

    logger.info("Finished join aligner");

    setStatus(TaskStatus.FINISHED);

  }

  /**
   * all unaligned rows are checked against the list of base rows
   *
   * @param unalignedRows FeatureList<Rows>
   * @param baseRowsByMz  list of base rows sorted by acsending mz
   */
  private void alignRowsOnBaseRows(List<List<FeatureListRow>> unalignedRows,
      List<FeatureListRow> baseRowsByMz) {

    // key = a row to be aligned, value = all possible matches in the aligned fl and it's scores
    final ConcurrentLinkedDeque<RowVsRowScore> scoresList = new ConcurrentLinkedDeque<>();

    // stream all rows in all feature lists
    unalignedRows.stream().flatMap(Collection::stream).parallel().forEach(row -> {
      if (isCanceled()) {
        return;
      }

      final Range<Double> mzRange =
          mzWeight > 0 ? mzTolerance.getToleranceRange(row.getAverageMZ()) : Range.all();
      final Range<Float> rtRange =
          rtWeight > 0 ? rtTolerance.getToleranceRange(row.getAverageRT()) : Range.all();
      final Range<Float> mobilityRange =
          compareMobility && row.getAverageMobility() != null ? mobilityTolerance.getToleranceRange(
              row.getAverageMobility()) : Range.singleton(0f);

      // find all rows in the aligned rows that might match
      final List<FeatureListRow> candidatesInAligned = FeatureListUtils.getRows(baseRowsByMz,
          rtRange, mzRange, true);

      if (candidatesInAligned.isEmpty()) {
        return;
      }

      // calculate score for unaligned row against all candidates
      for (FeatureListRow candidateInAligned : candidatesInAligned) {
        // retention time and m/z is already checked for candidates
        if (checkMobility(candidateInAligned, mobilityRange) //
            && (!sameChargeRequired || FeatureUtils.compareChargeState(row, candidateInAligned)) //
            && (!sameIDRequired || FeatureUtils.compareIdentities(row, candidateInAligned))
            && checkIsotopePattern(row, candidateInAligned) //
            && checkSpectralSimilarity(row, candidateInAligned)) {

          final RowVsRowScore score;
          if (!compareMobility) {
            score = new RowVsRowScore(row, candidateInAligned, RangeUtils.rangeLength(mzRange) / 2.0, mzWeight,
                RangeUtils.rangeLength(rtRange) / 2.0, rtWeight);
          } else {
            score = new RowVsRowScore(row, candidateInAligned, RangeUtils.rangeLength(mzRange) / 2.0, mzWeight,
                RangeUtils.rangeLength(rtRange) / 2.0, rtWeight,
                RangeUtils.rangeLength(mobilityRange), mobilityWeight);
          }
          scoresList.add(score);
        }
      }
    });

    // after an iteration, rows of all other featureLists have been given a mapping
    // now we have to find the best match
    final RowVsRowScore[] scores = scoresList.stream().sorted().toArray(RowVsRowScore[]::new);
    // track all aligned rows - only align to highest scoring row
    final var alignedRowsMap = addFeaturesBasedOnScores(scores);

    // keep track of unaligned rows for the next interation.
    removeAlignedRows(unalignedRows, alignedRowsMap);
  }

  @NotNull
  private Object2BooleanOpenHashMap<FeatureListRow> addFeaturesBasedOnScores(
      RowVsRowScore[] scores) {
    final Object2BooleanOpenHashMap<FeatureListRow> alignedRowsMap = new Object2BooleanOpenHashMap<>(
        scores.length);

    for (RowVsRowScore score : scores) {
      final FeatureListRow alignedRow = score.getAlignedBaseRow();
      final FeatureListRow row = score.getRowToAdd();
      if (!alignedRowsMap.getOrDefault(row, false)) {
        // no row was aligned
        // put all features of the row into the aligned row
        for (Feature feature : row.getFeatures()) {
          final RawDataFile dataFile = feature.getRawDataFile();
          if (!alignedRow.hasFeature(dataFile)) {
            alignedRow.addFeature(dataFile, new ModularFeature(alignedFeatureList, feature), false);
            alignedRowsMap.put(row, true);
            this.alignedRows.getAndIncrement();
          }
        }
      }
    }

    return alignedRowsMap;
  }

  /**
   * Remove all rows that were algined in this step. Modifies the argument list
   *
   * @param allRows        FeatureList<List<Rows>>
   * @param alignedRowsMap marks all aligned rows
   */
  private void removeAlignedRows(List<List<FeatureListRow>> allRows,
      Object2BooleanOpenHashMap<FeatureListRow> alignedRowsMap) {
    AtomicInteger alignedCounter = new AtomicInteger(0);
    AtomicInteger remainingCounter = new AtomicInteger(0);
    final ListIterator<List<FeatureListRow>> iterator = allRows.listIterator();
    while (iterator.hasNext()) {
      // remove aligned rows
      final List<FeatureListRow> featureList = iterator.next();
      featureList.removeIf(row -> {
        if (alignedRowsMap.getOrDefault(row, false)) {
          alignedCounter.incrementAndGet();
          return true;
        }
        remainingCounter.incrementAndGet();
        return false;
      });
      // remove empty lists
      if (featureList.isEmpty()) {
        iterator.remove();
      }
    }
    logger.finest(() -> String.format("Rows: %d aligned; %d remaining. Iteration %d/%d (max)",
        alignedCounter.get(), remainingCounter.get(), iteration, featureLists.size()));
  }

  private boolean checkSpectralSimilarity(FeatureListRow row, FeatureListRow candidate) {
    // compare the similarity of spectra mass lists on MS1 or
    // MS2 level
    if (compareSpectraSimilarity) {
      DataPoint[] rowDPs = null;
      DataPoint[] candidateDPs = null;
      SpectralSimilarity sim;

      // get data points of mass list of the representative
      // scans
      if (msLevel == 1) {
        rowDPs = row.getBestFeature().getRepresentativeScan().getMassList().getDataPoints();
        candidateDPs = candidate.getBestFeature().getRepresentativeScan().getMassList()
            .getDataPoints();
      }

      // get data points of mass list of the best
      // fragmentation scans
      if (msLevel == 2) {
        if (row.getMostIntenseFragmentScan() != null
            && candidate.getMostIntenseFragmentScan() != null) {
          rowDPs = row.getMostIntenseFragmentScan().getMassList().getDataPoints();
          candidateDPs = candidate.getMostIntenseFragmentScan().getMassList().getDataPoints();
        } else {
          return false;
        }
      }

      // compare mass list data points of selected scans
      if (rowDPs != null && candidateDPs != null) {

        // calculate similarity using SimilarityFunction
        sim = createSimilarity(rowDPs, candidateDPs);

        // check if similarity is null. Similarity is not
        // null if similarity score is >= the
        // user set threshold
        if (sim == null) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean checkIsotopePattern(FeatureListRow row, FeatureListRow candidate) {
    if (compareIsotopePattern) {
      IsotopePattern ip1 = row.getBestIsotopePattern();
      IsotopePattern ip2 = candidate.getBestIsotopePattern();

      return (ip1 == null) || (ip2 == null) || IsotopePatternScoreCalculator.checkMatch(ip1, ip2,
          isotopeMZTolerance, isotopeNoiseLevel, minIsotopeScore);
    }
    return true;
  }

  //
  //  private boolean checkMZ(FeatureListRow candidate, Range<Double> mzRange) {
  //    return mzWeight <= 0 || mzRange.contains(candidate.getAverageMZ());
  //  }
  //
  //  private boolean checkRT(FeatureListRow candidate, Range<Float> rtRange) {
  //    return rtWeight <= 0 || candidate.getAverageRT() < 0 || rtRange.contains(
  //        candidate.getAverageRT());
  //  }

  private boolean checkMobility(FeatureListRow candidate, Range<Float> mobilityRange) {
    return !compareMobility || mobilityWeight <= 0 || candidate.getAverageMobility() == null
           || mobilityRange.contains(candidate.getAverageMobility());
  }

  /**
   * Uses the similarity function and filter to create similarity.
   *
   * @return positive match with similarity or null if criteria was not met
   */
  private SpectralSimilarity createSimilarity(DataPoint[] library, DataPoint[] query) {
    return simFunction.getModule()
        .getSimilarity(simFunction.getParameterSet(), mzTolerance, 0, library, query);
  }
}
