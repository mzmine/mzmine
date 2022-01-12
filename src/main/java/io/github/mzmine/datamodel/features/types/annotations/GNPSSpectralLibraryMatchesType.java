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
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.MatchingSignalsType;
import io.github.mzmine.datamodel.features.types.numbers.scores.CosineScoreType;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch.ATT;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

/**
 * This type has multiple sub columns
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class GNPSSpectralLibraryMatchesType extends ListWithSubsType<GNPSLibraryMatch> implements
    AnnotationType {

  // Unmodifiable list of all subtypes
  private static final List<DataType> subTypes = List.of(new GNPSSpectralLibraryMatchesType(),
      new CompoundNameType(), new IonAdductType(),
      new SmilesStructureType(), new InChIStructureType(),
      new CosineScoreType(), new MatchingSignalsType(), new GNPSLibraryUrlType(),
      new GNPSClusterUrlType(), new GNPSNetworkUrlType());

  private static final Map<Class<? extends DataType>, Function<GNPSLibraryMatch, Object>> mapper =
      Map.ofEntries(
          createEntry(GNPSSpectralLibraryMatchesType.class, match -> match),
          createEntry(CompoundNameType.class,
              match -> match.getResultOr(ATT.COMPOUND_NAME, "NONAME")),
          createEntry(IonAdductType.class, match -> match.getResultOr(ATT.ADDUCT, "")),
          createEntry(SmilesStructureType.class, match -> match.getResultOr(ATT.SMILES, "")),
          createEntry(InChIStructureType.class, match -> match.getResultOr(ATT.INCHI, "")),
          createEntry(CosineScoreType.class,
              match -> match.getResultOr(ATT.LIBRARY_MATCH_SCORE, null)),
          createEntry(MatchingSignalsType.class,
              match -> match.getResultOr(ATT.SHARED_SIGNALS, null)),
          createEntry(GNPSLibraryUrlType.class,
              match -> match.getResultOr(ATT.GNPS_LIBRARY_URL, null)),
          createEntry(GNPSClusterUrlType.class,
              match -> match.getResultOr(ATT.GNPS_CLUSTER_URL, null)),
          createEntry(GNPSNetworkUrlType.class,
              match -> match.getResultOr(ATT.GNPS_NETWORK_URL, null))
      );

  @Override
  protected Map<Class<? extends DataType>, Function<GNPSLibraryMatch, Object>> getMapper() {
    return mapper;
  }

  @NotNull
  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }


  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "gnps_library_matches";
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "GNPS library match";
  }

}
