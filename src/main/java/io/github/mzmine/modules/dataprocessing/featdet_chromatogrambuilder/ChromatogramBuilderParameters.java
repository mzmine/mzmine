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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogrambuilder;

import java.awt.Window;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.MassListParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.ExitCode;

public class ChromatogramBuilderParameters extends SimpleParameterSet {

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    public static final ScanSelectionParameter scanSelection = new ScanSelectionParameter(
            new ScanSelection(1));

    public static final MassListParameter massList = new MassListParameter();

    public static final DoubleParameter minimumTimeSpan = new DoubleParameter(
            "Min time span (min)",
            "Minimum time span over which the same ion must be observed in order to be recognized as a chromatogram.\n"
                    + "The optimal value depends on the chromatography system setup. The best way to set this parameter\n"
                    + "is by studying the raw data and determining what is the typical time span of chromatographic peaks.",
            MZmineCore.getConfiguration().getRTFormat());

    public static final DoubleParameter minimumHeight = new DoubleParameter(
            "Min height",
            "Minimum intensity of the highest data point in the chromatogram. If chromatogram height is below this level, it is discarded.",
            MZmineCore.getConfiguration().getIntensityFormat());

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

    public static final StringParameter suffix = new StringParameter("Suffix",
            "This string is added to filename as suffix", "chromatograms");

    public ChromatogramBuilderParameters() {
        super(new Parameter[] { dataFiles, scanSelection, massList,
                minimumTimeSpan, minimumHeight, mzTolerance, suffix });
    }

    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
        String message = "<html><b>Note:</b> starting with MZmine 2.39, this module is considered deprecated <br>"
                + "and will be removed in future MZmine versions. Please use the <b>ADAP Chromatogram Builder</b>,<br>"
                + "which is much faster and generates better results.<br>"
                + "Contact the developers if you have any questions or concerns.</html>";

        ParameterSetupDialog dialog = new ParameterSetupDialog(parent,
                valueCheckRequired, this, message);
        dialog.setVisible(true);
        return dialog.getExitCode();

    }

}
