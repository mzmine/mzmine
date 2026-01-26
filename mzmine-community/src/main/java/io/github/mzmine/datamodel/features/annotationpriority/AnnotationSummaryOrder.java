package io.github.mzmine.datamodel.features.annotationpriority;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.Comparator.reverseOrder;

import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import java.util.Comparator;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum AnnotationSummaryOrder implements UniqueIdSupplier {

  SCHYMANSKI, MSI, LIPIDS_FIRST;

  private static final Comparator<@NotNull AnnotationSummary> SCORES_LOW_TO_HIGH = Comparator.comparing(
          AnnotationSummary::ms2Score, nullsFirst(naturalOrder()))
      .thenComparing(AnnotationSummary::isotopeScore, nullsFirst(naturalOrder()))
      .thenComparing(AnnotationSummary::mzScore, nullsFirst(naturalOrder()))
      .thenComparing(AnnotationSummary::rtScore, nullsFirst(naturalOrder()))
      .thenComparing(AnnotationSummary::riScore, nullsFirst(naturalOrder()))
      .thenComparing(AnnotationSummary::ccsScore, nullsFirst(naturalOrder()));

  private static final Comparator<@NotNull AnnotationSummary> SCHYMANSKI_LOW_TO_HIGH_NON_NULL = Comparator.comparing(
          AnnotationSummary::deriveSchymanskiLevel, nullsFirst(reverseOrder()))
      .thenComparing(SCORES_LOW_TO_HIGH);

  public static final Comparator<@Nullable AnnotationSummary> SCHYMANSKI_LOW_TO_HIGH_CONFIDENCE = Comparator.comparing(
      Function.identity(), nullsFirst(SCHYMANSKI_LOW_TO_HIGH_NON_NULL));
  public static final Comparator<@Nullable AnnotationSummary> SCHYMANSKI_HIGH_TO_LOW_CONFIDENCE = SCHYMANSKI_LOW_TO_HIGH_CONFIDENCE.reversed();

  private static final Comparator<@NotNull AnnotationSummary> MSI_LOW_TP_HIGH_NON_NULL = Comparator.comparing(
      AnnotationSummary::deriveMsiLevel, Comparator.nullsFirst(Comparator.reverseOrder()));
  private static final Comparator<@Nullable AnnotationSummary> MSI_LOW_TO_HIGH = Comparator.comparing(
      Function.identity(), nullsFirst(MSI_LOW_TP_HIGH_NON_NULL).thenComparing(SCORES_LOW_TO_HIGH));

  public static final Comparator<@Nullable AnnotationSummary> LIPIDS_FIRST_LOW_TO_HIGH_CONFIDENCE = Comparator.comparing(
      Function.identity(), nullsFirst(Comparator.<AnnotationSummary>comparingInt(a -> {
        DataType<?> dt = DataTypes.get(a.annotation().getDataType());
        return switch (dt) {
          case LipidMatchListType _ -> 1;
          case SpectralLibraryMatchesType _ -> 2;
          case CompoundDatabaseMatchesType _ -> 3;
          case null, default -> 4;
        };
      }))).thenComparing(SCORES_LOW_TO_HIGH);

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case SCHYMANSKI -> "schymanski";
      case MSI -> "msi";
      case LIPIDS_FIRST -> "lipids_first";
    };
  }

  public Comparator<@Nullable AnnotationSummary> getComparator() {
    return switch (this) {
      case MSI -> MSI_LOW_TO_HIGH;
      case SCHYMANSKI -> SCHYMANSKI_LOW_TO_HIGH_CONFIDENCE;
      case LIPIDS_FIRST -> LIPIDS_FIRST_LOW_TO_HIGH_CONFIDENCE;
    };
  }
}
