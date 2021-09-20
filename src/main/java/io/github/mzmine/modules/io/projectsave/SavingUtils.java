package io.github.mzmine.modules.io.projectsave;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class SavingUtils {

  private static final Logger logger = Logger.getLogger(SavingUtils.class.getName());

  /**
   * Groups all queues by their mergability. Note that this list might still contain queues with
   * equal steps.
   *
   * @param originalQueues
   * @return The grouped queues.
   */
  public static List<List<BatchQueue>> groupQueuesByMergability(List<BatchQueue> originalQueues) {
    List<List<BatchQueue>> mergableQueuesList = new ArrayList<>();
    // find queues that are equal (same module calls and same parameters)
    for (BatchQueue originalQueue : originalQueues) {
      boolean match = false;
      for (List<BatchQueue> mergableQueues : mergableQueuesList) {
        boolean found = true;
        for (BatchQueue mergableQueue : mergableQueues) {
          if (!SavingUtils.queuesEqual(originalQueue, mergableQueue, true, true, true)) {
            found = false;
          }
        }
        if (found) {
          match = true;
          mergableQueues.add(originalQueue);
        }
      }

      if (!match) {
        List<BatchQueue> entry = new ArrayList<>();
        entry.add(originalQueue);
        mergableQueuesList.add(entry);
      }
    }
    return mergableQueuesList;
  }

  /**
   * @return The merged queue or null if queues are not equal.
   * @see SavingUtils#queuesEqual(BatchQueue, BatchQueue, boolean, boolean, boolean)
   */
  @Nullable
  public static BatchQueue mergeQueues(BatchQueue q1, BatchQueue q2, boolean mergeSubsets) {
    if (!queuesEqual(q1, q2, mergeSubsets, mergeSubsets, mergeSubsets)) {
      return null;
    }

    final BatchQueue mergedQueue = new BatchQueue();
    var longerQueue = (q1.size() > q2.size()) ? q1 : q2;
    var shorterQueue = (q1.size() < q2.size()) ? q1 : q2;
    for (int i = 0; i < shorterQueue.size(); i++) {
      var step1 = q1.get(i);
      var step2 = q2.get(i);

      final var parameterSet1 = step1.getParameterSet();
      final var parameterSet2 = step2.getParameterSet();

      final var mergedParameterSet = parameterSet1.cloneParameterSet();

      for (int j = 0; j < mergedParameterSet.getParameters().length; j++) {
        final Parameter<?> param1 = parameterSet1.getParameters()[j];
        final Parameter<?> param2 = parameterSet2.getParameters()[j];
        final Parameter<?> mergedParam = mergedParameterSet.getParameters()[j];

        // merge file names and selected raw data files
        if (mergedParam instanceof FileNamesParameter fnp) {
          Set<File> files = new LinkedHashSet<>(); // set so we don't have to bother with duplicates
          Collections.addAll(files, ((FileNamesParameter) param1).getValue());
          Collections.addAll(files, ((FileNamesParameter) param2).getValue());
          logger.finest(() -> "Combined FileNamesParameter to " + Arrays.toString(files.toArray()));
          fnp.setValue(files.toArray(new File[0]));
        } else if (mergedParam instanceof RawDataFilesParameter rfp
            && param1 instanceof RawDataFilesParameter rfp1
            && param2 instanceof RawDataFilesParameter rfp2) {
          final Set<RawDataFile> files = new LinkedHashSet<>(); // set so we don't have to bother with duplicates
          if (rfp1.getValue().getSelectionType() == RawDataFilesSelectionType.SPECIFIC_FILES) {
            Collections.addAll(files, rfp1.getValue().getSpecificFilesPlaceholders());
          } else {
            Arrays.stream(rfp1.getValue().getEvaluationResult()).map(RawDataFilePlaceholder::new)
                .forEach(files::add);
          }
          if (rfp2.getValue().getSelectionType() == RawDataFilesSelectionType.SPECIFIC_FILES) {
            Collections.addAll(files, rfp2.getValue().getSpecificFilesPlaceholders());
          } else {
            Arrays.stream(rfp2.getValue().getEvaluationResult()).map(RawDataFilePlaceholder::new)
                .forEach(files::add);
          }
          logger.finest(
              () -> "Combined RawDataFilesParameter to " + Arrays.toString(files.toArray()));
          rfp.setValue(RawDataFilesSelectionType.SPECIFIC_FILES, files.toArray(new RawDataFile[0]));
        }
      }

      mergedQueue.add(new MZmineProcessingStepImpl<>(q1.get(i).getModule(), mergedParameterSet));
    }

    // append steps of longer batch queues
    for (int j = shorterQueue.size(); j < longerQueue.size(); j++) {
      mergedQueue.add(new MZmineProcessingStepImpl<>(longerQueue.get(j).getModule(),
          longerQueue.get(j).getParameterSet().cloneParameterSet(true)));
    }
    return mergedQueue;
  }

  /**
   * Merges batch queues consisting of the same module calls with the same parameters.
   *
   * @return The merged batch queues.
   */
  public static List<BatchQueue> mergeBatchQueues(Map<RawDataFile, BatchQueue> rawDataSteps) {

    final List<BatchQueue> originalQueues = rawDataSteps.values().stream().toList();
    List<List<BatchQueue>> mergableQueuesList = SavingUtils
        .groupQueuesByMergability(originalQueues);

    // merge equal module calls
    List<BatchQueue> mergedBatchQueues = new ArrayList<>();
    for (List<BatchQueue> value : mergableQueuesList) {
      // if we just have one queue, add id directly.
      if (value.size() == 1) {
        mergedBatchQueues.add(value.get(0));
        continue;
      }

      Iterator<BatchQueue> iterator = value.iterator();
      BatchQueue merged = iterator.next();
      while (iterator.hasNext()) {
        merged = SavingUtils.mergeQueues(merged, iterator.next(), true);
      }
      mergedBatchQueues.add(merged);
    }

    logger.finest(
        () -> "Created " + mergedBatchQueues.size() + " batch queues for " + rawDataSteps.size()
            + " files.");
    return mergedBatchQueues;
  }

  public static List<BatchQueue> removeDuplicateSteps(List<BatchQueue> qs) {
    List<BatchQueue> queues = new ArrayList<>(qs);
    queues.sort((l1, l2) -> Integer.compare(l2.size(), l1.size())); // sort descending

    logger.finest(() -> "Removing duplicate steps from " + qs.size() + " batch queues.");
    AtomicInteger removedSteps = new AtomicInteger(0);

    for (int i = 0; i < queues.size(); i++) {
      final BatchQueue base = queues.get(i);
      for (int j = 1; j < queues.size(); j++) {
        if (i == j) {
          continue;
        }
        final BatchQueue potentialDuplicate = queues.get(j);

        AtomicBoolean differentStepFound = new AtomicBoolean(false);
        var remove = potentialDuplicate
            .stream().filter(potentialDuplicateStep -> {
              boolean found = false;
              for (int k = potentialDuplicate.indexOf(potentialDuplicateStep);
                  k < base.size() && !found; k++) {
                MZmineProcessingStep<MZmineProcessingModule> baseStep = base.get(k);
                // only remove steps from the beginning of the batch queue.
                if (processingStepEquals(potentialDuplicateStep, baseStep, false, false)
                    && !differentStepFound.get()) {
                  removedSteps.getAndIncrement();
                  found = true;
                  return true;
                } else {
                  differentStepFound.set(true);
                }
              }
              return false;
            }).toList();
        potentialDuplicate.removeAll(remove);
      }
    }

    logger.finest(() -> "Removed " + removedSteps.get() + " duplicate steps ");
    return queues;
  }

  /**
   * Compares two batch queues.
   *
   * @param q1                        A batch queue.
   * @param q2                        Another batch queue.
   * @param skipFileParameters        If true, contents of {@link FileNamesParameter} are not
   *                                  compared.
   * @param skipRawDataFileParameters If true, contents of {@link RawDataFilesParameter} are not
   *                                  compared.
   * @param allowSubsets              If true, the queues may be of different length, as long as the
   *                                  longer queue only appends additional steps.
   * @return true or false.
   */
  public static boolean queuesEqual(BatchQueue q1, BatchQueue q2, boolean skipFileParameters,
      boolean skipRawDataFileParameters, boolean allowSubsets) {
    if (q1.size() != q2.size() && !allowSubsets) {
      return false;
    }

    for (int i = 0; i < q1.size() && i < q2.size(); i++) {
      if (!processingStepEquals(q1.get(i), q2.get(i), skipFileParameters,
          skipRawDataFileParameters)) {
        return false;
      }
    }

    return true;
  }

  private static boolean processingStepEquals(MZmineProcessingStep<?> step1,
      MZmineProcessingStep<?> step2, boolean skipFileParameters,
      boolean skipRawDataFileParameters) {

    if (!step1.getModule().equals(step2.getModule())) {
      logger.finest(
          "Modules " + step1.getModule().getClass().getName() + " is not equal to " + step2
              .getModule().getClass().getName());
      return false;
    }

    final var parameterSet1 = step1.getParameterSet();
    final var parameterSet2 = step2.getParameterSet();

    if (!parameterSetsEqual(parameterSet1, parameterSet2, skipFileParameters,
        skipRawDataFileParameters)) {
      logger.finest(
          "Queues are not equal. Parameter sets of step " + step1.getModule() + " are not equal.");
      return false;
    }
    return true;
  }

  /**
   * @param skipFileParameters        If true, contents of {@link FileNamesParameter} are not
   *                                  compared.
   * @param skipRawDataFileParameters If true, values of {@link RawDataFilesParameter}s and {@link
   *                                  FileNamesParameter}s will be skipped.
   */
  public static boolean parameterSetsEqual(ParameterSet parameterSet1, ParameterSet parameterSet2,
      boolean skipFileParameters, boolean skipRawDataFileParameters) {
    if (parameterSet1 == null || parameterSet2 == null || parameterSet1.getClass() != parameterSet2
        .getClass()) {
      logger.info(() -> "Cannot compare parameters. Either null or not the same class.");
      return false;
    }

    if (parameterSet1.getParameters().length != parameterSet2.getParameters().length) {
      return false;
    }

    for (int j = 0;
        j < parameterSet1.getParameters().length && j < parameterSet2.getParameters().length; j++) {
      final Parameter<?> param1 = parameterSet1.getParameters()[j];
      final Parameter<?> param2 = parameterSet2.getParameters()[j];

      if (param1.getClass() != param2.getClass()) {
        logger.finest(
            () -> "Parameters " + param1.getName() + "(" + param1.getClass().getName() + ") and "
                + param2.getName() + " (" + param2.getClass().getName()
                + ") are not of the same class.");
        return false;
      }

      if ((param1 instanceof FileNamesParameter && skipFileParameters)
          || (param1 instanceof RawDataFilesParameter) && skipRawDataFileParameters) {
        // it does not matter if the file or raw data selection was different, we need to know
        // if the other values were the same if we want to merge the steps.
        logger.finest(
            () -> "Skipping parameter " + param1.getName() + " of class " + param1.getClass()
                .getName() + ".");
        continue;
      }

      if (param1 instanceof EmbeddedParameterSet embedded1
          && param2 instanceof EmbeddedParameterSet embedded2 && !parameterSetsEqual(
          embedded1.getEmbeddedParameters(), embedded2.getEmbeddedParameters(), skipFileParameters,
          skipRawDataFileParameters)) {
        return false;
      }

      if (!param1.valueEquals(param2)) {
        logger.finest(
            () -> "Parameter \"" + param1.getName() + "\" of parameter set " + parameterSet1
                .getClass().getName() + " has different values: " + param1.getValue() + " and "
                + param2.getValue());
        return false;
      }
    }
    return true;
  }
}
