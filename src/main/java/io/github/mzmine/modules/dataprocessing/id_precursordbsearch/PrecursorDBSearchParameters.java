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

package io.github.mzmine.modules.dataprocessing.id_precursordbsearch;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class PrecursorDBSearchParameters extends SimpleParameterSet {

    public static final PeakListsParameter peakLists = new PeakListsParameter();

    public static final FileNameParameter dataBaseFile = new FileNameParameter(
            "Spectral database file (MS/MS)",
            "(GNPS json, MONA json, NIST msp, JCAMP-DX jdx) Name of file that contains information for peak identification");

    public static final MZToleranceParameter mzTolerancePrecursor = new MZToleranceParameter(
            "Precursor m/z tolerance",
            "Precursor m/z tolerance is used to filter library entries", 0.001,
            5);

    public static final OptionalParameter<RTToleranceParameter> rtTolerance = new OptionalParameter<>(
            new RTToleranceParameter());

    public PrecursorDBSearchParameters() {
        super(new Parameter[] { peakLists, dataBaseFile, mzTolerancePrecursor,
                rtTolerance });
    }

}
