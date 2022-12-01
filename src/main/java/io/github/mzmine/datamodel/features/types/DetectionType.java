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

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.shape.Circle;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DetectionType extends DataType<FeatureStatus> implements
    GraphicalColumType<FeatureStatus> {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "feature_state";
  }

  @Override
  public @Nullable FeatureStatus getDefaultValue() {
    return FeatureStatus.UNKNOWN;
  }

  @Override
  public @NotNull String getFormattedString(FeatureStatus value) {
    return value == null ? getDefaultValue().toString() : value.toString();
  }

  @Override
  @NotNull
  public String getHeaderString() {
    return "State";
  }

  @Override
  public Class<FeatureStatus> getValueClass() {
    return FeatureStatus.class;
  }

  @Override
  public Node getCellNode(TreeTableCell<ModularFeatureListRow, FeatureStatus> cell,
      TreeTableColumn<ModularFeatureListRow, FeatureStatus> coll, FeatureStatus cellData,
      RawDataFile raw) {
    if (cellData == null) {
      return null;
    }

    Circle circle = new Circle();
    circle.setRadius(10);
    circle.setFill(cellData.getColorFX());
    return circle;
  }

  @Override
  public double getColumnWidth() {
    return 10;
  }

  @Override
  public ObjectProperty<FeatureStatus> createProperty() {
    return new SimpleObjectProperty<>(FeatureStatus.UNKNOWN);
  }

  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    if(value == null) {
      return;
    }
    if (!(value instanceof FeatureStatus status)) {
      throw new IllegalArgumentException(
          "Wrong value type for data type: " + this.getClass().getName() + " value class: " + value.getClass());
    }
    writer.writeCharacters(status.toString());
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    String elementText = reader.getElementText();
    if (elementText.isEmpty()) {
      return null;
    }
    return FeatureStatus.valueOf(elementText);
  }
}
