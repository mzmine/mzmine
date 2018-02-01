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

package net.sf.mzmine.modules.visualization.vankrevelendiagram;

import java.awt.Window;
import java.text.DecimalFormat;
import java.util.Arrays;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakSelectionParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class VanKrevelenDiagramParameters extends SimpleParameterSet {
    public static final String rawDataFilesOption = "Raw data file";

    public static final PeakListsParameter peakList = new PeakListsParameter(1,
            1);

    public static final RawDataFilesParameter rawFile = new RawDataFilesParameter();

    public static final PeakSelectionParameter selectedRows = new PeakSelectionParameter();

    public static final ComboParameter<String> zAxisValues = new ComboParameter<>(
            "Z-Axis",
            "Select a parameter for a third dimension, displayed as a heatmap or select none for a 2D plot",
            new String[] { "none", "Retention time", "Intensity", "Area",
                    "Tailing factor", "Asymmetry factor", "FWHM", "m/z" });
    public static final ComboParameter<String> zScaleType = new ComboParameter<>(
            "Z-Axis scale", "Select Z-Axis scale",
            new String[] { "percentile", "custom" });

    public static final DoubleRangeParameter zScaleRange = new DoubleRangeParameter(
            "Range for z-Axis scale",
            "Set the range for z-Axis scale."
                    + " If percentile is used for z-Axis scale type, you can remove extreme values of the scale."
                    + " E. g. type 0.5 and 99.5 to ignore the 0.5 smallest and 0.5 highest values. "
                    + "If you choose custom, set ranges manually "
                    + "Features out of scale range are displayed in magenta",
            new DecimalFormat("##0.00"));
    public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

    public VanKrevelenDiagramParameters() {
        super(new Parameter[] { peakList, rawFile, selectedRows, zAxisValues, zScaleType, zScaleRange,
                windowSettings, });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

        PeakList selectedPeakLists[] = getParameter(peakList).getValue()
                .getMatchingPeakLists();
        if (selectedPeakLists.length > 0) {
            PeakListRow plRows[] = selectedPeakLists[0].getRows();
            Arrays.sort(plRows, new PeakListRowSorter(SortingProperty.MZ,
                    SortingDirection.Ascending));
        }

        return super.showSetupDialog(parent, valueCheckRequired);
    }

}
