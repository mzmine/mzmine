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

package net.sf.mzmine.modules.peaklistmethods.gapfilling.peakfinder.multithreaded;

import java.util.Collection;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.desktop.preferences.MZminePreferences;
import net.sf.mzmine.desktop.preferences.NumOfThreadsParameter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class MultiThreadPeakFinderModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Peak finder (multi threaded)";
  private static final String MODULE_DESCRIPTION =
      "This method fills the missing peaks (gaps) in the peak list by searching for a peak in the raw data.";

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
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    PeakList[] peakLists = parameters.getParameter(MultiThreadPeakFinderParameters.peakLists)
        .getValue().getMatchingPeakLists();

    for (PeakList peakList : peakLists) {
      // Obtain the settings of max concurrent threads
      NumOfThreadsParameter parameter = MZmineCore.getConfiguration().getPreferences()
          .getParameter(MZminePreferences.numOfThreads);
      int maxRunningThreads;
      if (parameter.isAutomatic() || (parameter.getValue() == null))
        maxRunningThreads = Runtime.getRuntime().availableProcessors();
      else
        maxRunningThreads = parameter.getValue();

      // raw files
      int raw = peakList.getNumberOfRawDataFiles();
      int numPerTask = raw / maxRunningThreads;
      // start tasks
      for (int i = 0; i < maxRunningThreads; i++) {
        int start = numPerTask * i;
        int endexcl = i < maxRunningThreads - 1 ? numPerTask * (i + 1) : raw;

        // start task
        Task newTask = new MultiThreadPeakFinderTask(project, peakList, parameters, start, endexcl);
        tasks.add(newTask);
      }
    }

    return ExitCode.OK;

  }

  @Override
  public @Nonnull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.GAPFILLING;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return MultiThreadPeakFinderParameters.class;
  }

}
