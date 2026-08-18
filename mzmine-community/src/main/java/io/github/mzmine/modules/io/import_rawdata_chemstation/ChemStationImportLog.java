/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 * SPDX-License-Identifier: MIT
 */
package io.github.mzmine.modules.io.import_rawdata_chemstation;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Structured diagnostic events for native ChemStation {@code .D/DATA.MS} imports.
 *
 * <p>Events are emitted through {@link Logger} so they follow mzmine's normal logging
 * configuration and end up in the standard mzmine log. Routine progress events are logged at
 * {@link Level#FINE} so they stay out of the default log unless the user raises the level;
 * failures are logged at {@link Level#WARNING}.</p>
 */
public final class ChemStationImportLog {

  private static final Logger logger = Logger.getLogger(ChemStationImportLog.class.getName());

  private ChemStationImportLog() {
  }

  public static void write(@NotNull String event, @Nullable File source,
      @Nullable String details) {
    write(event, source, details, null);
  }

  public static void write(@NotNull String event, @Nullable File source, @Nullable String details,
      @Nullable Throwable error) {
    final Level level = error == null ? Level.FINE : Level.WARNING;
    if (!logger.isLoggable(level)) {
      return;
    }
    final String message = describe(event, source, details);
    if (error == null) {
      logger.log(level, message);
    } else {
      logger.log(level, message, error);
    }
  }

  private static String describe(@NotNull String event, @Nullable File source,
      @Nullable String details) {
    final StringBuilder entry = new StringBuilder("ChemStation import event=").append(event);
    if (source != null) {
      entry.append(" source=\"").append(source.getAbsolutePath()).append('"');
      final File dataFile = source.isFile() ? source : ChemStationMsParser.findDataMsFile(source);
      if (dataFile != null) {
        entry.append(" data_ms=\"").append(dataFile.getAbsolutePath()).append('"').append(" bytes=")
            .append(dataFile.length()).append(" modified_ms=").append(dataFile.lastModified());
      }
    }
    if (details != null && !details.isBlank()) {
      entry.append(" details=\"").append(oneLine(details)).append('"');
    }
    return entry.toString();
  }

  private static String oneLine(String value) {
    return value.replace('"', '\'').replace('\r', ' ').replace('\n', ' ');
  }
}
