/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.io.gnpsexport;

import java.util.Collection;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 * Exports all files needed for GNPS feature based molecular networking (quant table (csv export)),
 * MS2 mgf, additional edges (ion identity networks)
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class GNPSExportModule implements MZmineProcessingModule {
  private final Logger LOG = Logger.getLogger(getClass().getName());

  private static final String MODULE_NAME = "Export for/Submit to GNPS";
  private static final String MODULE_DESCRIPTION =
      "Exports the MGF file for GNPS (only for MS/MS), the quant table (CSV export) and additional edges (ion identity networks and correlation)";

  @Override
  public String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @Nonnull
  public ExitCode runModule(MZmineProject project, ParameterSet parameters,
      Collection<Task> tasks) {
    // add gnps export task
    GNPSExportAndSubmitTask task = new GNPSExportAndSubmitTask(parameters);
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
    return GNPSExportParameters.class;
  }

}

/*
 * GNPS: "If you use the GNPS export module (http://gnps.ucsd.edu), cite MZmine2 and the following
 * article: Wang et al., Nature Biotechnology 34.8 (2016): 828-837. [LINK]
 * (https://www.nature.com/nbt/journal/v34/n8/full/nbt.3597.htm)
 * 
 * See documentation about MZmine2 data pre-processing
 * [https://bix-lab.ucsd.edu/display/Public/Mass+spectrometry+data+pre-processing+for+GNPS] for GNPS
 * (http://gnps.ucsd.edu)
 */
