/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.modules.visualization.networking.visual;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javax.swing.JPanel;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSink;
import org.graphstream.stream.file.FileSinkGraphML;
import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.stream.file.FileSinkImages.LayoutPolicy;
import org.graphstream.stream.file.FileSinkImages.OutputType;
import org.graphstream.stream.file.FileSinkImages.Quality;
import org.graphstream.stream.file.FileSinkSVG;
import org.graphstream.ui.fx_viewer.FxViewPanel;
import org.graphstream.ui.fx_viewer.FxViewer;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.javafx.util.FxFileSinkImages;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;

public class NetworkPane extends BorderPane {

  public static final String DEFAULT_STYLE_FILE = "themes/graph_network_style.css";
  public static final String STYLE_SHEET =
      "edge {text-visibility-mode: under-zoom; text-visibility: 0.3; fill-color: rgb(100,160,100); stroke-color: rgb(50,100,50); stroke-width: 1px; text-alignment: along;} "
      + "edge.medium{fill-color: rgb(50,100,200); stroke-color: rgb(50,100,200); stroke-width: 2.5px;} "
      + "node {text-visibility-mode: under-zoom; text-visibility: 0.3; text-alignment: at-right; text-offset: 2; text-size: 12; fill-color: black; "
      + "size: 11px; stroke-mode: plain; stroke-color: rgb(50,100,50); stroke-width: 1px;} "
      + "node.important{fill-color: red;} node.big{size: 15px;} "
      + "node.MOL{text-visibility-mode: under-zoom; text-visibility: 0.99; fill-color: cyan; size: 15px;} "
      + "node.NEUTRAL{fill-color: violet;}";
  public static final String EXPORT_STYLE_SHEET =
      "edge {fill-color: rgb(25,85,25); stroke-color: rgb(50,100,50); stroke-width: 2px;}  node {text-size: 16; fill-color: black; size: 16px; stroke-mode: plain; stroke-color: rgb(50,100,50); stroke-width: 2px;} "
      + "node.important{fill-color: red;} node.big{size: 20px;} node.MOL{fill-color: cyan; size: 20px;}  node.NEUTRAL{fill-color: violet; }"
      + "edge.medium{fill-color: rgb(50,100,200); stroke-color: rgb(50,100,200); stroke-width: 5px;}";
  private static final Logger LOG = Logger.getLogger(NetworkPane.class.getName());
  private final HBox pnSettings;
  protected String styleSheet;
  // save screenshot
  protected FileSinkGraphML saveGraphML = new FileSinkGraphML();
  protected FileSinkSVG saveSVG = new FileSinkSVG();
  protected FileSinkImages savePNG = FileSinkImages.createDefault();
  // visual
  protected Graph graph;
  protected Viewer viewer;
  protected FxViewPanel view;
  protected double viewPercent = 1;
  protected boolean showNodeLabels = false;
  protected boolean showEdgeLabels = false;
  // selected node
  private List<Node> selectedNodes;
  private Label lbTitle;

  private FileChooser saveDialog;
  private ExtensionFilter graphmlExt = new ExtensionFilter("Export network to graphml (*.graphml)",
      "*.graphml");
  private ExtensionFilter pngExt = new ExtensionFilter("PNG pixel graphics file (*.png)", "*.png");
  private ExtensionFilter svgExt = new ExtensionFilter("SVG vector graphics file (*.svg)", "*.svg");


  /**
   * Create the panel.
   */
  public NetworkPane(String title, boolean showTitle) {
    this(title, "", showTitle);
  }

  /**
   * @wbp.parser.constructor
   */
  public NetworkPane(String title, String styleSheet2, boolean showTitle) {
    System.setProperty("org.graphstream.ui", "javafx");
//    System.setProperty("gs.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
//    System.setProperty("org.graphstream.ui.renderer",
//        "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

    saveDialog = new FileChooser();

    //Set extension filter for text files
    saveDialog.getExtensionFilters().add(pngExt);
    saveDialog.getExtensionFilters().add(svgExt);
    saveDialog.getExtensionFilters().add(graphmlExt);
    saveDialog.setSelectedExtensionFilter(graphmlExt);

    // load default from file
    if (styleSheet2 == null || styleSheet2.isEmpty()) {
      this.styleSheet = loadDefaultStyle();
    } else {
      this.styleSheet = styleSheet2;
    }

    // set default in this class
    if (styleSheet == null || styleSheet.isEmpty()) {
      this.styleSheet = STYLE_SHEET;
    }

    // add settings
    pnSettings = new HBox();
    pnSettings.setVisible(false);
    this.setBottom(pnSettings);
    // add title
    lbTitle = new Label(title);
    HBox pn = new HBox(lbTitle);
    this.setTop(pn);
    setShowTitle(showTitle);

    selectedNodes = new ArrayList<>();

    graph = new MultiGraph(title);
    setStyleSheet(this.styleSheet);
    graph.setAutoCreate(true);
    graph.setStrict(false);

    viewer = new FxViewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
    viewer.enableAutoLayout();

    view = (FxViewPanel) viewer.addDefaultView(false);
    this.setCenter(view);

    viewer.newViewerPipe().addViewerListener(new ViewerListener() {
      @Override
      public void viewClosed(String viewName) {
        LOG.info("view closed " + viewName);
      }

      @Override
      public void buttonPushed(String id) {
        LOG.info("button pushed " + id);
      }

      @Override
      public void buttonReleased(String id) {
        LOG.info("button released " + id);
      }

      @Override
      public void mouseOver(String id) {
        LOG.info("Mouse over " + id);
      }

      @Override
      public void mouseLeft(String id) {
        LOG.info("Mouse left " + id);
      }
    });
//    // listener
//    view.addMouseListener(new MouseAdapter() {
//      private Point last;
//
//      @Override
//      public void mouseClicked(MouseEvent e) {
//        if (e.getButton() == MouseEvent.BUTTON1) {
//          if (e.getClickCount() == 2) {
//            resetZoom();
//            e.consume();
//          } else if (e.getClickCount() == 1)
//            setCenter(e.getX(), e.getY());
//        } else if (e.getButton() == MouseEvent.BUTTON3) {
//          openSaveDialog();
//          e.consume();
//        }
//      }
//
//      @Override
//      public void mouseReleased(MouseEvent e) {
//        last = null;
//      }
//
//      @Override
//      public void mousePressed(MouseEvent e) {
//        if (last == null)
//          last = e.getPoint();
//      }
//
//      @Override
//      public void mouseDragged(MouseEvent e) {
//        if (last != null) {
//          // translate
//          translate(e.getX() - last.getX(), e.getY() - last.getY());
//        }
//        last = e.getPoint();
//      }
//    });
//    view.add
//    view.addMouseWheelListener(event -> zoom(event.getWheelRotation() < 0));
  }

  /**
   * Load default style from file
   *
   * @return
   */
  private String loadDefaultStyle() {
    try {
      File file = new File(DEFAULT_STYLE_FILE);
      String style =
          Files.readLines(file, Charsets.UTF_8).stream().collect(Collectors.joining(" "));
      LOG.info("Default style from file: " + style);
      return style;
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Cannot load graph_network_style.css resource", e);
    }
    return "";
  }

  public void openSaveDialog() {
    if (graph != null && graph.getNodeCount() > 0) {
      File f = saveDialog.showSaveDialog(null);
      if (f != null) {
        if (saveDialog.getSelectedExtensionFilter() == pngExt ||
            FileAndPathUtil.getExtension(f).equalsIgnoreCase("png")) {
          savePNG = new FxFileSinkImages();
          savePNG.setResolution(2500, 2500);
          savePNG.setOutputType(OutputType.png);
          savePNG.setLayoutPolicy(LayoutPolicy.COMPUTED_FULLY_AT_NEW_IMAGE);
          savePNG.setStyleSheet(EXPORT_STYLE_SHEET);
          savePNG.setQuality(Quality.HIGH);
          f = FileAndPathUtil.getRealFilePath(f, "png");
          saveToFile(savePNG, f);
        } else if (saveDialog.getSelectedExtensionFilter().equals(svgExt) ||
              FileAndPathUtil.getExtension(f).equalsIgnoreCase("svg")) {
            f = FileAndPathUtil.getRealFilePath(f, "svg");
            saveToFile(saveSVG, f);
          } else if (saveDialog.getSelectedExtensionFilter().equals(graphmlExt) ||
                FileAndPathUtil.getExtension(f).equalsIgnoreCase("graphml")) {
          f = FileAndPathUtil.getRealFilePath(f, "graphml");
          saveToFile(saveGraphML, f);
        }
      }
    }
  }

  public void saveToFile(FileSink sink, File f) {
    try {
      if (graph != null && graph.getNodeCount() > 0) {
        sink.writeAll(graph, f.getAbsolutePath());
      }
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "File of network not saved", e);
    }
  }

  public void setShowTitle(boolean showTitle) {
    lbTitle.setVisible(showTitle);
  }

  public void setTitle(String title) {
    lbTitle.setText(title);
  }

  public void showNodeLabels(boolean show) {
    this.showNodeLabels = show;
    for (Node node : graph) {
      if (show) {
        Object label = node.getAttribute("LABEL");
        if (label == null) {
          label = node.getId();
        }
        node.setAttribute("ui.label", label);
      } else {
        node.removeAttribute("ui.label");
      }
    }
  }

  public void showEdgeLabels(boolean show) {
    this.showEdgeLabels = show;

    graph.edges().forEach(edge -> {
      if (show) {
        Object label = edge.getAttribute("LABEL");
        if (label == null) {
          label = edge.getId();
        }
        edge.setAttribute("ui.label", label);
      } else {
        edge.removeAttribute("ui.label");
      }
    });
  }

  /**
   * @param styleSheet
   */
  public void setStyleSheet(String styleSheet) {
    this.styleSheet = styleSheet;
    graph.setAttribute("ui.stylesheet", styleSheet);
    graph.setAttribute("ui.quality", 3);
    graph.setAttribute("ui.antialias");
  }

  public void clear() {
    graph.clear();
    setStyleSheet(styleSheet);
  }

  public Graph getGraph() {
    return graph;
  }

  public FxViewPanel getView() {
    return view;
  }

  public Viewer getViewer() {
    return viewer;
  }

  public void setVisible(Node node, boolean visible) {
    if (!visible) {
      node.setAttribute("ui.hide");
    } else {
      node.removeAttribute("ui.hide");
    }
  }

  public void setVisible(Edge edge, boolean visible) {
    if (!visible) {
      edge.setAttribute("ui.hide");
    } else {
      edge.removeAttribute("ui.hide");
    }
  }

  public boolean isVisible(Edge edge) {
    return edge.getAttribute("ui.hide") == null;
  }

  public boolean isVisible(Node node) {
    return node.getAttribute("ui.hide") == null;
  }

  /**
   * Combines clear and add selection
   *
   * @param node
   */
  public void setSelectedNode(Node node) {
    clearSelections();
    addSelection(node);
  }

  public void addSelection(Node node) {
    if (node != null) {
      node.setAttribute("ui.class", "big, important");
      selectedNodes.add(node);
    }
  }

  public void clearSelections() {
    for (Node n : selectedNodes) {
      n.removeAttribute("ui.class");
    }
    selectedNodes.clear();
  }

  public String addNewEdge(Node node1, Node node2, String edgeNameSuffix) {
    String edge = node1.getId() + node2.getId() + edgeNameSuffix;
    graph.addEdge(edge, node1, node2);
    return edge;
  }

  public String addNewEdge(String node1, String node2, String edgeNameSuffix) {
    String edge = node1 + node2 + edgeNameSuffix;
    graph.addEdge(edge, node1, node2);
    return edge;
  }

  public String addNewEdge(Node node1, Node node2, String edgeNameSuffix, Object edgeLabel) {
    String edge = node1.getId() + node2.getId() + edgeNameSuffix;
    graph.addEdge(edge, node1, node2);
    graph.getEdge(edge).setAttribute("ui.label", edgeLabel);
    graph.getEdge(edge).setAttribute("LABEL", edgeLabel);
    return edge;
  }

  public String addNewEdge(String node1, String node2, String edgeNameSuffix, Object edgeLabel) {
    String edge = node1 + node2 + edgeNameSuffix;
    graph.addEdge(edge, node1, node2);
    graph.getEdge(edge).setAttribute("ui.label", edgeLabel);
    graph.getEdge(edge).setAttribute("LABEL", edgeLabel);
    return edge;
  }

  public void zoom(boolean zoomOut) {
    viewPercent += viewPercent * 0.1 * (zoomOut ? -1 : 1);
    view.getCamera().setViewPercent(viewPercent);
  }

  public void translate(double dx, double dy) {
    Point3 c = view.getCamera().getViewCenter();
    Point3 p0 = view.getCamera().transformPxToGu(0, 0);
    Point3 p = view.getCamera().transformPxToGu(dx, dy);

    view.getCamera().setViewCenter(c.x + p.x - p0.x, c.y + p.y + p0.y, c.z);
  }

  public void setCenter(int x, int y) {
    Point3 c = view.getCamera().getViewCenter();
    Point3 p = view.getCamera().transformPxToGu(x, y);
    view.getCamera().setViewCenter(p.x, p.y, c.z);
  }

  public void resetZoom() {
    viewPercent = 1;
    view.getCamera().resetView();
  }

  public Pane getPnSettings() {
    return pnSettings;
  }

}
