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
import io.github.mzmine.gui.chartbasics.simplechart.datasets.XYDatasetAndRenderer;
import io.github.mzmine.taskcontrol.Task;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.TreeMap;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.xy.XYDataset;

public class FxXYPlotModel implements FxPlotModel {

  private static final Logger logger = Logger.getLogger(FxXYPlotModel.class.getName());
  private final ObjectProperty<@Nullable XYPlot> plot = new SimpleObjectProperty<>();
  private final ReadOnlyObjectWrapper<@Nullable JFreeChart> chart = new ReadOnlyObjectWrapper<>();

  private final ListProperty<MarkerDefinition> domainMarkers = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ListProperty<MarkerDefinition> rangeMarkers = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  /**
   * Separation between permanent markers that are always kept on the chart and those that are often
   * cleared, removed, and new ones added ({@link #domainMarkers} and {@link #rangeMarkers}).
   * <p>
   * In any way, markers are now Observable when using {@link FxValueMarker} and can change its
   * value, visible state, and style.
   */
  private final ListProperty<MarkerDefinition> permanentDomainMarkers = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  /**
   * Separation between permanent markers that are always kept on the chart and those that are often
   * cleared, removed, and new ones added ({@link #domainMarkers} and {@link #rangeMarkers}).
   * <p>
   * In any way, markers are now Observable when using {@link FxValueMarker} and can change its
   * value, visible state, and style.
   */
  private final ListProperty<MarkerDefinition> permanentRangeMarkers = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  /**
   * index of dataset against dataset. sorted by keys
   */
  private final MapProperty<Integer, @NotNull XYDataset> datasets = new SimpleMapProperty<>(
      FXCollections.observableMap(new TreeMap<>()));

  /**
   * index of dataset against renderer. sorted by keys. Index corresponds to the datasets map
   */
  private final MapProperty<Integer, @NotNull XYItemRenderer> renderers = new SimpleMapProperty<>(
      FXCollections.observableHashMap());

  private final PlotCursorConfigModel cursorConfigModel;

  /**
   * rendering info is passed on from {@link FxEChartViewerModel} and {@link FxJFreeChartModel} and
   * represents the latest draw event. Useful for calculation on screen sizes
   */
  private final ObjectProperty<ChartRenderingInfo> renderingInfo = new SimpleObjectProperty<>();

  private final List<DatasetChangeListener> datasetChangeListeners = new ArrayList<>();


  public FxXYPlotModel(@Nullable XYPlot plot) {
    this.plot.set(plot);
    chart.bind(this.plot.map(xyPlot -> xyPlot != null ? xyPlot.getChart() : null).orElse(null));
    cursorConfigModel = new PlotCursorConfigModel(this::getAllDatasets);
  }

  /**
   * @return A copy of the current datasets
   */
  public @NotNull List<XYDataset> getAllDatasets() {
    return List.copyOf(datasets.values());
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


  @NotNull
  public MapProperty<Integer, @NotNull XYDataset> datasetsProperty() {
    return datasets;
  }

  public List<DatasetChangeListener> getDatasetChangeListeners() {
    return datasetChangeListeners;
  }

  public void addDatasetChangeListener(DatasetChangeListener listener) {
    datasetChangeListeners.add(listener);
  }

  public void removeDatasetChangeListener(DatasetChangeListener listener) {
    datasetChangeListeners.remove(listener);
  }

  public void clearDatasetChangeListeners() {
    datasetChangeListeners.clear();
  }

  /**
   * Replaces all
   */
  public void setDatasets(SequencedCollection<? extends XYDataset> datasets) {
    final ObservableMap<Integer, XYDataset> map = FXCollections.observableMap(new TreeMap<>());
    for (XYDataset dataset : datasets) {
      if (dataset == null) {
        continue;
      }
      map.put(map.size(), dataset);
    }

    this.datasets.setValue(map);
  }

  public MapProperty<Integer, XYItemRenderer> renderersProperty() {
    return renderers;
  }

  /**
   * Replaces all
   */
  public void setRenderers(SequencedCollection<? extends XYItemRenderer> renderers) {
    final ObservableMap<Integer, XYItemRenderer> map = FXCollections.observableMap(new TreeMap<>());
    for (XYItemRenderer renderer : renderers) {
      if (renderer == null) {
        continue;
      }
      map.put(map.size(), renderer);
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


  public ObservableList<MarkerDefinition> getDomainMarkers() {
    return domainMarkers.get();
  }

  public ListProperty<MarkerDefinition> domainMarkersProperty() {
    return domainMarkers;
  }

  public ObservableList<MarkerDefinition> getRangeMarkers() {
    return rangeMarkers.get();
  }

  public ListProperty<MarkerDefinition> rangeMarkersProperty() {
    return rangeMarkers;
  }

  /**
   * Separation between permanent markers that are always kept on the chart and those that are often
   * cleared, removed, and new ones added ({@link #domainMarkers} and {@link #rangeMarkers}).
   * <p>
   * In any way, markers are now Observable when using {@link FxValueMarker} and can change its
   * value, visible state, and style.
   */
  public ObservableList<MarkerDefinition> getPermanentDomainMarkers() {
    return permanentDomainMarkers.get();
  }

  /**
   * Separation between permanent markers that are always kept on the chart and those that are often
   * cleared, removed, and new ones added ({@link #domainMarkers} and {@link #rangeMarkers}).
   * <p>
   * In any way, markers are now Observable when using {@link FxValueMarker} and can change its
   * value, visible state, and style.
   */
  public ListProperty<MarkerDefinition> permanentDomainMarkersProperty() {
    return permanentDomainMarkers;
  }

  public ObservableList<MarkerDefinition> getPermanentRangeMarkers() {
    return permanentRangeMarkers.get();
  }

  public ListProperty<MarkerDefinition> permanentRangeMarkersProperty() {
    return permanentRangeMarkers;
  }

  // DATASETS

  /**
   * Add to first free index
   */
  public int addDataset(@Nullable XYDataset dataset, @Nullable XYItemRenderer renderer) {
    if (dataset == null) {
      return -1;
    }
    for (int i = 0; i <= datasets.size(); i++) {
      if (datasets.get(i) == null) {
        datasets.put(i, dataset);
        if (renderer != null) {
          renderers.put(i, renderer);
        }
        return i;
      }
    }

    throw new IllegalStateException("No free index found for dataset but should be within <=size");
  }

  /**
   * Add all datasets to respective first free indices. Will only trigger one update for datasets
   * and one for renderers
   */
  public void addDatasets(@NotNull List<? extends XYDataset> newDatasets,
      @Nullable List<? extends XYItemRenderer> newRenderers) {
    assert newRenderers == null || newRenderers.size()
        == newDatasets.size() : "Renderers do not have the same size like datasets";

    Map<Integer, XYDataset> dataToAdd = new TreeMap<>();
    Map<Integer, XYItemRenderer> renderToAdd = new TreeMap<>();

    int currentDatasetIndex = 0;
    for (int newDataIndex = 0; newDataIndex < newDatasets.size(); newDataIndex++) {
      boolean searching = true;
      while (searching) {
        if (datasets.get(currentDatasetIndex) == null) {
          dataToAdd.put(currentDatasetIndex, newDatasets.get(newDataIndex));
          if (newRenderers != null) {
            renderToAdd.put(currentDatasetIndex, newRenderers.get(newDataIndex));
          }
          searching = false;
        }
        currentDatasetIndex++;
      }
    }
    datasets.putAll(dataToAdd);
    if (newRenderers != null) {
      renderers.putAll(renderToAdd);
    }
  }

  @Nullable
  public XYDataset getDataset(int index) {
    if (index < 0) {
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
      throw new IndexOutOfBoundsException("Index must be >= 0");
    }
    if (dataset == null) {
      datasets.remove(index);
    } else {
      datasets.put(index, dataset);
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

  public void setRenderers(XYItemRenderer[] renderers) {
    setRenderers(Arrays.asList(renderers));
  }

  public int getIndexOf(XYDataset dataset) {
    for (Entry<Integer, XYDataset> entry : datasets.entrySet()) {
      if (Objects.equals(entry.getValue(), dataset)) {
        return entry.getKey();
      }
    }
    return -1;
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
    if (renderer == null) {
      removeRenderer(index);
    } else {
      renderers.put(index, renderer);
    }
  }

  public XYItemRenderer getRenderer(int index) {
    if (index < 0) {
      // jfreechart would return null if the remderer was set once
      return null;
    }
    return renderers.get(index);
  }

// MARKERS

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   * <p>
   * Prefer {@link FxMarker} for markers that may change their visibility or value.
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
   * <p>
   * Prefer {@link FxMarker} for markers that may change their visibility or value.
   */
  public void setAllDomainMarkers(Marker... markers) {
    setAllDomainMarkers(Arrays.stream(markers).filter(Objects::nonNull).map(MarkerDefinition::new)
        .toArray(MarkerDefinition[]::new));
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   * <p>
   * Prefer {@link FxMarker} for markers that may change their visibility or value.
   */
  public void setAllDomainMarkers(Collection<Marker> markers) {
    setAllDomainMarkers(markers.stream().filter(Objects::nonNull).map(MarkerDefinition::new)
        .toArray(MarkerDefinition[]::new));
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   * <p>
   * Prefer {@link FxMarker} for markers that may change their visibility or value.
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
   * Separation between permanent markers that are always kept on the chart and those that are often
   * cleared, removed, and new ones added ({@link #domainMarkersProperty()} and
   * {@link #rangeMarkersProperty()}).
   * <p>
   * In any way, markers are now Observable when using {@link FxValueMarker} and can change its
   * value, visible state, and style.
   */
  public void addPermanentDomainMarker(int index, Marker marker, Layer layer) {
    // change will happen through property subscription
    permanentDomainMarkers.add(new MarkerDefinition(index, marker, layer));
  }

  /**
   * Separation between permanent markers that are always kept on the chart and those that are often
   * cleared, removed, and new ones added ({@link #domainMarkersProperty()} and
   * {@link #rangeMarkersProperty()}).
   * <p>
   * In any way, markers are now Observable when using {@link FxValueMarker} and can change its
   * value, visible state, and style.
   */
  public void addPermanentRangeMarker(int index, Marker marker, Layer layer) {
    // change will happen through property subscription
    permanentRangeMarkers.add(new MarkerDefinition(index, marker, layer));
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   * <p>
   * Prefer {@link FxMarker} for markers that may change their visibility or value.
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
   * <p>
   * Prefer {@link FxMarker} for markers that may change their visibility or value.
   */
  public void setAllRangeMarkers(Marker... markers) {
    setAllRangeMarkers(Arrays.stream(markers).filter(Objects::nonNull).map(MarkerDefinition::new)
        .toArray(MarkerDefinition[]::new));
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   * <p>
   * Prefer {@link FxMarker} for markers that may change their visibility or value.
   */
  public void setAllRangeMarkers(Collection<Marker> markers) {
    setAllRangeMarkers(markers.stream().filter(Objects::nonNull).map(MarkerDefinition::new)
        .toArray(MarkerDefinition[]::new));
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   * <p>
   * Prefer {@link FxMarker} for markers that may change their visibility or value.
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
    if (index < 0) {
      // jfreechart would return null if the dataset was set once
      return null;
    }
    final XYDataset ds = datasets.get(index);
    if (ds instanceof Task) { // stop calculation in case it's still running
      ((Task) ds).cancel();
    }

    datasets.remove(index);
    removeRenderer(index);
    return ds;
  }

  private @Nullable XYItemRenderer removeRenderer(int index) {
    if (index < 0) {
      // jfreechart would return null if the renderer was set once
      return null;
    }
    return renderers.remove(index);
  }

  public void removeAllDatasets() {
    applyWithNotifyChanges(false, () -> {
      for (XYDataset ds : datasets.values()) {
        if (ds instanceof Task) {
          ((Task) ds).cancel();
        }
      }

      datasets.clear();
      renderers.clear();
    });
  }

  @Override
  public @NotNull ObjectProperty<@Nullable ChartRenderingInfo> renderingInfoProperty() {
    return renderingInfo;
  }

  /**
   *
   * @return index of removed dataset or -1 if not found
   */
  public int removeDataSet(XYDataset dataset) {
    if (dataset == null) {
      return -1;
    }
    final int index = getIndexOf(dataset);
    if (index == -1) {
      return index;
    }
    removeDataSet(index);
    return index;
  }

  /**
   * Convenience method to set both datasets and renderers at once
   */
  public void setDatasets(SequencedCollection<? extends XYDataset> datasets,
      SequencedCollection<? extends XYItemRenderer> renderers) {
    setDatasets(datasets);
    setRenderers(renderers);
  }


  /**
   * Replaces all datasets and renderers
   */
  public void setDatasetsRenderers(SequencedCollection<XYDatasetAndRenderer> datasets) {
    final var ds = datasets.stream().map(XYDatasetAndRenderer::dataset).toList();
    final var rs = datasets.stream().map(XYDatasetAndRenderer::renderer).toList();
    setDatasets(ds, rs);
  }

  /**
   * Adds datasets and renderers
   *
   */
  public void addDatasetsRenderers(SequencedCollection<XYDatasetAndRenderer> datasets) {
    final var ds = datasets.stream().map(XYDatasetAndRenderer::dataset).toList();
    final var rs = datasets.stream().map(XYDatasetAndRenderer::renderer).toList();
    addDatasets(ds, rs);
  }

  public void clearDomainMarkers(int datasetIndex) {
    domainMarkers.removeIf(m -> m.index() == datasetIndex);
  }

  public void clearRangeMarkers(int datasetIndex) {
    rangeMarkers.removeIf(m -> m.index() == datasetIndex);
  }

  public boolean removeDomainMarker(int index, Marker marker, Layer layer) {
    return domainMarkers.removeIf(
        m -> m.index() == index && m.marker().equals(marker) && m.layer() == layer);
  }

  public boolean removeRangeMarker(int index, Marker marker, Layer layer) {
    return rangeMarkers.removeIf(
        m -> m.index() == index && m.marker().equals(marker) && m.layer() == layer);
  }

}
