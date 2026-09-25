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

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.fx.TagCheckBoxPane;
import io.github.mzmine.datamodel.features.types.fx.TagTreeTableCell;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import java.util.BitSet;
import java.util.Comparator;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeTableColumn;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Row tags represented by an interactive, wrapping row of checkboxes in the feature table.
 */
public class TagDataType extends DataType<BitSet> {

  public static final Comparator<Object> comparator = (oa, ob) -> {
    if (!(oa instanceof BitSet a) || !(ob instanceof BitSet b)) {
      if (oa instanceof BitSet) {
        return 1;
      }
      if (ob instanceof BitSet) {
        return -1;
      }
      return 0;
    }

    final int minLen = Math.min(a.length(), b.length());

    int compare = 0;
    for (int i = 0; i < minLen; i++) {
      compare = Boolean.compare(a.get(i), b.get(i));
      if (compare != 0) {
        break;
      }
    }
    return compare != 0 ? compare : Integer.compare(a.length(), b.length());
  };
  private static final Logger logger = Logger.getLogger(TagDataType.class.getName());

  @Override
  public @NotNull String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type.
    return "row_tags";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Tag";
  }

  @Override
  public @NotNull Class<BitSet> getValueClass() {
    return BitSet.class;
  }

  @Override
  public @NotNull ObjectProperty<BitSet> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public double getPrefColumnWidth() {
    // + 10 is to write the full "Tag" label in the header
    return TagCheckBoxPane.CHECKBOX_WIDTH + 12;
  }

  @Override
  public @Nullable TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      @Nullable final RawDataFile raw, @Nullable final SubColumnsFactory parentType,
      final int subColumnIndex) {
    final TreeTableColumn<ModularFeatureListRow, Object> column = super.createColumn(raw,
        parentType, subColumnIndex);
    if (column != null) {
      column.setComparator(comparator);
      column.setCellFactory(_ -> new TagTreeTableCell(this));
      // The generic value factory omits null values. Tags need an empty value so an unchecked grid
      // is visible before the first tag is selected.
      column.setCellValueFactory(data -> {
        final ModularFeatureListRow row = data.getValue().getValue();
        final BitSet tags = row.get(TagDataType.this);
        return new SimpleObjectProperty<>(tags == null ? new BitSet() : tags);
      });
    }
    return column;
  }

  @Override
  public @NotNull String getFormattedString(@Nullable final BitSet value, final boolean export) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    return value.stream().mapToObj(index -> Integer.toString(index + 1))
        .collect(Collectors.joining(", "));
  }

  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    if (value == null) {
      return;
    }
    if (!(value instanceof BitSet tags)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type %s: %s".formatted(getClass().getName(),
              value.getClass().getName()));
    }
    writer.writeCharacters(
        tags.stream().mapToObj(Integer::toString).collect(Collectors.joining(",")));
  }

  @Override
  public @Nullable Object loadFromXML(@NotNull final XMLStreamReader reader,
      @NotNull final MZmineProject project, @NotNull final ModularFeatureList flist,
      @NotNull final ModularFeatureListRow row, @Nullable final ModularFeature feature,
      @Nullable final RawDataFile file) throws XMLStreamException {
    final String text = reader.getElementText();
    if (text.isBlank()) {
      return null;
    }

    final BitSet tags = new BitSet();
    for (final String token : text.split(",")) {
      try {
        final int index = Integer.parseInt(token.trim());
        if (index >= 0) {
          tags.set(index);
        }
      } catch (NumberFormatException e) {
        logger.warning("Cannot parse row tag index from XML: " + token);
      }
    }
    return tags;
  }
}
