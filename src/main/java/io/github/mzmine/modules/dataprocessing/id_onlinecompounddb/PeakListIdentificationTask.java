/*
 * Copyright 2006-2020 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_onlinecompounddb;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.gui.impl.HeadLessDesktop;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.tools.isotopepatternscore.IsotopePatternScoreCalculator;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.PeakListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;

public class PeakListIdentificationTask extends AbstractTask {

    // Logger.
    private static final Logger LOG = Logger
            .getLogger(PeakListIdentificationTask.class.getName());

    // Minimum abundance.
    private static final double MIN_ABUNDANCE = 0.001;

    // Counters.
    private int finishedItems;
    private int numItems;

    private final MZmineProcessingStep<OnlineDatabases> db;
    private final MZTolerance mzTolerance;
    private final int numOfResults;
    private final PeakList peakList;
    private final boolean isotopeFilter;
    private final ParameterSet isotopeFilterParameters;
    private final IonizationType ionType;
    private DBGateway gateway;
    private PeakListRow currentRow;

    /**
     * Create the identification task.
     * 
     * @param parameters
     *            task parameters.
     * @param list
     *            feature list to operate on.
     */
    PeakListIdentificationTask(final ParameterSet parameters,
            final PeakList list) {

        peakList = list;
        numItems = 0;
        finishedItems = 0;
        gateway = null;
        currentRow = null;

        db = parameters.getParameter(SingleRowIdentificationParameters.DATABASE)
                .getValue();
        mzTolerance = parameters
                .getParameter(SingleRowIdentificationParameters.MZ_TOLERANCE)
                .getValue();
        numOfResults = parameters
                .getParameter(SingleRowIdentificationParameters.MAX_RESULTS)
                .getValue();
        isotopeFilter = parameters
                .getParameter(SingleRowIdentificationParameters.ISOTOPE_FILTER)
                .getValue();
        isotopeFilterParameters = parameters
                .getParameter(SingleRowIdentificationParameters.ISOTOPE_FILTER)
                .getEmbeddedParameters();
        ionType = parameters
                .getParameter(PeakListIdentificationParameters.ionizationType)
                .getValue();
    }

    @Override
    public double getFinishedPercentage() {

        return numItems == 0 ? 0.0 : (double) finishedItems / (double) numItems;
    }

    @Override
    public String getTaskDescription() {

        return "Identification of peaks in " + peakList
                + (currentRow == null ? " using " + db
                        : " (" + MZmineCore.getConfiguration().getMZFormat()
                                .format(currentRow.getAverageMZ())
                                + " m/z) using " + db);
    }

    @Override
    public void run() {

        if (!isCanceled()) {
            try {

                setStatus(TaskStatus.PROCESSING);

                // Create database gateway.
                gateway = db.getModule().getGatewayClass().newInstance();

                // Identify the feature list rows starting from the biggest
                // peaks.
                final PeakListRow[] rows = peakList.getRows();
                Arrays.sort(rows, new PeakListRowSorter(SortingProperty.Area,
                        SortingDirection.Descending));

                // Initialize counters.
                numItems = rows.length;

                // Process rows.
                for (finishedItems = 0; !isCanceled()
                        && finishedItems < numItems; finishedItems++) {

                    // Retrieve results for each row.
                    retrieveIdentification(rows[finishedItems]);
                }

                if (!isCanceled()) {
                    setStatus(TaskStatus.FINISHED);
                }
            } catch (Throwable t) {

                final String msg = "Could not search " + db;
                LOG.log(Level.WARNING, msg, t);
                setStatus(TaskStatus.ERROR);
                setErrorMessage(
                        msg + ": " + ExceptionUtils.exceptionToString(t));
            }
        }
    }

    /**
     * Search the database for the peak's identity.
     * 
     * @param row
     *            the feature list row.
     * @throws IOException
     *             if there are i/o problems.
     */
    private void retrieveIdentification(final PeakListRow row)
            throws IOException {

        currentRow = row;

        // Determine peak charge.
        final Feature bestPeak = row.getBestPeak();
        int charge = bestPeak.getCharge();
        if (charge <= 0) {
            charge = 1;
        }

        // Calculate mass value.

        final double massValue = row.getAverageMZ() * (double) charge
                - ionType.getAddedMass();

        // Isotope pattern.
        final IsotopePattern rowIsotopePattern = bestPeak.getIsotopePattern();

        // Process each one of the result ID's.
        final String[] findCompounds = gateway.findCompounds(massValue,
                mzTolerance, numOfResults, db.getParameterSet());

        for (int i = 0; !isCanceled() && i < findCompounds.length; i++) {

            final DBCompound compound = gateway.getCompound(findCompounds[i],
                    db.getParameterSet());

            // In case we failed to retrieve data, skip this compound
            if (compound == null)
                continue;

            final String formula = compound
                    .getPropertyValue(PeakIdentity.PROPERTY_FORMULA);

            // If required, check isotope score.
            if (isotopeFilter && rowIsotopePattern != null && formula != null) {

                // First modify the formula according to ionization.
                final String adjustedFormula = FormulaUtils
                        .ionizeFormula(formula, ionType, charge);

                LOG.finest("Calculating isotope pattern for compound formula "
                        + formula + " adjusted to " + adjustedFormula);

                // Generate IsotopePattern for this compound
                final IsotopePattern compoundIsotopePattern = IsotopePatternCalculator
                        .calculateIsotopePattern(adjustedFormula, MIN_ABUNDANCE,
                                charge, ionType.getPolarity());

                // Check isotope pattern match
                boolean check = IsotopePatternScoreCalculator.checkMatch(
                        rowIsotopePattern, compoundIsotopePattern,
                        isotopeFilterParameters);

                if (!check)
                    continue;
            }

            // Add the retrieved identity to the feature list row
            row.addPeakIdentity(compound, false);

            // Notify the GUI about the change in the project
            MZmineCore.getProjectManager().getCurrentProject()
                    .notifyObjectChanged(row, false);
            // Repaint the window to reflect the change in the feature list
            Desktop desktop = MZmineCore.getDesktop();
            if (!(desktop instanceof HeadLessDesktop))
                desktop.getMainWindow().repaint();
        }
    }
}
