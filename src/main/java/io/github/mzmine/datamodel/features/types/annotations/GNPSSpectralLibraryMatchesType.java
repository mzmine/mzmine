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
