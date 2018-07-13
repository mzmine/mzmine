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

import EDU.oswego.cs.dl.util.concurrent.CountDown;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.datamodel.IonType;
import io.github.msdk.datamodel.MsSpectrum;
import io.github.msdk.datamodel.SimpleMsSpectrum;
import io.github.msdk.id.sirius.ConstraintsGenerator;
import io.github.msdk.id.sirius.FingerIdWebMethod;
import io.github.msdk.id.sirius.SiriusIdentificationMethod;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import io.github.msdk.util.IonTypeUtil;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.slf4j.LoggerFactory;

public class PeakListIdentificationTask extends AbstractTask {

  // Logger.
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(PeakListIdentificationTask.class);
  private final Semaphore semaphore;
  private final CountDownLatch latch;

  // Counters.
  private int finishedItems;
  private int numItems;
  private Integer itemsDone;

  private final ParameterSet parameters;
  private final PeakList peakList;
  private final IonizationType ionType;
  private final MolecularFormulaRange range;
  private final double mzTolerance;
  private final int siriusCandidates;
  private final int fingeridCandidates;
  private final int candidatesAmount;
  private final int charge;
  private final int threadsAmount;
  private PeakListRow currentRow;

  /**
   * Create the identification task.
   * 
   * @param parameters task parameters.
   * @param list peak list to operate on.
   */
  PeakListIdentificationTask(final ParameterSet parameters, final PeakList list) {
    peakList = list;
    numItems = 0;
    finishedItems = 0;
    currentRow = null;
    this.parameters = parameters;

    mzTolerance = parameters.getParameter(PeakListIdentificationParameters.MZ_TOLERANCE).getValue();
    ionType = parameters.getParameter(PeakListIdentificationParameters.ionizationType).getValue();
    range = parameters.getParameter(PeakListIdentificationParameters.ELEMENTS).getValue();
    siriusCandidates = 1;
    fingeridCandidates = 1;
    charge = parameters.getParameter(PeakListIdentificationParameters.charge).getValue();
    candidatesAmount = parameters.getParameter(PeakListIdentificationParameters.CANDIDATES_AMOUNT).getValue();

    threadsAmount = parameters.getParameter(PeakListIdentificationParameters.THREADS_AMOUNT).getValue();
    semaphore = new Semaphore(threadsAmount);
    latch = new CountDownLatch(list.getNumberOfRows());
    itemsDone = 0;
  }

  @Override
  public double getFinishedPercentage() {

//    return numItems == 0 ? 0.0 : (double) finishedItems / (double) numItems;
    return numItems == 0 ? 0.0 : (double) itemsDone / (double) numItems;
  }

  @Override
  public String getTaskDescription() {

    return "Identification of peaks in " + peakList
        + (currentRow == null ? " using "
            : " (" + MZmineCore.getConfiguration().getMZFormat().format(currentRow.getAverageMZ())
                + " m/z) using SIRIUS");
  }

  @Override
  public void run() {

    if (!isCanceled()) {
      try {
        setStatus(TaskStatus.PROCESSING);

        // Identify the peak list rows starting from the biggest peaks.
        final PeakListRow[] rows = peakList.getRows();
        Arrays.sort(rows, new PeakListRowSorter(SortingProperty.Area, SortingDirection.Descending));

        // Initialize counters.
        numItems = rows.length;

        // Process rows.
        for (finishedItems = 0; !isCanceled() && finishedItems < numItems;) {
          try {
            semaphore.acquire();
            logger.debug("Semaphore ACQUIRED");
            Thread th = new Thread(new SiriusThread(rows[finishedItems++], semaphore, parameters, latch));
            th.setDaemon(true);
            th.start();
          } catch (InterruptedException e) {
            logger.error("The thread was interrupted");
            e.printStackTrace();
          }

        }

        latch.await();
        if (!isCanceled()) {
          setStatus(TaskStatus.FINISHED);
        }
      } catch (Throwable t) {

        final String msg = "Could not search ";
        logger.warn(msg, t);
        setStatus(TaskStatus.ERROR);
        setErrorMessage(msg + ": " + ExceptionUtils.exceptionToString(t));
      }
    }
  }

  /**
   * Search the database for the peak's identity.
   * 
   * @param row the peak list row.
   * @throws IOException if there are i/o problems.
   */
  private void processSpectra(final PeakListRow row) throws IOException {
    currentRow = row;

//    Feature bestPeak = row.getBestPeak();
////    int charge = bestPeak.getCharge();
////    if (charge <= 0) {
////      charge = 1;
////    }
//
//    // Calculate mass value.
//
//    final double massValue = row.getAverageMZ() * (double) charge - ionType.getAddedMass();
//
//    SpectrumProcessing processor = new SpectrumProcessing(bestPeak);
//    List<MsSpectrum> ms1 = processor.getMsList();
//    List<MsSpectrum> ms2 = processor.getMsMsList();
//
//    processor.saveSpectrum(processor.getPeakName() + "_ms1.txt", 1);
//    processor.saveSpectrum(processor.getPeakName() + "_ms2.txt", 2);
//
//    ConstraintsGenerator generator = new ConstraintsGenerator();
//    FormulaConstraints constraints = generator.generateConstraint(range);
//    IonType siriusIon = IonTypeUtil.createIonType(ionType.toString());
//
//
//    List<IonAnnotation> siriusResults = null;
//    SiriusIdentificationMethod siriusMethod = null;
//
//    try {
//      final SiriusIdentificationMethod method = new SiriusIdentificationMethod(ms1, ms2, massValue, siriusIon, siriusCandidates, constraints, mzTolerance.getPpmTolerance());
//      final Future<List<IonAnnotation>> f = service.submit(() -> {
//        return method.execute();
//      });
//      siriusResults = f.get(5, TimeUnit.SECONDS);
//      siriusMethod = method;
//    } catch (InterruptedException|TimeoutException ie) {
//      logger.error("Timeout on Sirius method expired, abort.");
//      ie.printStackTrace();
//      return;
//    } catch (ExecutionException ce) {
//      logger.error("Concurrency error during Sirius method.");
//      ce.printStackTrace();
//      return;
//    }
//
//    if (!processor.peakContainsMsMs()) {
//      addSiriusCompounds(siriusResults, row, candidatesAmount);
//    } else {
//      try {
//        Ms2Experiment experiment = siriusMethod.getExperiment();
//
//        SiriusIonAnnotation annotation = (SiriusIonAnnotation) siriusResults.get(0);
//        FingerIdWebMethodTask task = new FingerIdWebMethodTask(annotation, experiment, fingeridCandidates, row);
//        MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);
//        Thread.sleep(300);
//      } catch (InterruptedException interrupt) {
//        logger.error("Processing of FingerWebMethods were interrupted");
//        interrupt.printStackTrace();
//        addSiriusCompounds(siriusResults, row, candidatesAmount);
//      }
//    }
  }

  public static void addSiriusCompounds(List<IonAnnotation> annotations, PeakListRow row, int amount) {
    for (int i = 0; i < amount; i++) { //todo: add howManyTopResultsToStore
      SiriusIonAnnotation annotation = (SiriusIonAnnotation) annotations.get(i);
      SiriusCompound compound = new SiriusCompound(annotation, annotation.getSiriusScore());
      row.addPeakIdentity(compound, false);
    }
    updateRow(row);
  }

  public static void updateRow(PeakListRow row) {
    MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row, false);

    Desktop desktop = MZmineCore.getDesktop();
    if (!(desktop instanceof HeadLessDesktop))
      desktop.getMainWindow().repaint();
  }
}
