/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package testutils;

import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.util.maths.Precision;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public sealed interface TaskResult permits TaskResult.ERROR, TaskResult.FINISHED,
    TaskResult.TIMEOUT {

  default String description() {
    return switch (this) {
      case TIMEOUT f -> "Timeout during " + f.moduleClass.getName() + ". Not finished in time.";
      case ERROR f -> "Error during " + f.moduleClass.getName() + ".";
      case FINISHED f ->
          "Finished " + f.moduleClass().getName() + " in " + f.getMiliSeconds() + " ms";
    };
  }

  record FINISHED(Class<? extends MZmineRunnableModule> moduleClass, long nanotime) implements
      TaskResult {

    public double getMiliSeconds() {
      return Precision.round(nanotime / 1000000.0, 1).doubleValue();
    }

    public double getSeconds() {
      return Precision.round(nanotime / 1000000000.0, 4).doubleValue();
    }
  }
  /**
   * at least one task
   */
  record ERROR(Class<? extends MZmineRunnableModule> moduleClass) implements TaskResult {

  }

  /**
   * did not finish in the specified time
   */
  record TIMEOUT(Class<? extends MZmineRunnableModule> moduleClass) implements TaskResult {

  }
}
