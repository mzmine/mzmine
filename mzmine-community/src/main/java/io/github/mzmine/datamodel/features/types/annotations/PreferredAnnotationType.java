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

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.abstr.SimpleSubColumnsType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.fx.PreferredEditComboCellFactory;
import io.github.mzmine.datamodel.features.types.modifiers.MappingType;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeTableColumn;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This type is only set through user action, so don't set this programmatically.
 * <p>
 * By default, this type is a {@link MappingType} using
 * {@link FeatureListRow#getPreferredAnnotation()} to retrieve either the user-specified preferred
 * annotation (user selected a specific annotation from a list in the GUI) or the most trusted
 * annotation of all {@link FeatureAnnotation} types.
 */
public class PreferredAnnotationType extends SimpleSubColumnsType<FeatureAnnotation> implements
    MappingType<FeatureAnnotation> {

  public static final List<DataType> subTypes = List.of(new PreferredAnnotationType(),
      new CompoundNameType(), new AnnotationSummaryType(), new PrecursorMZType(),
      new MolecularStructureType(), new ScoreType(), new IonTypeType(), new AnnotationMethodType());

  @Override
  public @NotNull String getUniqueID() {
    return "preferred_annotation";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Preferred Annotation";
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }

  @Override
  public Property<FeatureAnnotation> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<FeatureAnnotation> getValueClass() {
    return FeatureAnnotation.class;
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  public @NotNull List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType) {
    // add column for each sub data type
    List<TreeTableColumn<ModularFeatureListRow, Object>> cols = new ArrayList<>();

    List<DataType> subTypes = getSubDataTypes();
    // create column per name
    for (int index = 0; index < subTypes.size(); index++) {
      DataType type = subTypes.get(index);
      if (type instanceof NullColumnType) {
        continue;
      }
      if (this.equals(type)) {
        // create a special column for this type that actually represents the list of data

        TreeTableColumn<ModularFeatureListRow, Object> mainCol = DataType.createStandardColumn(type,
            raw, this, index);
        mainCol.setCellFactory(new PreferredEditComboCellFactory(raw, type, this, index));
        mainCol.setCellValueFactory(
            cdf -> new ReadOnlyObjectWrapper<>(cdf.getValue().getValue().getPreferredAnnotation()));
        mainCol.setPrefWidth(type.getPrefColumnWidth());
        cols.add(mainCol);
      } else {
        // create all other columns
        var col = type.createColumn(raw, this, index);
        // override type in CellValueFactory with this parent type
        cols.add(col);
      }
    }
    return cols;
  }

  @Override
  public @Nullable Object getSubColValue(DataType sub, Object value) {
    if (!(value instanceof FeatureAnnotation a)) {
      return null;
    }
    return CompoundAnnotationUtils.getTypeValue(a, sub);
  }

  @Override
  public @Nullable Object getSubColValue(int subcolumn, Object cellData) {
    DataType dataType = subTypes.get(subcolumn);
    return getSubColValue(dataType, cellData);
  }

  @Override
  public @Nullable FeatureAnnotation getValue(@NotNull ModularDataModel model) {
    return model instanceof FeatureListRow row ? row.getPreferredAnnotation() : null;
  }

  @Override
  public double getPrefColumnWidth() {
    return 150;
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {

    if (value == null || !row.isUserPreferredAnnotation()
        || !(value instanceof FeatureAnnotation annotation)) {
      return;
    }

    annotation.saveToXML(writer, flist, row);
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    if (!(reader.isStartElement() && reader.getLocalName().equals(CONST.XML_DATA_TYPE_ELEMENT)
        && reader.getAttributeValue(null, CONST.XML_DATA_TYPE_ID_ATTR).equals(getUniqueID()))) {
      throw new IllegalStateException("Wrong element");
    }

    FeatureAnnotation id = null;
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      reader.next();
      if (!reader.isStartElement()) {
        continue;
      }

      if (reader.getLocalName().equals(FeatureAnnotation.XML_ELEMENT)) {
        id = FeatureAnnotation.loadFromXML(reader, project, flist, row);
      }
    }

    return id;
  }
}
