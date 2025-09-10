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

package io.github.mzmine.parameters.parametertypes.row_type_filter.filters;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.structures.StructureInputType;
import io.github.mzmine.datamodel.structures.StructureParser;
import io.github.mzmine.datamodel.structures.SubstructureMatcher;
import io.github.mzmine.datamodel.structures.SubstructureMatcher.StructureMatchMode;
import io.github.mzmine.parameters.parametertypes.row_type_filter.MatchingMode;
import io.github.mzmine.parameters.parametertypes.row_type_filter.QueryFormatException;
import io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterOption;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import org.jetbrains.annotations.NotNull;

final class StructureRowTypeFilter extends AbstractRowTypeFilter {

  private final @NotNull SubstructureMatcher matcher;


  StructureRowTypeFilter(@NotNull RowTypeFilterOption selectedType,
      @NotNull MatchingMode matchingMode, @NotNull String query) {
    super(selectedType, matchingMode, query);

    final StructureMatchMode structureMatchMode = switch (matchingMode) {
      case EQUAL -> StructureMatchMode.EXACT;
      case CONTAINS, GREATER_EQUAL, LESSER_EQUAL, NOT_EQUAL -> StructureMatchMode.SUBSTRUCTURE;
    };

    final boolean isSmiles = selectedType == RowTypeFilterOption.SMILES;
    if (isSmiles || selectedType == RowTypeFilterOption.INCHI) {
      var structure = StructureParser.silent()
          .parseStructure(query, isSmiles ? StructureInputType.SMILES : StructureInputType.INCHI);
      if (structure == null) {
        throw new QueryFormatException(query);
      }
      matcher = SubstructureMatcher.fromStructure(structure, structureMatchMode);
    } else if (selectedType == RowTypeFilterOption.SMARTS) {
      final SubstructureMatcher internal = SubstructureMatcher.fromSmarts(query);
      if (internal == null) {
        throw new QueryFormatException("SMARTS format exception: " + query);
      }
      matcher = internal;
    } else {
      throw new IllegalArgumentException("Unsupported structure type: " + selectedType);
    }
  }

  @Override
  public boolean matches(FeatureListRow row) {
    return CompoundAnnotationUtils.streamFeatureAnnotations(row)
        .map(FeatureAnnotation::getStructure).anyMatch(matcher::matches);
  }

}
