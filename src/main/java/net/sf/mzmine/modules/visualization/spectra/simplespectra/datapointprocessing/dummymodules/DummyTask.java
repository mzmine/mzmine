package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.dummymodules;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingController;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingTask;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

public class DummyTask extends DataPointProcessingTask {

  public DummyTask(DataPoint[] dataPoints, SpectraPlot plot, ParameterSet parameterSet,
      DataPointProcessingController controller, TaskStatusListener listener) {
    super(dataPoints, plot, parameterSet, controller, listener);
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setResults((ProcessedDataPoint[])getDataPoints());
    setStatus(TaskStatus.FINISHED);
  }

  @Override
  public void displayResults() {
    // TODO Auto-generated method stub
    
  }

}
