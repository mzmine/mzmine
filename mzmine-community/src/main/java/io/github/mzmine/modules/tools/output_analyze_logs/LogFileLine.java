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

package io.github.mzmine.modules.tools.output_analyze_logs;

import io.github.mzmine.modules.tools.output_compare_csv.CheckResult;
import java.time.LocalDateTime;
import org.jetbrains.annotations.NotNull;

/**
 * @param logState    the state currently in
 * @param isException
 * @param severity    WARNING or ERROR are specific everything else is mapped to INFO
 * @param originClass the class of message origin
 * @param method      the method of message origin
 * @param message     the message
 */
record LogFileLine(@NotNull LogFileState logState, boolean isException,
                   @NotNull LocalDateTime dateTime, @NotNull CheckResult.Severity severity,
                   @NotNull String originClass, @NotNull String method, @NotNull String message) {

  public LogFileLine withMessage(final String message) {
    return new LogFileLine(logState, isException, dateTime, severity, originClass, method, message);
  }

  public LogFileLine with(final String message, final boolean isException) {
    return new LogFileLine(logState, isException, dateTime, severity, originClass, method, message);
  }

  public @NotNull String createIdentifier() {
    final String exception = isException ? " (exception)" : "";
    return "%s: %s in %s at %s%s".formatted(logState, originClass, method, dateTime, exception);
  }

  /**
   * Compares by simple class name
   *
   * @return true if simple name matches like BatchTask without package
   */
  public boolean matchesClass(final Class clazz) {
    return matchesClass(clazz.getSimpleName());
  }

  /**
   * Compares by simple class name
   *
   * @return true if simple name matches like BatchTask without package
   */
  public boolean matchesClass(final String clazz) {
    final String[] split = originClass.split("\\.");
    return split[split.length - 1].equalsIgnoreCase(clazz);
  }
}
