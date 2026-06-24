package io.github.mzmine.modules.dataanalysis.compounddashboard;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.util.color.SimpleColorPalette;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Computes a per-row color assignment for a {@link CompoundRow}'s evidence dashboard.
 * <p>
 * Rules:
 * <ul>
 *   <li>Color slot 0 of the palette is reserved for rows without an {@link IonIdentity}.</li>
 *   <li>Each distinct {@link IonIdentity} gets the next color from the palette.</li>
 *   <li>If a member row is itself a nested {@link CompoundRow} (i.e. an adduct group whose
 *       sub-members are isotopologues), the same color is propagated to every sub-member.</li>
 * </ul>
 * The palette neutral "representative" color is exposed separately and used by the MS1 background
 * scan only.
 * <p>
 * Pure utility, safe to call off the FX thread. The caller is responsible for cloning the palette
 * with index reset (see {@link SimpleColorPalette#clone(boolean)}).
 */
public final class CompoundDashboardColoring {

  private CompoundDashboardColoring() {
  }

  public record ColorAssignment(@NotNull Map<FeatureListRow, Color> rowColors,
                                @NotNull Color representativeBackgroundColor,
                                @NotNull Color noIonColor,
                                @NotNull LinkedHashMap<IonIdentity, Color> ionColors) {

    public @NotNull Color colorFor(@NotNull final FeatureListRow row) {
      final Color c = rowColors.get(row);
      return c != null ? c : noIonColor;
    }
  }

  /**
   * Build the assignment. {@code palette} should be a clone with the next-color counter reset to
   * 0. The first slot is consumed for the no-ion color and subsequent calls to
   * {@link SimpleColorPalette#getNextColor()} cycle through the remaining slots.
   */
  public static @NotNull ColorAssignment assign(@NotNull final CompoundRow compound,
      @NotNull final SimpleColorPalette palette) {

    // slot 0 -> no-ion color; burn the index so the first ion identity gets slot 1.
    final Color noIon = palette.get(0);
    palette.getNextColor();

    final Map<FeatureListRow, Color> rowColors = new IdentityHashMap<>();
    final LinkedHashMap<IonIdentity, Color> ionColors = new LinkedHashMap<>();

    for (final CompoundFeatureMember m : compound.getCompoundMembers()) {
      final FeatureListRow row = m.row();
      final IonIdentity ion = row.getBestIonIdentity();
      final Color color;
      if (ion == null) {
        color = noIon;
      } else {
        color = ionColors.computeIfAbsent(ion, _ -> palette.getNextColor());
      }
      rowColors.put(row, color);
      // Propagate the same color to every sub-member of a nested CompoundRow (typical adduct →
      // isotope grouping). assumption: nested member rows inherit their parent adduct's color.
      if (row instanceof CompoundRow nested) {
        for (final FeatureListRow sub : nested.getMemberRows()) {
          rowColors.put(sub, color);
        }
      }
    }
    return new ColorAssignment(rowColors, ConfigService.getDefaultColorPalette().getNeutralColor(),
        noIon, ionColors);
  }

  /**
   * Convenience: walk the top-level members, recursing into nested {@link CompoundRow} adduct
   * groups, and return every {@link FeatureListRow} encountered. Top-level entries come first,
   * followed by sub-members in the order they appear.
   */
  public static @NotNull List<FeatureListRow> flattenAllMemberRows(
      @NotNull final CompoundRow compound) {
    final List<FeatureListRow> out = new ArrayList<>();
    for (final CompoundFeatureMember m : compound.getCompoundMembers()) {
      final FeatureListRow row = m.row();
      out.add(row);
      if (row instanceof CompoundRow nested) {
        out.addAll(nested.getMemberRows());
      }
    }
    return out;
  }

  /**
   * @return ion-type label like "[M+H]+" if the row has a best ion identity, otherwise null.
   */
  public static @Nullable String ionTypeLabel(@NotNull final FeatureListRow row) {
    final IonIdentity ion = row.getBestIonIdentity();
    return ion == null ? null : ion.getIonType().toString();
  }

  /**
   * Short on-plot label for the parent stick of an ion group. Examples: {@code "[M+H]+"} or, when
   * no ion identity is set, just the formatted m/z (no {@code "m/z"} prefix).
   */
  public static @NotNull String shortIonLabel(@NotNull final FeatureListRow row) {
    final String ion = ionTypeLabel(row);
    if (ion != null) {
      return ion;
    }
    final Double mz = row.getAverageMZ();
    return mz == null ? ("row " + row.getID()) : ConfigService.getGuiFormats().mz(mz);
  }

  /**
   * Long descriptive label, used for hover tooltips. Examples: {@code "[M+H]+ m/z 123.4567"} or
   * {@code "unknown m/z 123.4567"} when no ion identity is set.
   */
  public static @NotNull String longIonLabel(@NotNull final FeatureListRow row) {
    final String ion = ionTypeLabel(row);
    final Double mz = row.getAverageMZ();
    final String mzStr = mz == null ? "?" : ConfigService.getGuiFormats().mz(mz);
    return (ion == null ? "unknown" : ion) + " m/z " + mzStr;
  }
}
