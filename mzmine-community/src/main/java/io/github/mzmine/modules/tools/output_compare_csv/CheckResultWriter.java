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
