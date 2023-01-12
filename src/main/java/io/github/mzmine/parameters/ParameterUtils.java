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

import java.util.Arrays;
import java.util.Map;
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
    for (Parameter p : target.getParameters()) {
      Parameter<?> value = sourceParams.getOrDefault(p.getName(), null);
      if (value != null) {
        try {
          p.setValue(value.getValue());
        } catch (Exception e) {
          logger.warning("Failed copy parameter value from " + p.getName() + ". " + e.getMessage());
        }
      }
    }
  }
}
