package io.github.mzmine.gui.chartbasics.gui.javafx.template;

import io.github.mzmine.gui.chartbasics.gui.javafx.template.providers.PlotDatasetProvider;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javax.annotation.Nonnull;

/*public class SimpleXYLineChartWithDatasetView<T extends PlotDatasetProvider> extends SplitPane {

  private final SimpleXYLineChart<T> chart;
  private DatasetControlPaneController datasetPaneController;

  public SimpleXYLineChartWithDatasetView(@Nonnull SimpleXYLineChart<T> chart) {
    super();
    setDividerPositions(0.7);

    this.chart = chart;

    datasetPaneController = null;
    FXMLLoader loader = new FXMLLoader((getClass().getResource("DatasetControlPane.fxml")));

    AnchorPane controllerPane;
    try {
      controllerPane = loader.load();
      datasetPaneController = loader.getController();
    } catch (IOException e) {
      System.out.println("nope");
      return;
    }

    datasetPaneController.setChart(chart);
    chart.addDatasetsChangedListener(datasetPaneController::onDatasetChanged);
    getChildren().addAll(chart, controllerPane);

    setVisible(true);
  }

  public SimpleXYLineChart<T> getSimpleXYLineChart() {
    return chart;
  }

  public DatasetControlPaneController getDatasetPaneController() {
    return datasetPaneController;
  }
}*/
