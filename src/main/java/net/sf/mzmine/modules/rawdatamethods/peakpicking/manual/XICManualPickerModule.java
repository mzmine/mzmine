/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.manual;

import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.modules.visualization.peaklisttable.table.PeakListTable;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import net.sf.mzmine.util.ExitCode;
import com.google.common.collect.Range;

public class XICManualPickerModule implements MZmineModule {

  /**
   * @see net.sf.mzmine.modules.MZmineProcessingModule#getName()
   */
  public @Nonnull String getName() {
    return "XIC Manual peak detector";
  }

  // public static ExitCode runManualDetection(RawDataFile dataFile, PeakListRow peakListRow,
  // PeakList peakList, PeakListTable table) {
  // return runManualDetection(new RawDataFile[] {dataFile}, peakListRow, peakList, table);
  // }

  public static ExitCode runManualDetection(RawDataFile dataFile, PeakListRow peakListRow,
      PeakList peakList, PeakListTable table) {

    Range<Double> mzRange = null, rtRange = null;
    mzRange = Range.closed(peakListRow.getAverageMZ(), peakListRow.getAverageMZ());
    rtRange = Range.closed(peakListRow.getAverageRT(), peakListRow.getAverageRT());
    
    for (Feature peak : peakListRow.getPeaks()) {
      if (peak == null)
        continue;
      // if the peak exists in the file, then we just use that one as a base
      if (peak.getDataFile() == dataFile) {
        mzRange = peak.getRawDataPointsMZRange();
        rtRange = peak.getRawDataPointsRTRange();
        break;
      }
      // if it does not exist, we set up on basis of the other peaks
      if (peak != null) {
        mzRange = mzRange.span(peak.getRawDataPointsMZRange());
        rtRange = rtRange.span(peak.getRawDataPointsRTRange());
      }
    }

    XICManualPickerParameters parameters = new XICManualPickerParameters();

    if (mzRange != null) {
      parameters.getParameter(XICManualPickerParameters.rtRange).setValue(rtRange);
      parameters.getParameter(XICManualPickerParameters.mzRange).setValue(mzRange);
    }
    if (dataFile != null) {
      RawDataFilesSelection selection = new RawDataFilesSelection();
      selection.setSpecificFiles(new RawDataFile[] {dataFile});
      selection.setSelectionType(RawDataFilesSelectionType.SPECIFIC_FILES);
      parameters.getParameter(XICManualPickerParameters.rawDataFiles).setValue(selection);
    }

    ExitCode exitCode = parameters.showSetupDialog(MZmineCore.getDesktop().getMainWindow(), true);

    if (exitCode != ExitCode.OK)
      return exitCode;

    ManualPickerParameters param = new ManualPickerParameters();
    param.getParameter(ManualPickerParameters.mzRange)
        .setValue(parameters.getParameter(XICManualPickerParameters.mzRange).getValue());
    param.getParameter(ManualPickerParameters.retentionTimeRange)
        .setValue(parameters.getParameter(XICManualPickerParameters.rtRange).getValue());


    ManualPickerTask task = new ManualPickerTask(MZmineCore.getProjectManager().getCurrentProject(),
        peakListRow, new RawDataFile[] {dataFile}, param, peakList, table);

    MZmineCore.getTaskController().addTask(task);
    return exitCode;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return ManualPickerParameters.class;
  }

}
