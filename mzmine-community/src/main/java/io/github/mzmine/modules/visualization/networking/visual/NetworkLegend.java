/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.modules.visualization.networking.visual.enums.EdgeType;
import io.github.mzmine.modules.visualization.networking.visual.enums.NodeType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Collapsible legend strip that documents the node + edge styles used by the network visualizer.
 * Visual swatches are derived from {@code themes/graph_network_style.css} via
 * {@link NetworkStyleSheet}, so editing a color in the CSS file automatically updates the legend —
 * no Java code change needed.
 *
 * <p>Lives in the FXML below the controls accordion in
 * {@code FeatureNetworkPane.fxml}; can also be instantiated programmatically.
 */
public class NetworkLegend extends TitledPane {

  // Legend entries are derived from NodeType / EdgeType values at runtime. Analog and direct
  // edge variants are kept as SEPARATE entries even though they share a CSS class - the enum is
  // the source of truth for which variants exist, and the user asked for them to not be combined.
  private static final double SWATCH_W = 20;
  private static final double SWATCH_H = 12;

  // Optional filters: when non-null, only show types contained in these sets. Populated by the
  // network generator as it builds nodes/edges, so the legend can mirror what's actually in the
  // graph and ignore enum values that never appear. ObservableSet so we update reactively if the
  // graph is later extended.
  private @Nullable ObservableSet<NodeType> boundNodeTypes;
  private @Nullable ObservableSet<EdgeType> boundEdgeTypes;

  // Schedule rebuilds on the FX thread because the generator may populate the sets from any
  // thread; mutating the scene graph off-FX would throw.
  private final SetChangeListener<NodeType> nodeChangeListener = c -> FxThread.runLater(
      this::rebuildContent);
  private final SetChangeListener<EdgeType> edgeChangeListener = c -> FxThread.runLater(
      this::rebuildContent);

  public NetworkLegend() {
    setText("Legend");
    // expanded by default so users can immediately see what the colors mean; the title bar lets
    // them collapse it if they need the screen space
    setExpanded(true);
    setCollapsible(true);
    rebuildContent();
  }

  /**
   * Bind the legend to the type sets maintained by the network generator. Only types contained in
   * the bound sets are rendered, so the legend reflects what the graph actually shows instead of
   * every possible enum value. Re-bind safely; the previous listeners are detached automatically.
   */
  public void bindToTypes(@NotNull final ObservableSet<NodeType> nodes,
      @NotNull final ObservableSet<EdgeType> edges) {
    if (boundNodeTypes != null) {
      boundNodeTypes.removeListener(nodeChangeListener);
    }
    if (boundEdgeTypes != null) {
      boundEdgeTypes.removeListener(edgeChangeListener);
    }
    boundNodeTypes = nodes;
    boundEdgeTypes = edges;
    nodes.addListener(nodeChangeListener);
    edges.addListener(edgeChangeListener);
    rebuildContent();
  }

  /**
   * Build the legend from a specific stylesheet (useful for tests or for reflecting a hot-reloaded
   * style). Filters respect the currently bound type sets.
   */
  public void rebuildFrom(@NotNull final NetworkStyleSheet style) {
    setContent(buildContent(style, boundNodeTypes, boundEdgeTypes));
  }

  private void rebuildContent() {
    setContent(buildContent(NetworkStyleSheet.loadDefault(), boundNodeTypes, boundEdgeTypes));
  }

  private static @NotNull javafx.scene.Node buildContent(@NotNull final NetworkStyleSheet style,
      @Nullable final Set<NodeType> nodeFilter, @Nullable final Set<EdgeType> edgeFilter) {
    final FlowPane nodeRow = new FlowPane(10, 4);
    // Dedup by toString() so enum values that share a display label (e.g. ION_FEATURE +
    // SINGLE_FEATURE both labelled "Feature") render as a single entry. nodeFilter == null means
    // "no graph yet bound" - show every enum value as a fallback.
    final Set<String> seenNodeLabels = new HashSet<>();
    for (final NodeType type : NodeType.values()) {
      if (nodeFilter != null && !nodeFilter.contains(type)) {
        continue;
      }
      if (seenNodeLabels.add(type.toString())) {
        nodeRow.getChildren().add(nodeItem(style, type));
      }
    }
    final FlowPane edgeRow = new FlowPane(10, 4);
    // EdgeType.toString() yields distinct readable labels via the underlying RowsRelationship.Type
    // so dedup-by-label collapses no analog/direct pairs unintentionally.
    final Set<String> seenEdgeLabels = new HashSet<>();
    for (final EdgeType type : EdgeType.values()) {
      if (edgeFilter != null && !edgeFilter.contains(type)) {
        continue;
      }
      if (seenEdgeLabels.add(type.toString())) {
        edgeRow.getChildren().add(edgeItem(style, type));
      }
    }

    // VBox keeps the two sections stacked; small left-side labels identify them
    final VBox box = new VBox(4, withSection("Nodes:", nodeRow), withSection("Edges:", edgeRow));
    box.setPadding(new Insets(4));
    return box;
  }

  private static @NotNull HBox withSection(@NotNull final String header,
      @NotNull final FlowPane items) {
    final Label label = new Label(header);
    label.setMinWidth(50);
    final HBox row = new HBox(8, label, items);
    HBox.setHgrow(items, Priority.ALWAYS);
    row.setAlignment(Pos.CENTER_LEFT);
    return row;
  }

  // ---- item builders ---------------------------------------------------------------------------

  private static @NotNull Node nodeItem(@NotNull final NetworkStyleSheet style,
      @NotNull final NodeType type) {
    final String cssClass = type.getUiClass().orElse("");
    final List<Color> fills = style.getNodeFillColors(cssClass);
    final String shape = style.getNodeShape(cssClass);
    final Shape icon =
        "diamond".equalsIgnoreCase(shape) ? diamondSwatch(SWATCH_H) : circleSwatch(SWATCH_H / 2.0);
    icon.setFill(fillOf(fills));
    icon.setStroke(Color.gray(0.4));
    icon.setStrokeWidth(0.5);
    return labelled(icon, type.toString());
  }

  private static @NotNull Node edgeItem(@NotNull final NetworkStyleSheet style,
      @NotNull final EdgeType type) {
    final String cssClass = type.getUiClass().orElse("");
    final List<Color> fills = style.getEdgeFillColors(cssClass);
    final String strokeMode = style.getEdgeStrokeMode(cssClass);

    final Line line = new Line(0, SWATCH_H / 2.0, SWATCH_W, SWATCH_H / 2.0);
    line.setStrokeWidth(2.5);
    line.setStrokeLineCap(StrokeLineCap.BUTT);
    // GraphStream draws edge color via fill-color; we map that onto JavaFX stroke since JavaFX
    // lines have no fill, only stroke
    line.setStroke(fillOf(fills));
    applyStrokeMode(line, strokeMode);
    return labelled(line, type.toString());
  }

  private static @NotNull HBox labelled(@NotNull final Shape icon, @NotNull final String text) {
    final HBox box = new HBox(4, icon, new Label(text));
    box.setAlignment(Pos.CENTER_LEFT);
    return box;
  }

  // Multi-color fills become a horizontal linear gradient so the icon hints at the dyn-plain
  // color range without having to enumerate breakpoints in the legend.
  private static @NotNull javafx.scene.paint.Paint fillOf(@NotNull final List<Color> fills) {
    if (fills.isEmpty()) {
      return Color.GRAY;
    }
    if (fills.size() == 1) {
      return fills.getFirst();
    }
    final Stop[] stops = new Stop[fills.size()];
    for (int i = 0; i < fills.size(); i++) {
      final double offset = fills.size() == 1 ? 0 : (double) i / (fills.size() - 1);
      stops[i] = new Stop(offset, fills.get(i));
    }
    return new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops);
  }

  private static void applyStrokeMode(@NotNull final Line line, final String strokeMode) {
    if (strokeMode == null) {
      return;
    }
    switch (strokeMode.toLowerCase()) {
      case "dashes" -> line.getStrokeDashArray().setAll(6.0, 3.0);
      case "dots" -> line.getStrokeDashArray().setAll(1.0, 3.0);
      // "plain" / "none" / anything else: solid line, nothing to do
      default -> {
      }
    }
  }

  private static @NotNull Circle circleSwatch(final double radius) {
    return new Circle(radius);
  }

  // Equilateral diamond bounded by the given height. Used for the "annotated"/diamond node shape.
  private static @NotNull Polygon diamondSwatch(final double height) {
    final double h = height;
    final Polygon p = new Polygon();
    p.getPoints().addAll(h / 2, 0.0, h, h / 2, h / 2, h, 0.0, h / 2);
    return p;
  }

}
