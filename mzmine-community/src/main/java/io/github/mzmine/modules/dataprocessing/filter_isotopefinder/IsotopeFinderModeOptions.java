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

package io.github.mzmine.modules.dataprocessing.filter_isotopefinder;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnum;
import io.github.mzmine.parameters.parametertypes.submodules.ValueWithParameters;
import io.github.mzmine.taskcontrol.Task;
import java.time.Instant;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Selectable isotope finder algorithms. Each maps to an {@link IsotopeFinderAlgorithmModule} that
 * carries the full setup of the detection run as its embedded parameters, so the top-level
 * {@link IsotopeFinderParameters} only has the feature lists and the algorithm choice.
 * <p>
 * Both options run the same carbon-averagine detection; they only differ in how much of its setup
 * they expose.
 */
public enum IsotopeFinderModeOptions implements ModuleOptionsEnum<IsotopeFinderAlgorithmModule> {

  /**
   * Simplified setup: m/z tolerance, 13C requirement, and maximum charge only. Default.
   */
  AUTOMATIC,
  /**
   * Full carbon-averagine setup: estimates the carbon count from m/z, no formula prediction.
   */
  CARBON_AVERAGINE;

  @Override
  public Class<? extends IsotopeFinderAlgorithmModule> getModuleClass() {
    return switch (this) {
      case AUTOMATIC -> AutomaticIsotopeFinderModule.class;
      case CARBON_AVERAGINE -> CarbonAveragineAlgorithmModule.class;
    };
  }

  @Override
  public String toString() {
    return switch (this) {
      case AUTOMATIC -> "Automatic";
      case CARBON_AVERAGINE -> "Carbon-averagine";
    };
  }

  @Override
  public String getStableId() {
    // do not change these values for save/load
    return switch (this) {
      case AUTOMATIC -> "automatic";
      // kept from the former "Signal based (carbon-averagine)" mode so a saved selection still resolves
      case CARBON_AVERAGINE -> "signal_based";
    };
  }

  /**
   * @param value          selected algorithm with its embedded parameters.
   * @param project        the current project.
   * @param featureLists   the feature lists to process.
   * @param topParameters  the top-level {@link IsotopeFinderParameters}, stored as applied method.
   * @param moduleCallDate the module call date of the applied method.
   * @return the tasks of the selected algorithm.
   */
  public static @NotNull List<Task> createTasks(
      @NotNull final ValueWithParameters<IsotopeFinderModeOptions> value,
      @NotNull final MZmineProject project, @NotNull final ModularFeatureList[] featureLists,
      @NotNull final ParameterSet topParameters, @NotNull final Instant moduleCallDate) {
    return value.value().getModuleInstance()
        .createTasks(project, featureLists, value.parameters(), topParameters, moduleCallDate);
  }
}
