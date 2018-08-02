/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.identification.sirius;

import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.PeakListIdentificationTask.addSiriusCompounds;

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;

import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.datamodel.IonType;
import io.github.msdk.datamodel.MsSpectrum;
import io.github.msdk.id.sirius.ConstraintsGenerator;
import io.github.msdk.id.sirius.SiriusIdentificationMethod;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import io.github.msdk.util.IonTypeUtil;

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

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.TaskPriority;

import org.openscience.cdk.formula.MolecularFormulaRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SiriusThread class
 * Allows to process PeakListIdentificationTask faster by subthreading it.
 */
public class SiriusThread implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(SiriusThread.class);

  // Use executor to run Sirius Identification Method as an Interruptable thread.
  // Otherwise it may compute for too long (or even forever).
  private static final ExecutorService service = Executors.newSingleThreadExecutor();

  // Identification params
  private final PeakListRow row;
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

  /**
   * Constructor for SiriusThread - initializes params
   * @param row
   * @param semaphore
   * @param parameters
   * @param latch
   */
  public SiriusThread(PeakListRow row, ParameterSet parameters, Semaphore semaphore, CountDownLatch latch) {
    ionType = parameters.getParameter(PeakListIdentificationParameters.ionizationType).getValue();
    range = parameters.getParameter(PeakListIdentificationParameters.ELEMENTS).getValue();
    siriusCandidates = parameters.getParameter(PeakListIdentificationParameters.CANDIDATES_AMOUNT).getValue();
    fingeridCandidates = parameters.getParameter(PeakListIdentificationParameters.CANDIDATES_FINGERID).getValue();
    siriusTimer = parameters.getParameter(PeakListIdentificationParameters.SIRIUS_TIMEOUT).getValue();
    this.semaphore = semaphore;
    this.row = row;
    this.latch = latch;

    MZTolerance mzTolerance = parameters.getParameter(PeakListIdentificationParameters.MZ_TOLERANCE).getValue();
    double mz = row.getAverageMZ();
    double upperPoint = mzTolerance.getToleranceRange(mz).upperEndpoint();
    deviationPpm = (upperPoint - mz) / (mz * 1E-6);
  }

  @Override
  public void run() {
    SpectrumScanner scanner = new SpectrumScanner(row);
    List<MsSpectrum> ms1 = scanner.getMsList();
    List<MsSpectrum> ms2 = scanner.getMsMsList();

    FormulaConstraints constraints = ConstraintsGenerator.generateConstraint(range);
    IonType siriusIon = IonTypeUtil.createIonType(ionType.toString());

    List<IonAnnotation> siriusResults = null;
    SiriusIdentificationMethod siriusMethod = null;

    /*
      Code block below gives SiriusMethod specific amount of time to be executed,
      if it expires -> log error and continue
    */
    try {

      final SiriusIdentificationMethod method = new SiriusIdentificationMethod(ms1, ms2, row.getAverageMZ(),
          siriusIon, siriusCandidates, constraints, deviationPpm);

      // On some spectra it may never stop (halting problem), that's why interruptable thread is used
      final Future<List<IonAnnotation>> f = service.submit(() -> {
        return method.execute();
      });
      siriusResults = f.get(siriusTimer, TimeUnit.SECONDS);
      siriusMethod = method;

      if (!scanner.peakContainsMsMs()) {
        /* If no MSMS spectra - add sirius results */
        addSiriusCompounds(siriusResults, row, siriusCandidates);
      } else {
        /* Initiate FingerId processing */
        Ms2Experiment experiment = siriusMethod.getExperiment();
        for (int index = 0; index < siriusCandidates; index++) {
          SiriusIonAnnotation annotation = (SiriusIonAnnotation) siriusResults.get(index);
          try {
            FingerIdWebMethodTask task = new FingerIdWebMethodTask(annotation, experiment, fingeridCandidates, row);
            MZmineCore.getTaskController().addTask(task, TaskPriority.NORMAL);
            Thread.sleep(1000);
          } catch (InterruptedException interrupt) {
            logger.error("Processing of FingerWebMethods were interrupted");
            interrupt.printStackTrace();

            /* If interrupted, store last item */
            List<IonAnnotation> lastItem = new LinkedList<>();
            lastItem.add(annotation);
            addSiriusCompounds(lastItem, row, 1);
          }
        }
      }
    } catch (InterruptedException|TimeoutException ie) {
      logger.error("Timeout on Sirius method expired, abort.");
      ie.printStackTrace();
    } catch (ExecutionException ce) {
      logger.error("Concurrency error during Sirius method.");
      ce.printStackTrace();
    } finally {
      // Do not forget to release resources!
      latch.countDown();
      semaphore.release();
      logger.debug("Semaphore RELEASED");
    }
  }
}
