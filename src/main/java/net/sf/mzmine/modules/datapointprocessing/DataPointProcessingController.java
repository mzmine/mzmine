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

package net.sf.mzmine.modules.datapointprocessing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jfree.data.xy.XYDataset;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.datapointprocessing.datamodel.PlotModuleCombo;
import net.sf.mzmine.modules.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.modules.datapointprocessing.datamodel.results.DPPIsotopePatternResult;
import net.sf.mzmine.modules.datapointprocessing.datamodel.results.DPPResult.ResultType;
import net.sf.mzmine.modules.datapointprocessing.datamodel.results.DPPResultsDataSet;
import net.sf.mzmine.modules.datapointprocessing.datamodel.results.DPPResultsLabelGenerator;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datasets.IsotopesDataSet;
import net.sf.mzmine.parameters.ParameterSet;
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
  List<DPPControllerStatusListener> listener;
  DataPointProcessingTask currentTask;

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


  /*
   * DataPointProcessingController() { }
   * 
   * DataPointProcessingController(PlotModuleCombo pmc) { setPlotModuleCombo(pmc); }
   */

  public DataPointProcessingController(DataPointProcessingQueue steps, SpectraPlot plot,
      DataPoint[] dataPoints) {

    pmc = new PlotModuleCombo(steps, plot);
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

  /**
   * 
   * @return Results of this task. Might be null, make sure to check the status first!
   */
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
      for (DPPControllerStatusListener l : listener)
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
    getCurrentTask().setStatus(TaskStatus.CANCELED);
  }

  public boolean isPlotModuleComboSet() {
    return (pmc != null);
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
  private void execute(DataPoint[] dp, MZmineProcessingStep<DataPointProcessingModule> step,
      SpectraPlot plot) {
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

    if (step.getModule() instanceof DataPointProcessingModule) {

      DataPointProcessingModule inst = step.getModule();
      ParameterSet parameters = step.getParameterSet();

      Task t = ((DataPointProcessingModule) inst).createTask(dp, parameters, plot, this,
          new TaskStatusListener() {
            @Override
            public void taskStatusChanged(Task task, TaskStatus newStatus, TaskStatus oldStatus) {
              if (!(task instanceof DataPointProcessingTask)) {
                // TODO: Throw exception?
                logger.warning("This should have been a DataPointProcessingTask.");
                return;
              }
              logger.info("Task status changed to " + newStatus.toString());
              switch (newStatus) {
                case FINISHED:
                  if (pmc.hasNextStep(step)) {
                    if (DataPointProcessingManager.getInst()
                        .isRunning(((DataPointProcessingTask) task).getController())) {

                      MZmineProcessingStep<DataPointProcessingModule> next = pmc.getNextStep(step);

                      // pass results to next task and start recursively
                      ProcessedDataPoint[] result = ((DataPointProcessingTask) task).getResults();
                      execute(result, next, plot);
                    } else {
                      logger.warning(
                          "This controller was already removed from the running list, although it had not finished processing. Exiting");
                      break;
                    }
                  } else {
                    setResults(((DataPointProcessingTask) task).getResults());
                    setStatus(ControllerStatus.FINISHED);

                    displayResults(getResults(), plot);

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

      setCurrentTask((DataPointProcessingTask) t); // maybe we need this some time
      logger.info("Start processing of " + t.getClass().getName());
      MZmineCore.getTaskController().addTask(t);
    }
  }

  /**
   * Displays the results in the plot.
   * 
   * @param results
   * @param plot
   */
  private void displayResults(ProcessedDataPoint[] results, SpectraPlot plot) {
    // add full data set
    DPPResultsLabelGenerator labelGen = new DPPResultsLabelGenerator(plot);
    plot.addDataSet(new DPPResultsDataSet("Results", getResults()), Color.MAGENTA, false, labelGen);

    // add all detected isotope patterns as a single dataset
    // IsotopesDataSet isotopes = compressIsotopeDataSets(results);
    // if(isotopes != null)
    // plot.addDataSet(isotopes, Color.GREEN, false, labelGen);

    // make sure we dont have overlapping labels
    clearOtherLabelGenerators(plot, DPPResultsDataSet.class);

    // now add detected isotope patterns

    int i = 0;
    for (ProcessedDataPoint result : results)
      if (result.resultTypeExists(ResultType.ISOTOPEPATTERN))
        i++;
    if(i == 0) i = 1;
    
    int j = 0;
    for (ProcessedDataPoint result : results)
      if (result.resultTypeExists(ResultType.ISOTOPEPATTERN)) {
        plot.addDataSet(
            new IsotopesDataSet(
                (IsotopePattern) result.getFirstResultByType(ResultType.ISOTOPEPATTERN).getValue()),
            getColor((double)j/(double)i), false);
        j++;
      }
  }

  public Color getColor(double power)
  {
      double H = power; // Hue (note 0.4 = Green, see huge chart below)
      double S = 0.9; // Saturation
      double B = 0.9; // Brightness

      return Color.getHSBColor((float)H, (float)S, (float)B);
  }
  
  /**
   * Executes the modules in the PlotModuleCombo to the plot with the given DataPoints. This will be
   * called by the DataPointProcessingManager. This starts the first module, which recursively
   * starts the following ones after finishing.
   */
  public void execute() {
    if (!isPlotModuleComboSet())
      return;

    MZmineProcessingStep<DataPointProcessingModule> first = pmc.getFirstStep();
    if (first == null)
      return;

    setStatus(ControllerStatus.PROCESSING);
    logger.finest("Executing DataPointProcessingTasks.");
    execute(getDataPoints(), first, pmc.getPlot());
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
   * Removes all label generators of datasets that are not of the given type.
   * 
   * @param plot Plot to apply this method to.
   * @param ignore Class object of the instances to ignore.
   */
  public void clearOtherLabelGenerators(SpectraPlot plot, Class<? extends XYDataset> ignore) {
    for (int i = 0; i < plot.getXYPlot().getDatasetCount(); i++) {
      XYDataset dataset = plot.getXYPlot().getDataset(i);
      // check if object of dataset is an instance of ignore.class
      if (!(ignore.isInstance(dataset)))
        plot.getXYPlot().getRendererForDataset(dataset).setDefaultItemLabelGenerator(null);
    }
  }

  /**
   * This method generates a single IsotopesDataSet from all detected isotope patterns in the
   * results.
   * 
   * @param dataPoints
   * @return
   */
  private IsotopesDataSet compressIsotopeDataSets(ProcessedDataPoint[] dataPoints) {
    List<IsotopePattern> list = new ArrayList<>();

    for (ProcessedDataPoint dp : dataPoints) {
      if (dp.resultTypeExists(ResultType.ISOTOPEPATTERN)) {
        list.add(((DPPIsotopePatternResult) dp.getFirstResultByType(ResultType.ISOTOPEPATTERN))
            .getValue());
      }
    }
    if (list.isEmpty())
      return null;

    List<DataPoint> dpList = new ArrayList<>();

    for (IsotopePattern pattern : list) {
      for (DataPoint dp : pattern.getDataPoints())
        dpList.add(dp);
    }
    if (dpList.isEmpty())
      return null;

    IsotopePattern full = new SimpleIsotopePattern(dpList.toArray(new DataPoint[0]),
        IsotopePatternStatus.DETECTED, "Isotope patterns");
    return new IsotopesDataSet(full);
  }
}
