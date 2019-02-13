package net.sf.mzmine.modules.datapointprocessing;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

public interface DataPointProcessingModule extends MZmineProcessingModule {
  
//  public DataPointProcessingTask createTask(DataPoint[] dataPoints, SpectraPlot plot, DataPointProcessingListener listener);
  public DataPointProcessingTask createTask(DataPoint[] dataPoints, SpectraPlot plot, TaskStatusListener listener);
  
  // TODO: runModule() is not needed for this. what do we do with it?
}
