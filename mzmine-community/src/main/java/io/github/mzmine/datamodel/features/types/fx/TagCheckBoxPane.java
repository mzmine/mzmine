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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.function.BiConsumer;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shared wrapping layout for the table and filter tag checkboxes.
 */
public final class TagCheckBoxPane extends FlowPane {

  public static final double DEFAULT_COLUMN_WIDTH = 66;
  public static final double CHECKBOX_WIDTH = 14;
  public static final double HORIZONTAL_GAP = 2;
  private static final double COLUMN_INSET = 0;
  private static final double WRAP_SLACK = 6;
  private final @NotNull List<CheckBox> checkBoxes = new ArrayList<>();
  private @NotNull BitSet selectedTags = new BitSet();
  private @Nullable BiConsumer<Integer, Boolean> onSelectionChanged;
  private int compactColumns;
  private double columnWidth = DEFAULT_COLUMN_WIDTH;
  private boolean synchronizing;

  public TagCheckBoxPane(@NotNull final Pos alignment) {
    setAlignment(alignment);
    setHgap(HORIZONTAL_GAP);
    setVgap(HORIZONTAL_GAP);
    backgroundProperty().bind(Bindings.createObjectBinding(() -> Background.EMPTY));
    borderProperty().bind(Bindings.createObjectBinding(() -> Border.EMPTY));
  }

  public void setTagLabels(@NotNull final List<String> labels) {
    final boolean countChanged = checkBoxes.size() != labels.size();
    while (checkBoxes.size() < labels.size()) {
      addCheckBox(checkBoxes.size());
    }
    while (checkBoxes.size() > labels.size()) {
      checkBoxes.removeLast();
    }
    for (int index = 0; index < labels.size(); index++) {
      TagCheckBoxFactory.setLabel(checkBoxes.get(index), labels.get(index));
    }
    selectedTags.clear(labels.size(), Math.max(labels.size(), selectedTags.length()));
    if (countChanged) {
      getChildren().setAll(checkBoxes);
    }
    updateWrapWidth();
  }

  public int getTagCount() {
    return checkBoxes.size();
  }

  public @NotNull CheckBox getCheckBox(final int index) {
    return checkBoxes.get(index);
  }

  public @NotNull BitSet getSelectedTags() {
    return (BitSet) selectedTags.clone();
  }

  public void setSelectedTags(@Nullable final BitSet tags) {
    selectedTags = tags == null ? new BitSet() : (BitSet) tags.clone();
    selectedTags.clear(checkBoxes.size(), Math.max(checkBoxes.size(), selectedTags.length()));
    synchronizing = true;
    try {
      for (int index = 0; index < checkBoxes.size(); index++) {
        checkBoxes.get(index).setSelected(selectedTags.get(index));
      }
    } finally {
      synchronizing = false;
    }
  }

  public void setOnSelectionChanged(@Nullable final BiConsumer<Integer, Boolean> handler) {
    onSelectionChanged = handler;
  }

  private void addCheckBox(final int index) {
    final CheckBox checkBox = TagCheckBoxFactory.create(index, "");
    if (index == 0) {
      // CSS may change at runtime, including when Presentation mode is toggled.
      checkBox.fontProperty().addListener((_, _, _) -> updateWrapWidth());
      checkBox.layoutBoundsProperty().addListener((_, _, _) -> updateWrapWidth());
    }
    checkBox.selectedProperty().addListener((_, _, selected) -> {
      if (synchronizing || index >= checkBoxes.size() || checkBoxes.get(index) != checkBox) {
        return;
      }
      selectedTags.set(index, selected);
      if (onSelectionChanged != null) {
        onSelectionChanged.accept(index, selected);
      }
    });
    checkBoxes.add(checkBox);
  }

  public void setCompactColumns(final int columns) {
    if (columns < 1) {
      throw new IllegalArgumentException("Column count must be positive: " + columns);
    }
    compactColumns = columns;
    updateWrapWidth();
  }

  public void setColumnWidth(final double width) {
    compactColumns = 0;
    columnWidth = width;
    updateWrapWidth();
  }

  public int columnsForColumnWidth(final double width) {
    final double checkboxWidth = checkboxWidth();
    final double available = Math.max(checkboxWidth, width - COLUMN_INSET);
    final double gap = snapSpaceX(getHgap());
    return Math.max(1, (int) Math.floor((available + gap) / (checkboxWidth + gap)));
  }

  private void updateWrapWidth() {
    final double checkboxWidth = checkboxWidth();
    final double width;
    if (compactColumns > 0) {
      // decision: measure the CSS-sized control, not a fixed pixel value that can clip its box.
      width =
          compactColumns * checkboxWidth + Math.max(0, compactColumns - 1) * snapSpaceX(getHgap())
              + WRAP_SLACK;
    } else {
      // decision: the table cell and filter popover wrap at the same column width.
      width = Math.max(checkboxWidth, columnWidth - COLUMN_INSET);
    }
    setWrapWidth(width);
  }

  private double checkboxWidth() {
    return checkBoxes.isEmpty() ? snapSizeX(CHECKBOX_WIDTH)
        : Math.max(snapSizeX(CHECKBOX_WIDTH), snapSizeX(checkBoxes.getFirst().prefWidth(-1)));
  }

  @Override
  protected void layoutChildren() {
    sizeCheckboxesToVisibleBoxHeight();
    super.layoutChildren();
  }

  private void sizeCheckboxesToVisibleBoxHeight() {
    if (checkBoxes.isEmpty() || !(checkBoxes.getFirst().lookup(".box") instanceof Region box)) {
      return;
    }
    // decision: the empty checkbox label still contributes font height; rows should follow the box.
    final double boxHeight = snapSizeY(box.prefHeight(-1));
    if (boxHeight <= 0) {
      return;
    }
    for (final CheckBox checkBox : checkBoxes) {
      if (Double.compare(checkBox.getPrefHeight(), boxHeight) == 0) {
        continue;
      }
      checkBox.setMinHeight(boxHeight);
      checkBox.setPrefHeight(boxHeight);
      checkBox.setMaxHeight(boxHeight);
    }
  }

  private void setWrapWidth(final double width) {
    if (Double.compare(getPrefWrapLength(), width) == 0
        && Double.compare(getPrefWidth(), width) == 0) {
      return;
    }
    setMinWidth(width);
    setPrefWidth(width);
    setMaxWidth(width);
    setPrefWrapLength(width);
  }
}
