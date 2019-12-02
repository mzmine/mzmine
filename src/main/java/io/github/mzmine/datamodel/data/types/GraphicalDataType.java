package io.github.mzmine.datamodel.data.types;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;

public abstract class GraphicalDataType<T> extends DataType<T> {

  /**
   * 
   * @param cell
   * @param coll
   * @param cellData same as cell.getItem
   * @param raw only provided for sample specific DataTypes
   * @return
   */
  public abstract Node getCellNode(TreeTableCell<ModularFeatureListRow, T> cell,
      TreeTableColumn<ModularFeatureListRow, T> coll, T cellData, RawDataFile raw);
}
