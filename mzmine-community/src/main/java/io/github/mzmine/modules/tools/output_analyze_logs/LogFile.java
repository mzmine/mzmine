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
