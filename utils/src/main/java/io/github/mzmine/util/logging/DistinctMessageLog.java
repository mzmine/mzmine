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

package io.github.mzmine.util.logging;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Logs a message only if it differs from the previous one, e.g., for a background job that repeats
 * a failing request on every run. The first message is logged with its level, repeats are skipped.
 * Use one instance per source of messages, as messages of different sources would alternate and
 * never repeat.
 */
public final class DistinctMessageLog {

  private final @NotNull Logger logger;

  // guarded by this
  private @Nullable String lastMessage;
  private int repeats = 0;

  /**
   * @param logger the logger that writes the messages
   */
  public DistinctMessageLog(@NotNull final Logger logger) {
    this.logger = logger;
  }

  /**
   * @param level   the level of a message that differs from the previous one
   * @param message the message, must not contain values that change on every call, e.g., counters
   * @return true if the message was logged with the level, false if it repeated the previous one
   */
  public boolean log(@NotNull final Level level, @NotNull final String message) {
    return log(level, message, null);
  }

  /**
   * @param level   the level of a message that differs from the previous one
   * @param message the message, must not contain values that change on every call, e.g., counters
   * @param thrown  the cause, only logged with a message that differs from the previous one
   * @return true if the message was logged with the level, false if it repeated the previous one
   */
  public synchronized boolean log(@NotNull final Level level, @NotNull final String message,
      @Nullable final Throwable thrown) {
    if (message.equals(lastMessage)) {
      repeats++;
//      logger.fine(() -> "Repeated: " + message);
      return false;
    }
//    logRepeats();
    lastMessage = message;
    repeats = 0;
    logger.log(level, message, thrown);
    return true;
  }

  /**
   * Forgets the previous message, so the next message is logged again, e.g., after the failing
   * request succeeded.
   */
  public synchronized void reset() {
//    logRepeats();
    lastMessage = null;
    repeats = 0;
  }

  /**
   * Logs how often the previous message repeated, if it did. Call while holding the lock.
   */
  private void logRepeats() {
    if (repeats > 0) {
      logger.info("Repeated %dx: %s".formatted(repeats, lastMessage));
    }
  }
}
