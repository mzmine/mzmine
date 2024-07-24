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

import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.features.types.otherdectectors.OtherFeatureDataType;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherFeatureImpl;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.BasicStroke;
import java.util.List;
import javafx.beans.binding.Bindings;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.plot.ValueMarker;

public class IntegrationPlotController extends FxController<IntegrationPlotModel> {

  protected final SimpleColorPalette palette = ConfigService.getDefaultColorPalette();
  private IntegrationPlotViewBuilder builder = new IntegrationPlotViewBuilder(model,
      this::onSetLeftPressed, this::onSetRightPressed, this::onFinishPressed, this::onAbortPressed,
      this::onEditPressed);

  protected IntegrationPlotController() {
    super(new IntegrationPlotModel());

    model.currentIntegrationValidProperty().bind(Bindings.createBooleanBinding(
        () -> model.getCurrentStartTime() != null && model.getCurrentEndTime() != null
            && model.getCurrentStartTime() < model.getCurrentEndTime(),
        model.currentStartTimeProperty(), model.currentEndTimeProperty()));

    model.currentStartMarkerProperty().bind(Bindings.createObjectBinding(
        () -> model.getCurrentStartTime() != null ? new ValueMarker(model.getCurrentStartTime(),
            ConfigService.getDefaultColorPalette().getPositiveColorAWT(), new BasicStroke(2f))
            : null, model.currentStartTimeProperty()));
    model.currentEndMarkerProperty().bind(Bindings.createObjectBinding(
        () -> model.getCurrentEndTime() != null ? new ValueMarker(model.getCurrentEndTime(),
            ConfigService.getDefaultColorPalette().getNegativeColorAWT(), new BasicStroke(2f))
            : null, model.currentEndTimeProperty()));
  }

  @Override
  protected @NotNull FxViewBuilder<IntegrationPlotModel> getViewBuilder() {
    return builder;
  }

  void onSetLeftPressed() {
    model.setNextBoundary(Boundary.LEFT);
    model.setIntegrating(true);
  }

  void onSetRightPressed() {
    model.setNextBoundary(Boundary.RIGHT);
    model.setIntegrating(true);
  }

  void onFinishPressed() {
    assert model.isCurrentIntegrationValid();

    final Double start = model.getCurrentStartTime();
    final Double end = model.getCurrentEndTime();

    if (start != null && end != null) {
      final OtherTimeSeries currentTimeSeries = model.getCurrentTimeSeries();

      final IntensityTimeSeries integrated = currentTimeSeries.subSeries(
          currentTimeSeries.getOtherDataFile().getCorrespondingRawDataFile().getMemoryMapStorage(),
          start.floatValue(), end.floatValue());
      final OtherFeature feature = new OtherFeatureImpl();
      feature.set(OtherFeatureDataType.class, (OtherTimeSeries) integrated);

      model.getOtherFeatures().add(feature);
      model.setSelectedFeature(feature);

    }
    clearIntegration();
  }

  void onAbortPressed() {
    onSetLeftPressed();
    clearIntegration();
  }

  private void clearIntegration() {
    model.setIntegrating(false);
    model.setCurrentStartTime(null);
    model.setCurrentEndTime(null);
//    model.setCurrentStartMarker(null);
//    model.setCurrentEndMarker(null);
  }

  void onEditPressed() {
    final OtherFeature feature = model.getSelectedFeature();
    assert feature != null;
    model.getOtherFeatures().remove(feature);

    final OtherTimeSeries timeSeries = feature.getFeatureData();
    model.setCurrentStartTime((double) timeSeries.getRetentionTime(0));
    model.setCurrentEndTime(
        (double) timeSeries.getRetentionTime(timeSeries.getNumberOfValues()) - 1);
    model.setNextBoundary(Boundary.LEFT);
  }

  public OtherTimeSeries getOtherTimeSeries() {
    return model.getCurrentTimeSeries();
  }

  public void setOtherTimeSeries(OtherTimeSeries otherTimeSeries) {
    model.otherFeaturesProperty().clear();
    onAbortPressed();
    model.setCurrentTimeSeries(otherTimeSeries);
  }

  /**
   * A copy of the current features.
   */
  public List<OtherFeature> getFeatures() {
    return List.copyOf(model.getOtherFeatures());
  }

  public void setFeatures(List<OtherFeature> otherFeatures) {
    if (otherFeatures == null || otherFeatures.isEmpty()) {
      model.otherFeaturesProperty().clear();
      return;
    }
    final boolean sameSeries = otherFeatures.stream()
        .allMatch(f -> f.getFeatureData().getTimeSeriesData() == model.getCurrentTimeSeries());
    if (!sameSeries) {
      throw new IllegalArgumentException("Set features to not match the current time series.");
    }

    model.setOtherFeatures(otherFeatures);
  }
}
