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

import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.id.sirius.FingerIdWebMethod;
import io.github.msdk.id.sirius.SiriusIonAnnotation;

import java.util.LinkedList;
import java.util.List;

import java.util.concurrent.CountDownLatch;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class FingerIdWebMethodTask
 * Wrapper around FingerIdWebMethod - calculates the result as a separate MZmine task
 * On end of execution - updates result containers (ResultWindow or PeakListRow)
 */
public class FingerIdWebMethodTask extends AbstractTask {
  private static final Logger logger = LoggerFactory.getLogger(FingerIdWebMethodTask.class);

  /* Web-request parameters */
  private final SiriusIonAnnotation annotation;
  private final Ms2Experiment experiment;
  private final String formula;
  private final Integer candidatesAmount;

  /* Result containers */
  private final ResultWindow window;
  private final PeakListRow row;

  /* MSDK-method */
  private FingerIdWebMethod method;

  /* Concurrency tracker */
  private CountDownLatch latch = null;

  private List<IonAnnotation> fingerResults = null;

  /**
   * Constructor for FingerIdWebMethodTask
   * @param annotation - SiriusIonAnnotation to process by FingerId
   * @param experiment - contains necessary information for a web-request
   * @param candidatesAmount - amount of candidates to return from this task
   * @param window - one of possible result containers
   * @param row - one of possible result containers
   */
  private FingerIdWebMethodTask(SiriusIonAnnotation annotation, Ms2Experiment experiment,
      Integer candidatesAmount, ResultWindow window, PeakListRow row) {
    if (window == null && row == null)
      throw new RuntimeException("Only one result container can be null at a time");

    this.candidatesAmount = candidatesAmount;
    this.experiment = experiment;
    this.annotation = annotation;
    this.window = window;
    this.row = row;
    formula = MolecularFormulaManipulator.getString(annotation.getFormula());
  }

  public void setLatch(CountDownLatch latch) {
    this.latch = latch;
  }

  /**
   * Constructor for FingerIdWebMethodTask, used by SingleRowIdentificationTask
   * @param annotation
   * @param experiment
   * @param candidatesAmount
   * @param window - Result container for SingleRowIdentificationTask
   */
  public FingerIdWebMethodTask(SiriusIonAnnotation annotation, Ms2Experiment experiment,
      Integer candidatesAmount, ResultWindow window) {
    this(annotation, experiment, candidatesAmount, window, null);
  }

  /**
   * Constructor for FingerIdWebMethodTask, used by PeakListIdentificationTask
   * @param annotation
   * @param experiment
   * @param candidatesAmount
   * @param row - Result container for PeakListIdentificationTask
   */
  public FingerIdWebMethodTask(SiriusIonAnnotation annotation, Ms2Experiment experiment,
      Integer candidatesAmount, PeakListRow row) {
    this(annotation, experiment, candidatesAmount, null, row);
  }

  @Override
  public String getTaskDescription() {
    return String.format("Processing element %s by FingerIdWebMethod", formula);
  }

  @Override
  public double getFinishedPercentage() {
    if (method == null || method.getFinishedPercentage() == null)
      return 0.0;
    return method.getFinishedPercentage();
  }

  @Override
  public void run()  {
    setStatus(TaskStatus.PROCESSING);

    try {
      method = new FingerIdWebMethod(experiment, annotation, candidatesAmount);
      fingerResults = method.execute();
      logger.debug("Successfully processed {} by FingerWebMethod", formula);
    } catch (RuntimeException e) {
      logger.error("Error during processing FingerIdWebMethod --- return initial compound");
      e.printStackTrace();
      fingerResults = null;
    } catch (MSDKException msdk) {
      logger.error("Internal FingerIdWebMethod error occured.");
      msdk.printStackTrace();
      fingerResults = null;
    }

    // Check exception handling and empty results from web request
    if (fingerResults == null || fingerResults.size() == 0) {
      logger.info("No results found by FingerId Web Method for {}, adding initial compound", formula);
      fingerResults = new LinkedList<>();
      fingerResults.add(annotation);
    }

    // Update containers
    submitResults(fingerResults);
    // If Barrier exists - count it down
    if (latch != null)
      latch.countDown();
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Updates result container - stores the results of execution
    * @param results
   */
  private void submitResults(List<IonAnnotation> results) {
    if (window != null) // Update ResultWindow - if called from SingleRowIdentificationTask
      window.addListofItems(results);

    if (row != null) { // Update PeakListRow - if called from PeakListIdentificationTask
      // Sometimes method may return less items than expected (1 or 2).
      int quantity = (candidatesAmount > results.size()) ? results.size() : candidatesAmount;
      PeakListIdentificationTask.addSiriusCompounds(results, row, quantity);
    }
  }

  /**
   * @return results of execution
   */
  public List<IonAnnotation> getResults() {
    return fingerResults;
  }
}
