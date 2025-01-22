/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot;

import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.gui.wrapper.ChartViewWrapper;
import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYZPieDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PieXYZDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYZDataProvider;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

public class ChromatogramPlotController extends FxController<ChromatogramPlotModel> {

  private final ChromatogramPlotBuilder chromatogramPlotBuilder;

  public ChromatogramPlotController() {
    super(new ChromatogramPlotModel());
    chromatogramPlotBuilder = new ChromatogramPlotBuilder(model);
  }

  @Override
  protected @NotNull FxViewBuilder<ChromatogramPlotModel> getViewBuilder() {
    return chromatogramPlotBuilder;
  }

  public void addDataset(XYDataset dataset, XYItemRenderer renderer) {
    model.datasetRenderersProperty().put(dataset, renderer);
  }

  public void addDataset(PlotXYDataProvider dataset, XYItemRenderer renderer) {
    if (dataset instanceof PieXYZDataProvider pie) {
      model.datasetRenderersProperty()
          .put(new ColoredXYZPieDataset<PieXYZDataProvider<?>>(pie), renderer);
    }
    if (dataset instanceof PlotXYZDataProvider xyz) {
      model.datasetRenderersProperty().put(new ColoredXYZDataset(xyz), renderer);
    }
    model.datasetRenderersProperty().put(new ColoredXYDataset(dataset), renderer);
  }

  public void addDataset(DatasetAndRenderer dsr) {
    addDataset(dsr.dataset(), dsr.renderer());
  }

  public void addDatasets(List<DatasetAndRenderer> datasets) {
    model.getChart().applyWithNotifyChanges(false, () -> {
      datasets.forEach(this::addDataset);
    });
  }

  public void addDatasets(Map<XYDataset, XYItemRenderer> datasetsRenderers) {
    model.getChart().applyWithNotifyChanges(false, () -> {
      datasetsRenderers.forEach(this::addDataset);
    });
  }

  public void addXYAnnotation(XYAnnotation annotation) {
    model.annotationsProperty().add(annotation);
  }

  public void addXYAnnotations(List<XYAnnotation> annotations) {
    model.getChart().applyWithNotifyChanges(false, () -> {
      annotations.forEach(this::addXYAnnotation);
    });
  }

  public void addDomainMarker(ValueMarker marker) {
    model.domainAxisMarkersProperty().add(marker);
  }

  public void addDomainMarkers(List<ValueMarker> markers) {
    model.getChart().applyWithNotifyChanges(false, () -> {
      markers.forEach(this::addDomainMarker);
    });
  }

  public Map<XYDataset, XYItemRenderer> getDatasetRenderers() {
    return model.datasetRenderersProperty().get();
  }

  public void removeDataset(XYDataset dataset) {
    model.datasetRenderersProperty().remove(dataset);
  }

  public void removeAnnotation(XYAnnotation annotation) {
    model.annotationsProperty().remove(annotation);
  }

  public void removeDomainMarker(ValueMarker marker) {
    model.domainAxisMarkersProperty().remove(marker);
  }

  public void setDataset(XYDataset dataset, XYItemRenderer renderer) {
    model.getChart().applyWithNotifyChanges(false, () -> {
      model.datasetRenderersProperty().clear();
      model.datasetRenderersProperty().put(dataset, renderer);
    });
  }

  public void clearDatasets() {
    model.getChart().applyWithNotifyChanges(false, () -> {
      model.datasetRenderersProperty().clear();
    });
  }

  public void clearPlot() {
    model.getChart().applyWithNotifyChanges(false, () -> {
      model.datasetRenderersProperty().clear();
      model.annotationsProperty().clear();
      model.domainAxisMarkersProperty().clear();
    });
  }

  public ObjectProperty<@Nullable PlotCursorPosition> cursorPositionProperty() {
    return model.cursorPositionProperty();
  }

  public StringProperty rangeAxisLabel() {
    return model.rangeLabelProperty();
  }

  public void setRangeAxisLabel(String label) {
    rangeAxisLabel().set(label);
  }

  public ObjectProperty<NumberFormat> rangeAxisFormat() {
    return model.rangeAxisFormatProperty();
  }

  public void setRangeAxisFormat(NumberFormat format) {
    rangeAxisFormat().set(format);
  }

  public StringProperty domainAxisLabel() {
    return model.domainLabelProperty();
  }

  public void setDomainAxisLabel(String label) {
    domainAxisLabel().set(label);
  }

  public ObjectProperty<NumberFormat> domainAxisFormat() {
    return model.domainAxisFormatProperty();
  }

  public void setDomainAxisFormat(NumberFormat format) {
    domainAxisFormat().set(format);
  }

  public void setTitle(String title) {
    // todo apply chart theme
    model.setTitle(title);
  }

  public StringProperty title() {
    return model.titleProperty();
  }

  public void setChartGroup(ChartGroup chartGroup) {
    chartGroup.add(new ChartViewWrapper(model.getChart()));
  }

  public void setRangeAxisStickyZero(boolean stickyZero) {
    model.getChart().setStickyZeroRangeAxis(stickyZero);
  }
}
