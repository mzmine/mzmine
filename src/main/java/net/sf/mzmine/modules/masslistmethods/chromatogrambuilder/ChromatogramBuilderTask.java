/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.masslistmethods.chromatogrambuilder;

import java.util.Arrays;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class ChromatogramBuilderTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private MZmineProject project;
    private RawDataFile dataFile;

    // scan counter
    private int processedScans = 0, totalScans;
    private ScanSelection scanSelection;
    private int newPeakID = 1;
    private Scan[] scans;

    // User parameters
    private String suffix, massListName;
    private MZTolerance mzTolerance;
    private double minimumTimeSpan, minimumHeight;

    private SimplePeakList newPeakList;

    /**
     * @param dataFile
     * @param parameters
     */
    public ChromatogramBuilderTask(MZmineProject project, RawDataFile dataFile,
            ParameterSet parameters) {

        this.project = project;
        this.dataFile = dataFile;
        this.scanSelection = parameters
                .getParameter(ChromatogramBuilderParameters.scanSelection)
                .getValue();
        this.massListName = parameters
                .getParameter(ChromatogramBuilderParameters.massList)
                .getValue();

        this.mzTolerance = parameters
                .getParameter(ChromatogramBuilderParameters.mzTolerance)
                .getValue();
        this.minimumTimeSpan = parameters
                .getParameter(ChromatogramBuilderParameters.minimumTimeSpan)
                .getValue();
        this.minimumHeight = parameters
                .getParameter(ChromatogramBuilderParameters.minimumHeight)
                .getValue();

        this.suffix = parameters
                .getParameter(ChromatogramBuilderParameters.suffix).getValue();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Detecting chromatograms in " + dataFile;
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalScans == 0)
            return 0;
        else
            return (double) processedScans / totalScans;
    }

    public RawDataFile getDataFile() {
        return dataFile;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        setStatus(TaskStatus.PROCESSING);

        logger.info("Started chromatogram builder on " + dataFile);

        scans = scanSelection.getMatchingScans(dataFile);
        int allScanNumbers[] = scanSelection.getMatchingScanNumbers(dataFile);
        totalScans = scans.length;

        // Check if the scans are properly ordered by RT
        double prevRT = Double.NEGATIVE_INFINITY;
        for (Scan s : scans) {
            if (s.getRetentionTime() < prevRT) {
                setStatus(TaskStatus.ERROR);
                final String msg = "Retention time of scan #"
                        + s.getScanNumber()
                        + " is smaller then the retention time of the previous scan."
                        + " Please make sure you only use scans with increasing retention times."
                        + " You can restrict the scan numbers in the parameters, or you can use the Crop filter module";
                setErrorMessage(msg);
                return;
            }
            prevRT = s.getRetentionTime();
        }

        // Create new peak list
        newPeakList = new SimplePeakList(dataFile + " " + suffix, dataFile);

        Chromatogram[] chromatograms;
        HighestDataPointConnector massConnector = new HighestDataPointConnector(
                dataFile, allScanNumbers, minimumTimeSpan, minimumHeight,
                mzTolerance);

        for (Scan scan : scans) {

            if (isCanceled())
                return;

            MassList massList = scan.getMassList(massListName);
            if (massList == null) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Scan " + dataFile + " #" + scan.getScanNumber()
                        + " does not have a mass list " + massListName);
                return;
            }

            DataPoint mzValues[] = massList.getDataPoints();

            if (mzValues == null) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Mass list " + massListName
                        + " does not contain m/z values for scan #"
                        + scan.getScanNumber() + " of file " + dataFile);
                return;
            }

            massConnector.addScan(scan.getScanNumber(), mzValues);
            processedScans++;
        }

        chromatograms = massConnector.finishChromatograms();

        // Sort the final chromatograms by m/z
        Arrays.sort(chromatograms,
                new PeakSorter(SortingProperty.MZ, SortingDirection.Ascending));

        // Add the chromatograms to the new peak list
        for (Feature finishedPeak : chromatograms) {
            SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
            newPeakID++;
            newRow.addPeak(dataFile, finishedPeak);
            newPeakList.addRow(newRow);
        }

        // Add new peaklist to the project
        project.addPeakList(newPeakList);

        // Add quality parameters to peaks
        QualityParameters.calculateQualityParameters(newPeakList);

        setStatus(TaskStatus.FINISHED);

        logger.info("Finished chromatogram builder on " + dataFile);

    }

}
