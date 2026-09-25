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

package io.github.mzmine.modules.visualization.intensitymap.spectrum;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Entity;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.Event;
import io.github.mzmine.gui.chartbasics.gestures.ChartGesture.GestureButton;
import io.github.mzmine.gui.chartbasics.gestures.ChartGestureHandler;
import io.github.mzmine.gui.chartbasics.gui.javafx.model.PlotCursorUtils;
import io.github.mzmine.gui.chartbasics.gui.wrapper.MouseEventWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.modules.visualization.intensitymap.plot.IntensityMapPlot;
import io.github.mzmine.modules.visualization.intensitymap.plot.IntensityMapRangeDrag;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import java.util.List;
import java.util.function.Consumer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Spectrum at the position selected in the 3D view. Clicking a signal selects its m/z for the 3D
 * view; with Ctrl/⌘ further m/z ranges are added or removed, like in the image viewer. Ctrl/⌘ +
 * drag adds the dragged m/z window.
 */
public final class IntensityMapSpectrumPane extends BorderPane {

  private final SpectraPlot spectrum = new SpectraPlot(false, true);
  // shown in the header of the collapsible section, so the spectrum keeps all the space
  private final StringProperty description = new SimpleStringProperty(
      "click the 3D view to show a spectrum");
  private final ObjectProperty<PlotCursorPosition> clicked = new SimpleObjectProperty<>();
  private List<Marker> markers = List.of();
  private @Nullable Listener listener;
  private @Nullable Consumer<Range<Double>> rangeListener;
  private final IntensityMapRangeDrag drag;

  /**
   * @param mz       m/z of the clicked data point
   * @param scan     the spectrum that was clicked, if any
   * @param additive Ctrl/⌘ was held to add or remove a range instead of replacing all
   */
  public record Click(double mz, @Nullable Scan scan, boolean additive) {

  }

  public record Marker(@NotNull Range<Double> range, @NotNull Color color) {

  }

  public record Spectrum(@NotNull String label, @NotNull Scan scan, @NotNull Color color) {

  }

  @FunctionalInterface
  public interface Listener {

    void clicked(@NotNull Click click);
  }

  public IntensityMapSpectrumPane() {
    spectrum.setMinHeight(120);
    spectrum.setLegendVisible(false);
    // same gesture as the frame range in the chromatogram of mobility frames (user request)
    drag = new IntensityMapRangeDrag(spectrum, range -> {
      if (rangeListener != null) {
        rangeListener.accept(range);
      }
    }, () -> spectrum.applyWithNotifyChanges(false, this::applyMarkers));
    // a separate cursor so that repeated clicks on the same signal are still reported
    spectrum.getMouseAdapter().addGestureHandler(new ChartGestureHandler(
        new ChartGesture(Entity.ALL_PLOT_AND_DATA, Event.CLICK, GestureButton.BUTTON1), e -> {
      if (drag.wasDragged()) {
        // the release of a range drag
        return;
      }
      clicked.set(null);
      PlotCursorUtils.findSetCursorPosition(e, spectrum.getRenderingInfo(), spectrum.getXYPlot(),
          clicked);
      final PlotCursorPosition cursor = clicked.get();
      if (cursor == null || listener == null) {
        return;
      }
      final MouseEventWrapper mouse = e.getMouseEvent();
      final boolean additive = mouse != null && (mouse.isMetaDown() || mouse.isControlDown());
      final Scan scan = cursor.getDataset() instanceof ScanDataSet data ? data.getScan() : null;
      listener.clicked(new Click(cursor.getDomainValue(), scan, additive));
    }));
    setCenter(spectrum);
  }

  /**
   * @return what the shown spectrum represents, e.g. the pixel or retention time
   */
  public @NotNull StringProperty descriptionProperty() {
    return description;
  }

  public static final String HINT =
      "Click a signal to show its m/z · Ctrl/⌘ + click to add or remove m/z"
          + " · Ctrl/⌘ + drag to add an m/z window";

  public void setListener(@Nullable final Listener listener) {
    this.listener = listener;
  }

  /**
   * @param listener receives m/z windows selected by Ctrl/⌘ + drag
   */
  public void setRangeListener(@Nullable final Consumer<Range<Double>> listener) {
    rangeListener = listener;
  }

  public void setSpectra(@NotNull final String description, @NotNull final List<Spectrum> spectra) {
    this.description.set(spectra.isEmpty() ? "no spectrum at this position" : description);
    // one redraw for all datasets and markers
    spectrum.applyWithNotifyChanges(false, () -> {
      spectrum.removeAllDataSets();
      for (final Spectrum value : spectra) {
        spectrum.addDataSet(new ScanDataSet(value.label(), value.scan()),
            FxColorUtil.fxColorToAWT(value.color()), false, false, false);
      }
      spectrum.setLegendVisible(spectra.size() > 1);
      applyMarkers();
    });
  }

  /**
   * Highlights the m/z ranges shown in the 3D view.
   */
  public void setMarkers(@NotNull final List<Marker> markers) {
    this.markers = List.copyOf(markers);
    spectrum.applyWithNotifyChanges(false, this::applyMarkers);
  }

  private void applyMarkers() {
    spectrum.getXYPlot().clearDomainMarkers();
    final Range<Double> dragged = drag.dragged();
    if (dragged != null) {
      spectrum.addDomainMarker(dragged, FxColorUtil.fxColorToAWT(IntensityMapPlot.ACCENT), 0.25f);
    }
    for (final Marker marker : markers) {
      final java.awt.Color color = FxColorUtil.fxColorToAWT(marker.color());
      spectrum.addDomainMarker(marker.range(), color, 0.25f);
      // narrow ppm windows are invisible at full zoom, a center line keeps them visible
      spectrum.addDomainMarker(
          (marker.range().lowerEndpoint() + marker.range().upperEndpoint()) / 2, color, 0.8f);
    }
  }
}
