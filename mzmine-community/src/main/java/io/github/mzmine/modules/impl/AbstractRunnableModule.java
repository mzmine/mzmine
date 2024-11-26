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

package io.github.mzmine.modules.impl;

import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.mzio.mzmine.datamodel.parameters.ParameterSet;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the default behavior of a {@link MZmineRunnableModule}. Extending classes may also
 * implement {@link MZmineProcessingModule}
 */
public abstract class AbstractRunnableModule extends AbstractMZmineModule implements
    MZmineRunnableModule {

  protected final @NotNull String description;
  protected final @NotNull MZmineModuleCategory moduleCategory;

  /**
   * @param name              name of the module in the menu and quick access
   * @param parameterSetClass the class of the parameters
   * @param moduleCategory    module category for quick access and batch mode
   * @param description       the description of the task
   */
  public AbstractRunnableModule(@NotNull final String name,
      @NotNull final Class<? extends ParameterSet> parameterSetClass,
      @NotNull final MZmineModuleCategory moduleCategory, @NotNull final String description) {
    super(name, parameterSetClass);
    this.description = description;
    this.moduleCategory = moduleCategory;
  }


  @Override
  public @NotNull String getDescription() {
    return description;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return moduleCategory;
  }

}
