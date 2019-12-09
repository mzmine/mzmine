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

package io.github.mzmine.modules.visualization.intensityplot;

import java.awt.Window;
import java.util.Arrays;

import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.MultiChoiceParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakSelectionParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.PeakListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;

public class IntensityPlotParameters extends SimpleParameterSet {

    public static final String rawDataFilesOption = "Raw data file";

    public static final PeakListsParameter peakList = new PeakListsParameter(1,
            1);

    public static final MultiChoiceParameter<RawDataFile> dataFiles = new MultiChoiceParameter<RawDataFile>(
            "Raw data files", "Raw data files to display", new RawDataFile[0]);

    public static final ComboParameter<Object> xAxisValueSource = new ComboParameter<Object>(
            "X axis value", "X axis value",
            new Object[] { rawDataFilesOption });

    public static final ComboParameter<YAxisValueSource> yAxisValueSource = new ComboParameter<YAxisValueSource>(
            "Y axis value", "Y axis value", YAxisValueSource.values());

    public static final PeakSelectionParameter selectedRows = new PeakSelectionParameter();

    /**
     * Windows size and position
     */
    public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

    public IntensityPlotParameters() {
        super(new Parameter[] { peakList, dataFiles, xAxisValueSource,
                yAxisValueSource, selectedRows, windowSettings });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

        PeakList selectedPeakLists[] = getParameter(peakList).getValue()
                .getMatchingPeakLists();
        if (selectedPeakLists.length > 0) {
            RawDataFile plDataFiles[] = selectedPeakLists[0].getRawDataFiles();
            PeakListRow plRows[] = selectedPeakLists[0].getRows();
            Arrays.sort(plRows, new PeakListRowSorter(SortingProperty.MZ,
                    SortingDirection.Ascending));
            getParameter(dataFiles).setChoices(plDataFiles);
            getParameter(dataFiles).setValue(plDataFiles);
        }

        return super.showSetupDialog(parent, valueCheckRequired);
    }

}
