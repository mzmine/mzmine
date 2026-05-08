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
import io.github.mzmine.datamodel.features.types.JsonStringType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireClassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireParentType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireSubclassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ClassyFireSuperclassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierClassType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierPathwayType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.NPClassifierSuperclassType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonAdductType;
import io.github.mzmine.datamodel.features.types.identifiers.CASType;
import io.github.mzmine.datamodel.features.types.identifiers.EntryIdType;
import io.github.mzmine.datamodel.features.types.identifiers.InternalIdType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.numbers.CCSRelativeErrorType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MatchingSignalsType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RIDiffType;
import io.github.mzmine.datamodel.features.types.numbers.RtAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.scores.DreamsScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.ExplainedIntensityPercentType;
import io.github.mzmine.datamodel.features.types.numbers.scores.MS2DeepscoreScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.SimilarityType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Analog spectral library matches in a list. Identical structure to
 * {@link SpectralLibraryMatchesType} but produced by the analog (modification-aware) library search
 * and additionally exposes ML score sub-columns ({@link MS2DeepscoreScoreType},
 * {@link DreamsScoreType}).
 */
public final class AnalogSpectralLibraryMatchesType extends
    AbstractSpectralLibraryMatchesType implements AnnotationType {

  // Unmodifiable list of all subtypes — mirrors SpectralLibraryMatchesType plus ML score columns.
  private static final List<DataType> subTypes = List.of( //
      new AnalogSpectralLibraryMatchesType(), //
      new CompoundNameType(), //
      new AnnotationSummaryType(),//
      new SimilarityType(),//
      new MS2DeepscoreScoreType(), //
      new DreamsScoreType(), //
      new MatchingSignalsType(),//
      new ExplainedIntensityPercentType(), //
      new IonAdductType(),//
      new FormulaType(),//
      new MolecularStructureType(),//
      new SmilesStructureType(),//
      // classifiers
      new InChIStructureType(), new ClassyFireSuperclassType(), new ClassyFireClassType(),
      new ClassyFireSubclassType(), new ClassyFireParentType(), new NPClassifierSuperclassType(),
      new NPClassifierClassType(), //
      new NPClassifierPathwayType(),//
      new NeutralMassType(),//
      new PrecursorMZType(),//
      new MzAbsoluteDifferenceType(),//
      new MzPpmDifferenceType(),//
      new RtAbsoluteDifferenceType(),//
      new CCSType(),//
      new CCSRelativeErrorType(), //
      new CommentType(), //
      new EntryIdType(),  //
      new CASType(), //
      new InternalIdType(), //
      new RIDiffType(), //
      new JsonStringType() //
  );

  @NotNull
  @Override
  public List<DataType> getSubDataTypes() {
    return subTypes;
  }

  @NotNull
  @Override
  public String getHeaderString() {
    return "Analog spectral match";
  }

  @NotNull
  @Override
  public final String getUniqueID() {
    // Never change the ID for compatibility during saving/loading of type
    return "analog_spectral_db_matches";
  }
}
