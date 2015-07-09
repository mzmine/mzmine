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

package net.sf.mzmine.modules.visualization.histogram;

import java.awt.Window;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.util.ExitCode;

public class HistogramParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakList = new PeakListsParameter(1,
            1);

    public static final MultiChoiceParameter<RawDataFile> dataFiles = new MultiChoiceParameter<RawDataFile>(
            "Raw data files", "Column of peaks to be plotted",
            new RawDataFile[0]);

    public static final HistogramRangeParameter dataRange = new HistogramRangeParameter();

    public static final IntegerParameter numOfBins = new IntegerParameter(
            "Number of bins", "The plot is divides into this number of bins",
            10);

    /**
     * Windows size and position
     */
    public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

    public HistogramParameters() {
        super(new Parameter[] { peakList, dataFiles, dataRange, numOfBins,
                windowSettings });
    }

    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
        PeakList selectedPeaklists[] = getParameter(
                HistogramParameters.peakList).getValue().getMatchingPeakLists();
        RawDataFile dataFiles[];
        if ((selectedPeaklists == null) || (selectedPeaklists.length != 1)) {
            dataFiles = MZmineCore.getProjectManager().getCurrentProject()
                    .getDataFiles();
        } else {
            dataFiles = selectedPeaklists[0].getRawDataFiles();
        }
        getParameter(HistogramParameters.dataFiles).setChoices(dataFiles);
        return super.showSetupDialog(parent, valueCheckRequired);
    }

}