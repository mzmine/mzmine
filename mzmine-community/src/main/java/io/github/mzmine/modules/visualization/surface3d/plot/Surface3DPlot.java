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

package io.github.mzmine.modules.visualization.surface3d.plot;

import static io.github.mzmine.modules.visualization.surface3d.render.Surface3DMesh.DEPTH;
import static io.github.mzmine.modules.visualization.surface3d.render.Surface3DMesh.WIDTH;

import com.google.common.collect.Range;
import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.colorpicker.ColorPickerMenuItem;
import io.github.mzmine.gui.preferences.ImageNormalization;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.gui.preferences.UnitFormat;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxIconButtonBuilder;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxPopOvers;
import io.github.mzmine.javafx.components.factories.MenuItems;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxFileChooser;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DAxisKind;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DBounds;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DData;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DDetail;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DRegion;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DSelection;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DSeries;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DSmoothing;
import io.github.mzmine.modules.visualization.surface3d.render.Surface3DMesh;
import io.github.mzmine.modules.visualization.surface3d.render.Surface3DPicker;
import io.github.mzmine.modules.visualization.surface3d.render.Surface3DScale;
import io.github.mzmine.modules.visualization.surface3d.render.Surface3DScreenGeometry;
import io.github.mzmine.modules.visualization.surface3d.render.Surface3DTile;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Transform;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javax.imageio.ImageIO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reusable, interactive 3D plot. All samples use the same physical axes and, unless normalized,
 * the same intensity scale.
 */
public final class Surface3DPlot extends BorderPane implements AutoCloseable {

  private static final Logger logger = Logger.getLogger(Surface3DPlot.class.getName());
  private static final double DEFAULT_TILT = 38;
  // sample points per axis to find the visible data window
  private static final int VISIBLE_STEPS = 24;
  // closest and farthest camera distance to the viewed point: fitted views are ~1250 away, so
  // zooming in reaches more than 10000 times (user request)
  private static final double MIN_DISTANCE = 0.05;
  private static final double MAX_DISTANCE = 4000;
  private static final double MIN_BOX_PIXELS = 3;
  private static final double DEFAULT_TURN = -32;
  private static final int FIT_DIVISIONS = 12;
  private static final int MAX_HOVER_ROWS = 8;
  // highlight of markers and selections, also used by the detail panes
  public static final Color ACCENT = Color.web("#f97316");

  private final Surface3DAxes axes = new Surface3DAxes();
  private final Group surfaces = new Group();
  private final Group markers = new Group();
  private final Group slices = new Group();
  // axes of the second and following side by side tiles
  private final Group extraAxesGroup = new Group();
  private final List<Surface3DAxes> extraAxes = new ArrayList<>();
  private final Pane labelLayer = new Pane();
  private final Pane titleLayer = new Pane();
  private final Spinner<Integer> gridColumns = new Spinner<>(0, 20, 0);
  private final Spinner<Integer> gridRows = new Spinner<>(0, 20, 0);
  private @Nullable Surface3DAxesSpec axesSpec;
  // scale of marker line widths, follows the zoom like the axes
  private double lineScale = 1;
  // 2D view: margins of the fitted plot area to the viewport edges, so the area follows resizing
  private @Nullable Insets plotInsets;

  private final Group model = new Group(axes.geometry(), extraAxesGroup, surfaces, markers,
      slices);
  private final Rotate tilt = new Rotate(DEFAULT_TILT, Rotate.X_AXIS);
  private final Rotate turn = new Rotate(DEFAULT_TURN, Rotate.Y_AXIS);
  private final Scale heightScale = new Scale(1, 1, 1, 0, 0, 0);
  private final PerspectiveCamera camera = new PerspectiveCamera(true);
  private final SubScene scene;
  private final StackPane viewport = new StackPane();
  private final Surface3DOverlayPanel panel = new Surface3DOverlayPanel();
  private final Label status = new Label("Loading data…");
  private final Label legendMinimum = new Label("0");
  private final Label legendMaximum = new Label();
  private final ImageView legend = new ImageView();
  private final HBox legendBox = new HBox(6);
  // same transformations and normalizations as the imaging preferences
  private final ComboBox<PaintScaleTransform> transform = FxComboBox.createComboBox(
      "Transformation of the height; colors stay linear", PaintScaleTransform.values(), null);
  private final ComboBox<ImageNormalization> intensityNormalization = FxComboBox.createComboBox(
      "Normalize every scan or pixel to the average TIC, as for images. Reads the data again.",
      ImageNormalization.values(), null);
  private final CheckBox normalized = new CheckBox("Each overlay to its maximum");
  private final CheckBox fromLowest = new CheckBox("Start at the lowest pixel");
  private @Nullable Consumer<ImageNormalization> normalizationListener;
  private boolean updatingNormalization;
  private final ComboBox<Surface3DColoring> coloring = FxComboBox.createComboBox(
      "Color surfaces by intensity or by overlay color", Surface3DColoring.values(), null);
  private final ComboBox<SimpleColorPalette> palettes = new ComboBox<>();
  private final VBox hoverBox = new VBox(3);
  private final HBox loadingBox = new HBox(8);
  private final ProgressIndicator loadingIndicator = new ProgressIndicator();
  private final Label loadingLabel = new Label();
  private final Label placeholder = FxLabels.newItalicLabel("No data");
  private final Box dropLine = new Box(0.9, 1, 0.9);
  private final Box crossX = new Box(WIDTH, 0.35, 0.35);
  private final Box crossZ = new Box(0.35, 0.35, DEPTH);
  private final Box regionBox = new Box(1, 0.6, 1);
  private final PhongMaterial sliceMaterial = new PhongMaterial(ACCENT.deriveColor(0, 1, 1, 0.06));
  private final PhongMaterial accentMaterial = new PhongMaterial(ACCENT);
  private final Button frontButton = new Button();
  private final Button sideButton = new Button();
  private final Tooltip frontTooltip = new Tooltip();
  private final Tooltip sideTooltip = new Tooltip();
  private final ObjectProperty<Surface3DLayout> layout = new SimpleObjectProperty<>(
      Surface3DLayout.OVERLAY);
  private final GridPane displayGrid = new GridPane(8, 8);
  private @Nullable Slider heightSlider;
  // decision: 0.1 % hides the noise carpet of typical overlays while keeping real signals
  private static final double DEFAULT_NOISE_PERCENT = 0.1;
  // initial opacity of overlaid surfaces
  private static final double OVERLAY_OPACITY = 0.8;
  private final Slider noiseFloor = new Slider(0, 20, DEFAULT_NOISE_PERCENT);
  private final TextField noiseField = new TextField();
  private final List<Label> tileLabels = new ArrayList<>();
  private final ToggleButton showLabels = FxIconButtonBuilder.ofToggleIconButton("bi-fonts")
      .build();
  private final ToggleButton showGrid = FxIconButtonBuilder.ofToggleIconButton("bi-border-all")
      .build();
  // plot background below the data, null follows the theme
  private @Nullable Color plotBackground;
  private List<Surface3DTile> tiles = List.of(Surface3DTile.IDENTITY);
  private List<Surface3DSeries> tiled = List.of();
  private boolean layoutChosen;
  private Surface3DTile regionTile = Surface3DTile.IDENTITY;
  // collapsible sections instead of show/hide buttons (user request)
  private final Surface3DSection detailSection = new Surface3DSection();
  private final BorderPane collapsedDetail = new BorderPane();
  private double detailDivider = 0.62;
  private final SplitPane split = new SplitPane();
  private final ExecutorService builders;
  private final AtomicInteger generation = new AtomicInteger();
  private final Map<String, Surface3DSeriesState> states = new LinkedHashMap<>();
  private final Map<SimpleColorPalette, WritableImage> textures = new HashMap<>();
  // intensity gradients of single overlay colors from the background, by color
  private final Map<Color, WritableImage> gradients = new HashMap<>();
  // input overlays, and the displayed overlays after smoothing with the same ids
  private List<Surface3DSeries> rawSeries = List.of();
  private List<Surface3DSeries> series = List.of();
  private final Spinner<Integer> smoothingRadius = new Spinner<>(0, 20, 0);
  private final ComboBox<Surface3DSmoothing.Axes> smoothingAxes = FxComboBox.createComboBox(
      "Smooth along both axes or one, e.g. only retention time to keep m/z separated",
      Surface3DSmoothing.Axes.values(), null);
  private final Map<String, SmoothedData> smoothed = new HashMap<>();
  private final AtomicInteger smoothingGeneration = new AtomicInteger();
  private @Nullable Surface3DBounds bounds;
  private @Nullable Surface3DScale scale;
  private @Nullable Consumer<Surface3DDetail> detailListener;
  private @Nullable Consumer<Surface3DSelection> selectionListener;
  private @Nullable BiConsumer<String, Color> colorListener;
  private @Nullable Surface3DSelection selection;
  private Surface3DSliceMode sliceMode = Surface3DSliceMode.POINT;
  private Surface3DFormat format = Surface3DFormat.PLAIN;
  // decision: without preferences, units follow the mzmine default style, e.g. "Intensity / %"
  private UnitFormat unitFormat = UnitFormat.DIVIDE;
  private @Nullable Node detailPane;
  private @Nullable Point3D regionStart;
  private @Nullable Point3D regionEnd;
  private boolean closed;
  private boolean dark;
  private double dragX;
  private double dragY;
  private boolean dragged;
  private final boolean flat;
  private boolean autoFit = true;
  // side by side tile the camera is fitted to, -1 fits all
  private int focusedTile = -1;
  private List<Point3D> fittingPoints = List.of();

  public Surface3DPlot(@NotNull final SimpleColorPalette palette) {
    this(palette, false);
  }

  /**
   * @param flat 2D view: a fixed top view of flat geometry colored by intensity, without heights,
   *             rotation, or lighting, and without the controls that only apply in 3D
   */
  public Surface3DPlot(@NotNull final SimpleColorPalette palette, final boolean flat) {
    this.flat = flat;
    panel.setIntensityFormat(format::intensity);
    final int threads = Math.clamp(Runtime.getRuntime().availableProcessors() - 1, 1, 4);
    final ThreadPoolExecutor pool = new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(), runnable -> {
      final Thread thread = new Thread(runnable, "3D surface geometry");
      thread.setDaemon(true);
      return thread;
    });
    // idle threads end, also when a plot is discarded without close()
    pool.allowCoreThreadTimeOut(true);
    builders = pool;
    model.getTransforms().addAll(tilt, turn, heightScale);
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
      // a plane facing the camera projects without perspective distortion, like a 2D image
      tilt.setAngle(90);
      turn.setAngle(0);
      // decision: coplanar floor, grid, data, and markers draw in scene order, a depth test
      // needs lifted layers, which drift apart from the axes at deep zoom
      model.setDepthTest(DepthTest.DISABLE);
    }
    scene = new SubScene(world, 900, 650, true, SceneAntialiasing.BALANCED);
    camera.setNearClip(10);
    camera.setFarClip(10000);
    camera.setTranslateZ(-1250);
    scene.setCamera(camera);
    surfaces.setMouseTransparent(true);
    initMarkers();

    final Pane overlay = new Pane(hoverBox);
    overlay.setMouseTransparent(true);
    overlay.setPickOnBounds(false);
    hoverBox.setManaged(false);
    hoverBox.setVisible(false);
    loadingIndicator.setPrefSize(18, 18);
    loadingBox.getChildren().setAll(loadingIndicator, loadingLabel);
    loadingBox.setAlignment(Pos.CENTER_LEFT);
    loadingBox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    loadingBox.setMouseTransparent(true);
    loadingBox.setVisible(false);
    StackPane.setAlignment(loadingBox, Pos.TOP_LEFT);
    StackPane.setMargin(loadingBox, new Insets(12));
    placeholder.setMouseTransparent(true);
    // one layer for all axis labels and titles, so their bounds share coordinates
    labelLayer.setMouseTransparent(true);
    labelLayer.setPickOnBounds(false);
    titleLayer.setMouseTransparent(true);
    labelLayer.getChildren().setAll(axes.labels(), titleLayer);
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

    setTop(createToolbar(palette));
    setRight(new Surface3DSideDrawer("Overlays", panel));
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

  private @NotNull Region createToolbar(@NotNull final SimpleColorPalette palette) {
    final Slider height = new Slider(0.2, 3, 1);
    heightSlider = height;
    height.setPrefWidth(100);
    height.setTooltip(new Tooltip("Vertical exaggeration; intensities retain their scale"));
    height.valueProperty().addListener((_, _, value) -> {
      heightScale.setY(value.doubleValue());
      if (autoFit) {
        fitView();
      }
      projectAxes();
    });
    transform.setValue(PaintScaleTransform.LINEAR);
    transform.valueProperty().addListener((_, _, _) -> rebuild());
    intensityNormalization.setValue(ImageNormalization.NO_NORMALIZATION);
    intensityNormalization.valueProperty().addListener((_, _, value) -> {
      if (!updatingNormalization && normalizationListener != null && value != null) {
        normalizationListener.accept(value);
      }
    });
    normalized.setTooltip(new Tooltip(
        "Scale every overlay to its own maximum, e.g. to compare ions of different abundance"));
    normalized.selectedProperty().addListener((_, _, _) -> rebuild());
    fromLowest.setTooltip(new Tooltip(
        "The weakest shown pixel starts at the floor, so heights show differences instead of a tall base"));
    fromLowest.selectedProperty().addListener((_, _, _) -> rebuild());
    coloring.setValue(Surface3DColoring.INTENSITY);
    coloring.valueProperty().addListener((_, _, _) -> updateMaterials());
    palettes.getItems().setAll(SimpleColorPalette.DEFAULT_PAINT_SCALES);
    if (!palettes.getItems().contains(palette)) {
      palettes.getItems().add(palette);
    }
    palettes.setConverter(new StringConverter<>() {
      @Override
      public @NotNull String toString(@Nullable final SimpleColorPalette value) {
        return value == null ? "" : value.getName();
      }

      @Override
      public @Nullable SimpleColorPalette fromString(@NotNull final String value) {
        return palettes.getValue();
      }
    });
    palettes.setValue(palette);
    palettes.setPrefWidth(150);
    palettes.valueProperty().addListener((_, _, _) -> updateMaterials());

    // decision: rarely changed settings live in a menu and a popover to keep one compact row
    final HBox views = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 2,
        viewButton("3d", "bi-box", "3D perspective that fits all data (R, double-click)",
            this::resetView),
        viewButton("top", "bi-grid-3x3", "Top view onto the coordinate plane (T)", this::topView),
        frontButton, sideButton);
    // the 2D view has one fixed camera
    views.setVisible(!flat);
    views.setManaged(!flat);
    frontButton.setId("surface3d-view-front");
    frontButton.setOnAction(_ -> frontView());
    sideButton.setId("surface3d-view-side");
    sideButton.setOnAction(_ -> sideView());
    frontButton.getStyleClass().add("icon-button");
    sideButton.getStyleClass().add("icon-button");
    frontButton.setTooltip(frontTooltip);
    sideButton.setTooltip(sideTooltip);
    FxLayout.bindManagedToVisible(frontButton);
    FxLayout.bindManagedToVisible(sideButton);
    displayGrid.setPadding(new Insets(10));
    smoothingRadius.setEditable(true);
    smoothingRadius.setPrefWidth(70);
    smoothingRadius.setTooltip(new Tooltip(
        "Gaussian smoothing over this many neighboring data points, 0 is off"));
    smoothingRadius.valueProperty().addListener((_, _, _) -> refreshDisplayed());
    smoothingAxes.setValue(Surface3DSmoothing.Axes.BOTH);
    smoothingAxes.valueProperty().addListener((_, _, _) -> refreshDisplayed());
    for (final Spinner<Integer> spinner : List.of(gridColumns, gridRows)) {
      spinner.setEditable(true);
      spinner.setPrefWidth(70);
      // 0 selects an automatic, near square grid
      spinner.getValueFactory().setConverter(new StringConverter<>() {
        @Override
        public @NotNull String toString(@Nullable final Integer value) {
          return value == null || value <= 0 ? "Auto" : value.toString();
        }

        @Override
        public @NotNull Integer fromString(@Nullable final String text) {
          try {
            return text == null || text.isBlank() || text.trim().equalsIgnoreCase("auto") ? 0
                : Math.max(0, Integer.parseInt(text.trim()));
          } catch (final NumberFormatException ex) {
            return 0;
          }
        }
      });
      spinner.valueProperty().addListener((_, _, _) -> {
        if (layout.getValue() == Surface3DLayout.GRID) {
          rebuild();
        }
      });
    }
    gridColumns.setTooltip(new Tooltip("Images per row in side by side, Auto for a square grid"));
    gridRows.setTooltip(new Tooltip("Rows in side by side, Auto to fit all images"));
    showLabels.setSelected(true);
    showLabels.setTooltip(new Tooltip(
        "Show tick labels, axis titles, and tile titles; hidden labels free space for the data"));
    showGrid.setSelected(true);
    showGrid.setTooltip(new Tooltip("Show grid lines"));
    showGrid.selectedProperty().addListener(
        (_, _, show) -> allAxes().forEach(value -> value.setGridVisible(show)));
    showLabels.selectedProperty().addListener((_, _, show) -> {
      labelLayer.setVisible(show);
      if (autoFit) {
        fitView();
      }
      projectAxes();
    });
    height.setPrefWidth(150);
    final Button display = FxButtons.createButton("Display",
        "Height, log scale, normalization, and colors", () -> {
        });
    FxPopOvers.install(display, FxPopOvers.newPopOver(displayGrid));
    final Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    final Button save = FxButtons.createButton(null, FxIcons.SAVE, "Save the current view as PNG",
        this::saveImage);
    final Label help = new Label(null, FxIconUtil.getFontIcon(FxIcons.QUESTION_CIRCLE));
    final String common = """
        Mouse wheel or pinch: zoom at the cursor
        Side by side: double-click a tile to fit it, double-click elsewhere for all
        Click: show the spectrum at this position
        Spectrum: click a signal to show its m/z, Ctrl/⌘ + click to add or remove m/z,
        Ctrl/⌘ + drag to add an m/z window
        """;
    final Tooltip helpTip = new Tooltip(flat ? """
        Drag, arrow keys, or two-finger scroll: pan
        """ + common + """
        Ctrl/⌘ + drag: zoom to a box
        Double-click or R: fit view · +/−: zoom""" : """
        Drag: rotate · Shift/right-drag or two-finger scroll: pan
        """ + common + """
        Ctrl/⌘ + drag on the floor: zoom to a box
        Double-click or R: fit view · T/F/S: top, front, side
        Arrow keys: rotate · +/−: zoom""");
    helpTip.setShowDelay(Duration.millis(150));
    help.setTooltip(helpTip);

    final MenuButton viewMode = new MenuButton("View mode",
        FxIconUtil.getFontIcon("bi-layers", 15));
    viewMode.setTooltip(new Tooltip("""
        Overlay: all overlays at their coordinates
        Side by side: small multiples with linked rotation and zoom"""));
    final ToggleGroup modes = new ToggleGroup();
    for (final Surface3DLayout value : Surface3DLayout.values()) {
      final RadioMenuItem item = new RadioMenuItem(value.toString());
      item.setToggleGroup(modes);
      item.setUserData(value);
      item.setSelected(value == layout.get());
      item.setOnAction(_ -> {
        layoutChosen = true;
        layout.set(value);
      });
      viewMode.getItems().add(item);
    }
    layout.addListener((_, _, value) -> {
      modes.getToggles().forEach(toggle -> toggle.setSelected(toggle.getUserData() == value));
      if (value == Surface3DLayout.OVERLAY && series.size() > 1) {
        coloring.setValue(Surface3DColoring.SAMPLE);
      }
      // decision: tiles change their size and place, so the zoom of the former layout is lost
      autoFit = true;
      focusedTile = -1;
      updateDisplayControls();
      rebuild();
    });
    noiseFloor.setPrefWidth(70);
    final String noiseTip =
        "Hide signals below this percentage of the maximum, e.g. to remove the noise carpet";
    noiseFloor.setTooltip(new Tooltip(noiseTip));
    final Label noiseLabel = new Label("Noise");
    noiseLabel.setMinWidth(Region.USE_PREF_SIZE);
    noiseField.setPrefColumnCount(3);
    noiseField.setTooltip(new Tooltip(noiseTip + " (0–100)"));
    noiseField.setText(formatPercent(noiseFloor.getValue()));
    noiseField.setOnAction(_ -> applyNoiseField());
    noiseField.focusedProperty().addListener((_, _, focused) -> {
      if (!focused) {
        applyNoiseField();
      }
    });
    final Label percent = new Label("%");
    percent.setMinWidth(Region.USE_PREF_SIZE);
    noiseFloor.valueProperty().addListener((_, _, value) -> {
      if (!noiseField.isFocused()) {
        noiseField.setText(formatPercent(value.doubleValue()));
      }
      if (!noiseFloor.isValueChanging()) {
        rebuild();
      }
    });
    noiseFloor.valueChangingProperty().addListener((_, _, changing) -> {
      if (!changing) {
        rebuild();
      }
    });
    final HBox noise = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 4, noiseLabel, noiseFloor,
        noiseField, percent);
    updateDisplayControls();
    // decision (user request): styling of the view next to saving it, top right
    final HBox style = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 2,
        createPlotBackgroundButton(), showGrid, showLabels);
    final HBox toolbar = FxLayout.newHBox(Pos.CENTER_LEFT, new Insets(4, 8, 4, 8), 8, views,
        viewMode, display, new Separator(Orientation.VERTICAL), noise, spacer, style,
        new Separator(Orientation.VERTICAL), save, help);
    return toolbar;
  }

  private static @NotNull Button viewButton(@NotNull final String id, @NotNull final String icon,
      @NotNull final String tooltip, @NotNull final Runnable action) {
    final Button button = FxButtons.graphicButton(FxIconUtil.getFontIcon(icon, 16), tooltip,
        _ -> action.run());
    button.setId("surface3d-view-" + id);
    return button;
  }

  /**
   * Shows only the display options that apply to the data and view mode: imaging
   * transformations and normalization for images, a plain log option otherwise, overlay options
   * with several overlays, and grid options for side by side.
   */
  private void updateDisplayControls() {
    final List<Surface3DSeries> current = series.isEmpty() ? rawSeries : series;
    final boolean imaging = !current.isEmpty() && current.getFirst().data().pixels();
    updateProfileButtons(current, imaging);
    final boolean several = rawSeries.size() > 1;
    final Surface3DLayout mode = layout.get();
    displayGrid.getChildren().clear();
    int row = 0;
    if (heightSlider != null && !flat) {
      displayGrid.addRow(row++, new Label("Height"), heightSlider);
    }
    // decision (user request): logarithmic heights only make sense for images
    if (!imaging && !current.isEmpty() && transform.getValue() != PaintScaleTransform.LINEAR) {
      transform.setValue(PaintScaleTransform.LINEAR);
    }
    // transformation and baseline change heights only, which the 2D view does not have
    if (imaging && !flat) {
      displayGrid.addRow(row++, new Label("Transform"), transform);
      displayGrid.addRow(row++, new Label("Heights"), fromLowest);
    }
    if (imaging) {
      if (normalizationListener != null) {
        displayGrid.addRow(row++, new Label("Normalize"), intensityNormalization);
      }
    }
    if (several) {
      displayGrid.addRow(row++, new Label("Overlays"), normalized);
    }
    if (several) {
      displayGrid.addRow(row++, new Label("Color by"), coloring);
    }
    displayGrid.addRow(row++, new Label("Paint scale"), palettes);
    displayGrid.addRow(row++, new Label("Smoothing"),
        FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 6, smoothingRadius, smoothingAxes));
    if (mode == Surface3DLayout.GRID) {
      displayGrid.addRow(row, new Label("Tiles"),
          FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 6, new Label("Columns"), gridColumns,
              new Label("Rows"), gridRows));
    }
  }

  /**
   * @param value vertical exaggeration, clamped to the slider range
   */
  public void setHeightScale(final double value) {
    if (heightSlider != null) {
      heightSlider.setValue(Math.clamp(value, heightSlider.getMin(), heightSlider.getMax()));
    }
  }

  public double minimumHeightScale() {
    return heightSlider == null ? 1 : heightSlider.getMin();
  }

  /**
   * Profile views match the axes: a peak for retention time or mobility, a spectrum for m/z.
   * Images have no meaningful profile, so both are hidden.
   */
  private void updateProfileButtons(@NotNull final List<Surface3DSeries> current,
      final boolean imaging) {
    frontButton.setVisible(!imaging && !flat);
    sideButton.setVisible(!imaging && !flat);
    final Surface3DData first = current.isEmpty() ? null : current.getFirst().data();
    final Surface3DAxisKind x = first == null ? Surface3DAxisKind.RETENTION_TIME
        : first.xKind();
    final Surface3DAxisKind y = first == null ? Surface3DAxisKind.MZ
        : first.yKind();
    frontButton.setGraphic(x == Surface3DAxisKind.MZ ? Surface3DIcons.spectrum()
        : Surface3DIcons.peak());
    sideButton.setGraphic(y == Surface3DAxisKind.MZ ? Surface3DIcons.spectrum()
        : Surface3DIcons.peak());
    frontTooltip.setText("Front view: " + profileName(x, first == null ? "x" : first.xLabel())
        + " (F)");
    sideTooltip.setText("Side view: " + profileName(y, first == null ? "y" : first.yLabel())
        + " (S)");
  }

  private static @NotNull String profileName(@NotNull final Surface3DAxisKind kind,
      @NotNull final String axis) {
    return switch (kind) {
      case MZ -> "spectrum along " + axis;
      case RETENTION_TIME -> "chromatogram along " + axis;
      case MOBILITY -> "mobilogram along " + axis;
      case OTHER -> "profile along " + axis;
    };
  }

  private static @NotNull String formatPercent(final double value) {
    return String.format(Locale.ROOT, value < 1 ? "%.2f" : "%.1f", value)
        .replaceAll("0+$", "").replaceAll("\\.$", "");
  }

  /**
   * Values beyond the slider range are allowed in the field, the slider then shows its maximum.
   */
  private void applyNoiseField() {
    try {
      final double value = Math.clamp(
          Double.parseDouble(noiseField.getText().trim().replace(',', '.')), 0, 100);
      noiseField.setText(formatPercent(value));
      if (value != noiseFloor.getValue()) {
        noiseFloor.setMax(Math.max(20, value));
        noiseFloor.setValue(value);
      }
    } catch (final NumberFormatException ex) {
      noiseField.setText(formatPercent(noiseFloor.getValue()));
    }
  }

  private void initMarkers() {
    dropLine.setMaterial(accentMaterial);
    crossX.setMaterial(accentMaterial);
    crossZ.setMaterial(accentMaterial);
    regionBox.setMaterial(new PhongMaterial(Color.rgb(59, 130, 246, 0.35)));
    // decision: a faint unlit plane plus a crisp floor line, specular highlights washed it out
    sliceMaterial.setSpecularColor(Color.TRANSPARENT);
    markers.getChildren().setAll(dropLine, crossX, crossZ, regionBox);
    markers.setMouseTransparent(true);
    slices.setMouseTransparent(true);
    extraAxesGroup.setMouseTransparent(true);
    markers.getChildren().forEach(node -> node.setVisible(false));
  }

  private void initInteraction() {
    scene.setOnMousePressed(event -> {
      viewport.requestFocus();
      dragX = event.getSceneX();
      dragY = event.getSceneY();
      dragged = false;
      // Ctrl/⌘ + drag zooms to a box on the floor
      if (event.isShortcutDown() && event.getButton() == MouseButton.PRIMARY && bounds != null) {
        regionTile = tileAtFloor(ray(event));
        markers.getTransforms().setAll(regionTile.transforms());
        regionStart = Surface3DPicker.floor(regionTile.toLocal(ray(event)), true);
        regionEnd = regionStart;
      }
      hideHover();
    });
    scene.setOnMouseDragged(event -> {
      dragged = true;
      if (regionStart != null) {
        regionEnd = Surface3DPicker.floor(regionTile.toLocal(ray(event)), true);
        updateRegionBox();
        return;
      }
      autoFit = false;
      final double dx = event.getSceneX() - dragX;
      final double dy = event.getSceneY() - dragY;
      // the 2D view cannot rotate, dragging pans
      if (flat || event.isShiftDown() || event.getButton() == MouseButton.SECONDARY
          || event.getButton() == MouseButton.MIDDLE) {
        final double factor = -camera.getTranslateZ() / 1050;
        model.setTranslateX(model.getTranslateX() + dx * factor);
        model.setTranslateY(model.getTranslateY() + dy * factor);
      } else {
        turn.setAngle(turn.getAngle() + dx * 0.35);
        tilt.setAngle(Math.clamp(tilt.getAngle() + dy * 0.35, 0, 90));
      }
      dragX = event.getSceneX();
      dragY = event.getSceneY();
      // the axes stay at the view edges while dragging
      updateAxesFrame();
      projectAxes();
    });
    scene.setOnMouseReleased(event -> {
      if (regionStart != null) {
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
      final boolean trackpad = event.getTouchCount() > 0 || event.isInertia()
          || event.getDeltaX() != 0;
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
          if (frontButton.isVisible()) {
            frontView();
          }
        }
        case S -> {
          if (sideButton.isVisible()) {
            sideView();
          }
        }
        case LEFT -> rotate(-5, 0);
        case RIGHT -> rotate(5, 0);
        case UP -> rotate(0, 5);
        case DOWN -> rotate(0, -5);
        case PLUS, EQUALS, ADD -> zoom(1.15);
        case MINUS, SUBTRACT -> zoom(1 / 1.15);
        case ESCAPE -> cancelRegion();
        // decision: other keys stay with the scene, e.g. for tab shortcuts
        default -> {
          return;
        }
      }
      event.consume();
    });
  }

  private @NotNull Surface3DPicker.Ray ray(@NotNull final MouseEvent event) {
    return Surface3DPicker.ray(camera, scene.getWidth(), scene.getHeight(), event.getX(),
        event.getY(), model);
  }

  private void pan(final double dx, final double dy) {
    autoFit = false;
    final double factor = -camera.getTranslateZ() / 1050;
    model.setTranslateX(model.getTranslateX() + dx * factor);
    model.setTranslateY(model.getTranslateY() + dy * factor);
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
    final Point3D cameraPosition = new Point3D(camera.getTranslateX(), camera.getTranslateY(),
        camera.getTranslateZ());
    final Point3D target = worldPointAt(x, y);
    if (target == null) {
      zoom(factor);
      return;
    }
    // moving the camera along the cursor ray keeps the target under the cursor
    final double distance = Math.clamp((target.getZ() - cameraPosition.getZ()) / factor,
        MIN_DISTANCE, MAX_DISTANCE);
    final double z = target.getZ() - distance;
    final double effective = (cameraPosition.getZ() - target.getZ()) / (z - target.getZ());
    if (!(effective > 0) || !Double.isFinite(effective)) {
      return;
    }
    final Point3D moved = target.add(cameraPosition.subtract(target).multiply(1 / effective));
    autoFit = false;
    camera.setTranslateX(moved.getX());
    camera.setTranslateY(moved.getY());
    camera.setTranslateZ(moved.getZ());
    hideHover();
    requestDetail();
  }

  /**
   * @return the picked surface or floor point under the cursor in world coordinates, or the
   * point at the depth of the plot center
   */
  private @Nullable Point3D worldPointAt(final double x, final double y) {
    final TileHit picked = pick(x, y);
    if (picked != null) {
      final Surface3DPicker.Hit hit = picked.hit();
      return model.localToParent(picked.tile().toModel(new Point3D(hit.x(), hit.y(), hit.z())));
    }
    final double tangent = Math.tan(Math.toRadians(camera.getFieldOfView() / 2));
    final double aspect = scene.getWidth() / scene.getHeight();
    final Point3D direction = new Point3D((2 * x / scene.getWidth() - 1) * tangent * aspect,
        (2 * y / scene.getHeight() - 1) * tangent, 1);
    final Point3D origin = new Point3D(camera.getTranslateX(), camera.getTranslateY(),
        camera.getTranslateZ());
    final double depth = model.localToParent(Point3D.ZERO).getZ();
    final double t = (depth - origin.getZ()) / direction.getZ();
    return t > 0 ? origin.add(direction.multiply(t)) : null;
  }

  /**
   * @return index of the side by side tile under the cursor, -1 if none
   */
  private int tileAt(final double x, final double y) {
    if (layout.get() != Surface3DLayout.GRID || tiles.size() < 2) {
      return -1;
    }
    final TileHit picked = pick(x, y);
    return picked == null ? -1 : tiles.indexOf(picked.tile());
  }

  /**
   * Fits the camera to one tile; the other tiles stay in place around it.
   */
  private void focusTile(final int index) {
    focusedTile = index;
    model.setTranslateX(0);
    model.setTranslateY(0);
    autoFit = true;
    hideHover();
    fitView();
    projectAxes();
    requestDetail();
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
    autoFit = false;
    camera.setTranslateZ(-Math.clamp(-camera.getTranslateZ() / factor, MIN_DISTANCE,
        MAX_DISTANCE));
    hideHover();
    requestDetail();
  }

  /**
   * Near and far clipping planes follow the distance to the viewed point, so that close-ups are
   * not clipped and the depth buffer keeps its precision.
   */
  private void updateClipping() {
    final Point3D center = worldPointAt(scene.getWidth() / 2, scene.getHeight() / 2);
    final double distance = center == null ? -camera.getTranslateZ()
        : Math.max(MIN_DISTANCE, center.getZ() - camera.getTranslateZ());
    camera.setNearClip(Math.clamp(distance * 0.01, MIN_DISTANCE * 0.01, 10));
    camera.setFarClip(Math.max(10000, distance * 100));
  }

  private void rotate(final double turnDelta, final double tiltDelta) {
    if (flat) {
      // arrow keys move the 2D view instead
      pan(-turnDelta * 8, tiltDelta * 8);
      return;
    }
    autoFit = false;
    turn.setAngle(turn.getAngle() + turnDelta);
    tilt.setAngle(Math.clamp(tilt.getAngle() + tiltDelta, 0, 90));
    projectAxes();
  }

  @Override
  protected void layoutChildren() {
    super.layoutChildren();
    projectAxes();
  }

  public void setDetailListener(@Nullable final Consumer<Surface3DDetail> listener) {
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
    transform.setValue(value);
  }

  /**
   * @param value start heights at the lowest shown intensity instead of zero, e.g. for images
   *              whose weakest pixels would otherwise form a tall base
   */
  public void setHeightsFromLowest(final boolean value) {
    fromLowest.setSelected(value);
  }

  /**
   * @return fraction of the maximum at the floor: the weakest pixel of all overlays, at least the
   * noise floor, as pixels below it are not drawn. 0 if heights start at zero.
   */
  private double baseline(@NotNull final Surface3DBounds scaleBounds, final boolean perOverlay,
      final double noise) {
    if (!fromLowest.isSelected()) {
      return 0;
    }
    double lowest = 1;
    for (final Surface3DSeries value : series) {
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
    intensityNormalization.setValue(value);
    updatingNormalization = false;
  }

  /**
   * Replaces the selectable paint scales, for example with all scales of the preferences.
   */
  public void setPaintScales(@NotNull final List<SimpleColorPalette> scales) {
    if (scales.isEmpty()) {
      return;
    }
    final SimpleColorPalette current = palettes.getValue();
    palettes.getItems().setAll(scales);
    if (current != null && !palettes.getItems().contains(current)) {
      palettes.getItems().addFirst(current);
    }
    palettes.setValue(current != null ? current : scales.getFirst());
  }

  public void setPaintScale(@NotNull final SimpleColorPalette scale) {
    if (!palettes.getItems().contains(scale)) {
      palettes.getItems().add(scale);
    }
    palettes.setValue(scale);
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
   * Expanded, the section shares a split pane with the 3D view. Collapsed, only its header
   * remains below the 3D view, which gets the full height.
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
  public void setOnSelectionChanged(@Nullable final Consumer<Surface3DSelection> listener) {
    selectionListener = listener;
  }

  /**
   * Notifies about overlay colors changed by the user, to keep other views consistent.
   */
  public void setOnSeriesColorChanged(
      @Nullable final BiConsumer<String, Color> listener) {
    colorListener = listener;
  }

  public void setSliceMode(@NotNull final Surface3DSliceMode mode) {
    sliceMode = mode;
  }

  public @Nullable Surface3DSelection getSelection() {
    return selection;
  }

  /**
   * Sets the selected position without notifying the listener.
   */
  public void setSelection(@Nullable final Surface3DSelection selection) {
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
    final TileHit picked = pick(x, y);
    if (picked == null) {
      return;
    }
    setSelection(new Surface3DSelection(Surface3DPicker.dataX(scale.bounds(), picked.hit().x()),
        Surface3DPicker.dataY(scale.bounds(), picked.hit().z())));
    selectionListener.accept(selection);
  }

  /**
   * A hit in tile coordinates.
   */
  private record TileHit(@NotNull Surface3DPicker.Hit hit, @NotNull Surface3DTile tile) {

  }

  /**
   * Picks the closest surface along the ray through the sub scene position, over all tiles.
   */
  private @Nullable TileHit pick(final double x, final double y) {
    if (scale == null) {
      return null;
    }
    final Surface3DPicker.Ray ray = Surface3DPicker.ray(camera, scene.getWidth(),
        scene.getHeight(), x, y, model);
    if (tiles.size() == 1) {
      final List<Surface3DPicker.Target> targets = visibleSeries().stream()
          .map(value -> new Surface3DPicker.Target(value, scale)).toList();
      final Surface3DPicker.Hit hit = Surface3DPicker.pick(ray, targets);
      return hit == null ? null : new TileHit(hit, Surface3DTile.IDENTITY);
    }
    // the closest surface wins, floors only if no surface is hit
    TileHit surface = null;
    TileHit floor = null;
    for (int i = 0; i < tiled.size() && i < tiles.size(); i++) {
      final Surface3DTile tile = tiles.get(i);
      final Surface3DPicker.Hit hit = Surface3DPicker.pick(tile.toLocal(ray),
          List.of(new Surface3DPicker.Target(tiled.get(i), scale)));
      if (hit == null) {
        continue;
      }
      if (hit.target() != null && (surface == null || hit.t() < surface.hit().t())) {
        surface = new TileHit(hit, tile);
      } else if (hit.target() == null && (floor == null || hit.t() < floor.hit().t())) {
        floor = new TileHit(hit, tile);
      }
    }
    return surface != null ? surface : floor;
  }

  private @NotNull Surface3DTile tileAtFloor(@NotNull final Surface3DPicker.Ray ray) {
    for (final Surface3DTile tile : tiles) {
      if (Surface3DPicker.floor(tile.toLocal(ray), false) != null) {
        return tile;
      }
    }
    return tiles.getFirst();
  }

  private @NotNull List<Surface3DSeries> visibleSeries() {
    return series.stream().filter(value -> states.get(value.id()).isVisible()).toList();
  }

  /**
   * Shows which spectrum a click would select, only while hovering (user decision: a persistent
   * marker was too heavy).
   *
   * @param selection hovered position, null hides the marker
   */
  private void updateSliceMarker(@Nullable final Surface3DSelection selection) {
    slices.getChildren().clear();
    final Surface3DBounds current = bounds;
    if (selection == null || current == null || selectionListener == null) {
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
      return;
    }
    final double x = Surface3DMesh.localX(current, selection.x());
    final double z = Surface3DMesh.localZ(current, selection.y());
    for (int i = 0; i < tiles.size(); i++) {
      // decision (user request): slices reach the highest data point, and their top edges form
      // a crosshair above the data, visible even over dense pixel columns
      final double top = sliceTop(i);
      final Group marker = new Group();
      if (sliceMode != Surface3DSliceMode.Y) {
        marker.getChildren().addAll(slicePlane(0.6 * lineScale, top, DEPTH, x, 0),
            sliceLine(0.7 * lineScale, DEPTH, x, 0, -top),
            sliceLine(0.5 * lineScale, DEPTH, x, 0, -0.3 * lineScale));
      }
      if (sliceMode != Surface3DSliceMode.X) {
        marker.getChildren().addAll(slicePlane(WIDTH, top, 0.6 * lineScale, 0, z),
            sliceLine(WIDTH, 0.7 * lineScale, 0, z, -top),
            sliceLine(WIDTH, 0.5 * lineScale, 0, z, -0.3 * lineScale));
      }
      marker.getTransforms().setAll(tiles.get(i).transforms());
      slices.getChildren().add(marker);
    }
  }

  /**
   * @return height of the highest data point in the tile, in local units, slightly above the
   * surface so that lines on top stay visible
   */
  private double sliceTop(final int tile) {
    final List<float[]> envelopes = new ArrayList<>();
    if (layout.getValue() == Surface3DLayout.GRID && tile < tiled.size()) {
      final Surface3DSeriesState state = states.get(tiled.get(tile).id());
      if (state != null) {
        envelopes.add(state.envelope());
      }
    } else {
      for (final Surface3DSeries value : visibleSeries()) {
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

  private @NotNull Box slicePlane(final double width, final double height, final double depth,
      final double x, final double z) {
    final Box plane = new Box(width, height, depth);
    plane.setTranslateX(x);
    plane.setTranslateZ(z);
    plane.setTranslateY(-height / 2);
    plane.setMaterial(sliceMaterial);
    return plane;
  }

  private @NotNull Box sliceLine(final double width, final double depth, final double x,
      final double z, final double y) {
    final Box line = new Box(width, 0.5 * lineScale, depth);
    line.setTranslateX(x);
    line.setTranslateZ(z);
    line.setTranslateY(y);
    line.setMaterial(accentMaterial);
    return line;
  }

  /**
   * Uses the mzmine number formats for axes, readouts and the legend.
   */
  public void setNumberFormats(@Nullable final NumberFormats formats) {
    format = new Surface3DFormat(formats);
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
    hoverBox.setStyle(bubble + shape);
    loadingBox.setStyle(bubble + shape);
    loadingLabel.setStyle(dark ? "-fx-text-fill: #e2e8f0;" : "-fx-text-fill: #1e293b;");
    placeholder.setStyle(dark ? "-fx-text-fill: #94a3b8;" : "-fx-text-fill: #64748b;");
    for (final Label label : tileLabels) {
      label.setStyle(tileTitleStyle());
    }
    projectAxes();
  }

  /**
   * @return the plot background below the data: the chosen color or the one of the theme
   */
  private @NotNull Color plotBackground() {
    return plotBackground != null ? plotBackground : Surface3DAxes.defaultFloor(dark);
  }

  private void applyPlotBackground() {
    allAxes().forEach(value -> value.setColors(dark, plotBackground));
    // overlay gradients start at the plot background
    gradients.clear();
    updateMaterials();
  }

  private @NotNull String tileTitleStyle() {
    return (dark ? "-fx-text-fill: #e2e8f0;" : "-fx-text-fill: #1e293b;")
        + "-fx-font-weight: bold; -fx-font-size: 12; -fx-padding: 0;";
  }

  /**
   * decision (user request): only the plot background is chosen here, everything else follows the
   * theme of the mzmine preferences
   */
  private @NotNull MenuButton createPlotBackgroundButton() {
    final MenuButton button = new FxIconButtonBuilder<>(new MenuButton(), "bi-paint-bucket")
        .build();
    button.setTooltip(new Tooltip("Plot background color"));
    final MenuItem theme = MenuItems.create("Default", () -> setPlotBackground(null));
    final MenuItem white = MenuItems.create("White", () -> setPlotBackground(Color.WHITE));
    final MenuItem black = MenuItems.create("Black", () -> setPlotBackground(Color.BLACK));
    button.getItems().setAll(theme, white, black, new SeparatorMenuItem());
    button.setOnShowing(_ -> {
      // decision: created on first use, the picker loads its FXML
      if (button.getItems().size() == 4) {
        try {
          final ColorPickerMenuItem picker = new ColorPickerMenuItem();
          picker.addColorSelectedListener(this::setPlotBackground);
          button.getItems().add(picker);
        } catch (final IOException ex) {
          logger.log(Level.WARNING, "Cannot create the color picker", ex);
        }
      }
    });
    return button;
  }

  /**
   * @param color plot background below the data, null for the one of the theme
   */
  public void setPlotBackground(@Nullable final Color color) {
    plotBackground = color;
    applyPlotBackground();
  }

  public @NotNull Surface3DDetail detail() {
    // decision: the 2D view samples logical pixels, cells of one physical pixel on HiDPI
    // screens still drop out between rows
    final double outputScale =
        !flat && getScene() != null && getScene().getWindow() != null ? getScene().getWindow()
            .getOutputScaleX() : 1;
    return new Surface3DDetail(Math.max(600, viewport.getWidth()) * outputScale,
        Math.max(400, viewport.getHeight()) * outputScale,
        Math.max(1, series.size()), flat);
  }

  private void requestDetail() {
    if (autoFit) {
      fitView();
    }
    updateClipping();
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
      loadingIndicator.setProgress(progress < 0 ? ProgressIndicator.INDETERMINATE_PROGRESS
          : Math.min(1, progress));
    }
  }

  public void setSeries(@NotNull final List<Surface3DSeries> values) {
    if (closed) {
      return;
    }
    if (values.isEmpty()) {
      generation.incrementAndGet();
      smoothingGeneration.incrementAndGet();
      rawSeries = List.of();
      smoothed.clear();
      series = List.of();
      bounds = null;
      scale = null;
      states.clear();
      surfaces.getChildren().clear();
      axes.clear();
      clearExtraAxes();
      fittingPoints = List.of();
      legendBox.setVisible(false);
      legendBox.setManaged(false);
      panel.setSeries(series, states);
      hideHover();
      showEmpty("No overlays");
      setStatus("No overlays. Add m/z values or raw files to begin.");
      return;
    }
    final Surface3DData first = values.getFirst().data();
    for (final Surface3DSeries value : values) {
      if (!first.xLabel().equals(value.data().xLabel()) || !first.yLabel()
          .equals(value.data().yLabel())) {
        throw new IllegalArgumentException(
            "Overlaid samples must use the same coordinate dimensions and units");
      }
    }
    if (rawSeries.size() <= 1 && values.size() > 1 && !layoutChosen) {
      // decision: coincident surfaces fight for depth, small multiples stay readable
      layout.set(Surface3DLayout.GRID);
    }
    rawSeries = List.copyOf(values);
    states.keySet().retainAll(rawSeries.stream().map(Surface3DSeries::id).toList());
    smoothed.keySet().retainAll(states.keySet());
    // while smoothing, displayed overlays may lag behind; removed ones must not be looked up
    series = series.stream().filter(value -> states.containsKey(value.id())).toList();
    generation.incrementAndGet();
    for (final Surface3DSeries value : rawSeries) {
      states.computeIfAbsent(value.id(), _ -> createState(value));
    }
    panel.setSeries(rawSeries, states);
    placeholder.setVisible(false);
    refreshDisplayed();
  }

  private @NotNull Surface3DSmoothing smoothing() {
    final Integer radius = smoothingRadius.getValue();
    return new Surface3DSmoothing(radius == null ? 0 : radius,
        Objects.requireNonNullElse(smoothingAxes.getValue(), Surface3DSmoothing.Axes.BOTH));
  }

  /**
   * Smooths the input overlays in the background if needed, then shows them. Results are cached
   * per overlay, so layout or scale changes do not smooth again.
   */
  private void refreshDisplayed() {
    if (rawSeries.isEmpty() || closed) {
      return;
    }
    final Surface3DSmoothing params = smoothing();
    if (!params.active()) {
      smoothingGeneration.incrementAndGet();
      smoothed.clear();
      showDisplayed(rawSeries);
      return;
    }
    final List<Surface3DSeries> snapshot = rawSeries;
    final List<Surface3DSeries> missing = snapshot.stream().filter(value -> {
      final SmoothedData cached = smoothed.get(value.id());
      return cached == null || cached.source() != value.data() || !cached.params().equals(params);
    }).toList();
    if (missing.isEmpty()) {
      showDisplayed(snapshot.stream().map(value -> new Surface3DSeries(value.id(), value.name(),
          value.description(), smoothed.get(value.id()).result(), value.color())).toList());
      return;
    }
    final int request = smoothingGeneration.incrementAndGet();
    final BooleanSupplier canceled = () -> smoothingGeneration.get() != request || closed;
    setLoading("Smoothing…", -1);
    CompletableFuture.supplyAsync(() -> {
      final Map<String, SmoothedData> results = new HashMap<>();
      for (final Surface3DSeries value : missing) {
        results.put(value.id(),
            new SmoothedData(value.data(), params, params.apply(value.data(), canceled)));
      }
      return results;
    }, builders).whenComplete((results, error) -> Platform.runLater(() -> {
      if (closed || smoothingGeneration.get() != request) {
        return;
      }
      if (error != null) {
        setLoading(null, 0);
        logger.log(Level.WARNING, "Cannot smooth 3D data", error);
        setStatus("Cannot smooth: " + error.getMessage());
        return;
      }
      smoothed.putAll(results);
      refreshDisplayed();
    }));
  }

  private void showDisplayed(@NotNull final List<Surface3DSeries> values) {
    series = values;
    updateDisplayControls();
    bounds = Surface3DBounds.of(series.stream().map(Surface3DSeries::data).toList());
    rebuild();
  }

  private @NotNull Surface3DSeriesState createState(@NotNull final Surface3DSeries value) {
    final Surface3DSeriesState state = new Surface3DSeriesState(value.color(),
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
      if (layout.getValue() == Surface3DLayout.OVERLAY) {
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
    final double noise = noiseFloor.getValue() / 100;
    final Surface3DScale target = flat ? new Surface3DScale(bounds, PaintScaleTransform.LINEAR,
        normalized.isSelected(), noise, 0, true) : new Surface3DScale(bounds,
        Objects.requireNonNullElse(transform.getValue(), PaintScaleTransform.LINEAR),
        normalized.isSelected(), noise, baseline(bounds, normalized.isSelected(), noise), false);
    final int request = generation.incrementAndGet();
    final BooleanSupplier canceled = () -> generation.get() != request || closed;
    final List<Surface3DSeries> snapshot = series;
    final Map<String, CompletableFuture<BuiltMesh>> builds = new LinkedHashMap<>();
    for (final Surface3DSeries value : snapshot) {
      if (states.get(value.id()).needsMesh(value.data(), target)) {
        builds.put(value.id(), CompletableFuture.supplyAsync(
            () -> new BuiltMesh(Surface3DMesh.build(value.data(), target, canceled),
                Surface3DMesh.envelope(value.data(), target, FIT_DIVISIONS, canceled)),
            builders));
      }
    }
    if (builds.isEmpty()) {
      finish(request, snapshot, target, Map.of());
      return;
    }
    setLoading("Building " + builds.size() + (builds.size() == 1 ? " surface…" : " surfaces…"),
        -1);
    CompletableFuture.allOf(builds.values().toArray(CompletableFuture[]::new))
        .whenComplete((_, error) -> Platform.runLater(() -> {
          if (closed || generation.get() != request) {
            return;
          }
          if (error != null) {
            setLoading(null, 0);
            logger.log(Level.WARNING, "Cannot build 3D surface", error);
            setStatus("Cannot build surface: " + error.getMessage());
            return;
          }
          final Map<String, BuiltMesh> meshes = new HashMap<>();
          builds.forEach((id, future) -> meshes.put(id, future.join()));
          finish(request, snapshot, target, meshes);
        }));
  }

  /**
   * Places the overlays according to the layout. Only transforms change, meshes are reused.
   */
  private void applyLayout() {
    final Surface3DLayout value = layout.getValue();
    titleLayer.getChildren().clear();
    tileLabels.clear();
    for (final Surface3DSeriesState state : states.values()) {
      state.view().getTransforms().clear();
      state.view().setTranslateY(0);
    }
    axes.geometry().getTransforms().clear();
    surfaces.getChildren().setAll(series.stream().map(s -> states.get(s.id()).view()).toList());
    if (value == Surface3DLayout.OVERLAY) {
      clearExtraAxes();
      tiles = List.of(Surface3DTile.IDENTITY);
      tiled = List.of();
      // assumption: coincident surfaces are common for replicate samples; a small lift per
      // overlay resolves depth ties consistently instead of flickering stripes
      for (int i = 0; i < series.size(); i++) {
        states.get(series.get(i).id()).view().setTranslateY(-0.3 * i);
      }
      return;
    }
    tiled = visibleSeries();
    tiles = Surface3DTile.grid(tiled.size(), spinnerValue(gridColumns), spinnerValue(gridRows));
    final List<String> titles = Surface3DTileTitles.of(tiled);
    ensureExtraAxes(Math.max(0, tiled.size() - 1));
    for (int i = 0; i < tiled.size(); i++) {
      final Surface3DTile tile = tiles.get(i);
      states.get(tiled.get(i).id()).view().getTransforms().setAll(tile.transforms());
      // every tile has its own axes, calibrated like the first one
      tileAxes(i).geometry().getTransforms().setAll(tile.transforms());
      final Label label = new Label(titles.get(i));
      label.setManaged(false);
      label.setMouseTransparent(true);
      label.setVisible(false);
      label.setStyle(tileTitleStyle());
      titleLayer.getChildren().add(label);
      tileLabels.add(label);
    }
  }

  private void finish(final int request, @NotNull final List<Surface3DSeries> snapshot,
      @NotNull final Surface3DScale target, @NotNull final Map<String, BuiltMesh> meshes) {
    if (closed || generation.get() != request) {
      return;
    }
    for (final Surface3DSeries value : snapshot) {
      final Surface3DSeriesState state = states.get(value.id());
      final BuiltMesh built = meshes.get(value.id());
      if (built != null) {
        state.setMesh(value.data(), target, built.mesh(), built.envelope());
      } else if (!state.needsMesh(value.data(), target)) {
        // same geometry under a new scale instance
        state.updateScale(target);
      }
    }
    scale = target;
    final Surface3DData first = snapshot.getFirst().data();
    final boolean relative = target.normalized();
    // decision: per-overlay log heights have no common numeric scale, so only the title remains
    axesSpec = new Surface3DAxesSpec(target.bounds(), first.xLabel(), first.yLabel(),
        first.xKind(), first.yKind(),
        relative ? unitFormat.format("Relative intensity", "%") : "Intensity",
        relative ? target.logarithmic() ? 0 : 100 : target.bounds().maximum(), target.baseline(),
        target.transform(), relative ? Surface3DFormat.PLAIN : format,
        axesSpec == null ? null : axesSpec.frame());
    axes.rebuild(axesSpec);
    extraAxes.forEach(value -> value.rebuild(axesSpec));
    applyLayout();
    slices.getChildren().clear();
    fittingPoints = fittingPoints(snapshot);
    if (autoFit) {
      fitView();
    }
    updateMaterials();
    setLoading(null, 0);
    updateStatus();
    hideHover();
    requestLayout();
  }

  private void updateStatus() {
    if (series.isEmpty() || scale == null) {
      return;
    }
    long triangles = 0;
    int visible = 0;
    for (final Surface3DSeries value : series) {
      final Surface3DSeriesState state = states.get(value.id());
      if (state.isVisible()) {
        visible++;
        triangles += state.triangles();
      }
    }
    setStatus(String.format(Locale.ROOT, "%d of %d overlays visible · %s triangles · %s", visible,
        series.size(), triangles >= 1_000_000 ? String.format(Locale.ROOT, "%.1f M",
            triangles / 1e6) : String.format(Locale.ROOT, "%.0f k", triangles / 1e3),
        scale.normalized() ? "each overlay normalized" : "shared intensity scale"));
  }

  private void hover(final double x, final double y) {
    if (scale == null || series.isEmpty() || regionStart != null) {
      hideHover();
      return;
    }
    final TileHit picked = pick(x, y);
    if (picked == null) {
      hideHover();
      return;
    }
    final Surface3DPicker.Hit hit = picked.hit();
    markers.getTransforms().setAll(picked.tile().transforms());
    final List<Surface3DSeries> visible = visibleSeries();
    // the emphasized overlay is the hit surface
    final Surface3DSeries emphasized = hit.target() == null ? null : hit.target().series();
    final double dataX = Surface3DPicker.dataX(scale.bounds(), hit.x());
    final double dataY = Surface3DPicker.dataY(scale.bounds(), hit.z());
    updateSliceMarker(new Surface3DSelection(dataX, dataY));
    final double t = lineScale;
    dropLine.setVisible(hit.target() != null && hit.y() < -0.5 * t);
    dropLine.setWidth(0.9 * t);
    dropLine.setDepth(0.9 * t);
    crossX.setHeight(0.35 * t);
    crossX.setDepth(0.35 * t);
    crossZ.setWidth(0.35 * t);
    crossZ.setHeight(0.35 * t);
    if (dropLine.isVisible()) {
      dropLine.setHeight(-hit.y());
      dropLine.setTranslateX(hit.x());
      dropLine.setTranslateY(hit.y() / 2);
      dropLine.setTranslateZ(hit.z());
    }
    crossX.setTranslateZ(hit.z());
    crossX.setTranslateY(0.6);
    crossZ.setTranslateX(hit.x());
    crossZ.setTranslateY(0.6);
    crossX.setVisible(true);
    crossZ.setVisible(true);

    final Surface3DData first = series.getFirst().data();
    final List<Node> rows = new ArrayList<>();
    rows.add(hoverLabel(first.xLabel() + "  " + format.value(dataX, first.xKind()) + "     "
        + first.yLabel() + "  " + format.value(dataY, first.yKind()) + (smoothing().active()
        ? "     smoothed" : ""), true));
    int shown = 0;
    for (final Surface3DSeries value : visible) {
      if (shown == MAX_HOVER_ROWS) {
        rows.add(hoverLabel("+ " + (visible.size() - shown) + " more", false));
        break;
      }
      final double intensity = intensityAt(value.data(), dataX, dataY);
      final String text = value.fullName() + ":  " + (Double.isNaN(intensity) ? "–"
          : format.intensity(intensity));
      final Label label = hoverLabel(text, value == emphasized);
      final Circle swatch = new Circle(4, seriesColor(value));
      rows.add(FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 6, swatch, label));
      shown++;
    }
    hoverBox.getChildren().setAll(rows);
    hoverBox.setVisible(true);
    // new labels need CSS before they can be measured, otherwise they collapse to an ellipsis
    hoverBox.applyCss();
    hoverBox.autosize();
    final double left = x + 18 + hoverBox.getWidth() > viewport.getWidth() ? x - 12
        - hoverBox.getWidth() : x + 18;
    final double top = y + 18 + hoverBox.getHeight() > viewport.getHeight() ? y - 12
        - hoverBox.getHeight() : y + 18;
    hoverBox.relocate(Math.max(4, left), Math.max(4, top));
  }

  private @NotNull Color seriesColor(@NotNull final Surface3DSeries value) {
    return states.get(value.id()).colorProperty().get();
  }

  private @NotNull Label hoverLabel(@NotNull final String text, final boolean bold) {
    final Label label = new Label(text);
    label.setStyle((dark ? "-fx-text-fill: #e2e8f0;" : "-fx-text-fill: #1e293b;") + (bold
        ? "-fx-font-weight: bold;" : ""));
    return label;
  }

  private static double intensityAt(@NotNull final Surface3DData data, final double x,
      final double y) {
    final int column = data.binX(x);
    final int row = data.binY(y);
    if (column < 0 || row < 0 || !data.isPresent(column, row)) {
      return Double.NaN;
    }
    return data.intensity(column, row);
  }

  private void hideHover() {
    slices.getChildren().clear();
    hoverBox.setVisible(false);
    dropLine.setVisible(false);
    crossX.setVisible(false);
    crossZ.setVisible(false);
  }

  private void updateRegionBox() {
    if (regionStart == null || regionEnd == null) {
      regionBox.setVisible(false);
      return;
    }
    final double minimum = 0.5 * lineScale;
    regionBox.setWidth(Math.max(minimum, Math.abs(regionEnd.getX() - regionStart.getX())));
    regionBox.setDepth(Math.max(minimum, Math.abs(regionEnd.getZ() - regionStart.getZ())));
    regionBox.setTranslateX((regionEnd.getX() + regionStart.getX()) / 2);
    regionBox.setTranslateZ((regionEnd.getZ() + regionStart.getZ()) / 2);
    // decision (user request): the box reaches above the highest data point, a box on the floor
    // is hidden below the data; the 2D view draws it after the data anyway
    final int tile = Math.max(0, tiles.indexOf(regionTile));
    final double height = flat ? minimum : Math.max(minimum, sliceTop(tile));
    regionBox.setHeight(height);
    regionBox.setTranslateY(-height / 2);
    regionBox.setVisible(true);
  }

  /**
   * Fits the camera to the dragged box (user decision: zooming replaces selecting subsets). The
   * visible range is then shown in full detail.
   */
  private void zoomToBox() {
    final Point3D start = regionStart;
    final Point3D end = regionEnd;
    cancelRegion();
    // a box of a few pixels on screen is a click, not a drag
    if (start == null || end == null || screenDistance(start, new Point3D(end.getX(), 0,
        start.getZ())) < MIN_BOX_PIXELS || screenDistance(start, new Point3D(start.getX(), 0,
        end.getZ())) < MIN_BOX_PIXELS) {
      return;
    }
    final List<Point3D> corners = new ArrayList<>();
    final List<Point3D> modelCorners = new ArrayList<>();
    for (final double x : new double[]{start.getX(), end.getX()}) {
      for (final double z : new double[]{start.getZ(), end.getZ()}) {
        final Point3D corner = regionTile.toModel(new Point3D(x, 0, z));
        modelCorners.add(corner);
        corners.add(model.localToParent(corner));
      }
    }
    autoFit = false;
    focusedTile = -1;
    fitCamera(corners);
    if (flat) {
      fitPlotArea(corners, modelCorners);
    }
    hideHover();
    requestDetail();
  }

  /**
   * Moves the camera of the top view so that the fitted box fills the fixed plot area of the 2D
   * view instead of the viewport.
   *
   * @param corners      box corners in camera coordinates, already fitted to the viewport
   * @param modelCorners the same corners in model coordinates
   */
  private void fitPlotArea(@NotNull final List<Point3D> corners,
      @NotNull final List<Point3D> modelCorners) {
    final Rectangle2D area = plotArea();
    if (area == null) {
      return;
    }
    final double[] screen = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE,
        -Double.MAX_VALUE};
    for (final Point3D corner : modelCorners) {
      final Point3D sceneCorner = model.localToScene(corner, true);
      if (sceneCorner == null) {
        return;
      }
      final Point2D local = scene.sceneToLocal(sceneCorner.getX(), sceneCorner.getY());
      include(screen, local.getX(), local.getY());
    }
    double minX = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE;
    double depth = 0;
    for (final Point3D corner : corners) {
      minX = Math.min(minX, corner.getX());
      maxX = Math.max(maxX, corner.getX());
      depth += corner.getZ() / corners.size();
    }
    final double screenWidth = screen[1] - screen[0];
    final double screenHeight = screen[3] - screen[2];
    if (!(screenWidth > 0) || !(screenHeight > 0) || !(maxX > minX)) {
      return;
    }
    // assumption: the flat top view projects both axes with the same pixels per unit
    final double factor = Math.min(area.getWidth() / screenWidth,
        area.getHeight() / screenHeight);
    final double distance = Math.max(MIN_DISTANCE, (depth - camera.getTranslateZ()) / factor);
    final double pixelsPerUnit = screenWidth / (maxX - minX) * (depth - camera.getTranslateZ())
        / distance;
    final double boxX = (screen[0] + screen[1]) / 2;
    final double boxY = (screen[2] + screen[3]) / 2;
    camera.setTranslateX(camera.getTranslateX() - (area.getMinX() + area.getWidth() / 2 - boxX)
        / pixelsPerUnit);
    camera.setTranslateY(camera.getTranslateY() - (area.getMinY() + area.getHeight() / 2 - boxY)
        / pixelsPerUnit);
    camera.setTranslateZ(depth - distance);
  }

  /**
   * @return screen distance of two local points of the zoom box tile
   */
  private double screenDistance(@NotNull final Point3D a, @NotNull final Point3D b) {
    final Point3D sa = model.localToScene(regionTile.toModel(a), true);
    final Point3D sb = model.localToScene(regionTile.toModel(b), true);
    return sa == null || sb == null ? 0 : Math.hypot(sa.getX() - sb.getX(), sa.getY() - sb.getY());
  }

  /**
   * @return the data window visible in the viewport, full if nearly all data are visible
   */
  public @NotNull Surface3DRegion visibleWindow() {
    // the 2D view clips everything outside its fixed plot area
    final double[] area = flat ? plotAreaFloor() : null;
    final double[] floor = area != null ? area : visibleFloor();
    final Surface3DBounds current = bounds;
    if (floor == null || current == null || nearlyAll(floor)) {
      return Surface3DRegion.FULL;
    }
    return toData(current, floor);
  }

  /**
   * @return the visible part of the floor in local coordinates {x0, x1, z0, z1}, null if unknown.
   * Rays through the viewport corners, edge centers, and center are exact for the top view and
   * for zoomed tiles. If a ray misses, e.g. towards the horizon of a tilted view, floor and top
   * points of every tile are projected instead.
   */
  private double @Nullable [] visibleFloor() {
    if (bounds == null || series.isEmpty() || scene.getWidth() <= 0 || scene.getHeight() <= 0) {
      return null;
    }
    final List<Surface3DTile> shown = tiles;
    final double[] extent = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE,
        -Double.MAX_VALUE};
    boolean all = true;
    for (int i = 0; i <= 2 && all; i++) {
      for (int j = 0; j <= 2 && all; j++) {
        final Surface3DPicker.Ray ray = Surface3DPicker.ray(camera, scene.getWidth(),
            scene.getHeight(), scene.getWidth() * i / 2, scene.getHeight() * j / 2, model);
        Point3D hit = null;
        for (final Surface3DTile tile : shown) {
          hit = Surface3DPicker.floor(tile.toLocal(ray), false);
          if (hit != null) {
            break;
          }
        }
        if (hit == null) {
          all = false;
        } else {
          include(extent, hit.getX(), hit.getZ());
        }
      }
    }
    if (!all) {
      extent[0] = extent[2] = Double.MAX_VALUE;
      extent[1] = extent[3] = -Double.MAX_VALUE;
      projectFloor(shown, extent);
    }
    return extent[0] > extent[1] ? null : extent;
  }

  /**
   * Adds the floor and top sample points of the tiles that are visible in the viewport.
   */
  private void projectFloor(@NotNull final List<Surface3DTile> shown,
      final double @NotNull [] extent) {
    final Bounds view = viewport.localToScene(viewport.getLayoutBounds());
    final int steps = VISIBLE_STEPS;
    for (final Surface3DTile tile : shown) {
      for (int i = 0; i <= steps; i++) {
        final double x = (i / (double) steps - 0.5) * WIDTH;
        for (int j = 0; j <= steps; j++) {
          final double z = (j / (double) steps - 0.5) * DEPTH;
          for (final double y : flat ? new double[]{0} : new double[]{0, -Surface3DMesh.HEIGHT}) {
            final Point3D local = tile.toModel(new Point3D(x, y, z));
            if (model.localToParent(local).getZ()
                <= camera.getTranslateZ() + camera.getNearClip()) {
              continue;
            }
            final Point3D screen = model.localToScene(local, true);
            if (screen != null && view.contains(screen.getX(), screen.getY())) {
              include(extent, x, z);
            }
          }
        }
      }
    }
    if (extent[0] <= extent[1]) {
      // points between the samples may be visible too
      extent[0] = Math.max(-WIDTH / 2, extent[0] - WIDTH / steps);
      extent[1] = Math.min(WIDTH / 2, extent[1] + WIDTH / steps);
      extent[2] = Math.max(-DEPTH / 2, extent[2] - DEPTH / steps);
      extent[3] = Math.min(DEPTH / 2, extent[3] + DEPTH / steps);
    }
  }

  private static void include(final double @NotNull [] extent, final double x, final double z) {
    extent[0] = Math.min(extent[0], x);
    extent[1] = Math.max(extent[1], x);
    extent[2] = Math.min(extent[2], z);
    extent[3] = Math.max(extent[3], z);
  }

  private static boolean nearlyAll(final double @NotNull [] floor) {
    return floor[1] - floor[0] >= WIDTH * 0.9 && floor[3] - floor[2] >= DEPTH * 0.9;
  }

  private static @NotNull Surface3DRegion toData(@NotNull final Surface3DBounds current,
      final double @NotNull [] floor) {
    return new Surface3DRegion(
        Range.closed(Surface3DPicker.dataX(current, floor[0]),
            Surface3DPicker.dataX(current, floor[1])),
        Range.closed(Surface3DPicker.dataY(current, floor[2]),
            Surface3DPicker.dataY(current, floor[3])));
  }

  /**
   * The 2D view keeps a fixed plot area like a zoomed chart (user decision): the axes stay where
   * the fitted view placed them and only their tick values change, and data outside the area are
   * clipped instead of covering the tick labels. The 3D view keeps its axes at the data edges.
   */
  private void updateAxesFrame() {
    final Surface3DAxesSpec spec = axesSpec;
    final Surface3DBounds current = bounds;
    if (spec == null || current == null) {
      return;
    }
    updateClip();
    Surface3DRegion frame = null;
    lineScale = 1;
    final double[] plotFloor = plotAreaFloor();
    // side by side 2D tiles without focus have no plot area, their lines still follow the zoom
    final double[] area = plotFloor != null ? plotFloor : visibleFloor();
    if (area != null) {
      final boolean complete = area[0] <= -WIDTH / 2 + 1e-9 && area[1] >= WIDTH / 2 - 1e-9
          && area[2] <= -DEPTH / 2 + 1e-9 && area[3] >= DEPTH / 2 - 1e-9;
      if (!complete && area[1] > area[0] && area[3] > area[2]) {
        lineScale = Math.min(1, Surface3DAxes.thickness(area));
        frame = plotFloor != null ? toData(current, area) : null;
      }
    }
    if (Objects.equals(frame, spec.frame())) {
      return;
    }
    axesSpec = spec.withFrame(frame);
    allAxes().forEach(value -> value.rebuild(axesSpec));
    projectAxes();
  }

  private void cancelRegion() {
    regionStart = null;
    regionEnd = null;
    regionBox.setVisible(false);
  }

  private @NotNull WritableImage texture(@NotNull final SimpleColorPalette palette) {
    return textures.computeIfAbsent(palette, Surface3DColors::texture);
  }

  private @NotNull WritableImage gradient(@NotNull final Color color) {
    // decision (user request): low intensities fade into the plot background; a transparent base
    // looked washed out and showed depth sorting errors
    return gradients.computeIfAbsent(color, c -> Surface3DColors.gradient(c, plotBackground()));
  }

  /**
   * @return true if overlay opacities apply: only overlaid surfaces cover each other (user
   * decision), side by side tiles stay opaque
   */
  private boolean opacityApplies() {
    return layout.getValue() == Surface3DLayout.OVERLAY && series.size() > 1;
  }

  private void updateMaterials() {
    panel.setOpacityEnabled(opacityApplies());
    panel.setColorRangeEnabled(colorsShowIntensity());
    if (scale == null || palettes.getValue() == null) {
      return;
    }
    for (final Surface3DSeries value : series) {
      states.get(value.id()).colorMaximumProperty().set(scale.maximum(value.data()));
    }
    for (final Surface3DSeriesState state : states.values()) {
      updateMaterial(state);
    }
    final boolean byIntensity = coloring.getValue() == Surface3DColoring.INTENSITY;
    palettes.setDisable(!byIntensity);
    legendBox.setVisible(byIntensity);
    legendBox.setManaged(byIntensity);
    legend.setImage(texture(palettes.getValue()));
    legendMaximum.setText(scale.normalized() ? "100 %" : format.intensity(
        scale.bounds().maximum()));
  }

  /**
   * @return true if overlays are shaded by a gradient of their color instead of a flat color
   */
  private boolean overlayGradient() {
    // decision (user request): imaging overlays shade their color by intensity, because pixel
    // heights are compressed (log, minimum height) and a flat color hides intensity differences;
    // the 2D view has no heights at all
    return coloring.getValue() != Surface3DColoring.INTENSITY && !series.isEmpty() && (flat
        || series.getFirst().data().pixels());
  }

  /**
   * @return true if colors show intensities, so their range can be clipped
   */
  private boolean colorsShowIntensity() {
    return coloring.getValue() == Surface3DColoring.INTENSITY || overlayGradient();
  }

  private void updateMaterial(@NotNull final Surface3DSeriesState state) {
    if (palettes.getValue() == null) {
      return;
    }
    final boolean byIntensity = coloring.getValue() == Surface3DColoring.INTENSITY;
    final boolean gradient = overlayGradient();
    final Color overlayColor = state.colorProperty().get();
    final Color color = byIntensity || gradient ? Color.WHITE : overlayColor;
    final double opacity = opacityApplies() ? state.opacityProperty().get() : 1;
    final PhongMaterial material = state.material();
    material.setDiffuseColor(
        new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity));
    // the cached texture instances avoid a GPU upload on every slider or color change
    final WritableImage colors;
    if (byIntensity) {
      colors = texture(palettes.getValue());
    } else if (gradient) {
      colors = gradient(overlayColor);
    } else {
      colors = null;
    }
    state.colorBarProperty().set(colors);
    material.setDiffuseMap(colors == null ? null : state.clippedTexture(colors));
    material.setSpecularColor(Color.color(0.85, 0.9, 1, opacity));
    material.setSpecularPower(64);
  }

  private void resetView() {
    focusedTile = -1;
    tilt.setAngle(flat ? 90 : DEFAULT_TILT);
    turn.setAngle(flat ? 0 : DEFAULT_TURN);
    applyPreset();
  }

  private void topView() {
    tilt.setAngle(90);
    turn.setAngle(0);
    applyPreset();
  }

  private void frontView() {
    tilt.setAngle(0);
    turn.setAngle(0);
    applyPreset();
  }

  private void sideView() {
    tilt.setAngle(0);
    turn.setAngle(90);
    applyPreset();
  }

  private void applyPreset() {
    model.setTranslateX(0);
    model.setTranslateY(0);
    autoFit = true;
    hideHover();
    fitView();
    projectAxes();
    requestDetail();
  }

  private void projectAxes() {
    final List<Surface3DAxes> all = allAxes();
    for (final Surface3DAxes value : all) {
      value.setIntensityVisible(tilt.getAngle() < 75);
      value.project();
    }
    if (model.getScene() == null) {
      return;
    }
    // tile outlines, titles first, then axis labels where they cover nothing
    final List<List<Point2D>> hulls = new ArrayList<>();
    for (int i = 0; i < tiled.size() && i < tiles.size(); i++) {
      // tiles can briefly refer to removed overlays until the new layout is applied
      final Surface3DSeriesState state = states.get(tiled.get(i).id());
      hulls.add(state == null ? List.of()
          : tileHull(tiles.get(i), state.envelope(), labelLayer));
    }
    final List<Bounds> occupied = projectTileTitles(hulls);
    for (int i = 0; i < all.size(); i++) {
      final List<List<Point2D>> others = new ArrayList<>(hulls);
      if (i < others.size()) {
        // labels of a tile may touch its own outline, e.g. ticks under tall 3D columns
        others.remove(i);
      }
      all.get(i).hideOverlaps(occupied, others,
          new BoundingBox(0, 0, labelLayer.getWidth(), labelLayer.getHeight()));
    }
  }

  private @NotNull List<Surface3DAxes> allAxes() {
    final List<Surface3DAxes> all = new ArrayList<>(extraAxes.size() + 1);
    all.add(axes);
    all.addAll(extraAxes);
    return all;
  }

  private @NotNull Surface3DAxes tileAxes(final int index) {
    return index == 0 ? axes : extraAxes.get(index - 1);
  }

  private void ensureExtraAxes(final int count) {
    while (extraAxes.size() > count) {
      final Surface3DAxes removed = extraAxes.removeLast();
      extraAxesGroup.getChildren().remove(removed.geometry());
      labelLayer.getChildren().remove(removed.labels());
    }
    while (extraAxes.size() < count) {
      final Surface3DAxes added = new Surface3DAxes();
      added.setColors(dark, plotBackground);
      added.setGridVisible(showGrid.isSelected());
      if (axesSpec != null) {
        added.rebuild(axesSpec);
      }
      extraAxes.add(added);
      extraAxesGroup.getChildren().add(added.geometry());
      // below the titles
      labelLayer.getChildren().add(labelLayer.getChildren().size() - 1, added.labels());
    }
  }

  private void clearExtraAxes() {
    ensureExtraAxes(0);
  }

  private static int spinnerValue(@NotNull final Spinner<Integer> spinner) {
    final Integer value = spinner.getValue();
    return value == null ? 0 : value;
  }

  /**
   * Places tile titles above or below the projected outline of their tile, and only where they
   * cover no other tile, axis label, or title.
   */
  private @NotNull List<Bounds> projectTileTitles(
      @NotNull final List<List<Point2D>> hulls) {
    final List<Bounds> obstacles = new ArrayList<>();
    if (tileLabels.isEmpty()) {
      return obstacles;
    }
    final Pane pane = labelLayer;
    final Bounds area = new BoundingBox(0, 0, pane.getWidth(),
        pane.getHeight());
    for (int i = 0; i < tileLabels.size(); i++) {
      final Label label = tileLabels.get(i);
      if (!showLabels.isSelected() || i >= hulls.size() || hulls.get(i).isEmpty()) {
        label.setVisible(false);
        continue;
      }
      final Bounds tile = Surface3DScreenGeometry.bounds(hulls.get(i));
      label.applyCss();
      final double width = Math.min(label.prefWidth(-1), Math.max(60, tile.getWidth()));
      final double height = label.prefHeight(width);
      final double left = tile.getCenterX() - width / 2;
      Bounds placed = null;
      for (final double top : new double[]{tile.getMinY() - height - 4, tile.getMaxY() + 4}) {
        final Bounds candidate = new BoundingBox(left, top,
            width, height);
        if (fits(candidate, i, hulls, obstacles, area)) {
          placed = candidate;
          break;
        }
      }
      label.setVisible(placed != null);
      if (placed != null) {
        // long titles are truncated to the tile width
        label.resizeRelocate(placed.getMinX(), placed.getMinY(), width, height);
        obstacles.add(placed);
      }
    }
    return obstacles;
  }

  private static boolean fits(@NotNull final Bounds candidate, final int own,
      @NotNull final List<List<Point2D>> hulls,
      @NotNull final List<Bounds> obstacles,
      @NotNull final Bounds area) {
    if (!area.contains(candidate)) {
      return false;
    }
    for (int j = 0; j < hulls.size(); j++) {
      if (Surface3DScreenGeometry.intersects(candidate, hulls.get(j))) {
        return false;
      }
    }
    for (final Bounds obstacle : obstacles) {
      if (candidate.intersects(obstacle)) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return screen outline of the tile floor and the envelope of its signal
   */
  private @NotNull List<Point2D> tileHull(@NotNull final Surface3DTile tile,
      final float @NotNull [] envelope, @NotNull final Pane pane) {
    final List<Point2D> points = new ArrayList<>();
    for (final double x : new double[]{-WIDTH / 2, WIDTH / 2}) {
      for (final double z : new double[]{-DEPTH / 2, DEPTH / 2}) {
        addScreenPoint(points, tile.toModel(new Point3D(x, 0, z)), pane);
      }
    }
    for (int cell = 0; cell < envelope.length; cell++) {
      if (envelope[cell] > -1) {
        continue;
      }
      final int cx = cell % FIT_DIVISIONS;
      final int cz = cell / FIT_DIVISIONS;
      for (int dx = 0; dx <= 1; dx++) {
        for (int dz = 0; dz <= 1; dz++) {
          addScreenPoint(points, tile.toModel(new Point3D(
              ((cx + dx) / (double) FIT_DIVISIONS - 0.5) * WIDTH, envelope[cell],
              ((cz + dz) / (double) FIT_DIVISIONS - 0.5) * DEPTH)), pane);
        }
      }
    }
    return Surface3DScreenGeometry.convexHull(points);
  }

  private void addScreenPoint(@NotNull final List<Point2D> points,
      @NotNull final Point3D modelPoint, @NotNull final Pane pane) {
    final Point3D scene = model.localToScene(modelPoint, true);
    if (scene != null) {
      points.add(pane.sceneToLocal(scene.getX(), scene.getY()));
    }
  }

  /**
   * Small boxes enclosing the actual signal give a tighter fit than one large bounding box.
   */
  private @NotNull List<Point3D> fittingPoints(@NotNull final List<Surface3DSeries> snapshot) {
    final List<Point3D> points = new ArrayList<>();
    switch (layout.getValue()) {
      case OVERLAY -> {
        final float[] heights = new float[FIT_DIVISIONS * FIT_DIVISIONS];
        for (final Surface3DSeries value : snapshot) {
          final float[] envelope = states.get(value.id()).envelope();
          for (int i = 0; i < envelope.length && i < heights.length; i++) {
            heights[i] = Math.min(heights[i], envelope[i]);
          }
        }
        addEnvelope(points, heights, Surface3DTile.IDENTITY);
      }
      case GRID -> {
        for (int i = 0; i < tiled.size() && i < tiles.size(); i++) {
          final Surface3DSeriesState state = states.get(tiled.get(i).id());
          if (state != null) {
            addEnvelope(points, state.envelope(), tiles.get(i));
          }
        }
      }
    }
    return points;
  }

  private static void addEnvelope(@NotNull final List<Point3D> points,
      final float @NotNull [] heights, @NotNull final Surface3DTile tile) {
    for (int z = 0; z < FIT_DIVISIONS; z++) {
      for (int x = 0; x < FIT_DIVISIONS; x++) {
        final int cell = z * FIT_DIVISIONS + x;
        final double y = cell < heights.length ? heights[cell] : 0;
        for (int dx = 0; dx <= 1; dx++) {
          for (int dz = 0; dz <= 1; dz++) {
            points.add(tile.toModel(
                new Point3D(((x + dx) / (double) FIT_DIVISIONS - 0.5) * WIDTH, y,
                    ((z + dz) / (double) FIT_DIVISIONS - 0.5) * DEPTH)));
          }
        }
      }
    }
  }

  private void fitView() {
    if (viewport.getHeight() <= 0 || viewport.getWidth() <= 0) {
      return;
    }
    // the fit places the axes at the data edges and defines the plot area of the 2D view
    resetAxesFrame();
    final List<Point3D> corners = new ArrayList<>();
    final boolean focused = layout.get() == Surface3DLayout.GRID && focusedTile >= 0
        && focusedTile < tiled.size() && focusedTile < tiles.size()
        && states.get(tiled.get(focusedTile).id()) != null;
    if (focused) {
      final List<Point3D> points = new ArrayList<>();
      addEnvelope(points, states.get(tiled.get(focusedTile).id()).envelope(),
          tiles.get(focusedTile));
      for (final Point3D point : points) {
        corners.add(model.localToParent(point));
      }
    } else {
      focusedTile = -1;
      for (final Point3D point : fittingPoints) {
        corners.add(model.localToParent(point));
      }
    }
    for (final Surface3DAxes value : focused ? List.of(tileAxes(focusedTile)) : allAxes()) {
      value.setIntensityVisible(tilt.getAngle() < 75);
      // without labels no room is reserved for text
      for (final Point3D point : showLabels.isSelected() ? value.fittingPoints()
          : List.<Point3D>of()) {
        corners.add(model.localToParent(value.geometry().localToParent(point)));
      }
    }
    fitCamera(corners);
    if (flat) {
      updatePlotArea();
    }
  }

  private void resetAxesFrame() {
    final Surface3DAxesSpec spec = axesSpec;
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
  private @Nullable Surface3DTile plotTile() {
    if (!flat) {
      return null;
    }
    if (focusedTile >= 0 && focusedTile < tiles.size()) {
      return tiles.get(focusedTile);
    }
    return tiles.size() == 1 ? tiles.getFirst() : null;
  }

  /**
   * Stores the margins of the fitted plot floor to the viewport edges.
   */
  private void updatePlotArea() {
    final Surface3DTile tile = plotTile();
    plotInsets = null;
    if (tile != null) {
      final double[] screen = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE,
          -Double.MAX_VALUE};
      for (final double x : new double[]{-WIDTH / 2, WIDTH / 2}) {
        for (final double z : new double[]{-DEPTH / 2, DEPTH / 2}) {
          final Point3D sceneCorner = model.localToScene(tile.toModel(new Point3D(x, 0, z)), true);
          if (sceneCorner != null) {
            final Point2D local = scene.sceneToLocal(sceneCorner.getX(), sceneCorner.getY());
            include(screen, local.getX(), local.getY());
          }
        }
      }
      if (screen[1] > screen[0] && screen[3] > screen[2]) {
        plotInsets = new Insets(screen[2], scene.getWidth() - screen[1],
            scene.getHeight() - screen[3], screen[0]);
      }
    }
    updateClip();
  }

  /**
   * @return screen area of the fixed 2D plot in the current viewport, null if there is none
   */
  private @Nullable Rectangle2D plotArea() {
    final Insets insets = plotInsets;
    if (insets == null || plotTile() == null) {
      return null;
    }
    final double width = scene.getWidth() - insets.getLeft() - insets.getRight();
    final double height = scene.getHeight() - insets.getTop() - insets.getBottom();
    return width > 0 && height > 0 ? new Rectangle2D(insets.getLeft(), insets.getTop(), width,
        height) : null;
  }

  /**
   * Clips the 2D view to its plot area, so zoomed data do not cover the tick labels.
   */
  private void updateClip() {
    final Rectangle2D area = plotArea();
    // a small margin keeps the axis lines at the border
    scene.setClip(area == null ? null : new Rectangle(area.getMinX() - 2, area.getMinY() - 2,
        area.getWidth() + 4, area.getHeight() + 4));
  }

  /**
   * @return local floor {x0, x1, z0, z1} shown in the plot area of the 2D view, null without a
   * fixed plot area
   */
  private double @Nullable [] plotAreaFloor() {
    final Rectangle2D area = plotArea();
    final Surface3DTile tile = plotTile();
    if (area == null || tile == null) {
      return null;
    }
    final double[] extent = {Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE,
        -Double.MAX_VALUE};
    for (final double x : new double[]{area.getMinX(), area.getMaxX()}) {
      for (final double y : new double[]{area.getMinY(), area.getMaxY()}) {
        final Point3D hit = Surface3DPicker.floor(tile.toLocal(
            Surface3DPicker.ray(camera, scene.getWidth(), scene.getHeight(), x, y, model)), true);
        if (hit == null) {
          return null;
        }
        include(extent, hit.getX(), hit.getZ());
      }
    }
    return extent;
  }

  /**
   * Places the camera so that all points are visible.
   */
  private void fitCamera(@NotNull final List<Point3D> corners) {
    if (corners.isEmpty()) {
      return;
    }
    double minX = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    double maxY = -Double.MAX_VALUE;
    for (final Point3D point : corners) {
      minX = Math.min(minX, point.getX());
      maxX = Math.max(maxX, point.getX());
      minY = Math.min(minY, point.getY());
      maxY = Math.max(maxY, point.getY());
    }
    final double cx = (minX + maxX) / 2;
    final double cy = (minY + maxY) / 2;
    final double tangent = Math.tan(Math.toRadians(camera.getFieldOfView() / 2)) * 0.84;
    final double aspect = viewport.getWidth() / viewport.getHeight();
    double z = Double.MAX_VALUE;
    for (final Point3D point : corners) {
      final double fitted = Math.max(Math.abs(point.getY() - cy) / tangent,
          Math.abs(point.getX() - cx) / (tangent * aspect));
      z = Math.min(z, point.getZ() - Math.max(MIN_DISTANCE, fitted));
    }
    camera.setTranslateX(cx);
    camera.setTranslateY(cy);
    camera.setTranslateZ(z);
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
    generation.incrementAndGet();
    builders.shutdownNow();
  }

  private record BuiltMesh(@NotNull Surface3DMesh mesh, float @NotNull [] envelope) {

  }

  private record SmoothedData(@NotNull Surface3DData source, @NotNull Surface3DSmoothing params,
                              @NotNull Surface3DData result) {

  }

}
