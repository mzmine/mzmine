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

package io.github.mzmine.modules.impl;

import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * MZmine processing step implementation
 */
public class MZmineProcessingStepImpl<ModuleType extends MZmineModule> implements
    MZmineProcessingStep<ModuleType> {

  private final ModuleType module;
  private final ParameterSet parameters;

  public MZmineProcessingStepImpl(ModuleType module, ParameterSet parameters) {
    this.module = module;
    this.parameters = parameters;
  }

  public @NotNull ModuleType getModule() {
    return module;
  }

  public @NotNull ParameterSet getParameterSet() {
    return parameters;
  }

  @Override
  public String toString() {
    return module.getName();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getModule(), parameters.getClass());
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof MZmineProcessingStep that)) {
      return false;
    }

    return getModule().equals(that.getModule()) && ParameterUtils.equalValues(parameters,
        that.getParameterSet(), false, false);
  }
}
