package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterDialogWithFeaturePreview;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BaselineResolverSetupDialog extends ParameterDialogWithFeaturePreview {

  @Override
  protected @NotNull SimpleXYChart<PlotXYDataProvider> createChart() {
    return new SimpleXYChart<>("Preview", formats.unit("Retention time", "min"),
        formats.unit("Intensity", "a.u."));
  }

  public BaselineResolverSetupDialog(boolean valueCheckRequired, ParameterSet parameters,
      Region message) {
    super(valueCheckRequired, parameters, message);
  }

  @Override
  protected void updateChart(@Nullable Feature selectedFeature,
      @NotNull SimpleXYChart<? extends PlotXYDataProvider> chart) {
    if (selectedFeature == null) {
      chart.removeAllDatasets();
      return;
    }

    final BaselineCorrectors enumValue = parameterSet.getParameter(
        BaselineCorrectionParameters.correctionAlgorithm).getValue();
    final BaselineCorrector baselineCorrector = ((BaselineCorrector) enumValue.getModuleInstance()).newInstance(
        (BaselineCorrectionParameters) parameterSet, null);

    IonTimeSeries<? extends Scan> corrected = baselineCorrector.correctBaseline(
        selectedFeature.getFeatureData());

    chart.applyWithNotifyChanges(false, () -> {
      chart.removeAllDatasets();
      chart.addDataset(new ColoredXYDataset(
          new IonTimeSeriesToXYProvider(corrected, selectedFeature.toString() + " corrected",
              selectedFeature.getRawDataFile().getColor())), new ColoredAreaShapeRenderer());
      chart.addDataset(new ColoredXYDataset(
              new IonTimeSeriesToXYProvider(selectedFeature.getFeatureData(),
                  selectedFeature.toString(), selectedFeature.getRawDataFile().getColor())),
          new ColoredXYLineRenderer());
    });
  }
}
