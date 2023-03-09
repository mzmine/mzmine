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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterContainer;
import io.github.mzmine.parameters.ParameterSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * In case a parameter embeds a parameter set, this interface shall be implemented. This is required
 * because embedded parameter sets might have a
 * {@link io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter} which needs to
 * be set. With this interface, it can be done via
 * {@link io.github.mzmine.modules.batchmode.BatchTask}
 */
public interface EmbeddedParameterSet<T extends ParameterSet, V> extends Parameter<V>,
    ParameterContainer {

  T getEmbeddedParameters();

  @Override
  default void setSkipSensitiveParameters(boolean skipSensitiveParameters) {
    getEmbeddedParameters().setSkipSensitiveParameters(skipSensitiveParameters);
  }

  /**
   * Checks all embedded parameter values. If errors occur, a new line is added for the parent
   * parameter and all parameters
   *
   * @param errorMessages all errors as lines, headed by the parent parameter
   * @return true if all values are set correctly
   */
  default boolean checkEmbeddedValues(final Collection<String> errorMessages) {
    List<String> newMessages = new ArrayList<>();
    boolean success = getEmbeddedParameters().checkParameterValues(newMessages);
    if (!newMessages.isEmpty()) {
      String name = (this instanceof Parameter<?> p) ? p.getName() : "";
      errorMessages.add(name + ":");
      errorMessages.addAll(newMessages);
    }
    return success;
  }

}
