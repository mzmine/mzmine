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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.rawdatamethods.filtering.cropper;

import java.util.logging.Logger;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class CropFilterTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private MZmineProject project;
    private RawDataFile dataFile;
    private int processedScans, totalScans;
    private Scan[] scans;

    // User parameters
    private ScanSelection scanSelection;
    private Range<Double> mzRange;
    private String suffix;
    private boolean removeOriginal;

    CropFilterTask(MZmineProject project, RawDataFile dataFile,
            ParameterSet parameters) {
        this.project = project;
        this.dataFile = dataFile;

        this.scanSelection = parameters
                .getParameter(CropFilterParameters.scanSelection).getValue();
        this.mzRange = parameters.getParameter(CropFilterParameters.mzRange)
                .getValue();
        this.suffix = parameters.getParameter(CropFilterParameters.suffix)
                .getValue();
        this.removeOriginal = parameters
                .getParameter(CropFilterParameters.autoRemove).getValue();
    }

    /**
     * @see Runnable#run()
     */
    @Override
    public void run() {

        setStatus(TaskStatus.PROCESSING);

        logger.info("Started crop filter on " + dataFile);

        scans = scanSelection.getMatchingScans(dataFile);
        totalScans = scans.length;

        // Check if we have any scans
        if (totalScans == 0) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("No scans match the selected criteria");
            return;
        }

        try {

            RawDataFileWriter rawDataFileWriter = MZmineCore
                    .createNewFile(dataFile.getName() + " " + suffix);

            for (Scan scan : scans) {

                SimpleScan scanCopy = new SimpleScan(scan);

                // Check if we have something to crop
                if (!mzRange.encloses(scan.getDataPointMZRange())) {
                    DataPoint croppedDataPoints[] = scan
                            .getDataPointsByMass(mzRange);
                    scanCopy.setDataPoints(croppedDataPoints);
                }

                rawDataFileWriter.addScan(scanCopy);

                processedScans++;
            }

            RawDataFile filteredRawDataFile = rawDataFileWriter.finishWriting();
            project.addFile(filteredRawDataFile);

            // Remove the original file if requested
            if (removeOriginal) {
                project.removeFile(dataFile);
            }

            setStatus(TaskStatus.FINISHED);

        } catch (Exception e) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage(e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public double getFinishedPercentage() {
        if (totalScans == 0)
            return 0;
        return (double) processedScans / totalScans;
    }

    @Override
    public String getTaskDescription() {
        return "Cropping file " + dataFile.getName();
    }

}
