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

package io.github.mzmine.datamodel.features.types.annotations;

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.LinkedGraphicalType;
import io.github.mzmine.datamodel.features.types.graphicalnodes.CountingRowChartCellFactory;
import io.github.mzmine.datamodel.features.types.graphicalnodes.LipidSpectrumCell;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import java.util.logging.Logger;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LipidSpectrumType extends LinkedGraphicalType {

  private static final Logger logger = Logger.getLogger(LipidSpectrumType.class.getName());

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "lipid_matched_signals";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Matched Lipid Signals";
  }

  @Override
  public @Nullable TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType, int subColumnIndex) {
    final TreeTableColumn<ModularFeatureListRow, Object> column = super.createColumn(raw,
        parentType, -1);
    column.setCellFactory(new CountingRowChartCellFactory(LipidSpectrumCell::new));
    column.setCellValueFactory(cdf -> new ReadOnlyObjectWrapper<>(cdf.getValue().getValue()));
    return column;
  }

  @Override
  public @Nullable Node createCellContent(@NotNull ModularFeatureListRow row, Boolean cellData,
      @Nullable RawDataFile raw, AtomicDouble progress) {
    throw new UnsupportedOperationException();
  }

  @Override
  public double getColumnWidth() {
    return DEFAULT_GRAPHICAL_CELL_WIDTH;
  }

}
