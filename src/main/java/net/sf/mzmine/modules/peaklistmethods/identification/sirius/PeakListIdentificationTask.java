/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.IsotopePattern;
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

public class PeakListIdentificationTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(
      PeakListIdentificationTask.class.getName());

  // Minimum abundance.
  private static final double MIN_ABUNDANCE = 0.001;

  // Counters.
  private int finishedItems;
  private int numItems;

  private final MZTolerance mzTolerance;
  private final int numOfResults;
  private final PeakList peakList;
  private final boolean isotopeFilter;
  private final ParameterSet isotopeFilterParameters;
  private final IonizationType ionType;
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

    mzTolerance =
        parameters.getParameter(SiriusParameters.MZ_TOLERANCE).getValue();
    numOfResults =
        parameters.getParameter(SiriusParameters.MAX_RESULTS).getValue();
    isotopeFilter =
        parameters.getParameter(SiriusParameters.ISOTOPE_FILTER).getValue();
    isotopeFilterParameters = parameters
        .getParameter(SiriusParameters.ISOTOPE_FILTER).getEmbeddedParameters();
    ionType = parameters.getParameter(SiriusParameters.ionizationType).getValue();
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
                + " m/z) using ");
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

          // Retrieve results for each row.
          retrieveIdentification(rows[finishedItems]);
        }

        if (!isCanceled()) {
          setStatus(TaskStatus.FINISHED);
        }
      } catch (Throwable t) {

        final String msg = "Could not search ";
        LOG.log(Level.WARNING, msg, t);
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
  private void retrieveIdentification(final PeakListRow row) throws IOException {

    currentRow = row;

    // Determine peak charge.
    final Feature bestPeak = row.getBestPeak();
    int charge = bestPeak.getCharge();
    if (charge <= 0) {
      charge = 1;
    }

    // Calculate mass value.

    final double massValue = row.getAverageMZ() * (double) charge - ionType.getAddedMass();

    // Isotope pattern.
    final IsotopePattern rowIsotopePattern = bestPeak.getIsotopePattern();

    // Process each one of the result ID's.
//    final String[] findCompounds =
//        gateway.findCompounds(massValue, mzTolerance, numOfResults, db.getParameterSet());

//    for (int i = 0; !isCanceled() && i < findCompounds.length; i++) {
//
//      final DBCompound compound = gateway.getCompound(findCompounds[i], db.getParameterSet());
//
//       In case we failed to retrieve data, skip this compound
//      if (compound == null)
//        continue;
//
//      final String formula = compound.getPropertyValue(PeakIdentity.PROPERTY_FORMULA);
//
//       If required, check isotope score.
//      if (isotopeFilter && rowIsotopePattern != null && formula != null) {
//
//         First modify the formula according to ionization.
//        final String adjustedFormula = FormulaUtils.ionizeFormula(formula, ionType, charge);
//
//        LOG.finest("Calculating isotope pattern for compound formula " + formula + " adjusted to "
//            + adjustedFormula);
//
//         Generate IsotopePattern for this compound
//        final IsotopePattern compoundIsotopePattern = IsotopePatternCalculator
//            .calculateIsotopePattern(adjustedFormula, MIN_ABUNDANCE, charge, ionType.getPolarity());
//
        // Check isotope pattern match
//        boolean check = IsotopePatternScoreCalculator.checkMatch(rowIsotopePattern,
//            compoundIsotopePattern, isotopeFilterParameters);
//
//        if (!check)
//          continue;
//      }
//
      // Add the retrieved identity to the peak list row
//      row.addPeakIdentity(compound, false);

      // Notify the GUI about the change in the project
      MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row, false);
      // Repaint the window to reflect the change in the peak list
      Desktop desktop = MZmineCore.getDesktop();
      if (!(desktop instanceof HeadLessDesktop))
        desktop.getMainWindow().repaint();
  }
}
