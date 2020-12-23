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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.fx.DataTypeCellFactory;
import io.github.mzmine.datamodel.features.types.fx.DataTypeCellValueFactory;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.numbers.CosineScoreType;
import io.github.mzmine.datamodel.features.types.numbers.MatchingSignalsType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ListDataType;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBEntry;
import io.github.mzmine.util.spectraldb.entry.SpectralDBFeatureIdentity;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SpectralLibraryMatchType extends ModularType implements AnnotationType {

  // Unmodifiable list of all subtypes
  private final List<DataType> subTypes = List.of(new SpectralLibMatchSummaryType(),
          new CompoundNameType(), new IonAdductType(),
          new FormulaType(), new SmilesStructureType(), new InChIStructureType(),
          new PrecursorMZType(), new NeutralMassType(), new CosineScoreType(), new MatchingSignalsType());

  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  public String getHeaderString() {
    return "Spectral Library Match";
  }

  @Override
  public ModularTypeProperty createProperty() {
    ModularTypeProperty property = super.createProperty();

    // add bindings: If first element in summary column changes - update all other columns based on this object
    ListProperty<SpectralDBFeatureIdentity> summaryProperty = property.get(SpectralLibMatchSummaryType.class);
    summaryProperty.addListener((ListChangeListener<SpectralDBFeatureIdentity>) change -> {
      boolean firstElementChanged = false;
      while (change.next()) {
        firstElementChanged = firstElementChanged || change.getFrom() == 0;
      }
      if(firstElementChanged) {
        // first list elements has changed - set all other fields
        setCurrentElement(property, !summaryProperty.isEmpty()? null : summaryProperty.get(0));
      }
    });

    return property;
  }


  private void setCurrentElement(ModularTypeProperty data, SpectralDBFeatureIdentity match) {
    if(match==null) {
      for (DataType type : this.getSubDataTypes()) {
        if (!(type instanceof SpectralLibMatchSummaryType)) {
          data.set(type, null);
        }
      }
    }
    else {
      SpectralDBEntry entry = match.getEntry();
      SpectralSimilarity score = match.getSimilarity();

      // update selected values
      data.set(CompoundNameType.class, entry.getField(DBEntryField.NAME));
      data.set(FormulaType.class, entry.getField(DBEntryField.FORMULA));
      data.set(IonAdductType.class, entry.getField(DBEntryField.ION_TYPE));
      data.set(SmilesStructureType.class, entry.getField(DBEntryField.SMILES));
      data.set(InChIStructureType.class, entry.getField(DBEntryField.INCHI));
      data.set(CosineScoreType.class, score.getScore());
      data.set(MatchingSignalsType.class, score.getOverlap());
      data.set(PrecursorMZType.class, entry.getField(DBEntryField.MZ));
      data.set(NeutralMassType.class, entry.getField(DBEntryField.EXACT_MASS));
    }
  }
}
