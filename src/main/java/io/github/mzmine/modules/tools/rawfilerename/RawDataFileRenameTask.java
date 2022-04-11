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

package io.github.mzmine.modules.tools.rawfilerename;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RawDataFileRenameTask extends AbstractTask {
  private final ParameterSet parameters;

  public RawDataFileRenameTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      ParameterSet parameters) {
    super(storage, moduleCallDate);
    this.parameters = parameters;
  }

  @Override
  public String getTaskDescription() {
    return "Renaming raw data file.";
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    final RawDataFile[] matchingRawDataFiles = parameters.getParameter(
        RawDataFileRenameParameters.files).getValue().getMatchingRawDataFiles();
    final String newName = parameters.getParameter(RawDataFileRenameParameters.newName).getValue();

    if (matchingRawDataFiles.length == 0) {
      setStatus(TaskStatus.FINISHED);
    }

    RawDataFile file = matchingRawDataFiles[0];

    // set name is now threadsafe and will return the set name after checking for duplicates etc
    final String realName = file.setName(newName);
    parameters.setParameter(RawDataFileRenameParameters.newName, realName);

    file.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(RawDataFileRenameModule.class, parameters,
            getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);
  }
}
