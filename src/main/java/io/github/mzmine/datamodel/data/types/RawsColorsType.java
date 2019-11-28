/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.data.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureListRow;
import io.github.mzmine.datamodel.data.types.fx.DataTypeCellFactory;
import io.github.mzmine.datamodel.data.types.fx.RawsMapCellValueFactory;
import io.github.mzmine.datamodel.data.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.data.types.modifiers.SubColumnsFactory;
import io.github.mzmine.util.color.ColorsFX;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * This FeaturesType contains features for each RawDataFile. Sub columns for samples and charts are
 * created.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class RawsColorsType extends DataType<ObservableMap<RawDataFile, Color>>
    implements SubColumnsFactory, GraphicalColumType {

  public RawsColorsType(Map<RawDataFile, Color> map) {
    super(FXCollections.unmodifiableObservableMap(FXCollections.observableMap(map)));
  }

  @Override
  @Nonnull
  public String getHeaderString() {
    return "Colors";
  }

  @Override
  @Nonnull
  public List<TreeTableColumn<ModularFeatureListRow, ?>> createSubColumns() {
    List<TreeTableColumn<ModularFeatureListRow, ?>> cols = new ArrayList<>();

    // create all sample columns
    for (Entry<RawDataFile, Color> entry : value.entrySet()) {
      RawDataFile raw = entry.getKey();
      // create column per name
      TreeTableColumn<ModularFeatureListRow, RawsColorsType> sampleCol =
          new TreeTableColumn<>(raw.getName());
      sampleCol.setCellValueFactory(new RawsMapCellValueFactory<>(raw, this.getClass()));
      sampleCol.setCellFactory(new DataTypeCellFactory<>(raw, this.getClass()));

      // add all
      cols.add(sampleCol);
    }
    return cols;
  }

  @Override
  public Node getCellNode(TreeTableCell<ModularFeatureListRow, ? extends DataType> cell,
      TreeTableColumn<ModularFeatureListRow, ? extends DataType> coll, DataType<?> cellData,
      RawDataFile raw) {
    Color color = null;
    if (cellData instanceof RawsColorsType) {
      color = ((RawsColorsType) cellData).value.get(raw);
    }
    if (color == null)
      color = Color.BLACK;
    Pane pane = new Pane();
    pane.setStyle("-fx-background-color: #" + ColorsFX.toHexString(color));
    return pane;
  }

}
