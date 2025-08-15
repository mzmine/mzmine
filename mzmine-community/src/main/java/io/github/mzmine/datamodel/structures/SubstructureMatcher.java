/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.structures;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.Pattern;

/**
 * Centralized substructure matching
 */
public interface SubstructureMatcher {

  enum StructureMatchMode implements UniqueIdSupplier {
    EXACT, SUBSTRUCTURE;

    @Override
    public @NotNull String getUniqueID() {
      return switch (this) {
        case EXACT -> "exact";
        case SUBSTRUCTURE -> "substructure";
      };
    }
  }

  boolean matches(IAtomContainer structure);

  default boolean matches(@Nullable MolecularStructure structure) {
    if (structure == null) {
      return false;
    }
    return matches(structure.structure());
  }

  /**
   * SMARTS is always substructure matcher
   */
  @Nullable
  static SubstructureMatcher fromSmarts(@NotNull String smarts) {
    final SmartsMolecularStructure subStructure = StructureParser.silent().parseSmarts(smarts);
    if (subStructure == null) {
      return null;
    }
    return fromSmarts(subStructure);
  }

  /**
   * SMARTS is always substructure matcher
   */
  @NotNull
  static SubstructureMatcher fromSmarts(@NotNull SmartsMolecularStructure subStructure) {
    return new CdkPatternSubstructureMatcher(subStructure.pattern());
  }

  @NotNull
  static SubstructureMatcher fromStructure(@NotNull MolecularStructure subStructure,
      SubstructureMatcher.StructureMatchMode mode) {
    return fromStructure(subStructure.structure(), mode);
  }

  @NotNull
  static SubstructureMatcher fromStructure(@NotNull IAtomContainer subStructure,
      StructureMatchMode mode) {
    subStructure = StructureUtils.harmonizeRemoveStereoChemistry(subStructure);

    final Pattern pattern = mode == StructureMatchMode.EXACT ? Pattern.findIdentical(subStructure)
        : Pattern.findSubstructure(subStructure);

    return new CdkPatternSubstructureMatcher(pattern);
  }


  record CdkPatternSubstructureMatcher(Pattern subStructure) implements SubstructureMatcher {

    @Override
    public boolean matches(IAtomContainer structure) {
      if (structure == null) {
        return false;
      }
      structure = StructureUtils.harmonizeRemoveStereoChemistry(structure);

      return subStructure.matches(structure);
    }
  }

}
