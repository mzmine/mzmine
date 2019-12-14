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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.identification.sumformulaprediction;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingTask;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.MSLevel;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ModuleSubCategory;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.TaskStatusListener;

public class DPPSumFormulaPredictionModule
        implements DataPointProcessingModule {

    @Override
    public String getName() {
        return "Sum formula prediction";
    }

    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {
        return DPPSumFormulaPredictionParameters.class;
    }

    @Override
    public ModuleSubCategory getModuleSubCategory() {
        return ModuleSubCategory.IDENTIFICATION;
    }

    @Override
    public DataPointProcessingTask createTask(DataPoint[] dataPoints,
            ParameterSet parameterSet, SpectraPlot plot,
            DataPointProcessingController controller,
            TaskStatusListener listener) {
        return new DPPSumFormulaPredictionTask(dataPoints, plot, parameterSet,
                controller, listener);
    }

    @Override
    public MSLevel getApplicableMSLevel() {
        return MSLevel.MSANY;
    }
}
