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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.learnermodule;

import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.MSLevel;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ModuleSubCategory;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatusListener;

/**
 * New modules need to implement DataPointProcessingModules. To make them show up in the list of
 * addable modules, they have to be added in the MZmineModulesList.java
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DPPLearnerModule implements DataPointProcessingModule {

  @Override
  public String getName() {
    return "Learner module";
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {

    return DPPLearnerModuleParameters.class;
  }

  @Override
  public DataPointProcessingTask createTask(MassSpectrum spectrum, ParameterSet parameterSet,
      SpectraPlot plot, DataPointProcessingController controller, TaskStatusListener listener) {

    return new DPPLearnerModuleTask(spectrum, plot, parameterSet, controller, listener);
  }

  /**
   * Additional module categories can be added in {@link ModuleSubCategory}. The module is
   * classified by this value and listed accordingly in the tree view of the ProcessingComponent.
   */
  @Override
  public ModuleSubCategory getModuleSubCategory() {
    return ModuleSubCategory.IDENTIFICATION;
  }

  @Override
  public MSLevel getApplicableMSLevel() {
    return MSLevel.MSANY;
  }
}
