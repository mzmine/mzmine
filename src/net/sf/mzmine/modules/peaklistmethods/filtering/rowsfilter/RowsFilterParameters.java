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
package net.sf.mzmine.modules.peaklistmethods.filtering.rowsfilter;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.SimpleParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.parametertypes.*;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ExitCode;

public class RowsFilterParameters extends SimpleParameterSet {

        public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

        public static final StringParameter SUFFIX = new StringParameter(
                "Name suffix",
                "Suffix to be added to peak list name",
                "filtered");

        public static final IntegerParameter MIN_PEAK_COUNT = new IntegerParameter(
                "Minimum peaks in a row",
                "Minimum number of peak detections required per row",
                1, 0, null);

        public static final IntegerParameter MIN_ISOTOPE_PATTERN_COUNT = new IntegerParameter(
                "Minimum peaks in an isotope pattern",
                "Minimum number of peaks required in an isotope pattern");

        public static final RangeParameter MZ_RANGE = new RangeParameter(
                "m/z range",
                "Permissible range of (average) m/z values per row",
                MZmineCore.getMZFormat());

        public static final RangeParameter RT_RANGE = new RangeParameter(
                "Retention time range",
                "Permissible range of (average) retention times per row",
                MZmineCore.getRTFormat());
        
        public static final RangeParameter PEAK_DURATION = new RangeParameter(
                "Peak duration range",
                "Permissible range of (average) peak durations per row",
                MZmineCore.getRTFormat(),
                new Range(0.0, 10.0));

        public static final BooleanParameter GROUPS = new BooleanParameter(
                "Filtering by groups",
                "Samples are separated by groups before filtering the peaks");

        public static final ComboParameter<Object> GROUPSPARAMETER = new ComboParameter<Object>(
                "Parameter",
                "Paremeter defining the group of each sample.",
                new Object[0]);

        public static final BooleanParameter HAS_IDENTITIES = new BooleanParameter(
                "Only identified?",
                "Select to filter only identified compounds");

        public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
                "Remove source peak list after filtering",
                "If checked, the original peak list will be removed leaving only the filtered version");

        public RowsFilterParameters() {
                super(new Parameter[]{
                                PEAK_LISTS, SUFFIX, MIN_PEAK_COUNT, MIN_ISOTOPE_PATTERN_COUNT, MZ_RANGE, RT_RANGE, PEAK_DURATION,
                                GROUPS, GROUPSPARAMETER, HAS_IDENTITIES, AUTO_REMOVE});
        }

        @Override
        public ExitCode showSetupDialog() {

                // Update the parameter choices
                UserParameter newChoices[] = MZmineCore.getCurrentProject().getParameters();
                getParameter(RowsFilterParameters.GROUPSPARAMETER).setChoices(newChoices);             
                ParameterSetupDialog dialog = new ParameterSetupDialog(this, null);
                dialog.setVisible(true);
                return dialog.getExitCode();
        }
}
