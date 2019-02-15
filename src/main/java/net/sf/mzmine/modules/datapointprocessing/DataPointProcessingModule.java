package net.sf.mzmine.modules.datapointprocessing;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

/**
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public interface DataPointProcessingModule extends MZmineModule {
  
  public DataPointProcessingTask createTask(DataPoint[] dataPoints, ParameterSet parameterSet, SpectraPlot plot, DataPointProcessingController controller, TaskStatusListener listener);
  
}
