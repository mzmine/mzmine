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

package io.github.mzmine.modules.dataprocessing.id_complexsearch;

import java.util.Arrays;
import java.util.logging.Logger;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.impl.SimplePeakList;
import io.github.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import io.github.mzmine.gui.Desktop;
import io.github.mzmine.gui.impl.HeadLessDesktop;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.PeakListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;

public class ComplexSearchTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private int finishedRows, totalRows;
    private PeakList peakList;

    private RTTolerance rtTolerance;
    private MZTolerance mzTolerance;
    private double maxComplexHeight;
    private IonizationType ionType;
    private ParameterSet parameters;

    /**
     * @param parameters
     * @param peakList
     */
    public ComplexSearchTask(ParameterSet parameters, PeakList peakList) {

        this.peakList = peakList;
        this.parameters = parameters;

        ionType = parameters
                .getParameter(ComplexSearchParameters.ionizationMethod)
                .getValue();
        rtTolerance = parameters
                .getParameter(ComplexSearchParameters.rtTolerance).getValue();
        mzTolerance = parameters
                .getParameter(ComplexSearchParameters.mzTolerance).getValue();
        maxComplexHeight = parameters
                .getParameter(ComplexSearchParameters.maxComplexHeight)
                .getValue();

    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalRows == 0)
            return 0;
        return ((double) finishedRows) / totalRows;
    }

    /**
     * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Identification of complexes in " + peakList;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    public void run() {

        setStatus(TaskStatus.PROCESSING);

        logger.info("Starting complex search in " + peakList);

        PeakListRow rows[] = peakList.getRows();
        totalRows = rows.length;

        // Sort the array by m/z so we start with biggest peak (possible
        // complex)
        Arrays.sort(rows, new PeakListRowSorter(SortingProperty.MZ,
                SortingDirection.Descending));

        // Compare each three rows against each other
        for (int i = 0; i < totalRows; i++) {

            Range<Double> testRTRange = rtTolerance
                    .getToleranceRange(rows[i].getAverageRT());
            PeakListRow testRows[] = peakList
                    .getRowsInsideScanRange(testRTRange);

            for (int j = 0; j < testRows.length; j++) {

                for (int k = j; k < testRows.length; k++) {

                    // Task canceled?
                    if (isCanceled())
                        return;

                    // To avoid finding a complex of the peak itself and another
                    // very small m/z peak
                    if ((rows[i] == testRows[j]) || (rows[i] == testRows[k]))
                        continue;

                    if (checkComplex(rows[i], testRows[j], testRows[k]))
                        addComplexInfo(rows[i], testRows[j], testRows[k]);

                }

            }

            finishedRows++;

        }

        // Add task description to peakList
        ((SimplePeakList) peakList).addDescriptionOfAppliedTask(
                new SimplePeakListAppliedMethod("Identification of complexes",
                        parameters));

        // Repaint the window to reflect the change in the feature list
        Desktop desktop = MZmineCore.getDesktop();
        if (!(desktop instanceof HeadLessDesktop))
            desktop.getMainWindow().repaint();

        setStatus(TaskStatus.FINISHED);

        logger.info("Finished complexes search in " + peakList);

    }

    /**
     * Check if candidate peak may be a possible complex of given two peaks
     * 
     */
    private boolean checkComplex(PeakListRow complexRow, PeakListRow row1,
            PeakListRow row2) {

        // Check retention time condition
        Range<Double> rtRange = rtTolerance
                .getToleranceRange(complexRow.getAverageRT());
        if (!rtRange.contains(row1.getAverageRT()))
            return false;
        if (!rtRange.contains(row2.getAverageRT()))
            return false;

        // Check mass condition
        double expectedMass = row1.getAverageMZ() + row2.getAverageMZ()
                - (2 * ionType.getAddedMass());
        double detectedMass = complexRow.getAverageMZ()
                - ionType.getAddedMass();
        Range<Double> mzRange = mzTolerance.getToleranceRange(detectedMass);
        if (!mzRange.contains(expectedMass))
            return false;

        // Check height condition
        if ((complexRow.getAverageHeight() > row1.getAverageHeight()
                * maxComplexHeight)
                || (complexRow.getAverageHeight() > row2.getAverageHeight()
                        * maxComplexHeight))
            return false;

        return true;

    }

    /**
     * Add new identity to the complex row
     * 
     * @param mainRow
     * @param fragmentRow
     */
    private void addComplexInfo(PeakListRow complexRow, PeakListRow row1,
            PeakListRow row2) {
        ComplexIdentity newIdentity = new ComplexIdentity(row1, row2);
        complexRow.addPeakIdentity(newIdentity, false);

        // Notify the GUI about the change in the project
        MZmineCore.getProjectManager().getCurrentProject()
                .notifyObjectChanged(complexRow, false);
    }

}
