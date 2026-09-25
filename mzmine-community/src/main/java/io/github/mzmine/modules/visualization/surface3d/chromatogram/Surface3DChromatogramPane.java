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

package io.github.mzmine.modules.visualization.surface3d.chromatogram;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Key;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureEvent;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gui.javafx.model.PlotCursorUtils;
import io.github.mzmine.gui.chartbasics.gui.wrapper.MouseEventWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.surface3d.plot.Surface3DPlot;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base peak chromatogram of the mobility frames. Clicking a retention time selects the frame shown
 * in the 3D view; Ctrl/⌘ + drag selects a retention time range whose frames are averaged, like
 * the frame selection of the ion mobility raw data overview.
 */
public final class Surface3DChromatogramPane extends BorderPane {

  public static final String HINT =
      "Click to show the closest frame · Ctrl/⌘ + drag to average the frames of a range";
  private static final java.awt.Color ACCENT = FxColorUtil.fxColorToAWT(Surface3DPlot.ACCENT);

  private final TICPlot chromatogram = new TICPlot();
  private final ObjectProperty<PlotCursorPosition> clicked = new SimpleObjectProperty<>();
  private @Nullable Consumer<Range<Float>> listener;
  private @Nullable Range<Float> selected;
  // retention time range while dragging
  private @Nullable Double dragStart;
  private @Nullable Range<Float> dragged;

  public record Chromatogram(@NotNull TICDataSet data, @NotNull Color color) {

  }

  public Surface3DChromatogramPane() {
    chromatogram.setMinHeight(120);
    chromatogram.setLegendVisible(false);
    chromatogram.setPlotType(TICPlotType.BASEPEAK);
    // decision: the selected frame is a permanent marker, the click crosshair would duplicate it
    chromatogram.getXYPlot().setShowCursorCrosshair(false, false);
    // a separate cursor so that repeated clicks on the same retention time are still reported
    chromatogram.getMouseAdapter().addGestureHandler(new ChartGestureHandler(
        new ChartGesture(Entity.ALL_PLOT_AND_DATA, Event.CLICK, GestureButton.BUTTON1), e -> {
      if (modifier(e.getMouseEvent())) {
        // ends a range drag
        return;
      }
      clicked.set(null);
      PlotCursorUtils.findSetCursorPosition(e, chromatogram.getRenderingInfo(),
          chromatogram.getXYPlot(), clicked);
      final PlotCursorPosition cursor = clicked.get();
      if (cursor != null && listener != null) {
        listener.accept(Range.singleton((float) cursor.getDomainValue()));
      }
    }));
    // Ctrl on Windows and Linux, ⌘ on macOS, as for m/z ranges in the spectrum
    chromatogram.getMouseAdapter().addGestureHandler(new ChartGestureHandler(
        new ChartGesture(Entity.ALL_PLOT_AND_DATA,
            new Event[]{Event.PRESSED, Event.DRAGGED, Event.RELEASED}, GestureButton.BUTTON1,
            Key.ALL), this::onRangeGesture));
    setCenter(chromatogram);
  }

  private static boolean modifier(@Nullable final MouseEventWrapper mouse) {
    return mouse != null && (mouse.isControlDown() || mouse.isMetaDown());
  }

  private void onRangeGesture(@NotNull final ChartGestureEvent e) {
    final MouseEventWrapper mouse = e.getMouseEvent();
    if (mouse == null) {
      return;
    }
    final Point2D point = e.getCoordinates();
    if (mouse.isPressed()) {
      dragStart = modifier(mouse) ? point.getX() : null;
      dragged = null;
    } else if (dragStart != null) {
      final float start = dragStart.floatValue();
      final float end = (float) point.getX();
      dragged = Range.closed(Math.min(start, end), Math.max(start, end));
      if (mouse.isReleased()) {
        final Range<Float> range = dragged;
        dragStart = null;
        dragged = null;
        if (listener != null) {
          listener.accept(range);
        }
      }
      chromatogram.applyWithNotifyChanges(false, this::applyMarker);
    } else {
      return;
    }
    // the drag selects frames instead of zooming
    mouse.consume();
  }

  /**
   * @param listener receives a single retention time for clicks and a range for Ctrl/⌘ + drag
   */
  public void setListener(@Nullable final Consumer<Range<Float>> listener) {
    this.listener = listener;
  }

  public void setChromatograms(@NotNull final List<Chromatogram> chromatograms) {
    // one redraw for all datasets and the marker
    chromatogram.applyWithNotifyChanges(false, () -> {
      chromatogram.removeAllDataSets(false);
      for (final Chromatogram value : chromatograms) {
        chromatogram.addTICDataSet(value.data(), FxColorUtil.fxColorToAWT(value.color()));
      }
      chromatogram.setLegendVisible(chromatograms.size() > 1);
      applyMarker();
    });
  }

  /**
   * @param retentionTimes retention time of the shown frame or the averaged range, null for none
   */
  public void setSelected(@Nullable final Range<Float> retentionTimes) {
    selected = retentionTimes;
    chromatogram.applyWithNotifyChanges(false, this::applyMarker);
  }

  private void applyMarker() {
    chromatogram.getXYPlot().clearDomainMarkers();
    final Range<Float> range = dragged != null ? dragged : selected;
    if (range == null) {
      return;
    }
    if (range.lowerEndpoint().equals(range.upperEndpoint())) {
      chromatogram.addDomainMarker(range.lowerEndpoint(), ACCENT, 0.9f);
    } else {
      chromatogram.addDomainMarker(range.lowerEndpoint(), range.upperEndpoint(), ACCENT, 0.25f);
    }
  }
}
