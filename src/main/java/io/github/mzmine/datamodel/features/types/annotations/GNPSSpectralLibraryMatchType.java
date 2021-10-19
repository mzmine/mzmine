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
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch.ATT;
import java.util.List;
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
  private static final List<DataType> subTypes = List.of(new GNPSSpectralLibMatchSummaryType(),
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
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "gnps_library_match_annotation";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "GNPS library match";
  }

  /**
   * On change of the first list element, change all the other sub types.
   *
   * @param data  the data property
   * @param match the current element
   */
  private void setCurrentElement(ModularTypeMap data, GNPSLibraryMatch match) {
    if (match == null) {
      for (DataType type : this.getSubDataTypes()) {
        if (!(type instanceof GNPSSpectralLibMatchSummaryType)) {
          data.set(type, null);
        }
      }
    } else {
      // update selected values
      data.set(CompoundNameType.class, match.getResultOr(ATT.COMPOUND_NAME, "NONAME").toString());
      data.set(IonAdductType.class, match.getResultOr(ATT.ADDUCT, "").toString());
      data.set(SmilesStructureType.class, match.getResultOr(ATT.SMILES, "").toString());
      data.set(InChIStructureType.class, match.getResultOr(ATT.INCHI, "").toString());
      data.set(CosineScoreType.class, match.getResultOr(ATT.LIBRARY_MATCH_SCORE, null));
      data.set(MatchingSignalsType.class, match.getResultOr(ATT.SHARED_SIGNALS, null));
      data.set(GNPSLibraryUrlType.class, match.getResultOr(ATT.GNPS_LIBRARY_URL, null));
      data.set(GNPSClusterUrlType.class, match.getResultOr(ATT.GNPS_CLUSTER_URL, null));
      data.set(GNPSNetworkUrlType.class, match.getResultOr(ATT.GNPS_NETWORK_URL, null));
    }
  }
}
