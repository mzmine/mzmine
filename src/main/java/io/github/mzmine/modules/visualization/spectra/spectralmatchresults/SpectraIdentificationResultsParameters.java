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

package io.github.mzmine.modules.visualization.spectra.spectralmatchresults;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;

/**
 * Saves the export paths of the SpectraIdentificationResultsWindow
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class SpectraIdentificationResultsParameters extends SimpleParameterSet {

    public static final FileNameParameter file = new FileNameParameter("file",
            "file without extension");

    public static final BooleanParameter all = new BooleanParameter(
            "Show export all", "Show button in panel", true);
    public static final BooleanParameter pdf = new BooleanParameter(
            "Show export pdf", "Show button in panel", true);
    public static final BooleanParameter emf = new BooleanParameter(
            "Show export emf", "Show button in panel", true);
    public static final BooleanParameter eps = new BooleanParameter(
            "Show export eps", "Show button in panel", true);
    public static final BooleanParameter svg = new BooleanParameter(
            "Show export svg", "Show button in panel", true);

    public SpectraIdentificationResultsParameters() {
        super(new Parameter[] { file, all, pdf, emf, eps, svg });
    }

}
