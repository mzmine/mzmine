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

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.SIRIUS_TIMEOUT;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusParameters.MASS_LIST;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SiriusParameters.ionizationType;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.ELEMENTS;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.FINGERID_CANDIDATES;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.MZ_TOLERANCE;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.ION_MASS;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.SIRIUS_CANDIDATES;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.datamodel.IonType;
import io.github.msdk.datamodel.MsSpectrum;
import io.github.msdk.id.sirius.ConstraintsGenerator;
import io.github.msdk.id.sirius.SiriusIdentificationMethod;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import io.github.msdk.util.IonTypeUtil;

import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;

import org.openscience.cdk.formula.MolecularFormulaRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  // Error messages
  private final String siriusErrorMessage;
  private final String timerErrorMessage;

  // Amount of components to show
  private final Integer fingerCandidates;
  private final Integer siriusCandidates;

  // Timer for Sirius Identification method. If it expires, dialogue window shows up.
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

    timerErrorMessage = String.format("Processing of the peaklist with mass %.2f by Sirius module expired.\n", parentMass) +
        "Reinitialize the task with larger Sirius Timer value.";
    siriusErrorMessage = String.format("Sirius failed to predict compounds from row with id = %d", peakListRow.getID());
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (isFinished())
      return 1.0;
    else if (fingerTasks != null) {
      int amount = fingerTasks.size();
      double value = 0;
      for (FingerIdWebMethodTask t: fingerTasks)
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

    NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();
    ResultWindow window = new ResultWindow(peakListRow, this);
    window.setTitle("Sirius identifies peak with " + massFormater.format(parentMass) + " amu");
    window.setVisible(true);

    SpectrumScanner scanner;
    List<MsSpectrum> ms1list, ms2list;
    try {
      scanner = new SpectrumScanner(peakListRow, massListName);
      ms1list = scanner.getMsList();
      ms2list = scanner.getMsMsList();

      if (ms1list == null && ms2list == null) {
        throw new MethodRuntimeException("There are no scans for requested Mass List name");
      }
    } catch (MethodRuntimeException f) {
      showErrorAndCancel(window, String.format("Empty Mass List for %.2f", parentMass),
          "There is no MS2 scans that have a given precursor mass.\n" +
      "MS1 and MS2 lists are empty.\nCheck the Mass List field.");
      return;
    }

    // Use executor to run Sirius Identification Method as an Interruptable thread.
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
    } catch (InterruptedException|TimeoutException ie) {
      logger.error("Timeout on Sirius method expired, abort.");
      showErrorAndCancel(window, "Timer expired", timerErrorMessage);
      return;
    } catch (ExecutionException ce) {
      logger.error("Concurrency error during Sirius method.");
      showErrorAndCancel(window, "Sirius Error", siriusErrorMessage);
      return;
    }

    /* FingerId processing */
    if (scanner.peakContainsMsMs()) {
      try {
        latch = new CountDownLatch(siriusResults.size());
        Ms2Experiment experiment = siriusMethod.getExperiment();
        fingerTasks = new LinkedList<>();

        /* Create a new FingerIdWebTask for each Sirius result */
        for (IonAnnotation ia : siriusResults) {
          SiriusIonAnnotation annotation = (SiriusIonAnnotation) ia;
          FingerIdWebMethodTask task = new FingerIdWebMethodTask(annotation, experiment, fingerCandidates, window);
          task.setLatch(latch);
          fingerTasks.add(task);
          MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);
        }

        // Sleep for not overloading boecker-labs servers
        Thread.sleep(1000);
      } catch (InterruptedException interrupt) {
        logger.error("Processing of FingerWebMethods were interrupted");
      }
    } else {
      /* MS/MS spectrum is not present */
      window.addListofItems(siriusMethod.getResult());
    }

    // If there was a FingerId processing, wait until subtasks finish
    try {
      if (latch != null)
        latch.await();
    } catch (Exception e) {}
    setStatus(TaskStatus.FINISHED);
  }

  private void showErrorAndCancel(ResultWindow window, String title, String msg) {
    MZmineCore.getDesktop().displayErrorMessage(window, title,
        msg);
    this.setStatus(TaskStatus.CANCELED);
  }
}
