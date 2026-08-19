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

package io.github.mzmine.datamodel.features.types.numbers;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.preferences.FeatureListPreferences;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.fx.SampleTypeFilterHeaderColumn;
import io.github.mzmine.datamodel.features.types.modifiers.MappingType;
import io.github.mzmine.datamodel.features.types.modifiers.MinSamplesRequirement;
import io.github.mzmine.datamodel.features.types.modifiers.NoDataColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.abstr.AbstractRsdType;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeTableColumn;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The main type of all {@link AbstractRsdType} sub types, which are the relative standard
 * deviations of the abundance measures over the samples selected in
 * {@link FeatureListPreferences#getRsdSampleTypeFilter()}.
 * <p>
 * The only purpose of the type is to group the RSD columns under one header that lets the user
 * change those sample types, see {@link SampleTypeFilterHeaderColumn}. Its value is only the data
 * model the sub types compute their value from on demand, see {@link MappingType}, and is therefore
 * never stored or saved.
 */
public class SampleRsdType extends DataType<ModularDataModel> implements SubColumnsFactory,
    MappingType<ModularDataModel>, NoDataColumnType, MinSamplesRequirement {

  public static final String UNIQUE_ID = "sample_rsd";

  private static final @NotNull List<DataType> subTypes = List.of(new AreaRsdType(),
      new HeightRsdType(), new NormalizedAreaRsdType(), new NormalizedHeightRsdType());

  @Override
  public @NotNull String getUniqueID() {
    return UNIQUE_ID;
  }

  @Override
  public @NotNull String getHeaderString() {
    // the header only shows the sample type filter, the sub columns carry the titles
    return "RSD";
  }

  @Override
  public Property<ModularDataModel> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<ModularDataModel> getValueClass() {
    return ModularDataModel.class;
  }

  @Override
  public boolean getDefaultVisibility() {
    // the visibility of the sub columns is only applied if this main column is visible
    return true;
  }

  /**
   * @return the model itself, the sub types map their values from it on demand
   */
  @Override
  public @Nullable ModularDataModel getValue(@NotNull final ModularDataModel model) {
    return model;
  }

  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file) {
    // the sub values are computed on demand, there is nothing to persist
  }

  /**
   * @return the sub data types in the order of their columns
   */
  public @NotNull List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  public @Nullable TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      @Nullable final RawDataFile raw, @Nullable final SubColumnsFactory parentType,
      final int subColumnIndex) {
    final SampleTypeFilterHeaderColumn column = new SampleTypeFilterHeaderColumn(this,
        FeatureListPreferences.createDefault().getRsdSampleTypeFilter());
    column.getColumns().addAll(createSubColumns(raw, parentType));
    return column;
  }

  @Override
  public @NotNull List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      @Nullable final RawDataFile raw, @Nullable final SubColumnsFactory parentType) {
    final List<DataType> subTypes = getSubDataTypes();
    final List<TreeTableColumn<ModularFeatureListRow, Object>> columns = new ArrayList<>(
        subTypes.size());
    for (int index = 0; index < subTypes.size(); index++) {
      // this type is the parent of the sub column, so its cells request their value from #getSubColValue
      final TreeTableColumn<ModularFeatureListRow, Object> column = subTypes.get(index)
          .createColumn(raw, this, index);
      if (column != null) {
        columns.add(column);
      }
    }
    return columns;
  }

  @Override
  public int getNumberOfSubColumns() {
    return getSubDataTypes().size();
  }

  @Override
  public @Nullable String getHeader(final int subcolumn) {
    return getType(subcolumn).getHeaderString();
  }

  @Override
  public @Nullable String getUniqueID(final int subcolumn) {
    return getType(subcolumn).getUniqueID();
  }

  @Override
  public @NotNull DataType<?> getType(final int subcolumn) {
    final List<DataType> subTypes = getSubDataTypes();
    if (subcolumn < 0 || subcolumn >= subTypes.size()) {
      throw new IndexOutOfBoundsException(
          "Sub column index %d is out of range %d".formatted(subcolumn, subTypes.size()));
    }
    return subTypes.get(subcolumn);
  }

  @Override
  public @Nullable Object getSubColValue(final DataType sub, final Object value) {
    // the value of this type is the model the sub types calculate their RSD from
    if (value instanceof ModularDataModel model && sub instanceof MappingType<?> mapping) {
      return mapping.getValue(model);
    }
    return null;
  }

  @Override
  public @Nullable Object getSubColValue(final int subcolumn, final Object cellData) {
    return getSubColValue(getType(subcolumn), cellData);
  }
}
