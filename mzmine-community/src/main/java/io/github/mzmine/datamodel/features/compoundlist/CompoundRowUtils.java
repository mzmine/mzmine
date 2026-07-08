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

package io.github.mzmine.datamodel.features.compoundlist;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.FeatureListRowID;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundMembersType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
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
    final FeatureListRowID newRepresentativeId = newRepresentative.getTypedID();
    if (current.preferredRow().getTypedID().equals(newRepresentativeId)) {
      return false;
    }
    final List<CompoundFeatureMember> oldMembers = current.members();
    boolean found = false;
    final List<CompoundFeatureMember> updated = new ArrayList<>(oldMembers.size());
    for (final CompoundFeatureMember m : oldMembers) {
      if (m.row().getTypedID().equals(newRepresentativeId)) {
        updated.add(new CompoundFeatureMember(m.row(), CompoundMemberRole.REPRESENTATIVE, 1.0f));
        found = true;
      } else if (m.role() == CompoundMemberRole.REPRESENTATIVE) {
        updated.add(new CompoundFeatureMember(m.row(), CompoundMemberRole.CORRELATED, m.score()));
      } else {
        updated.add(m);
      }
    }
    if (!found) {
      logger.warning(() -> "Row typedId=" + newRepresentativeId + " is not a member of compound id="
          + compound.getCompoundId() + "; cannot set as representative");
      return false;
    }
    final CompoundMembers next = new CompoundMembers(newRepresentative, List.copyOf(updated),
        current.confidence());
    // membership stays the same; only the preferredRow + role flags change. No need to rebuild
    // the reverse-member index — listeners on CompoundMembersType propagate the change.
    compound.set(CompoundMembersType.class, next);
    compound.set(NeutralMassType.class, resolveNeutralMass(newRepresentative));
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
    final Set<FeatureListRowID> moveIds = collectTypedIds(rowsToMove);
    final CompoundMembers sourceData = source.getCompoundMembersData();
    final List<CompoundFeatureMember> moved = new ArrayList<>(rowsToMove.size());
    final List<CompoundFeatureMember> remaining = new ArrayList<>(sourceData.members().size());
    for (final CompoundFeatureMember m : sourceData.members()) {
      if (moveIds.contains(m.row().getTypedID())) {
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
   * Strip the given member rows from every compound row in {@code list}, recursing into nested
   * compound rows. Used when feature list rows are deleted from the underlying
   * {@link ModularFeatureList} so the compound list stays consistent instead of being invalidated.
   * <ul>
   *   <li>Leaf member rows with a matching id are stripped at any depth.</li>
   *   <li>A nested compound row that ends up empty after stripping is dropped from its parent.</li>
   *   <li>A top-level compound that ends up empty is dropped from the list.</li>
   *   <li>Compounds that lose their representative get a new one promoted.</li>
   * </ul>
   *
   * @return true if any compound row was modified or removed
   */
  public static boolean detachMemberRows(@NotNull final CompoundList list,
      @NotNull final Collection<? extends FeatureListRow> rowsToDetach) {
    if (rowsToDetach.isEmpty()) {
      return false;
    }
    final Set<FeatureListRowID> detachIds = collectTypedIds(rowsToDetach);
    final AtomicBoolean changed = new AtomicBoolean(false);

    final List<ModularCompoundRow> nextRows = new ArrayList<>(list.getRows().size());
    for (final ModularCompoundRow cr : list.getRows()) {
      final boolean shouldDrop = detachLeavesRecursive(cr, detachIds, changed);
      if (!shouldDrop) {
        nextRows.add(cr);
      }
    }
    if (changed.get()) {
      list.setRows(nextRows);
    }
    return changed.get();
  }

  /**
   * Per-parent variant of {@link #detachMemberRows(CompoundList, Collection)}: strip the given
   * member rows from {@code parent} and all of its nested compound members (recursive). Nested
   * compounds that become empty are dropped from {@code parent}. {@code parent} itself is never
   * removed by this method — if it becomes empty the caller is responsible for removing it (see the
   * return value). Bindings/indexes on the owning {@link CompoundList} are not rebuilt; call
   * {@link #detachMemberRows(CompoundList, Collection)} if you want full-list consistency.
   *
   * @return true if {@code parent} is now empty and should be removed by the caller
   */
  public static boolean detachMemberRowsFrom(@NotNull final ModularCompoundRow parent,
      @NotNull final Collection<? extends FeatureListRow> rowsToDetach) {
    if (rowsToDetach.isEmpty()) {
      return false;
    }
    final Set<FeatureListRowID> detachIds = collectTypedIds(rowsToDetach);
    return detachLeavesRecursive(parent, detachIds, new AtomicBoolean(false));
  }

  /**
   * Recursive walk. Recurses into nested compound members first so that an empty nested compound
   * gets dropped from its parent. Then rewrites {@code compound}'s members, removing both leaf rows
   * whose id is in {@code detachIds} and nested compounds that just became empty.
   *
   * @return true if {@code compound} should be dropped from its parent (became empty after the
   * rewrite)
   */
  private static boolean detachLeavesRecursive(@NotNull final ModularCompoundRow compound,
      @NotNull final Set<FeatureListRowID> detachIds,
      @NotNull final AtomicBoolean changedAccumulator) {
    // First pass: recurse into nested compounds. Any nested compound that becomes empty must also
    // be dropped from this compound.
    final Set<ModularCompoundRow> nestedToDrop = new HashSet<>();
    for (final CompoundFeatureMember m : compound.getCompoundMembersData().members()) {
      if (m.row() instanceof ModularCompoundRow nested && detachLeavesRecursive(nested, detachIds,
          changedAccumulator)) {
        nestedToDrop.add(nested);
      }
    }

    // Second pass: drop leaves whose id matches detachIds + drop nested compounds emptied above.
    return rewriteCompoundMembers(compound, m -> {
      if (m.row() instanceof ModularCompoundRow nested) {
        return nestedToDrop.contains(nested);
      }
      return detachIds.contains(m.row().getTypedID());
    }, changedAccumulator);
  }

  /**
   * Remove the given {@code compoundsToRemove} from {@code list}. Compounds may live at any depth:
   * <ul>
   *   <li>Top-level compounds are dropped from {@link CompoundList#getRows()}.</li>
   *   <li>Nested compounds are stripped from every parent compound that lists them as a member.</li>
   *   <li>If a parent loses all of its members because of this removal, the parent is also
   *       removed (cascading up to the top-level list).</li>
   * </ul>
   * The underlying feature list rows of the removed compounds are not touched — only the compound
   * grouping is dropped.
   *
   * @return true if at least one compound was removed
   */
  public static boolean removeCompoundRows(@NotNull final CompoundList list,
      @NotNull final Collection<ModularCompoundRow> compoundsToRemove) {
    if (compoundsToRemove.isEmpty()) {
      return false;
    }

    final Set<ModularCompoundRow> toRemoveByRef = newIdentitySet(compoundsToRemove);
    final Deque<ModularCompoundRow> queue = new ArrayDeque<>(toRemoveByRef);
    boolean changed = false;

    while (!queue.isEmpty()) {
      final ModularCompoundRow target = queue.poll();
      changed = true;

      // Direct parents from the reverse index. The index is rebuilt only by setRows so it stays
      // stable across our per-row mutations below.
      final List<ModularCompoundRow> parents = list.findCompoundsOf(target);
      for (final ModularCompoundRow parent : parents) {
        if (toRemoveByRef.contains(parent)) {
          // parent is also being removed entirely — its member list does not need to be rewritten
          continue;
        }
        final boolean parentNowEmpty = rewriteCompoundMembers(parent, m -> m.row() == target,
            new AtomicBoolean(false));
        if (parentNowEmpty && toRemoveByRef.add(parent)) {
          // cascade: parent lost all members, also drop it (and find its own parents on next pass)
          queue.add(parent);
        }
      }
    }

    if (changed) {
      final List<ModularCompoundRow> nextRows = new ArrayList<>(list.getRows().size());
      for (final ModularCompoundRow cr : list.getRows()) {
        if (!toRemoveByRef.contains(cr)) {
          nextRows.add(cr);
        }
      }
      list.setRows(nextRows);
    }
    return changed;
  }

  /**
   * Rewrite {@code compound}'s {@link CompoundMembersType} in place: keep only members for which
   * {@code shouldRemove} returns false, then re-normalize the representative (the previous
   * representative may have been removed). If the compound ends up empty no write happens — the
   * method returns true so the caller can drop the now-empty compound from its parent.
   *
   * @return true if the compound is empty after the rewrite and should be dropped by the caller
   */
  private static boolean rewriteCompoundMembers(@NotNull final ModularCompoundRow compound,
      @NotNull final Predicate<CompoundFeatureMember> shouldRemove,
      @NotNull final AtomicBoolean changedAccumulator) {
    final CompoundMembers data = compound.getCompoundMembersData();
    final List<CompoundFeatureMember> remaining = new ArrayList<>(data.members().size());
    boolean compoundChanged = false;
    for (final CompoundFeatureMember m : data.members()) {
      if (shouldRemove.test(m)) {
        compoundChanged = true;
      } else {
        remaining.add(m);
      }
    }
    if (!compoundChanged) {
      return false;
    }
    changedAccumulator.set(true);
    if (remaining.isEmpty()) {
      // signal to caller that this compound should be dropped from its parent; do not write empty
      return true;
    }
    final List<CompoundFeatureMember> normalized = ensureRepresentative(remaining);
    final ModularFeatureListRow newPref = pickRepresentativeRow(normalized);
    compound.set(CompoundMembersType.class,
        new CompoundMembers(newPref, List.copyOf(normalized), data.confidence()));
    return false;
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
    final Set<FeatureListRowID> presentIds = HashSet.newHashSet(merged.size() * 2);
    for (final CompoundFeatureMember m : merged) {
      presentIds.add(m.row().getTypedID());
    }

    boolean changed = false;

    // 1) Absorb members from compounds being merged
    for (final ModularCompoundRow other : toRemove) {
      for (final CompoundFeatureMember m : other.getCompoundMembersData().members()) {
        if (presentIds.add(m.row().getTypedID())) {
          merged.add(demoteIfRepresentative(m));
          changed = true;
        }
      }
    }

    // 2) Detach extras from any *other* compound (excluding target and ones being removed entirely)
    final Set<FeatureListRowID> extraIds = collectTypedIds(extraMemberRows);
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
          if (extraIds.contains(m.row().getTypedID())) {
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
      if (presentIds.add(extra.getTypedID())) {
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

  /**
   * Build a copy of {@code src} attached to {@code target} compound list, remapping each member's
   * feature row via {@code rowMapping} (old row → new row, or {@code null} when the row was removed
   * from the new feature list). Nested compound members are copied recursively and dropped when
   * {@code compoundRowFilter} rejects them ({@code null} keeps all). Members whose row is gone are
   * stripped, and the representative is re-picked when it was removed. Compound id, member roles,
   * scores, confidence and neutral mass are preserved; derived values and compound features are
   * recomputed when the target list's {@link CompoundList#setRows(List)} applies its bindings.
   *
   * @param compoundRowFilter keep predicate applied to nested compound members, or {@code null} to
   *                          keep all. The caller applies it to {@code src} itself (top-level
   *                          rows).
   * @param rowMapping        mapping function applied to each member's feature row, or {@code null}
   *                          when the row was removed. This means that the original
   *                          {@link FeatureListRow} are already copied before calling this method
   *                          to allow copying members of compound rows.
   * @return the copied compound row, or {@code null} if no member survived (the compound is
   * dropped)
   */
  public static @Nullable ModularCompoundRow copyRowFiltered(@NotNull final ModularCompoundRow src,
      @NotNull final CompoundList target,
      @NotNull final Function<FeatureListRow, ModularFeatureListRow> rowMapping,
      @Nullable final Predicate<ModularCompoundRow> compoundRowFilter) {
    final List<CompoundFeatureMember> members = src.getCompoundMembers();
    final List<CompoundFeatureMember> kept = new ArrayList<>(members.size());
    for (final CompoundFeatureMember m : members) {
      final FeatureListRow memberRow = m.row();
      if (memberRow instanceof ModularCompoundRow nested) {
        // drop nested compound rows rejected by the compound-id filter
        if (compoundRowFilter != null && !compoundRowFilter.test(nested)) {
          continue;
        }
        // recurse: a nested compound survives only if it still has members
        final ModularCompoundRow nestedCopy = copyRowFiltered(nested, target, rowMapping,
            compoundRowFilter);
        if (nestedCopy != null) {
          kept.add(new CompoundFeatureMember(nestedCopy, m.role(), m.score()));
        }
      } else {
        final ModularFeatureListRow mapped = rowMapping.apply(memberRow);
        if (mapped != null) {
          kept.add(new CompoundFeatureMember(mapped, m.role(), m.score()));
        }
      }
    }
    if (kept.isEmpty()) {
      return null;
    }
    final List<CompoundFeatureMember> normalized = ensureRepresentative(kept);
    final ModularFeatureListRow preferred = pickRepresentativeRow(normalized);
    return new ModularCompoundRow(target, src.getCompoundId(), preferred, normalized,
        src.getCompoundConfidenceScore(), src.getCompoundNeutralMass());
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
    final FeatureListRowID preferredId = preferred.getTypedID();
    boolean placed = false;
    for (final CompoundFeatureMember m : members) {
      if (m.row().getTypedID().equals(preferredId)) {
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

  private static @NotNull Set<FeatureListRowID> collectTypedIds(
      @NotNull final Collection<? extends FeatureListRow> rows) {
    final Set<FeatureListRowID> ids = HashSet.newHashSet(rows.size());
    for (final FeatureListRow r : rows) {
      ids.add(r.getTypedID());
    }
    return ids;
  }

  private static @Nullable Double resolveNeutralMass(@NotNull final FeatureListRow row) {
    final IonIdentity ion = row.getBestIonIdentity();
    if (ion == null) {
      return null;
    }
    final IonNetwork network = ion.getNetwork();
    if (network == null) {
      return null;
    }
    try {
      final double neutralMass = network.getNeutralMass();
      if (Double.isNaN(neutralMass) || neutralMass == 0d) {
        return null;
      }
      return neutralMass;
    } catch (final NullPointerException e) {
      // assumption: synthetic or sparse rows may not have enough state to compute neutral mass.
      return null;
    }
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
