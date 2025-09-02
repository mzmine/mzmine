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

package io.github.mzmine.gui.chartbasics.gui.javafx.model;

import io.github.mzmine.gui.chartbasics.FxChartFactory;
import io.github.mzmine.gui.chartbasics.gui.javafx.MarkerDefinition;
import io.github.mzmine.gui.chartbasics.gui.javafx.XYPlotWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.taskcontrol.Task;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Logger;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.data.xy.XYDataset;

/**
 * Prefer to use {@link FxXYPlot} as a direct implementation of a plot that uses
 * {@link FxXYPlotModel} and is created by for example {@link FxChartFactory}.
 * <p>
 * This wrapper makes it possible to use other XYPlot for example for already existing plots.
 *
 */// TODO most likely will remove this class. Was first try at abstracting away plot with a wrapper, had issues that not all functions are public
@Deprecated
public class FxXYPlotWrapper extends XYPlotWrapper {

  private static final Logger logger = Logger.getLogger(FxXYPlotWrapper.class.getName());

  private final ListProperty<MarkerDefinition> domainMarkers = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ListProperty<MarkerDefinition> rangeMarkers = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  private final ListProperty<XYDataset> datasets = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  /**
   * index of dataset against renderer. Index corresponds to the datasets list
   */
  private final MapProperty<Integer, XYItemRenderer> renderers = new SimpleMapProperty<>(
      FXCollections.observableHashMap());

  private final ObjectProperty<@Nullable PlotCursorPosition> cursorPosition = new SimpleObjectProperty<>();

  public FxXYPlotWrapper(XYPlot plot) {
    super(plot);

    applyNotifyLater(datasets, nv -> {
      logger.fine("DATASETS CHANGED " + datasets.size());
      for (int i = 0; i < nv.size(); i++) {
        // finally set changes to plot
        plot.setDataset(i, nv.get(i));
      }
      for (int i = nv.size(); i < plot.getDatasetCount(); i++) {
        plot.setDataset(i, null);
      }
    });

    applyNotifyLater(renderers, map -> {
      logger.fine("RENDERERS CHANGED " + renderers.size());
      // erase all because we never know if all datasets have a renderer
      // also order in which dataset and renderer is set may differ
      for (int i = 0; i < plot.getRendererCount(); i++) {
        // finally set changes to plot
        plot.setRenderer(i, null);
      }
      // now set all renderers
      for (Entry<Integer, XYItemRenderer> entry : map.entrySet()) {
        final Integer index = entry.getKey();
        plot.setRenderer(index, entry.getValue());
      }
    });

    applyNotifyLater(domainMarkers, nv -> {
      // finally set changes to plot
      plot.clearDomainMarkers();
      for (MarkerDefinition m : nv) {
        plot.addDomainMarker(m.index(), m.marker(), m.layer());
      }
    });
    applyNotifyLater(rangeMarkers, nv -> {
      // finally set changes to plot
      plot.clearRangeMarkers();
      for (MarkerDefinition m : nv) {
        plot.addRangeMarker(m.index(), m.marker(), m.layer());
      }
    });
  }

  // properties
  public ObservableList<XYDataset> getDatasets() {
    return datasets.get();
  }

  public ListProperty<XYDataset> datasetsProperty() {
    return datasets;
  }

  public void setDatasets(List<XYDataset> datasets) {
    this.datasets.getValue().setAll(datasets);
  }

  public MapProperty<Integer, XYItemRenderer> renderersProperty() {
    return renderers;
  }

  public void setRenderers(List<XYItemRenderer> renderers) {
    final ObservableMap<Integer, XYItemRenderer> map = FXCollections.observableHashMap();
    for (int i = 0; i < renderers.size(); i++) {
      map.put(i, renderers.get(i));
    }
    this.renderers.setValue(map);
  }

  public void setCursorPosition(@Nullable PlotCursorPosition cursorPosition) {
    this.cursorPosition.set(cursorPosition);
  }

  public @Nullable PlotCursorPosition getCursorPosition() {
    return cursorPosition.get();
  }

  public ObjectProperty<@Nullable PlotCursorPosition> cursorPositionProperty() {
    return cursorPosition;
  }

  public ListProperty<MarkerDefinition> domainMarkersProperty() {
    return domainMarkers;
  }

  public ListProperty<MarkerDefinition> rangeMarkersProperty() {
    return rangeMarkers;
  }

  // DATASETS

  public void addDataset(XYDataset dataset, @Nullable XYItemRenderer renderer) {
    if (dataset == null) {
      return;
    }
    if (renderer != null) {
      // set dataset index for renderer
      renderers.put(datasets.size(), renderer);
    }
    datasets.add(dataset);
  }

  @Override
  public XYDataset getDataset(int index) {
    return datasets.get(index);
  }

  @Override
  public void setDataset(XYDataset dataset) {
    setDataset(0, dataset);
  }

  @Override
  public void setDataset(int index, XYDataset dataset) {
    if (index < 0) {
      return;
    } else if (index < datasets.size()) {
      if (dataset == null) {
        datasets.remove(index);
      }
    } else if (index == datasets.size()) {
      addDataset(dataset, null);
    } else {
      throw new IndexOutOfBoundsException(
          "Cannot add dataset to index: " + index + ", for list with size: " + datasets.size());
    }
  }

  @Override
  public int getDatasetCount() {
    return datasets.size();
  }

  @Override
  public int getRendererCount() {
    return renderers.size();
  }

  @Override
  public void setRenderer(int index, XYItemRenderer renderer, boolean notify) {
    renderers.put(index, renderer);
  }

  @Override
  public void setRenderers(XYItemRenderer[] renderers) {
    setRenderers(Arrays.asList(renderers));
  }

  @Override
  public int getIndexOf(XYItemRenderer renderer) {
    for (Entry<Integer, XYItemRenderer> entry : renderers.entrySet()) {
      if (Objects.equals(entry.getValue(), renderer)) {
        return entry.getKey();
      }
    }
    return -1;
  }

  @Override
  public void setRenderer(XYItemRenderer renderer) {
    setRenderer(0, renderer);
  }

  @Override
  public void setRenderer(int index, XYItemRenderer renderer) {
    renderers.put(index, renderer);
  }

  @Override
  public XYItemRenderer getRenderer(int index) {
    return super.getRenderer(index);
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

  public @Nullable XYDataset removeDataSet(int index) {
    final XYDataset ds = datasets.get(index);
    if (ds instanceof Task) { // stop calculation in case it's still running
      ((Task) ds).cancel();
    }
    if (ds != null) {
      ds.removeChangeListener(this);
    }

    applyWithNotifyChanges(false, () -> {
      setDataset(index, null);
      setRenderer(index, null);
    });
    return ds;
  }

  public void removeAllDatasets() {
    applyWithNotifyChanges(false, () -> {
      for (int i = 0; i < datasets.size(); i++) {
        XYDataset ds = datasets.get(i);
        if (ds instanceof Task) {
          ((Task) ds).cancel();
        }
        if (ds != null) {
          ds.removeChangeListener(this);
        }
      }

      datasets.clear();
      renderers.clear();
    });
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return new FxXYPlotWrapper((XYPlot) plot.clone());
  }
}
