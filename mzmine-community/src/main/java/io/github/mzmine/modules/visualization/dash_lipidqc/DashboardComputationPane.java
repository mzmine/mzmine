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

package io.github.mzmine.modules.visualization.dash_lipidqc;

import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.javafx.mvci.LatestTaskScheduler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for all dashboard computation panes. Manages a shared {@link LatestTaskScheduler} and
 * a centred placeholder label that is shown whenever no computation result is available.
 */
public abstract class DashboardComputationPane extends BorderPane {

  private static final Duration UPDATE_DELAY = Duration.millis(120);

  protected final @NotNull LatestTaskScheduler scheduler = new LatestTaskScheduler();
  protected final @NotNull Label placeholder;

  protected DashboardComputationPane(final @NotNull String initialPlaceholderText) {
    placeholder = new Label(initialPlaceholderText);
    showPlaceholder(initialPlaceholderText);
  }

  /** Schedules a computation task with the standard 120 ms debounce delay. */
  protected void scheduleUpdate(final @NotNull FxUpdateTask<?> task) {
    scheduler.onTaskThreadDelayed(task, UPDATE_DELAY);
  }

  /** Cancels any pending scheduled task. */
  protected void cancelScheduledTasks() {
    scheduler.cancelTasks();
  }

  /** Shows the placeholder label centred in this pane. */
  protected void showPlaceholder(final @NotNull String text) {
    placeholder.setText(text);
    setCenter(placeholder);
    BorderPane.setAlignment(placeholder, Pos.CENTER);
  }
}
