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

package io.github.mzmine.modules.visualization.surface3d.plot;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Key;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureEvent;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.gui.chartbasics.gui.wrapper.MouseEventWrapper;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Ctrl/⌘ + drag on a chart selects a domain range, like the frame range selection of the ion
 * mobility raw data overview. The range is shown while dragging and reported on release. A press
 * without movement stays a click, so click handlers keep working.
 */
public final class Surface3DRangeDrag {

  // pixels a drag must move, less is a click
  private static final double MIN_DRAG = 3;

  private final @NotNull Consumer<Range<Double>> onRange;
  private final @NotNull Runnable onChange;
  private @Nullable Double start;
  private double startPixel;
  private @Nullable Range<Double> dragged;
  private boolean wasDragged;

  /**
   * @param onRange  receives the selected range on release
   * @param onChange called when the range shown while dragging changes, to redraw markers
   */
  public Surface3DRangeDrag(@NotNull final EChartViewer chart,
      @NotNull final Consumer<Range<Double>> onRange, @NotNull final Runnable onChange) {
    this.onRange = onRange;
    this.onChange = onChange;
    chart.getMouseAdapter().addGestureHandler(new ChartGestureHandler(
        new ChartGesture(Entity.ALL_PLOT_AND_DATA,
            new Event[]{Event.PRESSED, Event.DRAGGED, Event.RELEASED}, GestureButton.BUTTON1,
            Key.ALL), this::handle));
  }

  /**
   * @return true if Ctrl (Windows, Linux) or ⌘ (macOS) is held
   */
  public static boolean modifier(@Nullable final MouseEventWrapper mouse) {
    return mouse != null && (mouse.isControlDown() || mouse.isMetaDown());
  }

  /**
   * @return the range while dragging, null otherwise
   */
  public @Nullable Range<Double> dragged() {
    return dragged;
  }

  /**
   * @return true if the last press ended a drag, so that the following click is ignored
   */
  public boolean wasDragged() {
    return wasDragged;
  }

  private void handle(@NotNull final ChartGestureEvent e) {
    final MouseEventWrapper mouse = e.getMouseEvent();
    if (mouse == null) {
      return;
    }
    final double value = e.getCoordinates().getX();
    if (mouse.isPressed()) {
      wasDragged = false;
      start = modifier(mouse) ? value : null;
      startPixel = mouse.getX();
      dragged = null;
      return;
    }
    if (start == null) {
      return;
    }
    final boolean moved = Math.abs(mouse.getX() - startPixel) >= MIN_DRAG;
    dragged = moved ? Range.closed(Math.min(start, value), Math.max(start, value)) : null;
    if (mouse.isReleased()) {
      final Range<Double> range = dragged;
      start = null;
      dragged = null;
      wasDragged = range != null;
      if (range != null) {
        onRange.accept(range);
      }
    }
    onChange.run();
    // the drag selects a range instead of zooming
    mouse.consume();
  }
}
