/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.util.javafx;

import com.google.common.base.Preconditions;
import io.github.mzmine.main.MZmineCore;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

public class FxThreadUtil {

  private static final Logger logger = Logger.getLogger(FxThreadUtil.class.getName());

  /**
   * Simulates Swing's invokeAndWait(). Based on https://news.kynosarges.org/2014/05/01/simulating-platform-runandwait/
   */
  public static void runOnFxThreadAndWait(@NotNull Runnable action) {

    Preconditions.checkNotNull(action);

    // is headless mode or already runs synchronously on JavaFX thread
    if (MZmineCore.isHeadLessMode() || Platform.isFxApplicationThread()) {
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
      logger.log(Level.SEVERE, "Error during run and await for FXThread");
      logger.log(Level.SEVERE, e.getMessage(), e);
    }

  }

}
