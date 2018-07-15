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
import net.sf.mzmine.taskcontrol.TaskPriority;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiriusThread implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(SiriusThread.class);
  private static final ExecutorService service = Executors.newSingleThreadExecutor();

  private final Semaphore semaphore;
  private final PeakListRow row;
  private final IonizationType ionType;
  private final int charge;
  private final int siriusCandidates;
  private final int fingeridCandidates;
  private final double mzTolerance;
  private final MolecularFormulaRange range;
  private final CountDownLatch latch;
  private final int siriusTimer;

  public SiriusThread(PeakListRow row, Semaphore semaphore, ParameterSet parameters, CountDownLatch latch) {
    this.semaphore = semaphore;
    this.row = row;
    charge = parameters.getParameter(PeakListIdentificationParameters.charge).getValue();
    ionType = parameters.getParameter(PeakListIdentificationParameters.ionizationType).getValue();
    range = parameters.getParameter(PeakListIdentificationParameters.ELEMENTS).getValue();
    mzTolerance = parameters.getParameter(PeakListIdentificationParameters.MZ_TOLERANCE).getValue();
    siriusCandidates = parameters.getParameter(PeakListIdentificationParameters.CANDIDATES_AMOUNT).getValue();
    fingeridCandidates = parameters.getParameter(PeakListIdentificationParameters.CANDIDATES_FINGERID).getValue();
    siriusTimer = parameters.getParameter(PeakListIdentificationParameters.SIRIUS_TIMEOUT).getValue();
    this.latch = latch;
  }

  @Override
  public void run() {


    final double massValue = row.getAverageMZ() * (double) charge - ionType.getAddedMass();

    SpectrumScanner scanner = new SpectrumScanner(row.getBestPeak());
    List<MsSpectrum> ms1 = scanner.getMsList();
    List<MsSpectrum> ms2 = scanner.getMsMsList();

    /* Debug */
//    processor.saveSpectrum(processor.getPeakName() + "_ms1.txt", 1);
//    processor.saveSpectrum(processor.getPeakName() + "_ms2.txt", 2);

    //todo: after msdk update, update this code
    ConstraintsGenerator generator = new ConstraintsGenerator();
    FormulaConstraints constraints = generator.generateConstraint(range);
    IonType siriusIon = IonTypeUtil.createIonType(ionType.toString());


    List<IonAnnotation> siriusResults = null;
    SiriusIdentificationMethod siriusMethod = null;

    try {
      final SiriusIdentificationMethod method = new SiriusIdentificationMethod(ms1, ms2, massValue, siriusIon, siriusCandidates, constraints, mzTolerance);
      final Future<List<IonAnnotation>> f = service.submit(() -> {
        return method.execute();
      });
      siriusResults = f.get(siriusTimer, TimeUnit.SECONDS);
      siriusMethod = method;

      if (!scanner.peakContainsMsMs()) {
        addSiriusCompounds(siriusResults, row, siriusCandidates);
      } else {
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
      latch.countDown();
      semaphore.release();
      logger.debug("Semaphore RELEASED");
    }
  }
}
