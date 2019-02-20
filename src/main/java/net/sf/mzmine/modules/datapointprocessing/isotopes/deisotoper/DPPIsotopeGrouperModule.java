/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.datapointprocessing.isotopes.deisotoper;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingController;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingModule;
import net.sf.mzmine.modules.datapointprocessing.DataPointProcessingTask;
import net.sf.mzmine.modules.datapointprocessing.datamodel.ModuleSubCategory;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

public class DPPIsotopeGrouperModule implements DataPointProcessingModule {

  @Override
  public String getName() {
    return "Data point isotope grouper";
  }

  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return DPPIsotopeGrouperParameters.class;
  }

  @Override
  public ModuleSubCategory getModuleSubCategory() {
    return ModuleSubCategory.ISOTOPES;
  }

  @Override
  public DataPointProcessingTask createTask(DataPoint[] dataPoints, ParameterSet parameterSet,
      SpectraPlot plot, DataPointProcessingController controller, TaskStatusListener listener) {
    return new DPPIsotopeGrouperTask(dataPoints, plot, parameterSet, controller, listener);
  }

}
