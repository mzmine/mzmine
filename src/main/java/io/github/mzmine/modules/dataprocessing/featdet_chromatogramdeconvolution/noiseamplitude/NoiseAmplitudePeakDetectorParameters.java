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

package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.noiseamplitude;

import java.awt.Window;

import com.google.common.collect.Range;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.PeakResolverSetupDialog;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import io.github.mzmine.util.ExitCode;

public class NoiseAmplitudePeakDetectorParameters extends SimpleParameterSet {

    public static final DoubleParameter MIN_PEAK_HEIGHT = new DoubleParameter(
            "Min peak height",
            "Minimum acceptable height (intensity) for a chromatographic peak",
            MZmineCore.getConfiguration().getIntensityFormat());

    public static final DoubleRangeParameter PEAK_DURATION = new DoubleRangeParameter(
            "Peak duration range (min)", "Range of acceptable peak lengths",
            MZmineCore.getConfiguration().getRTFormat(),
            Range.closed(0.0, 10.0));

    public static final DoubleParameter NOISE_AMPLITUDE = new DoubleParameter(
            "Amplitude of noise",
            "This value is the intensity amplitude of the signal in the noise region",
            MZmineCore.getConfiguration().getIntensityFormat());

    public NoiseAmplitudePeakDetectorParameters() {
        super(new Parameter[] { MIN_PEAK_HEIGHT, PEAK_DURATION,
                NOISE_AMPLITUDE });
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
        final PeakResolverSetupDialog dialog = new PeakResolverSetupDialog(
                parent, valueCheckRequired, this,
                NoiseAmplitudePeakDetector.class);
        dialog.setVisible(true);
        return dialog.getExitCode();
    }
}
