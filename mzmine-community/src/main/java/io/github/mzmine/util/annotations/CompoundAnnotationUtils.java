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

package io.github.mzmine.util.annotations;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularDataModel;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummaryOrder;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.AnnotationMethodType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.MissingValueType;
import io.github.mzmine.datamodel.features.types.annotations.MolecularStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseNameType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.abstr.ScoreType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypeParser;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.collections.CollectionUtils;
import io.github.mzmine.util.collections.SortOrder;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CompoundAnnotationUtils {

  /**
   * This list does <b>not</b> represent an absolute order of annotation priorities, but may be used
   * for rough pre-grouping if required.
   */
  public static final List<DataType> annotationTypePriority = DataTypes.getAll(
      CompoundDatabaseMatchesType.class, LipidMatchListType.class,
      SpectralLibraryMatchesType.class);

  private static final Logger logger = Logger.getLogger(CompoundAnnotationUtils.class.getName());

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

    final MissingValueType notAnnotatedType =
        mapMissingValues ? DataTypes.get(MissingValueType.class) : null;

    final Map<@NotNull FeatureListRow, @NotNull AnnotationSummary> rowsToBestAnnotationSummary = mapRowsToBestAnnotationSummary(
        rows, true);

    Map<FeatureListRow, DataType<?>> map = new HashMap<>();

    for (final Entry<@NotNull FeatureListRow, @NotNull AnnotationSummary> entry : rowsToBestAnnotationSummary.entrySet()) {
      AnnotationSummary summary = entry.getValue();
      FeatureListRow row = entry.getKey();

      final DataType best =
          summary.annotation() != null ? DataTypes.get(summary.annotation().getDataType())
              : notAnnotatedType;

      if (best != null) {
        map.put(row, best);
      }
    }
    return map;
  }

  /**
   * @param types can contain duplicates or nulls - that are filtered out
   * @param order order either ascending from missing to best match or reverse
   * @return map of DataType to their index in
   * {@link CompoundAnnotationUtils#annotationTypePriority}. If {@link MissingValueType} is found,
   * it is added as the last rank priority to the map
   */
  @NotNull
  public static Map<DataType<?>, Integer> rankUniqueAnnotationTypes(Collection<DataType<?>> types,
      @NotNull final SortOrder order) {
    var sortedUniqueTypes = types.stream().filter(Objects::nonNull).distinct()
        .filter(CompoundAnnotationUtils::isAnnotationOrMissingType).sorted(
            (d1, d2) -> order.intComparator()
                .compare(annotationTypePriority.indexOf(d1), annotationTypePriority.indexOf(d2)))
        .toList();
    return CollectionUtils.indexMapOrdered(sortedUniqueTypes);
  }

  /**
   * @return true if type is either annotation type or {@link MissingValueType}
   */
  public static boolean isAnnotationOrMissingType(final DataType<?> type) {
    return type instanceof MissingValueType || annotationTypePriority.contains(type);
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
        .collect(Collectors.groupingBy(CompoundAnnotationUtils::getAnnotationIdentifier));
    // sort by number of adducts + modifications
    List<T> oneMatchPerCompound = compoundsMap.values().stream()
        .map(compound -> compound.stream().min(getSorterLeastModifiedCompoundFirst()).orElse(null))
        .filter(Objects::nonNull).toList();

    return oneMatchPerCompound;
  }

  /**
   * An identifier to group matches by compound name or if this is unavailable by InChI key or
   * other
   */
  @NotNull
  public static <T extends FeatureAnnotation> String getAnnotationIdentifier(T match) {
    String inChIKey = match.getInChIKey();
    if (StringUtils.hasValue(inChIKey)) {
      return inChIKey;
    }
    String compoundName = match.getCompoundName();
    if (StringUtils.hasValue(compoundName)) {
      return compoundName;
    }
    var inChI = match.getInChI();
    if (StringUtils.hasValue(inChI)) {
      return inChI;
    }
    var mz = match.getPrecursorMZ();
    return mz == null ? "UNKNOWN" : String.valueOf(mz);
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
   * max score first, score descending. Sorts by {@link FeatureAnnotation#getScore()}, which may be
   * of different nature. (e.g. Compound match score and cosine). Consider using
   * {@link CompoundAnnotationUtils#getAllFeatureAnnotationsByDescendingConfidence(FeatureListRow)}
   * or {@link CompoundAnnotationUtils#streamBestAnnotationSummaries(List, boolean)} and sort using
   * {@link AnnotationSummaryOrder#SCHYMANSKI_HIGH_TO_LOW_CONFIDENCE} and
   * {@link AnnotationSummaryOrder#SCHYMANSKI_LOW_TO_HIGH_CONFIDENCE}
   *
   * @return sorter
   */
  public static Comparator<@NotNull FeatureAnnotation> getSorterMaxScoreFirst() {
    return Comparator.comparing(FeatureAnnotation::getScore,
        Comparator.nullsLast(Comparator.reverseOrder()));
  }

  /**
   * @return {@link Optional#ofNullable(Object)} of {@link FeatureListRow#getPreferredAnnotation()}
   */
  public static Optional<FeatureAnnotation> getBestFeatureAnnotation(
      @NotNull final FeatureListRow row) {
    // specifically use the row method and dont get the best annotations and sort, as the best
    // annotation may also be user-defined
    return Optional.ofNullable(row.getPreferredAnnotation());
  }

  public static void calculateBoundTypes(CompoundDBAnnotation annotation, FeatureListRow row) {
    ConnectedTypeCalculation.LIST.forEach(calc -> calc.calculateIfAbsent(row, annotation));
  }

  /**
   * Get value from annotation by {@link DataType} key. {@link SpectralDBAnnotation} currently does
   * not use DataTypes, but {@link DBEntryField#fromDataTypeClass(Class)} allows mapping between the
   * keys.
   *
   * @param annotation to extract value from
   * @param type       key to extract value
   * @param <T>        Type of value
   * @return The mapped value for this annotation and key or null if there is no mapping.
   */
  public static <T> @Nullable T getTypeValue(@NotNull FeatureAnnotation annotation,
      @NotNull Class<? extends DataType<T>> type) {
    return getTypeValue(annotation, DataTypes.get(type));
  }

  /**
   * Get value from annotation by {@link DataType} key. {@link SpectralDBAnnotation} currently does
   * not use DataTypes, but {@link DBEntryField#fromDataTypeClass(Class)} allows mapping between the
   * keys.
   *
   * @param annotation to extract value from
   * @param type       key to extract value
   * @param <T>        Type of value
   * @return The mapped value for this annotation and key or null if there is no mapping.
   */
  public static <T> @Nullable T getTypeValue(@NotNull FeatureAnnotation annotation,
      @NotNull DataType<T> type) {
    switch (annotation) {
      case CompoundDBAnnotation db -> db.get(type);
      case SpectralDBAnnotation db -> {
        DBEntryField f = DBEntryField.fromDataType(type);
        if (f != DBEntryField.UNSPECIFIED) {
          // types like MolecularStructureType have no mapping in DBEntryField, so only use
          // fromDataType in case we get something useful. Otherwise, fallback to default mapping below
          return db.getEntry().getOrElse(f, null);
        }
      }
//      Matched lipids currently uses the default case below
      default -> {
      }
    }
    ;

    return (T) switch (type) {
      case PrecursorMZType _, MZType _ -> annotation.getPrecursorMZ();
      case SmilesStructureType _ -> annotation.getSmiles();
      case CompoundNameType _ -> annotation.getCompoundName();
      case IonTypeType _ -> annotation.getAdductType();
      case FormulaType _ -> annotation.getFormula();
      case InChIStructureType _ -> annotation.getInChI();
      case InChIKeyStructureType _ -> annotation.getInChIKey();
      case MolecularStructureType _ -> annotation.getStructure();
      case CCSType _ -> annotation.getCCS();
      case MobilityType _ -> annotation.getMobility();
      case ScoreType _ -> annotation.getScore();
      case RTType _ -> annotation.getRT();
      case DatabaseNameType _ -> annotation.getDatabase();
      case AnnotationMethodType _ -> annotation.getAnnotationMethodName();
      default -> null;
    };
  }

  /**
   * @return usually the signed charge if present. Either from adduct or from charge type
   */
  public static OptionalInt extractCharge(@Nullable final FeatureAnnotation annotation) {
    if (annotation == null) {
      return OptionalInt.empty();
    }

    IonType adduct = annotation.getAdductType();
    if (adduct != null) {
      return OptionalInt.of(adduct.getCharge());
    }

    Integer annCharge = getTypeValue(annotation, ChargeType.class);
    if (annCharge != null) {
      return OptionalInt.of(annCharge);
    }

    return OptionalInt.empty();
  }

  /**
   * @return absolute charge if present
   */
  public static OptionalInt extractAbsCharge(@Nullable final FeatureAnnotation annotation) {
    return extractCharge(annotation).stream().map(Math::abs).findFirst();
  }

  /**
   * Convert spectral library entry to compound DB entry. Tries to copy over all fields
   */
  @NotNull
  public static CompoundDBAnnotation convertSpectralToCompoundDb(final SpectralLibraryEntry spec) {
    SimpleCompoundDBAnnotation db = new SimpleCompoundDBAnnotation();
    spec.getFields().forEach((field, value) -> {
      Class<? extends DataType> dataType = field.getDataType();
      if (DataTypes.isAbstractType(dataType)) {
        return;
      }

      try {
        final DataType instance = DataTypes.get(dataType);
        if (instance.getValueClass().isInstance(value)) {
          db.putIfNotNull((Class) dataType, value);
        } else if (instance instanceof IonTypeType && value instanceof String s) {
          var ionType = IonTypeParser.parse(s);
          db.putIfNotNull((Class) dataType, ionType);
        } else {
          logger.finest("Skipping value conversion of field\t" + field + "  for type\t"
              + instance.getUniqueID() + " with value\t" + value);
        }
      } catch (Exception e) {
        try {
          logger.finer(
              "Cannot convert value from spectral library to compound DB entry: %s = %s".formatted(
                  dataType.getName(), value));
        } catch (Exception ex) {
        }
      }
    });

    // currently spec library entry uses MZType and not PrecursorMZType
    // for now just make sure to delete MZType for compatibility that MZType is not used in CompoundDB
    db.put(MZType.class, null);
    db.putIfNotNull(PrecursorMZType.class, spec.getPrecursorMZ());

    MolecularStructure structure = spec.getStructure();
    if (structure != null) {
      db.setStructure(structure);
    }
    return db;
  }

  public static @Nullable String getBestFormula(@NotNull ModularFeatureListRow row) {
    return getAllFeatureAnnotationsByDescendingConfidence(row).stream()
        .map(FeatureAnnotation::getFormula).filter(Objects::nonNull).findFirst().orElseGet(() -> {
          final List<ResultFormula> formulas = row.getFormulas();
          return formulas.isEmpty() ? null : formulas.getFirst().getFormulaAsString();
        });
  }

  /**
   * An additional json string that may contain additional fields that are otherwise not captured.
   *
   */
  @Nullable
  public static String getAdditionalJson(FeatureAnnotation annotation) {
    return switch (annotation) {
      case CompoundDBAnnotation db -> db.getAdditionalJson();
      case SpectralDBAnnotation db -> db.getAdditionalJson();
      case MatchedLipid db -> null;
      default -> null;
    };
  }

  /**
   *
   * @param topN The maximum number of annotations per type.
   * @return A list of the top-N feature annotations in no particular order.
   */
  public static @NotNull List<@NotNull FeatureAnnotation> getTopNFeatureAnnotations(
      @Nullable ModularDataModel row, int topN) {
    if (row == null || row.isEmpty()) {
      return List.of();
    }

    final List<@NotNull FeatureAnnotation> results = new ArrayList<>(annotationTypePriority.size());
    for (final DataType type : annotationTypePriority) {
      final Object value = row.get(type);
      if (value == null) {
        continue;
      }

      if (value instanceof List list) {
        if (list.isEmpty()) {
          continue;
        }
        for (int i = 0; i < list.size() && i < topN; i++) {
          Object o = list.get(i);
          if (o instanceof FeatureAnnotation a) {
            results.add(a);
          }
        }
      } else if (value instanceof FeatureAnnotation a) {
        results.add(a);
      }
    }
    return results;
  }

  /**
   *
   * @return A list of all feature annotations in no particular order.
   */
  public static @NotNull List<@NotNull FeatureAnnotation> getAllFeatureAnnotations(
      @Nullable ModularDataModel row) {
    return getTopNFeatureAnnotations(row, Integer.MAX_VALUE);
  }

  /**
   *
   * @return A list of the top annotation per type. Not sorted by overall confidence.
   */
  public static @NotNull List<@NotNull FeatureAnnotation> getTopAnnotationsPerType(
      @Nullable final ModularDataModel model) {
    return getTopNFeatureAnnotations(model, 1);
  }

  /**
   *
   * @return A map of {@link FeatureAnnotation#getDataType()} -> {@link FeatureAnnotation} for the
   * given row.
   */
  public static @NotNull Map<@NotNull Class<? extends DataType>, @NotNull FeatureAnnotation> getTopAnnotationsPerTypeMap(
      @Nullable ModularDataModel model) {
    return getTopAnnotationsPerType(model).stream()
        .collect(Collectors.toMap(FeatureAnnotation::getDataType, a -> a));
  }

  public static @Nullable AnnotationSummary getBestAnnotationSummary(
      @Nullable final FeatureListRow model) {
    if (model == null) {
      return null;
    }
    Comparator<@Nullable AnnotationSummary> sorter = model.getFeatureList()
        .getAnnotationSortConfig().sortOrder().getComparator();
    return getTopAnnotationsPerType(model).stream().map(a -> AnnotationSummary.of(model, a))
        .min(sorter).orElse(null);
  }

  /**
   *
   * @param topN Number of annotations <b>per</b> annotation type.
   * @return Annotation types sorted by descending confidence as defined by
   * {@link FeatureList#getAnnotationSortConfig()}
   */
  public static @NotNull List<@NotNull FeatureAnnotation> getFeatureAnnotationsByDescendingConfidence(
      @Nullable final FeatureListRow row, int topN) {
    if (row == null) {
      return List.of();
    }
    Comparator<@Nullable AnnotationSummary> sorter = row.getFeatureList().getAnnotationSortConfig()
        .sortOrder().getComparator();
    return getTopNFeatureAnnotations(row, topN).stream().map(a -> AnnotationSummary.of(row, a))
        .sorted(sorter).map(AnnotationSummary::annotation)
        //.filter(Objects::nonNull) // cannot be null because input is not null
        .toList();
  }

  /**
   *
   * @return Annotation types sorted by descending confidence as defined by
   * {@link AnnotationSummaryOrder#SCHYMANSKI_HIGH_TO_LOW_CONFIDENCE}
   */
  public static @NotNull List<@NotNull FeatureAnnotation> getAllFeatureAnnotationsByDescendingConfidence(
      @Nullable final FeatureListRow row) {
    return getFeatureAnnotationsByDescendingConfidence(row, Integer.MAX_VALUE);
  }

  /**
   * @param includeUnannotated if false, not annotated rows are dropped in the returned stream.
   * @return A stream of the highest ranked {@link AnnotationSummary}s per row.
   */
  public static @NotNull Stream<@NotNull AnnotationSummary> streamBestAnnotationSummaries(
      @NotNull List<@NotNull FeatureListRow> rows, final boolean includeUnannotated) {
    return rows.stream().map(row -> Objects.requireNonNullElse(getBestAnnotationSummary(row),
        AnnotationSummary.of(row, null))).filter(s -> s.annotation() != null || includeUnannotated);
  }

  /**
   *
   * @param includeUnannotated if false, not annotated rows are dropped from the returned map.
   * @return Mapping of row -> annotation summary. {@link AnnotationSummary#annotation()} may be
   * null.
   */
  public static Map<@NotNull FeatureListRow, @NotNull AnnotationSummary> mapRowsToBestAnnotationSummary(
      @NotNull List<@NotNull FeatureListRow> rows, final boolean includeUnannotated) {
    return CompoundAnnotationUtils.streamBestAnnotationSummaries(rows, includeUnannotated)
        .collect(Collectors.toMap(AnnotationSummary::row, a -> a));
  }

  /**
   * Convenience method to map anything that references a row to the best annotation summary.
   *
   * @see this#mapRowsToBestAnnotationSummary(List, boolean)
   */
  public static <T> Map<@NotNull T, @NotNull AnnotationSummary> mapRowsToBestAnnotationSummary(
      @NotNull List<@NotNull T> data, @NotNull Function<T, @NotNull FeatureListRow> mapper,
      final boolean includeUnannotated) {

    final Map<T, AnnotationSummary> result = new HashMap<>();
    for (T d : data) {
      AnnotationSummary summary = getBestAnnotationSummary(mapper.apply(d));
      if (summary == null && !includeUnannotated) {
        continue;
      }
      result.put(d, summary);
    }

    return result;
  }
}
