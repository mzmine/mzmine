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

package io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot;

import io.github.mzmine.gui.chartbasics.simplechart.PlotCursorPosition;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import java.text.NumberFormat;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

public class ChromatogramPlotModel {

  private final ObjectProperty<@Nullable PlotCursorPosition> cursorPosition = new SimpleObjectProperty<>();

  private final MapProperty<XYDataset, XYItemRenderer> datasetRenderers = new SimpleMapProperty<>(
      FXCollections.observableHashMap());

  private final ListProperty<XYAnnotation> annotations = new SimpleListProperty<>(
      FXCollections.observableArrayList());

  private final StringProperty title = new SimpleStringProperty();
  private final StringProperty domainLabel = new SimpleStringProperty();
  private final StringProperty rangeLabel = new SimpleStringProperty();
  private final ObjectProperty<NumberFormat> rangeAxisFormat = new SimpleObjectProperty<>();
  private final ObjectProperty<NumberFormat> domainAxisFormat = new SimpleObjectProperty<>();
  private final ListProperty<ValueMarker> domainAxisMarkers = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ObjectProperty<SimpleXYChart<PlotXYDataProvider>> chart = new SimpleObjectProperty<>(
      new SimpleXYChart<>());

  public @Nullable PlotCursorPosition getCursorPosition() {
    return cursorPosition.get();
  }

  public void setCursorPosition(@Nullable PlotCursorPosition cursorPosition) {
    this.cursorPosition.set(cursorPosition);
  }

  public ObjectProperty<@Nullable PlotCursorPosition> cursorPositionProperty() {
    return cursorPosition;
  }

  public ObservableMap<XYDataset, XYItemRenderer> getDatasetRenderers() {
    return datasetRenderers.get();
  }

  public void setDatasetRenderers(ObservableMap<XYDataset, XYItemRenderer> datasetRenderers) {
    this.datasetRenderers.set(datasetRenderers);
  }

  public MapProperty<XYDataset, XYItemRenderer> datasetRenderersProperty() {
    return datasetRenderers;
  }

  public ObservableList<XYAnnotation> getAnnotations() {
    return annotations.get();
  }

  public void setAnnotations(ObservableList<XYAnnotation> annotations) {
    this.annotations.set(annotations);
  }

  public ListProperty<XYAnnotation> annotationsProperty() {
    return annotations;
  }

  public String getTitle() {
    return title.get();
  }

  public void setTitle(String title) {
    this.title.set(title);
  }

  public StringProperty titleProperty() {
    return title;
  }

  public String getDomainLabel() {
    return domainLabel.get();
  }

  public void setDomainLabel(String domainLabel) {
    this.domainLabel.set(domainLabel);
  }

  public StringProperty domainLabelProperty() {
    return domainLabel;
  }

  public String getRangeLabel() {
    return rangeLabel.get();
  }

  public void setRangeLabel(String rangeLabel) {
    this.rangeLabel.set(rangeLabel);
  }

  public StringProperty rangeLabelProperty() {
    return rangeLabel;
  }

  public NumberFormat getRangeAxisFormat() {
    return rangeAxisFormat.get();
  }

  public void setRangeAxisFormat(NumberFormat rangeAxisFormat) {
    this.rangeAxisFormat.set(rangeAxisFormat);
  }

  public ObjectProperty<NumberFormat> rangeAxisFormatProperty() {
    return rangeAxisFormat;
  }

  public NumberFormat getDomainAxisFormat() {
    return domainAxisFormat.get();
  }

  public void setDomainAxisFormat(NumberFormat domainAxisFormat) {
    this.domainAxisFormat.set(domainAxisFormat);
  }

  public ObjectProperty<NumberFormat> domainAxisFormatProperty() {
    return domainAxisFormat;
  }

  public ObservableList<ValueMarker> getDomainAxisMarkers() {
    return domainAxisMarkers.get();
  }

  public void setDomainAxisMarkers(ObservableList<ValueMarker> domainAxisMarkers) {
    this.domainAxisMarkers.set(domainAxisMarkers);
  }

  public ListProperty<ValueMarker> domainAxisMarkersProperty() {
    return domainAxisMarkers;
  }

  /**
   * Package private, only the controller needs access.
   */
  SimpleXYChart<PlotXYDataProvider> getChart() {
    return chart.get();
  }

}
