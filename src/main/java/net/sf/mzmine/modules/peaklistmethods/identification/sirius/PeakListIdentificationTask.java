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

  // Counters.
  private int finishedItems;
  private int numItems;

  private final MZTolerance mzTolerance;
  private final PeakList peakList;
  private final IonizationType ionType;
  private final double parentMass;
  private final MolecularFormulaRange range;
  private final int siriusCandidates;
  private final int fingeridCandidates;
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

    mzTolerance = parameters.getParameter(PeakListIdentificationParameters.MZ_TOLERANCE).getValue();
    ionType = parameters.getParameter(PeakListIdentificationParameters.NEUTRAL_MASS).getIonType();
    parentMass = parameters.getParameter(PeakListIdentificationParameters.NEUTRAL_MASS).getValue();
    range = parameters.getParameter(PeakListIdentificationParameters.ELEMENTS).getValue();
    siriusCandidates = parameters.getParameter(PeakListIdentificationParameters.SIRIUS_CANDIDATES).getValue();
    fingeridCandidates = parameters.getParameter(PeakListIdentificationParameters.FINGERID_CANDIDATES).getValue();
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

        // Identify the peak list rows starting from the biggest peaks.
        final PeakListRow[] rows = peakList.getRows();
        Arrays.sort(rows, new PeakListRowSorter(SortingProperty.Area, SortingDirection.Descending));

        // Initialize counters.
        numItems = rows.length;

        // Process rows.
        for (finishedItems = 0; !isCanceled() && finishedItems < numItems; finishedItems++) {
          //TODO: add code here
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

    Feature bestPeak = row.getBestPeak();
    SpectrumProcessing processor = new SpectrumProcessing(bestPeak);

    // Add the retrieved identity to the peak list row
//    row.addPeakIdentity(compound, false);

    // Notify the GUI about the change in the project
    MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row, false);
    // Repaint the window to reflect the change in the peak list
    Desktop desktop = MZmineCore.getDesktop();
    if (!(desktop instanceof HeadLessDesktop))
      desktop.getMainWindow().repaint();
  }
}
