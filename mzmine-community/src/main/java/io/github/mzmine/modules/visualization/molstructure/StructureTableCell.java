package io.github.mzmine.modules.visualization.molstructure;

import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
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
    graphicProperty().bind(
        Bindings.createObjectBinding(() -> !isEmpty() ? molViewer : null,
            itemProperty(), emptyProperty()));
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
  }

}
