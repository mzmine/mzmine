package net.sf.mzmine.modules.datapointprocessing;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

/**
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public interface DataPointProcessingModule extends MZmineModule {
  
  public DataPointProcessingTask createTask(DataPoint[] dataPoints, SpectraPlot plot, TaskStatusListener listener);
  
  // TODO: runModule() is not needed for this. what do we do with it?
}
