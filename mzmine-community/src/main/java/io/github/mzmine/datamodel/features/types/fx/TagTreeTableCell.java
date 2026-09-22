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

package io.github.mzmine.datamodel.features.types.fx;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.features.types.TagDataType;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TreeTableCell;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reusable feature-table cell for the checkbox grid of a {@link TagDataType}.
 */
public class TagTreeTableCell extends TreeTableCell<ModularFeatureListRow, Object> {

  private static final int GRID_WIDTH = 3;

  private final @NotNull TagDataType type;
  private final @NotNull GridPane grid = new GridPane();
  private final @NotNull List<CheckBox> checkBoxes = new ArrayList<>();
  private final @NotNull List<BooleanProperty> tagStates = new ArrayList<>();

  private @NotNull List<String> labels = List.of();
  private @NotNull BitSet currentTags = new BitSet();
  private boolean synchronizing;

  public TagTreeTableCell(@NotNull final TagDataType type) {
    this.type = type;

    grid.setAlignment(Pos.CENTER);
    grid.setHgap(2);
    grid.setVgap(1);
    setAlignment(Pos.CENTER);
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    grid.backgroundProperty().bind(Bindings.createObjectBinding(() -> Background.EMPTY));
    grid.borderProperty().bind(Bindings.createObjectBinding(() -> Border.EMPTY));

    // Keep one grid for the lifetime of the virtualized cell and only switch its visibility.
    graphicProperty().bind(
        Bindings.createObjectBinding(() -> isEmpty() ? null : grid, emptyProperty()));
  }

  @Override
  protected void updateItem(@Nullable final Object value, final boolean empty) {
    super.updateItem(value, empty);
    final ModularFeatureListRow row = getTableRow() == null ? null : getTableRow().getItem();
    if (empty || row == null) {
      return;
    }

    updateLabels(row.getFeatureList().getPreferences().getTagLabels());
    currentTags = value instanceof BitSet tags ? (BitSet) tags.clone() : new BitSet();
    updateTagStates();
  }

  private void updateLabels(@NotNull final List<String> newLabels) {
    if (labels.equals(newLabels)) {
      return;
    }
    labels = List.copyOf(newLabels);

    while (checkBoxes.size() < labels.size()) {
      addCheckBox(checkBoxes.size());
    }
    while (checkBoxes.size() > labels.size()) {
      final CheckBox checkBox = checkBoxes.removeLast();
      final BooleanProperty state = tagStates.removeLast();
      checkBox.selectedProperty().unbindBidirectional(state);
    }

    for (int index = 0; index < labels.size(); index++) {
      final CheckBox checkBox = checkBoxes.get(index);
      final String label = labels.get(index);
      TagCheckBoxFactory.setLabel(checkBox, label);
      GridPane.setColumnIndex(checkBox, index % GRID_WIDTH);
      GridPane.setRowIndex(checkBox, index / GRID_WIDTH);
    }
    grid.getChildren().setAll(checkBoxes);
  }

  private void addCheckBox(final int tagIndex) {
    final BooleanProperty state = new SimpleBooleanProperty(false);
    final CheckBox checkBox = TagCheckBoxFactory.create(tagIndex, "");
    checkBox.selectedProperty().bindBidirectional(state);
    state.addListener((_, _, _) -> updateRowValue());
    tagStates.add(state);
    checkBoxes.add(checkBox);
  }

  private void updateTagStates() {
    synchronizing = true;
    try {
      for (int index = 0; index < tagStates.size(); index++) {
        tagStates.get(index).set(currentTags.get(index));
      }
    } finally {
      synchronizing = false;
    }
  }

  private void updateRowValue() {
    if (synchronizing || isEmpty() || getTableRow() == null) {
      return;
    }
    final ModularFeatureListRow row = getTableRow().getItem();
    if (row == null) {
      return;
    }

    for (int index = 0; index < tagStates.size(); index++) {
      currentTags.set(index, tagStates.get(index).get());
    }
    final FeatureListRow targetRow;
    if (row instanceof CompoundRow compoundRow) {
      // decision: tags edited on a compound row belong to its representative row.
      row.remove(type);
      targetRow = compoundRow.getPreferredRow();
    } else {
      targetRow = row;
    }

    // Store a copy so later UI changes cannot mutate the data model without notification.
    targetRow.set(type, (BitSet) currentTags.clone());
    if (getTreeTableView() != null) {
      getTreeTableView().refresh();
    }
  }
}
