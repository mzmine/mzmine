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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.massdetection;

import java.awt.Color;

import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.localmaxima.LocalMaxMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.recursive.RecursiveMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.wavelet.WaveletMassDetector;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ColorParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.ModuleComboParameter;

public class DPPMassDetectionParameters extends SimpleParameterSet {

    public static final MassDetector massDetectors[] = {
            new CentroidMassDetector(), new ExactMassDetector(),
            new LocalMaxMassDetector(), new RecursiveMassDetector(),
            new WaveletMassDetector() };

    public static final ModuleComboParameter<MassDetector> massDetector = new ModuleComboParameter<MassDetector>(
            "Mass detector",
            "Algorithm to use for mass detection and its parameters",
            massDetectors);

    public static final BooleanParameter displayResults = new BooleanParameter(
            "Display results",
            "Check if you want to display the mass detection results in the plot. Displaying too much datasets might decrease clarity.",
            false);

    public static final ColorParameter datasetColor = new ColorParameter(
            "Dataset color",
            "Set the color you want the detected isotope patterns to be displayed with.",
            Color.CYAN);

    public DPPMassDetectionParameters() {
        super(new Parameter[] { massDetector, displayResults, datasetColor });
    }

}
