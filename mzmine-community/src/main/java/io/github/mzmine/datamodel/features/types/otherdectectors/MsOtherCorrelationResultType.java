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

package io.github.mzmine.datamodel.features.types.otherdectectors;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.fx.CorrelatedTraceEditComboCellFactory;
import io.github.mzmine.datamodel.features.types.modifiers.MappingType;
import io.github.mzmine.datamodel.features.types.modifiers.NullColumnType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.otherdetectors.MsOtherCorrelationResolver;
import io.github.mzmine.datamodel.otherdetectors.MsOtherCorrelationRowResult;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TreeTableColumn;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Row-level "correlated traces" column. Lists the aligned other-detector rows correlated to an MS
 * row, for selection. The value is <b>derived on render</b> via {@link MappingType} from
 * {@link ModularFeatureList#getMsOtherCorrelationMaps()} (the ID-only source of truth) rather than
 * stored - so it is not serialized here; project persistence lives in the maps and the attached
 * {@link io.github.mzmine.datamodel.otherdetectors.OtherFeatureList}.
 * <p>
 * The per-file values of the currently selected trace are shown by the separate feature-level
 * {@link CorrelatedOtherFeatureType}.
 */
public class MsOtherCorrelationResultType extends ListWithSubsType<MsOtherCorrelationRowResult>
    implements MappingType<List<MsOtherCorrelationRowResult>> {

  @Override
  public @NotNull String getUniqueID() {
    return "ms_other_correlation_result";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Correlated traces";
  }

  @Override
  public @Nullable List<MsOtherCorrelationRowResult> getValue(@NotNull final ModularDataModel model) {
    // derive from the correlation maps; never call model.get(this) here (would recurse)
    if (model instanceof FeatureListRow row
        && row.getFeatureList() instanceof ModularFeatureList flist) {
      return MsOtherCorrelationResolver.resolveRowCorrelations(flist, row);
    }
    return null;
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) {
    // derived value - not persisted here (source of truth is MsOtherCorrelationMaps)
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    // derived value - skip any legacy embedded content without failing (no backward compatibility)
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      reader.next();
    }
    return null;
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return List.of(new MsOtherCorrelationResultType(), new ChromatogramTypeType(),
        new WavelengthType());
  }

  @Override
  public @NotNull List<TreeTableColumn<ModularFeatureListRow, Object>> createSubColumns(
      @Nullable final RawDataFile raw, @Nullable final SubColumnsFactory parentType) {
    final List<TreeTableColumn<ModularFeatureListRow, Object>> cols = new ArrayList<>();
    final List<DataType> subTypes = getSubDataTypes();
    for (int index = 0; index < subTypes.size(); index++) {
      final DataType type = subTypes.get(index);
      if (type instanceof NullColumnType) {
        continue;
      }
      if (this.equals(type)) {
        // main column: an editable combo to select the current correlated trace
        final TreeTableColumn<ModularFeatureListRow, Object> mainCol = DataType.createStandardColumn(
            type, raw, this, index);
        mainCol.setCellFactory(new CorrelatedTraceEditComboCellFactory());
        mainCol.setCellValueFactory(cdf -> {
          final List<MsOtherCorrelationRowResult> list = cdf.getValue().getValue()
              .get(MsOtherCorrelationResultType.class);
          // resolveRowCorrelations orders the selected trace first
          return new ReadOnlyObjectWrapper<>(
              list == null || list.isEmpty() ? null : list.getFirst());
        });
        mainCol.setPrefWidth(type.getPrefColumnWidth());
        cols.add(mainCol);
      } else {
        cols.add(type.createColumn(raw, this, index));
      }
    }
    return cols;
  }

  @Override
  protected <K> @Nullable K map(@NotNull DataType<K> subType, MsOtherCorrelationRowResult parentItem) {
    // trace identity comes from the aligned other-row's canonical TraceKey (same across files)
    return (K) switch (subType) {
      case ChromatogramTypeType c -> parentItem.otherRow().getTraceKey().chromatogramType();
      case WavelengthType w -> parentItem.otherRow().getTraceKey().wavelength();
      default -> throw new UnsupportedOperationException(
          "DataType %s is not covered in map ".formatted(subType.toString()));
    };
  }
}
