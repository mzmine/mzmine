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
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.abstr.SimpleSubColumnsType;
import io.github.mzmine.datamodel.features.types.modifiers.MappingType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.otherdetectors.MsOtherCorrelationResolver;
import io.github.mzmine.datamodel.otherdetectors.CorrelatedOtherFeature;
import io.github.mzmine.datamodel.otherdetectors.OtherFeature;
import io.github.mzmine.modules.io.projectload.version_3_0.CONST;
import java.util.List;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Feature-level "correlated trace" column: reflects the preferred correlated other-detector row (the
 * first entry of the MS row's correlation list) resolved to <b>this file's</b> per-file correlation.
 * The value is derived on render via {@link MappingType} from
 * {@link ModularFeatureList#getMsOtherCorrelationMaps()}. Sub-columns expose the correlation type,
 * chromatogram type, area, height and area%.
 *
 * @see MsOtherCorrelationRowResultType the row-level list column where the preferred trace is chosen
 */
public class CorrelatedOtherFeatureType extends SimpleSubColumnsType<CorrelatedOtherFeature>
    implements MappingType<CorrelatedOtherFeature> {

  @Override
  public @NotNull String getUniqueID() {
    return "correlated_other_feature";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Correlated trace";
  }

  @Override
  public Property<CorrelatedOtherFeature> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<CorrelatedOtherFeature> getValueClass() {
    return CorrelatedOtherFeature.class;
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return List.of(new CorrelatedOtherFeatureType(), new MsOtherCorrelationTypeType(),
        new ChromatogramTypeType(), new AreaType(), new HeightType(), new AreaPercentType());
  }

  @Override
  public @Nullable CorrelatedOtherFeature getValue(@NotNull final ModularDataModel model) {
    // resolve the preferred correlated trace for this feature's file; never call model.get(this)
    if (model instanceof ModularFeature feature
        && feature.getFeatureList() instanceof ModularFeatureList flist) {
      final FeatureListRow row = feature.getRow();
      if (row == null) {
        return null;
      }
      return MsOtherCorrelationResolver.resolvePreferredCorrelation(flist, row,
          feature.getRawDataFile());
    }
    return null;
  }

  @Override
  public @Nullable Object getSubColValue(final DataType sub, final Object value) {
    if (!(value instanceof CorrelatedOtherFeature result)) {
      return null;
    }
    if (this.equals(sub)) {
      return result;
    }
    final OtherFeature of = result.otherFeature();
    return switch (sub) {
      case MsOtherCorrelationTypeType t -> result.type();
      case ChromatogramTypeType c -> of.getChromatogramType();
      default -> of.get(sub);
    };
  }

  @Override
  public void saveToXML(@NotNull XMLStreamWriter writer, @Nullable Object value,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) {
    // derived value - not persisted (source of truth is MsOtherCorrelationMaps)
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    // derived value - skip any legacy content without failing
    while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName()
        .equals(CONST.XML_DATA_TYPE_ELEMENT))) {
      reader.next();
    }
    return null;
  }
}
