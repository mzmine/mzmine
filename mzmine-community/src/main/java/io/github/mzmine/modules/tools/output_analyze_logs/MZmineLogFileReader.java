package io.github.mzmine.modules.tools.output_analyze_logs;

import io.github.mzmine.main.impl.MZmineConfigurationImpl;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.modules.tools.output_compare_csv.CheckResult.Severity;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MZmineLogFileReader {

  // this is repeated multiple times for exception stack trace
  // 	at java.base/java.lang.Class.forName(Class.java:462)
  private final String exceptionRegex = "\\s+at .*\\.java:\\d+\\)$";
  private final Pattern exceptionPattern = Pattern.compile(exceptionRegex);

  // pattern is date time level class method message
  private final String regex = "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) (\\w+)\\s+([\\w.]+) (\\w+) (.*)";
  private final Pattern pattern = Pattern.compile(regex);
  private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  // track state of log
  LogFileState logState = LogFileState.PRE_INIT;


  public LogFile readLogFile(final File logFile) throws IOException {
    // a log line may span multiple text lines with breaks
    // accumulate the message and then finalize
    LogFileLine currentLine = null;
    StringBuilder message = null;
    boolean isMultiLineMessage = false;

    // collect lines
    List<LogFileLine> logLines = new ArrayList<>();

    try (var reader = Files.newBufferedReader(logFile.toPath(), StandardCharsets.UTF_8)) {
      String line = null;
      while ((line = reader.readLine()) != null) {
        final Optional<LogFileLine> nextLine = convertToLogLine(line);
        if (nextLine.isPresent()) {
          // finalize previous line - may replace message if multi line
          if (currentLine != null) {
            if (isMultiLineMessage) {
              // finalize multi line message and check for exceptions
              final String newMessage = message.toString();
              boolean isException = newMessage.lines()
                  .anyMatch(exceptionPattern.asMatchPredicate());
              logLines.add(currentLine.with(newMessage, isException));
            } else {
              logLines.add(currentLine);
            }
          }
          currentLine = nextLine.get();
          message = new StringBuilder(currentLine.message());
          isMultiLineMessage = false;
        } else {
          // expand message
          if (message != null) {
            message.append("\n").append(line);
            isMultiLineMessage = true;
          }
        }
      }
    }
    return new LogFile(logLines);
  }

  Optional<LogFileLine> convertToLogLine(final String line) {
    final Matcher matcher = pattern.matcher(line);
    if (matcher.matches()) {
      final LocalDateTime date = LocalDateTime.parse(matcher.group(1), dateFormat);
      final Severity severity = Severity.parse(matcher.group(2).trim());
      final String originClass = matcher.group(3).trim();
      final String method = matcher.group(4).trim();
      final String message = matcher.group(5).trim();

      // find log state
      handleLogMessageState(originClass, method, message);

      // cannot know if this is an exception - need full message for this
      return Optional.of(
          new LogFileLine(logState, false, date, severity, originClass, method, message));
    }
    return Optional.empty();
  }

  private void handleLogMessageState(final String originClass, final String method,
      final String message) {
    final String lowerMessage = message.toLowerCase().trim();
    final String lowerMethod = method.toLowerCase().trim();
    final String[] splitClass = originClass.toLowerCase().split("\\.");
    final String lowerClass = splitClass[splitClass.length - 1];
    if (logState == LogFileState.PRE_INIT && (lowerClass.endsWith("mzminecore")
                                              || lowerClass.endsWith("mzmineprocore"))) {
      logState = LogFileState.INIT_MZMINE;

    } else if (equalsClass(lowerClass, MZmineConfigurationImpl.class) && method.equals(
        "loadConfiguration") && lowerMessage.startsWith("loaded configuration")) {
      logState = LogFileState.RUN_MANUAL;

    } else if (equalsClass(lowerClass, MZmineConfigurationImpl.class) && method.equals(
        "loadConfiguration") && lowerMessage.startsWith("loading desktop configuration")) {
      logState = LogFileState.LOAD_CONFIG;

      // MZmineConfigurationImpl loadConfiguration Loaded configuration from
    } else if (lowerMessage.startsWith("starting a batch") && equalsClass(lowerClass,
        BatchTask.class)) {
      logState = LogFileState.RUN_BATCH;

    } else if (logState == LogFileState.RUN_BATCH && lowerMessage.startsWith(
        "processing of task batch step") && lowerMessage.endsWith("finished")) {
      logState = LogFileState.RUN_MANUAL;

    }
  }

  /**
   * @param classNameLower lower case last part of class name
   * @param clazz          a class
   * @return true if the name of class matches
   */
  boolean equalsClass(String classNameLower, Class clazz) {
    return classNameLower.equals(clazz.getSimpleName().toLowerCase());
  }
}
