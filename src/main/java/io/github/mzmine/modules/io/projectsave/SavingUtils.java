package io.github.mzmine.modules.io.projectsave;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class SavingUtils {

  private static final Logger logger = Logger.getLogger(SavingUtils.class.getName());

  public static boolean importStepsEqual(MZmineProcessingStep<?> a, MZmineProcessingStep<?> b) {
    if (a.getModule() != b.getModule()) {
      return false;
    }

    if (!a.getParameterSet().getClass().equals(b.getParameterSet().getClass())) {
      return false;
    }

    if (a.getParameterSet().getParameters().length != b.getParameterSet().getParameters().length) {
      return false;
    }

    Parameter<?>[] parameters = a.getParameterSet().getParameters();
    boolean[] found = new boolean[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      Parameter<?> pa = a.getParameterSet().getParameters()[i];
      Parameter<?> pb = b.getParameterSet().getParameters()[i];

      if (!pa.getName().equals(pb.getName())) {
        return false;
      }

      if (pa instanceof FileNamesParameter filesA && pb instanceof FileNamesParameter filesB) {
        if (!(filesA.getValue().length == filesB.getValue().length)) {
          return false;
        }

        for (int j = 0; j < filesA.getValue().length; j++) {
          if (!filesA.getValue()[j].getAbsolutePath()
              .equals(filesB.getValue()[j].getAbsolutePath())) {
            return false;
          }
        }
      }

      if (pa instanceof AdvancedSpectraImportParameters aa
          && pb instanceof AdvancedSpectraImportParameters ab) {
        if (aa.getParameter(AdvancedSpectraImportParameters.msMassDetection).getValue() == ab
            .getParameter(AdvancedSpectraImportParameters.msMassDetection).getValue()) {
          return false;
        }

        if (aa.getParameter(AdvancedSpectraImportParameters.ms2MassDetection).getValue() == ab
            .getParameter(AdvancedSpectraImportParameters.ms2MassDetection).getValue()) {
          return false;
        }
        // it's unlikely it was different module calls now...
      }
    }
    return true;
  }

  @Nullable
  public static BatchQueue mergeQueues(BatchQueue q1, BatchQueue q2) {
    if (!queuesEqual(q1, q2)) {
      return null;
    }

    final BatchQueue mergedQueue = new BatchQueue();
    for (int i = 0; i < q1.size(); i++) {
      var step1 = q1.get(i);
      var step2 = q2.get(i);

      final var parameterSet1 = step1.getParameterSet();
      final var parameterSet2 = step2.getParameterSet();

      final var mergedParameterSet = parameterSet1.cloneParameterSet();

      for (int j = 0; j < mergedParameterSet.getParameters().length; j++) {
        final Parameter<?> param1 = parameterSet1.getParameters()[j];
        final Parameter<?> param2 = parameterSet2.getParameters()[j];
        final Parameter<?> mergedParam = mergedParameterSet.getParameters()[j];

        if (mergedParam instanceof FileNamesParameter fnp) {
          List<File> files = new ArrayList<>();
          Collections.addAll(files, ((FileNamesParameter) param1).getValue());
          Collections.addAll(files, ((FileNamesParameter) param2).getValue());
          logger.finest(() -> "Combined FileNamesParameter to " + Arrays.toString(files.toArray()));
          fnp.setValue(files.toArray(new File[0]));
        } else if (mergedParam instanceof RawDataFilesParameter rfp) {
          List<RawDataFile> files = new ArrayList<>();
          Collections
              .addAll(files, ((RawDataFilesParameter) param1).getValue().getMatchingRawDataFiles());
          Collections
              .addAll(files, ((RawDataFilesParameter) param2).getValue().getMatchingRawDataFiles());
          logger.finest(() -> "Combined RawDataFilesParameter to " + Arrays.toString(files.toArray()));
          rfp.setValue(RawDataFilesSelectionType.SPECIFIC_FILES, files.toArray(new RawDataFile[0]));
        }
      }

      mergedQueue.add(new MZmineProcessingStepImpl<>(q1.get(i).getModule(), mergedParameterSet));
    }

    return mergedQueue;
  }

  public static boolean queuesEqual(BatchQueue q1, BatchQueue q2) {
    if (!queueModulesEqual(q1, q2)) {
      return false;
    }

    for (int i = 0; i < q1.size(); i++) {
      var step1 = q1.get(i);
      var step2 = q2.get(i);

      final var parameterSet1 = step1.getParameterSet();
      final var parameterSet2 = step2.getParameterSet();

      if (!parameterSetsEqual(parameterSet1, parameterSet2, true)) {
        logger.finest("Queues are not equal. Parameter sets of step " + i + " are not equal.");
        return false;
      }
    }

    return true;
  }

  /**
   * @param skipRawAndFileParameters If true, values of {@link RawDataFilesParameter}s and {@link
   *                                 FileNamesParameter}s will be skipped.
   */
  private static boolean parameterSetsEqual(ParameterSet parameterSet1, ParameterSet parameterSet2,
      boolean skipRawAndFileParameters) {
    if (parameterSet1 == null || parameterSet2 == null || parameterSet1.getClass() != parameterSet2
        .getClass()) {
      logger.info(() -> "Cannot compare parameters. Either null or not the same class.");
      return true;
    }

    for (int j = 0; j < parameterSet1.getParameters().length; j++) {
      final Parameter<?> param1 = parameterSet1.getParameters()[j];
      final Parameter<?> param2 = parameterSet2.getParameters()[j];

      if (param1.getClass() != param2.getClass()) {
        logger.finest(
            () -> "Parameters " + param1.getName() + "(" + param1.getClass().getName() + ") and "
                + param2.getName() + " (" + param2.getClass().getName()
                + ") are not of the same class.");
        return true;
      }

      if ((param1 instanceof FileNamesParameter || param1 instanceof RawDataFilesParameter)
          && skipRawAndFileParameters) {
        // it does not matter if the file or raw data selection was different, we need to know
        // if the other values were the same if we want to merge the steps.
        logger.finest(
            () -> "Skipping parameter " + param1.getName() + " of class " + param1.getClass()
                .getName() + ".");
        continue;
      }

      if (param1 instanceof EmbeddedParameterSet embedded1
          && param2 instanceof EmbeddedParameterSet embedded2) {
        return parameterSetsEqual(embedded1.getEmbeddedParameters(),
            embedded2.getEmbeddedParameters(), skipRawAndFileParameters);
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

  /**
   * @return True if the Modules used in both queues are equal.
   */
  private static boolean queueModulesEqual(BatchQueue q1, BatchQueue q2) {
    if (q1.size() != q2.size()) {
      return false;
    }

    for (int i = 0; i < q1.size(); i++) {
      if (!q1.get(i).getModule().equals(q2.get(i).getModule())) {
        logger.finest(
            "Modules " + q1.get(i).getModule().getClass().getName() + " is not equal to " + q2
                .get(i).getModule().getClass().getName());
        return false;
      }
    }

    return true;
  }
}
