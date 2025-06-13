/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.javafx.concurrent.threading;

import io.github.mzmine.gui.DesktopService;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

public class FxThread {

  private static final Logger logger = Logger.getLogger(FxThread.class.getName());
  private static boolean isFxInitialized = false;

  public static boolean isFxInitialized() {
    return isFxInitialized;
  }

  public static void setIsFxInitialized(boolean state) {
    isFxInitialized = state;
  }

  /**
   * @param r runnable to either run directly or on the JavaFX thread
   */
  public static void runLater(Runnable r) {
    if (DesktopService.isHeadLess() || Platform.isFxApplicationThread()) {
      r.run();
    } else {
      Platform.runLater(r);
    }
  }

  /**
   * @param r runnable to either run directly or on the JavaFX thread
   */
  public static void runLaterEnsureFxInitialized(Runnable r) {
    if (Platform.isFxApplicationThread()) {
      r.run();
    } else {
      if (!isFxInitialized) {
        initJavaFxInHeadlessMode();
      }
      Platform.runLater(r);
    }
  }

  /**
   * Simulates Swing's invokeAndWait(). Based on
   * https://news.kynosarges.org/2014/05/01/simulating-platform-runandwait/. By default this method
   * does not enforce execution on the java fx thread in headless mode. If that is required use the
   * overloaded {@link #runOnFxThreadAndWait(Runnable, boolean)} instead.
   */
  public static void runOnFxThreadAndWait(@NotNull Runnable action) {
    runOnFxThreadAndWait(action, false);
  }

  /**
   * @param forceFxThread If true, the action is forced to run on the fx thread, even if in headless
   *                      mode.
   */
  public static void runOnFxThreadAndWait(@NotNull Runnable action, boolean forceFxThread) {
    if (!isFxInitialized) {
      initJavaFxInHeadlessMode();
    }
    // is headless mode or already runs synchronously on JavaFX thread
    if ((DesktopService.isHeadLess() && !forceFxThread) || Platform.isFxApplicationThread()) {
      action.run();
      return;
    }

    // queue on JavaFX thread and wait for completion
    final CountDownLatch doneLatch = new CountDownLatch(1);
    Platform.runLater(() -> {
      try {
        action.run();
      } finally {
        doneLatch.countDown();
      }
    });

    try {
      doneLatch.await();
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
    }
  }

  /**
   * Might be needed for graphics export in headless batch mode
   */
  public static void initJavaFxInHeadlessMode() {
    if (isFxInitialized) {
      return;
    }
    Platform.startup(() -> {
    });
    isFxInitialized = true;
  }
}
