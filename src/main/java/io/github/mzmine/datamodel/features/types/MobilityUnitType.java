/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.SimpleRowBinding;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MobilityUnitType extends DataType<MobilityType> {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "ion_mobility_unit";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Mobility unit";
  }

  @Override
  public SimpleObjectProperty<MobilityType> createProperty() {
    return new SimpleObjectProperty<>(MobilityType.NONE);
  }

  @Override
  public Class<MobilityType> getValueClass() {
    return MobilityType.class;
  }

  @Override
  public @NotNull String getFormattedString(MobilityType value) {
    return value == null? "" : value.getUnit();
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if(value == null) {
      return;
    }
    if(!(value instanceof MobilityType mt)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: " + value.getClass());
    }
    writer.writeCharacters(mt.name());
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    String name = reader.getElementText();
    if (name.isEmpty()) {
      return null;
    }
    return MobilityType.valueOf(name);
  }

  @Override
  public @NotNull List<RowBinding> createDefaultRowBindings() {
    return List.of(new SimpleRowBinding(this, BindingsType.CONSENSUS));
  }

  public Object evaluateBindings(@NotNull BindingsType bindingType,
      @NotNull List<? extends ModularDataModel> models) {
    if (bindingType == BindingsType.CONSENSUS) {
      MobilityType unit = null;
      for (var model : models) {
        final MobilityType tmpUnit = model.get(this);
        if (tmpUnit != null) {
          if (unit == null) {
            unit = tmpUnit;
          } else if (unit != tmpUnit) {
            return MobilityType.MIXED;
          }
        }
      }
      return unit == null ? MobilityType.NONE : unit;
    } else {
      return super.evaluateBindings(bindingType, models);
    }
  }
}
