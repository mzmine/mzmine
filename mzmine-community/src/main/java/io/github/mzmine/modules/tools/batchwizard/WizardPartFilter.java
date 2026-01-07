/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * A filter to restrict wizard workflow selections.
 */
public interface WizardPartFilter {

  /**
   * @param allowed The allowed steps
   */
  static WizardPartFilter allow(Collection<WizardParameterFactory> allowed) {
    final HashSet<WizardParameterFactory> set = new HashSet<>(allowed);
    return set::contains;
  }

  /**
   * @param allowed The allowed steps
   */
  static WizardPartFilter allow(WizardParameterFactory... allowed) {
    return allow(Arrays.asList(allowed));
  }

  /**
   * @param denied The denied steps
   */
  static WizardPartFilter deny(Collection<WizardParameterFactory> denied) {
    final HashSet<WizardParameterFactory> set = new HashSet<>(denied);
    return part -> !set.contains(part);
  }

  /**
   * @param denied The denied steps
   */
  static WizardPartFilter deny(WizardParameterFactory... denied) {
    return deny(Arrays.stream(denied).collect(Collectors.toSet()));
  }

  static WizardPartFilter combine(WizardPartFilter... filters) {
    return part -> Arrays.stream(filters).allMatch(filter -> filter.accept(part));
  }

  /**
   * @param part The wizard parameter factory to check if it is allowed.
   * @return true or false
   */
  boolean accept(@NotNull WizardParameterFactory part);
}
