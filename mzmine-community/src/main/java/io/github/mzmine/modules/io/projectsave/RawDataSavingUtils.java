/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.io.projectsave;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RawDataSavingUtils {

  private static final Logger logger = Logger.getLogger(RawDataSavingUtils.class.getName());

  /**
   * @param files The raw data files to create a batch queue for.
   * @return A single batch queue to process all files in the same order.
   */
  public static BatchQueue makeBatchQueue(List<RawDataFile> files) {
    // get all applied methods
    final List<FeatureListAppliedMethod> appliedMethods = files.stream()
        .flatMap(file -> file.getAppliedMethods().stream())
        .sorted(Comparator.comparing(FeatureListAppliedMethod::getModuleCallDate)).toList();

    // group applied methods by date
    final Map<Instant, List<FeatureListAppliedMethod>> methodMap = new TreeMap<>();
    for (FeatureListAppliedMethod method : appliedMethods) {
      final List<FeatureListAppliedMethod> value = methodMap.computeIfAbsent(
          method.getModuleCallDate(), d -> new ArrayList<>());
      value.add(method);
    }
    logger.finest(
        () -> "Detected " + methodMap.size() + " individual module calls of raw data methods.");

    final BatchQueue queue = new BatchQueue();
    for (final List<FeatureListAppliedMethod> methodList : methodMap.values()) {
      final MZmineModule module = methodList.get(0).getModule();
      if (!(module instanceof MZmineProcessingModule procModule)) {
        logger.warning(() -> "Cannot add module " + module.getName()
            + " to raw file batch queue, because it is not an MZmineProcessingModule."
            + " This could lead to problems on project import.");
        continue;
      }

      // add a new queue step, replace raw file parameters to SPECIFIC
      queue.add(new MZmineProcessingStepImpl<>(procModule,
          RawDataSavingUtils.replaceAndMergeFileAndRawParameters(
              methodList.stream().map(FeatureListAppliedMethod::getParameters).toList(),
              procModule)));
      logger.finest(() -> "Added module " + module.getName() + " to raw file batch queue.");
    }

    return queue;
  }

  /**
   * Groups all queues by their mergability. Note that this list might still contain queues with
   * equal steps.
   *
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
          if (!RawDataSavingUtils.queuesEqual(originalQueue, mergableQueue, true, true, true)) {
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
   * @see RawDataSavingUtils#queuesEqual(BatchQueue, BatchQueue, boolean, boolean, boolean)
   */
  @Nullable
  public static BatchQueue mergeQueues(BatchQueue q1, BatchQueue q2, boolean mergeSubsets) {
    if (!queuesEqual(q1, q2, mergeSubsets, mergeSubsets, mergeSubsets)) {
      return null;
    }
    // newest version
    final BatchQueue mergedQueue = new BatchQueue();
    var longerQueue = (q1.size() > q2.size()) ? q1 : q2;
    var shorterQueue = (q1.size() < q2.size()) ? q1 : q2;
    for (int i = 0; i < shorterQueue.size(); i++) {
      final var step1 = q1.get(i);
      final var step2 = q2.get(i);

      final var parameterSet1 = step1.getParameterSet();
      final var parameterSet2 = step2.getParameterSet();

      final ParameterSet mergedParameterSet = replaceAndMergeFileAndRawParameters(parameterSet1,
          parameterSet2, step1.getModule());

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
   * @see this#replaceAndMergeFileAndRawParameters(ParameterSet, ParameterSet,
   * MZmineProcessingModule)
   */
  public static ParameterSet replaceAndMergeFileAndRawParameters(
      Collection<ParameterSet> parameterSets, MZmineProcessingModule module) {
    final Iterator<ParameterSet> iterator = parameterSets.iterator();
    ParameterSet merged = iterator.next();

    if (parameterSets.size() == 1) {
      // dirty hack to replace the raw file selection if we only have one parameter set
      return replaceAndMergeFileAndRawParameters(merged, merged, module);
    }

    while (iterator.hasNext()) {
      merged = replaceAndMergeFileAndRawParameters(merged, iterator.next(), module);
    }

    return merged;
  }

  /**
   * Combines the contents of {@link RawDataFilesParameter} and {@link FileNamesParameter} for the
   * given parameter sets. Files will not be duplicated if their {@link Object#hashCode()} method
   * returns the same value. The {@link RawDataFilesSelectionType} of the
   * {@link RawDataFilesParameter} will be set to {@link RawDataFilesSelectionType#SPECIFIC_FILES}.
   *
   * @param parameterSet1 The first parameter set.
   * @param parameterSet2 The second parameter set.
   * @return The merged parameter set.
   */
  @NotNull
  public static ParameterSet replaceAndMergeFileAndRawParameters(
      @NotNull final ParameterSet parameterSet1, @NotNull final ParameterSet parameterSet2,
      MZmineProcessingModule module) {

    if (!ParameterUtils.equalValues(parameterSet1, parameterSet2, true, true)) {
      throw new IllegalArgumentException("Parameter sets differ in more than raw/file parameters.");
    }

    final var mergedParameterSet = parameterSet1.cloneParameterSet(true);

    for (int j = 0; j < mergedParameterSet.getParameters().length; j++) {
      final Parameter<?> param1 = parameterSet1.getParameters()[j];
      final Parameter<?> param2 = parameterSet2.getParameters()[j];
      final Parameter<?> mergedParam = mergedParameterSet.getParameters()[j];

      // merge file names and selected raw data files
      if (mergedParam instanceof FileNamesParameter fnp) {
        Set<File> files = new LinkedHashSet<>(); // set so we don't have to bother with duplicates
        if (module.getModuleCategory() == MZmineModuleCategory.RAWDATAIMPORT) {
          // check if the files still exist in the project
          files.addAll(Arrays.stream(((FileNamesParameter) param1).getValue()).filter(
              f -> new RawDataFilePlaceholder(f.getName(), f.getAbsolutePath()).getMatchingFile()
                  != null).toList());
          files.addAll(Arrays.stream(((FileNamesParameter) param2).getValue()).filter(
              f -> new RawDataFilePlaceholder(f.getName(), f.getAbsolutePath()).getMatchingFile()
                  != null).toList());
        } else {
          Collections.addAll(files, ((FileNamesParameter) param1).getValue());
          Collections.addAll(files, ((FileNamesParameter) param2).getValue());
        }
        logger.finest(() -> "Combined FileNamesParameter to " + Arrays.toString(files.toArray()));
        fnp.setValue(files.toArray(new File[0]));
      } else if (mergedParam instanceof RawDataFilesParameter rfp
          && param1 instanceof RawDataFilesParameter rfp1
          && param2 instanceof RawDataFilesParameter rfp2) {
        final Set<RawDataFile> files = new LinkedHashSet<>(); // set so we don't have to bother with duplicates
        if (rfp1.getValue().getSelectionType() == RawDataFilesSelectionType.SPECIFIC_FILES) {
          files.addAll(getRemainingProjectFiles(rfp1.getValue().getSpecificFilesPlaceholders()));
        } else {
          files.addAll(getRemainingProjectFiles(rfp1.getValue().getEvaluationResult()));
        }
        if (rfp2.getValue().getSelectionType() == RawDataFilesSelectionType.SPECIFIC_FILES) {
          files.addAll(getRemainingProjectFiles(rfp2.getValue().getSpecificFilesPlaceholders()));
        } else {
          files.addAll(getRemainingProjectFiles(rfp2.getValue().getEvaluationResult()));
        }
        logger.finest(
            () -> "Combined RawDataFilesParameter to " + Arrays.toString(files.toArray()));
        rfp.setValue(RawDataFilesSelectionType.SPECIFIC_FILES, files.toArray(new RawDataFile[0]));
      }
    }
    return mergedParameterSet;
  }

  @NotNull
  private static List<RawDataFile> getRemainingProjectFiles(RawDataFilePlaceholder[] files) {
    return Arrays.stream(files).<RawDataFile>mapMulti((ph, c) -> {
      final RawDataFile file = ph.getMatchingFile();
      if (file != null) {
        c.accept(file);
      }
    }).toList();
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
      logger.finest("Modules " + step1.getModule().getClass().getName() + " is not equal to "
          + step2.getModule().getClass().getName());
      return false;
    }

    final var parameterSet1 = step1.getParameterSet();
    final var parameterSet2 = step2.getParameterSet();

    if (!ParameterUtils.equalValues(parameterSet1, parameterSet2, skipFileParameters,
        skipRawDataFileParameters)) {
      logger.finest(
          "Queues are not equal. Parameter sets of step " + step1.getModule() + " are not equal.");
      return false;
    }
    return true;
  }

}
