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

package io.github.mzmine.modules.visualization.vankrevelendiagram;

import java.awt.Window;
import java.text.DecimalFormat;
import java.util.Arrays;

import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakSelectionParameter;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.PeakListRowSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;

/*
 * Van Krevelen diagram class
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class VanKrevelenDiagramParameters extends SimpleParameterSet {
    public static final PeakListsParameter peakList = new PeakListsParameter(1,
            1);

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

    public static final ComboParameter<String> paintScale = new ComboParameter<>(
            "Heatmap style", "Select the style for the third dimension",
            new String[] { "Rainbow", "Monochrome red", "Monochrome green",
                    "Monochrome yellow", "Monochrome cyan" });

    public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

    public VanKrevelenDiagramParameters() {
        super(new Parameter[] { peakList, selectedRows, zAxisValues, zScaleType,
                zScaleRange, paintScale, windowSettings });
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
