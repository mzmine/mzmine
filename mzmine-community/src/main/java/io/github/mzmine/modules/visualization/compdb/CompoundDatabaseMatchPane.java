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

package io.github.mzmine.modules.visualization.compdb;

import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.modules.visualization.molstructure.Structure2DComponent;
import java.awt.Toolkit;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompoundDatabaseMatchPane extends BorderPane {

  public static final int structureWidth = (int) Math.min(
      (Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 3), 500);
  public static final int structureHeight = (int) Math.min(
      (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 6), 250);

  private static final Logger logger = Logger.getLogger(CompoundDatabaseMatchPane.class.getName());

  private static final String VALUE_UNAVAILABLE = "N/A";
  private static final List<DataType<?>> staticFields = List.of(new CompoundNameType(),
      new NeutralMassType(), new IonTypeType(), new PrecursorMZType());
  private final GridPane entries;
  @NotNull
  private final CompoundDBAnnotation annotation;
  @Nullable
  private final ModularFeatureListRow row;

  public CompoundDatabaseMatchPane(@NotNull CompoundDBAnnotation annotation,
      @Nullable ModularFeatureListRow row) {

    this.annotation = annotation;
    this.row = row;
    entries = buildGridPane(annotation, row);
    final Canvas structure = buildStructurePane(annotation);
    structure.setHeight(structureHeight);
    structure.setWidth(structureWidth);

    setCenter(structure);
    setRight(entries);
  }

  public CompoundDatabaseMatchPane(CompoundDBAnnotation annotation) {
    this(annotation, null);
  }

  private static Canvas buildStructurePane(@Nullable final CompoundDBAnnotation annotation) {
    if (annotation == null) {
      return new Canvas();
    }
    MolecularStructure structure = annotation.getStructure();
    if (structure == null) {
      return new Canvas();
    }
    return new Structure2DComponent(structure.structure());
  }

  private static GridPane buildGridPane(@Nullable final CompoundDBAnnotation annotation,
      @Nullable ModularFeatureListRow row) {
    final GridPane pane = new GridPane();
    pane.setVgap(5);
    pane.setHgap(5);
    pane.setPadding(new Insets(5));
    if (annotation == null) {
      return pane;
    }

    int rowCounter = 0;
    for (DataType<?> type : staticFields) {
      final Label label = new Label(type.getHeaderString());
      label.getStyleClass().add("title-label");
      Object value = annotation.get(type);
      String strValue = value != null ? type.getFormattedStringCheckType(value) : VALUE_UNAVAILABLE;
      final Label valueLabel = new Label(strValue);
      pane.add(label, 0, rowCounter);
      pane.add(valueLabel, 1, rowCounter);
      rowCounter++;
    }

    for (Entry<DataType, Object> entry : annotation.getReadOnlyMap().entrySet()) {
      final DataType<?> type = entry.getKey();
      final Object value = entry.getValue();

      if (staticFields.contains(type)) {
        continue;
      }

      final Label label = new Label(type.getHeaderString());
      String strValue = value != null ? type.getFormattedStringCheckType(value) : VALUE_UNAVAILABLE;
      final Label valueLabel = new Label(strValue);
      if (row != null && row.getBestFeature() != null) {
        valueLabel.setOnMouseClicked(e -> type.getDoubleClickAction(null, row,
            List.of(row.getBestFeature().getRawDataFile()), new CompoundDatabaseMatchesType(),
            value));
      }

      pane.add(label, 0, rowCounter);
      pane.add(valueLabel, 1, rowCounter);
      rowCounter++;
    }

    return pane;
  }
}
