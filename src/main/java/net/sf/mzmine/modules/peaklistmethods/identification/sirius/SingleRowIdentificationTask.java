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

import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.ELEMENTS;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.FINGERID_CANDIDATES;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.MZ_TOLERANCE;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.NEUTRAL_MASS;
import static net.sf.mzmine.modules.peaklistmethods.identification.sirius.SingleRowIdentificationParameters.SIRIUS_CANDIDATES;

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
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleRowIdentificationTask extends AbstractTask {
  public static final NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

  private double searchedMass;
  private MZTolerance mzTolerance;
  private PeakListRow peakListRow;
  private IonizationType ionType;
  private MolecularFormulaRange formulaRange;
  private Double parentMass;
  private Integer fingerCandidates;
  private Integer siriusCandidates;
  private LinkedList<FingerIdWebMethodTask> fingerTasks;

  private static final Logger logger = LoggerFactory.getLogger(SingleRowIdentificationTask.class);


  /**
   * Create the task.
   * 
   * @param parameters task parameters.
   * @param peakListRow peak-list row to identify.
   */
  public SingleRowIdentificationTask(ParameterSet parameters, PeakListRow peakListRow) {

    this.peakListRow = peakListRow;

    searchedMass = parameters.getParameter(NEUTRAL_MASS).getValue();
    mzTolerance = parameters.getParameter(MZ_TOLERANCE).getValue();
    siriusCandidates = parameters.getParameter(SIRIUS_CANDIDATES).getValue();
    fingerCandidates = parameters.getParameter(FINGERID_CANDIDATES).getValue();

    ionType = parameters.getParameter(NEUTRAL_MASS).getIonType();
    parentMass = parameters.getParameter(NEUTRAL_MASS).getValue();

    formulaRange = parameters.getParameter(ELEMENTS).getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    //TODO: refactor
    if (isFinished())
      return 100.0;
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

  //TODO: refactor
  public String getTaskDescription() {
    return "Peak identification of " + massFormater.format(searchedMass) + " using Sirius module";
  }

  /**
   * @see Runnable#run()
   */
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    NumberFormat massFormater = MZmineCore.getConfiguration().getMZFormat();

    ResultWindow window = new ResultWindow(peakListRow, searchedMass, this);
    window.setTitle("Sirius makes fun " + massFormater.format(searchedMass) + " amu");
    window.setVisible(true);

    Feature bestPeak = peakListRow.getBestPeak();
    int ms1index = bestPeak.getRepresentativeScanNumber();
    int ms2index = bestPeak.getMostIntenseFragmentScanNumber();

    RawDataFile rawfile = bestPeak.getDataFile();
    List<MsSpectrum> ms1list = processRawScan(rawfile, ms1index);
    List<MsSpectrum> ms2list = processRawScan(rawfile, ms2index);

    SiriusIdentificationMethod siriusMethod = null;
    try {
      siriusMethod = processSirius(ms1list, ms2list);
    } catch (MSDKException e) {
      logger.error("Internal error of Sirius MSDK module appeared");
      e.printStackTrace();
    }
    /* If code below will failure, then siriusMethod will be null... Unhandled Null-pointer exception? */
    List<IonAnnotation> items = siriusMethod.getResult(); // TODO: use a HEAP to sort items
    //TODO SORT ITEMS BY FINGERID SCORE

    if (rowContainsMsMs(ms2index)) {
      try {
        items = new LinkedList<>();
        fingerTasks = new LinkedList<>();
        CountDownLatch latch = new CountDownLatch(siriusMethod.getResult().size());
        Ms2Experiment experiment = siriusMethod.getExperiment();

      /* // Serial processing
      for (IonAnnotation ia: siriusMethod.getResult()) {
        SiriusIonAnnotation annotation = (SiriusIonAnnotation) ia;
        List<IonAnnotation> fingerResults = processFingerId(annotation, siriusMethod.getExperiment());
        items.addAll(fingerResults);
      } */
        for (IonAnnotation ia : siriusMethod.getResult()) {
          SiriusIonAnnotation annotation = (SiriusIonAnnotation) ia;
          FingerIdWebMethodTask task = new FingerIdWebMethodTask(annotation, experiment, fingerCandidates, latch);
          fingerTasks.add(task);
          MZmineCore.getTaskController().addTask(task);
        }

        latch.await();
        for (FingerIdWebMethodTask t : fingerTasks) {
          items.addAll(t.getResults());
        }

        Thread.sleep(1000);
      } catch (InterruptedException interrupt) {
        logger.error("Processing of FingerWebMethods were interrupted");
        interrupt.printStackTrace();
        items = siriusMethod.getResult();
      }
    }

    addListItems(window, items);
    setStatus(TaskStatus.FINISHED);
  }

  private boolean rowContainsMsMs(int ms2index) {
    return ms2index != -1; // equals -1, if no ms2 spectra is found
  }

  private List<IonAnnotation> processFingerId(SiriusIonAnnotation annotation, Ms2Experiment experiment) {
    //make it as a task
    List<IonAnnotation> methodResults = new LinkedList<>();
    SiriusIonAnnotation siriusAnnotation = (SiriusIonAnnotation) annotation;
    try {
      FingerIdWebMethod method = new FingerIdWebMethod(experiment, siriusAnnotation, fingerCandidates);
      methodResults.addAll(method.execute());
    } catch (RuntimeException r) {
      // No edges exception happened. - probably only ms1 spectrum is used.
      // Return initial item
      methodResults.add(annotation);
    } catch (MSDKException s) {
      logger.error("Error during FingerIdWebMethod processing");
      s.printStackTrace();
      //TODO: refactor
    }

    return methodResults;
  }

  private SiriusIdentificationMethod processSirius(List<MsSpectrum> ms1list, List<MsSpectrum> ms2list) throws MSDKException {
    ConstraintsGenerator generator = new ConstraintsGenerator();
    FormulaConstraints constraints = generator.generateConstraint(formulaRange);
    double ppm = mzTolerance.getPpmTolerance();
    IonType siriusIon = IonTypeUtil.createIonType(ionType.toString());

    SiriusIdentificationMethod siriusMethod = new SiriusIdentificationMethod(
        ms1list,
        ms2list,
        parentMass,
        siriusIon,
        siriusCandidates,
        constraints,
        ppm
    );

    siriusMethod.execute(); // todo: Make it as a task
    return siriusMethod;
  }

  private void addListItems(ResultWindow window, List<IonAnnotation> items) {
    for (IonAnnotation a: items) {
      SiriusIonAnnotation temp = (SiriusIonAnnotation) a;
      SiriusCompound compound = new SiriusCompound(temp, temp.getFingerIdScore());
      window.addNewListItem(compound);
    }
  }

  private List<MsSpectrum> processRawScan(RawDataFile rawfile, int index) {
    LinkedList<MsSpectrum> spectra = null;
    if (index != -1) {
      spectra = new LinkedList<>();
      Scan scan = rawfile.getScan(index);
      DataPoint[] points = scan.getDataPoints();
      MsSpectrum ms = buildSpectrum(points);
      spectra.add(ms);
    }

    return spectra;
  }

  private MsSpectrum buildSpectrum(DataPoint[] points) {
    SimpleMsSpectrum spectrum = new SimpleMsSpectrum();
    double mz[] = new double[points.length];
    float intensity[] = new float[points.length];

    for (int i = 0; i < points.length; i++) {
      mz[i] = points[i].getMZ();
      intensity[i] = (float) points[i].getIntensity();
    }

    spectrum.setDataPoints(mz, intensity, points.length);
    return spectrum;
  }
}
