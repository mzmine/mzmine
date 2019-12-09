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

package io.github.mzmine.modules.io.adapmgfexport;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

/**
 * Exports a feature cluster to mgf. Used for GC-GNPS
 * 
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */
public class AdapMgfExportParameters extends SimpleParameterSet {
    /**
     * Defines the representative m/z value for a cluster
     * 
     * @author Robin Schmid (robinschmid@uni-muenster.de)
     *
     */
    public static enum MzMode {
        AS_IN_FEATURE_TABLE("As in feature table"), HIGHEST_MZ(
                "Highest m/z"), MAX_INTENSITY("Max. intensity");

        private final String name;

        MzMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static final String ROUND_MODE_MAX = "Maximum";
    public static final String ROUND_MODE_SUM = "Sum";

    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final FileNameParameter FILENAME = new FileNameParameter(
            "Filename",
            "Name of the output MGF file. "
                    + "Use pattern \"{}\" in the file name to substitute with feature list name. "
                    + "(i.e. \"blah{}blah.mgf\" would become \"blahSourcePeakListNameblah.mgf\"). "
                    + "If the file already exists, it will be overwritten.",
            "mgf");

    public static final BooleanParameter FRACTIONAL_MZ = new BooleanParameter(
            "Fractional m/z values", "If checked, write fractional m/z values",
            false);

    public static final ComboParameter<String> ROUND_MODE = new ComboParameter<>(
            "Merging Mode",
            "Determines how to merge intensities with the same m/z values",
            new String[] { ROUND_MODE_MAX, ROUND_MODE_SUM }, ROUND_MODE_MAX);

    public static final ComboParameter<MzMode> REPRESENTATIVE_MZ = new ComboParameter<AdapMgfExportParameters.MzMode>(
            "Representative m/z", "Choose the representative m/z of a cluster.",
            MzMode.values(), MzMode.AS_IN_FEATURE_TABLE);

    public AdapMgfExportParameters() {
        super(new Parameter[] { PEAK_LISTS, FILENAME, REPRESENTATIVE_MZ,
                FRACTIONAL_MZ, ROUND_MODE });
    }
}
