package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMembersType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In-place mutation helpers for an existing {@link CompoundList}: change the representative of a
 * compound row, split selected members off into a new compound row, and merge compound rows.
 * <p>
 * All membership writes go through {@link CompoundList#setRows(List)} so the reverse-member index
 * and bindings stay consistent. Callers are expected to invoke these from the FX thread (e.g. from
 * a context-menu action).
 */
public final class CompoundRowUtils {

  private static final Logger logger = Logger.getLogger(CompoundRowUtils.class.getName());

  private CompoundRowUtils() {
  }

  /**
   * Replace the preferred row of {@code compound} with {@code newRepresentative}. The previous
   * representative is demoted to {@link CompoundMemberRole#CORRELATED}. No-op if the row is already
   * the representative or is not a member of the compound.
   *
   * @return true if the representative changed
   */
  public static boolean setRepresentative(@NotNull final ModularCompoundRow compound,
      @NotNull final ModularFeatureListRow newRepresentative) {
    final CompoundMembers current = compound.getCompoundMembersData();
    if (current.preferredRow().getID().equals(newRepresentative.getID())) {
      return false;
    }
    final List<CompoundFeatureMember> oldMembers = current.members();
    boolean found = false;
    final List<CompoundFeatureMember> updated = new ArrayList<>(oldMembers.size());
    for (final CompoundFeatureMember m : oldMembers) {
      if (m.row().getID().equals(newRepresentative.getID())) {
        updated.add(
            new CompoundFeatureMember(m.row(), CompoundMemberRole.REPRESENTATIVE, m.score()));
        found = true;
      } else if (m.role() == CompoundMemberRole.REPRESENTATIVE) {
        updated.add(new CompoundFeatureMember(m.row(), CompoundMemberRole.CORRELATED, m.score()));
      } else {
        updated.add(m);
      }
    }
    if (!found) {
      logger.warning(
          () -> "Row id=" + newRepresentative.getID() + " is not a member of compound id="
              + compound.getCompoundId() + "; cannot set as representative");
      return false;
    }
    final CompoundMembers next = new CompoundMembers(newRepresentative, List.copyOf(updated),
        current.confidence());
    // membership stays the same; only the preferredRow + role flags change. No need to rebuild
    // the reverse-member index — listeners on CompoundMembersType propagate the change.
    compound.set(CompoundMembersType.class, next);
    return true;
  }

  /**
   * Remove {@code rowsToMove} from {@code source} and create a new compound row that holds them.
   * The new compound's representative is whichever moved member was already REPRESENTATIVE in the
   * source; if none, the first moved member is promoted. Confidence and neutral mass are inherited
   * from the source. If the source ends up empty it is dropped from the list.
   *
   * @return the newly created compound row, or null if no rows were actually moved
   */
  public static @Nullable ModularCompoundRow splitIntoNewCompound(@NotNull final CompoundList list,
      @NotNull final ModularCompoundRow source,
      @NotNull final Collection<? extends ModularFeatureListRow> rowsToMove) {
    if (rowsToMove.isEmpty()) {
      return null;
    }
    final Set<Integer> moveIds = collectIds(rowsToMove);
    final CompoundMembers sourceData = source.getCompoundMembersData();
    final List<CompoundFeatureMember> moved = new ArrayList<>(rowsToMove.size());
    final List<CompoundFeatureMember> remaining = new ArrayList<>(sourceData.members().size());
    for (final CompoundFeatureMember m : sourceData.members()) {
      if (moveIds.contains(m.row().getID())) {
        moved.add(m);
      } else {
        remaining.add(m);
      }
    }
    if (moved.isEmpty()) {
      return null;
    }

    final List<CompoundFeatureMember> movedNormalized = ensureRepresentative(moved);
    final ModularFeatureListRow newPreferred = pickRepresentativeRow(movedNormalized);
    final int newId = nextCompoundId(list);
    final ModularCompoundRow newCompound = new ModularCompoundRow(list, newId, newPreferred,
        movedNormalized, sourceData.confidence(), source.getCompoundNeutralMass());

    final List<ModularCompoundRow> nextRows = new ArrayList<>(list.getRows());
    final int sourceIndex = nextRows.indexOf(source);
    if (!remaining.isEmpty()) {
      final List<CompoundFeatureMember> remainingNormalized = ensureRepresentative(remaining);
      final ModularFeatureListRow remainingPreferred = pickRepresentativeRow(remainingNormalized);
      source.set(CompoundMembersType.class,
          new CompoundMembers(remainingPreferred, List.copyOf(remainingNormalized),
              sourceData.confidence()));
    } else if (sourceIndex >= 0) {
      nextRows.remove(sourceIndex);
    }
    final int insertAt =
        sourceIndex >= 0 ? Math.min(nextRows.size(), sourceIndex + (remaining.isEmpty() ? 0 : 1))
            : nextRows.size();
    nextRows.add(insertAt, newCompound);

    list.setRows(nextRows);
    return newCompound;
  }

  /**
   * Merge member rows from {@code otherCompounds} and {@code extraMemberRows} into {@code target}.
   * <ul>
   *   <li>{@code otherCompounds}: every compound in this collection is removed from the list and
   *       its members are appended to {@code target} (duplicates skipped).
   *   <li>{@code extraMemberRows}: each row is appended to {@code target} (duplicates skipped) and
   *       stripped from any other compound in the list that currently lists it as a member.
   * </ul>
   * Target's representative is preserved; any secondary REPRESENTATIVE entries are demoted.
   *
   * @return true if anything changed
   */
  public static boolean mergeCompoundRows(@NotNull final CompoundList list,
      @NotNull final ModularCompoundRow target,
      @NotNull final Collection<ModularCompoundRow> otherCompounds,
      @NotNull final Collection<? extends ModularFeatureListRow> extraMemberRows) {

    // identity set to compare compound rows by reference
    final Set<ModularCompoundRow> toRemove = newIdentitySet(otherCompounds);
    toRemove.remove(target);

    final CompoundMembers targetData = target.getCompoundMembersData();
    final List<CompoundFeatureMember> merged = new ArrayList<>(targetData.members());
    final Set<Integer> presentIds = new HashSet<>(merged.size() * 2);
    for (final CompoundFeatureMember m : merged) {
      presentIds.add(m.row().getID());
    }

    boolean changed = false;

    // 1) Absorb members from compounds being merged
    for (final ModularCompoundRow other : toRemove) {
      for (final CompoundFeatureMember m : other.getCompoundMembersData().members()) {
        if (presentIds.add(m.row().getID())) {
          merged.add(demoteIfRepresentative(m));
          changed = true;
        }
      }
    }

    // 2) Detach extras from any *other* compound (excluding target and ones being removed entirely)
    final Set<Integer> extraIds = collectIds(extraMemberRows);
    final Map<ModularCompoundRow, List<CompoundFeatureMember>> trimmed = new HashMap<>();
    if (!extraIds.isEmpty()) {
      for (final ModularCompoundRow cr : list.getRows()) {
        if (cr == target || toRemove.contains(cr)) {
          continue;
        }
        final List<CompoundFeatureMember> kept = new ArrayList<>(
            cr.getCompoundMembersData().members().size());
        boolean removedAny = false;
        for (final CompoundFeatureMember m : cr.getCompoundMembersData().members()) {
          if (extraIds.contains(m.row().getID())) {
            removedAny = true;
          } else {
            kept.add(m);
          }
        }
        if (removedAny) {
          trimmed.put(cr, kept);
          changed = true;
        }
      }
    }
    // 2b) Append extras to target
    for (final ModularFeatureListRow extra : extraMemberRows) {
      if (presentIds.add(extra.getID())) {
        merged.add(new CompoundFeatureMember(extra, CompoundMemberRole.CORRELATED, 0.5f));
        changed = true;
      }
    }

    if (!changed) {
      return false;
    }

    final List<CompoundFeatureMember> normalized = ensureSingleRepresentative(merged,
        targetData.preferredRow());
    target.set(CompoundMembersType.class,
        new CompoundMembers(targetData.preferredRow(), List.copyOf(normalized),
            targetData.confidence()));

    // apply trimmed membership to other compounds; drop them if they end up empty
    final Set<ModularCompoundRow> emptyAfterTrim = newIdentitySet(List.of());
    for (final var entry : trimmed.entrySet()) {
      final ModularCompoundRow cr = entry.getKey();
      final List<CompoundFeatureMember> kept = entry.getValue();
      if (kept.isEmpty()) {
        emptyAfterTrim.add(cr);
        continue;
      }
      final List<CompoundFeatureMember> keptNormalized = ensureRepresentative(kept);
      final ModularFeatureListRow newPref = pickRepresentativeRow(keptNormalized);
      cr.set(CompoundMembersType.class, new CompoundMembers(newPref, List.copyOf(keptNormalized),
          cr.getCompoundMembersData().confidence()));
    }

    // Rebuild the top-level row list
    final List<ModularCompoundRow> nextRows = new ArrayList<>(list.getRows().size());
    for (final ModularCompoundRow cr : list.getRows()) {
      if (toRemove.contains(cr) || emptyAfterTrim.contains(cr)) {
        continue;
      }
      nextRows.add(cr);
    }

    list.setRows(nextRows);
    return true;
  }

  // ----- helpers -----

  private static @NotNull CompoundFeatureMember demoteIfRepresentative(
      @NotNull final CompoundFeatureMember m) {
    return m.role() == CompoundMemberRole.REPRESENTATIVE ? new CompoundFeatureMember(m.row(),
        CompoundMemberRole.CORRELATED, m.score()) : m;
  }

  /**
   * If {@code members} contains no REPRESENTATIVE, promote the first member to representative
   * (score 1.0). If more than one REPRESENTATIVE exists, keep the first and demote the others to
   * CORRELATED.
   */
  private static @NotNull List<CompoundFeatureMember> ensureRepresentative(
      @NotNull final List<CompoundFeatureMember> members) {
    if (members.isEmpty()) {
      return members;
    }
    boolean seenRep = false;
    final List<CompoundFeatureMember> out = new ArrayList<>(members.size());
    for (final CompoundFeatureMember m : members) {
      if (m.role() == CompoundMemberRole.REPRESENTATIVE) {
        if (seenRep) {
          out.add(new CompoundFeatureMember(m.row(), CompoundMemberRole.CORRELATED, m.score()));
        } else {
          out.add(m);
          seenRep = true;
        }
      } else {
        out.add(m);
      }
    }
    if (!seenRep) {
      final CompoundFeatureMember first = out.removeFirst();
      out.addFirst(new CompoundFeatureMember(first.row(), CompoundMemberRole.REPRESENTATIVE, 1.0f));
    }
    return out;
  }

  /**
   * Pins the representative to the entry whose row id matches {@code preferred}. Any other
   * REPRESENTATIVE entry is demoted to CORRELATED. If {@code preferred} is not in {@code members},
   * it is prepended as the representative.
   */
  private static @NotNull List<CompoundFeatureMember> ensureSingleRepresentative(
      @NotNull final List<CompoundFeatureMember> members,
      @NotNull final ModularFeatureListRow preferred) {
    final List<CompoundFeatureMember> out = new ArrayList<>(members.size());
    boolean placed = false;
    for (final CompoundFeatureMember m : members) {
      if (m.row().getID().equals(preferred.getID())) {
        out.add(new CompoundFeatureMember(m.row(), CompoundMemberRole.REPRESENTATIVE, 1.0f));
        placed = true;
      } else if (m.role() == CompoundMemberRole.REPRESENTATIVE) {
        out.add(new CompoundFeatureMember(m.row(), CompoundMemberRole.CORRELATED, m.score()));
      } else {
        out.add(m);
      }
    }
    if (!placed) {
      out.addFirst(new CompoundFeatureMember(preferred, CompoundMemberRole.REPRESENTATIVE, 1.0f));
    }
    return out;
  }

  private static @NotNull ModularFeatureListRow pickRepresentativeRow(
      @NotNull final List<CompoundFeatureMember> normalized) {
    for (final CompoundFeatureMember m : normalized) {
      if (m.role() == CompoundMemberRole.REPRESENTATIVE) {
        return (ModularFeatureListRow) m.row();
      }
    }
    throw new IllegalStateException(
        "ensureRepresentative() did not produce a REPRESENTATIVE member");
  }

  private static @NotNull Set<Integer> collectIds(
      @NotNull final Collection<? extends FeatureListRow> rows) {
    final Set<Integer> ids = new HashSet<>(rows.size() * 2);
    for (final FeatureListRow r : rows) {
      ids.add(r.getID());
    }
    return ids;
  }

  private static @NotNull <T> Set<T> newIdentitySet(@NotNull final Collection<? extends T> seed) {
    final Set<T> s = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    s.addAll(seed);
    return s;
  }

  /**
   * Smallest positive id not in use across the existing top-level rows or nested compound rows.
   */
  private static int nextCompoundId(@NotNull final CompoundList list) {
    int candidate = 1;
    for (final ModularCompoundRow cr : list.getRows()) {
      if (cr.getCompoundId() >= candidate) {
        candidate = cr.getCompoundId() + 1;
      }
    }
    while (list.findRowByCompoundId(candidate) != null) {
      candidate++;
    }
    return candidate;
  }
}
