/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.features.types;

import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.CosineScoreType;
import io.github.mzmine.datamodel.features.types.numbers.MatchingSignalsType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

/**
 * This type has multiple sub columns. The first is a list of complex objects ({@link
 * SpectralLibMatchSummaryType}). The first object in this list defines all the other sub columns.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class SpectralLibraryMatchType extends ModularType implements AnnotationType {

  // Unmodifiable list of all subtypes
  private final List<DataType> subTypes = List.of(new SpectralLibMatchSummaryType(),
      new CompoundNameType(), new IonAdductType(),
      new FormulaType(), new SmilesStructureType(), new InChIStructureType(),
      new PrecursorMZType(), new NeutralMassType(), new CosineScoreType(),
      new MatchingSignalsType());

  @NotNull
  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Spectral library match";
  }

  @Override
  public ModularTypeProperty createProperty() {
    ModularTypeProperty property = super.createProperty();

    // add bindings: If first element in summary column changes - update all other columns based on this object
    property.get(SpectralLibMatchSummaryType.class)
        .addListener((ListChangeListener<SpectralDBFeatureIdentity>) change -> {
          ObservableList<? extends SpectralDBFeatureIdentity> summaryProperty = change.getList();
          boolean firstElementChanged = false;
          while (change.next()) {
            firstElementChanged = firstElementChanged || change.getFrom() == 0;
          }
          if (firstElementChanged) {
            // first list elements has changed - set all other fields
            setCurrentElement(property, summaryProperty.isEmpty() ? null : summaryProperty.get(0));
          }
        });

    return property;
  }

  /**
   * On change of the first list element, change all the other sub types.
   *
   * @param data  the data property
   * @param match the current element
   */
  private void setCurrentElement(ModularTypeProperty data, SpectralDBFeatureIdentity match) {
    if (match == null) {
      for (DataType type : this.getSubDataTypes()) {
        if (!(type instanceof SpectralLibMatchSummaryType)) {
          data.set(type, null);
        }
      }
    } else {
      SpectralDBEntry entry = match.getEntry();
      SpectralSimilarity score = match.getSimilarity();

      // update selected values
      data.set(CompoundNameType.class, entry.getField(DBEntryField.NAME).orElse(""));
      data.set(FormulaType.class, entry.getField(DBEntryField.FORMULA).orElse(""));
      data.set(IonAdductType.class, entry.getField(DBEntryField.ION_TYPE).orElse(""));
      data.set(SmilesStructureType.class, entry.getField(DBEntryField.SMILES).orElse(""));
      data.set(InChIStructureType.class, entry.getField(DBEntryField.INCHI).orElse(""));
      data.set(CosineScoreType.class, score.getScore());
      data.set(MatchingSignalsType.class, score.getOverlap());
      if (entry.getField(DBEntryField.MZ).isPresent()) {
        data.set(PrecursorMZType.class, entry.getField(DBEntryField.MZ).get());
      }
      if (entry.getField(DBEntryField.EXACT_MASS).isPresent()) {
        data.set(NeutralMassType.class, entry.getField(DBEntryField.EXACT_MASS).get());
      }
    }
  }
}
