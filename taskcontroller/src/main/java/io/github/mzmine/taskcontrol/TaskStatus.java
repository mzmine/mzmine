/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.taskcontrol;

import java.util.Comparator;
import java.util.List;

public enum TaskStatus {

  /**
   * WAITING - task is waiting for available thread
   * <p>
   * PROCESSING - task is running
   * <p>
   * FINISHED - task finished successfully
   * <p>
   * CANCELED - task was canceled by user
   * <p>
   * ERROR - task finished with error, error message can be obtained by getErrorMessage()
   */
  WAITING, PROCESSING, FINISHED, CANCELED, ERROR;

  /**
   * ERROR is worst then CANCELED, WAITING, PROCESSING, FINISHED (best)
   */
  public static TaskStatus findWorstStatus(final List<Task> tasks) {
    return tasks.stream().map(Task::getStatus)
        .min(Comparator.comparingInt(TaskStatus::getResultRating)).orElse(FINISHED);
  }

  /**
   * ERROR is worst then CANCELED, WAITING, PROCESSING, FINISHED (best)
   *
   * @return low to high ERROR -> FINISHED
   */
  private int getResultRating() {
    return switch (this) {
      case ERROR -> -20;
      case CANCELED -> -10;
      case WAITING -> 0;
      case PROCESSING -> 10;
      case FINISHED -> 20;
    };
  }

  /**
   * WAITING and PROCESSING can be changed but other terminal states are unmodifiable
   *
   * @return true if the state cannot be changed once a task is set to this state
   */
  public boolean isUnmodifiable() {
    return switch (this) {
      case FINISHED, ERROR, CANCELED -> true;
      case WAITING, PROCESSING -> false;
    };
  }
}
