/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.util.annotations;

import io.github.mzmine.datamodel.features.FeatureAnnotationPriority;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.MissingValueType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.util.ArrayUtils;
import io.github.mzmine.util.DataTypeUtils;
import io.github.mzmine.util.collections.CollectionUtils;
import io.github.mzmine.util.collections.SortOrder;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

public class CompoundAnnotationUtils {

  /**
   * @param rows             The rows to group
   * @param mapMissingValues if none of the provided types is present, rows will be mapped to
   *                         {@link MissingValueType} if this parameter is true. Otherwise they are
   *                         dropped.
   * @return A map of the annotation types and the matching rows. The map is a tree map sorted
   * according to the hierarchy of the specified types.
   */
  @NotNull
  public static Map<DataType<?>, List<FeatureListRow>> groupRowsByAnnotationPriority(
      List<FeatureListRow> rows, boolean mapMissingValues) {
    return DataTypeUtils.groupByBestDataType(rows, mapMissingValues,
        FeatureAnnotationPriority.getDataTypesInOrder());
  }

  /**
   * Get the best annotation types or {@link MissingValueType} for each feature list row.
   *
   * @param rows             the rows to be mapped.
   * @param mapMissingValues true, add {@link MissingValueType}. Otherwise map will not contain null
   *                         mapping for missing values. If true the map will be of rows.size
   * @return a map of best annotation types per row
   */
  @NotNull
  public static Map<FeatureListRow, DataType<?>> mapBestAnnotationTypesByPriority(
      List<FeatureListRow> rows, boolean mapMissingValues) {
    var orderedTypes = FeatureAnnotationPriority.getDataTypesInOrder();
    var notAnnotatedType = mapMissingValues ? DataTypes.get(MissingValueType.class) : null;

    Map<FeatureListRow, DataType<?>> map = new HashMap<>();

    for (final FeatureListRow row : rows) {
      DataType<?> best = DataTypeUtils.getBestTypeWithValue(row, notAnnotatedType, orderedTypes);
      if (best != null) {
        map.put(row, best);
      }
    }
    return map;
  }

  /**
   * @param types can contain duplicates or nulls - that are filtered out
   * @param order order either ascending from missing to best match or reverse
   * @return map of DataType to their rank in
   * {@link FeatureAnnotationPriority#getDataTypesInOrder()}. If {@link MissingValueType} is found,
   * it is added as the last rank priority to the map
   */
  @NotNull
  public static Map<DataType<?>, Integer> rankUniqueAnnotationTypes(Collection<DataType<?>> types,
      @NotNull final SortOrder order) {
    var sortedUniqueTypes = types.stream().filter(Objects::nonNull).distinct()
        .filter(CompoundAnnotationUtils::isAnnotationOrMissingType)
        .sorted(FeatureAnnotationPriority.createSorter(order)).toList();
    return CollectionUtils.indexMap(sortedUniqueTypes);
  }

  /**
   * @return true if type is either annotation type or {@link MissingValueType}
   */
  public static boolean isAnnotationOrMissingType(final DataType<?> type) {
    return type instanceof MissingValueType || ArrayUtils.contains(type,
        FeatureAnnotationPriority.getDataTypesInOrder());
  }


  /**
   * A list of matches where each entry has a different compound name.
   *
   * @param matches can contain duplicate compound names - the method will find the best annotation
   *                for each compound name by sorting by least modified adduct and highest score
   * @return list of unique compound names
   */
  public static <T extends FeatureAnnotation> List<T> getBestMatchesPerCompoundName(
      final List<T> matches) {
    // might have different adducts for the same compound - list them by compound name
    Map<String, List<T>> compoundsMap = matches.stream()
        .collect(Collectors.groupingBy(FeatureAnnotation::getCompoundName));
    // sort by number of adducts + modifications
    List<T> oneMatchPerCompound = compoundsMap.values().stream()
        .map(compound -> compound.stream().min(getSorterLeastModifiedCompoundFirst()).orElse(null))
        .filter(Objects::nonNull).toList();

    return oneMatchPerCompound;
  }

  /**
   * First sort by adduct type: simple IonType first, which means M+H better than 2M+H2+2 and
   * 2M-H2O+H+.
   *
   * @return sorter
   */
  public static Comparator<FeatureAnnotation> getSorterLeastModifiedCompoundFirst() {
    return Comparator.comparing(FeatureAnnotation::getAdductType,
            Comparator.nullsLast(Comparator.comparingInt(IonType::getTotalPartsCount)))
        .thenComparing(getSorterMaxScoreFirst());
  }

  /**
   * max score first, score descending
   *
   * @return sorter
   */
  public static Comparator<FeatureAnnotation> getSorterMaxScoreFirst() {
    return Comparator.comparing(FeatureAnnotation::getScore,
        Comparator.nullsLast(Comparator.reverseOrder()));
  }

  /**
   * Stream all instances of {@link FeatureAnnotation}
   */
  public static Stream<FeatureAnnotation> streamFeatureAnnotations(final FeatureListRow row) {
    return row.streamAllFeatureAnnotations().filter(ann -> ann instanceof FeatureAnnotation)
        .map(FeatureAnnotation.class::cast);
  }

  public static void calculateBoundTypes(CompoundDBAnnotation annotation, FeatureListRow row) {
    ConnectedTypeCalculation.LIST.forEach(calc -> calc.calculateIfAbsent(row, annotation));
  }
}
