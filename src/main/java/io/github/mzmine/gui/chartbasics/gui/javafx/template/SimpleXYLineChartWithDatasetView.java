package io.github.mzmine.gui.chartbasics.gui.javafx.template;

import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.PlotDatasetProvider;
import javafx.scene.control.SplitPane;
import javax.annotation.Nonnull;

public class SimpleXYLineChartWithDatasetView<T extends PlotDatasetProvider> extends SplitPane {

  private final SimpleXYLineChart<T> chart;

  private final DatasetControlPane<T> datasetPane;

  public SimpleXYLineChartWithDatasetView(@Nonnull SimpleXYLineChart<T> chart) {
    super();
    this.chart = chart;
    datasetPane = new DatasetControlPane<>(chart);
    chart.addDatasetsChangedListener(datasetPane::onDatasetChanged);
    getChildren().addAll(chart, datasetPane);
    setDividerPositions(0.7);
    setVisible(true);
  }

  public SimpleXYLineChart<T> getSimpleXYLineChart() {
    return chart;
  }

  public DatasetControlPane getDatasetPane() {
    return datasetPane;
  }
}
