/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.manual;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class ManualPeakPicker implements MZmineModule {

    /**
     * @see net.sf.mzmine.main.MZmineModule#initModule(net.sf.mzmine.main.MZmineCore)
     */
    public void initModule() {

    }

    /**
     * @see net.sf.mzmine.modules.BatchStep#toString()
     */
    public String toString() {
        return "Manual peak detector";
    }

    public static void runManualDetection(RawDataFile dataFile,
            PeakListRow peakListRow) {
        runManualDetection(new RawDataFile[] { dataFile }, peakListRow);
    }
    
    public static void runManualDetection(RawDataFile dataFiles[],
            PeakListRow peakListRow) {

        Range mzRange = null, rtRange = null;

        // Check the peaks for selected data files
        for (RawDataFile dataFile : dataFiles) {
            ChromatographicPeak peak = peakListRow.getPeak(dataFile);
            if (peak == null)
                continue;
            if (mzRange == null) {
                mzRange = new Range(peak.getRawDataPointsMZRange());
                rtRange = new Range(peak.getRawDataPointsRTRange());
            } else {
                mzRange.extendRange(peak.getRawDataPointsMZRange());
                rtRange.extendRange(peak.getRawDataPointsRTRange());
            }

        }

        // If none of the data files had a peak, check the whole row
        if (mzRange == null) {
            for (ChromatographicPeak peak : peakListRow.getPeaks()) {
                if (peak == null)
                    continue;
                if (mzRange == null) {
                    mzRange = new Range(peak.getRawDataPointsMZRange());
                    rtRange = new Range(peak.getRawDataPointsRTRange());
                } else {
                    mzRange.extendRange(peak.getRawDataPointsMZRange());
                    rtRange.extendRange(peak.getRawDataPointsRTRange());
                }

            }
        }

        ManualPickerParameters parameters = new ManualPickerParameters();

        if (mzRange != null) {
            parameters.setParameterValue(
                    ManualPickerParameters.retentionTimeRange, rtRange);
            parameters.setParameterValue(ManualPickerParameters.mzRange,
                    mzRange);
        }

        ParameterSetupDialog dialog = new ParameterSetupDialog(
                "Please set parameter values for Manual peak detector",
                parameters);
        dialog.setVisible(true);

        if (dialog.getExitCode() != ExitCode.OK)
            return;

        ManualPickerTask task = new ManualPickerTask(peakListRow, dataFiles,
                parameters);

        MZmineCore.getTaskController().addTask(task);

    }

    public ParameterSet getParameterSet() {
        return null;
    }

    public void setParameters(ParameterSet parameterValues) {
    }

}
