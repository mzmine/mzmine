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
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.abstr.EnumDataType;
import io.github.mzmine.datamodel.features.types.fx.DataTypeCellValueFactory;
import io.github.mzmine.datamodel.features.types.modifiers.GraphicalColumType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DetectionType extends EnumDataType<FeatureStatus> implements
    GraphicalColumType<FeatureStatus> {

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "feature_state";
  }

  @Override
  public @Nullable FeatureStatus getDefaultValue() {
    return FeatureStatus.UNKNOWN;
  }

  @Override
  @NotNull
  public String getHeaderString() {
    return "State";
  }

  @Override
  public Class<FeatureStatus> getValueClass() {
    return FeatureStatus.class;
  }

  @Override
  public double getColumnWidth() {
    return 10;
  }

  @Override
  public @Nullable TreeTableColumn<ModularFeatureListRow, Object> createColumn(
      @Nullable RawDataFile raw, @Nullable SubColumnsFactory parentType, int subColumnIndex) {
    TreeTableColumn<ModularFeatureListRow, Object> col = new TreeTableColumn<>(
        this.getHeaderString());
    col.setUserData(this);
    col.setSortable(true);

    // define observable
    col.setCellValueFactory(new DataTypeCellValueFactory(raw, this, parentType, subColumnIndex));
    // value representation

    col.setCellFactory(
        new Callback<TreeTableColumn<ModularFeatureListRow, Object>, TreeTableCell<ModularFeatureListRow, Object>>() {

          @Override
          public TreeTableCell<ModularFeatureListRow, Object> call(
              TreeTableColumn<ModularFeatureListRow, Object> param) {
            return new TreeTableCell<>() {

              private Circle circle;
              private StackPane pane;

              {
                pane = new StackPane();
                circle = new Circle(10);
                pane.setAlignment(Pos.CENTER);
                pane.getChildren().add(circle);
              }

              @Override
              protected void updateItem(Object item, boolean empty) {

                try {
                  super.updateItem(item, empty);
                  // needs to check for row visibility
                  if (empty || item == null) {
                    setGraphic(null);
                    return;
                  }

                  final ModularFeatureListRow row = this.getTableRow().getItem();
                  if (row == null) {
                    setGraphic(null);
                    return;
                  }
                  final ModularFeature f = row.getFeature(raw);
                  if (f == null) {
                    setGraphic(null);
                    return;
                  }

                  circle.setFill(f.getFeatureStatus().getColorFX());
                  setGraphic(pane);

                } catch (Exception ex) {
                  // silent
                }
              }
            };
          }
        });
    return col;
  }


  @Override
  public @Nullable Node createCellContent(ModularFeatureListRow row, FeatureStatus cellData,
      RawDataFile raw, AtomicDouble progress) {
    if (cellData == null) {
      return null;
    }

    Circle circle = new Circle();
    circle.setRadius(10);
    circle.setFill(cellData.getColorFX());
    return circle;
  }

  @Override
  public ObjectProperty<FeatureStatus> createProperty() {
    return new SimpleObjectProperty<>(FeatureStatus.UNKNOWN);
  }
}
