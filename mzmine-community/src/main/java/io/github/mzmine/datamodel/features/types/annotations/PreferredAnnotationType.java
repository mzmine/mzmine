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

import io.github.mzmine.datamodel.features.FeatureAnnotationPriority;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.modifiers.MappingType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.RtAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PreferredAnnotationType extends ListWithSubsType<FeatureAnnotation> implements
    MappingType<Object> {

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return List.of(DataTypes.get(CompoundNameType.class),
        DataTypes.get(AnnotationSummaryType.class), DataTypes.get(PrecursorMZType.class),
        DataTypes.get(MzAbsoluteDifferenceType.class), DataTypes.get(MolecularStructureType.class),
        DataTypes.get(RTType.class), DataTypes.get(RtAbsoluteDifferenceType.class),
        DataTypes.get(ScoreType.class), DataTypes.get(AnnotationMethodType.class));
  }

  @Override
  public <K> @Nullable K map(@NotNull DataType<K> subType, FeatureAnnotation parentItem) {
    if (parentItem == null) {
      return null;
    }
    return (K) switch (subType) {
      case CompoundNameType _ -> parentItem.getCompoundName();
      case AnnotationSummaryType _ -> parentItem;
      case PrecursorMZType _ -> parentItem.getPrecursorMZ();
      case MzAbsoluteDifferenceType _ -> null;
      case MolecularStructureType _ -> parentItem.getStructure();
      case RTType _ -> parentItem.getRT();
      case RtAbsoluteDifferenceType _ -> null;
      case ScoreType _ -> parentItem.getScore();
      case AnnotationMethodType _ -> parentItem.getAnnotationMethodUniqueId();
      default -> null;
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return "preferred_annotation";
  }

  @Override
  public @NotNull String getHeaderString() {
    return "Preferred Annotation";
  }

  @Override
  public @Nullable Object getValue(@NotNull ModularDataModel model) {

    DataType<?> preferredAnnotationType = FeatureAnnotationPriority.getPreferredAnnotationType(
        model);
    if (preferredAnnotationType == null) {
      return null;
    }
    if (model instanceof ModularFeatureListRow row) {
      Object preferredAnnotation = row.get(preferredAnnotationType);
      return preferredAnnotation;
    }
    return null;
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }
}
