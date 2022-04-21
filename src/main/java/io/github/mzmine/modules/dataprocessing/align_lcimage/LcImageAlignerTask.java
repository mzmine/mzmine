package io.github.mzmine.modules.dataprocessing.align_lcimage;

import com.google.common.collect.Range;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.ImageType;
import io.github.mzmine.modules.dataprocessing.align_join.RowVsRowScore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LcImageAlignerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(LcImageAlignerTask.class.getName());

  private String description;
  private double totalRows;

  private final double mobWeight;
  private final double mzWeight;
  private final AtomicDouble progress = new AtomicDouble(0);
  private final AtomicInteger scoredRows = new AtomicInteger(0);
  private final AtomicInteger processedScores = new AtomicInteger(0);
  private final AtomicInteger totalScores = new AtomicInteger(1);
  private final ParameterSet parameters;
  private final MZmineProject project;
  private final FeatureList lcFeatureList;
  private final FeatureList imageList;
  private final MZTolerance mzTol;
  private final MobilityTolerance mobTol;

  public LcImageAlignerTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull final ParameterSet parameters, @NotNull MZmineProject project) {
    super(storage, moduleCallDate);

    this.parameters = parameters;
    this.project = project;

    final ModularFeatureList[] matchingFeatureLists = parameters.getValue(
        LcImageAlignerParameters.flists).getMatchingFeatureLists();

    // checked in setup dialog
    imageList = Arrays.stream(matchingFeatureLists)
        .filter(flist -> flist.getFeatureTypes().containsValue(new ImageType())).findAny()
        .orElse(null);
    lcFeatureList = Arrays.stream(matchingFeatureLists)
        .filter(flist -> !flist.getFeatureTypes().containsValue(new ImageType())).findAny()
        .orElse(null);

    totalRows = imageList.getNumberOfRows();
    mzTol = parameters.getValue(LcImageAlignerParameters.mzTolerance);
    mobTol = parameters.getValue(LcImageAlignerParameters.mobTolerance);
    mzWeight = parameters.getValue(LcImageAlignerParameters.mzWeight);
    mobWeight = parameters.getValue(LcImageAlignerParameters.mobilityWeight);
    description =
        "Aligning images from " + imageList.getName() + " on LC feature list " + lcFeatureList.getName();
  }

  @Override
  public String getTaskDescription() {
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    return scoredRows.get() / totalRows * 0.8d
        + processedScores.get() / (double) totalScores.get() * 0.2d;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // align images to all possible base list rows
    final ConcurrentLinkedDeque<RowVsRowScore> scores = new ConcurrentLinkedDeque<>();

    final List<FeatureList> sourceLists = List.of(lcFeatureList, imageList);
    final List<RawDataFile> allDataFiles = FeatureListUtils.getAllDataFiles(sourceLists);
    final ModularFeatureList alignedFlist = new ModularFeatureList(this.lcFeatureList.getName() + " img",
        getMemoryMapStorage(), allDataFiles);
    FeatureListUtils.transferRowTypes(alignedFlist, sourceLists);
    FeatureListUtils.transferSelectedScans(alignedFlist, sourceLists);

    final List<FeatureListRow> lcRows = this.lcFeatureList.getRows().stream().map(
            row -> (FeatureListRow) new ModularFeatureListRow(alignedFlist, (ModularFeatureListRow) row,
                true)).sorted(new FeatureListRowSorter(SortingProperty.MZ, SortingDirection.Ascending))
        .toList();

    // score all rows (parallel)
    imageList.parallelStream().forEach(imageRow -> {
      if (isCanceled()) {
        return;
      }

      final Range<Double> mzRange = mzTol.getToleranceRange(imageRow.getAverageMZ());
      final double maxMzDiff = RangeUtils.rangeLength(mzRange) / 2;
      final Float mobility = imageRow.getAverageMobility();
      final Range<Float> mobRange =
          mobility != null ? mobTol.getToleranceRange(mobility) : Range.all();
      final double maxMobDiff = mobRange.equals(Range.all()) ? Double.POSITIVE_INFINITY
          : RangeUtils.rangeLength(mobRange);

      final List<FeatureListRow> matchingLcRows = FeatureListUtils.getRows(lcRows, Range.all(),
          mzRange, mobRange, true);
      for (FeatureListRow lcRow : matchingLcRows) {
        final RowVsRowScore score = new RowVsRowScore(imageRow, lcRow, maxMzDiff, mzWeight,
            Double.POSITIVE_INFINITY, 0, maxMobDiff, mobWeight);
        scores.add(score);
      }
      scoredRows.getAndIncrement();
    });

    if (isCanceled()) {
      return;
    }

    final RowVsRowScore[] sortedScores = scores.stream().sorted(Comparator.reverseOrder())
        .toArray(RowVsRowScore[]::new);
    totalScores.set(scores.size());
    addFeaturesBasedOnScores(sortedScores, alignedFlist);

    lcRows.forEach(alignedFlist::addRow);

    alignedFlist.getAppliedMethods().addAll(lcFeatureList.getAppliedMethods());
    alignedFlist.getAppliedMethods().addAll(
        new SimpleFeatureListAppliedMethod(LcImageAlignerModule.class, parameters,
            getModuleCallDate()));

    if (isCanceled()) {
      return;
    }

    project.addFeatureList(alignedFlist);

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Copied from the join aligner, with the difference that we don't use the alignedRowsMap here, to
   * keep track of the candidate (=image) rows that we align on the base feature list. Instead, an
   * image feature may be present more than once in the resulting aligned feature list.
   *
   * @param scores Sorted descending 1 -> 0
   */
  @NotNull
  private Object2BooleanOpenHashMap<FeatureListRow> addFeaturesBasedOnScores(RowVsRowScore[] scores,
      ModularFeatureList alignedList) {
    final Object2BooleanOpenHashMap<FeatureListRow> alignedRowsMap = new Object2BooleanOpenHashMap<>(
        scores.length);

    for (RowVsRowScore score : scores) {
      final FeatureListRow alignedRow = score.getAlignedBaseRow(); // lc row = aligned row
      final FeatureListRow row = score.getRowToAdd();
      if (alignedRowsMap.getOrDefault(row, false)) {
        logger.finest(() -> "Will align feature " + row.getID() + " multiple times");
        // in join aligner we would stop here
      }
      for (Feature feature : row.getFeatures()) {
        final RawDataFile dataFile = feature.getRawDataFile();
        if (!alignedRow.hasFeature(
            dataFile)) { // only add the best fit. If there is a fit already, don't align.
          alignedRow.addFeature(dataFile, new ModularFeature(alignedList, feature), false);
          alignedRowsMap.put(row, true);
        }
      }
    }

    return alignedRowsMap;
  }
}
