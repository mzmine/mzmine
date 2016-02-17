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

package net.sf.mzmine.modules.peaklistmethods.filtering.rowsfilter;

import java.awt.Window;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.util.ExitCode;

import com.google.common.collect.Range;

public class RowsFilterParameters extends SimpleParameterSet {

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final StringParameter SUFFIX = new StringParameter(
            "Name suffix", "Suffix to be added to peak list name", "filtered");

    public static final OptionalParameter<IntegerParameter> MIN_PEAK_COUNT = new OptionalParameter<>(
            new IntegerParameter("Minimum peaks in a row",
                    "Minimum number of peak detections required per row", 1, 0,
                    null));

    public static final OptionalParameter<IntegerParameter> MIN_ISOTOPE_PATTERN_COUNT = new OptionalParameter<>(
            new IntegerParameter("Minimum peaks in an isotope pattern",
                    "Minimum number of peaks required in an isotope pattern"));

    public static final OptionalParameter<MZRangeParameter> MZ_RANGE = new OptionalParameter<>(
            new MZRangeParameter());

    public static final OptionalParameter<RTRangeParameter> RT_RANGE = new OptionalParameter<>(
            new RTRangeParameter());

    public static final OptionalParameter<DoubleRangeParameter> PEAK_DURATION = new OptionalParameter<>(
            new DoubleRangeParameter("Peak duration range",
                    "Permissible range of (average) peak durations per row",
                    MZmineCore.getConfiguration().getRTFormat(), Range.closed(
                            0.0, 10.0)));

    public static final ComboParameter<Object> GROUPSPARAMETER = new ComboParameter<Object>(
            "Parameter", "Paremeter defining the group of each sample.",
            new Object[0]);

    public static final BooleanParameter HAS_IDENTITIES = new BooleanParameter(
            "Only identified?", "Select to filter only identified compounds");

    public static final OptionalParameter<StringParameter> IDENTITY_TEXT = new OptionalParameter<>(
            new StringParameter(
                    "Text in identity",
                    "Only rows that contain this text in their peak identity field will be retained."));

    public static final OptionalParameter<StringParameter> COMMENT_TEXT = new OptionalParameter<>(
            new StringParameter("Text in comment",
                    "Only rows that contain this text in their comment field will be retained."));
    
    public static final ComboParameter<Object> REMOVE_ROW = new ComboParameter<Object>(
            "Keep or remove rows",
            "If selected, rows will be removed based on criteria instead of kept",new Object[0]);

    public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
            "Remove source peak list after filtering",
            "If checked, the original peak list will be removed leaving only the filtered version");

    public RowsFilterParameters() {
        super(new Parameter[] { PEAK_LISTS, SUFFIX, MIN_PEAK_COUNT,
                MIN_ISOTOPE_PATTERN_COUNT, MZ_RANGE, RT_RANGE, PEAK_DURATION,
                GROUPSPARAMETER, HAS_IDENTITIES, IDENTITY_TEXT,
                COMMENT_TEXT, REMOVE_ROW,AUTO_REMOVE });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

        // Update the parameter choices
        UserParameter<?, ?> newChoices[] = MZmineCore.getProjectManager()
                .getCurrentProject().getParameters();
        String[] choices;
        if (newChoices == null || newChoices.length == 0) {
            choices = new String[1];
            choices[0] = "No parameters defined";
        } else {
            choices = new String[newChoices.length + 1];
            choices[0] = "Ignore groups";
            for (int i = 0; i < newChoices.length; i++) {
                choices[i + 1] = "Filtering by " + newChoices[i].getName();
            }
        }

        String[] removeRowChoices = {"Keep rows that match all criteria","Remove rows that match all criteria"};
        getParameter(RowsFilterParameters.REMOVE_ROW).setChoices(removeRowChoices);
        
        getParameter(RowsFilterParameters.GROUPSPARAMETER).setChoices(choices);
        ParameterSetupDialog dialog = new ParameterSetupDialog(parent,
                valueCheckRequired, this);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }
}
