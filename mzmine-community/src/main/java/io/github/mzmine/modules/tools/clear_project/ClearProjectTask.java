/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.clear_project;

import io.github.mzmine.project.ProjectService;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;

public class ClearProjectTask extends AbstractTask {

  protected ClearProjectTask(@NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
  }

  @Override
  public String getTaskDescription() {
    return "Clearing the project space";
  }

  @Override
  public double getFinishedPercentage() {
    return 1;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    ProjectService.getProjectManager().clearProject();
    setStatus(TaskStatus.FINISHED);
  }
}
