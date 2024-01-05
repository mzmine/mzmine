/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.graphicalnodes.AreaBarChart;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import java.util.Map;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AreaBarType extends DataType<Map<RawDataFile, ModularFeature>> implements
    GraphicalColumType<Map<RawDataFile, ModularFeature>> {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "area_bar";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Area Bars";
  }

  @Override
  public Property<Map<RawDataFile, ModularFeature>> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<Map<RawDataFile, ModularFeature>> getValueClass() {
    return (Class) Map.class;
  }

  @Override
  public double getColumnWidth() {
    return 150;
  }

  @Override
  public @Nullable Node createCellContent(ModularFeatureListRow row,
      Map<RawDataFile, ModularFeature> cellData, RawDataFile raw, AtomicDouble progress) {

    if(row.get(AreaBarType.class) != null) {
      return null;
    }

    if(row.getNumberOfFeatures() > 1) {
      return new AreaBarChart(row, progress);
    };
    return null;
  }
}
