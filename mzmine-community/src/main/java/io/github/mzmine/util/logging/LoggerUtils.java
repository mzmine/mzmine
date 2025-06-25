package io.github.mzmine.util.logging;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

public class LoggerUtils {

  private static final Logger logger = Logger.getLogger(LoggerUtils.class.getName());

  public static @Nullable File getLogFile() {
    try {
      // get root logger
      return Stream.of(logger).<Handler>mapMulti((logger, downstream) -> {
            Logger parent = logger;
            while (((parent = parent.getParent()) != null)) {
              for (final Handler handler : parent.getHandlers()) {
                downstream.accept(handler);
              }
            }
          }).filter(FileHandler.class::isInstance).map(FileHandler.class::cast) // find file handlers
          .map(LoggerUtils::reflectiveAccessToLogFile) // find first file
          .filter(Objects::nonNull).findFirst().orElse(null);
    } catch (Exception ex) {
      // silent for now
    }
    return null;
  }

  private static @Nullable File reflectiveAccessToLogFile(final FileHandler fh) {
    try {
      Field filesField = fh.getClass().getDeclaredField("files");
      filesField.setAccessible(true);
      File[] logFiles = (File[]) filesField.get(fh);
      if (logFiles.length > 0 && logFiles[0] != null) {
        return logFiles[0];
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      // silent for now
      logger.log(Level.WARNING, "Cannot get logger via reflection: " + e.getMessage(), e);
    }
    return null;
  }
}
