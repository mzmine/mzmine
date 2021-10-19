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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ModularType;
import io.github.mzmine.datamodel.features.types.ModularTypeMap;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
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
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "spectral_lib_match_annotation";
  }

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


  /**
   * On change of the first list element, change all the other sub types.
   *
   * @param data  the data property
   * @param match the current element
   */
  private void setCurrentElement(ModularTypeMap data, SpectralDBFeatureIdentity match) {
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
      data.set(CompoundNameType.class, entry.getField(DBEntryField.NAME).orElse("").toString());
      data.set(FormulaType.class, entry.getField(DBEntryField.FORMULA).orElse("").toString());
      data.set(IonAdductType.class, entry.getField(DBEntryField.ION_TYPE).orElse("").toString());
      data.set(SmilesStructureType.class,
          entry.getField(DBEntryField.SMILES).orElse("").toString());
      data.set(InChIStructureType.class, entry.getField(DBEntryField.INCHI).orElse("").toString());
      data.set(CosineScoreType.class, (float) score.getScore());
      data.set(MatchingSignalsType.class, score.getOverlap());
      if (entry.getField(DBEntryField.MZ).isPresent()) {
        data.set(PrecursorMZType.class, (double) entry.getField(DBEntryField.MZ).orElse(null));
      }
      if (entry.getField(DBEntryField.EXACT_MASS).isPresent()) {
        data.set(NeutralMassType.class,
            (double) entry.getField(DBEntryField.EXACT_MASS).orElse(null));
      }
    }
  }
}
