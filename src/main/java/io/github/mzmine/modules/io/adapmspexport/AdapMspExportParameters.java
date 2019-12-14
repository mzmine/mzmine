/*
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */

package io.github.mzmine.modules.io.adapmspexport;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */

public class AdapMspExportParameters extends SimpleParameterSet {
    public static final String ROUND_MODE_MAX = "Merging mode: Maximum";
    public static final String ROUND_MODE_SUM = "Merging mode: Sum";

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final FileNameParameter FILENAME = new FileNameParameter(
            "Filename",
            "Name of the output MSP file. "
                    + "Use pattern \"{}\" in the file name to substitute with feature list name. "
                    + "(i.e. \"blah{}blah.msp\" would become \"blahSourcePeakListNameblah.msp\"). "
                    + "If the file already exists, it will be overwritten.",
            "msp");

    public static final OptionalParameter<StringParameter> ADD_RET_TIME = new OptionalParameter<>(
            new StringParameter("Add retention time",
                    "If selected, each MSP record will contain the feature's retention time",
                    "RT"),
            true);

    public static final OptionalParameter<StringParameter> ADD_ANOVA_P_VALUE = new OptionalParameter<>(
            new StringParameter("Add ANOVA p-value (if calculated)",
                    "If selected, each MSP record will contain the One-way ANOVA p-value (if calculated)",
                    "ANOVA_P_VALUE"),
            true);

    public static final OptionalParameter<ComboParameter<String>> INTEGER_MZ = new OptionalParameter<>(
            new ComboParameter<>("Integer m/z",
                    "If selected, fractional m/z values will be merged into integer values, based on the selected "
                            + "merging mode",
                    new String[] { ROUND_MODE_MAX, ROUND_MODE_SUM },
                    ROUND_MODE_MAX),
            false);

    public AdapMspExportParameters() {
        super(new Parameter[] { PEAK_LISTS, FILENAME, ADD_RET_TIME,
                ADD_ANOVA_P_VALUE, INTEGER_MZ });
    }
}
