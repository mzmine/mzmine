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

package io.github.mzmine.modules.visualization.otherdetectors.integrationplot;

import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
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

  private final BooleanProperty currentIntegrationValid = new SimpleBooleanProperty(false);
  private final ObjectProperty<ChromatogramPlotController> chromatogramPlot = new SimpleObjectProperty<>(
      new ChromatogramPlotController());
  private final ListProperty<IntensityTimeSeries> integratedFeatures = new SimpleListProperty<>(
      FXCollections.observableArrayList());
  private final ObjectProperty<@Nullable IntensityTimeSeries> selectedFeature = new SimpleObjectProperty<>();
  private final ObjectProperty<@Nullable IntensityTimeSeries> currentTimeSeries = new SimpleObjectProperty<>();
  private final ObjectProperty<@Nullable Double> currentStartTime = new SimpleObjectProperty<>();
  private final ObjectProperty<@Nullable Double> currentEndTime = new SimpleObjectProperty<>();
  private final ObjectProperty<@NotNull State> state = new SimpleObjectProperty<>(
      State.NOT_INTEGRATING);
  private final ObjectProperty<@Nullable ValueMarker> currentStartMarker = new SimpleObjectProperty<>();
  private final ObjectProperty<@Nullable ValueMarker> currentEndMarker = new SimpleObjectProperty<>();

  public ChromatogramPlotController getChromatogramPlot() {
    return chromatogramPlot.get();
  }

  public void setChromatogramPlot(ChromatogramPlotController chromatogramPlot) {
    this.chromatogramPlot.set(chromatogramPlot);
  }

  public ObjectProperty<ChromatogramPlotController> chromatogramPlotProperty() {
    return chromatogramPlot;
  }

  public @Nullable IntensityTimeSeries getCurrentTimeSeries() {
    return currentTimeSeries.get();
  }

  public void setCurrentTimeSeries(@Nullable IntensityTimeSeries currentTimeSeries) {
    this.currentTimeSeries.set(currentTimeSeries);
  }

  public ObjectProperty<@Nullable IntensityTimeSeries> currentTimeSeriesProperty() {
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

  public ObservableList<IntensityTimeSeries> getIntegratedFeatures() {
    return integratedFeatures.get();
  }

  public ListProperty<IntensityTimeSeries> integratedFeaturesProperty() {
    return integratedFeatures;
  }

  public void setIntegratedFeatures(List<IntensityTimeSeries> integratedFeatures) {
    this.integratedFeatures.setAll(integratedFeatures);
  }

  public void addIntegratedFeature(IntensityTimeSeries integratedFeatures) {
    this.integratedFeatures.add(integratedFeatures);
  }

  public @Nullable IntensityTimeSeries getSelectedFeature() {
    return selectedFeature.get();
  }

  public void setSelectedFeature(@Nullable IntensityTimeSeries selectedFeature) {
    this.selectedFeature.set(selectedFeature);
  }

  public ObjectProperty<@Nullable IntensityTimeSeries> selectedFeatureProperty() {
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

  public @NotNull State getState() {
    return state.get();
  }

  public ObjectProperty<@NotNull State> stateProperty() {
    return state;
  }

  public void setState(@NotNull State state) {
    this.state.set(state);
  }
}
