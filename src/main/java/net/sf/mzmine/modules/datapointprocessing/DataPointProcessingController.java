package net.sf.mzmine.modules.datapointprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;

/**
 * This class will control the tasks to process the DataPoints in a SpectraWindow. Every SpectraPlot
 * is meant to have an instance of this class associated with it.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DataPointProcessingController {

  private Logger logger = Logger.getLogger(DataPointProcessingController.class.getName());



  PlotModuleCombo pmc;
  DataPoint[] dataPoints;
  ProcessedDataPoint[] results;
  List<DPControllerStatusListener> listener;
  DataPointProcessingTask currentTask;

  public enum ControllerStatus {
    WAITING, PROCESSING, ERROR, CANCELED, FINISHED
  };

  /**
   * This is used to cancel the execution of this controller. It is set to NORMAL in the constructor. 
   */
  public enum ForcedControllerStatus {
    NORMAL, CANCEL
  };

  ControllerStatus status;
  ForcedControllerStatus forcedStatus;


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
    setForcedStatus(ForcedControllerStatus.NORMAL);
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

  /**
   * Changes the status of this controller and notifies listeners.
   * 
   * @param status New ControllerStatus.
   */
  public void setStatus(ControllerStatus status) {
    ControllerStatus old = this.status;
    this.status = status;

    if (listener != null)
      for (DPControllerStatusListener l : listener)
        l.statusChanged(this, this.status, old);
  }

  public DataPointProcessingTask getCurrentTask() {
    return currentTask;
  }

  private void setCurrentTask(DataPointProcessingTask currentTask) {
    this.currentTask = currentTask;
  }

  public ForcedControllerStatus getForcedStatus() {
    return forcedStatus;
  }

  public void setForcedStatus(ForcedControllerStatus forcedStatus) {
    this.forcedStatus = forcedStatus;
  }

  /**
   * Convenience method to cancel the execution of this controller. The manager will listen to this
   * change by it's DPControllerStatusListener. The ControllerStatus is changed in the execute()
   * method of this controller.
   */
  public void cancelTasks() {
    setForcedStatus(ForcedControllerStatus.CANCEL);
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

    if (getForcedStatus() == ForcedControllerStatus.CANCEL) {
      setResults(ProcessedDataPoint.convert(dp));
      logger
          .finest("Canceled controller, not starting new tasks. Results are set to latest array.");
      setStatus(ControllerStatus.CANCELED);
      return;
    }

    MZmineProcessingModule inst = MZmineCore.getModuleInstance(module);

    if (inst instanceof DataPointProcessingModule) {

      Task t = ((DataPointProcessingModule) inst).createTask(dp, plot, new TaskStatusListener() {
        @Override
        public void taskStatusChanged(Task task, TaskStatus newStatus, TaskStatus oldStatus) {
          if (!(task instanceof DataPointProcessingTask)) {
            // TODO: Throw exception?
            return;
          }
          if (newStatus == TaskStatus.FINISHED) {
            if (pmc.hasNextModule(module)) {
              Class<DataPointProcessingModule> next = pmc.getNextModule(module);

              // pass results to next task and start recursively
              ProcessedDataPoint[] result = ((DataPointProcessingTask) task).getResults();
              execute(result, next, plot);
            } else {
              // TODO: finish and display
              setResults(((DataPointProcessingTask) task).getResults());
              setStatus(ControllerStatus.FINISHED);

              logger.finest("Controller finished.");
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

      setCurrentTask((DataPointProcessingTask) t); // maybe we need this some time
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

  public boolean addControllerStatusListener(DPControllerStatusListener list) {
    if (listener == null)
      listener = new ArrayList<DPControllerStatusListener>();
    return listener.add(list);
  }

  public boolean removeControllerStatusListener(DPControllerStatusListener list) {
    if (listener != null)
      return listener.remove(list);
    return false;
  }

  public void clearControllerStatusListeners() {
    if (listener != null)
      listener.clear();
  }
}
