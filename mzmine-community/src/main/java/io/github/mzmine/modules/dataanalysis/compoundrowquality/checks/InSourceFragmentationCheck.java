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

package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.DefaultQualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckContext;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/// Flags member rows whose precursor m/z appears as a fragment in the MS2 of a higher-m/z member,
/// suggesting the member is an in-source fragment of the larger molecule. Rows previously tagged
/// upstream with {@link CompoundMemberRole#IN_SOURCE_FRAGMENT} are also reported even when no MS2
/// parent can be located in this compound.
public final class InSourceFragmentationCheck implements QualityCheck {

  /// Minimum m/z difference between the parent (MS2 precursor) and the candidate fragment row.
  /// Anything within this window is considered too close in mass to confidently call an in-source
  /// fragmentation event.
  private static final double MIN_PARENT_MZ_OFFSET = 10d;

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.IN_SOURCE_FRAGMENTATION;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    final List<CompoundFeatureMember> members = row.getCompoundMembers();
    final MZTolerance ms2Tol = context.ms2Tolerance();

    // Walk every member that could conceivably be a fragment (skip the compound's representative
    // and any pure isotopologue). For each fragment candidate, collect every higher-m/z member
    // whose MS2 spectrum contains the candidate's precursor m/z within tolerance. Rows tagged
    // upstream as IN_SOURCE_FRAGMENT are always emitted, even if no MS2 parent shows up here.
    final List<FragmentParents> fragmentEntries = new ArrayList<>();
    final Set<FeatureListRow> involvedFragments = new LinkedHashSet<>();
    int taggedCount = 0;
    int matchedCount = 0;

    for (final CompoundFeatureMember candidate : members) {
      if (candidate.role() == CompoundMemberRole.REPRESENTATIVE
          || candidate.role() == CompoundMemberRole.ISOTOPOLOGUE) {
        continue;
      }
      final FeatureListRow candidateRow = candidate.row();
      final Double candidateMz = candidateRow.getAverageMZ();
      if (candidateMz == null) {
        continue;
      }
      final boolean tagged = candidate.role() == CompoundMemberRole.IN_SOURCE_FRAGMENT;
      final List<FeatureListRow> parents = collectHigherMzMs2Parents(members, candidateRow,
          candidateMz, ms2Tol);
      if (!tagged && parents.isEmpty()) {
        continue;
      }
      parents.sort(FragmentParentsRendering.PARENT_ORDER);
      fragmentEntries.add(new FragmentParents(candidateRow, List.copyOf(parents)));
      involvedFragments.add(candidateRow);
      if (tagged) {
        taggedCount++;
      }
      if (!parents.isEmpty()) {
        matchedCount++;
      }
    }

    if (fragmentEntries.isEmpty()) {
      return new DefaultQualityCheckResult(QualityCheckType.IN_SOURCE_FRAGMENTATION,
          QualityCheckStatus.PASS, "No in-source fragments detected", List.of(), List.of());
    }

    final int total = fragmentEntries.size();
    final String summary = "%d possible in-source fragment%s".formatted(total,
        total == 1 ? "" : "s");

    final ColorAssignment coloring = context.colorAssignment();
      return new InSourceFragmentationQualityResult(QualityCheckStatus.WARN, summary,
          fragmentEntries, coloring, context.selectedMemberRow());
  }

  /// Compact text fallback used when no {@link ColorAssignment} is supplied (e.g. the quality pane
  /// runs without a host dashboard). Preserves the existing "tagged" / "MS2 hits" split.
  private static @NotNull List<@NotNull String> buildPlainDetails(
      @NotNull final List<@NotNull FragmentParents> fragmentEntries, final int taggedCount,
      final int matchedCount) {
    final List<String> details = new ArrayList<>();
    if (taggedCount > 0) {
      details.add("%d member%s already tagged as in-source".formatted(taggedCount,
          taggedCount == 1 ? "" : "s"));
    }
    if (matchedCount > 0) {
      final List<String> matchedLabels = new ArrayList<>();
      for (final FragmentParents entry : fragmentEntries) {
        if (entry.parents().isEmpty()) {
          continue;
        }
        final FeatureListRow r = entry.fragment();
        final IonIdentity ion = r.getBestIonIdentity();
        final Double mz = r.getAverageMZ();
        matchedLabels.add(ion != null ? ion.getIonType().toString()
            : (mz == null ? ("row " + r.getID()) : "%.4f".formatted(mz)));
      }
      details.add("MS2 hits: " + String.join(", ", matchedLabels));
    }
    return details;
  }

  private static @NotNull List<@NotNull FeatureListRow> collectHigherMzMs2Parents(
      @NotNull final List<CompoundFeatureMember> members, @NotNull final FeatureListRow candidate,
      final double candidateMz, @NotNull final MZTolerance ms2Tol) {
    final List<FeatureListRow> parents = new ArrayList<>();
    for (final CompoundFeatureMember other : members) {
      final FeatureListRow otherRow = other.row();
      if (otherRow == candidate) {
        continue;
      }
      final Double otherMz = otherRow.getAverageMZ();
      if (otherMz == null) {
        continue;
      }
      // require the other row to be at least MIN_PARENT_MZ_OFFSET above the candidate
      if (otherMz < candidateMz + MIN_PARENT_MZ_OFFSET) {
        continue;
      }
      if (anyMs2Contains(otherRow, candidateMz, ms2Tol)) {
        parents.add(otherRow);
      }
    }
    return parents;
  }

  private static boolean anyMs2Contains(@NotNull final FeatureListRow parent, final double targetMz,
      @NotNull final MZTolerance tol) {
    for (final Scan scan : parent.getAllFragmentScans()) {
      final MassList ml = scan.getMassList();
      if (ml == null) {
        continue;
      }
      if (massListContains(ml, targetMz, tol)) {
        return true;
      }
    }
    return false;
  }

  private static boolean massListContains(@NotNull MassList ml, double targetMz,
      @NotNull MZTolerance tol) {
    final int n = ml.getNumberOfDataPoints();
    if (n == 0) {
      return false;
    }
    final int closestIdx = ml.binarySearch(targetMz, DefaultTo.CLOSEST_VALUE);
    if (closestIdx < 0) {
      return false;
    }
    return tol.checkWithinTolerance(ml.getMzValue(closestIdx), targetMz);
  }
}
