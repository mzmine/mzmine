/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.filter_baselinecorrection;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialogWithChromatogramPreview;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.R.RSessionWrapper;
import io.github.mzmine.util.R.RSessionWrapperException;
import javafx.application.Platform;
import javafx.scene.control.ProgressBar;

/**
 * @description This class extends ParameterSetupDialogWithChromatogramPreview class. This is used
 *              to preview how the selected baseline correction method and its parameters works over
 *              the raw data file.
 *
 */
public class BaselineCorrectorSetupDialog extends ParameterSetupDialogWithChromatogramPreview {


  // Logger.
  private static final Logger logger =
      Logger.getLogger(ParameterSetupDialogWithChromatogramPreview.class.getName());

  private ParameterSet correctorParameters;
  private BaselineCorrector baselineCorrector;

  private PreviewTask previewTask = null;
  private Thread previewThread = null;

  // Listen to VK_ESCAPE KeyEvent and allow to abort preview computing
  // if input parameters gave a computation task that is about to take
  // forever...
  KeyListener keyListener = new KeyListener() {
    @Override
    public void keyPressed(KeyEvent ke) {

      int keyCode = ke.getKeyCode();
      if (keyCode == KeyEvent.VK_ESCAPE) {

        logger.info("<ESC> Presssed.");
        previewTask.kill();
        hidePreview();

      }
    }

    @Override
    public void keyReleased(KeyEvent ke) {}

    @Override
    public void keyTyped(KeyEvent ke) {}
  };

  public static List<Component> getAllComponents(final Container c) {
    Component[] comps = c.getComponents();
    List<Component> compList = new ArrayList<Component>();
    for (Component comp : comps) {
      compList.add(comp);
      if (comp instanceof Container) {
        compList.addAll(getAllComponents((Container) comp));
      }
    }
    return compList;
  }

  /*
   * private void set_VK_ESCAPE_KeyListener() { // Set VK_ESCAPE KeyEvent listeners List<Component>
   * comps = BaselineCorrectorSetupDialog.getAllComponents(BaselineCorrectorSetupDialog.this); for
   * (Component c : comps) { c.addKeyListener(BaselineCorrectorSetupDialog.this.keyListener); } }
   *
   * private void unset_VK_ESCAPE_KeyListener() { // Remove VK_ESCAPE KeyEvent listeners
   * List<Component> comps =
   * BaselineCorrectorSetupDialog.getAllComponents(BaselineCorrectorSetupDialog.this); for
   * (Component c : comps) { c.removeKeyListener(BaselineCorrectorSetupDialog.this.keyListener); } }
   */

  /**
   *
   * @param correctorParameters Method specific parameters
   * @param correctorClass Chosen corrector to be instantiated
   */
  public BaselineCorrectorSetupDialog(boolean valueCheckRequired, ParameterSet correctorParameters,
      Class<? extends BaselineCorrector> correctorClass) {

    super(valueCheckRequired, correctorParameters);

    this.correctorParameters = correctorParameters;

    try {
      this.baselineCorrector = correctorClass.getConstructor().newInstance();
    } catch (Exception e) {
      e.printStackTrace();
    }

    this.baselineCorrector.collectCommonParameters(null);

    // Default plot type. Initialized according to the chosen chromatogram
    // type.
    this.setPlotType(
        (this.baselineCorrector.getChromatogramType() == ChromatogramType.TIC) ? TICPlotType.TIC
            : TICPlotType.BASEPEAK);

  }

  class PreviewTask extends AbstractTask {

    private String errorMsg;

    private TICPlot ticPlot;
    private RawDataFile dataFile;
    private Range<Double> rtRange, mzRange;
    private BaselineCorrectorSetupDialog dialog;
    private ProgressThread progressThread;

    private RSessionWrapper rSession;
    private boolean userCanceled;

    public PreviewTask(BaselineCorrectorSetupDialog dialog, TICPlot ticPlot, RawDataFile dataFile,
        Range<Double> rtRange, Range<Double> mzRange) {

      this.dialog = dialog;
      this.ticPlot = ticPlot;
      this.dataFile = dataFile;
      this.rtRange = rtRange;
      this.mzRange = mzRange;

      this.userCanceled = false;

      // this.addTaskListener(dialog);
    }

    public RawDataFile getDataFile() {
      return this.dataFile;
    }

    @Override
    public void run() {

      errorMsg = null;

      // Check if the parameter settings are valid
      ArrayList<String> messages = new ArrayList<String>();
      boolean paramsCheck = correctorParameters.checkParameterValues(messages);
      //
      if (!paramsCheck) {

        errorMsg = "Invalid parameter settings for module " + baselineCorrector.getName() + ": "
            + Arrays.toString(messages.toArray());
      } else {

        // Update the status of this task
        updateStatus(TaskStatus.PROCESSING);

        // Get parent module parameters
        baselineCorrector.collectCommonParameters(null);

        // Check R availability, by trying to open the connection
        try {
          String[] reqPackages = baselineCorrector.getRequiredRPackages();
          this.rSession = new RSessionWrapper(baselineCorrector.getRengineType(),
              baselineCorrector.getName(), reqPackages, null);
          this.rSession.open();
        } catch (RSessionWrapperException e) {
          errorMsg = e.getMessage();
          updateStatus(TaskStatus.ERROR);
          return;
        }

        // Set VK_ESCAPE KeyEvent listeners
        // set_VK_ESCAPE_KeyListener();

        // Reset TIC plot
        ticPlot.removeAllTICDataSets();
        ticPlot.setPlotType(getPlotType());

        // Add the original raw data file
        final ScanSelection sel = new ScanSelection(rtRange, 1);
        Scan scans[] = sel.getMatchingScans(dataFile);

        TICDataSet ticDataset = new TICDataSet(dataFile, scans, mzRange, null, getPlotType());
        ticPlot.addTICDataset(ticDataset);

        try {

          // Start progress bar
          baselineCorrector.initProgress(dataFile);
          progressThread = new ProgressThread(this.dialog, this, dataFile);
          progressThread.start();

          // Create a new corrected raw data file
          RawDataFile newDataFile =
              baselineCorrector.correctDatafile(this.rSession, dataFile, correctorParameters, null);

          // If successful, add the new data file
          if (newDataFile != null) {
            scans = sel.getMatchingScans(newDataFile);
            final TICDataSet newDataset =
                new TICDataSet(newDataFile, scans, mzRange, null, getPlotType());
            ticPlot.addTICDataset(newDataset);

            // Show the trend line as well
            XYDataset tlDataset = createBaselineDataset(dataFile, newDataFile, getPlotType());
            ticPlot.addTICDataset(tlDataset);
          }
        } catch (IOException | RSessionWrapperException e) {
          if (!this.userCanceled) {
            errorMsg = "'R computing error' during baseline correction. \n" + e.getMessage();
          }
        }

        // Turn off R instance.
        try {
          if (!this.userCanceled)
            this.rSession.close(false);
        } catch (RSessionWrapperException e) {
          if (!this.userCanceled) {
            if (errorMsg == null)
              errorMsg = e.getMessage();
          } else {
            // User canceled: Silent.
          }
        }

      }

      // Task is over: Restore "parametersChanged" listeners
      // nset_VK_ESCAPE_KeyListener();

      if (errorMsg != null) {
        // Handle error.
        updateStatus(TaskStatus.ERROR);
      } else {
        // We're done!
        updateStatus(TaskStatus.FINISHED);
      }

    }

    public void kill() {

      RawDataFile dataFile = getPreviewDataFile();
      if (baselineCorrector != null && dataFile != null) {

        this.userCanceled = true;

        // Turn off R instance.
        try {
          if (this.rSession != null)
            this.rSession.close(true);
        } catch (RSessionWrapperException e) {
          // User canceled: Silent.
        }

        // Cancel task.
        this.cancel();
        // Release "ESC" listener.
        // unset_VK_ESCAPE_KeyListener();
        // Abort current processing thread
        baselineCorrector.setAbortProcessing(dataFile, true);

        logger.info("Preview task canceled!");
      }

    }

    /*
     * (non-Javadoc)
     *
     * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
     */
    @Override
    public String getTaskDescription() {
      return baselineCorrector.getName() + ": preview for file " + dataFile.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    @Override
    public double getFinishedPercentage() {
      return baselineCorrector.getFinishedPercentage(dataFile);
    }

    // GLG NOTE: Workaround to "TaskListener" death...
    public void updateStatus(TaskStatus status) {
      setStatus(status);
      if (this.getStatus() == TaskStatus.ERROR) {
        setErrorMessage(errorMsg);
        logger.log(Level.SEVERE, "Baseline correction error", this.getErrorMessage());
        MZmineCore.getDesktop().displayErrorMessage(this.getErrorMessage());
        hidePreview();
      }
    }
  }

  class ProgressThread extends Thread {

    private RawDataFile dataFile;
    private PreviewTask previewTask;
    private BaselineCorrectorSetupDialog dialog;
    private ProgressBar progressBar;

    public ProgressThread(BaselineCorrectorSetupDialog dialog, PreviewTask previewTask,
        RawDataFile dataFile) {
      this.previewTask = previewTask;
      this.dataFile = dataFile;
      this.dialog = dialog;
    }

    @Override
    public void run() {

      addProgessBar();
      while ((this.previewTask != null && this.previewTask.getStatus() == TaskStatus.PROCESSING)) {

        Platform.runLater(
            () -> progressBar.setProgress(baselineCorrector.getFinishedPercentage(dataFile)));
        try {
          Thread.sleep(5);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      // Clear GUI stuffs
      removeProgessBar();
      // unset_VK_ESCAPE_KeyListener();
    }

    private void addProgessBar() {
      // Add progress bar
      progressBar = new ProgressBar();
      progressBar.setProgress(0.25);
      // progressBar.setetStringPainted(true);
      // Border border =
      // BorderFactory.createTitledBorder("Processing... <Press \"ESC\" to cancel> ");
      // progressBar.setBorder(border);
      dialog.mainPane.setTop(progressBar);
      // this.dialog.repaint();
      progressBar.setVisible(true);
      // this.dialog.pack();
    }

    private void removeProgessBar() {
      // Remove progress bar
      progressBar.setVisible(false);
      dialog.mainPane.getChildren().remove(progressBar);
      // this.dialog.pack();
    }

  }

  /**
   * This function sets all the information into the plot chart
   */
  @Override
  protected void loadPreview(TICPlot ticPlot, RawDataFile dataFile, Range<Double> rtRange,
      Range<Double> mzRange) {

    boolean ready = true;
    // Abort previous preview task.
    if (previewTask != null && previewTask.getStatus() == TaskStatus.PROCESSING) {
      ready = false;
      previewTask.kill();
      try {
        previewThread.join();
        ready = true;
      } catch (InterruptedException e) {
        ready = false;
      }
    }

    // Start processing new preview task.
    if (ready && (previewTask == null || previewTask.getStatus() != TaskStatus.PROCESSING)) {

      baselineCorrector.initProgress(dataFile);
      previewTask = new PreviewTask(this, ticPlot, dataFile, rtRange, mzRange);
      previewThread = new Thread(previewTask);
      logger.info("Launch preview task.");
      previewThread.start();
    }
  }

  /**
   * Quick way to recover the baseline plot (by subtracting the corrected file from the original
   * one).
   *
   * @param dataFile original datafile
   * @param newDataFile corrected datafile
   * @param plotType expected plot type
   * @return the baseline additional dataset
   */
  private XYDataset createBaselineDataset(RawDataFile dataFile, RawDataFile newDataFile,
      TICPlotType plotType) {

    XYSeriesCollection dataset = new XYSeriesCollection();
    XYSeries bl_series = new XYSeries("Baseline");

    double intensity;
    Scan sc, new_sc;
    DataPoint dp, new_dp;

    // Get scan numbers from original file.
    final int[] scanNumbers = dataFile.getScanNumbers(1);
    final int numScans = scanNumbers.length;
    for (int scanIndex = 0; scanIndex < numScans; ++scanIndex) {

      sc = dataFile.getScan(scanNumbers[scanIndex]);
      new_sc = newDataFile.getScan(scanNumbers[scanIndex]);

      if (plotType == TICPlotType.BASEPEAK) {
        dp = sc.getHighestDataPoint();
        new_dp = new_sc.getHighestDataPoint();
        if (dp == null) {
          intensity = 0.0;
        } else if (new_dp == null) {
          intensity = dp.getIntensity();
        } else {
          intensity = dp.getIntensity() - new_dp.getIntensity();
        }
      } else {
        intensity = sc.getTIC() - new_sc.getTIC();
      }

      bl_series.add(sc.getRetentionTime(), intensity);
    }

    dataset.addSeries(bl_series);

    return dataset;
  }

  // /* (non-Javadoc)
  // * @see
  // io.github.mzmine.taskcontrol.TaskListener#statusChanged(io.github.mzmine.taskcontrol.TaskEvent)
  // */
  // @Override
  // public void statusChanged(TaskEvent e) {
  // if (e.getStatus() == TaskStatus.ERROR) {
  // logger.log(Level.SEVERE, "Baseline correction error",
  // e.getSource().getErrorMessage());
  // MZmineCore.getDesktop().displayErrorMessage( "Error of preview task ",
  // e.getSource().getErrorMessage());
  // hidePreview();
  // }
  // }
}
