/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.gui.chartbasics.gui.javafx;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.Layer;

public class FxXYPlotWrapper extends XYPlotWrapper {

  private final ListProperty<MarkerDefinition> domainMarkers = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ListProperty<MarkerDefinition> rangeMarkers = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  public FxXYPlotWrapper(XYPlot plot) {
    super(plot);

    applyNotifyLater(domainMarkers, nv -> {
      plot.clearDomainMarkers();
      for (MarkerDefinition m : nv) {
        plot.addDomainMarker(m.index(), m.marker(), m.layer());
      }
    });
    applyNotifyLater(rangeMarkers, nv -> {
      plot.clearRangeMarkers();
      for (MarkerDefinition m : nv) {
        plot.addRangeMarker(m.index(), m.marker(), m.layer());
      }
    });
  }

  // MARKERS

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setSingleDomainRangeMarker(@Nullable MarkerDefinition domain,
      @Nullable MarkerDefinition range) {
    applyWithNotifyChanges(false, () -> {
      setAllDomainMarkers(domain);
      setAllRangeMarkers(range);
    });
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setSingleDomainRangeMarker(@Nullable Marker domain, @Nullable Marker range) {
    setSingleDomainRangeMarker(domain == null ? null : new MarkerDefinition(domain),
        range == null ? null : new MarkerDefinition(range));
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setAllDomainMarkers(MarkerDefinition... markers) {
    markers = Arrays.stream(markers).filter(Objects::nonNull).toArray(MarkerDefinition[]::new);
    if (markers.length == 0) {
      clearDomainMarkers();
      return;
    }
    domainMarkers.setAll(markers);
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setAllDomainMarkers(Marker... markers) {
    setAllDomainMarkers(Arrays.stream(markers).filter(Objects::nonNull).map(MarkerDefinition::new)
        .toArray(MarkerDefinition[]::new));
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setAllDomainMarkers(List<Marker> markers) {
    setAllDomainMarkers(markers.stream().filter(Objects::nonNull).map(MarkerDefinition::new)
        .toArray(MarkerDefinition[]::new));
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  @Override
  public void addDomainMarker(int index, Marker marker, Layer layer, boolean notify) {
    // change will happen through property subscription
    domainMarkers.add(new MarkerDefinition(index, marker, layer));
  }

  @Override
  public void clearDomainMarkers() {
    // change will happen through property subscription
    domainMarkers.clear();
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setAllRangeMarkers(MarkerDefinition... markers) {
    markers = Arrays.stream(markers).filter(Objects::nonNull).toArray(MarkerDefinition[]::new);
    if (markers.length == 0) {
      clearRangeMarkers();
      return;
    }
    rangeMarkers.setAll(markers);
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setAllRangeMarkers(Marker... markers) {
    setAllRangeMarkers(Arrays.stream(markers).filter(Objects::nonNull).map(MarkerDefinition::new)
        .toArray(MarkerDefinition[]::new));
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setAllRangeMarkers(List<Marker> markers) {
    setAllRangeMarkers(markers.stream().filter(Objects::nonNull).map(MarkerDefinition::new)
        .toArray(MarkerDefinition[]::new));
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  @Override
  public void addRangeMarker(int index, Marker marker, Layer layer, boolean notify) {
    // change will happen through property subscription
    rangeMarkers.add(new MarkerDefinition(index, marker, layer));
  }

  @Override
  public void clearRangeMarkers() {
    // change will happen through property subscription
    rangeMarkers.clear();
  }

}
