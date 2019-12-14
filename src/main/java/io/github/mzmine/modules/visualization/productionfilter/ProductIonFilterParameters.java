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

package io.github.mzmine.modules.visualization.productionfilter;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ListDoubleParameter;
import io.github.mzmine.parameters.parametertypes.WindowSettingsParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import io.github.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class ProductIonFilterParameters extends SimpleParameterSet {

    public static final String xAxisPrecursor = "Precursor mass";
    public static final String xAxisRT = "Retention time";

    public static final String[] xAxisTypes = { xAxisPrecursor, xAxisRT };

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    public static final ComboParameter<String> xAxisType = new ComboParameter<String>(
            "X axis", "X axis type", xAxisTypes);

    public static final RTRangeParameter retentionTimeRange = new RTRangeParameter();

    public static final MZRangeParameter mzRange = new MZRangeParameter(
            "Precursor m/z", "Range of precursor m/z values");

    public static final MZToleranceParameter mzDifference = new MZToleranceParameter();

    public static final ListDoubleParameter targetedMZ_List = new ListDoubleParameter(
            "Diagnostic product ions (m/z)",
            "Product m/z-values that must be included in MS/MS", false, null);

    public static final ListDoubleParameter targetedNF_List = new ListDoubleParameter(
            "Diagnostic neutral loss values (Da)",
            "Neutral loss m/z-values that must be included in MS/MS", false,
            null);

    public static final DoubleParameter basePeakPercent = new DoubleParameter(
            "Minimum diagnostic ion intensity (% base peak)",
            "Percent of scan base peak of which ms/ms product ions must be above to be included in analysis",
            MZmineCore.getConfiguration().getRTFormat(), 5.0);

    public static final FileNameParameter fileName = new FileNameParameter(
            "Peaklist output file",
            "Name of the output CSV file containing m/z and RT of selected precursor ions. "
                    + "If the file already exists, it will be overwritten.",
            "csv");

    /**
     * Windows size and position
     */
    public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();

    public ProductIonFilterParameters() {
        super(new Parameter[] { dataFiles, xAxisType, retentionTimeRange,
                mzRange, windowSettings, mzDifference, targetedMZ_List,
                targetedNF_List, basePeakPercent, fileName });
    }

}
