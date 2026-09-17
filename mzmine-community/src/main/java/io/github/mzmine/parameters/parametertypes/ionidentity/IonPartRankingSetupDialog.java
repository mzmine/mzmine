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

package io.github.mzmine.parameters.parametertypes.ionidentity;

import io.github.mzmine.datamodel.identities.global.GlobalIonLibraryService;
import io.github.mzmine.datamodel.identities.iontype.IonPart;
import io.github.mzmine.datamodel.identities.iontype.IonPartFrequency;
import io.github.mzmine.datamodel.identities.iontype.IonPartReference;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonTypeRanking;
import io.github.mzmine.javafx.components.FilterableListView;
import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.factories.FxListViews;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.converter.FloatStringConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Setup dialog for the single ion building block ranking. The left list offers the building blocks
 * of the global ion library, the right table holds the current ranking with an editable frequency
 * and re-sorts itself as soon as a frequency is changed. Both lists grow with the dialog.
 */
final class IonPartRankingSetupDialog extends Dialog<List<IonPartFrequency>> {

  /**
   * Frequency offered when the user is asked for the frequency of a newly added building block. Mid
   * range so the user immediately sees where it lands and can move it up or down.
   */
  private static final float DEFAULT_NEW_FREQUENCY = 0.5f;

  private final @NotNull ObservableList<IonPartFrequency> entries = FXCollections.observableArrayList();
  private final @NotNull ObservableList<IonPartReference> available = FXCollections.observableArrayList();
  private final @NotNull ButtonType okButtonType = new ButtonType("OK", ButtonData.OK_DONE);

  /// manual entry of a building block that is not in the global library, e.g. +Ca+2
  private final @NotNull StringProperty manualPart = new SimpleStringProperty("");
  private final @NotNull StringProperty manualFrequency = new SimpleStringProperty("");

  IonPartRankingSetupDialog(final @NotNull List<IonPartFrequency> current) {
    entries.setAll(current);
    available.setAll(collectAvailableParts(current));

    setTitle("Ion building block ranking");
    setHeaderText("""
        Higher frequency means the building block is observed more often and the ion type is ranked higher.
        Positive and negative charge carriers share one list, a loss like -H2O is ranked separately from
        the addition +H2O. Building blocks that are not listed count as frequency 0.""");
    setResizable(true);

    getDialogPane().sceneProperty().subscribe(scene -> {
      if (scene != null) {
        ConfigService.getConfiguration().getTheme().apply(scene.getStylesheets());
      }
    });

    final FilterableListView<IonPartReference> partList = createUnrankedIonsList();
    final TableView<IonPartFrequency> table = createRankingTable();

    final Button addButton = FxButtons.createButton("Add selected", FxIcons.ARROW_RIGHT,
        "Add the selected building blocks to the ranking and ask for the frequency of each",
        () -> addSelected(partList.getSelectedItems()));
    final Button removeButton = FxButtons.createButton("Remove", FxIcons.ARROW_LEFT,
        "Remove the selected entries from the ranking",
        () -> entries.removeAll(List.copyOf(table.getSelectionModel().getSelectedItems())));
    final Button defaultsButton = FxButtons.createButton("Restore defaults", FxIcons.RELOAD,
        "Replace the ranking with the mzmine default",
        () -> entries.setAll(IonTypeRanking.createDefault().getFrequencies()));

    // fillWidth so the list and the table use the full width of their column on resize
    final VBox leftBox = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true,
        FxLabels.newBoldLabel("Available ion building blocks"), partList);
    VBox.setVgrow(partList, Priority.ALWAYS);

    final VBox rightBox = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true,
        FxLabels.newBoldLabel("Ranking"), table,
        FxLayout.newHBox(Insets.EMPTY, removeButton, defaultsButton), createManualEntryRow());
    VBox.setVgrow(table, Priority.ALWAYS);

    final VBox middleBox = FxLayout.newVBox(Pos.CENTER, Insets.EMPTY, addButton);

    final HBox content = FxLayout.newHBox(Insets.EMPTY, leftBox, middleBox, rightBox);
    // only the two lists take the additional width, the add button stays as wide as its label
    HBox.setHgrow(leftBox, Priority.ALWAYS);
    HBox.setHgrow(rightBox, Priority.ALWAYS);
    HBox.setHgrow(middleBox, Priority.NEVER);

    getDialogPane().setContent(content);
    getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

    setResultConverter(buttonType -> buttonType == okButtonType ? List.copyOf(entries) : null);
  }

  /**
   * The ion parts that are currently in the global ion library, plus the ones already ranked so a
   * ranking entry never disappears just because the global library changed.
   * <p>
   * decision: only the count direction a part is defined with is offered, so +H and -H2O show up
   * but +H2O does not. {@link IonPartReference#of(IonPart)} reduces the count to its sign, which
   * also collapses +H, +2H and +3H into the one entry +H. The opposite direction can still be typed
   * in the manual entry row.
   */
  private static @NotNull List<IonPartReference> collectAvailableParts(
      final @NotNull List<IonPartFrequency> current) {
    final GlobalIonLibraryService global = GlobalIonLibraryService.getGlobalLibrary();
    // linked set keeps insertion order stable, the list view sorts anyway
    final Set<IonPartReference> references = new LinkedHashSet<>();
    for (final IonPart part : global.getIonPartsUnmodifiable()) {
      references.add(IonPartReference.of(part));
    }
    for (final IonPartFrequency entry : current) {
      references.add(entry.part());
    }
    return new ArrayList<>(references);
  }

  /**
   * The list of building blocks that may still be added, so all collected parts minus the ones that
   * are already in the ranking. The filter is re-evaluated whenever the ranking changes.
   */
  private @NotNull FilterableListView<IonPartReference> createUnrankedIonsList() {
    final FilterableListView<IonPartReference> partList = FxListViews.newFilterableListView(
        available, false, SelectionMode.MULTIPLE);
    partList.sortingComparatorProperty().set(IonPartReference.SORTER);
    partList.setPrefWidth(250);
    partList.setMaxWidth(Double.MAX_VALUE);

    // hide everything that is already ranked, re-evaluate whenever the ranking changes
    final Runnable updateFilter = () -> {
      final Set<IonPartReference> used = entries.stream().map(IonPartFrequency::part)
          .collect(Collectors.toSet());
      partList.externalFilterPredicateProperty().set(reference -> !used.contains(reference));
    };
    entries.addListener((ListChangeListener<IonPartFrequency>) _ -> updateFilter.run());
    updateFilter.run();

    return partList;
  }

  private @NotNull TableView<IonPartFrequency> createRankingTable() {
    // fixed comparator instead of column sorting - the ranking order is the frequency order
    final SortedList<IonPartFrequency> sorted = new SortedList<>(entries,
        IonPartFrequency.MOST_FREQUENT_FIRST);

    final TableView<IonPartFrequency> table = new TableView<>(sorted);
    table.setEditable(true);
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.setPrefHeight(320);
    table.setPrefWidth(280);
    table.setMaxWidth(Double.MAX_VALUE);
    table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    table.setPlaceholder(FxLabels.newLabel("No entry - all building blocks count as frequency 0"));

    final TableColumn<IonPartFrequency, String> partColumn = new TableColumn<>("Building block");
    partColumn.setCellValueFactory(
        data -> new ReadOnlyObjectWrapper<>(data.getValue().part().toString()));
    partColumn.setEditable(false);
    // the additional width of the table goes to the name, the frequency needs a fixed width only
    partColumn.setMinWidth(120);

    final TableColumn<IonPartFrequency, Float> frequencyColumn = new TableColumn<>("Frequency");
    frequencyColumn.setCellValueFactory(
        data -> new ReadOnlyObjectWrapper<>(data.getValue().frequency()));
    frequencyColumn.setCellFactory(TextFieldTableCell.forTableColumn(new FloatStringConverter()));
    frequencyColumn.setEditable(true);
    frequencyColumn.setMaxWidth(110);
    frequencyColumn.setOnEditCommit(event -> {
      final Float frequency = event.getNewValue();
      if (frequency == null) {
        return;
      }
      // the table shows the sorted view, map back to the backing list before replacing
      final int sourceIndex = sorted.getSourceIndex(event.getTablePosition().getRow());
      final IonPartFrequency old = entries.get(sourceIndex);
      entries.set(sourceIndex, new IonPartFrequency(old.part(), frequency));
    });

    table.getColumns().add(partColumn);
    table.getColumns().add(frequencyColumn);
    return table;
  }

  /**
   * Lets the user type a building block that the global library does not know, e.g. +Ca+2 with the
   * frequency 0.4. Only a parsable definition is added, see {@link IonParts#parseSilent(String)}.
   */
  private @NotNull HBox createManualEntryRow() {
    final TextField partField = FxTextFields.newTextField(10, manualPart, "+Ca+2",
        "Ion building block to add, e.g. +Ca+2, -H2O or +CH3CN");
    final TextField frequencyField = FxTextFields.newTextField(5, manualFrequency, "0.4",
        "How often the building block is observed, 1 is the most common");
    final Button addManualButton = FxButtons.createButton("Add ranking", FxIcons.ADD,
        "Add the typed building block with the typed frequency to the ranking",
        this::addManualEntry);
    addManualButton.disableProperty().bind(manualPart.isEmpty().or(manualFrequency.isEmpty()));

    // enter in either field adds as well
    partField.setOnAction(_ -> addManualEntry());
    frequencyField.setOnAction(_ -> addManualEntry());

    final HBox row = FxLayout.newHBox(Insets.EMPTY, partField, frequencyField, addManualButton);
    HBox.setHgrow(partField, Priority.ALWAYS);
    return row;
  }

  private void addManualEntry() {
    final IonPart part = IonParts.parseSilent(manualPart.get().trim());
    if (part == null) {
      DialogLoggerUtil.showWarningDialog("Cannot parse ion building block",
          "%s is no valid ion building block. Use a format like +Ca+2, -H2O or +CH3CN.".formatted(
              manualPart.get()));
      return;
    }
    final Float frequency = parseFrequency(manualFrequency.get());
    if (frequency == null) {
      DialogLoggerUtil.showWarningDialog("Cannot parse frequency",
          "%s is no valid frequency. Use a positive number like 0.4.".formatted(
              manualFrequency.get()));
      return;
    }

    putEntry(IonPartReference.of(part), frequency);
    // only the definition is cleared, the frequency is usually reused for the next entry
    manualPart.set("");
  }

  /**
   * Asks for the frequency of every selected building block, one dialog at a time. Cancelling stops
   * at that building block and keeps the ones added before.
   */
  private void addSelected(final @Nullable List<IonPartReference> selected) {
    if (selected == null || selected.isEmpty()) {
      return;
    }
    final Set<IonPartReference> used = entries.stream().map(IonPartFrequency::part)
        .collect(Collectors.toSet());
    for (final IonPartReference reference : List.copyOf(selected)) {
      if (!used.add(reference)) {
        continue;
      }
      final Float frequency = askFrequency(reference);
      if (frequency == null) {
        return;
      }
      entries.add(new IonPartFrequency(reference, frequency));
    }
  }

  /**
   * @return the frequency the user entered for this building block or null if the user cancelled
   */
  private static @Nullable Float askFrequency(final @NotNull IonPartReference reference) {
    String input = String.valueOf(DEFAULT_NEW_FREQUENCY);
    while (true) {
      final TextInputDialog dialog = DialogLoggerUtil.createTextInputDialog(
          "Frequency of " + reference,
          "How often is %s observed? 1 is as common as the most frequent building block.".formatted(
              reference), "Frequency");
      dialog.getEditor().setText(input);
      final Optional<String> result = dialog.showAndWait();
      if (result.isEmpty()) {
        return null;
      }
      input = result.get();
      final Float frequency = parseFrequency(input);
      if (frequency != null) {
        return frequency;
      }
      DialogLoggerUtil.showWarningDialog("Cannot parse frequency",
          "%s is no valid frequency. Use a positive number like 0.4.".formatted(input));
    }
  }

  /**
   * Adds the entry or replaces the frequency if the building block is already ranked, so typing a
   * known building block again is an update instead of a silently ignored click.
   */
  private void putEntry(final @NotNull IonPartReference reference, final float frequency) {
    final IonPartFrequency entry = new IonPartFrequency(reference, frequency);
    for (int i = 0; i < entries.size(); i++) {
      if (entries.get(i).part().equals(reference)) {
        entries.set(i, entry);
        return;
      }
    }
    entries.add(entry);
  }

  /**
   * @return the parsed frequency or null if it is no finite number of at least 0
   */
  private static @Nullable Float parseFrequency(final @Nullable String input) {
    if (input == null) {
      return null;
    }
    try {
      final float frequency = Float.parseFloat(input.trim());
      return Float.isFinite(frequency) && frequency >= 0 ? frequency : null;
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
