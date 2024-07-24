/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.otherdetectors.integrationplot;

import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.modules.visualization.otherdetectors.chromatogramplot.ChromatogramPlotController;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.ValueMarker;

public class IntegrationPlotModel {

  private BooleanProperty currentIntegrationValid = new SimpleBooleanProperty(false);
  private ObjectProperty<ChromatogramPlotController> chromatogramPlot = new SimpleObjectProperty<>(
      new ChromatogramPlotController());
  private ListProperty<OtherFeature> otherFeatures = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private ObjectProperty<@Nullable OtherFeature> selectedFeature = new SimpleObjectProperty<>();
  private ObjectProperty<@Nullable OtherTimeSeries> currentTimeSeries = new SimpleObjectProperty<>();
  private ObjectProperty<@Nullable Double> currentStartTime = new SimpleObjectProperty<>();
  private ObjectProperty<@Nullable Double> currentEndTime = new SimpleObjectProperty<>();
  private ObjectProperty<@NotNull Boundary> nextBoundary = new SimpleObjectProperty<>(
      Boundary.LEFT);
  private ObjectProperty<@Nullable ValueMarker> currentStartMarker = new SimpleObjectProperty<>();
  private ObjectProperty<@Nullable ValueMarker> currentEndMarker = new SimpleObjectProperty<>();
  private BooleanProperty isIntegrating = new SimpleBooleanProperty(false);

  public ChromatogramPlotController getChromatogramPlot() {
    return chromatogramPlot.get();
  }

  public void setChromatogramPlot(ChromatogramPlotController chromatogramPlot) {
    this.chromatogramPlot.set(chromatogramPlot);
  }

  public ObjectProperty<ChromatogramPlotController> chromatogramPlotProperty() {
    return chromatogramPlot;
  }

  public ObservableList<OtherFeature> getOtherFeatures() {
    return otherFeatures.get();
  }

  public void setOtherFeatures(List<OtherFeature> otherFeatures) {
    this.otherFeatures.setAll(otherFeatures);
  }

  public ListProperty<OtherFeature> otherFeaturesProperty() {
    return otherFeatures;
  }

  public @Nullable OtherTimeSeries getCurrentTimeSeries() {
    return currentTimeSeries.get();
  }

  public void setCurrentTimeSeries(@Nullable OtherTimeSeries currentTimeSeries) {
    this.currentTimeSeries.set(currentTimeSeries);
  }

  public ObjectProperty<@Nullable OtherTimeSeries> currentTimeSeriesProperty() {
    return currentTimeSeries;
  }

  public @Nullable Double getCurrentStartTime() {
    return currentStartTime.get();
  }

  public void setCurrentStartTime(@Nullable Double currentStartTime) {
    this.currentStartTime.set(currentStartTime);
  }

  public ObjectProperty<@Nullable Double> currentStartTimeProperty() {
    return currentStartTime;
  }

  public @Nullable Double getCurrentEndTime() {
    return currentEndTime.get();
  }

  public void setCurrentEndTime(@Nullable Double currentEndTime) {
    this.currentEndTime.set(currentEndTime);
  }

  public ObjectProperty<@Nullable Double> currentEndTimeProperty() {
    return currentEndTime;
  }

  public @NotNull Boundary getNextBoundary() {
    return nextBoundary.get();
  }

  public void setNextBoundary(@NotNull Boundary nextBoundary) {
    this.nextBoundary.set(nextBoundary);
  }

  public ObjectProperty<@NotNull Boundary> nextBoundaryProperty() {
    return nextBoundary;
  }

  public @Nullable OtherFeature getSelectedFeature() {
    return selectedFeature.get();
  }

  public void setSelectedFeature(@Nullable OtherFeature selectedFeature) {
    this.selectedFeature.set(selectedFeature);
  }

  public ObjectProperty<@Nullable OtherFeature> selectedFeatureProperty() {
    return selectedFeature;
  }

  public boolean isCurrentIntegrationValid() {
    return currentIntegrationValid.get();
  }

  public void setCurrentIntegrationValid(boolean currentIntegrationValid) {
    this.currentIntegrationValid.set(currentIntegrationValid);
  }

  public BooleanProperty currentIntegrationValidProperty() {
    return currentIntegrationValid;
  }

  public @Nullable ValueMarker getCurrentStartMarker() {
    return currentStartMarker.get();
  }

  public void setCurrentStartMarker(@Nullable ValueMarker currentStartMarker) {
    this.currentStartMarker.set(currentStartMarker);
  }

  public ObjectProperty<@Nullable ValueMarker> currentStartMarkerProperty() {
    return currentStartMarker;
  }

  public @Nullable ValueMarker getCurrentEndMarker() {
    return currentEndMarker.get();
  }

  public void setCurrentEndMarker(@Nullable ValueMarker currentEndMarker) {
    this.currentEndMarker.set(currentEndMarker);
  }

  public ObjectProperty<@Nullable ValueMarker> currentEndMarkerProperty() {
    return currentEndMarker;
  }

  public boolean isIntegrating() {
    return isIntegrating.get();
  }

  public void setIntegrating(boolean isIntegrating) {
    this.isIntegrating.set(isIntegrating);
  }

  public BooleanProperty isIntegratingProperty() {
    return isIntegrating;
  }
}
