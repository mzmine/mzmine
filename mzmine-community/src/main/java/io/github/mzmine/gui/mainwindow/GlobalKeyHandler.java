/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.gui.mainwindow;

import io.github.mzmine.modules.batchmode.ModuleQuickSelectDialog;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Those keys can be added to other dialogs to enable the same behavior like in the main window.
 * E.g. quick search. Adds support for double-click on keys like double shift
 */
public class GlobalKeyHandler implements EventHandler<KeyEvent> {

  private static final Logger logger = Logger.getLogger(GlobalKeyHandler.class.getName());

  public static final GlobalKeyHandler instance = new GlobalKeyHandler();
  private KeyEvent lastKeyEvent = null;
  private Instant lastKeyEventTime;

  private GlobalKeyHandler() {
  }

  public static GlobalKeyHandler getInstance() {
    return instance;
  }

  @Override
  public void handle(final KeyEvent event) {
    Instant now = Instant.now();

    long timeDiff =
        lastKeyEventTime != null ? Duration.between(lastKeyEventTime, now).toMillis() : -1;
    // there is an issue with an editable ComboBox duplicating key events with 0-1 ms delay
    // therefore filter events that occur within 4 ms of each other
    if (timeDiff <= 5 || timeDiff > 600) {
      lastKeyEvent = null;
    }

    // some debugging message in case issues arise
//    logger.info("KEY %s as %s and lastKEY %s times %s - %s diff %d ms".formatted(event.getCode(),
//        event.getEventType(), lastKeyEvent != null ? lastKeyEvent.getCode() : "null", now,
//        lastKeyEventTime != null ? lastKeyEventTime : "null", timeDiff));

    if (event.getCode() == KeyCode.F && event.isShortcutDown()) {
      ModuleQuickSelectDialog.openQuickSearch();
      event.consume();
    } else if (event.getCode() == KeyCode.SHIFT) {
      if (lastKeyEvent != null && lastKeyEvent.getCode() == KeyCode.SHIFT) {
        ModuleQuickSelectDialog.openQuickSearch();
        event.consume();
        lastKeyEvent = null;
        return;
      }
    }
    lastKeyEventTime = now;
    lastKeyEvent = event;
  }
}
