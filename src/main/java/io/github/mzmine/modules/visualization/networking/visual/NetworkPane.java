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
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.spectra.simplespectra.mirrorspectra.MirrorScanWindowFXML;
import io.github.mzmine.util.files.FileAndPathUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
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
import org.graphstream.ui.graphicGraph.GraphicGraph;
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

  public static final String DEFAULT_STYLE_FILE = "/themes/graph_network_style.css";

  public static final String STYLE_SHEET = """
      graph {
         fill-color: white;
       }
       
       edge {
         text-visibility-mode: under-zoom;
         text-visibility: 0.3;
         text-alignment: along;
         fill-mode: none;
         stroke-color: rgb(108, 108, 108);
         stroke-width: 1px;
         stroke-mode: plain;
       }
       
       edge.medium {
         stroke-color: rgb(108, 108, 108);
         stroke-width: 2.5px;
       }
       edge.IIN {
         stroke-color: rgb(227, 116, 30);
         stroke-width: 1.5px;
         stroke-mode: dashes;
       }
       edge.FEATURECORR {
         stroke-color: rgb(151, 124, 70);
         stroke-width: 1px;
       }
       edge.COSINE {
         stroke-color: rgb(30, 86, 227);
         size-mode: dyn-size;
       }
       edge.GNPS {
         stroke-color: rgb(77, 108, 187);
         size-mode: dyn-size;
       }
       
       edge.IINREL {
         stroke-color: rgb(31, 173, 152);
         stroke-mode: dots;
       }
       
       node {
         shape: circle;
         text-visibility-mode: under-zoom;
         text-visibility: 0.3;
         text-alignment: at-right;
         text-offset: 2;
         text-size: 12;
         fill-color: #636363;
         size-mode: dyn-size;
         size: 11px;
         stroke-mode: plain;
         stroke-color: #636363;
         stroke-width: 1px;
       }
       
       node:clicked {
         fill-color: #f8ec02;
       }
       
       /* node.setAttribute("ui.class", "big, important"); */
       /* node.removeAttribute("ui.class"); // go back to default */
       node.important {
         fill-color: red;
       }
       
       node.big {
         size: 15px;
       }
       
       node.MOL {
         text-visibility-mode: under-zoom;
         text-visibility: 0.99;
         fill-color: cyan;
         size: 15px;
       }
       
       node.NEUTRAL {
         fill-color: violet;
       }
       
       /* add gradient to node: node1.setAttribute("ui.color", 0); from 0 - 1 */
       node.GRADIENT {
         fill-mode: dyn-plain;
         fill-color: yellow, orange, #c10000;
       }
      """;
  //      "edge {text-visibility-mode: under-zoom; text-visibility: 0.3; fill-color: rgb(100,160,100); stroke-color: rgb(50,100,50); stroke-width: 1px; text-alignment: along;} "
//      + "edge.medium{fill-color: rgb(50,100,200); stroke-color: rgb(50,100,200); stroke-width: 2.5px;} "
//      + "node {text-visibility-mode: under-zoom; text-visibility: 0.3; text-alignment: at-right; text-offset: 2; text-size: 12; fill-color: black; "
//      + "size: 11px; stroke-mode: plain; stroke-color: rgb(50,100,50); stroke-width: 1px;} "
//      + "node.important{fill-color: red;} node.big{size: 15px;} "
//      + "node.MOL{text-visibility-mode: under-zoom; text-visibility: 0.99; fill-color: cyan; size: 15px;} "
//      + "node.NEUTRAL{fill-color: violet;}";
  public static final String EXPORT_STYLE_SHEET =
      "edge {fill-color: rgb(25,85,25); stroke-color: rgb(50,100,50); stroke-width: 2px;}  node {text-size: 16; fill-color: black; size: 16px; stroke-mode: plain; stroke-color: rgb(50,100,50); stroke-width: 2px;} "
          + "node.important{fill-color: red;} node.big{size: 20px;} node.MOL{fill-color: cyan; size: 20px;}  node.NEUTRAL{fill-color: violet; }"
          + "edge.medium{fill-color: rgb(50,100,200); stroke-color: rgb(50,100,200); stroke-width: 5px;}";
  private static final Logger LOG = Logger.getLogger(NetworkPane.class.getName());
  private final HBox pnSettings;
  // selected node
  private final List<Node> selectedNodes;
  protected FilteredGraph graph;
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
  protected Node mouseClickedNode;

  protected Edge mouseClickedEdge;
  protected double viewPercent = 1;
  protected boolean showNodeLabels = false;
  protected boolean showEdgeLabels = false;


  /**
   * Create the panel.
   */
  public NetworkPane(String title, boolean showTitle) {
    this(title, "", showTitle);
  }

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
    graph = new FilteredGraph(title);
    selectedNodes = new ArrayList<>();
    setStyleSheet(this.styleSheet);
    graph.setAutoCreate(true);
    graph.setStrict(false);

    viewer = new FxViewer(graph, ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
    viewer.enableAutoLayout();
    graph.setAttribute("Layout.frozen"); //Block the layout algorithm
    view = (FxViewPanel) viewer.addDefaultView(false);
    // wrap in stackpane to make sure coordinates work properly.
    // Might be confused by other components in the same pane
    StackPane graphpane = new StackPane(view);
    graphpane.setStyle("-fx-border-color: black");
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
      public void init(GraphicGraph graph, View view) {
        this.view = view;
        view.addListener(KeyEvent.KEY_PRESSED, this.keyPressed);
      }

      public void release() {
        this.view.removeListener(KeyEvent.KEY_PRESSED, this.keyPressed);
        LOG.info("Key released");
      }

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
    });

    rightClickMenu = new ContextMenu();
    exportGraphItem = new MenuItem("Export Graph");
    rightClickMenu.getItems().add(exportGraphItem);

    view.setOnScroll(this::setZoomOnMouseScroll);

    view.setOnMouseClicked(e -> {
      if (e.getButton() == MouseButton.PRIMARY) {
        if (e.getClickCount() == 2) {
          resetZoom();
          e.consume();
        } else if (e.getClickCount() == 1) {
          mouseClickedNode = (Node) view.findGraphicElementAt((EnumSet.of(InteractiveElement.NODE)),
              e.getX(), e.getY()); //for retrieving mouse-clicked node
          setCenter(e.getX(), e.getY());
          mouseClickedEdge = NetworkMouseManager.findEdgeAt(view,
              view.getViewer().getGraphicGraph(), e.getX(),
              e.getY()); //for retrieving mouse-clicked edge
          if (mouseClickedEdge != null) {
            showMSMSMirrorScanModule();
          }
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

  private void setZoomOnMouseScroll(ScrollEvent e) {
    if (e.getDeltaY() < 0) {
      double new_view_percent = view.getCamera().getViewPercent() + 0.05;
      view.getCamera().setViewPercent(new_view_percent);
    } else if (e.getDeltaY() > 0) {
      double current_view_percent = view.getCamera().getViewPercent();
      if (current_view_percent > 0.05) {
        view.getCamera().setViewPercent(current_view_percent - 0.05);
      }
    }
  }

  /**
   * Run the MSMS-MirrorScan module whenever user clicks on edges
   */

  public void showMSMSMirrorScanModule() {
    Node a = mouseClickedEdge.getNode0();
    Node b = mouseClickedEdge.getNode1();
    MirrorScanWindowFXML mirrorScanTab = new MirrorScanWindowFXML();
    FeatureListRow Row1 = (FeatureListRow) a.getAttribute("FeatureListOnNode");
    FeatureListRow Row2 = (FeatureListRow) b.getAttribute("FeatureListOnNode");
    mirrorScanTab.getController()
        .setScans(Row1.getMostIntenseFragmentScan(), Row2.getMostIntenseFragmentScan());
    mirrorScanTab.show();
  }

  /**
   * Load default style from file
   *
   * @return the loaded style or an empty string on error
   */
  private String loadDefaultStyle() {
    try {
      File file = new File(getClass().getResource(DEFAULT_STYLE_FILE).toExternalForm());
      String style = Files.readLines(file, Charsets.UTF_8).stream()
          .collect(Collectors.joining(" "));
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
          savePNG.setStyleSheet(EXPORT_STYLE_SHEET);
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

  public void setCenter(double x, double y) {
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
}