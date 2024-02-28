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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.TaskStatusListener;

/**
 * This class will control the tasks to process the DataPoints in a SpectraWindow. Every SpectraPlot
 * is meant to have an instance of this class associated with it.
 *
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class DataPointProcessingController {

  private Logger logger = Logger.getLogger(DataPointProcessingController.class.getName());

  private MassSpectrum spectrum;
  private ProcessedDataPoint[] results;
  private List<DPPControllerStatusListener> listener;
  private DataPointProcessingTask currentTask;
  private MZmineProcessingStep<DataPointProcessingModule> currentStep;
  private DataPointProcessingQueue queue;
  private SpectraPlot plot;

  public enum ControllerStatus {
    WAITING, PROCESSING, ERROR, CANCELED, FINISHED
  };

  /**
   * This is used to cancel the execution of this controller. It is set to NORMAL in the
   * constructor.
   */
  public enum ForcedControllerStatus {
    NORMAL, CANCEL
  };

  ControllerStatus status;
  ForcedControllerStatus forcedStatus;

  public DataPointProcessingController(DataPointProcessingQueue steps, SpectraPlot plot,
      MassSpectrum spectrum) {

    setQueue(steps);
    setPlot(plot);
    setdataPoints(spectrum);
    setStatus(ControllerStatus.WAITING);
    setForcedStatus(ForcedControllerStatus.NORMAL);
  }

  public DataPointProcessingQueue getQueue() {
    return queue;
  }

  public SpectraPlot getPlot() {
    return plot;
  }

  private void setQueue(DataPointProcessingQueue queue) {
    this.queue = queue;
  }

  private void setPlot(SpectraPlot plot) {
    this.plot = plot;
  }

  /**
   *
   * @return The original data points this controller started execution with.
   */
  public MassSpectrum getDataPoints() {
    return spectrum;
  }

  private void setdataPoints(MassSpectrum spectrum) {
    this.spectrum = spectrum;
  }

  /**
   *
   * @return Results of this task. Might be null, make sure to check the status first!
   */
  public ProcessedDataPoint[] getResults() {
    return results;
  }

  private void setResults(ProcessedDataPoint[] results) {
    this.results = results;
  }

  public DataPointProcessingTask getCurrentTask() {
    return currentTask;
  }

  private void setCurrentTask(DataPointProcessingTask currentTask) {
    this.currentTask = currentTask;
  }

  private MZmineProcessingStep<DataPointProcessingModule> getCurrentStep() {
    return this.currentStep;
  }

  private void setCurrentStep(MZmineProcessingStep<DataPointProcessingModule> step) {
    this.currentStep = step;
  }

  public ForcedControllerStatus getForcedStatus() {
    return forcedStatus;
  }

  private void setForcedStatus(ForcedControllerStatus forcedStatus) {
    this.forcedStatus = forcedStatus;
  }

  /**
   * Convenience method to cancel the execution of this controller. The manager will listen to this
   * change by its DPControllerStatusListener. The ControllerStatus is changed in the execute()
   * method of this controller.
   */
  public void cancelTasks() {
    setForcedStatus(ForcedControllerStatus.CANCEL);
    if (getCurrentTask() != null)
      getCurrentTask().setStatus(TaskStatus.CANCELED);
  }

  /**
   * This will execute the modules associated with the plot. It will start with the first one and
   * execute the following ones afterwards automatically. This method is called by the public method
   * execute(). The status listener in this method starts the next task recursively after the
   * previous one has finished.
   *
   * @param dp
   * @param module
   * @param plot
   */
  private void execute(MassSpectrum spectrum, MZmineProcessingStep<DataPointProcessingModule> step,
      SpectraPlot plot) {
    if (queue == null || queue.isEmpty() || plot == null) {
      logger.warning("execute called, without queue or plot being set.");
      setStatus(ControllerStatus.FINISHED);
      return;
    }

    if (getForcedStatus() == ForcedControllerStatus.CANCEL
        || getStatus() == ControllerStatus.CANCELED) {
      setResults(ProcessedDataPoint.convert(null)); // dp ));
      logger
          .finest("Canceled controller, not starting new tasks. Results are set to latest array.");
      setStatus(ControllerStatus.CANCELED);
      return;
    }

    List<String> err = new ArrayList<>();
    if (!step.getParameterSet().checkParameterValues(err)) {
      setResults(ProcessedDataPoint.convert(null)); // dp));
      setStatus(ControllerStatus.CANCELED);
      logger.warning(step.getModule().getName() + " - Not all parameters set."
          + Arrays.toString(err.toArray(new String[0])));
      return;
    }

    if (step.getModule() instanceof DataPointProcessingModule) {

      DataPointProcessingModule inst = step.getModule();
      ParameterSet parameters = step.getParameterSet();

      Task t = inst.createTask(spectrum, parameters, plot, this, new TaskStatusListener() {
        @Override
        public void taskStatusChanged(Task task, TaskStatus newStatus, TaskStatus oldStatus) {
          if (!(task instanceof DataPointProcessingTask)) {
            // TODO: Throw exception?
            logger.warning("This should have been a DataPointProcessingTask.");
            return;
          }
          // logger.finest("Task status changed to " +
          // newStatus.toString());
          switch (newStatus) {
            case FINISHED:
              if (queue.hasNextStep(step)) {
                if (DataPointProcessingManager.getInst()
                    .isRunning(((DataPointProcessingTask) task).getController())) {

                  MZmineProcessingStep<DataPointProcessingModule> next = queue.getNextStep(step);

                  // pass results to next task and start
                  // recursively
                  MassSpectrum result = null; // ((DataPointProcessingTask) task).getResults();
                  ((DataPointProcessingTask) task).displayResults();

                  execute(result, next, plot);
                } else {
                  logger.warning(
                      "This controller was already removed from the running list, although it "
                          + "had not finished processing. Exiting");
                  break;
                }
              } else {
                setResults(((DataPointProcessingTask) task).getResults());
                ((DataPointProcessingTask) task).displayResults();
                setStatus(ControllerStatus.FINISHED);
              }
              break;
            case PROCESSING:
              setStatus(ControllerStatus.PROCESSING);
              break;
            case WAITING:
              // should we even set to WAITING here?
              break;
            case ERROR:
              setStatus(ControllerStatus.ERROR);
              break;
            case CANCELED:
              setStatus(ControllerStatus.CANCELED);
              break;
          }
        }
      });

      logger.finest("Start processing of " + t.getClass().getName());
      MZmineCore.getTaskController().addTask(t);
      setCurrentTask((DataPointProcessingTask) t); // maybe we need this
                                                   // some time
      setCurrentStep(step);
    }
  }

  /**
   * Executes the modules in the PlotModuleCombo to the plot with the given DataPoints. This will be
   * called by the DataPointProcessingManager. This starts the first module, which recursively
   * starts the following ones after finishing.
   */
  public void execute() {
    if (queue == null || queue.isEmpty() || plot == null) {
      setStatus(ControllerStatus.FINISHED);
      logger.warning("execute called, without queue or plot being set.");
      return;
    }

    MZmineProcessingStep<DataPointProcessingModule> first = queue.getFirstStep();
    if (first == null) {
      setStatus(ControllerStatus.ERROR);
      return;
    }

    setStatus(ControllerStatus.PROCESSING);
    logger.finest("Executing DataPointProcessingTasks.");
    execute(getDataPoints(), first, getPlot());
  }

  public boolean addControllerStatusListener(DPPControllerStatusListener list) {
    if (listener == null)
      listener = new ArrayList<DPPControllerStatusListener>();
    return listener.add(list);
  }

  public boolean removeControllerStatusListener(DPPControllerStatusListener list) {
    if (listener != null)
      return listener.remove(list);
    return false;
  }

  public void clearControllerStatusListeners() {
    if (listener != null)
      listener.clear();
  }

  /**
   *
   * @return True if the current task is the last one, false otherwise.
   */
  public boolean isLastTaskRunning() {
    if (getQueue().indexOf(getCurrentStep()) == getQueue().size() - 1)
      return true;
    return false;
  }

  /**
   *
   * @return The current ControllerStatus.
   */
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
      for (DPPControllerStatusListener l : listener)
        l.statusChanged(this, this.status, old);
  }
}
