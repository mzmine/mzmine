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

package io.github.mzmine.parameters;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameter;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.util.concurrent.CloseableReentrantReadWriteLock;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParameterUtils {

  private static final Logger logger = Logger.getLogger(ParameterUtils.class.getName());

  private static final CloseableReentrantReadWriteLock parameterEditLock = new CloseableReentrantReadWriteLock();


  /**
   * Get the matching feature lists from {@link FeatureListsParameter}.
   *
   * @param parameters the parameters to search in
   * @return the matching featurelist selection
   * @throws IllegalStateException if there are != 1 FeatureListsParameters in the parameters
   */
  @NotNull
  public static FeatureList[] getMatchingFeatureListsFromParameter(
      final @NotNull ParameterSet parameters) throws IllegalStateException {
    // get parameters here only needed to run one task per featureList
    var featureListParameters = parameters.streamForClass(FeatureListsParameter.class).toList();
    int nFlistParams = featureListParameters.size();
    if (featureListParameters.isEmpty()) {
      throw new IllegalStateException(
          "There is no FeatureListsParameter (needs 1) in class " + parameters.getClass()
              .getName());
    }
    if (nFlistParams > 1) {
      throw new IllegalStateException(
          "There are too many (" + nFlistParams + ") FeatureListsParameter in class "
              + parameters.getClass().getName() + ". Can only have 1. Coding error.");
    }
    // exactly one parameter for feature lists found
    return parameters.getValue(featureListParameters.getFirst()).getMatchingFeatureLists();
  }

  /**
   * Get the matching raw data files from {@link RawDataFilesParameter}.
   *
   * @param parameters the parameters to search in
   * @return the matching RawDataFile selection
   * @throws IllegalStateException if there are != 1 RawDataFilesParameter in the parameters
   */
  @NotNull
  public static RawDataFile[] getMatchingRawDataFilesFromParameter(
      final @NotNull ParameterSet parameters) throws IllegalStateException {
    // get parameters here only needed to run one task per RawDataFile
    var rawFilesParameter = parameters.streamForClass(RawDataFilesParameter.class).toList();
    int nRawFiles = rawFilesParameter.size();
    if (rawFilesParameter.isEmpty()) {
      throw new IllegalStateException(
          "There is no RawDataFilesParameter (needs 1) in class " + parameters.getClass()
              .getName());
    }
    if (nRawFiles > 1) {
      throw new IllegalStateException(
          "There are too many (" + nRawFiles + ") RawDataFilesParameter in class "
              + parameters.getClass().getName() + ". Can only have 1. Coding error.");
    }
    // exactly one parameter for RawDataFiles found
    return parameters.getValue(rawFilesParameter.getFirst()).getMatchingRawDataFiles();
  }

  /**
   * Attemp to copy parameters by name (this is how its usually done in MZmine). Exceptions because
   * of changed data types etc are caught and logged.
   *
   * @param source source parameters
   * @param target target parameters will receive values from the source
   */
  public static void copyParameters(final ParameterSet source, final ParameterSet target) {
    Map<String, ? extends Parameter<?>> sourceParams = Arrays.stream(source.getParameters())
        .collect(Collectors.toMap(Parameter::getName, key -> key));
    for (Parameter targetParam : target.getParameters()) {
      Parameter<?> sourceParam = sourceParams.getOrDefault(targetParam.getName(), null);
      if (sourceParam != null) {
        copyParameterValue(sourceParam, targetParam);
      }
    }
  }

  /**
   * Set value of source to target. Also apply to nested parameters of {@link EmbeddedParameter} and
   * {@link EmbeddedParameterSet}
   *
   * @param sourceParam source parameter is set to target
   * @param targetParam target parameter
   */
  public static void copyParameterValue(final Parameter sourceParam, final Parameter targetParam) {
    try {
      targetParam.setValue(sourceParam.getValue());
      if (targetParam instanceof EmbeddedParameterSet<?, ?> targetEm
          && sourceParam instanceof EmbeddedParameterSet<?, ?> sourceEm) {
        copyParameters(sourceEm.getEmbeddedParameters(), targetEm.getEmbeddedParameters());
      }
      if (targetParam instanceof EmbeddedParameter<?, ?, ?> targetEm
          && sourceParam instanceof EmbeddedParameter<?, ?, ?> sourceEm) {
        copyParameterValue(sourceEm.getEmbeddedParameter(), targetEm.getEmbeddedParameter());
      }
    } catch (Exception e) {
      logger.warning(
          "Failed copy parameter value from " + targetParam.getName() + ". " + e.getMessage());
    }
  }

  /**
   * Checks if all parameters in a equal in value to b and vice versa. So the parameters need to
   * exactly match in name and value
   *
   * @param a parameters
   * @param b parameters
   * @return true only if all parameters in a and b equal in name and value
   */
  public static boolean equalValues(final ParameterSet a, final ParameterSet b) {
    return equalValues(a, b, true, true);
  }


  /**
   * Checks if all parameters in a equal in value to b and vice versa. So the parameters need to
   * exactly match in name and value
   *
   * @param skipFileParameters        If true, contents of {@link FileNamesParameter} are not
   *                                  compared.
   * @param skipRawDataFileParameters If true, values of {@link RawDataFilesParameter}s and
   *                                  {@link FileNamesParameter}s will be skipped.
   */
  public static boolean equalValues(ParameterSet a, ParameterSet b, boolean skipFileParameters,
      boolean skipRawDataFileParameters) {
    if (a == null || b == null || a.getClass() != b.getClass()) {
      logger.info(() -> "Cannot compare parameters. Either null or not the same class.");
      return false;
    }

    if (a.getParameters().length != b.getParameters().length) {
      return false;
    }

    // order of parameters might be different due to loading from file and change in order in java file
    for (final Parameter<?> param1 : a.getParameters()) {
      try {
        Parameter<?> param2 = b.getParameter(param1);

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
            && param2 instanceof EmbeddedParameterSet embedded2 && !equalValues(
            embedded1.getEmbeddedParameters(), embedded2.getEmbeddedParameters(),
            skipFileParameters, skipRawDataFileParameters)) {
          return false;
        }

        if (!param1.valueEquals(param2)) {
          logger.finest(
              () -> "Parameter \"" + param1.getName() + "\" of parameter set " + a.getClass()
                  .getName() + " has different values: " + param1.getValue() + " and "
                  + param2.getValue());
          return false;
        }

      } catch (Exception ex) {
        // parameter does not exist
        logger.log(Level.WARNING,
            "ParameterSet b does not have all parameters available in a. " + ex.getMessage(), ex);
        return false;
      }
    }

    return true;
  }

  @NotNull
  public static <T> Optional<T> getValueFromAppliedMethods(
      Collection<FeatureListAppliedMethod> appliedMethods,
      Class<? extends ParameterSet> parameterClass, Parameter<T> mzTolParameter) {
    return appliedMethods.stream()
        .filter(appliedMethod -> appliedMethod.getParameters().getClass().equals(parameterClass))
        .findFirst().map(FeatureListAppliedMethod::getParameters)
        .map(parameterSet -> parameterSet.getValue(mzTolParameter));
  }

  /**
   * Replaces a file in the first FileNamesParameter of the parameter set. Synchronized method in
   * case multiple tasks want to edit the same parameter set.
   */
  public static boolean replaceRawFileName(ParameterSet parameterSet, File oldPath, File newPath) {
    try (var lock = parameterEditLock.lockWrite()) {

      for (Parameter<?> parameter : parameterSet.getParameters()) {
        if (!(parameter instanceof FileNamesParameter fnp)) {
          continue;
        }
        final File[] files = fnp.getValue();
        for (int i = 0; i < files.length; i++) {
          File file = files[i];
          if (file.equals(oldPath)) {
            files[i] = newPath;
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Stream all parameters and embedded parameters depth first
   *
   * @param params input parameters
   * @return flat stream of parameters
   */
  public static Stream<Parameter<?>> streamParametersDeep(final ParameterSet params) {
    if (params == null) {
      return Stream.empty();
    }

    return Arrays.stream(params.getParameters()).flatMap(ParameterUtils::streamThisAndEmbeddedDeep);
  }

  /**
   * Stream all parameters and embedded parameters depth first
   *
   * @param params input parameters
   * @return flat stream of parameters of specific type
   */
  public static <T extends Parameter<?>> Stream<T> streamParametersDeep(final ParameterSet params,
      final Class<T> paramClass) {
    return streamParametersDeep(params).filter(paramClass::isInstance).map(paramClass::cast);
  }

  /**
   * Stream this parameter and its embedded parameters depth first
   *
   * @param parameter input parameter
   * @return flat stream of this parameter first followed by any embedded parameters
   */
  public static Stream<Parameter<?>> streamThisAndEmbeddedDeep(final Parameter<?> parameter) {
    if (parameter == null) {
      return Stream.empty();
    }
    Stream<Parameter<?>> paramStream = Stream.of(parameter);
    // search for embedded parameters
    switch (parameter) {
      case EmbeddedParameterSet<?, ?> parent -> {
        ParameterSet embedded = parent.getEmbeddedParameters();
        return Stream.concat(paramStream, streamParametersDeep(embedded));
      }
      // optional?
      case EmbeddedParameter<?, ?, ?> parent -> {
        UserParameter<?, ?> embedded = parent.getEmbeddedParameter();
        return Stream.concat(paramStream, streamThisAndEmbeddedDeep(embedded));
      }
      case HiddenParameter<?> parent -> {
        var embedded = parent.getEmbeddedParameter();
        return Stream.concat(paramStream, streamThisAndEmbeddedDeep(embedded));
      }
      default -> {
      }
    }
    return paramStream;
  }

  /**
   * @param batch A list of {@link MZmineProcessingStep}s, e.g.,
   *              {@link io.github.mzmine.modules.batchmode.BatchQueue} or a pre-filtered batch.
   * @return The most-used export path from the {@link FileNameSuffixExportParameter}s.
   */
  @Nullable
  public static <S extends MZmineProcessingStep<?>, T extends Collection<S>> File extractMajorityExportPath(
      T batch) {
    final List<File> allExportPaths = batch.stream().map(MZmineProcessingStep::getParameterSet)
        .<File>mapMulti((paramSet, c) -> streamParametersDeep(paramSet,
            FileNameSuffixExportParameter.class).forEach(fnp -> {
          if (fnp.getValue() != null) {
            c.accept(fnp.getValue().getParentFile());
          }
        })).filter(Objects::nonNull).toList();

    return FileAndPathUtil.getMajorityFilePath(allExportPaths);
  }

  /**
   * @param batch A list of {@link MZmineProcessingStep}s, e.g.,
   *              {@link io.github.mzmine.modules.batchmode.BatchQueue} or a pre-filtered batch.
   * @return The most-used raw file import path. The batch is searched for modules where the
   * {@link MZmineProcessingModule#getModuleCategory()}  is
   * {@link MZmineModuleCategory#RAWDATAIMPORT} is used.
   */
  public static <M extends MZmineProcessingModule, S extends MZmineProcessingStep<M>, T extends List<S>> File extractMajorityRawFileImportFilePath(
      T batch) {
    final List<File> allImportedFiles = batch.stream()
        .filter(step -> step.getModule().getModuleCategory() == MZmineModuleCategory.RAWDATAIMPORT)
        .map(MZmineProcessingStep::getParameterSet).<File>mapMulti((paramSet, c) -> {
          streamParametersDeep(paramSet, FileNamesParameter.class).flatMap(
                  p -> Arrays.stream(p.getValue() != null ? p.getValue() : new File[0]))
              .filter(Objects::nonNull).forEach(c);
        }).toList();
    return FileAndPathUtil.getMajorityFilePath(
        allImportedFiles.stream().map(File::getParentFile).toList());
  }
}
