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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.msms;

import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ScanUtils;

import com.google.common.collect.Range;

public class MsMsPeakPickingTask extends AbstractTask {
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private int processedScans, totalScans;

    private final MZmineProject project;
    private final RawDataFile dataFile;
    private double binSize, binTime;
    private final ScanSelection scanSelection;

    private SimplePeakList newPeakList;

    public MsMsPeakPickingTask(MZmineProject project, RawDataFile dataFile,
            ParameterSet parameters) {
        this.project = project;
        this.dataFile = dataFile;
        binSize = parameters.getParameter(MsMsPeakPickerParameters.mzWindow)
                .getValue();
        binTime = parameters.getParameter(MsMsPeakPickerParameters.rtWindow)
                .getValue();

        scanSelection = parameters.getParameter(
                MsMsPeakPickerParameters.scanSelection).getValue();
        newPeakList = new SimplePeakList(dataFile.getName() + " MS/MS peaks",
                dataFile);
    }

    public RawDataFile getDataFile() {
        return dataFile;
    }

    public double getFinishedPercentage() {
        if (totalScans == 0)
            return 0f;
        return (double) processedScans / totalScans;
    }

    public String getTaskDescription() {
        return "Building MS/MS Peaklist based on MS/MS from " + dataFile;
    }

    public void run() {

        setStatus(TaskStatus.PROCESSING);

        final Scan scans[] = scanSelection.getMatchingScans(dataFile);
        totalScans = scans.length;
        for (Scan scan : scans) {
            if (isCanceled())
                return;

            // Get the MS Scan
            Scan bestScan = null;
            Range<Double> rtWindow = Range.closed(scan.getRetentionTime()
                    - (binTime / 2.0), scan.getRetentionTime()
                    + (binTime / 2.0));
            Range<Double> mzWindow = Range.closed(scan.getPrecursorMZ()
                    - (binSize / 2.0), scan.getPrecursorMZ() + (binSize / 2.0));
            DataPoint point;
            DataPoint maxPoint = null;
            int[] regionScanNumbers = dataFile.getScanNumbers(1, rtWindow);
            for (int regionScanNumber : regionScanNumbers) {
                Scan regionScan = dataFile.getScan(regionScanNumber);
                point = ScanUtils.findBasePeak(regionScan, mzWindow);

                // no datapoint found
                if (point == null) {
                    continue;
                }
                if (maxPoint == null) {
                    maxPoint = point;
                }
                int result = Double.compare(maxPoint.getIntensity(),
                        point.getIntensity());
                if (result <= 0) {
                    maxPoint = point;
                    bestScan = regionScan;
                }

            }

            // if no representative dataPoint
            if (bestScan == null) {
                continue;
            }

            assert maxPoint != null;

            SimpleFeature c = new SimpleFeature(dataFile,
                    scan.getPrecursorMZ(), bestScan.getRetentionTime(),
                    maxPoint.getIntensity(), maxPoint.getIntensity(),
                    new int[] { bestScan.getScanNumber() },
                    new DataPoint[] { maxPoint }, FeatureStatus.DETECTED,
                    bestScan.getScanNumber(), scan.getScanNumber(),
                    Range.singleton(bestScan.getRetentionTime()),
                    Range.singleton(scan.getPrecursorMZ()),
                    Range.singleton(maxPoint.getIntensity()));

            PeakListRow entry = new SimplePeakListRow(scan.getScanNumber());
            entry.addPeak(dataFile, c);

            newPeakList.addRow(entry);
            processedScans++;
        }

        project.addPeakList(newPeakList);

        // Add quality parameters to peaks
        QualityParameters.calculateQualityParameters(newPeakList);

        logger.info("Finished MS/MS peak builder on " + dataFile + ", "
                + processedScans + " scans processed");

        setStatus(TaskStatus.FINISHED);
    }

}
