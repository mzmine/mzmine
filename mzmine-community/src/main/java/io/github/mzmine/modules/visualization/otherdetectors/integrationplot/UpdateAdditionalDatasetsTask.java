package io.github.mzmine.modules.visualization.otherdetectors.integrationplot;

import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.otherdetectors.OtherTimeSeries;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.ColoredXYDataset;
import io.github.mzmine.gui.chartbasics.simplechart.datasets.RunOption;
import io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series.IntensityTimeSeriesToXYProvider;
import io.github.mzmine.javafx.mvci.FxUpdateTask;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class UpdateAdditionalDatasetsTask extends FxUpdateTask<IntegrationPlotModel> {

  private final List<ColoredXYDataset> newDatasets = new ArrayList<>();
  private long total = 0;
  private long processed = 0;

  protected UpdateAdditionalDatasetsTask(IntegrationPlotModel model) {
    super("Updating additional datasets.", model);
  }

  @Override
  protected void process() {
    final List<IntensityTimeSeries> newSeries = model.getAdditionalTimeSeries();
    total = newSeries.size();

    final Color fileColor = extractFileColor(newSeries);
    final SimpleColorPalette colors = ConfigService.getDefaultColorPalette();
    final int fileColorIndex = colors.indexOfAwt(fileColor);

    for (int i = 0; i < newSeries.size(); i++) {
      IntensityTimeSeries series = newSeries.get(i);
      final var provider = new IntensityTimeSeriesToXYProvider(series,
          colors.getAWT(fileColorIndex + i + 1));
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

  private static Color extractFileColor(List<IntensityTimeSeries> newSeries) {
    final SimpleColorPalette colors = ConfigService.getDefaultColorPalette();
    if (newSeries.isEmpty()) {
      return colors.getMainColorAWT();
    }

    final IntensityTimeSeries series = newSeries.getFirst();
    final Color fallbackColor = colors.getMainColorAWT();
    final Color fileColor = switch (series) {
      case OtherTimeSeries other ->
          other.getOtherDataFile().getCorrespondingRawDataFile().getColorAWT();
      case IonTimeSeries<?> ion -> ion.getSpectra().isEmpty() ? fallbackColor
          : ion.getSpectra().get(0).getDataFile().getColorAWT();
      default -> fallbackColor;
    };
    return fileColor;
  }

  @Override
  public boolean checkPreConditions() {
    // if both are empty, we don't need to do anything -> return false in that case
    return !(model.additionalTimeSeriesProperty().isEmpty()
        && model.additionalTimeSeriesDatasetsProperty().isEmpty());
  }
}
