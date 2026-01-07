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

package io.github.mzmine.datamodel.features.types.annotations;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.MatchingSignalsType;
import io.github.mzmine.datamodel.features.types.numbers.scores.SimilarityType;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSLibraryMatch.ATT;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This type has multiple sub columns
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class GNPSSpectralLibraryMatchesType extends ListWithSubsType<GNPSLibraryMatch> implements
    AnnotationType {

  // Unmodifiable list of all subtypes
  private static final List<DataType> subTypes = List.of(new GNPSSpectralLibraryMatchesType(),
      new CompoundNameType(), new IonAdductType(), new SmilesStructureType(),
      new InChIStructureType(), new SimilarityType(), new MatchingSignalsType(),
      new GNPSLibraryUrlType(), new GNPSClusterUrlType(), new GNPSNetworkUrlType());


  @NotNull
  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @Override
  public <K> @Nullable K map(@NotNull final DataType<K> subType, final GNPSLibraryMatch match) {
    return (K) switch (subType) {
      case GNPSSpectralLibraryMatchesType __ -> match;
      case CompoundNameType __ -> match.getResultOr(ATT.COMPOUND_NAME, "NONAME");
      case IonAdductType __ -> match.getResultOr(ATT.ADDUCT, "");
      case SmilesStructureType __ -> match.getResultOr(ATT.SMILES, "");
      case InChIStructureType __ -> match.getResultOr(ATT.INCHI, "");
      case SimilarityType __ -> match.getResultOr(ATT.LIBRARY_MATCH_SCORE, null);
      case MatchingSignalsType __ -> match.getResultOr(ATT.SHARED_SIGNALS, null);
      case GNPSLibraryUrlType __ -> match.getResultOr(ATT.GNPS_LIBRARY_URL, null);
      case GNPSClusterUrlType __ -> match.getResultOr(ATT.GNPS_CLUSTER_URL, null);
      case GNPSNetworkUrlType __ -> match.getResultOr(ATT.GNPS_NETWORK_URL, null);
      default -> throw new UnsupportedOperationException(
          "DataType %s is not covered in map".formatted(subType.toString()));
    };
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
