/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.learnermodule;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.MSLevel;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ModuleSubCategory;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatusListener;

/**
 * New modules need to implement DataPointProcessingModules. To make them show
 * up in the list of addable modules, they have to be added in the
 * MZmineModulesList.java
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
    public DataPointProcessingTask createTask(DataPoint[] dataPoints,
            ParameterSet parameterSet, SpectraPlot plot,
            DataPointProcessingController controller,
            TaskStatusListener listener) {

        return new DPPLearnerModuleTask(dataPoints, plot, parameterSet,
                controller, listener);
    }

    /**
     * Additional module categories can be added in {@link ModuleSubCategory}.
     * The module is classified by this value and listed accordingly in the tree
     * view of the ProcessingComponent.
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
