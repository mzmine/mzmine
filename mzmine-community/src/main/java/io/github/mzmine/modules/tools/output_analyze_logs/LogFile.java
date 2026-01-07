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

import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.tools.output_compare_csv.CheckResult.Severity;
import java.util.List;

record LogFile(List<LogFileLine> lines) {

  public int count(final LogFileState state) {
    return (int) lines.stream().map(LogFileLine::logState).filter(s -> s == state).count();
  }

  public int count(final Severity severity) {
    return (int) lines.stream().map(LogFileLine::severity).filter(s -> s == severity).count();
  }

  public List<LogFileLine> findExceptions() {
    return lines.stream().filter(LogFileLine::isException).toList();
  }

  public List<LogFileLine> findBatchSpeed() {
    return lines.stream().filter(
        line -> line.method().equals("printBatchTimes") && line.matchesClass(BatchTask.class)
                && line.message().startsWith("Timing: Whole batch took")).toList();
  }
}
