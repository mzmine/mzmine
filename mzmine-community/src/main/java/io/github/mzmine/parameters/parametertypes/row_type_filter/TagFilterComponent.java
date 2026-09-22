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

package io.github.mzmine.parameters.parametertypes.row_type_filter;

import io.github.mzmine.datamodel.features.types.fx.TagCheckBoxFactory;
import io.github.mzmine.parameters.ValuePropertyComponent;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Checkbox-grid editor for the tag selection of a row-type filter.
 */
public final class TagFilterComponent extends GridPane implements ValuePropertyComponent<BitSet> {

  private static final int GRID_WIDTH = 3;

  private final @NotNull ObjectProperty<BitSet> value = new SimpleObjectProperty<>(new BitSet());
  private final @NotNull List<CheckBox> checkBoxes = new ArrayList<>();
  private boolean synchronizing;

  public TagFilterComponent(final int tagCount) {
    setAlignment(Pos.CENTER_LEFT);
    setHgap(2);
    setVgap(1);
    value.addListener((_, _, tags) -> updateCheckBoxes(tags));
    setTagCount(tagCount);
  }

  public void setTagCount(final int tagCount) {
    if (tagCount < 0) {
      throw new IllegalArgumentException("Tag count must not be negative: " + tagCount);
    }
    if (checkBoxes.size() == tagCount) {
      return;
    }

    while (checkBoxes.size() < tagCount) {
      addCheckBox(checkBoxes.size());
    }
    while (checkBoxes.size() > tagCount) {
      checkBoxes.removeLast();
    }

    for (int index = 0; index < checkBoxes.size(); index++) {
      final CheckBox checkBox = checkBoxes.get(index);
      TagCheckBoxFactory.setLabel(checkBox, "Tag index " + index);
      GridPane.setColumnIndex(checkBox, index % GRID_WIDTH);
      GridPane.setRowIndex(checkBox, index / GRID_WIDTH);
    }
    getChildren().setAll(checkBoxes);

    final BitSet trimmed = getValue();
    trimmed.clear(tagCount, Math.max(tagCount, trimmed.length()));
    setValue(trimmed);
  }

  public void ensureTagCount(final int tagCount) {
    if (tagCount <= checkBoxes.size()) {
      return;
    }
    setTagCount(tagCount);
  }

  private void addCheckBox(final int tagIndex) {
    final CheckBox checkBox = TagCheckBoxFactory.create(tagIndex, "");
    checkBox.selectedProperty().addListener((_, _, _) -> updateValue());
    checkBoxes.add(checkBox);
  }

  private void updateValue() {
    if (synchronizing) {
      return;
    }
    final BitSet selectedTags = new BitSet(checkBoxes.size());
    for (int index = 0; index < checkBoxes.size(); index++) {
      selectedTags.set(index, checkBoxes.get(index).isSelected());
    }
    value.set(selectedTags);
  }

  private void updateCheckBoxes(@Nullable final BitSet selectedTags) {
    synchronizing = true;
    try {
      for (int index = 0; index < checkBoxes.size(); index++) {
        checkBoxes.get(index).setSelected(selectedTags != null && selectedTags.get(index));
      }
    } finally {
      synchronizing = false;
    }
  }

  public @NotNull BitSet getValue() {
    return (BitSet) value.get().clone();
  }

  public void setValue(@Nullable final BitSet selectedTags) {
    final BitSet copy = selectedTags == null ? new BitSet() : (BitSet) selectedTags.clone();
    copy.clear(checkBoxes.size(), Math.max(checkBoxes.size(), copy.length()));
    value.set(copy);
    updateCheckBoxes(copy);
  }

  @Override
  public @NotNull Property<BitSet> valueProperty() {
    return value;
  }
}
