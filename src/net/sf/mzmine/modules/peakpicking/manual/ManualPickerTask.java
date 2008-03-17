/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.manual;

import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ScanUtils;

// TODO: zero intensity data points may be added to the end of the peak, this
// should be fixed

class ManualPickerTask implements Task {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private TaskStatus status;
    private String errorMessage;

    private int processedScans;
    private int totalScans;

    private PeakListRow selectedRow;
    private RawDataFile selectedFile;
    private float minRT, maxRT, minMZ, maxMZ;

    ManualPickerTask(PeakListRow selectedRow, RawDataFile selectedFile,
            float minRT, float maxRT, float minMZ, float maxMZ) {

        status = TaskStatus.WAITING;

        this.selectedRow = selectedRow;
        this.selectedFile = selectedFile;

        this.minRT = minRT;
        this.maxRT = maxRT;
        this.minMZ = minMZ;
        this.maxMZ = maxMZ;

    }

    public void cancel() {
        status = TaskStatus.CANCELED;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public float getFinishedPercentage() {
        if (totalScans == 0)
            return 0.0f;
        return (float) processedScans / totalScans;
    }

    public Object getResult() {
        return null;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getTaskDescription() {
        return "Manually picking a peak from " + selectedFile;
    }

    public void run() {

        /*status = TaskStatus.PROCESSING;

        logger.finest("Starting manual peak picker, RT: " + minRT + " - "
                + maxRT + ", m/z: " + minMZ + " - " + maxMZ);

        int[] scanNumbers = selectedFile.getScanNumbers(1, minRT, maxRT);
        totalScans = scanNumbers.length;

        ManualPeak ucPeak = new ManualPeak(selectedFile);
        boolean dataPointFound = false;

        for (int i = 0; i < totalScans; i++) {

            if (status == TaskStatus.CANCELED)
                return;

            // Get next scan
            Scan scan = selectedFile.getScan(scanNumbers[i]);

            DataPoint basePeak = ScanUtils.findBasePeak(scan, minMZ, maxMZ);

            if (basePeak != null) {
                dataPointFound = true;
                ucPeak.addDatapoint(scan.getScanNumber(), basePeak.getMZ(),
                        scan.getRetentionTime(), basePeak.getIntensity());
            } else if (dataPointFound) {
                ucPeak.addDatapoint(scan.getScanNumber(), ucPeak.getMZ(),
                        scan.getRetentionTime(), 0f);
            }

            processedScans++;

        }

        ucPeak.finalizedAddingDatapoints(PeakStatus.MANUAL);

        if (ucPeak.getHeight() > 0) 
            selectedRow.addPeak(selectedFile, ucPeak, ucPeak);

        logger.finest("Finished manual peak picker, " + processedScans
                + " scans processed");

        status = TaskStatus.FINISHED;
*/
    }

}
