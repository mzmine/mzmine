package io.github.mzmine.modules.visualization.otherdetectors.integrationplot;

import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IntensityTimeSeriesToXYProvider;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class UpdateAdditionalDatasetsTask extends FxUpdateTask<IntegrationPlotModel> {

  private final List<ColoredXYDataset> newDatasets = new ArrayList<>();
  private long total = 0;
  private long processed = 0;

  protected UpdateAdditionalDatasetsTask(@NotNull IntegrationPlotModel model) {
    super("Updating additional datasets.", model);
  }

  @Override
  protected void process() {
    final List<IntensityTimeSeriesToXYProvider> newSeries = model.getAdditionalDataProviders();
    total = newSeries.size();

    for (int i = 0; i < newSeries.size(); i++) {
      PlotXYDataProvider provider = newSeries.get(i);
      ColoredXYDataset ds = new ColoredXYDataset(provider, RunOption.THIS_THREAD);
      newDatasets.add(ds);
      processed++;
    }
  }

  @Override
  protected void updateGuiModel() {
    model.setAdditionalTimeSeriesDatasets(newDatasets);
  }

  @Override
  public String getTaskDescription() {
    return "Updating additional datasets.";
  }

  @Override
  public double getFinishedPercentage() {
    return total != 0 ? (double) processed / total : 0;
  }

  @Override
  public boolean checkPreConditions() {
    // if both are empty, we don't need to do anything -> return false in that case
    return !(model.additionalDataProvidersProperty().isEmpty()
        && model.additionalTimeSeriesDatasetsProperty().isEmpty());
  }
}
