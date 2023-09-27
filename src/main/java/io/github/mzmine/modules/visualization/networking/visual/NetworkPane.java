/*
 * Copyright (c) 2004-2023 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.networking.visual;

import com.google.common.collect.Range;
import com.google.common.io.Resources;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Element;
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
import org.graphstream.ui.fx_viewer.util.FxShortcutManager;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicEdge;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.javafx.util.FxFileSinkImages;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.Viewer.ThreadingModel;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.camera.Camera;
import org.graphstream.ui.view.util.InteractiveElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NetworkPane extends BorderPane {

  public static final String DEFAULT_STYLE_FILE = "themes/graph_network_style.css";

  public static final String EXPORT_STYLE_SHEET =
      "edge {fill-color: rgb(25,85,25); stroke-color: rgb(50,100,50); stroke-width: 2px;}  node {text-size: 16; fill-color: black; size: 16px; stroke-mode: plain; stroke-color: rgb(50,100,50); stroke-width: 2px;} "
      + "node.important{fill-color: red;} node.big{size: 20px;} node.MOL{fill-color: cyan; size: 20px;}  node.NEUTRAL{fill-color: violet; }"
      + "edge.medium{fill-color: rgb(50,100,200); stroke-color: rgb(50,100,200); stroke-width: 5px;}";
  private static final Logger LOG = Logger.getLogger(NetworkPane.class.getName());
  // selected node
  protected final ObservableList<Node> selectedNodes = FXCollections.observableArrayList();
  protected final ObservableList<Edge> selectedEdges = FXCollections.observableArrayList();
  protected final FilterableGraph graph;
  private final HBox pnSettings;
  private final Label lbTitle;
  private final FileChooser saveDialog;

  private final ContextMenu rightClickMenu;
  private final MenuItem exportGraphItem;
  private final ExtensionFilter graphmlExt = new ExtensionFilter(
      "Export network to graphml (*.graphml)", "*.graphml");
  private final ExtensionFilter pngExt = new ExtensionFilter("PNG pixel graphics file (*.png)",
      "*.png");
  private final ExtensionFilter svgExt = new ExtensionFilter("SVG vector graphics file (*.svg)",
      "*.svg");
  // needs more resources
  private final boolean enableMouseOnEdges = false;
  protected String styleSheet;
  // save screenshot
  protected FileSinkGraphML saveGraphML = new FileSinkGraphML();
  protected FileSinkSVG saveSVG = new FileSinkSVG();
  protected FileSinkImages savePNG = FileSinkImages.createDefault();
  // visual
  protected Viewer viewer;
  protected FxViewPanel view;
  protected GraphicNode mouseClickedNode;
  protected double viewPercent = 1;
  protected boolean showNodeLabels = false;
  protected boolean showEdgeLabels = false;

  private double lastDragX = -1000;
  private double lastDragY = -1000;

  /**
   * Create the panel.
   */
  public NetworkPane(String title, boolean showTitle, MultiGraph fullGraph) {
    this(title, "", showTitle, fullGraph);
  }

  public NetworkPane(String title, String styleSheet2, boolean showTitle, MultiGraph fullGraph) {
    System.setProperty("org.graphstream.ui", "javafx");
//    System.setProperty("gs.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
//    System.setProperty("org.graphstream.ui.renderer",
//        "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

    // when selection changes, add / remove attributes
    selectedNodes.addListener(this::handleSelectedNodesChanged);
    selectedEdges.addListener(this::handleSelectedEdgesChanged);

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

    // add settings
    pnSettings = new HBox();
    pnSettings.setVisible(false);
    this.setBottom(pnSettings);
    // add title
    lbTitle = new Label(title);
    HBox pn = new HBox(lbTitle);
    this.setTop(pn);
    setShowTitle(showTitle);
    graph = new FilterableGraph(title, fullGraph, false);
    setStyleSheet(this.styleSheet);

    viewer = new FxViewer(graph, ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
    viewer.disableAutoLayout();
    graph.setAttribute("Layout.frozen"); //Block the layout algorithm
    view = (FxViewPanel) viewer.addDefaultView(false);
    // wrap in stackpane to make sure coordinates work properly.
    // Might be confused by other components in the same pane
    StackPane graphpane = new StackPane(view);
//    graphpane.setStyle("-fx-border-color: black");
    this.setCenter(graphpane);

    // enable selection of edges by mouse
    if (enableMouseOnEdges) {
      view.enableMouseOptions();
    }

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

    viewer.getDefaultView().setShortcutManager(new FxShortcutManager() {
      final EventHandler<KeyEvent> keyPressed = event -> {
        Camera camera = view.getCamera();

        if (event.getCode() == KeyCode.PAGE_UP) {
          camera.setViewPercent(Math.max(0.0001f, camera.getViewPercent() * 0.9f));
        } else if (event.getCode() == KeyCode.PAGE_DOWN) {
          camera.setViewPercent(camera.getViewPercent() * 1.1f);
        } else if (event.getCode() == KeyCode.LEFT) {
          if (event.isAltDown()) {
            double r = camera.getViewRotation();
            camera.setViewRotation(r - 5);
          } else {
            double delta = 0;

            if (event.isShiftDown()) {
              delta = camera.getGraphDimension() * 0.1f;
            } else {
              delta = camera.getGraphDimension() * 0.01f;
            }

            delta *= camera.getViewPercent();

            Point3 p = camera.getViewCenter();
            camera.setViewCenter(p.x - delta, p.y, 0);
          }
        } else if (event.getCode() == KeyCode.RIGHT) {
          if (event.isAltDown()) {
            double r = camera.getViewRotation();
            camera.setViewRotation(r + 5);
          } else {
            double delta = 0;

            if (event.isShiftDown()) {
              delta = camera.getGraphDimension() * 0.1f;
            } else {
              delta = camera.getGraphDimension() * 0.01f;
            }

            delta *= camera.getViewPercent();

            Point3 p = camera.getViewCenter();
            camera.setViewCenter(p.x + delta, p.y, 0);
          }
        } else if (event.getCode() == KeyCode.UP) {
          double delta = 0;

          if (event.isShiftDown()) {
            delta = camera.getGraphDimension() * 0.1f;
          } else {
            delta = camera.getGraphDimension() * 0.01f;
          }

          delta *= camera.getViewPercent();

          Point3 p = camera.getViewCenter();
          camera.setViewCenter(p.x, p.y + delta, 0);
        } else if (event.getCode() == KeyCode.DOWN) {
          double delta = 0;

          if (event.isShiftDown()) {
            delta = camera.getGraphDimension() * 0.1f;
          } else {
            delta = camera.getGraphDimension() * 0.01f;
          }

          delta *= camera.getViewPercent();

          Point3 p = camera.getViewCenter();
          camera.setViewCenter(p.x, p.y - delta, 0);
        }
      };

      public void init(GraphicGraph graph, View view) {
        this.view = view;
        view.addListener(KeyEvent.KEY_PRESSED, this.keyPressed);
      }

      public void release() {
        this.view.removeListener(KeyEvent.KEY_PRESSED, this.keyPressed);
        LOG.info("Key released");
      }
    });

    rightClickMenu = new ContextMenu();
    exportGraphItem = new MenuItem("Export Graph");
    rightClickMenu.getItems().add(exportGraphItem);

    view.setOnScroll(this::setZoomOnMouseScroll);

    view.setOnMouseDragged(e -> {
      if (e.getButton() == MouseButton.PRIMARY) {
        if (lastDragX != -1000) {
          Point3 c = view.getCamera().getViewCenter();
          Point3 end = view.getCamera().transformPxToGu(e.getX(), e.getY());
          Point3 start = view.getCamera().transformPxToGu(lastDragX, lastDragY);
          double x = end.x - start.x;
          double y = end.y - start.y;
          view.getCamera().setViewCenter(c.x - x, c.y - y, c.z);

          lastDragX = e.getX();
          lastDragY = e.getY();
        } else if (findNodeAt(e.getX(), e.getY()) == null) {
          // no node - drag activated
          lastDragX = e.getX();
          lastDragY = e.getY();
        }
      }
    });
    view.setOnMouseClicked(e -> {
      if (lastDragX != -1000) {
        // need to clear drag here
        // click is always triggered at the end of release
        lastDragX = -1000;
        lastDragY = -1000;
        return;
      }

      if (e.getButton() == MouseButton.PRIMARY) {
        mouseClickedNode = null;
        if (e.getClickCount() == 2) {
          resetZoom();
          e.consume();
        } else if (e.getClickCount() == 1) {
          mouseClickedNode = findNodeAt(e.getX(), e.getY()); //for retrieving mouse-clicked node
          GraphicEdge mouseClickedEdge = findEdgeAt(e.getX(), e.getY());

          onGraphClicked(e, mouseClickedNode, mouseClickedEdge,
              mapGraphicObjectToGraph(mouseClickedNode), mapGraphicObjectToGraph(mouseClickedEdge));
        }
      } else if (e.getButton() == MouseButton.SECONDARY) {
        if (rightClickMenu.isShowing()) {
          rightClickMenu.hide();
        } else {
          rightClickMenu.show(view, e.getScreenX(), e.getScreenY());
          exportGraphItem.setOnAction(event -> openSaveDialog());
          e.consume();
        }
      }
    });
  }

  @Nullable
  private GraphicNode findNodeAt(double x, double y) {
    return (GraphicNode) view.findGraphicElementAt((EnumSet.of(InteractiveElement.NODE)), x, y);
  }

  @Nullable
  private GraphicEdge findEdgeAt(double x, double y) {
    return NetworkMouseManager.findEdgeAt(view, view.getViewer().getGraphicGraph(), x, y);
  }

  /**
   * Graphics objects that were clicked. might need to be mapped to real objects
   *
   * @param e
   * @param goNode
   * @param goEdge
   */
  protected void onGraphClicked(final @NotNull MouseEvent e, final @Nullable GraphicNode goNode,
      final @Nullable GraphicEdge goEdge, final @Nullable Node node, final @Nullable Edge edge) {
    // convert to real edge
    if (node != null) {
      // shift - add to selection
      if (e.isShiftDown()) {
        toggleSelection(node);
      } else {
        setSelectedNode(node);
      }
    } else if (edge != null) {
      setSelectedEdge(edge);
    } else {
      // nothing clicked - keep selection
      setCenter(e.getX(), e.getY());
    }
  }

  protected Edge mapGraphicObjectToGraph(@Nullable Edge edge) {
    return edge == null ? null : graph.getFullGraph().getEdge(edge.getId());
  }

  protected Node mapGraphicObjectToGraph(@Nullable Node node) {
    return node == null ? null : graph.getFullGraph().getNode(node.getId());
  }

  protected GraphicElement mapElementToGraphicObject(@Nullable Element element) {
    return switch (element) {
      case Edge e -> (GraphicElement) getGraphicGraph().getEdge(e.getId());
      case Node n -> (GraphicElement) getGraphicGraph().getNode(n.getId());
      case null -> null;
      default -> throw new IllegalStateException("Unexpected value: " + element);
    };
  }

  protected GraphicEdge mapElementToGraphicObject(@Nullable Edge edge) {
    return edge == null ? null : (GraphicEdge) getGraphicGraph().getEdge(edge.getId());
  }

  protected GraphicNode mapElementToGraphicObject(@Nullable Node node) {
    return node == null ? null : (GraphicNode) getGraphicGraph().getNode(node.getId());
  }

  public GraphicGraph getGraphicGraph() {
    return viewer.getGraphicGraph();
  }

  /**
   * Changes are triggered on {@link #selectedNodes}
   *
   * @param change change event
   */
  protected void handleSelectedNodesChanged(final Change<? extends Node> change) {
    handleSelectedElementsChanged(change);
  }

  /**
   * Changes are triggered on  {@link #selectedEdges}
   *
   * @param change change event
   */
  protected void handleSelectedEdgesChanged(final Change<? extends Edge> change) {
    handleSelectedElementsChanged(change);
  }

  /**
   * Changes are triggered on {@link #selectedNodes} and {@link #selectedEdges}
   *
   * @param change change event
   */
  protected void handleSelectedElementsChanged(final Change<? extends Element> change) {
    while (change.next()) {
      change.getRemoved().stream().map(this::mapElementToGraphicObject).filter(Objects::nonNull)
          .forEach(e -> e.removeAttribute("ui.clicked"));
      change.getAddedSubList().stream().map(this::mapElementToGraphicObject)
          .filter(Objects::nonNull).forEach(e -> e.setAttribute("ui.clicked"));
    }
  }


  private void setZoomOnMouseScroll(ScrollEvent e) {
    if (e.getDeltaY() < 0) {
      double new_view_percent = view.getCamera().getViewPercent() / 0.9;
      view.getCamera().setViewPercent(new_view_percent);
    } else if (e.getDeltaY() > 0) {
      double new_view_percent = view.getCamera().getViewPercent() * 0.9;
      view.getCamera().setViewPercent(new_view_percent);
    }
  }

  public ObservableList<Node> getSelectedNodes() {
    return selectedNodes;
  }

  public ObservableList<Edge> getSelectedEdges() {
    return selectedEdges;
  }

  /**
   * Load default style from file
   *
   * @return the loaded style or an empty string on error
   */
  private String loadDefaultStyle() {
    try {
      var style = Resources.toString(Resources.getResource(DEFAULT_STYLE_FILE),
          StandardCharsets.UTF_8);
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
        if (saveDialog.getSelectedExtensionFilter() == pngExt || FileAndPathUtil.getExtension(f)
            .equalsIgnoreCase("png")) {
          savePNG = new FxFileSinkImages();
          savePNG.setResolution(2500, 2500);
          savePNG.setOutputType(OutputType.png);
          savePNG.setLayoutPolicy(LayoutPolicy.COMPUTED_FULLY_AT_NEW_IMAGE);
          savePNG.setStyleSheet(styleSheet);
          savePNG.setQuality(Quality.HIGH);
          f = FileAndPathUtil.getRealFilePath(f, "png");
          saveToFile(savePNG, f);
        } else if (saveDialog.getSelectedExtensionFilter().equals(svgExt)
                   || FileAndPathUtil.getExtension(f).equalsIgnoreCase("svg")) {
          f = FileAndPathUtil.getRealFilePath(f, "svg");
          saveToFile(saveSVG, f);
        } else if (saveDialog.getSelectedExtensionFilter().equals(graphmlExt)
                   || FileAndPathUtil.getExtension(f).equalsIgnoreCase("graphml")) {
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

  public void setStyleSheet(String styleSheet) {
    this.styleSheet = styleSheet;
    graph.setAttribute("ui.stylesheet", styleSheet);
    // was at 3 but slow?
    graph.setAttribute("ui.quality", 2);
    graph.setAttribute("ui.antialias");
  }

  public void clear() {
    graph.clear();
    setStyleSheet(styleSheet);
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

  public boolean isVisible(Element edge) {
    return edge.getAttribute("ui.hide") == null;
  }

  /**
   * Combines clear and add selection
   *
   * @param node target node
   */
  public void setSelectedNode(Node node) {
    if (node == null) {
      selectedNodes.clear();
      return;
    }
    selectedNodes.setAll(List.of(node));
  }

  public void setSelectedEdge(final Edge edge) {
    if (edge == null) {
      selectedEdges.clear();
      return;
    }
    selectedEdges.setAll(List.of(edge));
    selectedNodes.setAll(List.of(edge.getNode0(), edge.getNode1()));
  }


  public void toggleSelection(Element element) {
    if (element instanceof Node node) {
      if (selectedNodes.remove(node)) {
        node.removeAttribute("ui.clicked");
      } else {
        node.setAttribute("ui.clicked");
        selectedNodes.add(node);
      }
    }
    if (element instanceof Edge edge) {
      if (selectedEdges.remove(edge)) {
        edge.removeAttribute("ui.clicked");
      } else {
        edge.setAttribute("ui.clicked");
        selectedEdges.add(edge);
        selectedNodes.setAll(List.of(edge.getNode0(), edge.getNode1()));
      }
    }
  }

  public void setCenter(double x, double y) {
    Point3 c = view.getCamera().getViewCenter();
    Point3 p = view.getCamera().transformPxToGu(x, y);
    view.getCamera().setViewCenter(p.x, p.y, c.z);
  }

  public void resetZoom() {
    viewPercent = 1;
    view.getCamera().resetView();
  }

  public void zoomOnSelectedNodes() {
    zoomOnNodes(selectedNodes);
  }

  public void zoomOnNodes(List<Node> nodes) {
    List<GraphicNode> graphicNodes = nodes.stream()
        .map(n -> (GraphicNode) getGraphicGraph().getNode(n.getId())).filter(Objects::nonNull)
        .toList();
    Range<Double> rx = null;
    Range<Double> ry = null;
    for (final GraphicNode n : graphicNodes) {
      double x = n.getX();
      double y = n.getY();
      if (rx == null) {
        rx = Range.singleton(x);
        ry = Range.singleton(y);
      } else {
        rx = rx.span(Range.singleton(x));
        ry = ry.span(Range.singleton(y));
      }
    }
    if (rx == null) {
      return;
    }
    Camera camera = view.getCamera();

    double distX = RangeUtils.rangeCenter(rx);
    double distY = RangeUtils.rangeCenter(ry);
    camera.setViewCenter(distX, distY, 0);
//    GraphMetrics metrics = camera.getMetrics();
//    double diag = Math.sqrt(distX * distX + distY * distY);
//    double[] size = metrics.size.data;
//    double zoom = Math.max(distX / size[0], distY / size[1]) * 1.5;
//    camera.setViewPercent(zoom);
  }

  public Pane getPnSettings() {
    return pnSettings;
  }

  public Node getMouseClickedNode() {
    return mouseClickedNode;
  }

  /**
   * Get attribute or return defaultValue for null
   *
   * @param target       target element
   * @param attribute    the attribute string or null (will return default value)
   * @param defaultValue the default if the attribute or its value is null
   * @param <T>          the type of the target value
   * @return return the mapping for attribute or defaultValue if null
   */
  public <T> T getOrElse(@NotNull Element target, @Nullable String attribute,
      @Nullable T defaultValue) {
    return attribute == null ? defaultValue
        : (T) Objects.requireNonNullElse(target.getAttribute(attribute), defaultValue);
  }

  /**
   * Get attribute or return defaultValue for null
   *
   * @param target       target element
   * @param attribute    the attribute string or null (will return default value)
   * @param defaultValue the default if the attribute or its value is null
   * @return return the mapping for attribute or defaultValue if null
   */
  public String getOrElseString(@NotNull Element target, @Nullable String attribute,
      @Nullable String defaultValue) {
    return attribute == null ? defaultValue
        : Objects.toString(target.getAttribute(attribute), defaultValue);
  }

  public FilterableGraph getGraph() {
    return graph;
  }

  public void showFullGraph() {
    graph.showFullNetwork();
  }
}
