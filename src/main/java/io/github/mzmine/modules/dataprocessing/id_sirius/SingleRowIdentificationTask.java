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

package io.github.mzmine.modules.dataprocessing.id_sirius;

import static io.github.mzmine.modules.dataprocessing.id_sirius.SingleRowIdentificationParameters.FINGERID_CANDIDATES;
import static io.github.mzmine.modules.dataprocessing.id_sirius.SingleRowIdentificationParameters.ION_MASS;
import static io.github.mzmine.modules.dataprocessing.id_sirius.SingleRowIdentificationParameters.SIRIUS_CANDIDATES;
import static io.github.mzmine.modules.dataprocessing.id_sirius.SingleRowIdentificationParameters.SIRIUS_TIMEOUT;
import static io.github.mzmine.modules.dataprocessing.id_sirius.SiriusParameters.ELEMENTS;
import static io.github.mzmine.modules.dataprocessing.id_sirius.SiriusParameters.MASS_LIST;
import static io.github.mzmine.modules.dataprocessing.id_sirius.SiriusParameters.MZ_TOLERANCE;
import static io.github.mzmine.modules.dataprocessing.id_sirius.SiriusParameters.ionizationType;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import javafx.application.Platform;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.datamodel.IonType;
import io.github.msdk.datamodel.MsSpectrum;
import io.github.msdk.datamodel.SimpleMsSpectrum;
import io.github.msdk.id.sirius.ConstraintsGenerator;
import io.github.msdk.id.sirius.SiriusIdentificationMethod;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import io.github.msdk.util.DataPointSorter;
import io.github.msdk.util.DataPointSorter.SortingDirection;
import io.github.msdk.util.DataPointSorter.SortingProperty;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.msdk.util.IonTypeUtil;

public class SingleRowIdentificationTask extends AbstractTask {
  private static final Logger logger = LoggerFactory.getLogger(SingleRowIdentificationTask.class);
  private static final NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

  private final PeakListRow peakListRow;
  private final String massListName;

  // Parameters for Sirius & FingerId methods
  private final IonizationType ionType;
  private final MolecularFormulaRange range; // Future constraints object
  private final Double parentMass;
  private final Double deviationPpm;

  // Amount of components to show
  private final Integer fingerCandidates;
  private final Integer siriusCandidates;
  ResultWindowFX resultWindowFX;

  // Timer for Sirius Identification method. If it expires, dialogue window
  // shows up.
  private final Integer timer;

  // Barrier for this task, it will be scheduled until all subtasks finish.
  private CountDownLatch latch;

  // Dynamic list of tasks
  private List<FingerIdWebMethodTask> fingerTasks;

  /**
   * Create the task.
   *
   * @param parameters task parameters.
   * @param peakListRow peak-list row to identify.
   */
  public SingleRowIdentificationTask(ParameterSet parameters, PeakListRow peakListRow) {
    this.peakListRow = peakListRow;
    siriusCandidates = parameters.getParameter(SIRIUS_CANDIDATES).getValue();
    fingerCandidates = parameters.getParameter(FINGERID_CANDIDATES).getValue();
    ionType = parameters.getParameter(ionizationType).getValue();
    parentMass = parameters.getParameter(ION_MASS).getValue();
    range = parameters.getParameter(ELEMENTS).getValue();
    timer = parameters.getParameter(SIRIUS_TIMEOUT).getValue();
    massListName = parameters.getParameter(MASS_LIST).getValue();

    MZTolerance mzTolerance = parameters.getParameter(MZ_TOLERANCE).getValue();
    double mz = peakListRow.getAverageMZ();
    double upperPoint = mzTolerance.getToleranceRange(mz).upperEndpoint();
    deviationPpm = (upperPoint - mz) / (mz * 1E-6);
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (isFinished())
      return 1.0;
    else if (fingerTasks != null) {
      int amount = fingerTasks.size();
      double value = 0;
      for (FingerIdWebMethodTask t : fingerTasks)
        value += t.getFinishedPercentage();
      value /= amount;

      return value;
    }
    return 0;
  }

  public String getTaskDescription() {
    return "Peak identification of " + massFormater.format(parentMass) + " using Sirius module";
  }

  /**
   * @see Runnable#run()
   */

  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final FutureTask query = new FutureTask(()-> {
            resultWindowFX = new ResultWindowFX(peakListRow, this);
      resultWindowFX.setTitle(
              "SIRIUS/CSI-FingerID identification of " + massFormater.format(parentMass) + " m/z");
      resultWindowFX.setMinHeight(200);
      resultWindowFX.setMinWidth(700);
      resultWindowFX.show();
      return resultWindowFX;

    });
    Platform.runLater(query);

    try {
      query.get();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }

    List<MsSpectrum> ms1list = new ArrayList<>(), ms2list = new ArrayList<>();

    try {

      Scan ms1Scan = peakListRow.getBestPeak().getRepresentativeScan();
      Collection<Scan> top10ms2Scans = ScanUtils.selectBestMS2Scans(peakListRow, massListName, 10);
      logger.debug("Adding MS1 scan " + ScanUtils.scanToString(ms1Scan, true)
          + " for SIRIUS identification");

      // Convert to MSDK data model
      ms1list.add(buildMSDKSpectrum(ms1Scan, massListName));
      for (Scan ms2Scan : top10ms2Scans) {
        logger.debug("Adding MS/MS scan " + ScanUtils.scanToString(ms2Scan, true)
            + " for SIRIUS identification");
        ms2list.add(buildMSDKSpectrum(ms2Scan, massListName));
      }

    } catch (MissingMassListException f) {
      showError(resultWindowFX,
          "Scan does not contain Mass List with requested name. [" + massListName + "]");
      return;
    }

    // Use executor to run Sirius Identification Method as an Interruptable
    // thread.
    // Otherwise it may compute for too long (or even forever).
    final ExecutorService service = Executors.newSingleThreadExecutor();
    SiriusIdentificationMethod siriusMethod = null;
    List<IonAnnotation> siriusResults = null;

    /* Sirius processing */
    try {
      FormulaConstraints constraints = ConstraintsGenerator.generateConstraint(range);
      IonType type = IonTypeUtil.createIonType(ionType.toString());

      final SiriusIdentificationMethod method = new SiriusIdentificationMethod(ms1list, ms2list,
          parentMass, type, siriusCandidates, constraints, deviationPpm);
      final Future<List<IonAnnotation>> f = service.submit(() -> {
        return method.execute();
      });
      siriusResults = f.get(timer, TimeUnit.SECONDS);
      siriusMethod = method;
    }
    catch (InterruptedException | TimeoutException ie) {
      logger.error("Timeout on Sirius method expired, abort.");
      showError(resultWindowFX,
          String.format("Processing of the peaklist with mass %.2f by Sirius module expired.\n",
              parentMass) + "Reinitialize the task with larger Sirius Timer value.");
      return;
    }
    catch (ExecutionException ce) {
      ce.printStackTrace();
      logger.error("Concurrency error during Sirius method: " + ce.getMessage());
      showError(resultWindowFX, String.format("Sirius failed to predict compounds from row with id = %d",
          peakListRow.getID()));
      return;
    }
    /* FingerId processing */
    if (!ms2list.isEmpty()) {
      try {
        latch = new CountDownLatch(siriusResults.size());
        Ms2Experiment experiment = siriusMethod.getExperiment();
        fingerTasks = new LinkedList<>();

        /* Create a new FingerIdWebTask for each Sirius result */
        for (IonAnnotation ia : siriusResults) {
          SiriusIonAnnotation annotation = (SiriusIonAnnotation) ia;
          FingerIdWebMethodTask task =
              new FingerIdWebMethodTask(annotation, experiment, fingerCandidates, resultWindowFX);
          task.setLatch(latch);
          fingerTasks.add(task);
          MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);
        }

        // Sleep for not overloading boecker-labs servers
        Thread.sleep(1000);
      }
      catch (InterruptedException interrupt) {
        logger.error("Processing of FingerWebMethods were interrupted");
      }
    }
    else
        {
      /* MS/MS spectrum is not present */
      resultWindowFX.addListofItems(siriusMethod.getResult());
    }

    // If there was a FingerId processing, wait until subtasks finish
    try {
      if (latch != null)
        latch.await();
    } catch (InterruptedException e) {
    }
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Shows error dialogue window and sets task status as ERROR
   *  @param window - where to create dialogue
   * @param msg of the error window
   */
  private void showError(ResultWindowFX window, String msg) {
    window.dispose();
    setErrorMessage(msg);
    this.setStatus(TaskStatus.ERROR);
  }

  /**
   * Construct MsSpectrum object from DataPoint array
   * 
   * @return new MsSpectrum
   */
  private MsSpectrum buildMSDKSpectrum(Scan scan, String massListName)
      throws MissingMassListException {

    MassList ml = scan.getMassList(massListName);
    if (ml == null)
      throw new MissingMassListException(
          "Scan #" + scan.getScanNumber() + " does not have mass list", massListName);

    DataPoint[] points = ml.getDataPoints();

    SimpleMsSpectrum spectrum = new SimpleMsSpectrum();
    double mz[] = new double[points.length];
    float intensity[] = new float[points.length];

    for (int i = 0; i < points.length; i++) {
      mz[i] = points[i].getMZ();
      intensity[i] = (float) points[i].getIntensity();
    }
    DataPointSorter.sortDataPoints(mz, intensity, points.length, SortingProperty.MZ,
        SortingDirection.ASCENDING);

    spectrum.setDataPoints(mz, intensity, points.length);
    return spectrum;
  }
}
