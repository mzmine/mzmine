/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.rawdatamethods.rawdataexport;

import java.io.File;
import java.util.Collection;

import javax.annotation.Nonnull;
import javax.swing.JOptionPane;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 * Raw data export module
 */
public class RawDataExportModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Raw data export";
  private static final String MODULE_DESCRIPTION =
      "This module exports raw data files from your MZmine project into various formats";

  @Override
  public @Nonnull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nonnull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @Nonnull
  public ExitCode runModule(final @Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    File fileName = parameters.getParameter(RawDataExportParameters.fileName).getValue();
    RawDataFile dataFile = parameters.getParameter(RawDataExportParameters.dataFiles).getValue()
        .getMatchingRawDataFiles()[0];

    if (!fileName.getName().toLowerCase().endsWith("mzml")
        && !fileName.getName().toLowerCase().endsWith("cdf")) {
      MZmineCore.getDesktop().displayErrorMessage(MZmineCore.getDesktop().getMainWindow(),
          "Unknown file type - please include either .mzML or .cdf extension in the filename");
      return ExitCode.ERROR;
    }
    
    if (!(MZmineCore.getDesktop() instanceof HeadLessDesktop) && fileName.exists()) {
      String msg = "The file already exists. Do you want to overwrite the file?";
      int confirmResult =
          JOptionPane.showConfirmDialog(null, msg, "Existing file", JOptionPane.YES_NO_OPTION);
      if (confirmResult == JOptionPane.NO_OPTION)
        return ExitCode.CANCEL;

    }

    Task newTask = new RawDataExportTask(dataFile, fileName);
    tasks.add(newTask);

    return ExitCode.OK;
  }

  @Override
  public @Nonnull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.RAWDATA;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return RawDataExportParameters.class;
  }

}
