package net.sf.mzmine.modules.peaklistmethods.identification.sirius;/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Single;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.IonAnnotation;
import io.github.msdk.id.sirius.FingerIdWebMethod;
import io.github.msdk.id.sirius.SiriusIonAnnotation;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FingerIdWebMethodTask extends AbstractTask {
  private FingerIdWebMethod method;
  private List<IonAnnotation> fingerResults = null;
  private final int candidatesAmount;
  private final Ms2Experiment experiment;
  private final SiriusIonAnnotation annotation;
  private final CountDownLatch latch;
  private final String formula;
  private final ResultWindow window;

  private static final Logger logger = LoggerFactory.getLogger(FingerIdWebMethodTask.class);

  public FingerIdWebMethodTask(SiriusIonAnnotation annotation, Ms2Experiment experiment, Integer candidatesAmount, CountDownLatch latch, ResultWindow window) {
    this.candidatesAmount = candidatesAmount;
    this.experiment = experiment;
    this.annotation = annotation;
    this.latch = latch;
    this.window = window;
    formula = MolecularFormulaManipulator.getString(annotation.getFormula());
  }

  @Override
  public String getTaskDescription() {
    return String.format("Processing element %s by FingerIdWebMethod", formula);
  }

  @Override
  public double getFinishedPercentage() {
    if (method == null || method.getFinishedPercentage() == null)
      return 0;
    return method.getFinishedPercentage();
  }

  @Override
  public void run()  {
    try {
      method = new FingerIdWebMethod(experiment, annotation, candidatesAmount);
      fingerResults = method.execute();

      logger.debug("Successfully processed {} by FingerWebMethod", formula);
    } catch (RuntimeException e) {
      logger.error("Error during processing FingerIdWebMethod --- return initial compound");
      e.printStackTrace();

      fingerResults = new LinkedList<>();
      fingerResults.add(annotation);
    } catch (MSDKException msdk) {
      logger.error("Internal FingerIdWebMethod error occured.");
      msdk.printStackTrace();
    }

    setStatus(TaskStatus.FINISHED);
    SingleRowIdentificationTask.addListItems(window, fingerResults);
//    latch.countDown();
  }

  public List<IonAnnotation> getResults() {
    return fingerResults;
  }
}
