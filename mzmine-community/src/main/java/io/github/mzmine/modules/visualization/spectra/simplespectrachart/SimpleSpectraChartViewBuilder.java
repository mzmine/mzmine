package io.github.mzmine.modules.visualization.spectra.simplespectrachart;

import io.github.mzmine.gui.chartbasics.simplechart.SimpleXYChart;
import io.github.mzmine.gui.chartbasics.simplechart.providers.PlotXYDataProvider;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import javafx.collections.MapChangeListener;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * Builds the {@link SimpleSpectraChartController} view. Responsible for wiring
 * the {@link SimpleXYChart} to the model's observable state.
 */
public class SimpleSpectraChartViewBuilder extends FxViewBuilder<SimpleSpectraChartModel> {

  protected SimpleSpectraChartViewBuilder(@NotNull SimpleSpectraChartModel model) {
    super(model);
  }

  @Override
  public @NotNull Region build() {
    final BorderPane pane = new BorderPane();
    final SimpleXYChart<PlotXYDataProvider> chart = model.getChart();

    chart.setMinHeight(100);
    chart.setMinWidth(100);
    chart.setStickyZeroRangeAxis(true); // spectra start at 0 intensity
    pane.setCenter(chart);

    bindDatasets(chart);
    bindAxesAndTitle(chart);

    return pane;
  }

  private void bindDatasets(@NotNull SimpleXYChart<PlotXYDataProvider> chart) {
    model.datasetRenderersProperty()
        .addListener((MapChangeListener<XYDataset, XYItemRenderer>) change -> {
          if (change.wasAdded()) {
            chart.addDataset(change.getKey(), change.getValueAdded());
          }
          if (change.wasRemoved()) {
            if (change.getMap().isEmpty()) {
              chart.removeAllDatasets();
              return;
            }
            final XYDataset removedKey = change.getKey();
            chart.getAllDatasets().entrySet().stream() //
                .filter(e -> e.getValue() == removedKey) //
                .findFirst() //
                .ifPresent(e -> chart.removeDataSet(e.getKey()));
          }
        });
  }

  private void bindAxesAndTitle(@NotNull SimpleXYChart<PlotXYDataProvider> chart) {
    model.titleProperty().subscribe(title -> {
      chart.getChart().setTitle(title == null ? "" : title);
      ConfigService.getConfiguration().getDefaultChartTheme().applyToTitles(chart.getChart());
    });
    model.domainLabelProperty()
        .subscribe(label -> chart.getXYPlot().getDomainAxis().setLabel(label));
    model.rangeLabelProperty()
        .subscribe(label -> chart.getXYPlot().getRangeAxis().setLabel(label));
    model.domainAxisFormatProperty().subscribe(format -> {
      if (format != null) {
        ((NumberAxis) chart.getXYPlot().getDomainAxis()).setNumberFormatOverride(format);
      }
    });
    model.rangeAxisFormatProperty().subscribe(format -> {
      if (format != null) {
        ((NumberAxis) chart.getXYPlot().getRangeAxis()).setNumberFormatOverride(format);
      }
    });
  }
}
