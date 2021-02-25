/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.visualization.rawdataoverviewims;

import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IMSRawDataOverviewModule implements MZmineRunnableModule {

  @Nonnull
  @Override
  public String getName() {
    return "Ion mobility raw data overview";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return IMSRawDataOverviewParameters.class;
  }

  @Nonnull
  @Override
  public String getDescription() {
    return "Visualizes ion mobility raw data files.";
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {
    RawDataFilesParameter param = parameters
        .getParameter(IMSRawDataOverviewParameters.rawDataFiles);
    RawDataFilesSelection selection = param.getValue();
    RawDataFile[] files = selection.getMatchingRawDataFiles();
    for (RawDataFile file : files) {
      if (!(file instanceof IMSRawDataFile)) {
        MZmineCore.getDesktop().displayMessage("Invalid file type",
            "Cannot display raw data file " + file.getName() + " in \"" + getName()
                + "\", since it is does not possess an ion mobility dimension.");
        continue;
      }
      MZmineTab tab = new IMSRawDataOverviewTab(parameters);
      tab.onRawDataFileSelectionChanged(Set.of(file));
      MZmineCore.getDesktop().addTab(tab);
    }

    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }
}
