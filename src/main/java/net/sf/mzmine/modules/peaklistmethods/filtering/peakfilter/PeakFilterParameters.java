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

package net.sf.mzmine.modules.peaklistmethods.filtering.peakfilter;

import java.awt.Window;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.IntRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.util.ExitCode;

import com.google.common.collect.Range;

public class PeakFilterParameters extends SimpleParameterSet {

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final StringParameter SUFFIX = new StringParameter(
            "Name suffix", "Suffix to be added to peak list name", "filtered");

    public static final OptionalParameter<DoubleRangeParameter> PEAK_DURATION = new OptionalParameter<>(
            new DoubleRangeParameter("Duration",
                    "Permissible range of peak durations", MZmineCore
                            .getConfiguration().getRTFormat(), Range.closed(
                            0.0, 10.0)));

    public static final OptionalParameter<DoubleRangeParameter> PEAK_AREA = new OptionalParameter<>(
            new DoubleRangeParameter("Area",
                    "Permissible range of peak area", MZmineCore
                            .getConfiguration().getIntensityFormat(),
                    Range.closed(0.0, 10000000.0)));

    public static final OptionalParameter<DoubleRangeParameter> PEAK_HEIGHT = new OptionalParameter<>(
            new DoubleRangeParameter("Height",
                    "Permissible range of peak height", MZmineCore
                            .getConfiguration().getIntensityFormat(),
                    Range.closed(0.0, 10000000.0)));

    public static final OptionalParameter<IntRangeParameter> PEAK_DATAPOINTS = new OptionalParameter<>(
            new IntRangeParameter(
                    "# data points",
                    "Permissible range of the number of data points over a peak",
                    false, Range.closed(8, 30)));

    public static final OptionalParameter<DoubleRangeParameter> PEAK_FWHM = new OptionalParameter<>(
            new DoubleRangeParameter(
                    "FWHM",
                    "Permissible range of the full width half minimum (FWHM) for a peak",
                    MZmineCore.getConfiguration().getRTFormat(), Range.closed(
                            0.0, 2.0)));

    public static final OptionalParameter<DoubleRangeParameter> PEAK_TAILINGFACTOR = new OptionalParameter<>(
            new DoubleRangeParameter("Tailing factor",
                    "Permissible range of the tailing factor for a peak",
                    MZmineCore.getConfiguration().getRTFormat(), Range.closed(
                            0.5, 2.0)));

    public static final OptionalParameter<DoubleRangeParameter> PEAK_ASYMMETRYFACTOR = new OptionalParameter<>(
            new DoubleRangeParameter("Asymmetry factor",
                    "Permissible range of the asymmetry factor for a peak",
                    MZmineCore.getConfiguration().getRTFormat(), Range.closed(
                            0.5, 2.0)));

    public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
            "Remove source peak list after filtering",
            "If checked, the original peak list will be removed leaving only the filtered version");
    
    public static final BooleanParameter MS2_Filter = new BooleanParameter(
            "Keep only features with MS/MS scan",
            "If checked, the feature that don't contain MS2 scan will be removed.");
    public PeakFilterParameters() {
        super(new Parameter[] { PEAK_LISTS, SUFFIX, PEAK_DURATION, PEAK_AREA,
                PEAK_HEIGHT, PEAK_DATAPOINTS, PEAK_FWHM, PEAK_TAILINGFACTOR,
                PEAK_ASYMMETRYFACTOR, MS2_Filter, AUTO_REMOVE });
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

        ParameterSetupDialog dialog = new ParameterSetupDialog(parent,
                valueCheckRequired, this);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }
}
