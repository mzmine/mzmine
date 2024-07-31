package io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.DatasetAndRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IonTimeSeriesToXYProvider;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredAreaShapeRenderer;
import io.github.mzmine.gui.chartbasics.simplechart.renderers.ColoredXYLineRenderer;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterDialogWithFeaturePreview;
import java.util.List;
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
  protected void updateChart(@NotNull List<DatasetAndRenderer> datasets,
      @NotNull SimpleXYChart<? extends PlotXYDataProvider> chart) {
    chart.applyWithNotifyChanges(false, () -> {
      chart.removeAllDatasets();
      datasets.forEach(dsr -> chart.addDataset(dsr.dataset(), dsr.renderer()));
    });
  }

  @Override
  protected @NotNull List<@NotNull DatasetAndRenderer> calculateNewDatasets(
      @Nullable Feature feature) {

    if (feature == null) {
      return List.of();
    }

    final BaselineCorrectors enumValue = parameterSet.getParameter(
        BaselineCorrectionParameters.correctionAlgorithm).getValue();
    final BaselineCorrector baselineCorrector = ((BaselineCorrector) enumValue.getModuleInstance()).newInstance(
        (BaselineCorrectionParameters) parameterSet, null);

    IonTimeSeries<? extends Scan> corrected = baselineCorrector.correctBaseline(
        feature.getFeatureData());

    return List.of(new DatasetAndRenderer(new ColoredXYDataset(
            new IonTimeSeriesToXYProvider(corrected, feature.toString() + " corrected",
                feature.getRawDataFile().getColor())), new ColoredAreaShapeRenderer()),

        new DatasetAndRenderer(new ColoredXYDataset(
            new IonTimeSeriesToXYProvider(feature.getFeatureData(), feature.toString(),
                feature.getRawDataFile().getColor())), new ColoredXYLineRenderer()));

  }
}
