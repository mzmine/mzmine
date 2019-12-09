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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection.wavelet;

import java.awt.Window;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectorSetupDialog;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.util.ExitCode;

public class WaveletMassDetectorParameters extends SimpleParameterSet {

    public static final DoubleParameter noiseLevel = new DoubleParameter(
            "Noise level",
            "Intensities less than this value are interpreted as noise",
            MZmineCore.getConfiguration().getIntensityFormat());

    public static final IntegerParameter scaleLevel = new IntegerParameter(
            "Scale level",
            "Number of wavelet'scale (coeficients) to use in m/z feature detection");

    public static final PercentParameter waveletWindow = new PercentParameter(
            "Wavelet window size (%)",
            "Size in % of wavelet window to apply in m/z feature detection");

    public WaveletMassDetectorParameters() {
        super(new Parameter[] { noiseLevel, scaleLevel, waveletWindow });
    }

    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
        MassDetectorSetupDialog dialog = new MassDetectorSetupDialog(parent,
                valueCheckRequired, WaveletMassDetector.class, this);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }
}
