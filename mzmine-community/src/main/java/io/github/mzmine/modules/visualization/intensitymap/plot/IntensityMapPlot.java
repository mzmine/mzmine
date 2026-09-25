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

package io.github.mzmine.modules.visualization.intensitymap.plot;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.preferences.ImageNormalization;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxFileChooser;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapBounds;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapDetail;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapPosition;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapRegion;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSeries;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapMesh;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapPicker;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapScale;
import io.github.mzmine.modules.visualization.intensitymap.render.IntensityMapTile;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javax.imageio.ImageIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reusable, interactive 3D plot, also the 2D view. All samples use the same physical axes and,
 * unless normalized, the same intensity scale. The plot owns the scene, the overlays, and their
 * state, and coordinates its parts: camera, fixed 2D plot area, zoom box, hover, materials,
 * controls, tile layout, and the background mesh builder.
 */
public final class IntensityMapPlot extends BorderPane implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(IntensityMapPlot.class.getName());
  // highlight of markers and selections, also used by the detail panes
  public static final Color ACCENT = Color.web("#f97316");
  // initial opacity of overlaid surfaces
  private static final double OVERLAY_OPACITY = 0.8;

  private final boolean flat;
  private final IntensityMapAxes axes = new IntensityMapAxes();
  private final Group surfaces = new Group();
  private final Group markers = new Group();
  private final Group model = new Group();
  private final Pane labelLayer = new Pane();
  private final StackPane viewport = new StackPane();
  private final SubScene scene;
  private final IntensityMapCamera camera;
  private final IntensityMapPlotArea plotArea;
  private final IntensityMapZoomBox zoomBox = new IntensityMapZoomBox(model);
  private final IntensityMapHover hover = new IntensityMapHover();
  private final IntensityMapMaterials materials = new IntensityMapMaterials();
  private final IntensityMapControls controls;
  private final IntensityMapTileLayout tileLayout = new IntensityMapTileLayout(model, axes,
      labelLayer);
  private final IntensityMapMeshBuilder meshBuilder = new IntensityMapMeshBuilder();
  private final IntensityMapOverlayPanel panel = new IntensityMapOverlayPanel();
  private final Label status = new Label("Loading data…");
  private final Label legendMinimum = new Label("0");
  private final Label legendMaximum = new Label();
  private final ImageView legend = new ImageView();
  private final HBox legendBox = new HBox(6);
  private final HBox loadingBox = new HBox(8);
  private final ProgressIndicator loadingIndicator = new ProgressIndicator();
  private final Label loadingLabel = new Label();
  private final Label placeholder = FxLabels.newItalicLabel("No data");
  // collapsible sections instead of show/hide buttons (user request)
  private final IntensityMapSection detailSection = new IntensityMapSection();
  private final BorderPane collapsedDetail = new BorderPane();
  private final SplitPane split = new SplitPane();
  private final Map<String, IntensityMapSeriesState> states = new LinkedHashMap<>();
  private double detailDivider = 0.62;
  private @Nullable IntensityMapAxesSpec axesSpec;
  // scale of marker line widths, follows the zoom like the axes
  private double lineScale = 1;
  private @Nullable Consumer<ImageNormalization> normalizationListener;
  private boolean updatingNormalization;
  // plot background below the data, null follows the theme
  private @Nullable Color plotBackground;
  // input overlays, and the displayed overlays after smoothing with the same ids
  private List<IntensityMapSeries> rawSeries = List.of();
  private List<IntensityMapSeries> series = List.of();
  private @Nullable IntensityMapBounds bounds;
  private @Nullable IntensityMapScale scale;
  private @Nullable Consumer<IntensityMapDetail> detailListener;
  private @Nullable Consumer<IntensityMapPosition> selectionListener;
  private @Nullable BiConsumer<String, Color> colorListener;
  private @Nullable IntensityMapPosition selection;
  private IntensityMapSliceMode sliceMode = IntensityMapSliceMode.POINT;
  private IntensityMapFormat format = IntensityMapFormat.PLAIN;
  // decision: without preferences, units follow the mzmine default style, e.g. "Intensity / %"
  private UnitFormat unitFormat = UnitFormat.DIVIDE;
  private @Nullable Node detailPane;
  private boolean closed;
  private boolean dark;
  private double dragX;
  private double dragY;
  private boolean dragged;
  // side by side tile the camera is fitted to, -1 fits all
  private int focusedTile = -1;
  private List<Point3D> fittingPoints = List.of();

  public IntensityMapPlot(@NotNull final SimpleColorPalette palette) {
    this(palette, false);
  }

  /**
   * @param flat 2D view: a fixed top view of flat geometry colored by intensity, without heights,
   *             rotation, or lighting, and without the controls that only apply in 3D
   */
  public IntensityMapPlot(@NotNull final SimpleColorPalette palette, final boolean flat) {
    this.flat = flat;
    controls = new IntensityMapControls(palette, flat);
    panel.setIntensityFormat(format::intensity);
    model.getChildren()
        .setAll(axes.geometry(), tileLayout.extraAxesGroup(), surfaces, markers, hover.slices());
    final PointLight key = new PointLight(Color.rgb(165, 170, 180));
    key.setTranslateX(-350);
    key.setTranslateY(-650);
    key.setTranslateZ(-400);
    final PointLight fill = new PointLight(Color.rgb(45, 50, 60));
    fill.setTranslateX(600);
    fill.setTranslateY(-250);
    fill.setTranslateZ(300);
    final PointLight front = new PointLight(Color.rgb(85, 90, 100));
    front.setTranslateY(-150);
    front.setTranslateZ(-1400);
    // decision: the 2D view is unlit, so colors match the paint scale exactly
    final Group world = flat ? new Group(model, new AmbientLight(Color.WHITE))
        : new Group(model, new AmbientLight(Color.rgb(110, 110, 110)), key, fill, front);
    if (flat) {
      // decision: coplanar floor, grid, data, and markers draw in scene order, a depth test
      // needs lifted layers, which drift apart from the axes at deep zoom
      model.setDepthTest(DepthTest.DISABLE);
    }
    scene = new SubScene(world, 900, 650, true, SceneAntialiasing.BALANCED);
    camera = new IntensityMapCamera(model, scene, viewport, flat);
    plotArea = new IntensityMapPlotArea(camera, model, scene);
    surfaces.setMouseTransparent(true);
    markers.getChildren().setAll(hover.cursor());
    markers.getChildren().add(zoomBox.node());
    markers.setMouseTransparent(true);

    final Pane overlay = new Pane(hover.readout());
    overlay.setMouseTransparent(true);
    overlay.setPickOnBounds(false);
    loadingIndicator.setPrefSize(18, 18);
    loadingBox.getChildren().setAll(loadingIndicator, loadingLabel);
    loadingBox.setAlignment(Pos.CENTER_LEFT);
    loadingBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    loadingBox.setMouseTransparent(true);
    loadingBox.setVisible(false);
    StackPane.setAlignment(loadingBox, Pos.TOP_LEFT);
    StackPane.setMargin(loadingBox, new Insets(12));
    placeholder.setMouseTransparent(true);
    labelLayer.setMouseTransparent(true);
    labelLayer.setPickOnBounds(false);
    viewport.getChildren().addAll(scene, labelLayer, overlay, loadingBox, placeholder);
    // labels near the border must not paint over the toolbar or the side panel
    final Rectangle clip = new Rectangle();
    clip.widthProperty().bind(viewport.widthProperty());
    clip.heightProperty().bind(viewport.heightProperty());
    viewport.setClip(clip);
    viewport.setMinSize(260, 260);
    viewport.setPrefSize(1050, 740);
    viewport.setFocusTraversable(true);
    scene.widthProperty().bind(viewport.widthProperty());
    scene.heightProperty().bind(viewport.heightProperty());
    initInteraction();
    viewport.widthProperty().addListener((_, _, _) -> requestDetail());
    viewport.heightProperty().addListener((_, _, _) -> requestDetail());
    setCenter(viewport);

    setTop(controls.toolbar(
        new IntensityMapViewActions(this::resetView, this::topView, this::frontView, this::sideView,
            this::saveImage, this::setPlotBackground)));
    bindControls();
    updateDisplayControls();
    setRight(new IntensityMapSideDrawer("Overlays", panel));
    detailSection.expandedProperty().addListener((_, _, _) -> layoutDetail());

    legend.setFitWidth(160);
    legend.setFitHeight(12);
    legend.setSmooth(true);
    legendBox.getChildren().setAll(new Label("Intensity"), legendMinimum, legend, legendMaximum);
    legendBox.setAlignment(Pos.CENTER_LEFT);
    status.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(status, Priority.ALWAYS);
    final HBox footer = FxLayout.newHBox(Pos.CENTER_LEFT, new Insets(6, 12, 6, 12), status,
        legendBox);
    footer.setSpacing(18);
    setBottom(footer);
    setDarkMode(false);
    showEmpty("Loading data…");
  }

  private void bindControls() {
    controls.heightSlider().valueProperty().addListener((_, _, value) -> {
      camera.setHeightScale(value.doubleValue());
      if (camera.isAutoFit()) {
        fitView();
      }
      projectAxes();
    });
    controls.transformBox().valueProperty().addListener((_, _, _) -> rebuild());
    controls.intensityNormalization().valueProperty().addListener((_, _, value) -> {
      if (!updatingNormalization && normalizationListener != null && value != null) {
        normalizationListener.accept(value);
      }
    });
    controls.normalized().selectedProperty().addListener((_, _, _) -> rebuild());
    controls.fromLowest().selectedProperty().addListener((_, _, _) -> rebuild());
    controls.coloring().valueProperty().addListener((_, _, _) -> updateMaterials());
    controls.palettes().valueProperty().addListener((_, _, _) -> updateMaterials());
    controls.smoothingRadius().valueProperty().addListener((_, _, _) -> refreshDisplayed());
    controls.smoothingAxes().valueProperty().addListener((_, _, _) -> refreshDisplayed());
    for (final var spinner : controls.gridSpinners()) {
      spinner.valueProperty().addListener((_, _, _) -> {
        if (controls.layout().get() == IntensityMapLayout.GRID) {
          rebuild();
        }
      });
    }
    controls.showGrid().selectedProperty()
        .addListener((_, _, show) -> allAxes().forEach(value -> value.setGridVisible(show)));
    controls.showLabels().selectedProperty().addListener((_, _, show) -> {
      labelLayer.setVisible(show);
      if (camera.isAutoFit()) {
        fitView();
      }
      projectAxes();
    });
    controls.layout().addListener((_, _, value) -> {
      if (value == IntensityMapLayout.OVERLAY && series.size() > 1) {
        controls.coloring().setValue(IntensityMapColoring.SAMPLE);
      }
      // decision: tiles change their size and place, so the zoom of the former layout is lost
      camera.setAutoFit(true);
      focusedTile = -1;
      updateDisplayControls();
      rebuild();
    });
    controls.setOnNoiseChanged(this::rebuild);
  }

  private void updateDisplayControls() {
    controls.updateDisplay(series.isEmpty() ? rawSeries : series, rawSeries.size() > 1,
        normalizationListener != null);
  }

  /**
   * @param value vertical exaggeration, clamped to the slider range
   */
  public void setHeightScale(final double value) {
    final Slider slider = controls.heightSlider();
    slider.setValue(Math.clamp(value, slider.getMin(), slider.getMax()));
  }

  public double minimumHeightScale() {
    return controls.heightSlider().getMin();
  }

  private void initInteraction() {
    scene.setOnMousePressed(event -> {
      viewport.requestFocus();
      dragX = event.getSceneX();
      dragY = event.getSceneY();
      dragged = false;
      // Ctrl/⌘ + drag zooms to a box on the floor
      if (event.isShortcutDown() && event.getButton() == MouseButton.PRIMARY && bounds != null) {
        final IntensityMapPicker.Ray ray = camera.ray(event.getX(), event.getY());
        final IntensityMapTile tile = tileLayout.tileAtFloor(ray);
        markers.getTransforms().setAll(tile.transforms());
        zoomBox.start(tile, IntensityMapPicker.floor(tile.toLocal(ray), true));
      }
      hideHover();
    });
    scene.setOnMouseDragged(event -> {
      dragged = true;
      if (zoomBox.isActive()) {
        zoomBox.drag(
            IntensityMapPicker.floor(zoomBox.tile().toLocal(camera.ray(event.getX(), event.getY())),
                true));
        updateZoomBox();
        return;
      }
      camera.drag(event.getSceneX() - dragX, event.getSceneY() - dragY,
          event.isShiftDown() || event.getButton() == MouseButton.SECONDARY
              || event.getButton() == MouseButton.MIDDLE);
      dragX = event.getSceneX();
      dragY = event.getSceneY();
      // the axes stay at the view edges while dragging
      updateAxesFrame();
      projectAxes();
    });
    scene.setOnMouseReleased(event -> {
      if (zoomBox.isActive()) {
        zoomToBox();
        return;
      }
      if (dragged) {
        // a new view may show another part of the data
        requestDetail();
      } else {
        if (event.getButton() == MouseButton.PRIMARY && !event.isShortcutDown()) {
          select(event.getX(), event.getY());
        }
        hover(event.getX(), event.getY());
      }
    });
    scene.setOnMouseClicked(event -> {
      if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
        // side by side: a tile fills the view, elsewhere everything is fitted again
        final int tile = tileAt(event.getX(), event.getY());
        if (tile >= 0) {
          focusTile(tile);
        } else {
          resetView();
        }
      }
    });
    scene.setOnMouseMoved(event -> hover(event.getX(), event.getY()));
    scene.setOnMouseExited(_ -> hideHover());
    scene.setOnScroll(event -> {
      // assumption: trackpads report touches, momentum, or horizontal deltas; mouse wheels
      // do not. Two-finger scrolling pans, the wheel and Ctrl/⌘ + scroll zoom at the cursor.
      final boolean trackpad =
          event.getTouchCount() > 0 || event.isInertia() || event.getDeltaX() != 0;
      if (trackpad && !event.isShortcutDown()) {
        pan(event.getDeltaX(), event.getDeltaY());
      } else {
        zoomAt(Math.exp(event.getDeltaY() * 0.0015), event.getX(), event.getY());
      }
      event.consume();
    });
    scene.setOnZoom(event -> {
      zoomAt(event.getZoomFactor(), event.getX(), event.getY());
      event.consume();
    });
    viewport.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
      switch (event.getCode()) {
        case R -> resetView();
        case T -> topView();
        case F -> {
          if (controls.hasProfileViews()) {
            frontView();
          }
        }
        case S -> {
          if (controls.hasProfileViews()) {
            sideView();
          }
        }
        case LEFT -> rotate(-5, 0);
        case RIGHT -> rotate(5, 0);
        case UP -> rotate(0, 5);
        case DOWN -> rotate(0, -5);
        case PLUS, EQUALS, ADD -> zoom(1.15);
        case MINUS, SUBTRACT -> zoom(1 / 1.15);
        case ESCAPE -> zoomBox.cancel();
        // decision: other keys stay with the scene, e.g. for tab shortcuts
        default -> {
          return;
        }
      }
      event.consume();
    });
  }

  private void pan(final double dx, final double dy) {
    camera.pan(dx, dy);
    hideHover();
    requestDetail();
  }

  /**
   * Zooms towards the point under the cursor, which stays in place on screen. This allows zooming
   * into any tile of side by side views.
   */
  private void zoomAt(final double factor, final double x, final double y) {
    if (!(factor > 0) || !Double.isFinite(factor)) {
      return;
    }
    final Point3D target = worldPointAt(x, y);
    if (target == null) {
      zoom(factor);
      return;
    }
    if (camera.zoomTowards(factor, target)) {
      hideHover();
      requestDetail();
    }
  }

  /**
   * Zooms towards the viewport center.
   */
  private void zoom(final double factor) {
    if (!(factor > 0) || !Double.isFinite(factor)) {
      return;
    }
    if (worldPointAt(scene.getWidth() / 2, scene.getHeight() / 2) != null) {
      zoomAt(factor, scene.getWidth() / 2, scene.getHeight() / 2);
      return;
    }
    camera.zoomDistance(factor);
    hideHover();
    requestDetail();
  }

  private void rotate(final double turnDelta, final double tiltDelta) {
    if (flat) {
      // arrow keys move the 2D view instead
      pan(-turnDelta * 8, tiltDelta * 8);
      return;
    }
    camera.rotate(turnDelta, tiltDelta);
    projectAxes();
  }

  /**
   * @return the picked surface or floor point under the cursor in world coordinates, or the point
   * at the depth of the plot center
   */
  private @Nullable Point3D worldPointAt(final double x, final double y) {
    final IntensityMapTileHit picked = pick(x, y);
    if (picked != null) {
      final IntensityMapPicker.Hit hit = picked.hit();
      return model.localToParent(picked.tile().toModel(new Point3D(hit.x(), hit.y(), hit.z())));
    }
    return camera.pointAtModelDepth(x, y);
  }

  /**
   * @return index of the side by side tile under the cursor, -1 if none
   */
  private int tileAt(final double x, final double y) {
    final List<IntensityMapTile> tiles = tileLayout.tiles();
    if (controls.layout().get() != IntensityMapLayout.GRID || tiles.size() < 2) {
      return -1;
    }
    final IntensityMapTileHit picked = pick(x, y);
    return picked == null ? -1 : tiles.indexOf(picked.tile());
  }

  /**
   * Fits the camera to one tile; the other tiles stay in place around it.
   */
  private void focusTile(final int index) {
    focusedTile = index;
    camera.resetPan();
    camera.setAutoFit(true);
    hideHover();
    fitView();
    projectAxes();
    requestDetail();
  }

  @Override
  protected void layoutChildren() {
    super.layoutChildren();
    projectAxes();
  }

  public void setDetailListener(@Nullable final Consumer<IntensityMapDetail> listener) {
    detailListener = listener;
  }

  public void setOnAddSelectedFiles(@Nullable final Runnable listener) {
    panel.setOnAddFiles(listener);
  }

  /**
   * Enables the m/z overlay input. The listener receives the parsed extraction ranges.
   */
  public void setOnAddMzRanges(@Nullable final Consumer<List<Range<Double>>> listener) {
    panel.setOnAddMz(listener);
  }

  public void setOnRemoveSeries(@Nullable final Consumer<String> listener) {
    panel.setOnRemove(listener);
  }

  /**
   * @param value intensity transformation of height and colors
   */
  public void setTransform(@NotNull final PaintScaleTransform value) {
    controls.transformBox().setValue(value);
  }

  /**
   * @param value start heights at the lowest shown intensity instead of zero, e.g. for images whose
   *              weakest pixels would otherwise form a tall base
   */
  public void setHeightsFromLowest(final boolean value) {
    controls.fromLowest().setSelected(value);
  }

  /**
   * @return fraction of the maximum at the floor: the weakest pixel of all overlays, at least the
   * noise floor, as pixels below it are not drawn. 0 if heights start at zero.
   */
  private double baseline(@NotNull final IntensityMapBounds scaleBounds, final boolean perOverlay,
      final double noise) {
    if (!controls.fromLowest().isSelected()) {
      return 0;
    }
    double lowest = 1;
    for (final IntensityMapSeries value : series) {
      final double maximum = perOverlay ? value.data().maximum() : scaleBounds.maximum();
      if (maximum > 0 && value.data().minimum() > 0) {
        lowest = Math.min(lowest, value.data().minimum() / maximum);
      }
    }
    return lowest < 1 ? Math.max(noise, lowest) : 0;
  }

  /**
   * Shows the intensity normalization selection. Normalization changes the sampled data, so the
   * listener should resample.
   */
  public void setOnIntensityNormalizationChanged(
      @Nullable final Consumer<ImageNormalization> listener) {
    normalizationListener = listener;
    updateDisplayControls();
  }

  public void setIntensityNormalization(@NotNull final ImageNormalization value) {
    updatingNormalization = true;
    controls.intensityNormalization().setValue(value);
    updatingNormalization = false;
  }

  /**
   * Replaces the selectable paint scales, for example with all scales of the preferences.
   */
  public void setPaintScales(@NotNull final List<SimpleColorPalette> scales) {
    controls.setPaintScales(scales);
  }

  public void setPaintScale(@NotNull final SimpleColorPalette scale) {
    controls.setPaintScale(scale);
  }

  /**
   * @param title header of the collapsible section
   */
  public void setDetailPane(@Nullable final Node pane, @NotNull final String title) {
    detailPane = pane;
    detailSection.setTitle(title);
    detailSection.setContent(pane);
    layoutDetail();
  }

  /**
   * @param title header text of the detail section, e.g. with the shown position
   * @param hint  short usage hint on the right of the header, also a tooltip
   */
  public void setDetailHeader(@NotNull final String title, @NotNull final String hint) {
    detailSection.setTitle(title);
    detailSection.setHint(hint);
  }

  /**
   * Expanded, the section shares a split pane with the 3D view. Collapsed, only its header remains
   * below the 3D view, which gets the full height.
   */
  private void layoutDetail() {
    if (!split.getItems().isEmpty() && split.getDividers().size() == 1) {
      detailDivider = split.getDividerPositions()[0];
    }
    split.getItems().clear();
    collapsedDetail.setCenter(null);
    collapsedDetail.setBottom(null);
    if (detailPane == null) {
      setCenter(viewport);
      return;
    }
    if (detailSection.isExpanded()) {
      split.setOrientation(Orientation.VERTICAL);
      split.getItems().setAll(viewport, detailSection);
      // proportional resizing, a fixed height would keep the size from before the first layout
      split.setDividerPositions(detailDivider);
      setCenter(split);
    } else {
      collapsedDetail.setCenter(viewport);
      collapsedDetail.setBottom(detailSection);
      setCenter(collapsedDetail);
    }
  }

  /**
   * Plain clicks on the 3D view select a position, for example to show its spectrum.
   */
  public void setOnSelectionChanged(@Nullable final Consumer<IntensityMapPosition> listener) {
    selectionListener = listener;
  }

  /**
   * Notifies about overlay colors changed by the user, to keep other views consistent.
   */
  public void setOnSeriesColorChanged(@Nullable final BiConsumer<String, Color> listener) {
    colorListener = listener;
  }

  public void setSliceMode(@NotNull final IntensityMapSliceMode mode) {
    sliceMode = mode;
  }

  public @Nullable IntensityMapPosition getSelection() {
    return selection;
  }

  /**
   * Sets the selected position without notifying the listener.
   */
  public void setSelection(@Nullable final IntensityMapPosition selection) {
    this.selection = selection;
  }

  public @Nullable MZTolerance mzTolerance() {
    return panel.mzTolerance();
  }

  public void setOnResetMzRanges(@Nullable final Runnable listener) {
    panel.setOnResetMz(listener);
  }

  private void select(final double x, final double y) {
    if (selectionListener == null || scale == null) {
      return;
    }
    final IntensityMapTileHit picked = pick(x, y);
    if (picked == null) {
      return;
    }
    setSelection(
        new IntensityMapPosition(IntensityMapPicker.dataX(scale.bounds(), picked.hit().x()),
            IntensityMapPicker.dataY(scale.bounds(), picked.hit().z())));
    selectionListener.accept(selection);
  }

  /**
   * Picks the closest surface along the ray through the sub scene position, over all tiles.
   */
  private @Nullable IntensityMapTileHit pick(final double x, final double y) {
    if (scale == null) {
      return null;
    }
    final IntensityMapPicker.Ray ray = camera.ray(x, y);
    final List<IntensityMapTile> tiles = tileLayout.tiles();
    final List<IntensityMapSeries> tiled = tileLayout.tiled();
    if (tiles.size() == 1) {
      final List<IntensityMapPicker.Target> targets = visibleSeries().stream()
          .map(value -> new IntensityMapPicker.Target(value, scale)).toList();
      final IntensityMapPicker.Hit hit = IntensityMapPicker.pick(ray, targets);
      return hit == null ? null : new IntensityMapTileHit(hit, IntensityMapTile.IDENTITY);
    }
    // the closest surface wins, floors only if no surface is hit
    IntensityMapTileHit surface = null;
    IntensityMapTileHit floor = null;
    for (int i = 0; i < tiled.size() && i < tiles.size(); i++) {
      final IntensityMapTile tile = tiles.get(i);
      final IntensityMapPicker.Hit hit = IntensityMapPicker.pick(tile.toLocal(ray),
          List.of(new IntensityMapPicker.Target(tiled.get(i), scale)));
      if (hit == null) {
        continue;
      }
      if (hit.target() != null && (surface == null || hit.t() < surface.hit().t())) {
        surface = new IntensityMapTileHit(hit, tile);
      } else if (hit.target() == null && (floor == null || hit.t() < floor.hit().t())) {
        floor = new IntensityMapTileHit(hit, tile);
      }
    }
    return surface != null ? surface : floor;
  }

  private @NotNull List<IntensityMapSeries> visibleSeries() {
    return series.stream().filter(value -> states.get(value.id()).isVisible()).toList();
  }

  /**
   * Shows which spectrum a click would select, only while hovering (user decision: a persistent
   * marker was too heavy).
   *
   * @param selection hovered position, null hides the marker
   */
  private void updateSliceMarker(@Nullable final IntensityMapPosition selection) {
    final IntensityMapBounds current = bounds;
    if (selection == null || current == null || selectionListener == null) {
      hover.clearSlices();
      return;
    }
    // a slice only depends on one coordinate
    final boolean xInside = selection.x() >= current.xMin() && selection.x() <= current.xMax();
    final boolean yInside = selection.y() >= current.yMin() && selection.y() <= current.yMax();
    final boolean inside = switch (sliceMode) {
      case X -> xInside;
      case Y -> yInside;
      case POINT -> xInside && yInside;
    };
    if (!inside) {
      hover.clearSlices();
      return;
    }
    hover.showSlices(tileLayout.tiles(), IntensityMapMesh.localX(current, selection.x()),
        IntensityMapMesh.localZ(current, selection.y()), sliceMode, this::sliceTop, lineScale);
  }

  /**
   * @return height of the highest data point in the tile, in local units, slightly above the
   * surface so that lines on top stay visible
   */
  private double sliceTop(final int tile) {
    final List<float[]> envelopes = new ArrayList<>();
    final List<IntensityMapSeries> tiled = tileLayout.tiled();
    if (controls.layout().get() == IntensityMapLayout.GRID && tile < tiled.size()) {
      final IntensityMapSeriesState state = states.get(tiled.get(tile).id());
      if (state != null) {
        envelopes.add(state.envelope());
      }
    } else {
      for (final IntensityMapSeries value : visibleSeries()) {
        envelopes.add(states.get(value.id()).envelope());
      }
    }
    double lowest = 0;
    for (final float[] envelope : envelopes) {
      for (final float height : envelope) {
        lowest = Math.min(lowest, height);
      }
    }
    // no signal: a low marker on the floor
    return lowest < 0 ? -lowest + 1 : 4;
  }

  /**
   * Uses the mzmine number formats for axes, readouts and the legend.
   */
  public void setNumberFormats(@Nullable final NumberFormats formats) {
    format = new IntensityMapFormat(formats);
    panel.setIntensityFormat(format::intensity);
    rebuild();
  }

  /**
   * Unit style of axis titles the plot creates. Data labels are formatted by their source.
   */
  public void setUnitFormat(@NotNull final UnitFormat unitFormat) {
    this.unitFormat = unitFormat;
    rebuild();
  }

  public void setDarkMode(final boolean dark) {
    this.dark = dark;
    final Color background = Color.web(dark ? "#1b1f24" : "#f8fafc");
    scene.setFill(background);
    // the clipped 2D view shows the viewport around its plot area
    viewport.setBackground(Background.fill(background));
    applyPlotBackground();
    final String bubble = dark
        ? "-fx-background-color: rgba(30,35,42,0.94); -fx-border-color: #475569; -fx-text-fill: #e2e8f0;"
        : "-fx-background-color: rgba(255,255,255,0.94); -fx-border-color: #cbd5e1; -fx-text-fill: #1e293b;";
    final String shape = "-fx-background-radius: 6; -fx-border-radius: 6; -fx-padding: 6 9 6 9;";
    hover.setStyle(dark, bubble + shape);
    loadingBox.setStyle(bubble + shape);
    loadingLabel.setStyle(dark ? "-fx-text-fill: #e2e8f0;" : "-fx-text-fill: #1e293b;");
    placeholder.setStyle(dark ? "-fx-text-fill: #94a3b8;" : "-fx-text-fill: #64748b;");
    tileLayout.setTitleStyle(tileTitleStyle());
    projectAxes();
  }

  /**
   * @return the plot background below the data: the chosen color or the one of the theme
   */
  private @NotNull Color plotBackground() {
    return plotBackground != null ? plotBackground : IntensityMapAxes.defaultFloor(dark);
  }

  private void applyPlotBackground() {
    allAxes().forEach(value -> value.setColors(dark, plotBackground));
    // overlay gradients start at the plot background
    materials.clearGradients();
    updateMaterials();
  }

  private @NotNull String tileTitleStyle() {
    return (dark ? "-fx-text-fill: #e2e8f0;" : "-fx-text-fill: #1e293b;")
        + "-fx-font-weight: bold; -fx-font-size: 12; -fx-padding: 0;";
  }

  /**
   * @param color plot background below the data, null for the one of the theme
   */
  public void setPlotBackground(@Nullable final Color color) {
    plotBackground = color;
    applyPlotBackground();
  }

  public @NotNull IntensityMapDetail detail() {
    // decision: the 2D view samples logical pixels, cells of one physical pixel on HiDPI
    // screens still drop out between rows
    final double outputScale =
        !flat && getScene() != null && getScene().getWindow() != null ? getScene().getWindow()
            .getOutputScaleX() : 1;
    return new IntensityMapDetail(Math.max(600, viewport.getWidth()) * outputScale,
        Math.max(400, viewport.getHeight()) * outputScale, Math.max(1, series.size()), flat);
  }

  private void requestDetail() {
    if (camera.isAutoFit()) {
      fitView();
    }
    camera.updateClipping(worldPointAt(scene.getWidth() / 2, scene.getHeight() / 2));
    updateAxesFrame();
    projectAxes();
    if (detailListener != null && !closed) {
      detailListener.accept(detail());
    }
  }

  public void setStatus(@NotNull final String message) {
    status.setText(message);
  }

  /**
   * @param message  null hides the indicator
   * @param progress progress in [0, 1] or negative for indeterminate
   */
  public void setLoading(@Nullable final String message, final double progress) {
    loadingBox.setVisible(message != null);
    if (message != null) {
      loadingLabel.setText(message);
      loadingIndicator.setProgress(
          progress < 0 ? ProgressIndicator.INDETERMINATE_PROGRESS : Math.min(1, progress));
    }
  }

  public void setSeries(@NotNull final List<IntensityMapSeries> values) {
    if (closed) {
      return;
    }
    if (values.isEmpty()) {
      meshBuilder.reset();
      rawSeries = List.of();
      series = List.of();
      bounds = null;
      scale = null;
      states.clear();
      surfaces.getChildren().clear();
      axes.clear();
      tileLayout.clear();
      fittingPoints = List.of();
      legendBox.setVisible(false);
      legendBox.setManaged(false);
      panel.setSeries(series, states);
      hideHover();
      showEmpty("No overlays");
      setStatus("No overlays. Add m/z values or raw files to begin.");
      return;
    }
    final IntensityMapGrid first = values.getFirst().data();
    for (final IntensityMapSeries value : values) {
      if (!first.xLabel().equals(value.data().xLabel()) || !first.yLabel()
          .equals(value.data().yLabel())) {
        throw new IllegalArgumentException(
            "Overlaid samples must use the same coordinate dimensions and units");
      }
    }
    if (rawSeries.size() <= 1 && values.size() > 1 && !controls.isLayoutChosen()) {
      // decision: coincident surfaces fight for depth, small multiples stay readable
      controls.layout().set(IntensityMapLayout.GRID);
    }
    rawSeries = List.copyOf(values);
    states.keySet().retainAll(rawSeries.stream().map(IntensityMapSeries::id).toList());
    meshBuilder.retainSmoothed(states.keySet());
    // while smoothing, displayed overlays may lag behind; removed ones must not be looked up
    series = series.stream().filter(value -> states.containsKey(value.id())).toList();
    meshBuilder.invalidate();
    for (final IntensityMapSeries value : rawSeries) {
      states.computeIfAbsent(value.id(), _ -> createState(value));
    }
    panel.setSeries(rawSeries, states);
    placeholder.setVisible(false);
    refreshDisplayed();
  }

  /**
   * Smooths the input overlays in the background if needed, then shows them.
   */
  private void refreshDisplayed() {
    if (rawSeries.isEmpty() || closed) {
      return;
    }
    if (meshBuilder.smooth(rawSeries, controls.smoothing(), this::showDisplayed,
        this::refreshDisplayed, error -> {
          setLoading(null, 0);
          setStatus("Cannot smooth: " + error.getMessage());
        })) {
      setLoading("Smoothing…", -1);
    }
  }

  private void showDisplayed(@NotNull final List<IntensityMapSeries> values) {
    series = values;
    updateDisplayControls();
    bounds = IntensityMapBounds.of(series.stream().map(IntensityMapSeries::data).toList());
    rebuild();
  }

  private @NotNull IntensityMapSeriesState createState(@NotNull final IntensityMapSeries value) {
    final IntensityMapSeriesState state = new IntensityMapSeriesState(value.color(),
        OVERLAY_OPACITY);
    state.opacityProperty().addListener((_, _, _) -> updateMaterial(state));
    state.colorLowProperty().addListener((_, _, _) -> updateMaterial(state));
    state.colorHighProperty().addListener((_, _, _) -> updateMaterial(state));
    state.colorProperty().addListener((_, _, color) -> {
      updateMaterial(state);
      if (colorListener != null && color != null) {
        colorListener.accept(value.id(), color);
      }
    });
    state.visibleProperty().addListener((_, _, _) -> {
      hideHover();
      // the grid only contains visible overlays
      if (controls.layout().get() == IntensityMapLayout.OVERLAY) {
        updateStatus();
      } else {
        rebuild();
      }
    });
    return state;
  }

  private void showEmpty(@NotNull final String message) {
    placeholder.setText(message);
    placeholder.setVisible(true);
  }

  private void rebuild() {
    if (series.isEmpty() || bounds == null || closed) {
      return;
    }
    final double noise = controls.noiseFloor();
    final boolean perOverlay = controls.normalized().isSelected();
    final IntensityMapScale target =
        flat ? new IntensityMapScale(bounds, PaintScaleTransform.LINEAR, perOverlay, noise, 0, true)
            : new IntensityMapScale(bounds, controls.transform(), perOverlay, noise,
                baseline(bounds, perOverlay, noise), false);
    final List<IntensityMapSeries> snapshot = series;
    final int building = meshBuilder.build(snapshot, states, target,
        meshes -> finish(snapshot, target, meshes), error -> {
          setLoading(null, 0);
          setStatus("Cannot build surface: " + error.getMessage());
        });
    if (building > 0) {
      setLoading("Building " + building + (building == 1 ? " surface…" : " surfaces…"), -1);
    }
  }

  private void finish(@NotNull final List<IntensityMapSeries> snapshot,
      @NotNull final IntensityMapScale target,
      @NotNull final Map<String, IntensityMapBuiltMesh> meshes) {
    if (closed) {
      return;
    }
    for (final IntensityMapSeries value : snapshot) {
      final IntensityMapSeriesState state = states.get(value.id());
      final IntensityMapBuiltMesh built = meshes.get(value.id());
      if (built != null) {
        state.setMesh(value.data(), target, built.mesh(), built.envelope());
      } else if (!state.needsMesh(value.data(), target)) {
        // same geometry under a new scale instance
        state.updateScale(target);
      }
    }
    scale = target;
    final IntensityMapGrid first = snapshot.getFirst().data();
    final boolean relative = target.normalized();
    // decision: per-overlay log heights have no common numeric scale, so only the title remains
    axesSpec = new IntensityMapAxesSpec(target.bounds(), first.xLabel(), first.yLabel(),
        first.xKind(), first.yKind(),
        relative ? unitFormat.format("Relative intensity", "%") : "Intensity",
        relative ? target.logarithmic() ? 0 : 100 : target.bounds().maximum(), target.baseline(),
        target.transform(), relative ? IntensityMapFormat.PLAIN : format,
        axesSpec == null ? null : axesSpec.frame());
    allAxes().forEach(value -> value.rebuild(axesSpec));
    tileLayout.apply(controls.layout().get(), series, visibleSeries(), states,
        controls.gridColumns(), controls.gridRows(), surfaces, tileTitleStyle(), this::setUpAxes);
    hover.clearSlices();
    fittingPoints = tileLayout.fittingPoints(controls.layout().get(), snapshot, states);
    if (camera.isAutoFit()) {
      fitView();
    }
    updateMaterials();
    setLoading(null, 0);
    updateStatus();
    hideHover();
    requestLayout();
  }

  /**
   * Axes of additional side by side tiles look like the ones of the first tile.
   */
  private void setUpAxes(@NotNull final IntensityMapAxes added) {
    added.setColors(dark, plotBackground);
    added.setGridVisible(controls.showGrid().isSelected());
    if (axesSpec != null) {
      added.rebuild(axesSpec);
    }
  }

  private void updateStatus() {
    if (series.isEmpty() || scale == null) {
      return;
    }
    long triangles = 0;
    int visible = 0;
    for (final IntensityMapSeries value : series) {
      final IntensityMapSeriesState state = states.get(value.id());
      if (state.isVisible()) {
        visible++;
        triangles += state.triangles();
      }
    }
    setStatus(String.format(Locale.ROOT, "%d of %d overlays visible · %s triangles · %s", visible,
        series.size(),
        triangles >= 1_000_000 ? String.format(Locale.ROOT, "%.1f M", triangles / 1e6)
            : String.format(Locale.ROOT, "%.0f k", triangles / 1e3),
        scale.normalized() ? "each overlay normalized" : "shared intensity scale"));
  }

  private void hover(final double x, final double y) {
    if (scale == null || series.isEmpty() || zoomBox.isActive()) {
      hideHover();
      return;
    }
    final IntensityMapTileHit picked = pick(x, y);
    if (picked == null) {
      hideHover();
      return;
    }
    final IntensityMapPicker.Hit hit = picked.hit();
    markers.getTransforms().setAll(picked.tile().transforms());
    // the emphasized overlay is the hit surface
    final IntensityMapSeries emphasized = hit.target() == null ? null : hit.target().series();
    final double dataX = IntensityMapPicker.dataX(scale.bounds(), hit.x());
    final double dataY = IntensityMapPicker.dataY(scale.bounds(), hit.z());
    updateSliceMarker(new IntensityMapPosition(dataX, dataY));
    hover.showCursor(hit, lineScale);

    final IntensityMapGrid first = series.getFirst().data();
    final String header =
        first.xLabel() + "  " + format.value(dataX, first.xKind()) + "     " + first.yLabel() + "  "
            + format.value(dataY, first.yKind()) + (controls.smoothing().active() ? "     smoothed"
            : "");
    final List<IntensityMapHoverRow> rows = new ArrayList<>();
    for (final IntensityMapSeries value : visibleSeries()) {
      final double intensity = intensityAt(value.data(), dataX, dataY);
      rows.add(new IntensityMapHoverRow(
          value.fullName() + ":  " + (Double.isNaN(intensity) ? "–" : format.intensity(intensity)),
          seriesColor(value), value == emphasized));
    }
    hover.showReadout(header, rows, x, y, viewport.getWidth(), viewport.getHeight());
  }

  private @NotNull Color seriesColor(@NotNull final IntensityMapSeries value) {
    return states.get(value.id()).colorProperty().get();
  }

  private static double intensityAt(@NotNull final IntensityMapGrid data, final double x,
      final double y) {
    final int column = data.binX(x);
    final int row = data.binY(y);
    if (column < 0 || row < 0 || !data.isPresent(column, row)) {
      return Double.NaN;
    }
    return data.intensity(column, row);
  }

  private void hideHover() {
    hover.hide();
  }

  private void updateZoomBox() {
    final double minimum = 0.5 * lineScale;
    // decision (user request): the box reaches above the highest data point, a box on the floor
    // is hidden below the data; the 2D view draws it after the data anyway
    final int tile = Math.max(0, tileLayout.tiles().indexOf(zoomBox.tile()));
    zoomBox.show(minimum, flat ? minimum : Math.max(minimum, sliceTop(tile)));
  }

  /**
   * Fits the camera to the dragged box. The visible range is then shown in full detail.
   */
  private void zoomToBox() {
    final List<Point3D> modelCorners = zoomBox.finish();
    if (modelCorners == null) {
      return;
    }
    final List<Point3D> corners = modelCorners.stream().map(model::localToParent).toList();
    camera.setAutoFit(false);
    focusedTile = -1;
    camera.fit(corners);
    final Rectangle2D area = flat ? plotArea.area(plotTile()) : null;
    if (area != null) {
      camera.fitToArea(area, corners, modelCorners);
    }
    hideHover();
    requestDetail();
  }

  /**
   * @return the data window visible in the viewport, full if nearly all data are visible
   */
  public @NotNull IntensityMapRegion visibleWindow() {
    // the 2D view clips everything outside its fixed plot area
    final double[] area = flat ? plotArea.floor(plotTile()) : null;
    final double[] floor = area != null ? area : visibleFloor();
    final IntensityMapBounds current = bounds;
    if (floor == null || current == null || IntensityMapExtent.nearlyAll(floor)) {
      return IntensityMapRegion.FULL;
    }
    return IntensityMapExtent.toData(current, floor);
  }

  private double @Nullable [] visibleFloor() {
    return bounds == null || series.isEmpty() ? null : camera.visibleFloor(tileLayout.tiles());
  }

  /**
   * The 2D view keeps its axes at the fixed plot area, only their tick values change. The 3D view
   * keeps its axes at the data edges. Lines of both follow the zoom.
   */
  private void updateAxesFrame() {
    final IntensityMapAxesSpec spec = axesSpec;
    final IntensityMapBounds current = bounds;
    if (spec == null || current == null) {
      return;
    }
    final IntensityMapTile tile = plotTile();
    plotArea.updateClip(tile);
    IntensityMapRegion frame = null;
    lineScale = 1;
    final double[] plotFloor = plotArea.floor(tile);
    // side by side 2D tiles without focus have no plot area, their lines still follow the zoom
    final double[] area = plotFloor != null ? plotFloor : visibleFloor();
    if (area != null && !IntensityMapExtent.complete(area) && area[1] > area[0]
        && area[3] > area[2]) {
      lineScale = Math.min(1, IntensityMapAxes.thickness(area));
      frame = plotFloor != null ? IntensityMapExtent.toData(current, area) : null;
    }
    if (Objects.equals(frame, spec.frame())) {
      return;
    }
    axesSpec = spec.withFrame(frame);
    allAxes().forEach(value -> value.rebuild(axesSpec));
    projectAxes();
  }

  /**
   * @return true if overlay opacities apply: only overlaid surfaces cover each other (user
   * decision), side by side tiles stay opaque
   */
  private boolean opacityApplies() {
    return controls.layout().get() == IntensityMapLayout.OVERLAY && series.size() > 1;
  }

  private void updateMaterials() {
    panel.setOpacityEnabled(opacityApplies());
    panel.setColorRangeEnabled(colorsShowIntensity());
    final SimpleColorPalette palette = controls.palettes().getValue();
    if (scale == null || palette == null) {
      return;
    }
    for (final IntensityMapSeries value : series) {
      states.get(value.id()).colorMaximumProperty().set(scale.maximum(value.data()));
    }
    for (final IntensityMapSeriesState state : states.values()) {
      updateMaterial(state);
    }
    final boolean byIntensity = controls.coloring().getValue() == IntensityMapColoring.INTENSITY;
    controls.palettes().setDisable(!byIntensity);
    legendBox.setVisible(byIntensity);
    legendBox.setManaged(byIntensity);
    legend.setImage(materials.texture(palette));
    legendMaximum.setText(
        scale.normalized() ? "100 %" : format.intensity(scale.bounds().maximum()));
  }

  /**
   * @return true if overlays are shaded by a gradient of their color instead of a flat color
   */
  private boolean overlayGradient() {
    // decision (user request): imaging overlays shade their color by intensity, because pixel
    // heights are compressed (log, minimum height) and a flat color hides intensity differences;
    // the 2D view has no heights at all
    return controls.coloring().getValue() != IntensityMapColoring.INTENSITY && !series.isEmpty()
        && (flat || series.getFirst().data().pixels());
  }

  /**
   * @return true if colors show intensities, so their range can be clipped
   */
  private boolean colorsShowIntensity() {
    return controls.coloring().getValue() == IntensityMapColoring.INTENSITY || overlayGradient();
  }

  private void updateMaterial(@NotNull final IntensityMapSeriesState state) {
    final SimpleColorPalette palette = controls.palettes().getValue();
    if (palette == null) {
      return;
    }
    final boolean byIntensity = controls.coloring().getValue() == IntensityMapColoring.INTENSITY;
    materials.apply(state, byIntensity ? palette : null, overlayGradient(), plotBackground(),
        opacityApplies() ? state.opacityProperty().get() : 1);
  }

  private void resetView() {
    focusedTile = -1;
    camera.resetAngles();
    applyPreset();
  }

  private void topView() {
    camera.setAngles(90, 0);
    applyPreset();
  }

  private void frontView() {
    camera.setAngles(0, 0);
    applyPreset();
  }

  private void sideView() {
    camera.setAngles(0, 90);
    applyPreset();
  }

  private void applyPreset() {
    camera.resetPan();
    camera.setAutoFit(true);
    hideHover();
    fitView();
    projectAxes();
    requestDetail();
  }

  private void projectAxes() {
    tileLayout.project(camera.showsIntensity(), controls.showLabels().isSelected(), states);
  }

  private @NotNull List<IntensityMapAxes> allAxes() {
    return tileLayout.allAxes();
  }

  private void fitView() {
    if (viewport.getHeight() <= 0 || viewport.getWidth() <= 0) {
      return;
    }
    // the fit places the axes at the data edges and defines the plot area of the 2D view
    resetAxesFrame();
    final List<Point3D> corners = new ArrayList<>();
    final List<Point3D> focusedPoints =
        controls.layout().get() == IntensityMapLayout.GRID ? tileLayout.tileFittingPoints(
            focusedTile, states) : null;
    final boolean focused = focusedPoints != null;
    if (focused) {
      for (final Point3D point : focusedPoints) {
        corners.add(model.localToParent(point));
      }
    } else {
      focusedTile = -1;
      for (final Point3D point : fittingPoints) {
        corners.add(model.localToParent(point));
      }
    }
    for (final IntensityMapAxes value : focused ? List.of(tileLayout.tileAxes(focusedTile))
        : allAxes()) {
      value.setIntensityVisible(camera.showsIntensity());
      // without labels no room is reserved for text
      for (final Point3D point : controls.showLabels().isSelected() ? value.fittingPoints()
          : List.<Point3D>of()) {
        corners.add(model.localToParent(value.geometry().localToParent(point)));
      }
    }
    camera.fit(corners);
    if (flat) {
      plotArea.update(plotTile());
    }
  }

  private void resetAxesFrame() {
    final IntensityMapAxesSpec spec = axesSpec;
    lineScale = 1;
    if (spec != null && spec.frame() != null) {
      axesSpec = spec.withFrame(null);
      allAxes().forEach(value -> value.rebuild(axesSpec));
    }
  }

  /**
   * @return the tile with a fixed plot area in the 2D view: the only tile or the focused one. Side
   * by side tiles without focus keep their axes at the data edges, like in 3D.
   */
  private @Nullable IntensityMapTile plotTile() {
    if (!flat) {
      return null;
    }
    final List<IntensityMapTile> tiles = tileLayout.tiles();
    if (focusedTile >= 0 && focusedTile < tiles.size()) {
      return tiles.get(focusedTile);
    }
    return tiles.size() == 1 ? tiles.getFirst() : null;
  }

  private void saveImage() {
    if (getScene() == null || getScene().getWindow() == null) {
      return;
    }
    final FileChooser chooser = FxFileChooser.newFileChooser(
        List.of(new FileChooser.ExtensionFilter("PNG image", "*.png")), null,
        flat ? "Save 2D view" : "Save 3D view");
    chooser.setInitialFileName(flat ? "2d-view.png" : "3d-view.png");
    final File file = chooser.showSaveDialog(getScene().getWindow());
    if (file == null) {
      return;
    }
    hideHover();
    final SnapshotParameters parameters = new SnapshotParameters();
    // render at display resolution or better for crisp figures
    final double factor = Math.max(2, getScene().getWindow().getOutputScaleX());
    parameters.setTransform(Transform.scale(factor, factor));
    parameters.setFill(scene.getFill());
    final WritableImage image = viewport.snapshot(parameters, null);
    try {
      ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
      setStatus("Saved " + file.getName());
    } catch (final IOException ex) {
      logger.log(Level.WARNING, "Cannot save 3D view", ex);
      setStatus("Cannot save image: " + ex.getMessage());
    }
  }

  @Override
  public void close() {
    closed = true;
    meshBuilder.close();
  }
}
