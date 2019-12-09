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

/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 * 
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.gnpsexport.fbmn;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PasswordParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;

/**
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class GnpsFbmnSubmitParameters extends SimpleParameterSet {

    public enum Preset {
        HIGHRES, LOWRES;
    }

    /**
     * Optional: Select meta data file
     */
    public static final OptionalParameter<FileNameParameter> META_FILE = new OptionalParameter<FileNameParameter>(
            new FileNameParameter("Meta data file",
                    "Optional meta file for GNPS"),
            false);

    public static final ComboParameter<Preset> PRESETS = new ComboParameter<>(
            "Presets",
            "GNPS parameter presets for high or low resolution mass spectrometry data",
            Preset.values(), Preset.HIGHRES);

    public static final StringParameter JOB_TITLE = new StringParameter(
            "Job title",
            "The title of the new GNPS feature-based molecular networking job",
            "", false);
    /**
     * Email to be notified on job status
     */
    public static final StringParameter EMAIL = new StringParameter("Email",
            "Email address for notifications about the job", "", false, true);
    public static final StringParameter USER = new StringParameter("Username",
            "Username for login", "", false, true);
    public static final PasswordParameter PASSWORD = new PasswordParameter(
            "Password",
            "The password is sent without encryption, until the server has has moved to its final destination.",
            "", false);

    /**
     * Export ion identity network edges (if available)
     */
    // public static final BooleanParameter ANN_EDGES =
    // new BooleanParameter("Annotation edges", "Add annotation edges to GNPS
    // job", true);
    // public static final BooleanParameter CORR_EDGES =
    // new BooleanParameter("Correlation edges", "Add correlation edges to GNPS
    // job", false);

    /**
     * Show GNPS job website
     */
    public static final BooleanParameter OPEN_WEBSITE = new BooleanParameter(
            "Open website", "Website of GNPS job", true);

    public GnpsFbmnSubmitParameters() {
        super(new Parameter[] { META_FILE, PRESETS, JOB_TITLE, EMAIL, USER,
                PASSWORD, OPEN_WEBSITE });
    }
}
