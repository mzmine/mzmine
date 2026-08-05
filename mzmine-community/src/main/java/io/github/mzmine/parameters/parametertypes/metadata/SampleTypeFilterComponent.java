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

package io.github.mzmine.parameters.parametertypes.metadata;

import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter;
import io.github.mzmine.modules.visualization.projectmetadata.SampleTypeFilter.Mode;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.ValuePropertyComponent;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Editing component for {@link SampleTypeFilterParameter}: a combo box like control that shows the
 * current filter and drops down a list with one checkbox per known sample type.
 * <p>
 * The item list is the union of the predefined {@link SampleType}s, the distinct values actually
 * present in the {@code mzmine_sample_type} metadata column, and any value the user typed into the
 * add field or that came from a loaded filter. It is rebuilt every time the menu opens, because
 * parameter dialogs are routinely constructed before any metadata has been imported.
 * <p>
 * Every item is a <b>normalized</b> value in the sense of {@link SampleTypeFilter}: trimmed and
 * lower cased. A column holding both {@code Sample} and {@code sample} therefore shows up as the
 * single item {@code sample} whose file count covers both, so the user cannot pick one spelling and
 * silently miss the other.
 * <p>
 * Only user added values can be removed again - values backed by the metadata column or by the enum
 * would simply reappear on the next rebuild, so offering to remove them would look like a bug.
 * Every item shows how many files carry it so that a typo, which would silently filter to nothing,
 * is visible as {@code 0 files}.
 */
public class SampleTypeFilterComponent extends HBox implements
    ValuePropertyComponent<SampleTypeFilter> {

  private static final Logger logger = Logger.getLogger(SampleTypeFilterComponent.class.getName());

  /**
   * Keeps the collapsed button from growing with the number of selected values.
   */
  private static final int MAX_DISPLAY_CHARS = 45;
  private static final double POPUP_WIDTH = 320;
  private static final double MAX_POPUP_LIST_HEIGHT = 320;
  private static final int REMOVE_ICON_SIZE = 12;
  /**
   * The remove button is pinned to this box so a removable row is exactly as tall as a row without
   * one - the default {@code .icon-button} padding of 0.5em makes it noticeably taller.
   */
  private static final double REMOVE_BUTTON_SIZE = 18;
  /**
   * Every row reserves this much space for the remove button, also the rows that cannot be removed,
   * so that the file counts of all rows line up in one column.
   */
  private static final double REMOVE_SLOT_WIDTH = 24;
  /**
   * Keeps "2 files" and "10 files" ending at the same x.
   */
  private static final double COUNT_WIDTH = 52;
  /**
   * Combo boxes flip their arrow while the popup is open, this drives the same CSS state.
   */
  private static final PseudoClass SHOWING = PseudoClass.getPseudoClass("showing");

  /**
   * Where an item in the list came from. Only {@link #CUSTOM} items can be removed again.
   */
  private enum Source {
    PREDEFINED, METADATA, CUSTOM
  }

  /**
   * One row of the list, kept so the checkboxes can be synced when the value changes from the
   * outside or through the all/none choices.
   *
   * @param value the normalized sample type value of this row
   */
  private record ItemRow(@NotNull String value, @NotNull CheckBox box) {

  }

  private final ObjectProperty<@NotNull SampleTypeFilter> value = new SimpleObjectProperty<>(
      SampleTypeFilter.all());
  /**
   * True while the filter is {@link Mode#ALL} or {@link Mode#NONE}, where picking individual types
   * is meaningless. Every item checkbox binds its {@code disableProperty} to this, so the items
   * re-enable automatically as soon as the mode goes back to {@link Mode#LIST} - no manual
   * enable/disable bookkeeping that could get out of sync.
   */
  private final BooleanBinding openEndedMode = Bindings.createBooleanBinding(
      () -> value.get().getMode() != Mode.LIST, value);
  /**
   * Normalized values the user typed in this component and that are neither predefined nor present
   * in the metadata column. Kept separately so they survive a rebuild of the item list.
   */
  private final Set<String> customValues = new LinkedHashSet<>();
  private final List<ItemRow> itemRows = new ArrayList<>();
  /**
   * Built from the {@code combo-box-base} / {@code list-cell} / {@code arrow-button} style classes
   * rather than being a real {@link javafx.scene.control.ComboBox}, so it picks up the theme's
   * combo box look while dropping down our own content. A {@code MenuButton} cannot be used: the
   * themes style {@code .menu-button} as a flat transparent button.
   */
  private final HBox comboBox = new HBox();
  private final Label displayLabel = new Label();
  private final Popup popup = new Popup();
  private final VBox popupContent;
  private final VBox itemsBox = FxLayout.newVBox(Insets.EMPTY);
  private final StringProperty newValueText = new SimpleStringProperty("");
  private final CheckBox allBox = new CheckBox("All sample types");
  private final CheckBox noneBox = new CheckBox("None");

  public SampleTypeFilterComponent(@Nullable SampleTypeFilter initialValue) {
    super(FxLayout.DEFAULT_SPACE);
    setAlignment(Pos.CENTER_LEFT);
    setPadding(Insets.EMPTY);
    setMaxWidth(Double.MAX_VALUE);
    // an HBox fills the height of its row by default. In the statistics dashboard this control
    // shares a FlowPane row with taller nodes (the help button), which stretched it well beyond the
    // height of a combo box - stay at the preferred height instead.
    setMaxHeight(Region.USE_PREF_SIZE);

    buildComboBox();
    popupContent = buildPopupContent();
    popup.getContent().add(popupContent);
    popup.setAutoHide(true);
    popup.setHideOnEscape(true);
    popup.setAutoFix(true);
    // keep the arrow in the "open" look while the popup is up, like a real combo box
    popup.showingProperty()
        .subscribe(showing -> comboBox.pseudoClassStateChanged(SHOWING, showing));

    value.subscribe(this::onValueChanged);
    setValue(initialValue);
  }

  /**
   * Reproduces the visual structure of a combo box: a {@code list-cell} for the text and an
   * {@code arrow-button} holding the {@code arrow}, inside a {@code combo-box-base} container.
   */
  private void buildComboBox() {
    displayLabel.getStyleClass().add("list-cell");
    displayLabel.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(displayLabel, Priority.ALWAYS);

    final Region arrow = new Region();
    arrow.getStyleClass().add("arrow");
    // the arrow is a shape scaled to the size of its Region. A ComboBox skin sizes that region
    // explicitly, but a StackPane would stretch the arrow to fill it, which renders a much larger
    // triangle than a neighbouring ComboBox - so keep it at its preferred (css padding) size.
    arrow.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    final StackPane arrowButton = new StackPane(arrow);
    arrowButton.getStyleClass().add("arrow-button");

    comboBox.getStyleClass().addAll("combo-box-base", "combo-box");
    comboBox.getChildren().setAll(displayLabel, arrowButton);
    comboBox.setAlignment(Pos.CENTER_LEFT);
    comboBox.setMaxWidth(Double.MAX_VALUE);
    comboBox.setMaxHeight(Region.USE_PREF_SIZE);
    comboBox.setFocusTraversable(true);
    comboBox.setOnMouseClicked(_ -> togglePopup());
    comboBox.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.ENTER
          || event.getCode() == KeyCode.DOWN) {
        togglePopup();
        event.consume();
      }
    });

    HBox.setHgrow(comboBox, Priority.ALWAYS);
    getChildren().add(comboBox);
  }

  private void togglePopup() {
    if (popup.isShowing()) {
      popup.hide();
      return;
    }
    // the project metadata is usually imported after the dialog was created, so rebuild on open
    refreshItems();
    // never narrower than the control itself
    popupContent.setPrefWidth(Math.max(POPUP_WIDTH, comboBox.getWidth()));
    final var bounds = comboBox.localToScreen(comboBox.getBoundsInLocal());
    popup.show(comboBox, bounds.getMinX(), bounds.getMaxY());
  }

  // ---------------------------------------------------------------------------------------------
  // value
  // ---------------------------------------------------------------------------------------------

  @Override
  public Property<SampleTypeFilter> valueProperty() {
    return value;
  }

  public @NotNull SampleTypeFilter getValue() {
    return value.get();
  }

  public void setValue(@Nullable SampleTypeFilter newValue) {
    final SampleTypeFilter filter = newValue == null ? SampleTypeFilter.all() : newValue;
    // a loaded filter may reference groups that are not in the project (yet) - keep them
    // selectable instead of silently dropping them
    customValues.addAll(filter.customValues());
    value.set(filter);
  }

  /**
   * Single place that reflects the value in the UI. All the checkboxes are updated programmatically
   * here, which never fires their {@code onAction}, so this cannot loop back into the value.
   */
  private void onValueChanged(@NotNull final SampleTypeFilter filter) {
    allBox.setSelected(filter.getMode() == Mode.ALL);
    noneBox.setSelected(filter.getMode() == Mode.NONE);
    syncItemChecks(filter);

    displayLabel.setText(filter.toShortString(MAX_DISPLAY_CHARS));
    Tooltip.install(comboBox, new Tooltip(switch (filter.getMode()) {
      case ALL -> "Matches every sample type, also types that are added later.";
      case NONE -> "Matches no sample at all.";
      case LIST -> filter.getValues().isEmpty() ? "Matches no sample at all."
          : "Selected sample types:\n" + String.join("\n", filter.getValues());
    }));
  }

  /**
   * Ticks the item boxes that the filter contains. For the open ended modes every box is shown
   * ticked ({@link Mode#ALL}) or unticked ({@link Mode#NONE}) - they are disabled anyway through
   * {@link #openEndedMode}, but the visual state should still say what the filter does.
   */
  private void syncItemChecks(@NotNull final SampleTypeFilter filter) {
    for (ItemRow row : itemRows) {
      // both are normalized, so a plain contains is the same comparison the filter itself makes
      row.box().setSelected(filter.matchesValue(row.value()));
    }
  }

  // ---------------------------------------------------------------------------------------------
  // menu content
  // ---------------------------------------------------------------------------------------------

  private VBox buildPopupContent() {
    final Node toggleAll = FxIconUtil.newIconButton(FxIcons.CHECK_ALL,
        "Select or deselect all listed sample types.", this::toggleAll);
    final HBox header = FxLayout.newHBox(Insets.EMPTY, toggleAll, new Label("Sample types"));
    header.setAlignment(Pos.CENTER_LEFT);
    header.setMaxWidth(Double.MAX_VALUE);

    allBox.setMaxWidth(Double.MAX_VALUE);
    noneBox.setMaxWidth(Double.MAX_VALUE);
    allBox.setTooltip(new Tooltip(
        "Match every sample type, including custom types that are only added later. Recommended over checking every single type."));
    noneBox.setTooltip(new Tooltip("Match no sample at all."));
    // setOnAction only reacts to user clicks, unlike a selectedProperty listener, so no guard flag
    // is needed against the programmatic updates in onValueChanged
    allBox.setOnAction(_ -> value.set(
        allBox.isSelected() ? SampleTypeFilter.all() : SampleTypeFilter.ofValues(List.of())));
    noneBox.setOnAction(_ -> value.set(
        noneBox.isSelected() ? SampleTypeFilter.none() : SampleTypeFilter.ofValues(List.of())));

    final TextField newValueField = new TextField();
    newValueField.setPromptText("Add sample type…");
    newValueField.textProperty().bindBidirectional(newValueText);
    newValueField.setOnAction(_ -> addTypedValue());
    newValueField.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(newValueField, Priority.ALWAYS);
    final Node addButton = FxIconUtil.newIconButton(FxIcons.ADD,
        "Add a sample type that is not in the project yet and select it.", this::addTypedValue);
    final HBox addRow = FxLayout.newHBox(Insets.EMPTY, newValueField, addButton);
    addRow.setAlignment(Pos.CENTER_LEFT);
    addRow.setMaxWidth(Double.MAX_VALUE);

    itemsBox.setFillWidth(true);
    final ScrollPane scroll = new ScrollPane(itemsBox);
    scroll.setFitToWidth(true);
    scroll.setMaxHeight(MAX_POPUP_LIST_HEIGHT);
    scroll.setHbarPolicy(ScrollBarPolicy.NEVER);
    scroll.setMaxWidth(Double.MAX_VALUE);
    VBox.setVgrow(scroll, Priority.ALWAYS);

    final VBox content = FxLayout.newVBox(new Insets(FxLayout.DEFAULT_SPACE), header, allBox,
        noneBox, new Separator(), addRow, scroll);
    content.setFillWidth(true);
    content.setPrefWidth(POPUP_WIDTH);
    content.setMaxWidth(Double.MAX_VALUE);
    // a plain Popup rather than a ContextMenu/CustomMenuItem: menu items carry a :focused
    // background that highlights the whole panel grey as the mouse moves over it. Colors are the
    // theme's own, matching .combo-box-popup > .list-view
    content.setStyle("""
        -fx-background-color: -fx-control-inner-background; \
        -fx-background-radius: 6px; \
        -fx-border-color: -fx-outer-border; \
        -fx-border-radius: 6px; \
        -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 2);""");
    return content;
  }

  /**
   * Rebuilds the checkbox list from enum ∪ metadata ∪ custom values and the current filter. Every
   * source contributes normalized values, so spellings that differ only in case collapse into one
   * row.
   */
  private void refreshItems() {
    final SampleTypeFilter filter = getValue();
    final Map<String, Integer> counts = countFilesPerSampleType();

    // predefined first, then everything the project or the user added, each block sorted a-z
    final Map<String, Source> items = new LinkedHashMap<>();
    for (String predefined : SampleType.allValueStrings()) {
      items.put(predefined, Source.PREDEFINED);
    }
    // counts is sorted, and the filter values as well
    counts.keySet().forEach(v -> items.putIfAbsent(v, Source.METADATA));
    customValues.stream().sorted().forEach(v -> items.putIfAbsent(v, Source.CUSTOM));
    // a selected value must always be visible, even if it is neither known nor typed here
    filter.getValues().forEach(v -> items.putIfAbsent(v, Source.CUSTOM));

    itemRows.clear();
    final List<Node> rows = new ArrayList<>();
    items.forEach((itemValue, source) -> rows.add(
        createRow(itemValue, source, counts.getOrDefault(itemValue, 0))));
    itemsBox.getChildren().setAll(rows);

    // rows were just created, apply the current filter to them
    syncItemChecks(filter);
  }

  private HBox createRow(@NotNull final String itemValue, @NotNull final Source source,
      final int fileCount) {
    final CheckBox box = new CheckBox(itemValue);
    // re-enables automatically when the mode goes back to a plain list
    box.disableProperty().bind(openEndedMode);
    box.setOnAction(_ -> setSelected(itemValue, box.isSelected()));
    itemRows.add(new ItemRow(itemValue, box));

    final Label count = new Label(fileCount == 1 ? "1 file" : "%d files".formatted(fileCount));
    count.getStyleClass().add("text-muted");
    count.setMinWidth(COUNT_WIDTH);
    count.setAlignment(Pos.CENTER_RIGHT);
    if (fileCount == 0) {
      // a value that matches nothing filters everything away - typos must not fail silently
      count.setTooltip(new Tooltip(
          "No data file currently uses this sample type. Selecting only this type would exclude every sample."));
    }

    final Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    // reserved on every row, so the counts of removable and non removable rows share one column
    final StackPane removeSlot = new StackPane();
    removeSlot.setMinWidth(REMOVE_SLOT_WIDTH);
    removeSlot.setPrefWidth(REMOVE_SLOT_WIDTH);
    removeSlot.setMaxWidth(REMOVE_SLOT_WIDTH);
    removeSlot.setMaxHeight(REMOVE_BUTTON_SIZE);
    if (source == Source.CUSTOM) {
      // only values that this component added can be removed - predefined and metadata backed
      // values would come back on the next rebuild
      final ButtonBase remove = FxIconUtil.newIconButton(FxIcons.X_CIRCLE, REMOVE_ICON_SIZE,
          "Remove this custom sample type from the list.", () -> removeCustomValue(itemValue));
      // .icon-button carries -fx-padding: 0.5em, which makes the row taller and much wider
      remove.setPadding(Insets.EMPTY);
      remove.setMinSize(REMOVE_BUTTON_SIZE, REMOVE_BUTTON_SIZE);
      remove.setPrefSize(REMOVE_BUTTON_SIZE, REMOVE_BUTTON_SIZE);
      remove.setMaxSize(REMOVE_BUTTON_SIZE, REMOVE_BUTTON_SIZE);
      removeSlot.getChildren().add(remove);
    }

    final HBox row = FxLayout.newHBox(Insets.EMPTY, box, spacer, count, removeSlot);
    row.setAlignment(Pos.CENTER_LEFT);
    row.setMaxWidth(Double.MAX_VALUE);
    return row;
  }

  // ---------------------------------------------------------------------------------------------
  // actions
  // ---------------------------------------------------------------------------------------------

  private void setSelected(@NotNull final String itemValue, final boolean selected) {
    final Set<String> values = new LinkedHashSet<>(getValue().getValues());
    if (selected) {
      values.add(itemValue);
    } else {
      values.remove(itemValue);
    }
    // any individual choice leaves the open ended all/none modes
    value.set(SampleTypeFilter.ofValues(values));
  }

  /**
   * Selects all listed items, or deselects everything if all of them are already selected.
   */
  private void toggleAll() {
    final List<String> listed = itemRows.stream().map(ItemRow::value).toList();
    final Set<String> selected = getValue().getValues();
    final boolean allSelected =
        getValue().getMode() == Mode.LIST && !listed.isEmpty() && selected.containsAll(listed);

    // deliberately does not switch to Mode.ALL - checking every known type and "any type" are
    // different things, and the user picks the latter through the dedicated checkbox
    value.set(
        allSelected ? SampleTypeFilter.ofValues(List.of()) : SampleTypeFilter.ofValues(listed));
  }

  private void addTypedValue() {
    final String typed = StringUtils.normalizeStripLowerCase(newValueText.get());
    if (typed.isEmpty()) {
      return;
    }

    final boolean known = itemRows.stream().anyMatch(row -> row.value().equals(typed));
    if (!known) {
      customValues.add(typed);
    }
    newValueText.set("");

    // adding is only useful if it also selects - otherwise the user would have to hunt for the row
    final Set<String> values = new LinkedHashSet<>(
        getValue().getMode() == Mode.LIST ? getValue().getValues() : Set.of());
    values.add(typed);
    value.set(SampleTypeFilter.ofValues(values));
    refreshItems();
  }

  private void removeCustomValue(@NotNull final String itemValue) {
    customValues.remove(itemValue);

    final Set<String> values = new LinkedHashSet<>(getValue().getValues());
    if (values.remove(itemValue)) {
      value.set(SampleTypeFilter.ofValues(values));
    }
    refreshItems();
  }

  // ---------------------------------------------------------------------------------------------
  // project metadata
  // ---------------------------------------------------------------------------------------------

  /**
   * @return number of data files per normalized sample type value, sorted alphabetically. Values
   * that only differ in case or surrounding whitespace share one entry, e.g. a column holding both
   * {@code QC} and {@code qc} counts as two files of {@code qc}. Empty if there is no project or no
   * sample type column yet (headless runs, batch setup before import).
   */
  private @NotNull Map<String, Integer> countFilesPerSampleType() {
    try {
      final MetadataTable metadata = ProjectService.getMetadata();
      final MetadataColumn<?> column = metadata.getColumnByName(MetadataColumn.SAMPLE_TYPE_HEADER);
      if (column == null) {
        return Map.of();
      }
      final var columnData = metadata.getColumnData(column);
      if (columnData == null) {
        return Map.of();
      }

      final Map<String, Integer> counts = new TreeMap<>();
      for (Object rawValue : columnData.values()) {
        final String normalized = StringUtils.normalizeStripLowerCase(rawValue);
        if (!normalized.isEmpty()) {
          counts.merge(normalized, 1, Integer::sum);
        }
      }
      return counts;
    } catch (Exception e) {
      // no project or no metadata - the component still has to work, just without counts
      logger.log(Level.FINE, "Cannot read sample types from project metadata", e);
      return Map.of();
    }
  }
}
