package io.github.mzmine.modules.tools.output_analyze_logs;

import io.github.mzmine.modules.tools.output_compare_csv.CheckResult;
import java.time.LocalDateTime;
import org.jetbrains.annotations.NotNull;

/**
 * @param logState    the state currently in
 * @param severity    WARNING or ERROR are specific everything else is mapped to INFO
 * @param originClass the class of message origin
 * @param method      the method of message origin
 * @param message     the message
 */
record LogFileLine(LogFileState logState, LocalDateTime dateTime, CheckResult.Severity severity,
                   String originClass, String method, String message) {

  public LogFileLine withMessage(final String message) {
    return new LogFileLine(logState, dateTime, severity, originClass, method, message);
  }

  public @NotNull String createIdentifier() {
    return "%s: %s in %s at %s".formatted(logState, originClass, method, dateTime);
  }
}
