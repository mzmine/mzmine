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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TreeTableCell;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reusable feature-table cell for the wrapping checkboxes of a {@link TagDataType}.
 */
public class TagTreeTableCell extends TreeTableCell<ModularFeatureListRow, Object> {

  private final @NotNull TagDataType type;
  private final @NotNull TagCheckBoxPane checkBoxPane = new TagCheckBoxPane(Pos.CENTER);

  private @NotNull List<String> labels = List.of();

  public TagTreeTableCell(@NotNull final TagDataType type) {
    this.type = type;

    setAlignment(Pos.CENTER);
    setPadding(Insets.EMPTY);
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    widthProperty().addListener((_, _, width) -> checkBoxPane.setColumnWidth(width.doubleValue()));
    checkBoxPane.setOnSelectionChanged((_, _) -> updateRowValue());

    // Keep one pane for the lifetime of the virtualized cell and only switch its visibility.
    graphicProperty().bind(
        Bindings.createObjectBinding(() -> isEmpty() ? null : checkBoxPane, emptyProperty()));
  }

  static @NotNull List<String> labelsForTags(@NotNull final List<String> configuredLabels,
      @Nullable final BitSet tags) {
    final int checkboxCount = Math.max(configuredLabels.size(), tags == null ? 0 : tags.length());
    if (checkboxCount == configuredLabels.size()) {
      return configuredLabels;
    }
    // decision: unexpected stored tags remain visible and editable, so editing cannot discard them.
    final List<String> displayLabels = new ArrayList<>(configuredLabels);
    while (displayLabels.size() < checkboxCount) {
      displayLabels.add("");
    }
    return displayLabels;
  }

  @Override
  protected void updateItem(@Nullable final Object value, final boolean empty) {
    super.updateItem(value, empty);
    checkBoxPane.setColumnWidth(getWidth());
    final ModularFeatureListRow row = getTableRow() == null ? null : getTableRow().getItem();
    if (empty || row == null) {
      return;
    }

    final BitSet tags = value instanceof BitSet bitSet ? bitSet : null;
    updateLabels(labelsForTags(row.getFeatureList().getPreferences().getTagLabels(), tags));
    checkBoxPane.setSelectedTags(tags);
  }

  private void updateLabels(@NotNull final List<String> newLabels) {
    if (labels.equals(newLabels)) {
      return;
    }
    labels = List.copyOf(newLabels);
    checkBoxPane.setTagLabels(labels);
  }

  private void updateRowValue() {
    if (isEmpty() || getTableRow() == null) {
      return;
    }
    final ModularFeatureListRow row = getTableRow().getItem();
    if (row == null) {
      return;
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
    targetRow.set(type, checkBoxPane.getSelectedTags());
    if (getTreeTableView() != null) {
      getTreeTableView().refresh();
    }
  }
}
