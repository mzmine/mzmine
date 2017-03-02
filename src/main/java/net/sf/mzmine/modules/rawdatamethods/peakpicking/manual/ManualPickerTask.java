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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.manual;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.modules.visualization.peaklisttable.table.PeakListTable;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ScanUtils;

import com.google.common.collect.Range;

class ManualPickerTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private int processedScans, totalScans;

    private final MZmineProject project;
    private final PeakListTable table;
    private final PeakList peakList;
    private PeakListRow peakListRow;
    private RawDataFile dataFiles[];
    private Range<Double> rtRange, mzRange;

    ManualPickerTask(MZmineProject project, PeakListRow peakListRow,
            RawDataFile dataFiles[], ManualPickerParameters parameters,
            PeakList peakList, PeakListTable table) {

        this.project = project;
        this.peakListRow = peakListRow;
        this.dataFiles = dataFiles;
        this.peakList = peakList;
        this.table = table;

        rtRange = parameters
                .getParameter(ManualPickerParameters.retentionTimeRange)
                .getValue();
        mzRange = parameters.getParameter(ManualPickerParameters.mzRange)
                .getValue();


    }

    public double getFinishedPercentage() {
        if (totalScans == 0)
            return 0;
        return (double) processedScans / totalScans;
    }

    public String getTaskDescription() {
        return "Manually picking peaks from " + Arrays.toString(dataFiles);
    }

    public void run() {

        setStatus(TaskStatus.PROCESSING);

        logger.finest("Starting manual peak picker, RT: " + rtRange + ", m/z: "
                + mzRange);

        // Calculate total number of scans to process
        for (RawDataFile dataFile : dataFiles) {
            int[] scanNumbers = dataFile.getScanNumbers(1, rtRange);
            totalScans += scanNumbers.length;
        }

        // Find peak in each data file
        for (RawDataFile dataFile : dataFiles) {

            ManualPeak newPeak = new ManualPeak(dataFile);
            boolean dataPointFound = false;

            int[] scanNumbers = dataFile.getScanNumbers(1, rtRange);

            for (int scanNumber : scanNumbers) {

                if (isCanceled())
                    return;

                // Get next scan
                Scan scan = dataFile.getScan(scanNumber);

                // Find most intense m/z peak
                DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);

                if (basePeak != null) {
                    if (basePeak.getIntensity() > 0)
                        dataPointFound = true;
                    newPeak.addDatapoint(scan.getScanNumber(), basePeak);
                } else {
                    final double mzCenter = (mzRange.lowerEndpoint()
                            + mzRange.upperEndpoint()) / 2.0;
                    DataPoint fakeDataPoint = new SimpleDataPoint(mzCenter, 0);
                    newPeak.addDatapoint(scan.getScanNumber(), fakeDataPoint);
                }

                processedScans++;

            }

            if (dataPointFound) {
                newPeak.finalizePeak();
                if (newPeak.getArea() > 0)
                    peakListRow.addPeak(dataFile, newPeak);
            } else {
                peakListRow.removePeak(dataFile);
            }

        }

        // Notify the GUI that peaklist contents have changed
        if (peakList != null) {
            // Check if the peak list row has been added to the peak list, and
            // if it has not, add it
            List<PeakListRow> rows = Arrays.asList(peakList.getRows());
            if (!rows.contains(peakListRow)) {
                peakList.addRow(peakListRow);
            }

            // Add quality parameters to peaks
            QualityParameters.calculateQualityParameters(peakList);

            project.notifyObjectChanged(peakList, true);
        }
        if (table != null) {
            ((AbstractTableModel) table.getModel()).fireTableDataChanged();
        }

        logger.finest("Finished manual peak picker, " + processedScans
                + " scans processed");

        setStatus(TaskStatus.FINISHED);

    }

}
