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
import io.github.mzmine.gui.chartbasics.listener.AllDatasetsUpdatedListener;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.XYValueProvider;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.SequencedCollection;
import java.util.logging.Logger;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.Layer;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.xy.XYDataset;

/**
 * Direct implementation of a plot that uses {@link FxXYPlotModel} and is created by for example
 * {@link FxChartFactory}. This plot should overwrite the functionality of plot, delegate to
 * properties in the model and listen for changes of the model to trigger methods on the
 * super.plot.
 *
 */
public class FxXYPlot extends XYPlot implements FxBaseChartModel {

  private static final Logger logger = Logger.getLogger(FxXYPlot.class.getName());

  private final FxXYPlotModel plotModel;

  public FxXYPlot() {
    this(null, null, null, null);
  }

  public FxXYPlot(@Nullable XYDataset dataset, @Nullable NumberAxis xAxis,
      @Nullable NumberAxis yAxis, @Nullable XYItemRenderer renderer) {
    super(null, xAxis, yAxis, null);

    // always turn off native crosshair as we use a different one within plotModel
    // getCursorConfigModel()
    super.setDomainCrosshairVisible(false);
    super.setRangeCrosshairVisible(false);

    plotModel = new FxXYPlotModel(this);
    plotModel.setDataset(dataset);
    plotModel.setRenderer(renderer);

    initListeners();
  }


  private void initListeners() {
    applyNotifyLater(plotModel.datasetsProperty(), this::updateDatasets);
    applyNotifyLater(plotModel.renderersProperty(), this::updateRenderers);
    applyNotifyLater(plotModel.domainMarkersProperty(), this::updateDomainMarkers);
    applyNotifyLater(plotModel.rangeMarkersProperty(), this::updateRangeMarkers);

    // only update on visible changes to the cursors
    getCursorConfigModel().getDomainCursorMarker().lastVisibleChangeProperty()
        .subscribe((_) -> updateDomainMarkers(plotModel.domainMarkersProperty().getValue()));
    getCursorConfigModel().getRangeCursorMarker().lastVisibleChangeProperty()
        .subscribe((_) -> updateRangeMarkers(plotModel.rangeMarkersProperty().getValue()));
  }

  private void updateAll() {
    updateDatasets(plotModel.datasetsProperty());
    updateRenderers(plotModel.renderersProperty());
    updateDomainMarkers(plotModel.domainMarkersProperty().getValue());
    updateRangeMarkers(plotModel.rangeMarkersProperty().getValue());
  }

  private void updateRangeMarkers(ObservableList<MarkerDefinition> nv) {
    if (nv != null) {
      // finally set changes to plot
      super.clearRangeMarkers();
      for (MarkerDefinition m : nv) {
        // need to use the correct method so that super does not call this
        super.addRangeMarker(m.index(), m.marker(), m.layer(), false);
      }
    }
//    logger.fine("UPDATE RANGE MARKERS %d for %s".formatted(nv == null ? 0 : nv.size(),
//        JFreeChartUtils.createChartLogIdentifier(null, getChart(), this)));

    // add the cursor marker at all times - will be invisible if no value or visible false
    super.addRangeMarker(0, getCursorConfigModel().getRangeCursorMarker(), Layer.FOREGROUND, false);
  }

  private void updateDomainMarkers(ObservableList<MarkerDefinition> nv) {
    if (nv != null) {
      // finally set changes to plot
      super.clearDomainMarkers();
      for (MarkerDefinition m : nv) {
        // need to use the correct method so that super does not call this
        super.addDomainMarker(m.index(), m.marker(), m.layer(), false);
      }
    }
//    logger.fine("UPDATE DOMAIN MARKERS %d for %s".formatted(nv == null ? 0 : nv.size(),
//        JFreeChartUtils.createChartLogIdentifier(null, getChart(), this)));

    // add the cursor marker at all times - will be invisible if no value or visible false
    super.addDomainMarker(0, getCursorConfigModel().getDomainCursorMarker(), Layer.FOREGROUND,
        false);
  }

  private void updateRenderers(ObservableMap<Integer, XYItemRenderer> map) {
    if (map == null) {
      return;
    }
//    logger.fine("UPDATE RENDERERS %d for %s".formatted(map.size(),
//        JFreeChartUtils.createChartLogIdentifier(null, getChart(), this)));
    // erase all because we never know if all datasets have a renderer
    // also order in which dataset and renderer is set may differ
    for (int i = 0; i < super.getRendererCount(); i++) {
      // finally set changes to plot - needs to use the notify flag otherwise super will call this plot
      super.setRenderer(i, null, false);
    }
    // now set all renderers
    for (Entry<Integer, XYItemRenderer> entry : map.entrySet()) {
      final Integer index = entry.getKey();
      // finally set changes to plot - needs to use the notify flag otherwise super will call this plot
      super.setRenderer(index, entry.getValue(), false);
    }
  }

  private void updateDatasets(ObservableMap<Integer, XYDataset> nv) {
    if (nv == null) {
      return;
    }
//    logger.fine("UPDATE DATASETS %d for %s".formatted(nv.size(),
//        JFreeChartUtils.createChartLogIdentifier(null, getChart(), this)));

    for (int i = 0; i < nv.size(); i++) {
      // finally set changes to plot
      super.setDataset(i, nv.get(i));
    }
    for (int i = nv.size(); i < super.getDatasetCount(); i++) {
      super.setDataset(i, null);
    }

    // fire a single event once the datasets are really set to the plot
    // this is required to ensure that the plot actually knows about the datasets
    for (AllDatasetsUpdatedListener listener : plotModel.getAllDatasetsUpdatedListeners()) {
      listener.onAllDatasetsUpdated();
    }
  }

  @Override
  public void datasetChanged(DatasetChangeEvent event) {
    super.datasetChanged(event);
    if (plotModel != null) {
      plotModel.getDatasetChangeListeners().forEach(l -> l.datasetChanged(event));
    }
  }

  /**
   * Each individual dataset change event like when a {@link XYValueProvider} was calculated in
   * {@link ColoredXYDataset} or when a dataset was set to the plot
   *
   */
  public void addDatasetChangeListener(DatasetChangeListener listener) {
    plotModel.addDatasetChangeListener(listener);
  }

  public void removeDatasetChangeListener(DatasetChangeListener listener) {
    plotModel.removeDatasetChangeListener(listener);
  }

  public void clearDatasetChangeListeners() {
    plotModel.clearDatasetChangeListeners();
  }


  /**
   * Those events are triggered once after all datasets are updated and set to the internal chart
   * every time {@link #updateDatasets(ObservableMap)} is called.
   */
  public void addAllDatasetsUpdatedListener(AllDatasetsUpdatedListener listener) {
    plotModel.addAllDatasetsUpdatedListener(listener);
  }

  // properties
  public @NotNull ObservableMap<Integer, @NotNull XYDataset> getDatasets() {
    return datasetsProperty();
  }

  public @NotNull MapProperty<Integer, @NotNull XYDataset> datasetsProperty() {
    return plotModel.datasetsProperty();
  }

  public void setDatasets(SequencedCollection<? extends XYDataset> datasets) {
    plotModel.setDatasets(datasets);
  }

  public MapProperty<Integer, XYItemRenderer> renderersProperty() {
    return plotModel.renderersProperty();
  }

  public void setRenderers(SequencedCollection<? extends XYItemRenderer> renderers) {
    plotModel.setRenderers(renderers);
  }

  public void setCursorPosition(@Nullable PlotCursorPosition cursorPosition) {
    plotModel.setCursorPosition(cursorPosition);
  }

  public @Nullable PlotCursorPosition getCursorPosition() {
    return plotModel.getCursorPosition();
  }

  public @NotNull PlotCursorConfigModel getCursorConfigModel() {
    return plotModel.getCursorConfigModel();
  }

  public void setShowCursorCrosshair(boolean domain, boolean range) {
    getCursorConfigModel().setShowCursorCrosshair(domain, range);
  }


  public ObjectProperty<@Nullable PlotCursorPosition> cursorPositionProperty() {
    return plotModel.cursorPositionProperty();
  }

  public ListProperty<MarkerDefinition> domainMarkersProperty() {
    return plotModel.domainMarkersProperty();
  }

  public ListProperty<MarkerDefinition> rangeMarkersProperty() {
    return plotModel.rangeMarkersProperty();
  }

  // crosshair

  /**
   *
   * @return always false as this is used by the internal
   * {@link XYPlot#draw(Graphics2D, Rectangle2D, Point2D, PlotState, PlotRenderingInfo)} to decide
   * and draw the crosshair.
   */
  @Deprecated
  @Override
  public boolean isDomainCrosshairVisible() {
    return false;
  }

  @Override
  public void setDomainCrosshairVisible(boolean flag) {
    getCursorConfigModel().getDomainCursorMarker().setVisible(flag);
  }

  @Override
  public boolean isDomainCrosshairLockedOnData() {
    return getCursorConfigModel().isDomainCursorLockedOnData();
  }

  @Override
  public void setDomainCrosshairLockedOnData(boolean flag) {
    getCursorConfigModel().setDomainCursorLockedOnData(flag);
  }

  @Override
  public double getDomainCrosshairValue() {
    final PlotCursorPosition pos = getCursorConfigModel().getCursorPosition();
    return pos == null ? 0 : pos.getDomainValue();
  }

  @Override
  public void setDomainCrosshairValue(double value) {
    getCursorConfigModel().setDomainCursorPosition(value);
  }

  @Override
  public void setDomainCrosshairValue(double value, boolean notify) {
    setDomainCrosshairValue(value);
  }

  @Override
  @NotNull
  public Stroke getDomainCrosshairStroke() {
    return getCursorConfigModel().getDomainCursorMarker().getStroke();
  }

  @Override
  public void setDomainCrosshairStroke(Stroke stroke) {
    getCursorConfigModel().getDomainCursorMarker().setStroke(stroke);
  }

  @Override
  public Paint getDomainCrosshairPaint() {
    return getCursorConfigModel().getDomainCursorMarker().getPaint();
  }

  @Override
  public void setDomainCrosshairPaint(Paint paint) {
    getCursorConfigModel().getDomainCursorMarker().setPaint(paint);
  }

  /**
   *
   * @return always false as this is used by the internal
   * {@link XYPlot#draw(Graphics2D, Rectangle2D, Point2D, PlotState, PlotRenderingInfo)} to decide
   * and draw the crosshair.
   */
  @Deprecated
  @Override
  public boolean isRangeCrosshairVisible() {
    return false;
  }

  @Override
  public void setRangeCrosshairVisible(boolean flag) {
    getCursorConfigModel().getRangeCursorMarker().setVisible(flag);
  }

  @Override
  public boolean isRangeCrosshairLockedOnData() {
    return getCursorConfigModel().isRangeCursorLockedOnData();
  }

  @Override
  public void setRangeCrosshairLockedOnData(boolean flag) {
    getCursorConfigModel().setRangeCursorLockedOnData(flag);
  }

  @Override
  public double getRangeCrosshairValue() {
    final PlotCursorPosition pos = getCursorConfigModel().getCursorPosition();
    return pos == null ? 0 : pos.getRangeValue();
  }

  @Override
  public void setRangeCrosshairValue(double value) {
    getCursorConfigModel().setRangeCursorPosition(value);
  }

  @Override
  public void setRangeCrosshairValue(double value, boolean notify) {
    setRangeCrosshairValue(value);
  }

  @Override
  public Stroke getRangeCrosshairStroke() {
    return getCursorConfigModel().getRangeCursorMarker().getStroke();
  }

  @Override
  public void setRangeCrosshairStroke(Stroke stroke) {
    getCursorConfigModel().getRangeCursorMarker().setStroke(stroke);
  }

  @Override
  public Paint getRangeCrosshairPaint() {
    return getCursorConfigModel().getRangeCursorMarker().getPaint();
  }

  @Override
  public void setRangeCrosshairPaint(Paint paint) {
    getCursorConfigModel().getRangeCursorMarker().setPaint(paint);
  }


  // DATASETS
  public int addDataset(XYDataset dataset, @Nullable XYItemRenderer renderer) {
    return plotModel.addDataset(dataset, renderer);
  }

  @Override
  public XYDataset getDataset(int index) {
    if (plotModel == null) {
      return null;
    }
    return plotModel.getDataset(index);
  }

  @Override
  public void setDataset(XYDataset dataset) {
    setDataset(0, dataset);
  }

  @Override
  public void setDataset(int index, XYDataset dataset) {
    if (plotModel == null) {
      return;
    }
    plotModel.setDataset(index, dataset);
  }

  /**
   * Uses the number of datasets including null as this is sometimes important internally in
   * jfreechart like when overwriting the datasets
   *
   * @return dataset count including null values, default jfreechart behavior
   */
  @Override
  public int getDatasetCount() {
    return super.getDatasetCount();
  }

  /**
   *
   * @return the number of actually set datasets from the model
   */
  public int getNonNullDatasetCount() {
    return plotModel == null ? 0 : plotModel.getDatasetCount();
  }

  /**
   * Uses the number of renderers including null as this is sometimes important internally in
   * jfreechart like when overwriting the renderers
   *
   * @return renderers count including null values, default jfreechart behavior
   */
  @Override
  public int getRendererCount() {
    return super.getRendererCount();
  }

  public int getNonNullRendererCount() {
    return plotModel == null ? 0 : plotModel.getRendererCount();
  }

  @Override
  public void setRenderer(int index, XYItemRenderer renderer, boolean notify) {
    plotModel.setRenderer(index, renderer);
  }

  @Override
  public void setRenderers(XYItemRenderer[] renderers) {
    setRenderers(Arrays.asList(renderers));
  }

  @Override
  public int getIndexOf(XYItemRenderer renderer) {
    return plotModel.getIndexOf(renderer);
  }

  @Override
  public void setRenderer(XYItemRenderer renderer) {
    setRenderer(0, renderer);
  }

  @Override
  public void setRenderer(int index, XYItemRenderer renderer) {
    if (plotModel == null) {
      return;
    }
    plotModel.setRenderer(index, renderer);
  }

  @Override
  public XYItemRenderer getRenderer(int index) {
    if (plotModel == null) {
      return null;
    }
    return plotModel.getRenderer(index);
  }

  @Override
  public XYItemRenderer getRenderer() {
    return getRenderer(0);
  }

  // MARKERS

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setSingleDomainRangeMarker(@Nullable MarkerDefinition domain,
      @Nullable MarkerDefinition range) {
    plotModel.setSingleDomainRangeMarker(domain, range);
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setSingleDomainRangeMarker(@Nullable Marker domain, @Nullable Marker range) {
    plotModel.setSingleDomainRangeMarker(domain, range);
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setAllDomainMarkers(MarkerDefinition... markers) {
    plotModel.setAllDomainMarkers(markers);
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setAllDomainMarkers(Marker... markers) {
    plotModel.setAllDomainMarkers(markers);
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setAllDomainMarkers(Collection<Marker> markers) {
    plotModel.setAllDomainMarkers(markers);
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  @Override
  public void addDomainMarker(int index, Marker marker, Layer layer, boolean notify) {
    // only need to overwrite this method with all arguments, all other overloaded methods in super call this method
    plotModel.addDomainMarker(index, marker, layer, notify);
  }

  @Override
  public void clearDomainMarkers() {
    plotModel.clearDomainMarkers();
  }

  /**
   * IMPORTANT: This method is used to clear the internal plot markers in the super plot. Cannot
   * overwrite its functionality as it is used by the updateDomainMarkersMethod
   *
   * @param index the renderer index.
   */
  @Override
  public void clearDomainMarkers(int index) {
    super.clearDomainMarkers(index);
  }

  @Override
  public boolean removeDomainMarker(int index, Marker marker, Layer layer, boolean notify) {
    // only need to overwrite this method with all arguments, all other overloaded methods in super call this method
    return plotModel.removeDomainMarker(index, marker, layer);
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setAllRangeMarkers(MarkerDefinition... markers) {
    plotModel.setAllRangeMarkers(markers);
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setAllRangeMarkers(Marker... markers) {
    plotModel.setAllRangeMarkers(markers);
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  public void setAllRangeMarkers(Collection<Marker> markers) {
    plotModel.setAllRangeMarkers(markers);
  }

  /**
   * Prefer method to set all markers at once or use the apply with notify changes method for single
   * update calls
   *
   */
  @Override
  public void addRangeMarker(int index, Marker marker, Layer layer, boolean notify) {
    // only need to overwrite this method with all arguments, all other overloaded methods in super call this method
    plotModel.addRangeMarker(index, marker, layer, notify);
  }

  @Override
  public void clearRangeMarkers() {
    plotModel.clearRangeMarkers();
  }

  /**
   * IMPORTANT: This method is used to clear the internal plot markers in the super plot. Cannot
   * overwrite its functionality as it is used by the updateDomainMarkersMethod
   *
   * @param index the renderer index.
   */
  @Override
  public void clearRangeMarkers(int index) {
    super.clearRangeMarkers(index);
  }

  @Override
  public boolean removeRangeMarker(int index, Marker marker, Layer layer, boolean notify) {
    // only need to overwrite this method with all arguments, all other overloaded methods in super call this method
    return plotModel.removeRangeMarker(index, marker, layer);
  }

  public @Nullable XYDataset removeDataSet(int index) {
    return plotModel.removeDataSet(index);
  }

  public void removeAllDatasets() {
    plotModel.removeAllDatasets();
  }

  public void removeAllRenderers() {
    plotModel.removeAllRenderers();
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    // currently this converts the plot to a regular XYPlot
    // TODO implement cloning of the plot model
    return super.clone();
  }

  @Override
  public @Nullable Plot getPlot() {
    return this;
  }

  @NotNull
  public ObjectProperty<Double> domainCursorValueProperty() {
    return getCursorConfigModel().getDomainCursorMarker().actualValueProperty();
  }

  @NotNull
  public ObjectProperty<Double> rangeCursorValueProperty() {
    return getCursorConfigModel().getRangeCursorMarker().actualValueProperty();
  }

  @Override
  public @NotNull ObjectProperty<@Nullable ChartRenderingInfo> renderingInfoProperty() {
    return plotModel.renderingInfoProperty();
  }

  public void removeDataSet(XYDataset dataset) {
    plotModel.removeDataSet(dataset);
  }

  /**
   * Convenience method to add both datasets and renderers at once
   *
   */
  public void addDatasets(@NotNull List<? extends XYDataset> datasets,
      @NotNull List<XYItemRenderer> renderers) {
    plotModel.addDatasets(datasets, renderers);
  }

  /**
   * Convenience method to add both datasets and renderers at once
   *
   * @param renderer single renderer for all datasets
   */
  public void addDatasets(@NotNull SequencedCollection<? extends XYDataset> datasets,
      @NotNull XYItemRenderer renderer) {
    plotModel.addDatasets(datasets, renderer);
  }

  /**
   * Convenience method to set both datasets and renderers at once
   *
   */
  public void setDatasets(@NotNull SequencedCollection<? extends XYDataset> datasets,
      @NotNull SequencedCollection<XYItemRenderer> renderers) {
    plotModel.setDatasets(datasets, renderers);
  }

  /**
   * Convenience method to set both datasets and renderers at once
   *
   * @param renderer single renderer for all datasets
   */
  public void setDatasets(@NotNull SequencedCollection<? extends XYDataset> datasets,
      @NotNull XYItemRenderer renderer) {
    plotModel.setDatasets(datasets, renderer);
  }
}
