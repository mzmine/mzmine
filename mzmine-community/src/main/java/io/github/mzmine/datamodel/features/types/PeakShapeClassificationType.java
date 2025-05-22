/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.RowBinding;
import io.github.mzmine.datamodel.features.SimpleRowBinding;
import io.github.mzmine.datamodel.features.types.exceptions.UndefinedRowBindingException;
import io.github.mzmine.datamodel.features.types.modifiers.BindingsType;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.modules.dataprocessing.filter_featurefilter.peak_fitter.PeakShapeClassification;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PeakShapeClassificationType extends DataType<PeakShapeClassification> {

  @Override
  public @NotNull String getUniqueID() {
    return "peak_shape_class";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Shape type";
  }

  @Override
  public Property<PeakShapeClassification> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<PeakShapeClassification> getValueClass() {
    return PeakShapeClassification.class;
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    final String text = reader.getElementText();
    if (text != null && !text.isEmpty()) {
      return UniqueIdSupplier.parseOrElse(text, PeakShapeClassification.values(), null);
    }
    return null;
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (value == null) {
      return;
    }
    if (!(value instanceof PeakShapeClassification shapeType)) {
      throw new RuntimeException(
          "Illegal value type for PeakShapeClassificationType: " + value.getClass() + " "
              + value.toString());
    }
    writer.writeCharacters(shapeType.getUniqueID());
  }

  @Override
  public @NotNull List<RowBinding> createDefaultRowBindings() {
    return List.of(new SimpleRowBinding(DataTypes.get(PeakShapeClassificationType.class),
        BindingsType.CONSENSUS));
  }

  @Override
  public Object evaluateBindings(@NotNull BindingsType bindingType,
      @NotNull List<? extends ModularDataModel> models) {
    return switch (bindingType) {
      case AVERAGE, MIN, MAX, SUM, DIFFERENCE, COUNT, RANGE, LIST ->
          throw new UndefinedRowBindingException(this, bindingType);
      case CONSENSUS -> {
        yield models.stream().map(m -> m.get(this)).filter(Objects::nonNull)
            .collect(Collectors.groupingBy(model -> model, Collectors.counting())).entrySet()
            .stream().max(Comparator.comparingLong(Entry::getValue)).map(Entry::getKey)
            .orElse(null);
      }
    };
  }
}
