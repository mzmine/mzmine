/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.filter_merge;

import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import io.github.msdk.MSDKRuntimeException;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.dataprocessing.filter_merge.RawFileMergeParameters.MODE;
import io.github.mzmine.modules.dataprocessing.filter_merge.RawFileMergeParameters.POSITION;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

/**
 * Exports scans around a center time
 */
public class RawFileMergeModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Merge files";
  private static final String MODULE_DESCRIPTION =
      "Merge all scans of multiple files to one raw data file";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @NotNull
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    // one storage for all files in the same module call
    final MemoryMapStorage storage = MemoryMapStorage.forRawDataFile();

    // merge all selected
    if (parameters.getParameter(RawFileMergeParameters.mode).getValue()
        .equals(MODE.MERGE_SELECTED)) {
      RawDataFile[] raw = parameters.getParameter(RawFileMergeParameters.dataFiles).getValue()
          .getMatchingRawDataFiles();
      RawFileMergeTask task = new RawFileMergeTask(project, parameters, raw, storage, moduleCallDate);
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
          RawFileMergeTask task = new RawFileMergeTask(project, parameters,
              current.toArray(new RawDataFile[current.size()]), storage, moduleCallDate);
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
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.RAWDATA;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return RawFileMergeParameters.class;
  }

}
