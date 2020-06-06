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
    public FlowPane flowPane;

    @FXML
    public BorderPane plotePaneMI;

    @FXML
    public BorderPane plotePaneMMZ;
    @FXML
    public BorderPane plotePaneIRT;
    @FXML
    public BorderPane plotePane3;


    BorderPane getPlotPaneMI(){
        return  plotePaneMI;
    }
    BorderPane getPlotPaneMMZ(){
        return  plotePaneMMZ;
    }
    BorderPane getPlotePaneIRT(){ return  plotePaneIRT; }


}
