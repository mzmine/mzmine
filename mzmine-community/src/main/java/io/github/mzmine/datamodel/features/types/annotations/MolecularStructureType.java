/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.modules.visualization.molstructure.MolStructureViewer;
import io.github.mzmine.modules.visualization.molstructure.Structure2DComponent;
import io.github.mzmine.modules.visualization.molstructure.StructureTableCell;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeTableColumn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MolecularStructureType extends DataType<MolecularStructure> implements
    GraphicalColumType<MolecularStructure> {

  private static final Logger logger = Logger.getLogger(MolecularStructureType.class.getName());

  @Override
  public @NotNull String getUniqueID() {
    return "structure_2d";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Structure";
  }

  @Override
  public Property<MolecularStructure> createProperty() {
    return new SimpleObjectProperty<>();
  }

  @Override
  public Class<MolecularStructure> getValueClass() {
    return MolecularStructure.class;
  }

  @Override
  public double getColumnWidth() {
    return DEFAULT_GRAPHICAL_CELL_WIDTH;
  }

  @Override
  public @Nullable Node createCellContent(@NotNull final ModularFeatureListRow row,
      final MolecularStructure cellData, @Nullable final RawDataFile raw,
      final AtomicDouble progress) {
    if (cellData == null) {
      return null;
    }
// not really used as we are setting a different cell factory
    return new Structure2DComponent(cellData.structure());
  }

  @Override
  public @Nullable TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      final @Nullable RawDataFile raw, final @Nullable SubColumnsFactory parentType,
      final int subColumnIndex) {
    var column = super.createColumn(raw, parentType, subColumnIndex);
    column.setCellFactory(_ -> new StructureTableCell<>());
    return column;
  }

  @Override
  public @Nullable Runnable getDoubleClickAction(@NotNull final ModularFeatureListRow row,
      @NotNull final List<RawDataFile> file, @Nullable final DataType<?> superType,
      final @Nullable Object value) {
    return () -> FxThread.runLater(() -> {
      if (value instanceof MolecularStructure structure) {
        new MolStructureViewer("", structure.structure()).show();
      }
    });
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }
}
