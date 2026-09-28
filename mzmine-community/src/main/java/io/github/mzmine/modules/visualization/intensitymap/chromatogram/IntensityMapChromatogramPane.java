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

package io.github.mzmine.modules.visualization.intensitymap.chromatogram;

import com.google.common.collect.Range;
import io.github.mzmine.javafx.util.FxColorUtil;
import io.github.mzmine.modules.visualization.chromatogram.TICDataSet;
import io.github.mzmine.modules.visualization.chromatogram.TICPlot;
import io.github.mzmine.modules.visualization.chromatogram.TICPlotType;
import io.github.mzmine.modules.visualization.intensitymap.plot.IntensityMapPlot;
import io.github.mzmine.modules.visualization.intensitymap.plot.IntensityMapRangeDrag;
import io.github.mzmine.modules.visualization.intensitymap.sampling.IntensityMapFrameCache;
import io.github.mzmine.util.RangeUtils;
import java.util.List;
import java.util.function.Consumer;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base peak chromatogram of the mobility frames. Clicking a retention time selects the frame shown
 * in the intensity map; Ctrl/⌘ + drag selects a retention time range whose frames are averaged,
 * like the frame selection of the ion mobility raw data overview.
 */
public final class IntensityMapChromatogramPane extends BorderPane {

  public static final String HINT = "Click to show the closest frame · Ctrl/⌘ + drag to average the frames of a range";
  private static final java.awt.Color ACCENT = FxColorUtil.fxColorToAWT(IntensityMapPlot.ACCENT);

  private final TICPlot chromatogram = new TICPlot();
  private @Nullable Consumer<Range<Float>> listener;
  private @Nullable Range<Float> selected;
  private final IntensityMapRangeDrag drag;

  public record Chromatogram(@NotNull TICDataSet data, @NotNull Color color) {

  }

  public IntensityMapChromatogramPane() {
    chromatogram.setMinHeight(120);
    chromatogram.setLegendVisible(false);
    chromatogram.setPlotType(TICPlotType.BASEPEAK);
    // decision: the selected frame is a permanent marker, the click crosshair would duplicate it
    chromatogram.getXYPlot().setShowCursorCrosshair(false, false);
    drag = new IntensityMapRangeDrag(chromatogram, range -> {
      if (listener != null) {
        listener.accept(RangeUtils.toFloatRange(range));
      }
    }, () -> chromatogram.applyWithNotifyChanges(false, this::applyMarker));
    drag.onClick((cursor, _) -> {
      if (listener != null) {
        listener.accept(Range.singleton((float) cursor.getDomainValue()));
      }
    });
    setCenter(chromatogram);
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
    final Range<Double> dragged = drag.dragged();
    if (dragged != null) {
      chromatogram.addDomainMarker(dragged.lowerEndpoint(), dragged.upperEndpoint(), ACCENT, 0.25f);
    } else if (selected != null && IntensityMapFrameCache.isSingle(selected)) {
      chromatogram.addDomainMarker(selected.lowerEndpoint(), ACCENT, 0.9f);
    } else if (selected != null) {
      chromatogram.addDomainMarker(selected.lowerEndpoint(), selected.upperEndpoint(), ACCENT,
          0.25f);
    }
  }
}
