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

package io.github.mzmine.datamodel.features.rawfiletypes;

import io.github.mzmine.datamodel.features.types.DataType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Highest possible abundance of a mobility scan data point (detector saturation level) for MS1 and
 * MS2 mobility scans. Agilent calls it the frame spectrum abundance limit (frame spectrum = mzmine
 * mobility scan). The limit does not apply to frames (summed spectra).
 */
public class MobilityScanAbundanceLimitType extends DataType<MobilityScanAbundanceLimits> {

  @Override
  public @NotNull String getUniqueID() {
    return "rawfile_mobility_scan_abundance_limit";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Mobility scan abundance limit";
  }

  @Override
  public Class<MobilityScanAbundanceLimits> getValueClass() {
    return MobilityScanAbundanceLimits.class;
  }

  @Override
  public ObjectProperty<MobilityScanAbundanceLimits> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value)
      throws XMLStreamException {
    if (value == null) {
      return;
    }
    if (!(value instanceof MobilityScanAbundanceLimits limits)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: "
              + value.getClass());
    }
    writer.writeCharacters(limits.toSaveString());
  }

  @Override
  public @Nullable Object loadFromXML(@NotNull final XMLStreamReader reader)
      throws XMLStreamException {
    return MobilityScanAbundanceLimits.parse(reader.getElementText());
  }
}
