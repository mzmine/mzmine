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

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.msms.ActivationMethod;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring.ColorAssignment;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.DefaultQualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckContext;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.Ms2AvailableQualityResult.RowScans;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.Ms2AvailableQualityResult.ScanGroup;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/// Reports how many of the compound's member rows have at least one MS2 fragment scan, and lists
/// the fragmentation conditions (energy + activation method) per row when a host supplies a color
/// assignment. Falls back to a plain text summary otherwise.
public final class Ms2AvailableCheck implements QualityCheck {

  /// Two energies are considered the "same" group when they're within this fraction of each other.
  /// Used to fold nearly-identical collision energies (e.g. 23.366 vs 23.426 from per-scan
  /// rounding) into one display chip. Deliberately coarse — see the user's "does not have to be
  /// precise" guidance.
  private static final double ENERGY_CLUSTER_TOLERANCE = 0.05;

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.MS2_AVAILABLE;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    final List<CompoundFeatureMember> members = row.getCompoundMembers();
    final List<RowScans> rowScans = new ArrayList<>();
    final List<FeatureListRow> involved = new ArrayList<>();
    int withMs2 = 0;
    int totalScans = 0;

    for (final CompoundFeatureMember m : members) {
      final List<Scan> scans = m.row().getAllFragmentScans();
      if (scans.isEmpty()) {
        continue;
      }
      withMs2++;
      totalScans += scans.size();
      involved.add(m.row());
      rowScans.add(new RowScans(m.row(), groupByMethodAndEnergy(scans)));
    }

    // Sort rows by m/z ascending so adducts read in a predictable order (nulls last).
    rowScans.sort(Comparator.comparing((RowScans rs) -> rs.row().getAverageMZ() == null) //
        .thenComparingDouble(rs -> {
          final Double mz = rs.row().getAverageMZ();
          return mz == null ? Double.POSITIVE_INFINITY : mz;
        }));

    final int totalMembers = members.size();
    final double pct = totalMembers == 0 ? 0d : (100d * withMs2 / totalMembers);

    if (withMs2 == 0) {
      return new DefaultQualityCheckResult(QualityCheckType.MS2_AVAILABLE, QualityCheckStatus.WARN,
          "No MS2 scans across %d member%s".formatted(totalMembers,
              totalMembers == 1 ? "" : "s"), List.of(), List.of());
    }

    final String summary = "%d MS2 scan%s across %d/%d member%s (%.0f%%)".formatted(totalScans,
        totalScans == 1 ? "" : "s", withMs2, totalMembers, totalMembers == 1 ? "" : "s", pct);

    final ColorAssignment coloring = context.colorAssignment();
      return new Ms2AvailableQualityResult(QualityCheckStatus.PASS, summary, rowScans, involved,
          coloring, context.selectedMemberRow(), context.onEvent());
  }

  /// Group a row's scans first by activation method, then within each method cluster nearby
  /// energies into a single bucket using {@link #ENERGY_CLUSTER_TOLERANCE}. Output is sorted by
  /// method name, then by ascending cluster energy (null energies last per method).
  private static @NotNull List<@NotNull ScanGroup> groupByMethodAndEnergy(
      @NotNull final List<@NotNull Scan> scans) {
    // 1. Bucket by method.
    final Map<ActivationMethod, List<Float>> energiesByMethod = new EnumMap<>(
        ActivationMethod.class);
    final Map<ActivationMethod, Integer> nullEnergyByMethod = new EnumMap<>(ActivationMethod.class);
    for (final Scan scan : scans) {
      final ActivationMethod method = extractActivationMethod(scan);
      final Float energy = ScanUtils.extractCollisionEnergy(scan);
      if (energy == null) {
        nullEnergyByMethod.merge(method, 1, Integer::sum);
      } else {
        energiesByMethod.computeIfAbsent(method, k -> new ArrayList<>()).add(energy);
      }
    }

    // 2. Within each method, sort energies ascending and cluster within tolerance.
    final List<ScanGroup> groups = new ArrayList<>();
    for (final Map.Entry<ActivationMethod, List<Float>> entry : energiesByMethod.entrySet()) {
      final ActivationMethod method = entry.getKey();
      final List<Float> energies = entry.getValue();
      energies.sort(Float::compareTo);
      groups.addAll(clusterEnergies(energies, method));
    }
    // Null-energy scans: one group per method (we can't cluster them by value).
    for (final Map.Entry<ActivationMethod, Integer> entry : nullEnergyByMethod.entrySet()) {
      groups.add(new ScanGroup(null, entry.getKey(), entry.getValue()));
    }

    // 3. Stable display order across methods + within each method.
    groups.sort(Comparator //
        .comparing((ScanGroup g) -> g.method().name()) //
        .thenComparing(g -> g.energy() == null) // nulls last per method
        .thenComparingDouble(g -> g.energy() == null ? Double.POSITIVE_INFINITY : g.energy()));
    return groups;
  }

  /// Walk a sorted energy list and merge consecutive entries whose relative gap is within
  /// {@link #ENERGY_CLUSTER_TOLERANCE}. The representative energy for a cluster is the average of
  /// its members; the count is the number of original scans folded in. Simple linear cluster — not
  /// sophisticated, just good enough to fold per-scan rounding noise.
  private static @NotNull List<@NotNull ScanGroup> clusterEnergies(
      @NotNull final List<@NotNull Float> sortedEnergies, @NotNull final ActivationMethod method) {
    final List<ScanGroup> result = new ArrayList<>();
    if (sortedEnergies.isEmpty()) {
      return result;
    }
    final List<Float> current = new ArrayList<>();
    Float prev = null;
    for (final Float e : sortedEnergies) {
      // Start a new cluster when the next value drifts more than the tolerance from the previous
      // one. assumption: sorted list, so we only check the forward edge.
      final boolean splitCluster =
          prev != null && (e - prev) > ENERGY_CLUSTER_TOLERANCE * Math.max(prev, 1e-6f);
      if (splitCluster) {
        result.add(makeGroup(current, method));
        current.clear();
      }
      current.add(e);
      prev = e;
    }
    if (!current.isEmpty()) {
      result.add(makeGroup(current, method));
    }
    return result;
  }

  private static @NotNull ScanGroup makeGroup(@NotNull final List<@NotNull Float> energies,
      @NotNull final ActivationMethod method) {
    final double avg = energies.stream().mapToDouble(Float::doubleValue).average().orElse(0);
    return new ScanGroup((float) avg, method, energies.size());
  }

  /// Pull the {@link ActivationMethod} off a scan's {@link MsMsInfo}, defaulting to
  /// {@link ActivationMethod#UNKNOWN} when either the info or the method is missing.
  private static @NotNull ActivationMethod extractActivationMethod(@NotNull final Scan scan) {
    final MsMsInfo info = scan.getMsMsInfo();
    if (info == null) {
      return ActivationMethod.UNKNOWN;
    }
    return Objects.requireNonNullElse(info.getActivationMethod(), ActivationMethod.UNKNOWN);
  }
}
