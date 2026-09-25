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

import com.google.common.collect.Range;
import io.github.mzmine.gui.colorpicker.ColorPickerMenuItem;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.MenuItems;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DBounds;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DRegion;
import io.github.mzmine.modules.visualization.surface3d.data.Surface3DSeries;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Compact side panel: one row to add m/z overlays, one row per overlay with its options in a
 * menu, and a collapsible region section. Rarely used actions live in the header menu.
 */
final class Surface3DOverlayPanel extends VBox {

  private static final Logger logger = Logger.getLogger(Surface3DOverlayPanel.class.getName());

  private final Label title = FxLabels.newBoldLabel("Overlays");
  private final VBox cards = new VBox(2);
  private final HBox mzRow;
  private final TextField mzField = new TextField();
  private final TextField toleranceField = new TextField("10");
  private final ComboBox<String> toleranceUnit = new ComboBox<>();
  private final Label mzError = FxLabels.newLabel(FxLabels.Styles.ERROR, "");
  private final Button addFiles;
  private final Button resetMz;
  private final TitledPane regionSection;
  private final Label regionXLabel = new Label("X");
  private final Label regionYLabel = new Label("Y");
  private final TextField regionXMin = regionField();
  private final TextField regionXMax = regionField();
  private final TextField regionYMin = regionField();
  private final TextField regionYMax = regionField();
  private final Label regionError = FxLabels.newLabel(FxLabels.Styles.ERROR, "");
  private final Button fullRange;
  private @Nullable Consumer<List<Range<Double>>> onAddMz;
  private @Nullable Runnable onResetMz;
  private @Nullable Runnable onAddFiles;
  private @Nullable Consumer<String> onRemove;
  private @Nullable Consumer<Surface3DRegion> onRegion;
  private @Nullable Surface3DBounds bounds;

  Surface3DOverlayPanel() {
    super(6);
    setPadding(new Insets(8, 10, 8, 10));
    setPrefWidth(260);
    setMinWidth(200);

    // decision (user request): the rare overlay actions are icon buttons in the header
    addFiles = FxButtons.createButton(null, () -> "bi-file-earmark-plus",
        "Add the raw files selected in the project", () -> {
          if (onAddFiles != null) {
            onAddFiles.run();
          }
        });
    resetMz = FxButtons.createButton("m/z", "Show all m/z", () -> {
      if (onResetMz != null) {
        onResetMz.run();
      }
    });
    FxLayout.bindManagedToVisible(addFiles);
    FxLayout.bindManagedToVisible(resetMz);
    updateActions();
    final Region headerSpacer = new Region();
    HBox.setHgrow(headerSpacer, Priority.ALWAYS);
    final HBox header = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 4, title, headerSpacer,
        resetMz, addFiles);

    mzField.setPromptText("m/z or range");
    mzField.setTooltip(new Tooltip(
        "m/z values or ranges, separated by commas, e.g. 400.12, 512-514. Adds one overlay per sample and m/z."));
    mzField.setOnAction(_ -> addMz());
    mzField.setMinWidth(60);
    HBox.setHgrow(mzField, Priority.ALWAYS);
    toleranceField.setPrefColumnCount(3);
    toleranceField.setMinWidth(42);
    toleranceField.setTooltip(new Tooltip("Tolerance for single m/z values"));
    toleranceField.setOnAction(_ -> addMz());
    toleranceUnit.getItems().setAll("ppm", "m/z");
    toleranceUnit.setValue("ppm");
    toleranceUnit.setMinWidth(Region.USE_PREF_SIZE);
    final Button add = FxButtons.createButton(null, FxIcons.PLUS,
        "Add m/z overlays for every sample", this::addMz);
    final Label plusMinus = new Label("±");
    plusMinus.setMinWidth(Region.USE_PREF_SIZE);
    mzRow = FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 4, mzField, plusMinus,
        toleranceField, toleranceUnit, add);
    FxLayout.bindManagedToVisible(mzRow);
    mzRow.setVisible(false);
    FxLayout.bindManagedToVisible(mzError);
    mzError.setWrapText(true);
    mzError.setVisible(false);

    final ScrollPane scroll = new ScrollPane(cards);
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    // decision: keep the theme background, a transparent -fx-background derives unreadable text
    scroll.getStyleClass().add("edge-to-edge");
    VBox.setVgrow(scroll, Priority.ALWAYS);

    final GridPane regionGrid = new GridPane(4, 4);
    regionGrid.addRow(0, regionXLabel, regionXMin, new Label("–"), regionXMax);
    regionGrid.addRow(1, regionYLabel, regionYMin, new Label("–"), regionYMax);
    regionXLabel.setMaxWidth(90);
    regionYLabel.setMaxWidth(90);
    final Button apply = FxButtons.createButton("Apply", "Resample the entered region",
        this::applyRegion);
    fullRange = FxButtons.createButton("Full range", "Show all data again", () -> {
      if (onRegion != null) {
        onRegion.accept(Surface3DRegion.FULL);
      }
    });
    FxLayout.bindManagedToVisible(regionError);
    regionError.setVisible(false);
    regionError.setWrapText(true);
    final VBox regionContent = FxLayout.newVBox(Insets.EMPTY, regionGrid,
        FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 4, apply, fullRange), regionError);
    regionContent.setSpacing(6);
    regionSection = new TitledPane("Region", regionContent);
    regionSection.setExpanded(false);
    regionSection.setAnimated(false);
    regionSection.setTooltip(new Tooltip(
        "Ctrl/⌘ + drag on the floor to select a region. Smaller regions show more detail."));
    FxLayout.bindManagedToVisible(regionSection);
    regionSection.setVisible(false);

    getChildren().addAll(header, mzRow, mzError, scroll, regionSection);
  }

  /**
   * @return a small icon button that opens the menu, narrower than a menu button with arrow
   */
  private static @NotNull Button menuButton(@NotNull final String tooltip,
      @NotNull final ContextMenu menu) {
    final Button button = FxButtons.graphicButton(FxIconUtil.getFontIcon(FxIcons.COLUMNS_DOTS),
        tooltip, _ -> {
        });
    button.setOnAction(_ -> menu.show(button, Side.BOTTOM, 0, 0));
    return button;
  }

  private static @NotNull TextField regionField() {
    final TextField field = new TextField();
    field.setPrefColumnCount(5);
    return field;
  }

  private void updateActions() {
    addFiles.setVisible(onAddFiles != null);
    resetMz.setVisible(onResetMz != null);
  }

  void setOnAddMz(@Nullable final Consumer<List<Range<Double>>> listener) {
    onAddMz = listener;
    mzRow.setVisible(listener != null);
  }

  void setOnResetMz(@Nullable final Runnable listener) {
    onResetMz = listener;
    updateActions();
  }

  /**
   * @return the entered extraction tolerance, null if invalid
   */
  @Nullable MZTolerance mzTolerance() {
    try {
      final double value = Double.parseDouble(toleranceField.getText().trim());
      if (!(value > 0) || !Double.isFinite(value)) {
        return null;
      }
      return "ppm".equals(toleranceUnit.getValue()) ? new MZTolerance(0, value)
          : new MZTolerance(value, 0);
    } catch (final NumberFormatException ex) {
      return null;
    }
  }

  void setOnAddFiles(@Nullable final Runnable listener) {
    onAddFiles = listener;
    updateActions();
  }

  void setOnRemove(@Nullable final Consumer<String> listener) {
    onRemove = listener;
  }

  void setOnRegion(@Nullable final Consumer<Surface3DRegion> listener) {
    onRegion = listener;
    regionSection.setVisible(listener != null);
  }

  /**
   * Shows the region in the fields, with the data extent as prompt text.
   */
  void setRegion(@NotNull final Surface3DRegion region, @Nullable final Surface3DBounds bounds,
      @NotNull final String xLabel, @NotNull final String yLabel) {
    this.bounds = bounds;
    regionXLabel.setText(xLabel);
    regionYLabel.setText(yLabel);
    regionXLabel.setTooltip(new Tooltip(xLabel));
    regionYLabel.setTooltip(new Tooltip(yLabel));
    show(regionXMin, regionXMax, region.x(), bounds == null ? null
        : Range.closed(bounds.xMin(), bounds.xMax()));
    show(regionYMin, regionYMax, region.y(), bounds == null ? null
        : Range.closed(bounds.yMin(), bounds.yMax()));
    fullRange.setDisable(region.isFull());
    regionSection.setText(region.isFull() ? "Region" : "Region · zoomed");
    regionError.setVisible(false);
  }

  private static void show(@NotNull final TextField min, @NotNull final TextField max,
      @Nullable final Range<Double> value, @Nullable final Range<Double> extent) {
    min.setText(value == null ? "" : Surface3DAxes.format(value.lowerEndpoint()));
    max.setText(value == null ? "" : Surface3DAxes.format(value.upperEndpoint()));
    min.setPromptText(extent == null ? "" : Surface3DAxes.format(extent.lowerEndpoint()));
    max.setPromptText(extent == null ? "" : Surface3DAxes.format(extent.upperEndpoint()));
  }

  void setSeries(@NotNull final List<Surface3DSeries> series,
      @NotNull final Map<String, Surface3DSeriesState> states) {
    title.setText(series.isEmpty() ? "Overlays" : "Overlays (" + series.size() + ")");
    cards.getChildren().clear();
    if (series.isEmpty()) {
      final Label empty = FxLabels.newItalicLabel("Add m/z values or raw files to begin.");
      empty.setWrapText(true);
      cards.getChildren().add(empty);
      return;
    }
    for (final Surface3DSeries value : series) {
      cards.getChildren().add(row(value, states.get(value.id()), states));
    }
  }

  /**
   * One compact row: color, visibility with name, and a menu with opacity, solo and remove.
   */
  private @NotNull HBox row(@NotNull final Surface3DSeries value,
      @NotNull final Surface3DSeriesState state,
      @NotNull final Map<String, Surface3DSeriesState> states) {
    final Button color = colorButton(state);

    final CheckBox visible = new CheckBox();
    visible.selectedProperty().bindBidirectional(state.visibleProperty());
    visible.setTooltip(new Tooltip("Show or hide this overlay"));

    // decision (user request): name and extraction on separate lines and wrapped, not truncated
    final Label name = FxLabels.newLabel(value.name());
    name.setWrapText(true);
    name.setTextAlignment(TextAlignment.LEFT);
    name.setMaxWidth(Double.MAX_VALUE);
    final VBox text = new VBox(1, name);
    if (!value.description().isBlank()) {
      final Label description = FxLabels.newSmallLabel(value.description());
      description.setWrapText(true);
      description.setOpacity(0.75);
      text.getChildren().add(description);
    }
    text.setMinWidth(40);
    text.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(text, Priority.ALWAYS);
    Tooltip.install(text, new Tooltip(
        value.fullName() + "\n" + value.data().width() + " × " + value.data().height()
            + " grid"));
    // clicking the text toggles visibility like a check box label
    text.setOnMouseClicked(event -> {
      if (event.getButton() == MouseButton.PRIMARY) {
        state.visibleProperty().set(!state.visibleProperty().get());
      }
    });

    final Slider opacity = new Slider(0.1, 1, state.opacityProperty().get());
    opacity.valueProperty().bindBidirectional(state.opacityProperty());
    opacity.setPrefWidth(120);
    final CustomMenuItem opacityItem = new CustomMenuItem(
        FxLayout.newHBox(Pos.CENTER_LEFT, Insets.EMPTY, 6, new Label("Opacity"), opacity),
        false);
    final ContextMenu menu = new ContextMenu();
    final Button options = menuButton("Opacity, show only this, remove", menu);
    menu.getItems().setAll(opacityItem, new SeparatorMenuItem(),
        MenuItems.create("Show only this", () -> {
          for (final var entry : states.entrySet()) {
            entry.getValue().visibleProperty().set(entry.getKey().equals(value.id()));
          }
        }), MenuItems.create("Show all", () -> states.values()
            .forEach(other -> other.visibleProperty().set(true))));
    if (onRemove != null) {
      menu.getItems().addAll(new SeparatorMenuItem(),
          MenuItems.create("Remove", () -> onRemove.accept(value.id())));
    }
    final HBox row = FxLayout.newHBox(Pos.CENTER_LEFT, new Insets(2, 0, 2, 0), 4, color,
        visible, text, options);
    // right click anywhere on the row opens the same options
    row.setOnContextMenuRequested(event -> menu.show(row, event.getScreenX(), event.getScreenY()));
    return row;
  }

  /**
   * @return a swatch that opens the mzmine color picker with the colors of the default palette
   */
  private static @NotNull Button colorButton(@NotNull final Surface3DSeriesState state) {
    // decision: a shape keeps its size, the icon button style collapses a region graphic
    final Rectangle swatch = new Rectangle(14, 14);
    swatch.setArcWidth(4);
    swatch.setArcHeight(4);
    swatch.fillProperty().bind(state.colorProperty());
    final ContextMenu menu = new ContextMenu();
    final Button button = FxButtons.graphicButton(swatch, "Overlay color", _ -> {
    });
    button.setOnAction(_ -> {
      // decision: created on first use, each picker loads its FXML
      if (menu.getItems().isEmpty()) {
        try {
          final ColorPickerMenuItem picker = new ColorPickerMenuItem();
          picker.addColorSelectedListener(state.colorProperty()::set);
          menu.getItems().add(picker);
        } catch (final IOException ex) {
          logger.log(Level.WARNING, "Cannot create the color picker", ex);
          return;
        }
      }
      menu.show(button, Side.BOTTOM, 0, 0);
    });
    return button;
  }

  private void addMz() {
    if (onAddMz == null) {
      return;
    }
    final MZTolerance tolerance = mzTolerance();
    if (tolerance == null) {
      showError(mzError, "Enter a positive m/z tolerance");
      return;
    }
    try {
      final List<Range<Double>> ranges = Surface3DMzInput.parse(mzField.getText(), tolerance);
      mzError.setVisible(false);
      mzField.clear();
      onAddMz.accept(ranges);
    } catch (final IllegalArgumentException ex) {
      showError(mzError, ex.getMessage());
    }
  }

  private void applyRegion() {
    if (onRegion == null) {
      return;
    }
    try {
      final Range<Double> x = parse(regionXMin, regionXMax,
          bounds == null ? null : Range.closed(bounds.xMin(), bounds.xMax()));
      final Range<Double> y = parse(regionYMin, regionYMax,
          bounds == null ? null : Range.closed(bounds.yMin(), bounds.yMax()));
      onRegion.accept(new Surface3DRegion(x, y));
    } catch (final IllegalArgumentException ex) {
      showError(regionError, ex.getMessage());
    }
  }

  /**
   * @return null if both fields are blank, blank fields otherwise default to the data extent
   */
  private static @Nullable Range<Double> parse(@NotNull final TextField min,
      @NotNull final TextField max, @Nullable final Range<Double> extent) {
    final String lowerText = min.getText().trim();
    final String upperText = max.getText().trim();
    if (lowerText.isEmpty() && upperText.isEmpty()) {
      return null;
    }
    try {
      final double lower = lowerText.isEmpty() && extent != null ? extent.lowerEndpoint()
          : Double.parseDouble(lowerText);
      final double upper = upperText.isEmpty() && extent != null ? extent.upperEndpoint()
          : Double.parseDouble(upperText);
      if (!(upper > lower)) {
        throw new IllegalArgumentException("The region maximum must exceed the minimum");
      }
      return Range.closed(lower, upper);
    } catch (final NumberFormatException ex) {
      throw new IllegalArgumentException("Enter numeric region limits");
    }
  }

  private static void showError(@NotNull final Label label, @Nullable final String message) {
    label.setText(message == null ? "Invalid input" : message);
    label.setVisible(true);
  }
}
