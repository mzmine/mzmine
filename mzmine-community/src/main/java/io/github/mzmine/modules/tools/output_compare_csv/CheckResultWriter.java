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

package io.github.mzmine.modules.tools.output_compare_csv;

import io.github.mzmine.modules.tools.output_compare_csv.CheckResult.Severity;
import io.github.mzmine.util.io.CsvWriter;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CheckResultWriter {

  private static final Logger logger = Logger.getLogger(CheckResultWriter.class.getName());

  /**
   * Filters the checks and then logs them. Optional also write to file
   *
   * @param filter  filter up to this level
   * @param outFile export to file if not null
   * @param checks  the checks to be filtered and logged
   */
  public static void filterAndLogResults(final @NotNull Severity filter,
      final @Nullable File outFile, final List<CheckResult> checks) {
    filter.applyInPlace(checks);

    final String csv = CsvWriter.writeToString(checks, CheckResult.class, ',', true);
    // log all info
    logger.info("""
        Checks:
        
        %s""".formatted(csv));

    // write to file
    if (outFile != null) {
      try {
        CsvWriter.writeToFile(outFile, checks, CheckResult.class, WriterOptions.REPLACE, ',');
      } catch (IOException e) {
        logger.log(Level.WARNING, e.getMessage(), e);
      }
    }
  }
}
