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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonMobilogramTimeSeries;
import io.github.mzmine.gui.chartbasics.chartgroups.ChartGroup;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IntensityTimeSeriesToXYProvider;
import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.visualization.dash_integration.FeatureIntegrationData;
import io.github.mzmine.modules.visualization.otherdetectors.integrationplot.FeatureIntegratedListener.EventType;
import java.awt.BasicStroke;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.StringProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfree.chart.plot.ValueMarker;

public class IntegrationPlotController extends FxController<IntegrationPlotModel> {

  private static final Logger logger = Logger.getLogger(IntegrationPlotController.class.getName());

  private IntegrationPlotViewBuilder builder = new IntegrationPlotViewBuilder(model,
      this::onSetLeftPressed, this::onSetRightPressed, this::onFinishPressed, this::onAbortPressed,
      this::onEditPressed);

  public IntegrationPlotController() {
    super(new IntegrationPlotModel());

    model.currentIntegrationValidProperty().bind(Bindings.createBooleanBinding(
        () -> model.getCurrentStartTime() != null && model.getCurrentEndTime() != null
            && model.getCurrentStartTime() < model.getCurrentEndTime(),
        model.currentStartTimeProperty(), model.currentEndTimeProperty()));

    model.currentStartTimeProperty().addListener((_, _, value) -> model.setCurrentStartMarker(
        value != null ? new ValueMarker(value,
            ConfigService.getDefaultColorPalette().getPositiveColorAWT(), new BasicStroke(2f))
            : null));
    model.currentEndTimeProperty().addListener((_, _, value) -> model.setCurrentEndMarker(
        value != null ? new ValueMarker(value,
            ConfigService.getDefaultColorPalette().getNegativeColorAWT(), new BasicStroke(2f))
            : null));

    model.additionalDataProvidersProperty()
        .subscribe(_ -> onTaskThread(new UpdateAdditionalDatasetsTask(model)));

    model.getChromatogramPlot().title().bindBidirectional(model.titleProperty());
  }

  @Override
  protected @NotNull FxViewBuilder<IntegrationPlotModel> getViewBuilder() {
    return builder;
  }

  void onSetLeftPressed() {
    model.setState(State.SETTING_LEFT);
  }

  void onSetRightPressed() {
    model.setState(State.SETTING_RIGHT);
  }

  void onFinishPressed() {
    integrateFeature(EventType.INTERNAL_CHANGE);
  }

  void integrateFeature(EventType eventType) {
    if (!model.isCurrentIntegrationValid()) {
      if (eventType == EventType.EXTERNAL_CHANGE) {
        clearIntegration();
      }
      return;
    }

    final Double start = model.getCurrentStartTime();
    final Double end = model.getCurrentEndTime();
    logger.finest("Finish feature pressed. %.2f-%.2f".formatted(start, end));
    if (start != null && end != null) {

      final IntensityTimeSeries currentTimeSeries = model.getCurrentTimeSeries();
      @Nullable IntensityTimeSeries integrated;
      if (currentTimeSeries instanceof IonMobilogramTimeSeries imts) {
        integrated = imts.subSeries(currentTimeSeries.getStorage(), start.floatValue(),
            end.floatValue(), model.getBinningMobilogramDataAccess());
      } else {
        integrated = currentTimeSeries.subSeries(currentTimeSeries.getStorage(), start.floatValue(),
            end.floatValue());
      }
      if (FeatureDataUtils.calculateArea(integrated) <= 0) {
        integrated = null;
      }

      final int maxIntegratedFeatures = model.getMaxIntegratedFeatures();
      if (model.getIntegratedFeatures().size() + 1 > maxIntegratedFeatures && integrated != null) {
        // if there are more than the allowed integrated features, remove the first one.
        final List<IntensityTimeSeries> list = new ArrayList<>(model.getIntegratedFeatures());
        list.removeFirst();
        list.add(integrated);
        model.setIntegratedFeatures(list);
      } else if (integrated != null) {
        model.addIntegratedFeature(integrated);
      }

      model.setSelectedFeature(integrated);
      final @Nullable IntensityTimeSeries finalIntegrated = integrated;
      model.getIntegrationListeners().forEach(l -> l.accept(eventType, finalIntegrated,
          Range.closed(start.floatValue(), end.floatValue())));
    }
    clearIntegration();
  }

  void onAbortPressed() {
    logger.finest("Abort integration pressed");
    clearIntegration();
  }

  private void clearIntegration() {
    logger.finest("Clearing integration");
    model.setState(State.NOT_INTEGRATING);
    model.setCurrentStartTime(null);
    model.setCurrentEndTime(null);
  }

  void onEditPressed() {
    logger.finest("Edit feature pressed");
    final IntensityTimeSeries feature = model.getSelectedFeature();
    assert feature != null;
    model.getIntegratedFeatures().remove(feature);

    model.setCurrentStartTime((double) feature.getRetentionTime(0));
    model.setCurrentEndTime((double) feature.getRetentionTime(feature.getNumberOfValues() - 1));
    model.setState(State.SETTING_LEFT);
  }

  public IntensityTimeSeries getOtherTimeSeries() {
    return model.getCurrentTimeSeries();
  }

  public void setOtherTimeSeries(IntensityTimeSeries otherTimeSeries) {
    onAbortPressed();
    model.integratedFeaturesProperty().clear();

    // do not clear chromPlot manually like this, all done via listeners!
//    model.getChromatogramPlot().clearPlot();
    model.setCurrentTimeSeries(otherTimeSeries);
  }

  /**
   * A copy of the current features.
   */
  @NotNull
  public List<IntensityTimeSeries> getIntegratedFeatures() {
    return List.copyOf(model.getIntegratedFeatures());
  }

  public void setIntegratedFeatures(List<? extends IntensityTimeSeries> integratedFeatures) {
    if (integratedFeatures == null || integratedFeatures.isEmpty()) {
      model.integratedFeaturesProperty().clear();
      return;
    }
    model.setIntegratedFeatures((List<IntensityTimeSeries>) integratedFeatures);
  }

  public ListProperty<IntensityTimeSeries> integratedFeaturesProperty() {
    return model.integratedFeaturesProperty();
  }

  public void setAdditionalFeatures(List<IntensityTimeSeriesToXYProvider> additionalFeatures) {
    model.setAdditionalDataProviders(additionalFeatures);
  }

  public void clear() {
    setOtherTimeSeries(null);
    setIntegratedFeatures(null);
  }

  public void setFeatureDataEntry(@Nullable FeatureIntegrationData featureIntegrationData) {
    clear();
    if (featureIntegrationData == null) {
      return;
    }
    setOtherTimeSeries(featureIntegrationData.chromatogram());
    setIntegratedFeatures(
        featureIntegrationData.feature() != null ? List.of(featureIntegrationData.feature())
            : null);
    setAdditionalFeatures(featureIntegrationData.additionalData());
  }

  public void addIntegrationListener(@NotNull FeatureIntegratedListener listener) {
    model.getIntegrationListeners().add(listener);
  }

  public void removeIntegrationListener(@NotNull FeatureIntegratedListener listener) {
    model.getIntegrationListeners().remove(listener);
  }

  public StringProperty titleProperty() {
    return model.titleProperty();
  }

  public String getTitle() {
    return model.getTitle();
  }

  public void setTitle(String title) {
    model.setTitle(title);
  }

  public void integrateExternally(@Nullable Range<Float> newIntegrationRange) {
    model.setCurrentStartTime(newIntegrationRange.lowerEndpoint().doubleValue());
    model.setCurrentEndTime(newIntegrationRange.upperEndpoint().doubleValue());
    integrateFeature(EventType.EXTERNAL_CHANGE);
  }

  public void setChartGroup(ChartGroup group) {
    model.getChromatogramPlot().setChartGroup(group);
  }

  public int getMaxIntegratedFeatures() {
    return model.getMaxIntegratedFeatures();
  }

  public void setMaxIntegratedFeatures(int max) {
    model.setMaxIntegratedFeatures(max);
  }

  public void setRangeAxisStickyZero(boolean rangeStickyZero) {
    model.getChromatogramPlot().setRangeAxisStickyZero(rangeStickyZero);
  }

  public void setTextLessButtons(boolean textLessButtons) {
    model.setUseTextlessButtons(textLessButtons);
  }

  public void setBinningMobilogramDataAccess(@Nullable BinningMobilogramDataAccess dataAccess) {
    model.setBinningMobilogramDataAccess(dataAccess);
  }
}
