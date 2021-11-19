/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.FeatureStatus;
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
  public Object loadFromXML(@NotNull XMLStreamReader reader,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    String elementText = reader.getElementText();
    if(elementText.isEmpty()) {
      return null;
    }
    return FeatureStatus.valueOf(elementText);
  }
}
