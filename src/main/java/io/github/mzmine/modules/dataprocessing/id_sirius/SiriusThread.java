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

import static io.github.mzmine.modules.dataprocessing.id_sirius.PeakListIdentificationTask.addSiriusCompounds;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.logging.Logger;
import org.openscience.cdk.formula.MolecularFormulaRange;
import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.datamodel.IonType;
import io.github.msdk.datamodel.MsSpectrum;
import io.github.msdk.id.sirius.ConstraintsGenerator;
import io.github.msdk.id.sirius.SiriusIdentificationMethod;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import io.github.msdk.util.IonTypeUtil;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.impl.MZmineToMSDKMsScan;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;

/**
 * SiriusThread class Allows to process PeakListIdentificationTask faster by subthreading it.
 */
public class SiriusThread implements Runnable {
  private static final Logger logger = Logger.getLogger(SiriusThread.class.getName());

  // Use executor to run Sirius Identification Method as an Interruptable
  // thread.
  // Otherwise it may compute for too long (or even forever).
  private static final ExecutorService service = Executors.newSingleThreadExecutor();

  // Identification params
  private final FeatureListRow peakListRow;
  private final IonizationType ionType;
  private final MolecularFormulaRange range;
  private final Double deviationPpm;

  // Amount of items to store
  private final int siriusCandidates;
  private final int fingeridCandidates;

  // Multithreading params
  private final CountDownLatch latch;
  private final Semaphore semaphore;
  private final int siriusTimer;

  // Cancel link
  private final PeakListIdentificationTask task;

  /**
   * Constructor for SiriusThread - initializes params
   *
   * @param semaphore
   * @param parameters
   * @param latch
   */
  public SiriusThread(FeatureListRow peakListRow, ParameterSet parameters, Semaphore semaphore,
      CountDownLatch latch, PeakListIdentificationTask task) {
    ionType = parameters.getParameter(PeakListIdentificationParameters.ionizationType).getValue();
    range = parameters.getParameter(PeakListIdentificationParameters.ELEMENTS).getValue();
    siriusCandidates =
        parameters.getParameter(PeakListIdentificationParameters.CANDIDATES_AMOUNT).getValue();
    fingeridCandidates =
        parameters.getParameter(PeakListIdentificationParameters.CANDIDATES_FINGERID).getValue();
    siriusTimer =
        parameters.getParameter(PeakListIdentificationParameters.SIRIUS_TIMEOUT).getValue();
    this.task = task;

    this.semaphore = semaphore;
    this.peakListRow = peakListRow;
    this.latch = latch;

    MZTolerance mzTolerance =
        parameters.getParameter(PeakListIdentificationParameters.MZ_TOLERANCE).getValue();
    double mz = peakListRow.getAverageMZ();
    double upperPoint = mzTolerance.getToleranceRange(mz).upperEndpoint();
    deviationPpm = (upperPoint - mz) / (mz * 1E-6);
  }

  @Override
  public void run() {
    List<MsSpectrum> ms1list = new ArrayList<>(), ms2list = new ArrayList<>();

    try {

      Scan ms1Scan = peakListRow.getBestFeature().getRepresentativeScan();
      Collection<Scan> top10ms2Scans = ScanUtils.selectBestMS2Scans(peakListRow, 10);

      // Convert to MSDK data model
      ms1list.add(new MZmineToMSDKMsScan(ms1Scan));
      for (Scan s : top10ms2Scans) {
        ms2list.add(new MZmineToMSDKMsScan(s));
      }

    } catch (MissingMassListException f) {
      releaseResources();
      task.remoteCancel("Scan does not have a mass list");
      return;
    }

    FormulaConstraints constraints = ConstraintsGenerator.generateConstraint(range);
    IonType siriusIon = IonTypeUtil.createIonType(ionType.toString());

    List<IonAnnotation> siriusResults = null;
    SiriusIdentificationMethod siriusMethod = null;

    /*
     * Code block below gives SiriusMethod specific amount of time to be executed, if it expires ->
     * log error and continue
     */
    try {
      final SiriusIdentificationMethod method = new SiriusIdentificationMethod(ms1list, ms2list,
          peakListRow.getAverageMZ(), siriusIon, siriusCandidates, constraints, deviationPpm);

      // On some spectra it may never stop (halting problem), that's why
      // interruptable thread is
      // used
      final Future<List<IonAnnotation>> f = service.submit(() -> {
        return method.execute();
      });
      siriusResults = f.get(siriusTimer, TimeUnit.SECONDS);
      siriusMethod = method;

      if (ms2list.isEmpty()) {
        /* If no MSMS spectra - add sirius results */
        addSiriusCompounds(siriusResults, peakListRow, siriusCandidates);
      } else {
        /* Initiate FingerId processing */
        Ms2Experiment experiment = siriusMethod.getExperiment();
        for (int index = 0; index < siriusCandidates; index++) {
          SiriusIonAnnotation annotation = (SiriusIonAnnotation) siriusResults.get(index);
          try {
            FingerIdWebMethodTask task =
                new FingerIdWebMethodTask(annotation, experiment, fingeridCandidates, peakListRow);
            MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);
            Thread.sleep(1000);
          } catch (InterruptedException interrupt) {
            logger.severe("Processing of FingerWebMethods were interrupted");

            /* If interrupted, store last item */
            List<IonAnnotation> lastItem = new LinkedList<>();
            lastItem.add(annotation);
            addSiriusCompounds(lastItem, peakListRow, 1);
          }
        }
      }
    } catch (InterruptedException | TimeoutException ie) {
      logger.severe("Timeout on Sirius method expired, abort. Row id = " + peakListRow.getID());
    } catch (ExecutionException ce) {
      logger.severe("Concurrency error during Sirius method.  Row id = " + peakListRow.getID());
    } finally {
      // Do not forget to release resources!
      releaseResources();
    }
  }

  /**
   * Method for dealing with multithread resources 1) Release semaphore 2) Count down barrier
   */
  private void releaseResources() {
    latch.countDown();
    semaphore.release();
  }
}
