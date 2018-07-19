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

import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.AbstractParameters.ionizationType;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.ELEMENTS;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.FINGERID_CANDIDATES;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.MZ_TOLERANCE;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.NEUTRAL_MASS;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;

import org.openscience.cdk.formula.MolecularFormulaRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleRowIdentificationTask extends AbstractTask {
  public static final NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

  private double mzTolerance;
  private PeakListRow peakListRow;
  private IonizationType ionType;
  private MolecularFormulaRange range;
  private Double parentMass;
  private Integer fingerCandidates;
  private Integer siriusCandidates;
  private List<FingerIdWebMethodTask> fingerTasks;

  private static final Logger logger = LoggerFactory.getLogger(SingleRowIdentificationTask.class);

  /**
   * Create the task.
   *
   * @param parameters task parameters.
   * @param peakListRow peak-list row to identify.
   */
  public SingleRowIdentificationTask(ParameterSet parameters, PeakListRow peakListRow) {
    this.peakListRow = peakListRow;
    mzTolerance = parameters.getParameter(MZ_TOLERANCE).getValue();
    siriusCandidates = parameters.getParameter(SIRIUS_CANDIDATES).getValue();
    fingerCandidates = parameters.getParameter(FINGERID_CANDIDATES).getValue();
    ionType = parameters.getParameter(ionizationType).getValue();
    parentMass = parameters.getParameter(NEUTRAL_MASS).getValue();
    range = parameters.getParameter(ELEMENTS).getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    //TODO: refactor
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
    ResultWindow window = new ResultWindow(peakListRow, parentMass, this);
    window.setTitle("Sirius identifies peak with " + massFormater.format(parentMass) + " amu");
    window.setVisible(true);

    Feature bestPeak = peakListRow.getBestPeak();
    SpectrumScanner scanner = new SpectrumScanner(bestPeak);
    List<MsSpectrum> ms1list = scanner.getMsList();
    List<MsSpectrum> ms2list = scanner.getMsMsList();

    /* Debug */
//    processor.saveSpectrum(processor.getPeakName() + "_ms1.txt", 1);
//    processor.saveSpectrum(processor.getPeakName() + "_ms2.txt", 2);

    final ExecutorService service = Executors.newSingleThreadExecutor();
    SiriusIdentificationMethod siriusMethod = null;
    List<IonAnnotation> siriusResults = null;

    /* Sirius processing */
    try {
      FormulaConstraints constraints = ConstraintsGenerator.generateConstraint(range);
      IonType type = IonTypeUtil.createIonType(ionType.toString());

      final SiriusIdentificationMethod method = new SiriusIdentificationMethod(ms1list, ms2list,
          parentMass, type, siriusCandidates, constraints, mzTolerance);
      final Future<List<IonAnnotation>> f = service.submit(() -> {
        return method.execute();
      });
      siriusResults = f.get(40, TimeUnit.SECONDS);
      siriusMethod = method;
    } catch (InterruptedException|TimeoutException ie) {
      logger.error("Timeout on Sirius method expired, abort.");
      ie.printStackTrace();
    } catch (ExecutionException ce) {
      logger.error("Concurrency error during Sirius method.");
      ce.printStackTrace();
    }

    /* FingerId processing */
    if (scanner.peakContainsMsMs()) {
      try {
        Ms2Experiment experiment = siriusMethod.getExperiment();
        fingerTasks = new LinkedList<>();

        /* Create a new FingerIdWebTask for each Sirius result */
        for (IonAnnotation ia : siriusResults) {
          SiriusIonAnnotation annotation = (SiriusIonAnnotation) ia;
          FingerIdWebMethodTask task = new FingerIdWebMethodTask(annotation, experiment, fingerCandidates, window);
          fingerTasks.add(task);
          MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);
        }

        // Sleep for not overloading boecker-labs servers
        Thread.sleep(1000);
      } catch (InterruptedException interrupt) {
        logger.error("Processing of FingerWebMethods were interrupted");
        interrupt.printStackTrace();
      }
    } else {
      /* MS/MS spectrum is not present */
      window.addListofItems(siriusMethod.getResult());
    }

    setStatus(TaskStatus.FINISHED);
  }
}
