package io.github.mzmine.modules.visualization.test_visualizer;
import static javafx.application.Application.launch;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.ThreadingModel;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;

public class test_visualizer extends Application {
  protected Graph graph;
  protected Viewer viewer;
  protected FxViewPanel view;

  public void start(Stage primaryStage) throws Exception {

    Graph graph = new MultiGraph("Test-Graph");
    try {
      graph.read("imdb.dgs");
    } catch(Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    viewer = new FxViewer(graph, ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
    viewer.enableAutoLayout();
    view = (FxViewPanel) viewer.addDefaultView(false);
    view.enableMouseOptions();
    StackPane graphpane = new StackPane(view);
    Scene scene=new Scene(graphpane,800,600);
    primaryStage.setTitle("Test_Visualizer");
    primaryStage.setScene(scene);
    primaryStage.show();
  }
  public static void main(String[] args)
  {
    System.setProperty("org.graphstream.ui", "javafx");
    launch(args);
  }
}
