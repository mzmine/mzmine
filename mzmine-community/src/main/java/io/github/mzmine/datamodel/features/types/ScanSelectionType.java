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
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Records the {@link ScanSelection} a {@link ModularFeatureListRow} (and thereby its features) was
 * derived from. Stored on rows only - features resolve their selection via their row - to conserve
 * per-feature memory. Hidden from the feature table ({@link NullColumnType}).
 * <p>
 * The value is persisted compactly as the selection's stable index in the feature list's
 * {@link io.github.mzmine.datamodel.features.FeatureListScans}. The full {@link ScanSelection}
 * objects are serialized once at the feature list level (raw data files block).
 */
public class ScanSelectionType extends DataType<ScanSelection> implements NullColumnType {

  @Override
  public @NotNull String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "scan_selection";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Scan selection";
  }

  @Override
  public ObjectProperty<ScanSelection> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<ScanSelection> getValueClass() {
    return ScanSelection.class;
  }

  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    if (!(value instanceof ScanSelection selection)) {
      return;
    }
    // store the stable index into the feature list's scan selection registry. The selections
    // themselves are serialized at the feature list level (raw data files block).
    final int index = flist.getSelectedScansData().indexOf(selection);
    if (index < 0) {
      return;
    }
    writer.writeCharacters(String.valueOf(index));
  }

  @Override
  public Object loadFromXML(@NotNull final XMLStreamReader reader,
      @NotNull final MZmineProject project, @NotNull final ModularFeatureList flist,
      @NotNull final ModularFeatureListRow row, @Nullable final ModularFeature feature,
      @Nullable final RawDataFile file) throws XMLStreamException {
    final String str = reader.getElementText();
    if (str == null || str.isBlank()) {
      return null;
    }
    return flist.getSelectedScansData().getSelectionByIndex(Integer.parseInt(str.trim()));
  }
}
