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

import io.github.mzmine.gui.chartbasics.chartutils.paintscales.PaintScaleTransform;
import io.github.mzmine.gui.colorpicker.ColorPickerMenuItem;
import io.github.mzmine.gui.preferences.ImageNormalization;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxIconButtonBuilder;
import io.github.mzmine.javafx.components.factories.FxPopOvers;
import io.github.mzmine.javafx.components.factories.MenuItems;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapAxisKind;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapGrid;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapProjection;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSeries;
import io.github.mzmine.modules.visualization.intensitymap.data.IntensityMapSmoothing;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The toolbar and the Display popover of the plot. The owner reacts to changes of the controls. The
 * UI is compact (user request): one row with camera presets, the view mode menu, the Display
 * popover, and the noise floor; styling and saving on the right.
 */
final class IntensityMapControls {

  private static final Logger logger = Logger.getLogger(IntensityMapControls.class.getName());

  private final IntensityMapProjection projection;
  private final Slider heightSlider = new Slider(0.2, 3, 1);
  // same transformations and normalizations as the imaging preferences
  private final ComboBox<PaintScaleTransform> transform = FxComboBox.createComboBox(
      "Transformation of the height; colors stay linear", PaintScaleTransform.values(), null);
  private final ComboBox<ImageNormalization> intensityNormalization = FxComboBox.createComboBox(
      "Normalize every scan or pixel to the average TIC, as for images. Reads the data again.",
      ImageNormalization.values(), null);
  private final CheckBox normalized = new CheckBox("Each overlay to its maximum");
  private final CheckBox fromLowest = new CheckBox("Start at the lowest pixel");
  private final ComboBox<IntensityMapColoring> coloring = FxComboBox.createComboBox(
      "Color surfaces by intensity or by overlay color", IntensityMapColoring.values(), null);
  private final ComboBox<SimpleColorPalette> palettes = new ComboBox<>();
  private final Spinner<Integer> smoothingRadius = new Spinner<>(0, 20, 0);
  private final ComboBox<IntensityMapSmoothing.Axes> smoothingAxes = FxComboBox.createComboBox(
      "Smooth along both axes or one, e.g. only retention time to keep m/z separated",
      IntensityMapSmoothing.Axes.values(), null);
  private final Spinner<Integer> gridColumns = new Spinner<>(0, 20, 0);
  private final Spinner<Integer> gridRows = new Spinner<>(0, 20, 0);
  private final Slider noiseFloor = new Slider(0, 20, 0);
  private final TextField noiseField = new TextField();
  private final ToggleButton showLabels = FxIconButtonBuilder.ofToggleIconButton("bi-fonts")
      .build();
  private final ToggleButton showGrid = FxIconButtonBuilder.ofToggleIconButton("bi-border-all")
      .build();
  private final Button frontButton = new Button();
  private final Button sideButton = new Button();
  private final Tooltip frontTooltip = new Tooltip();
  private final Tooltip sideTooltip = new Tooltip();
  private final GridPane displayGrid = new GridPane(8, 8);
  private final ObjectProperty<IntensityMapLayout> layout = new SimpleObjectProperty<>(
      IntensityMapLayout.OVERLAY);
  private final ToggleGroup modes = new ToggleGroup();
  // the user picked a layout, which is then kept
  private boolean layoutChosen;

  /**
   * @param palette    initial paint scale
   * @param projection height controls and camera presets only apply to projections with them
   */
  IntensityMapControls(@NotNull final SimpleColorPalette palette,
      @NotNull final IntensityMapProjection projection) {
    this.projection = projection;
    heightSlider.setPrefWidth(150);
    heightSlider.setTooltip(new Tooltip("Vertical exaggeration; intensities retain their scale"));
    noiseFloor.setValue(projection.defaultNoisePercent());
    if (projection.transformsColors()) {
      // decision (user decision after testing): log10 shows weak signals best; the former 2D plot
      // also changed colors fastest at low intensities
      transform.setValue(PaintScaleTransform.LOG10);
      transform.setTooltip(new Tooltip(
          "Transformation of the colors, e.g. square root or log to show weak signals"));
    } else {
      transform.setValue(PaintScaleTransform.LINEAR);
    }
    intensityNormalization.setValue(ImageNormalization.NO_NORMALIZATION);
    normalized.setTooltip(new Tooltip(
        "Scale every overlay to its own maximum, e.g. to compare ions of different abundance"));
    fromLowest.setTooltip(new Tooltip(
        "The weakest shown pixel starts at the floor, so heights show differences instead of a tall base"));
    coloring.setValue(IntensityMapColoring.INTENSITY);
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
    displayGrid.setPadding(new Insets(10));
    smoothingRadius.setEditable(true);
    smoothingRadius.setPrefWidth(70);
    smoothingRadius.setTooltip(
        new Tooltip("Gaussian smoothing over this many neighboring data points, 0 is off"));
    smoothingAxes.setValue(IntensityMapSmoothing.Axes.BOTH);
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
    }
    gridColumns.setTooltip(new Tooltip("Images per row in side by side, Auto for a square grid"));
    gridRows.setTooltip(new Tooltip("Rows in side by side, Auto to fit all images"));
    showLabels.setSelected(true);
    showLabels.setTooltip(new Tooltip(
        "Show tick labels, axis titles, and tile titles; hidden labels free space for the data"));
    showGrid.setSelected(true);
    showGrid.setTooltip(new Tooltip("Show grid lines"));
    frontButton.setId("intensitymap-view-front");
    sideButton.setId("intensitymap-view-side");
    frontButton.getStyleClass().add("icon-button");
    sideButton.getStyleClass().add("icon-button");
    frontButton.setTooltip(frontTooltip);
    sideButton.setTooltip(sideTooltip);
    FxLayout.bindManagedToVisible(frontButton);
    FxLayout.bindManagedToVisible(sideButton);
    layout.addListener((_, _, value) -> modes.getToggles()
        .forEach(toggle -> toggle.setSelected(toggle.getUserData() == value)));
  }

  /**
   * @return the toolbar row
   */
  @NotNull Region toolbar(@NotNull final IntensityMapViewActions actions) {
    // decision: rarely changed settings live in a menu and a popover to keep one compact row
    frontButton.setOnAction(_ -> actions.frontView().run());
    sideButton.setOnAction(_ -> actions.sideView().run());
    final HBox views = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 2,
        viewButton("3d", "bi-box", "3D perspective that fits all data (R, double-click)",
            actions.resetView()),
        viewButton("top", "bi-grid-3x3", "Top view onto the coordinate plane (T)",
            actions.topView()), frontButton, sideButton);
    // the 2D view has one fixed camera
    views.setVisible(projection.rotatable());
    views.setManaged(projection.rotatable());
    final Button display = FxButtons.createButton("Display",
        "Height, log scale, normalization, and colors", () -> {
        });
    FxPopOvers.install(display, FxPopOvers.newPopOver(displayGrid));
    final Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    final Button save = FxButtons.createButton(null, FxIcons.SAVE, "Save the current view as PNG",
        actions.saveImage());
    // decision (user request): styling of the view next to saving it, top right
    final HBox style = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 2,
        plotBackgroundButton(actions), showGrid, showLabels);
    return FxLayout.newHBox(Pos.CENTER_LEFT, new Insets(4, 8, 4, 8), 8, views, viewModeButton(),
        display, new Separator(Orientation.VERTICAL), noiseControls(), spacer, style,
        new Separator(Orientation.VERTICAL), save, helpLabel());
  }

  private static @NotNull Button viewButton(@NotNull final String id, @NotNull final String icon,
      @NotNull final String tooltip, @NotNull final Runnable action) {
    final Button button = FxButtons.graphicButton(FxIconUtil.getFontIcon(icon, 16), tooltip,
        _ -> action.run());
    button.setId("intensitymap-view-" + id);
    return button;
  }

  private @NotNull MenuButton viewModeButton() {
    final MenuButton viewMode = new MenuButton("View mode",
        FxIconUtil.getFontIcon("bi-layers", 15));
    viewMode.setTooltip(new Tooltip("""
        Overlay: all overlays at their coordinates
        Side by side: small multiples with linked rotation and zoom"""));
    for (final IntensityMapLayout value : IntensityMapLayout.values()) {
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
    return viewMode;
  }

  private @NotNull Label helpLabel() {
    final Label help = new Label(null, FxIconUtil.getFontIcon(FxIcons.QUESTION_CIRCLE));
    final String common = """
        Mouse wheel or pinch: zoom at the cursor
        Side by side: double-click a tile to fit it, double-click elsewhere for all
        Click: show the spectrum at this position
        Spectrum: click a signal to show its m/z, Ctrl/⌘ + click to add or remove m/z,
        Ctrl/⌘ + drag to add an m/z window
        """;
    final Tooltip helpTip = new Tooltip(projection.rotatable() ? """
        Drag: rotate · Shift/right-drag or two-finger scroll: pan
        """ + common + """
        Ctrl/⌘ + drag on the floor: zoom to a box
        Double-click or R: fit view · T/F/S: top, front, side
        Arrow keys: rotate · +/−: zoom""" : """
        Drag, arrow keys, or two-finger scroll: pan
        """ + common + """
        Ctrl/⌘ + drag: zoom to a box
        Double-click or R: fit view · +/−: zoom""");
    helpTip.setShowDelay(Duration.millis(150));
    help.setTooltip(helpTip);
    return help;
  }

  private @NotNull HBox noiseControls() {
    noiseFloor.setPrefWidth(70);
    final String noiseTip = "Hide signals below this percentage of the maximum, e.g. to remove the noise carpet";
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
    });
    return FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 4, noiseLabel, noiseFloor, noiseField,
        percent);
  }

  /**
   * decision (user request): only the plot background is chosen here, everything else follows the
   * theme of the mzmine preferences
   */
  private @NotNull MenuButton plotBackgroundButton(@NotNull final IntensityMapViewActions actions) {
    final MenuButton button = new FxIconButtonBuilder<>(new MenuButton(),
        "bi-paint-bucket").build();
    button.setTooltip(new Tooltip("Plot background color"));
    // decision (user request): the theme default and the color picker, no fixed colors
    final MenuItem theme = MenuItems.create("Default", () -> actions.plotBackground().accept(null));
    button.getItems().setAll(theme, new SeparatorMenuItem());
    button.setOnShowing(_ -> {
      // decision: created on first use, the picker loads its FXML
      if (button.getItems().size() == 2) {
        try {
          final ColorPickerMenuItem picker = new ColorPickerMenuItem();
          picker.addColorSelectedListener(actions.plotBackground()::accept);
          button.getItems().add(picker);
        } catch (final IOException ex) {
          logger.log(Level.WARNING, "Cannot create the color picker", ex);
        }
      }
    });
    return button;
  }

  /**
   * Shows only the display options that apply to the data and view mode: imaging transformations
   * and normalization for images, a plain log option otherwise, overlay options with several
   * overlays, and grid options for side by side.
   *
   * @param current       displayed overlays
   * @param several       true for more than one overlay
   * @param normalization true if the owner resamples for a new intensity normalization
   */
  void updateDisplay(@NotNull final List<IntensityMapSeries> current, final boolean several,
      final boolean normalization) {
    final boolean imaging = !current.isEmpty() && current.getFirst().data().pixels();
    updateProfileButtons(current, imaging);
    final IntensityMapLayout mode = layout.get();
    displayGrid.getChildren().clear();
    int row = 0;
    if (projection.heights()) {
      displayGrid.addRow(row++, new Label("Height"), heightSlider);
    }
    // decision (user request): logarithmic heights only make sense for images
    if (projection.heights() && !imaging && !current.isEmpty()
        && transform.getValue() != PaintScaleTransform.LINEAR) {
      transform.setValue(PaintScaleTransform.LINEAR);
    }
    // the baseline changes heights only, which the 2D view does not have
    if (imaging && projection.heights()) {
      displayGrid.addRow(row++, new Label("Transform"), transform);
      displayGrid.addRow(row++, new Label("Heights"), fromLowest);
    }
    if (projection.transformsColors()) {
      displayGrid.addRow(row++, new Label("Colors"), transform);
    }
    if (imaging && normalization) {
      displayGrid.addRow(row++, new Label("Normalize"), intensityNormalization);
    }
    if (several) {
      displayGrid.addRow(row++, new Label("Overlays"), normalized);
      displayGrid.addRow(row++, new Label("Color by"), coloring);
    }
    displayGrid.addRow(row++, new Label("Paint scale"), palettes);
    displayGrid.addRow(row++, new Label("Smoothing"),
        FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 6, smoothingRadius, smoothingAxes));
    if (mode == IntensityMapLayout.GRID) {
      displayGrid.addRow(row, new Label("Tiles"),
          FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 6, new Label("Columns"), gridColumns,
              new Label("Rows"), gridRows));
    }
  }

  /**
   * Profile views match the axes: a peak for retention time or mobility, a spectrum for m/z. Images
   * have no meaningful profile, so both are hidden.
   */
  private void updateProfileButtons(@NotNull final List<IntensityMapSeries> current,
      final boolean imaging) {
    frontButton.setVisible(!imaging && projection.rotatable());
    sideButton.setVisible(!imaging && projection.rotatable());
    final IntensityMapGrid first = current.isEmpty() ? null : current.getFirst().data();
    final IntensityMapAxisKind x =
        first == null ? IntensityMapAxisKind.RETENTION_TIME : first.xKind();
    final IntensityMapAxisKind y = first == null ? IntensityMapAxisKind.MZ : first.yKind();
    frontButton.setGraphic(
        x == IntensityMapAxisKind.MZ ? IntensityMapIcons.spectrum() : IntensityMapIcons.peak());
    sideButton.setGraphic(
        y == IntensityMapAxisKind.MZ ? IntensityMapIcons.spectrum() : IntensityMapIcons.peak());
    frontTooltip.setText(
        "Front view: " + profileName(x, first == null ? "x" : first.xLabel()) + " (F)");
    sideTooltip.setText(
        "Side view: " + profileName(y, first == null ? "y" : first.yLabel()) + " (S)");
  }

  private static @NotNull String profileName(@NotNull final IntensityMapAxisKind kind,
      @NotNull final String axis) {
    return switch (kind) {
      case MZ -> "spectrum along " + axis;
      case RETENTION_TIME -> "chromatogram along " + axis;
      case MOBILITY -> "mobilogram along " + axis;
      case OTHER -> "profile along " + axis;
    };
  }

  static @NotNull String formatPercent(final double value) {
    return String.format(Locale.ROOT, value < 1 ? "%.2f" : "%.1f", value).replaceAll("0+$", "")
        .replaceAll("\\.$", "");
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

  /**
   * @param listener called when the noise floor was changed, not while its slider is dragged
   */
  void setOnNoiseChanged(@NotNull final Runnable listener) {
    noiseFloor.valueProperty().addListener((_, _, _) -> {
      if (!noiseFloor.isValueChanging()) {
        listener.run();
      }
    });
    noiseFloor.valueChangingProperty().addListener((_, _, changing) -> {
      if (!changing) {
        listener.run();
      }
    });
  }

  /**
   * @param percent noise floor in percent of the maximum
   */
  void setNoisePercent(final double percent) {
    noiseFloor.setMax(Math.max(20, percent));
    noiseFloor.setValue(Math.clamp(percent, 0, 100));
  }

  /**
   * @return noise floor as a fraction of the maximum
   */
  double noiseFloor() {
    return noiseFloor.getValue() / 100;
  }

  /**
   * Replaces the selectable paint scales, for example with all scales of the preferences.
   */
  void setPaintScales(@NotNull final List<SimpleColorPalette> scales) {
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

  void setPaintScale(@NotNull final SimpleColorPalette scale) {
    if (!palettes.getItems().contains(scale)) {
      palettes.getItems().add(scale);
    }
    palettes.setValue(scale);
  }

  @NotNull IntensityMapSmoothing smoothing() {
    final Integer radius = smoothingRadius.getValue();
    return new IntensityMapSmoothing(radius == null ? 0 : radius,
        Objects.requireNonNullElse(smoothingAxes.getValue(), IntensityMapSmoothing.Axes.BOTH));
  }

  @NotNull PaintScaleTransform transform() {
    return Objects.requireNonNullElse(transform.getValue(), PaintScaleTransform.LINEAR);
  }

  int gridColumns() {
    return spinnerValue(gridColumns);
  }

  int gridRows() {
    return spinnerValue(gridRows);
  }

  private static int spinnerValue(@NotNull final Spinner<Integer> spinner) {
    final Integer value = spinner.getValue();
    return value == null ? 0 : value;
  }

  @NotNull Slider heightSlider() {
    return heightSlider;
  }

  @NotNull ComboBox<PaintScaleTransform> transformBox() {
    return transform;
  }

  @NotNull ComboBox<ImageNormalization> intensityNormalization() {
    return intensityNormalization;
  }

  @NotNull CheckBox normalized() {
    return normalized;
  }

  @NotNull CheckBox fromLowest() {
    return fromLowest;
  }

  @NotNull ComboBox<IntensityMapColoring> coloring() {
    return coloring;
  }

  @NotNull ComboBox<SimpleColorPalette> palettes() {
    return palettes;
  }

  @NotNull Spinner<Integer> smoothingRadius() {
    return smoothingRadius;
  }

  @NotNull ComboBox<IntensityMapSmoothing.Axes> smoothingAxes() {
    return smoothingAxes;
  }

  @NotNull List<Spinner<Integer>> gridSpinners() {
    return List.of(gridColumns, gridRows);
  }

  @NotNull ToggleButton showLabels() {
    return showLabels;
  }

  @NotNull ToggleButton showGrid() {
    return showGrid;
  }

  /**
   * @return true if the profile presets apply to the data, e.g. not for images
   */
  boolean hasProfileViews() {
    return frontButton.isVisible();
  }

  @NotNull ObjectProperty<IntensityMapLayout> layout() {
    return layout;
  }

  boolean isLayoutChosen() {
    return layoutChosen;
  }
}
