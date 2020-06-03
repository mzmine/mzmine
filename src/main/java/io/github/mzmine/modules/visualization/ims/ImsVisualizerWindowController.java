package io.github.mzmine.modules.visualization.ims;

import io.github.mzmine.gui.chartbasics.gui.javafx.EChartViewer;
import io.github.mzmine.parameters.Parameter;
import javafx.event.*;
import javafx.scene.input.*;
import javafx.fxml.FXML;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.fxml.Initializable;
import org.jfree.chart.JFreeChart;

import java.net.URL;
import java.util.ResourceBundle;

public class ImsVisualizerWindowController {

    @FXML
    private BorderPane plotPane;

    public void initialize(Parameter parameter)
    {


    }


    BorderPane getPlotPane(){
        return  plotPane;
    }

    private JFreeChart getChart() {
        if (plotPane.getChildren().get(0) instanceof EChartViewer) {
            EChartViewer viewer = (EChartViewer) plotPane.getChildren().get(0);
            return viewer.getChart();
        }
        return null;
    }


}
