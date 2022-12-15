/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseMatchInfoType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.modules.visualization.molstructure.Structure2DComponent;
import java.awt.Toolkit;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;

public class CompoundDatabaseMatchPane extends BorderPane {

  public static final int structureWidth = (int) Math.min(
      (Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 3), 500);
  public static final int structureHeight = (int) Math.min(
      (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 6), 250);

  private static final Logger logger = Logger.getLogger(CompoundDatabaseMatchPane.class.getName());

  private static final String VALUE_UNAVAILABLE = "N/A";
  private final GridPane entries;
  @NotNull
  private final CompoundDBAnnotation annotation;
  @Nullable
  private final ModularFeatureListRow row;

  private static final List<DataType<?>> staticFields = List.of(new CompoundNameType(),
      new NeutralMassType(), new IonTypeType(), new PrecursorMZType(), new DatabaseMatchInfoType());

  public CompoundDatabaseMatchPane(@NotNull CompoundDBAnnotation annotation,
      @Nullable ModularFeatureListRow row) {

    this.annotation = annotation;
    this.row = row;
    entries = buildGridPane(annotation, row);
    final Canvas structure = buildStructurePane(annotation);
    if (structure != null) {
      structure.setHeight(structureHeight);
      structure.setWidth(structureWidth);
    }

    setCenter(structure);
    setRight(entries);
  }

  public CompoundDatabaseMatchPane(CompoundDBAnnotation annotation) {
    this(annotation, null);
  }

  private static Canvas buildStructurePane(@Nullable final CompoundDBAnnotation annotation) {
    final String smiles = annotation.getSmiles();
    final String inchi = annotation.get(new InChIStructureType());

    IAtomContainer structure = null;
    if (smiles != null) {
      structure = parseSmiles(smiles);
    } else if (inchi != null) {
      structure = parseInchi(inchi);
    }

    try {
      return structure != null ? new Structure2DComponent(structure) : new Canvas();
    } catch (CDKException e) {
      logger.log(Level.WARNING, "Cannot initialize Structure2DComponent.", e);
      return null;
    }
  }

  @Nullable
  private static IAtomContainer parseInchi(String inchi) {
    InChIGeneratorFactory factory;
    IAtomContainer molecule;
    if (inchi != null) {
      try {
        factory = InChIGeneratorFactory.getInstance();
        // Get InChIToStructure
        InChIToStructure inchiToStructure = factory.getInChIToStructure(inchi,
            DefaultChemObjectBuilder.getInstance());
        molecule = inchiToStructure.getAtomContainer();
        return molecule;
      } catch (CDKException e) {
        String errorMessage = "Could not load 2D structure\n" + "Exception: ";
        logger.log(Level.WARNING, errorMessage, e);
        return null;
      }
    } else {
      return null;
    }
  }

  private static IAtomContainer parseSmiles(String smiles) {
    SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
    IAtomContainer molecule;
    if (smilesParser != null) {
      try {
        molecule = smilesParser.parseSmiles(smiles);
        return molecule;
      } catch (InvalidSmilesException e1) {
        String errorMessage = "Could not load 2D structure\n" + "Exception: ";
        logger.log(Level.WARNING, errorMessage, e1);
        return null;
      }
    } else {
      return null;
    }
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

    for (Entry<DataType<?>, Object> entry : annotation.getReadOnlyMap().entrySet()) {
      final DataType<?> type = entry.getKey();
      final Object value = entry.getValue();

      if (staticFields.contains(type)) {
        continue;
      }

      final Label label = new Label(type.getHeaderString());
      String strValue = value != null ? type.getFormattedStringCheckType(value) : VALUE_UNAVAILABLE;
      final Label valueLabel = new Label(strValue);
      if (row != null && row.getBestFeature() != null) {
        valueLabel.setOnMouseClicked(
            e -> type.getDoubleClickAction(row, List.of(row.getBestFeature().getRawDataFile()),
                new CompoundDatabaseMatchesType(), value));
      }

      pane.add(label, 0, rowCounter);
      pane.add(valueLabel, 1, rowCounter);
      rowCounter++;
    }

    return pane;
  }
}
