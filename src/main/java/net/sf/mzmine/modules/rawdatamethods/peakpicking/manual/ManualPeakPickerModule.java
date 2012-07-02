/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

import javax.annotation.Nonnull;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.Range;

public class ManualPeakPickerModule implements MZmineModule {

    /**
     * @see net.sf.mzmine.modules.MZmineProcessingModule#getName()
     */
    public @Nonnull String getName() {
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
	    if ((mzRange == null) || (rtRange == null)) {
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
		if ((mzRange == null) || (rtRange == null)) {
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
	    parameters.getParameter(ManualPickerParameters.retentionTimeRange)
		    .setValue(rtRange);
	    parameters.getParameter(ManualPickerParameters.mzRange).setValue(
		    mzRange);
	}

	ExitCode exitCode = parameters.showSetupDialog();

	if (exitCode != ExitCode.OK)
	    return;

	ManualPickerTask task = new ManualPickerTask(peakListRow, dataFiles,
		parameters);

	MZmineCore.getTaskController().addTask(task);

    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
	return ManualPickerParameters.class;
    }

}
