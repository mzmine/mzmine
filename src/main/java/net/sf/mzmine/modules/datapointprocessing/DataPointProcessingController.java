package net.sf.mzmine.modules.datapointprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModulesList;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

/**
 * This class will control the tasks to process the DataPoints in a SpectraWindow. Every SpectraPlot
 * is meant to have an instance of this class associated with it.
 * 
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DataPointProcessingController {

  private Logger logger = Logger.getLogger(DataPointProcessingController.class.getName());
  
  PlotModuleCombo pmc;
  DataPoint[] dataPoints;
  ProcessedDataPoint[] results;

  public enum ControllerStatus {
    WAITING, PROCESSING, ERROR, CANCELED, FINISHED
  };

  ControllerStatus status;

  /*
   * DataPointProcessingController() { }
   * 
   * DataPointProcessingController(PlotModuleCombo pmc) { setPlotModuleCombo(pmc); }
   */

  DataPointProcessingController(List<Class<DataPointProcessingModule>> moduleList, SpectraPlot plot,
      DataPoint[] dataPoints) {
    pmc = new PlotModuleCombo(moduleList, plot);
    setdataPoints(dataPoints);
    setStatus(ControllerStatus.WAITING);
  }

  public void setPlotModuleCombo(PlotModuleCombo pmc) {
    this.pmc = pmc;
  }

  public PlotModuleCombo getPlotModuleCombo() {
    if (isPlotModuleComboSet())
      return this.pmc;
    return new PlotModuleCombo();
  }

  public DataPoint[] getDataPoints() {
    return dataPoints;
  }

  public void setdataPoints(DataPoint[] dataPoints) {
    this.dataPoints = dataPoints;
  }

  public ProcessedDataPoint[] getResults() {
    return results;
  }

  public void setResults(ProcessedDataPoint[] results) {
    this.results = results;
  }

  public ControllerStatus getStatus() {
    return status;
  }

  public void setStatus(ControllerStatus status) {
    this.status = status;
  }

  public boolean isPlotModuleComboSet() {
    return (pmc != null);
  }

  /**
   * This will execute the modules associated with the plot. It will start with the first one and
   * execute the following ones afterwards automatically. This method is called by the public method
   * execute().
   * 
   * @param dp
   * @param module
   * @param plot
   */
  private void execute(DataPoint[] dp, Class<DataPointProcessingModule> module, SpectraPlot plot) {
    if (!isPlotModuleComboSet()) {
      logger.warning("execute called, without pmc being set.");
      return;
    }
    MZmineProcessingModule inst = MZmineCore.getModuleInstance(module);

    if (inst instanceof DataPointProcessingModule) {

      Task t = ((DataPointProcessingModule) inst).createTask(dp, plot, new TaskStatusListener() {
        @Override
        public void taskStatusChanged(Task task, TaskStatus newStatus, TaskStatus oldStatus) {
          if (!(task instanceof DataPointProcessingTask))
            return; // TODO: Throw exception?
          if (newStatus == TaskStatus.FINISHED) {
            if (pmc.hasNextModule(module)) {
              Class<DataPointProcessingModule> next = pmc.getNextModule(module);
              // pass results to next task
              ProcessedDataPoint[] results = ((DataPointProcessingTask) task).getResults();
              execute(results, next, plot);
            } else {
              // TODO: finish and display
              setResults(((DataPointProcessingTask) task).getResults());

              setStatus(ControllerStatus.FINISHED);
            }
          } else if (newStatus == TaskStatus.PROCESSING) {
            setStatus(ControllerStatus.PROCESSING);
          } else if (newStatus == TaskStatus.WAITING) {
            // should set to waiting here?
          } else if (newStatus == TaskStatus.ERROR) {
            setStatus(ControllerStatus.ERROR);
          } else if (newStatus == TaskStatus.CANCELED) {
            setStatus(ControllerStatus.CANCELED);
          }
        }
      });

      MZmineCore.getTaskController().addTask(t);
    }
  }

  /**
   * Executes the modules in the PlotModuleCombo to the plot with the given DataPoints.
   */
  public void execute() {
    if (!isPlotModuleComboSet())
      return;

    Class<DataPointProcessingModule> first = pmc.getFirstModule();
    if (first == null)
      return;

    setStatus(ControllerStatus.PROCESSING);
    logger.finest("Executing DataPointProcessingTasks.");
    execute(getDataPoints(), first, pmc.getPlot());
  }
}
