package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A single piece of conflicting evidence detected within a {@link CompoundRow}. Recorded on the
 * involved member rows and rolled up on the owning compound row (see
 * {@link io.github.mzmine.datamodel.features.types.compoundlist.CompoundContradictionListType}).
 *
 * @param type           the kind of conflict
 * @param severity       0..1, higher = more serious
 * @param compoundId     the {@link CompoundRow#getCompoundId()} this contradiction belongs to (lets
 *                       a member row that is part of several compounds keep one flat list)
 * @param involvedRowIds source-row {@link io.github.mzmine.datamodel.features.FeatureListRow#getID()}
 *                       values that participate in the conflict
 * @param detail         human-readable explanation
 */
public record CompoundContradiction(@NotNull ContradictionType type, float severity, int compoundId,
                                    @NotNull List<Integer> involvedRowIds, @NotNull String detail) {

  public CompoundContradiction {
    involvedRowIds = List.copyOf(involvedRowIds);
  }

  @Override
  public String toString() {
    return type.getLabel() + " (" + detail + ")";
  }

  public enum ContradictionType implements UniqueIdSupplier {
    /**
     * Two members carry MS2 spectral-library matches to different structures.
     */
    MS2_ANNOTATION_CONFLICT,
    /**
     * An MS1 (compound DB) annotation disagrees with an MS2 spectral-library match.
     */
    MS1_MS2_CONFLICT,
    /**
     * Members carry incompatible molecular formula predictions.
     */
    FORMULA_CONFLICT,
    /**
     * Member retention times are spread wider than the allowed threshold.
     */
    RT_SPREAD;

    public @NotNull String getLabel() {
      final String s = name().toLowerCase().replace('_', ' ');
      return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public @NotNull String getUniqueID() {
      return switch (this) {
        case MS2_ANNOTATION_CONFLICT -> "ms2_annotation_conflict";
        case MS1_MS2_CONFLICT -> "ms1_ms2_conflict";
        case FORMULA_CONFLICT -> "formula_conflict";
        case RT_SPREAD -> "rt_spread";
      };
    }

    public static @NotNull ContradictionType fromUniqueID(@NotNull final String id) {
      for (final ContradictionType t : values()) {
        if (t.getUniqueID().equals(id)) {
          return t;
        }
      }
      throw new IllegalArgumentException("Unknown contradiction type id: " + id);
    }
  }
}
