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

package io.github.mzmine.parameters;

import io.github.mzmine.parameters.parametertypes.EmbeddedParameter;
import io.github.mzmine.parameters.parametertypes.EmbeddedParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ParameterUtils {

  private static final Logger logger = Logger.getLogger(ParameterUtils.class.getName());

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
}
