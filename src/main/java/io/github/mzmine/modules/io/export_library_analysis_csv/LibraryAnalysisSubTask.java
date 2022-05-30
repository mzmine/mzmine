/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.export_library_analysis_csv;

import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.jetbrains.annotations.NotNull;

/**
 * Run analysis of spectral similarity in parallel
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class LibraryAnalysisSubTask extends AbstractTask {

  private final LibraryAnalysisCSVExportTask mainTask;
  private final List<FilteredSpec[]> pairs;
  private final ConcurrentLinkedDeque<String> outputList;
  private final int total;
  private int done;


  protected LibraryAnalysisSubTask(@NotNull Instant moduleCallDate,
      LibraryAnalysisCSVExportTask mainTask, List<FilteredSpec[]> pairs,
      ConcurrentLinkedDeque<String> outputList) {
    super(null, moduleCallDate);
    this.mainTask = mainTask;
    this.pairs = pairs;
    this.outputList = outputList;
    total = pairs.size();
  }

  @Override
  public String getTaskDescription() {
    return String.format("Calculating library correlations: %d / %d", done, total);
  }

  @Override
  public double getFinishedPercentage() {
    return total > 0 ? done / (double) total : 0d;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    List<String> buffer = new ArrayList<>(1024);
    for (FilteredSpec[] pair : pairs) {
      if (isCanceled()) {
        return;
      }

      String line = mainTask.matchToCsvString(pair[0], pair[1]);
      if (line != null) {
        buffer.add(line);
      }
      done++;

      if (buffer.size() >= 1024) {
        outputList.addAll(buffer);
        buffer.clear();
      }
    }

    if (!buffer.isEmpty()) {
      outputList.addAll(buffer);
    }

    setStatus(TaskStatus.FINISHED);
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.HIGH;
  }
}
