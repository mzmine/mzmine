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

package io.github.mzmine.modules;

import java.time.Instant;
import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

/**
 * Interface representing a module that can be executed from the GUI through a menu item. This can
 * be either a data processing method (@see MZmineProcessingModule) or a visualization/data analysis
 * method.
 */
public interface MZmineRunnableModule extends MZmineModule {

  /**
   * Returns a brief module description for quick tooltips in the GUI
   * 
   * @return Module description
   */
  @NotNull
  public String getDescription();

  /**
   * Run this module with given parameters. The module may create new Tasks and add them to the
   * 'tasks' collection. The module is not supposed to submit the tasks to the TaskController by
   * itself.
   * 
   * @param project Project to apply this module on.
   * @param parameters ParameterSet to invoke this module with. The ParameterSet has already been
   *        cloned for exclusive use by this module, therefore the module does not need to clone it
   *        again. Upon invocation of the runModule() method it is guaranteed that the ParameterSet
   *        is of the proper class as returned by getParameterSetClass(). Also, it is guaranteed
   *        that the ParameterSet is checked by checkParameters(), therefore the module does not
   *        need to perform these checks again.
   * @param tasks A collection where the module should add its newly created Tasks, if it creates
   *        any.
   * @param moduleCallDate
   * @return Exit code of the operation. ExitCode.OK means the module was started properly, however
   *         it does not guarantee that the Tasks will finish without error. ExitCode.ERROR means
   *         there was a problem starting the module.
   */
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate);

  /**
   * Returns the category of the module (e.g. raw data processing, peak picking etc.). A menu item
   * for this module will be created according to the category.
   */
  @NotNull
  public MZmineModuleCategory getModuleCategory();

}
