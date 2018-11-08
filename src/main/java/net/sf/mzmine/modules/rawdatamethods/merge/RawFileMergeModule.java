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

package net.sf.mzmine.modules.rawdatamethods.merge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.modules.rawdatamethods.merge.RawFileMergeParameters.MODE;
import net.sf.mzmine.modules.rawdatamethods.merge.RawFileMergeParameters.POSITION;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 * Exports scans around a center time
 */
public class RawFileMergeModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Merge files";
  private static final String MODULE_DESCRIPTION =
      "Merge all scans of multiple files to one raw data file";

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
    // merge all selected
    if (parameters.getParameter(RawFileMergeParameters.mode).getValue()
        .equals(MODE.MERGE_SELECTED)) {
      RawDataFile[] raw = parameters.getParameter(RawFileMergeParameters.dataFiles).getValue()
          .getMatchingRawDataFiles();
      RawFileMergeTask task = new RawFileMergeTask(parameters, raw);
      tasks.add(task);
    } else {
      // sort files into merge groups
      RawDataFile[] raw = parameters.getParameter(RawFileMergeParameters.dataFiles).getValue()
          .getMatchingRawDataFiles();

      POSITION pos = parameters.getParameter(RawFileMergeParameters.position).getValue();
      String posMarker = parameters.getParameter(RawFileMergeParameters.posMarker).getValue();

      String groupingElement = "";
      List<RawDataFile> current = new ArrayList<>();
      do {
        current.clear();
        for (int i = 0; i < raw.length; i++) {
          if (raw[i] != null) {
            String group = extractGroup(pos, posMarker, raw[i]);
            if (current.isEmpty()) {
              groupingElement = group;
              current.add(raw[i]);
              raw[i] = null;
            } else if (group.equals(groupingElement)) {
              current.add(raw[i]);
              raw[i] = null;
            }
          }
        }
        // run task
        if (current.size() > 1) {
          RawFileMergeTask task =
              new RawFileMergeTask(parameters, current.toArray(new RawDataFile[current.size()]));
          tasks.add(task);
        }
      } while (!current.isEmpty());
    }
    return ExitCode.OK;
  }

  private String extractGroup(POSITION pos, String posMarker, RawDataFile raw) {
    String name = raw.getName();
    switch (pos) {
      case AFTER_LAST:
        int index = name.lastIndexOf(posMarker);
        if (index == -1)
          throw new MSDKRuntimeException(
              "Merging of raw data files not possible. Position marker was not present in file: "
                  + name);
        return name.substring(index + 1, name.length());
      case BEFORE_FIRST:
        int first = name.indexOf(posMarker);
        if (first == -1)
          throw new MSDKRuntimeException(
              "Merging of raw data files not possible. Position marker was not present in file: "
                  + name);
        return name.substring(0, first);

      default:
        throw new MSDKRuntimeException("Should not end here");
    }
  }

  @Override
  public @Nonnull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.RAWDATA;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return RawFileMergeParameters.class;
  }

}
