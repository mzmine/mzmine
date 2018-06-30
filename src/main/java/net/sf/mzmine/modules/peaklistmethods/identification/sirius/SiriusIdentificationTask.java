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
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.slf4j.LoggerFactory;

public class SiriusIdentificationTask extends AbstractTask {

  // Logger.
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SiriusIdentificationTask.class);


  // Minimum abundance.
  private static final double MIN_ABUNDANCE = 0.001;

  // Counters.
  private int finishedItems;
  private int numItems;

  private final MZTolerance mzTolerance;
  private final int numOfResults;
  private final PeakList peakList;
  private final IonizationType ionType;
  private final double parentMass;
  private final MolecularFormulaRange range;
  private PeakListRow currentRow;

  /**
   * Create the identification task.
   * 
   * @param parameters task parameters.
   * @param list peak list to operate on.
   */
  SiriusIdentificationTask(final ParameterSet parameters, final PeakList list) {

    peakList = list;
    numItems = 0;
    finishedItems = 0;
    currentRow = null;

    mzTolerance =
        parameters.getParameter(SiriusParameters.MZ_TOLERANCE).getValue();
    numOfResults =
        parameters.getParameter(SiriusParameters.MAX_RESULTS).getValue();
    ionType = parameters.getParameter(SiriusParameters.NEUTRAL_MASS).getIonType();
    parentMass = parameters.getParameter(SiriusParameters.PARENT_MASS).getValue();
    range = parameters.getParameter(SiriusParameters.ELEMENTS).getValue();
  }

  @Override
  public double getFinishedPercentage() {

    return numItems == 0 ? 0.0 : (double) finishedItems / (double) numItems;
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

        // Create database gateway.

        // Identify the peak list rows starting from the biggest peaks.
        final PeakListRow[] rows = peakList.getRows();
        Arrays.sort(rows, new PeakListRowSorter(SortingProperty.Area, SortingDirection.Descending));

        // Initialize counters.
        numItems = rows.length;

        // Process rows.
        for (finishedItems = 0; !isCanceled() && finishedItems < numItems; finishedItems++) {

          processSpectra(rows[finishedItems]);

        }

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

//    final Feature bestPeak = row.getBestPeak();
//    int charge = bestPeak.getCharge();
//    if (charge <= 0) {
//      charge = 1;
//    }

    // Calculate mass value.

//    final double massValue = row.getAverageMZ() * (double) charge - ionType.getAddedMass();

    // Isotope pattern.
//    final IsotopePattern rowIsotopePattern = bestPeak.getIsotopePattern();

    double ppm = mzTolerance.getPpmTolerance();
    SimpleMsSpectrum spectrum = new SimpleMsSpectrum();
    Feature[] peaks = row.getPeaks();
    double mz[] = new double[peaks.length];
    float intensity[] = new float[peaks.length];
    IonType siriusIonType = IonTypeUtil.createIonType(ionType.toString());

    for (int i = 0; i < peaks.length; i++) {
      mz[i] = peaks[i].getMZ();
      intensity[i] = (float) peaks[i].getHeight();
    }

    spectrum.setDataPoints(mz, intensity, peaks.length);
    List<MsSpectrum> ms1 = null, ms2 = null;

    if (true) {
      ms2 = new LinkedList<>();
      ms2.add(spectrum);
    }


    ConstraintsGenerator constraintsGenerator = new ConstraintsGenerator();
    FormulaConstraints constraints = constraintsGenerator.generateConstraint(range);
    SiriusIdentificationMethod siriusMethod = new SiriusIdentificationMethod(
        ms1,
        ms2,
        parentMass,
        siriusIonType,
        numOfResults,
        constraints,
        ppm
    );


    List<IonAnnotation> siriusAnnotations = null;
    List<IonAnnotation> fingerAnnotations = null;
    try {
      siriusAnnotations = siriusMethod.execute();
    } catch (MSDKException e) {
      e.printStackTrace();
      System.out.println("Hell is here");
    }


    Ms2Experiment experiment = siriusMethod.getExperiment();
    SiriusIonAnnotation siriusAnnotation = (SiriusIonAnnotation) siriusAnnotations.get(0);

    FingerIdWebMethod fingerMethod = null;
    try {
      fingerMethod = new FingerIdWebMethod(experiment, siriusAnnotation, 10);
      fingerAnnotations = fingerMethod.execute();
    } catch (Exception e) {
      logger.error("Exception!&@!#$^&@#$");
      System.out.println("Panic");
    }

    IonAnnotation best = fingerAnnotations.get(0);

    SiriusCompound compound = new SiriusCompound(best, 10.);
    // Add the retrieved identity to the peak list row
    row.addPeakIdentity(compound, false);

    // Notify the GUI about the change in the project
    MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row, false);
    // Repaint the window to reflect the change in the peak list
    Desktop desktop = MZmineCore.getDesktop();
    if (!(desktop instanceof HeadLessDesktop))
      desktop.getMainWindow().repaint();
  }
}
