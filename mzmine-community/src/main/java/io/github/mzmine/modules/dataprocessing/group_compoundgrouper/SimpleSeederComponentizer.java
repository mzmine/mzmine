package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundComponentizerStrategy;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.correlation.RowGroup;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Three-phase componentizer that groups feature list rows into compounds.
 * <p>
 * Phase A merges rows via Union-Find using IonIdentity Networks (IIN) and correlation RowGroups.
 * Phase B assigns roles to members within each component (delegated to {@link RoleAssigner}). Phase
 * C builds {@link ModularCompoundRow} objects with confidence scores and neutral masses.
 */
public final class SimpleSeederComponentizer implements CompoundComponentizerStrategy {

  private static final Logger logger = Logger.getLogger(SimpleSeederComponentizer.class.getName());

  @NotNull private final MZTolerance mzTolerance;
  @NotNull private final RTTolerance rtTolerance;

  public SimpleSeederComponentizer(@NotNull final MZTolerance mzTolerance,
      @NotNull final RTTolerance rtTolerance) {
    this.mzTolerance = mzTolerance;
    this.rtTolerance = rtTolerance;
  }

  @Override
  public @NotNull List<ModularCompoundRow> componentize(
      @NotNull final ModularFeatureList featureList,
      @NotNull final CompoundList targetList) {
    // snapshot rows once — Union-Find operates over a fixed array index space
    final List<FeatureListRow> rows = new ArrayList<>(featureList.getRows());
    if (rows.isEmpty()) {
      return List.of();
    }

    // Phase A — disjoint-set merging using IIN + RowGroups
    final Map<Integer, List<FeatureListRow>> components = mergeComponents(rows, featureList);

    // Phase B + C — role assignment and compound row construction
    final PolarityType polarity = deriveFeatureListPolarity(featureList);
    final List<ModularCompoundRow> compoundRows = new ArrayList<>(components.size());
    int compoundId = 1;
    for (final List<FeatureListRow> members : components.values()) {
      // assumption: components with a single row still become compounds (size 1) — represents
      // an unrelated singleton observation
      final List<CompoundFeatureMember> assigned = RoleAssigner.assignRoles(members, polarity,
          mzTolerance, rtTolerance);
      final FeatureListRow preferredRow = findRepresentativeRow(assigned);
      if (preferredRow == null) {
        // should not happen: RoleAssigner always assigns one REPRESENTATIVE per component
        logger.warning(
            () -> "No representative row found in component of size " + members.size() + "; skipping");
        continue;
      }
      final float confidence = computeConfidence(assigned, preferredRow);
      final Double neutralMass = resolveNeutralMass(preferredRow);
      compoundRows.add(new ModularCompoundRow(targetList, compoundId++, preferredRow, assigned,
          confidence, neutralMass));
    }
    return compoundRows;
  }

  // ----- Phase A: merging via Union-Find -----

  private @NotNull Map<Integer, List<FeatureListRow>> mergeComponents(
      @NotNull final List<FeatureListRow> rows, @NotNull final ModularFeatureList featureList) {
    final Map<Integer, Integer> rowIdToIdx = new HashMap<>(rows.size() * 2);
    for (int i = 0; i < rows.size(); i++) {
      rowIdToIdx.put(rows.get(i).getID(), i);
    }
    final int[] parent = new int[rows.size()];
    for (int i = 0; i < rows.size(); i++) {
      parent[i] = i;
    }

    // (1) Merge via IonNetworks — a row appearing in N networks transitively merges all of them
    final List<IonNetwork> networks = IonNetworkLogic.streamNetworks(featureList, false).toList();
    for (final IonNetwork net : networks) {
      unionAllRows(parent, rowIdToIdx, net.keySet());
    }

    // (2) Merge via RowGroups with IIN-safety guard: skip groups that span ≥2 distinct
    // existing IIN-rooted components
    final List<RowGroup> groups = featureList.getGroups();
    if (groups != null) {
      for (final RowGroup group : groups) {
        if (group == null || group.size() < 2) {
          continue;
        }
        if (groupBridgesDistinctIinRoots(parent, rowIdToIdx, group)) {
          continue;
        }
        unionAllRows(parent, rowIdToIdx, group.getRows());
      }
    }

    // group rows by their root
    final Map<Integer, List<FeatureListRow>> components = new LinkedHashMap<>();
    for (int i = 0; i < rows.size(); i++) {
      final int root = find(parent, i);
      components.computeIfAbsent(root, k -> new ArrayList<>()).add(rows.get(i));
    }
    return components;
  }

  private static boolean groupBridgesDistinctIinRoots(final int[] parent,
      @NotNull final Map<Integer, Integer> rowIdToIdx, @NotNull final RowGroup group) {
    final Set<Integer> rootsWithIin = new HashSet<>();
    for (final FeatureListRow row : group.getRows()) {
      if (!row.hasIonIdentity()) {
        continue;
      }
      final Integer idx = rowIdToIdx.get(row.getID());
      if (idx == null) {
        continue;
      }
      rootsWithIin.add(find(parent, idx));
      if (rootsWithIin.size() >= 2) {
        return true;
      }
    }
    return false;
  }

  private static void unionAllRows(final int[] parent,
      @NotNull final Map<Integer, Integer> rowIdToIdx,
      @NotNull final Iterable<FeatureListRow> rows) {
    int firstIdx = -1;
    for (final FeatureListRow row : rows) {
      final Integer idx = rowIdToIdx.get(row.getID());
      if (idx == null) {
        continue;
      }
      if (firstIdx < 0) {
        firstIdx = idx;
      } else {
        union(parent, firstIdx, idx);
      }
    }
  }

  // ----- Union-Find primitives (path compression) -----

  private static int find(final int[] parent, int i) {
    while (parent[i] != i) {
      parent[i] = parent[parent[i]];
      i = parent[i];
    }
    return i;
  }

  private static void union(final int[] parent, final int a, final int b) {
    final int ra = find(parent, a);
    final int rb = find(parent, b);
    if (ra != rb) {
      parent[ra] = rb;
    }
  }

  // ----- Phase C helpers -----

  private static @Nullable FeatureListRow findRepresentativeRow(
      @NotNull final List<CompoundFeatureMember> members) {
    for (final CompoundFeatureMember m : members) {
      if (m.role() == CompoundMemberRole.REPRESENTATIVE) {
        return m.row();
      }
    }
    return null;
  }

  private float computeConfidence(@NotNull final List<CompoundFeatureMember> members,
      @NotNull final FeatureListRow preferredRow) {
    if (members.isEmpty()) {
      return 0f;
    }
    // size component: caps at 5 members
    final float sizeScore = Math.min(1f, members.size() / 5f);

    // IIN evidence component: fraction of members with an IonIdentity assigned
    long iinCount = 0;
    for (final CompoundFeatureMember m : members) {
      if (m.row().hasIonIdentity()) {
        iinCount++;
      }
    }
    final float iinFraction = (float) iinCount / members.size();

    // RT coherence: fraction of members within RT tolerance of the representative
    final Float refRt = preferredRow.getAverageRT();
    long rtCoherent = 0;
    long rtTotal = 0;
    if (refRt != null) {
      for (final CompoundFeatureMember m : members) {
        final Float rt = m.row().getAverageRT();
        if (rt == null) {
          continue;
        }
        rtTotal++;
        if (rtTolerance.checkWithinTolerance(refRt, rt)) {
          rtCoherent++;
        }
      }
    }
    final float rtCoherence = rtTotal == 0 ? 0f : (float) rtCoherent / rtTotal;

    // weighted composite
    final float score = 0.3f * sizeScore + 0.5f * iinFraction + 0.2f * rtCoherence;
    return Math.max(0f, Math.min(1f, score));
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
      final double nm = network.getNeutralMass();
      if (Double.isNaN(nm) || nm == 0d) {
        return null;
      }
      return nm;
    } catch (final NullPointerException e) {
      // assumption: calcNeutralMass dereferences row intensities that may be unset in synthetic
      // contexts (tests, partial data). Fall back to no neutral mass rather than failing.
      return null;
    }
  }

  // ----- Polarity derivation -----

  private static @NotNull PolarityType deriveFeatureListPolarity(
      @NotNull final ModularFeatureList featureList) {
    long positive = 0;
    long negative = 0;
    final List<RawDataFile> files = featureList.getRawDataFiles();
    if (files != null) {
      for (final RawDataFile f : files) {
        for (final PolarityType p : f.getDataPolarity()) {
          // decision: count each (file, scan-polarity) once — files often only have one polarity
          if (p == PolarityType.POSITIVE) {
            positive++;
          } else if (p == PolarityType.NEGATIVE) {
            negative++;
          }
        }
      }
    }
    if (positive > negative) {
      return PolarityType.POSITIVE;
    }
    if (negative > positive) {
      return PolarityType.NEGATIVE;
    }
    return PolarityType.UNKNOWN;
  }
}
