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
