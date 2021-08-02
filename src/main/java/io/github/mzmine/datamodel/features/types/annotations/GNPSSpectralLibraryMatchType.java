/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ModularType;
import io.github.mzmine.datamodel.features.types.ModularTypeProperty;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.CosineScoreType;
import io.github.mzmine.datamodel.features.types.numbers.MatchingSignalsType;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch.ATT;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;

/**
 * This type has multiple sub columns. The first is a list of complex objects ({@link
 * GNPSSpectralLibMatchSummaryType}). The first object in this list defines all the other sub
 * columns.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class GNPSSpectralLibraryMatchType extends ModularType implements AnnotationType {

  // Unmodifiable list of all subtypes
  private final List<DataType> subTypes = List.of(new GNPSSpectralLibMatchSummaryType(),
      new CompoundNameType(), new IonAdductType(),
      new SmilesStructureType(), new InChIStructureType(),
      new CosineScoreType(), new MatchingSignalsType(), new GNPSLibraryUrlType(),
      new GNPSClusterUrlType(), new GNPSNetworkUrlType());

  @NotNull
  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "GNPS library match";
  }

  @Override
  public ModularTypeProperty createProperty() {
    ModularTypeProperty property = super.createProperty();

    // add bindings: If first element in summary column changes - update all other columns based on this object
    property.get(GNPSSpectralLibMatchSummaryType.class)
        .addListener((ListChangeListener<GNPSLibraryMatch>) change -> {
          ObservableList<? extends GNPSLibraryMatch> summaryProperty = change.getList();
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
  private void setCurrentElement(ModularTypeProperty data, GNPSLibraryMatch match) {
    if (match == null) {
      for (DataType type : this.getSubDataTypes()) {
        if (!(type instanceof GNPSSpectralLibMatchSummaryType)) {
          data.set(type, null);
        }
      }
    } else {
      // update selected values
      data.set(CompoundNameType.class, match.getResultOr(ATT.COMPOUND_NAME, "NONAME"));
      data.set(IonAdductType.class, match.getResultOr(ATT.ADDUCT, ""));
      data.set(SmilesStructureType.class, match.getResultOr(ATT.SMILES, ""));
      data.set(InChIStructureType.class, match.getResultOr(ATT.INCHI, ""));
      data.set(CosineScoreType.class, match.getResultOr(ATT.LIBRARY_MATCH_SCORE, -1));
      data.set(MatchingSignalsType.class, match.getResultOr(ATT.SHARED_SIGNALS, -1));
      data.set(GNPSLibraryUrlType.class, match.getResultOr(ATT.GNPS_LIBRARY_URL, null));
      data.set(GNPSClusterUrlType.class, match.getResultOr(ATT.GNPS_CLUSTER_URL, null));
      data.set(GNPSNetworkUrlType.class, match.getResultOr(ATT.GNPS_NETWORK_URL, null));
    }
  }
}
