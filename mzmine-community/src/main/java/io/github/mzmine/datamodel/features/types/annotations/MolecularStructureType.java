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
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.modifiers.NoTextColumn;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.compdb.CompoundDatabaseMatchTab;
import io.github.mzmine.modules.visualization.featurelisttable_modular.FeatureTableFX;
import io.github.mzmine.modules.visualization.molstructure.MolStructureViewer;
import io.github.mzmine.modules.visualization.molstructure.StructureTableCell;
import io.github.mzmine.modules.visualization.spectra.spectralmatchresults.SpectralIdentificationResultsTab;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.TreeTableColumn;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A molecular structure parsed from smiles or inchi by CDK. This data type is not saved to text
 * project files but rather recomputed on demand.
 */
public class MolecularStructureType extends DataType<MolecularStructure> implements
    GraphicalColumType<MolecularStructure>, NoTextColumn {

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
    throw new IllegalStateException("Use special cell factory instead: StructureTableCell");
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
  public @Nullable Runnable getDoubleClickAction(final @Nullable FeatureTableFX table,
      @NotNull final ModularFeatureListRow row, @NotNull final List<RawDataFile> file,
      @Nullable final DataType<?> superType, final @Nullable Object value) {
    if (!(value instanceof MolecularStructure structure)) {
      return null;
    }
    return () -> FxThread.runLater(() -> {
      if (superType instanceof CompoundDatabaseMatchesType) {
        CompoundDatabaseMatchTab tab = new CompoundDatabaseMatchTab(table);
        MZmineCore.getDesktop().addTab(tab);
      } else if (superType instanceof SpectralLibraryMatchesType) {
        MZmineCore.getDesktop().addTab(new SpectralIdentificationResultsTab(table));
      } else {
        new MolStructureViewer("", structure.structure()).show();
      }
    });
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }

  @Override
  public void saveToXML(@NotNull final XMLStreamWriter writer, @Nullable final Object value,
      @NotNull final ModularFeatureList flist, @NotNull final ModularFeatureListRow row,
      @Nullable final ModularFeature feature, @Nullable final RawDataFile file)
      throws XMLStreamException {
    // do nothing as this type shall not be saved. It is derived from smiles or inchi
  }
}
