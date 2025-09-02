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

import io.github.mzmine.gui.chartbasics.gui.javafx.MarkerDefinition;
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
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.data.xy.XYDataset;

public class FxXYPlotModel implements FxPlotModel {

  private static final Logger logger = Logger.getLogger(FxXYPlotModel.class.getName());
  private final ObjectProperty<@Nullable XYPlot> plot = new SimpleObjectProperty<>();
  private final ReadOnlyObjectWrapper<@Nullable JFreeChart> chart = new ReadOnlyObjectWrapper<>();

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

  private final PlotCursorConfigModel cursorConfigModel = new PlotCursorConfigModel();

  /**
   * rendering info is passed on from {@link FxEChartViewerModel} and {@link FxJFreeChartModel} and
   * represents the latest draw event. Useful for calculation on screen sizes
   */
  private final ObjectProperty<ChartRenderingInfo> renderingInfo = new SimpleObjectProperty<>();


  public FxXYPlotModel(@Nullable XYPlot plot) {
    this.plot.set(plot);
    chart.bind(this.plot.map(xyPlot -> xyPlot != null ? xyPlot.getChart() : null).orElse(null));
  }

  // properties
  public @Nullable XYPlot getPlot() {
    return plot.get();
  }


  public @Nullable JFreeChart getChart() {
    return chart.get();
  }

  public ObjectProperty<@Nullable XYPlot> plotProperty() {
    return plot;
  }

  public ReadOnlyObjectProperty<JFreeChart> chartProperty() {
    return chart.getReadOnlyProperty();
  }

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
    this.cursorConfigModel.setCursorPosition(cursorPosition);
  }

  public @Nullable PlotCursorPosition getCursorPosition() {
    return cursorConfigModel.getCursorPosition();
  }

  public ObjectProperty<@Nullable PlotCursorPosition> cursorPositionProperty() {
    return cursorConfigModel.cursorPositionProperty();
  }

  public PlotCursorConfigModel getCursorConfigModel() {
    return cursorConfigModel;
  }

  public ListProperty<MarkerDefinition> domainMarkersProperty() {
    return domainMarkers;
  }

  public ListProperty<MarkerDefinition> rangeMarkersProperty() {
    return rangeMarkers;
  }

  // DATASETS

  public int addDataset(XYDataset dataset, @Nullable XYItemRenderer renderer) {
    if (dataset == null) {
      return -1;
    }
    final int index = datasets.size();
    if (renderer != null) {
      // set dataset index for renderer
      renderers.put(index, renderer);
    }
    datasets.add(dataset);
    return index;
  }

  public XYDataset getDataset(int index) {
    if (index < 0 || index >= datasets.size()) {
      // jfreechart would return null if the chart was set once
      return null;
    }
    return datasets.get(index);
  }

  public void setDataset(XYDataset dataset) {
    setDataset(0, dataset);
  }

  public void setDataset(int index, XYDataset dataset) {
    if (index < 0) {
      return;
    } else if (index < datasets.size()) {
      if (dataset == null) {
        datasets.remove(index);
      } else {
        datasets.set(index, dataset);
      }
    } else if (index == datasets.size()) {
      addDataset(dataset, null);
    } else {
      throw new IndexOutOfBoundsException(
          "Cannot add dataset to index: " + index + ", for list with size: " + datasets.size());
    }
  }

  public int getDatasetCount() {
    return datasets.size();
  }

  public int getRendererCount() {
    return renderers.size();
  }

  public void removeAllRenderers() {
    renderers.clear();
  }

  public void setRenderer(int index, XYItemRenderer renderer, boolean notify) {
    renderers.put(index, renderer);
  }

  public void setRenderers(XYItemRenderer[] renderers) {
    setRenderers(Arrays.asList(renderers));
  }

  public int getIndexOf(XYItemRenderer renderer) {
    for (Entry<Integer, XYItemRenderer> entry : renderers.entrySet()) {
      if (Objects.equals(entry.getValue(), renderer)) {
        return entry.getKey();
      }
    }
    return -1;
  }

  public void setRenderer(XYItemRenderer renderer) {
    setRenderer(0, renderer);
  }

  public void setRenderer(int index, XYItemRenderer renderer) {
    renderers.put(index, renderer);
  }

  public XYItemRenderer getRenderer(int index) {
    if (index < 0 || index >= renderers.size()) {
      // jfreechart would return null if the remderer was set once
      return null;
    }
    return renderers.get(index);
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
  public void addDomainMarker(int index, Marker marker, Layer layer, boolean notify) {
    // change will happen through property subscription
    domainMarkers.add(new MarkerDefinition(index, marker, layer));
  }

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
  public void addRangeMarker(int index, Marker marker, Layer layer, boolean notify) {
    // change will happen through property subscription
    rangeMarkers.add(new MarkerDefinition(index, marker, layer));
  }

  public void clearRangeMarkers() {
    // change will happen through property subscription
    rangeMarkers.clear();
  }

  public @Nullable XYDataset removeDataSet(int index) {
    if (index < 0 || index >= datasets.size()) {
      return null;
    }
    final XYDataset ds = datasets.get(index);
    if (ds instanceof Task) { // stop calculation in case it's still running
      ((Task) ds).cancel();
    }

    applyWithNotifyChanges(false, () -> {
      setDataset(index, null);
      setRenderer(index, null);
    });
    return ds;
  }

  public void removeAllDatasets() {
    applyWithNotifyChanges(false, () -> {
      for (XYDataset ds : datasets) {
        if (ds instanceof Task) {
          ((Task) ds).cancel();
        }
      }

      datasets.clear();
      renderers.clear();
    });
  }

  @Override
  public ObjectProperty<@Nullable ChartRenderingInfo> renderingInfoProperty() {
    return renderingInfo;
  }
}
