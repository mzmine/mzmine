package io.github.mzmine.modules.tools.output_analyze_logs;

import io.github.mzmine.modules.tools.output_compare_csv.CheckResult.Severity;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class MZmineLogFileReaderTest {

  @Test
  void readLogFile() throws IOException {
    final String file = getClass().getClassLoader().getResource("outfiles/mzmine_log.log")
        .getFile();

    final MZmineLogFileReader reader = new MZmineLogFileReader();
    final LogFile logFile = reader.readLogFile(new File(file));

    assertEquals(0, logFile.count(Severity.ERROR));
    assertEquals(22, logFile.count(Severity.WARN));
    assertEquals(505, logFile.count(Severity.INFO));

    // specialized info
    List<LogFileLine> exceptions = logFile.findExceptions();
    assertEquals(12, exceptions.size());

    // speed of batch
    List<LogFileLine> speed = logFile.findBatchSpeed();
    assertEquals(1, speed.size());

    // monitor states
    assertEquals(0, logFile.count(LogFileState.PRE_INIT));
    assertEquals(9, logFile.count(LogFileState.INIT_MZMINE));
    assertEquals(244, logFile.count(LogFileState.LOAD_CONFIG));
    assertEquals(206, logFile.count(LogFileState.RUN_BATCH));
    assertEquals(68, logFile.count(LogFileState.RUN_MANUAL));
  }


  @Test
  void parseLogLine() {
    final MZmineLogFileReader reader = new MZmineLogFileReader();

    // First log line
    String logLine = "2025-03-20 13:24:30 INFO   io.github.mzmine.modules.batchmode.BatchTask method Starting step # 24";
    var line = reader.convertToLogLine(logLine).get();
    assertEquals(LocalDateTime.parse("2025-03-20T13:24:30"), line.dateTime()); // Date
    assertEquals(Severity.INFO, line.severity());       // Log Level
    assertEquals("io.github.mzmine.modules.batchmode.BatchTask", line.originClass()); // Class Name
    assertEquals("method", line.method()); // message
    assertEquals("Starting step # 24", line.message()); // message

    logLine = "2025-03-20 13:24:35 WARNING   io.github.mzmine.modules.batchmode.BatchTask method Starting step # 24";
    line = reader.convertToLogLine(logLine).get();
    assertEquals(LocalDateTime.parse("2025-03-20T13:24:35"), line.dateTime()); // Date
    assertEquals(Severity.WARN, line.severity());       // Log Level
    assertEquals("io.github.mzmine.modules.batchmode.BatchTask", line.originClass()); // Class Name
    assertEquals("Starting step # 24", line.message()); // message

    logLine = "2025-03-20 13:24:35 ERROR   io.github.mzmine.modules.batchmode.BatchTask method Starting step # 24";
    line = reader.convertToLogLine(logLine).get();
    assertEquals(LocalDateTime.parse("2025-03-20T13:24:35"), line.dateTime()); // Date
    assertEquals(Severity.ERROR, line.severity());       // Log Level
    assertEquals("io.github.mzmine.modules.batchmode.BatchTask", line.originClass()); // Class Name
    assertEquals("Starting step # 24", line.message()); // message

//    // Second log line
    logLine = "2025-03-20 13:24:30 FINEST io.github.mzmine.modules.batchmode.BatchTask method Module Save current batch called at 2025-03-20T12:24:30.168700800Z";
    line = reader.convertToLogLine(logLine).get();
    assertEquals(LocalDateTime.parse("2025-03-20T13:24:30"), line.dateTime()); // Date
    assertEquals(Severity.INFO, line.severity());       // Log Level
    assertEquals("io.github.mzmine.modules.batchmode.BatchTask", line.originClass()); // Class Name
    assertEquals("Module Save current batch called at 2025-03-20T12:24:30.168700800Z",
        line.message()); // message
  }
}