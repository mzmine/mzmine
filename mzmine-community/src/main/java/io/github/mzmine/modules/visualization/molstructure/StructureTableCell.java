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

package io.github.mzmine.modules.visualization.molstructure;

import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.MolecularStructureType;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TreeTableCell;

public class StructureTableCell<S> extends TreeTableCell<S, Object> {

  private static final Logger logger = Logger.getLogger(StructureTableCell.class.getName());

  public StructureTableCell() {
    final Structure2DComponent molViewer = new Structure2DComponent();
    setHeight(GraphicalColumType.DEFAULT_GRAPHICAL_CELL_HEIGHT);

    molViewer.moleculeProperty()
        .bind(itemProperty().map(o -> (MolecularStructure) o).map(MolecularStructure::structure));

    // show or hide pane
    graphicProperty().bind(Bindings.createObjectBinding(
        () -> !isEmpty() && itemProperty().get() != null ? molViewer : null, itemProperty(),
        emptyProperty()));
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    setMinWidth(DataTypes.get(MolecularStructureType.class).getColumnWidth());
  }
}
