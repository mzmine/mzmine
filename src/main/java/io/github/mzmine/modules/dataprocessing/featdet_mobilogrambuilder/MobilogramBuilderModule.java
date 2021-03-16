/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_mobilogrambuilder;

import com.google.common.collect.Lists;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.IMSRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.preferences.MZminePreferences;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MobilogramBuilderModule implements MZmineRunnableModule {

  @Nonnull
  @Override
  public String getDescription() {
    return "Builds mobilograms for each frame";
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    int numberOfThreads = MZmineCore.getConfiguration().getPreferences().getParameter(
        MZminePreferences.numOfThreads).getValue();

    RawDataFile[] files = parameters.getParameter(MobilogramBuilderParameters.rawDataFiles)
        .getValue().getMatchingRawDataFiles();

    for (RawDataFile file : files) {
      if (!(file instanceof IMSRawDataFile)) {
        continue;
      }

      List<List<Frame>> frameLists  =
          Lists.partition(((IMSRawDataFile) file).getFrames().stream().collect(Collectors.toList()),
          ((IMSRawDataFile) file).getFrames().size() / numberOfThreads);

      for(List<Frame> frames : frameLists) {
        MobilogramBuilderTask task = new MobilogramBuilderTask(frames, parameters);
        MZmineCore.getTaskController().addTask(task);
      }
    }

    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.EIC_DETECTION;
  }

  @Nonnull
  @Override
  public String getName() {
    return "Mobiligram builder";
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return MobilogramBuilderParameters.class;
  }
}
