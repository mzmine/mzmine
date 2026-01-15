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

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationPriority;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.ListWithSubsType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.modifiers.MappingType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PreferredAnnotationType extends ListWithSubsType<FeatureAnnotation> implements
    MappingType<List<? extends FeatureAnnotation>> {

  @Override
  public @NotNull List<DataType> getSubDataTypes() {
    return DataTypes.getAll(CompoundNameType.class, AnnotationSummaryType.class,
        PrecursorMZType.class, MolecularStructureType.class, ScoreType.class, IonTypeType.class,
        AnnotationMethodType.class);
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
      case MolecularStructureType _ -> parentItem.getStructure();
      case ScoreType _ -> parentItem.getScore();
      case AnnotationMethodType _ -> parentItem.getAnnotationMethodName();
      case IonTypeType _ -> parentItem.getAdductType();
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
  public @Nullable List<? extends FeatureAnnotation> getValue(@NotNull ModularDataModel model) {

    final DataType<?> preferredAnnotationType = AnnotationPriority.getBestAnnotationType(
        (FeatureListRow) model);
    if (preferredAnnotationType == null) {
      return null;
    }

    if (model instanceof ModularFeatureListRow row) {
      Object preferredAnnotation = row.get(preferredAnnotationType);
      if (preferredAnnotation instanceof List<?> l && (l.isEmpty()
          || l.getFirst() instanceof FeatureAnnotation)) {
        return (List<? extends FeatureAnnotation>) l;
      }
    }
    return null;
  }

  @Override
  public boolean getDefaultVisibility() {
    return true;
  }
}
