/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 3.
 * 
 * MZmine 3 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 3 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 3; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.image;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import io.github.mzmine.datamodel.ImagingRawDataFile;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

/*
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ImageVisualizerModule implements MZmineRunnableModule {
  private static final String MODULE_NAME = "Image visualizer";
  private static final String MODULE_DESCRIPTION = "Image visualizer";

  @Nonnull
  @Override
  public String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Nonnull
  @Override
  public String getName() {
    return MODULE_NAME;
  }

  @Nonnull
  @Override
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    RawDataFile[] files = parameters.getParameter(ImageVisualizerParameters.rawDataFiles).getValue()
        .getMatchingRawDataFiles();

    for (RawDataFile file : files) {
      if (!(file instanceof ImagingRawDataFile)) {
        continue;
      }

      Task newTask = new ImageVisualizerTask(file, parameters);
      tasks.add(newTask);
    }
    return ExitCode.OK;
  }

  @Nonnull
  @Override
  public MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONRAWDATA;
  }

  @Nullable
  @Override
  public Class<? extends ParameterSet> getParameterSetClass() {
    return ImageVisualizerParameters.class;
  }
}
