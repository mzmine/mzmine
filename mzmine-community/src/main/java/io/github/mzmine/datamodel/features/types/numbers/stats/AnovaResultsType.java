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

package io.github.mzmine.datamodel.features.types.numbers.stats;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleModularDataModel;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.abstr.SimpleSubColumnsType;
import io.github.mzmine.modules.dataanalysis.significance.anova.AnovaResult;
import java.util.List;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnovaResultsType extends SimpleSubColumnsType<AnovaResult> {

  @Override
  public @NotNull String getUniqueID() {
    return "anova_result";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "ANOVA";
  }

  @Override
  public Property<AnovaResult> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<AnovaResult> getValueClass() {
    return AnovaResult.class;
  }

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return List.of(DataTypes.get(AnovaPValueType.class), DataTypes.get(AnovaFValueType.class),
        DataTypes.get(SelectedMetadataColumnType.class));
  }

  @Override
  protected AnovaResult createRecord(SimpleModularDataModel model) {
    // not needed. usually called in super.laodFromXML, but this method is not used here.
    return null;
  }

  @Override
  public Object loadFromXML(@NotNull XMLStreamReader reader, @NotNull MZmineProject project,
      @NotNull ModularFeatureList flist, @NotNull ModularFeatureListRow row,
      @Nullable ModularFeature feature, @Nullable RawDataFile file) throws XMLStreamException {
    final SimpleModularDataModel model = loadSubColumnsFromXML(reader, project, flist, row, feature,
        file);

    if(model.isEmpty()) {
      return null;
    }
    return new AnovaResult(row, model.get(SelectedMetadataColumnType.class),
        model.get(AnovaPValueType.class), model.get(AnovaFValueType.class));
  }
}
