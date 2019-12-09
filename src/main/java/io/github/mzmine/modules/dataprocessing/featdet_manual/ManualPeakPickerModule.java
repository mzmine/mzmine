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

package io.github.mzmine.modules.dataprocessing.featdet_manual;

import javax.annotation.Nonnull;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.visualization.featurelisttable.table.PeakListTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.ExitCode;

public class ManualPeakPickerModule implements MZmineModule {

    /**
     * @see io.github.mzmine.modules.MZmineProcessingModule#getName()
     */
    public @Nonnull String getName() {
        return "Manual peak detector";
    }

    public static ExitCode runManualDetection(RawDataFile dataFile,
            PeakListRow peakListRow, PeakList peakList, PeakListTable table) {
        return runManualDetection(new RawDataFile[] { dataFile }, peakListRow,
                peakList, table);
    }

    public static ExitCode runManualDetection(RawDataFile dataFiles[],
            PeakListRow peakListRow, PeakList peakList, PeakListTable table) {

        Range<Double> mzRange = null, rtRange = null;

        // Check the peaks for selected data files
        for (RawDataFile dataFile : dataFiles) {
            Feature peak = peakListRow.getPeak(dataFile);
            if (peak == null)
                continue;
            if ((mzRange == null) || (rtRange == null)) {
                mzRange = peak.getRawDataPointsMZRange();
                rtRange = peak.getRawDataPointsRTRange();
            } else {
                mzRange = mzRange.span(peak.getRawDataPointsMZRange());
                rtRange = rtRange.span(peak.getRawDataPointsRTRange());
            }

        }

        // If none of the data files had a peak, check the whole row
        if (mzRange == null) {
            for (Feature peak : peakListRow.getPeaks()) {
                if (peak == null)
                    continue;
                if ((mzRange == null) || (rtRange == null)) {
                    mzRange = peak.getRawDataPointsMZRange();
                    rtRange = peak.getRawDataPointsRTRange();
                } else {
                    mzRange = mzRange.span(peak.getRawDataPointsMZRange());
                    rtRange = rtRange.span(peak.getRawDataPointsRTRange());
                }

            }
        }

        ManualPickerParameters parameters = new ManualPickerParameters();

        if (mzRange != null) {
            parameters.getParameter(ManualPickerParameters.retentionTimeRange)
                    .setValue(rtRange);
            parameters.getParameter(ManualPickerParameters.mzRange)
                    .setValue(mzRange);
        }

        ExitCode exitCode = parameters
                .showSetupDialog(MZmineCore.getDesktop().getMainWindow(), true);

        if (exitCode != ExitCode.OK)
            return exitCode;

        ManualPickerTask task = new ManualPickerTask(
                MZmineCore.getProjectManager().getCurrentProject(), peakListRow,
                dataFiles, parameters, peakList, table);

        MZmineCore.getTaskController().addTask(task);
        return exitCode;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return ManualPickerParameters.class;
    }

}
