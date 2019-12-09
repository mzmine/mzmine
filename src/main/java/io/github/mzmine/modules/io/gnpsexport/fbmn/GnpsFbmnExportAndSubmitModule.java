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
 * 2018-Nov: Changes by Robin Schmid - Direct submit
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 * 
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.gnpsexport.fbmn;

import java.util.Collection;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

/**
 * Exports all files needed for GNPS feature based molecular networking (quant
 * table (csv export)), MS2 mgf, additional edges (ion identity networks)
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class GnpsFbmnExportAndSubmitModule implements MZmineProcessingModule {
    private final Logger LOG = Logger.getLogger(getClass().getName());

    private static final String MODULE_NAME = "Export/Submit to GNPS-FBMN";
    private static final String MODULE_DESCRIPTION = "GNPS feature-based molecular networking export and submit module. Exports the MGF file for GNPS (only for MS/MS), the quant table (CSV export) and additional edges (ion identity networks and correlation)";

    @Override
    public String getDescription() {
        return MODULE_DESCRIPTION;
    }

    @Override
    @Nonnull
    public ExitCode runModule(MZmineProject project, ParameterSet parameters,
            Collection<Task> tasks) {
        // add gnps export task
        GnpsFbmnExportAndSubmitTask task = new GnpsFbmnExportAndSubmitTask(
                parameters);
        /*
         * We do not add the task to the tasks collection, but instead directly
         * submit to the task controller, because we need to set the priority to
         * HIGH. If the priority is not HIGH and the maximum number of
         * concurrent tasks is set to 1 in the MZmine preferences, then this
         * BatchTask would block all other tasks.
         */
        tasks.add(task);

        return ExitCode.OK;
    }

    @Override
    public MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.PEAKLISTEXPORT;
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @Override
    public Class<? extends ParameterSet> getParameterSetClass() {
        return GnpsFbmnExportAndSubmitParameters.class;
    }

}

/*
 * The GNPSExport module was designed for Feature-Based Molecular Networking on
 * [GNPS](https://gnps.ucsd.edu/ProteoSAFe/static/gnps-splash2.jsp). Please cite
 * our preprint [Nothias, L.F. et al bioRxiv
 * 812404](https://www.biorxiv.org/content/10.1101/812404v1). [See the
 * documentation here](https://ccms-ucsd.github.io/GNPSDocumentation/
 * featurebasedmolecularnetworking/). [See the tutorial on MZmine2 processing
 * for FBMN](https://ccms-ucsd.github.io/GNPSDocumentation/
 * featurebasedmolecularnetworking-with- mzmine2/).
 * =============================================================================
 * ============= If you use the GNPSExport module, please cite MZmine papers and
 * the GNPS article: Wang et al., [Nature Biotechnology 34.8 (2016):
 * 828-837](https://doi.org/10.1038/nbt.3597m).
 * =============================================================================
 * =============
 */
