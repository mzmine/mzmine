package io.github.mzmine.modules.visualization.networking.visual;

import io.github.mzmine.util.GraphUtils;
import javafx.scene.layout.StackPane;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.view.Viewer;

public class FilteredGraph {

  private MultiGraph filteredGraph;
  protected Viewer newViewer;
  protected FxViewPanel view;
   public FilteredGraph()
   {
     this.filteredGraph=GraphUtils.getCopyOfGraph(NetworkPane.getGraph());
   }

   public void showFilteredGraph()
   {
     NetworkPane.getView().setVisible(false);
     newViewer= new FxViewer(filteredGraph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
     newViewer.enableAutoLayout();
     filteredGraph.setAttribute("Layout.frozen"); //Block the layout algorithm
     view = (FxViewPanel) newViewer.addDefaultView(false);
     StackPane filteredGraphPane = new StackPane(view);
     NetworkPane.getGraphPane().getChildren().add(filteredGraphPane);
   }
}
