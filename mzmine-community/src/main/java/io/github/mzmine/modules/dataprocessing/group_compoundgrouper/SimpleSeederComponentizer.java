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
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * IIN-seeded, correlation-aware compound componentizer.
 * <p>
 * Step 1 — build adjacency lists from {@link ModularFeatureList#getMs1CorrelationMap()}.
 * Step 2 — seed compounds from Ion Identity Networks via Union-Find on IIN rows.
 * Step 3 — for each non-IIN row, attach to every IIN-seeded component reachable through correlation
 * edges. A row connected to two IIN seeds becomes a member of <em>both</em> compounds (dual
 * membership; the compounds stay distinct).
 * Step 4 — find dense communities in the residual (no-IIN) correlation subgraph: pick the highest
 * degree seed, take its closed neighborhood, peel low-degree nodes until the induced density
 * reaches {@code MIN_DENSITY}, commit as a compound, repeat. Leftover singletons become 1-member
 * compounds.
 * Roles are assigned by {@link RoleAssigner}; confidence by {@link #computeConfidence}.
 */
public final class SimpleSeederComponentizer implements CompoundComponentizerStrategy {

  private static final Logger logger = Logger.getLogger(SimpleSeederComponentizer.class.getName());

  @NotNull private final MZTolerance mzTolerance;
  @NotNull private final RTTolerance rtTolerance;
  private final double minDensity;

  public SimpleSeederComponentizer(@NotNull final MZTolerance mzTolerance,
      @NotNull final RTTolerance rtTolerance, final double minDensity) {
    this.mzTolerance = mzTolerance;
    this.rtTolerance = rtTolerance;
    this.minDensity = minDensity;
  }

  @Override
  public @Nullable String validateInputs(@NotNull final ModularFeatureList featureList) {
    if (featureList.getNumberOfRows() == 0) {
      return "CompoundGrouper requires a non-empty feature list.";
    }
    final boolean hasIin = IonNetworkLogic.streamNetworks(featureList, false).findAny().isPresent();
    final boolean hasCorrelation = featureList.getMs1CorrelationMap()
        .map(map -> !map.isEmpty()).orElse(false);
    if (!hasIin && !hasCorrelation) {
      return "SimpleSeeder requires Ion Identity Networking or MS1 correlation grouping output. "
          + "Run those modules first.";
    }
    return null;
  }

  @Override
  public @NotNull List<ModularCompoundRow> componentize(
      @NotNull final ModularFeatureList featureList,
      @NotNull final CompoundList targetList) {
    final List<FeatureListRow> rows = new ArrayList<>(featureList.getRows());
    if (rows.isEmpty()) {
      return List.of();
    }

    // Step 1 — adjacency from MS1 correlation map
    final Map<Integer, Integer> rowIdToIdx = buildRowIdIndex(rows);
    final int[][] neighbors = buildAdjacency(featureList, rowIdToIdx);

    // Step 2 — Union-Find over IIN rows only
    final int[] iinParent = new int[rows.size()];
    Arrays.fill(iinParent, -1); // -1 = not in any IIN
    seedFromIonNetworks(featureList, rowIdToIdx, iinParent);

    // collect IIN root → member rows (insertion-ordered for stable compound IDs)
    final Map<Integer, List<FeatureListRow>> seedMembers = new LinkedHashMap<>();
    for (int i = 0; i < rows.size(); i++) {
      if (iinParent[i] >= 0) {
        final int root = find(iinParent, i);
        seedMembers.computeIfAbsent(root, k -> new ArrayList<>()).add(rows.get(i));
      }
    }
    final Set<Integer> iinRoots = new HashSet<>(seedMembers.keySet());

    // Step 3 — attach non-IIN rows to every IIN seed reachable via correlation edges
    // (dual-membership for bridges)
    final boolean[] attached = new boolean[rows.size()];
    for (int i = 0; i < rows.size(); i++) {
      if (iinParent[i] >= 0) {
        attached[i] = true; // IIN rows already in their seed
        continue;
      }
      final Set<Integer> reachableSeeds = new HashSet<>();
      for (final int j : neighbors[i]) {
        if (iinParent[j] >= 0) {
          reachableSeeds.add(find(iinParent, j));
        }
      }
      if (reachableSeeds.isEmpty()) {
        continue; // leave for Step 4
      }
      for (final int root : reachableSeeds) {
        if (iinRoots.contains(root)) {
          seedMembers.get(root).add(rows.get(i));
        }
      }
      attached[i] = true;
    }

    // Step 4 — dense communities in the residual subgraph (rows not yet attached)
    final List<List<FeatureListRow>> residualCommunities = detectResidualCommunities(rows,
        neighbors, attached);

    // Build compound rows
    final PolarityType polarity = deriveFeatureListPolarity(featureList);
    final List<ModularCompoundRow> compoundRows = new ArrayList<>(
        seedMembers.size() + residualCommunities.size());
    int compoundId = 1;
    for (final List<FeatureListRow> members : seedMembers.values()) {
      final ModularCompoundRow built = buildCompound(targetList, compoundId++, members, polarity);
      if (built != null) {
        compoundRows.add(built);
      }
    }
    for (final List<FeatureListRow> members : residualCommunities) {
      final ModularCompoundRow built = buildCompound(targetList, compoundId++, members, polarity);
      if (built != null) {
        compoundRows.add(built);
      }
    }
    return compoundRows;
  }

  // ----- Step 1 helpers -----

  private static @NotNull Map<Integer, Integer> buildRowIdIndex(
      @NotNull final List<FeatureListRow> rows) {
    final Map<Integer, Integer> rowIdToIdx = new HashMap<>(rows.size() * 2);
    for (int i = 0; i < rows.size(); i++) {
      rowIdToIdx.put(rows.get(i).getID(), i);
    }
    return rowIdToIdx;
  }

  private static @NotNull int[][] buildAdjacency(@NotNull final ModularFeatureList featureList,
      @NotNull final Map<Integer, Integer> rowIdToIdx) {
    final int n = rowIdToIdx.size();
    final int[][] result = new int[n][];
    final Optional<R2RMap<RowsRelationship>> opMap = featureList.getMs1CorrelationMap();
    if (opMap.isEmpty() || opMap.get().isEmpty()) {
      // empty adjacency for all rows
      Arrays.setAll(result, i -> new int[0]);
      return result;
    }
    // collect into ArrayLists first for variable-size append, then compact to int[]
    final List<List<Integer>> tmp = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      tmp.add(new ArrayList<>(2));
    }
    for (final RowsRelationship r2r : opMap.get().values()) {
      final FeatureListRow rowA = r2r.getRowA();
      final FeatureListRow rowB = r2r.getRowB();
      if (rowA == null || rowB == null) {
        continue;
      }
      final Integer ia = rowIdToIdx.get(rowA.getID());
      final Integer ib = rowIdToIdx.get(rowB.getID());
      // assumption: stale relationships referencing rows not in this feature list are skipped
      if (ia == null || ib == null || ia.equals(ib)) {
        continue;
      }
      tmp.get(ia).add(ib);
      tmp.get(ib).add(ia);
    }
    for (int i = 0; i < n; i++) {
      final List<Integer> list = tmp.get(i);
      final int[] arr = new int[list.size()];
      for (int k = 0; k < arr.length; k++) {
        arr[k] = list.get(k);
      }
      result[i] = arr;
    }
    return result;
  }

  // ----- Step 2 helpers -----

  private static void seedFromIonNetworks(@NotNull final ModularFeatureList featureList,
      @NotNull final Map<Integer, Integer> rowIdToIdx, @NotNull final int[] iinParent) {
    final List<IonNetwork> networks = IonNetworkLogic.streamNetworks(featureList, false).toList();
    for (final IonNetwork net : networks) {
      // initialize parents for member rows on first sight, then union all together
      int firstIdx = -1;
      for (final FeatureListRow row : net.getRows()) {
        final Integer idx = rowIdToIdx.get(row.getID());
        if (idx == null) {
          continue;
        }
        if (iinParent[idx] < 0) {
          iinParent[idx] = idx; // self-rooted
        }
        if (firstIdx < 0) {
          firstIdx = idx;
        } else {
          union(iinParent, firstIdx, idx);
        }
      }
    }
  }

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

  // ----- Step 4: dense-region community detection on residual subgraph -----

  /**
   * Greedy density-first detector. While unassigned residual nodes remain, pick the highest-degree
   * one as a seed, take its closed neighborhood, peel off low-degree nodes until the induced
   * density reaches {@link #minDensity}, and commit. Leftover peeled nodes become singletons.
   */
  private @NotNull List<List<FeatureListRow>> detectResidualCommunities(
      @NotNull final List<FeatureListRow> rows, @NotNull final int[][] neighbors,
      @NotNull final boolean[] attached) {
    final boolean[] residualAvailable = new boolean[rows.size()];
    for (int i = 0; i < rows.size(); i++) {
      residualAvailable[i] = !attached[i];
    }
    // residual degree counts only edges to other residual nodes
    final int[] residualDegree = new int[rows.size()];
    for (int i = 0; i < rows.size(); i++) {
      if (!residualAvailable[i]) {
        continue;
      }
      int deg = 0;
      for (final int j : neighbors[i]) {
        if (residualAvailable[j]) {
          deg++;
        }
      }
      residualDegree[i] = deg;
    }

    final List<List<FeatureListRow>> communities = new ArrayList<>();
    while (true) {
      // pick highest-degree available residual node
      int seed = -1;
      int seedDegree = -1;
      for (int i = 0; i < rows.size(); i++) {
        if (residualAvailable[i] && residualDegree[i] > seedDegree) {
          seed = i;
          seedDegree = residualDegree[i];
        }
      }
      if (seed < 0) {
        break; // no residuals left
      }
      if (seedDegree == 0) {
        // isolated — emit as singleton compound and continue
        communities.add(List.of(rows.get(seed)));
        residualAvailable[seed] = false;
        continue;
      }

      // build closed neighborhood restricted to still-available residual nodes
      final Set<Integer> community = new HashSet<>();
      community.add(seed);
      for (final int j : neighbors[seed]) {
        if (residualAvailable[j]) {
          community.add(j);
        }
      }

      // peel lowest-degree-within-community node until density passes
      while (community.size() >= 2 && densityOf(community, neighbors) < minDensity) {
        int weakest = -1;
        int weakestDegree = Integer.MAX_VALUE;
        for (final int v : community) {
          if (v == seed) {
            continue; // never peel the seed
          }
          int dInside = 0;
          for (final int j : neighbors[v]) {
            if (community.contains(j)) {
              dInside++;
            }
          }
          if (dInside < weakestDegree) {
            weakestDegree = dInside;
            weakest = v;
          }
        }
        if (weakest < 0) {
          break;
        }
        community.remove(weakest);
      }

      // commit community
      final List<FeatureListRow> members = new ArrayList<>(community.size());
      for (final int v : community) {
        members.add(rows.get(v));
        residualAvailable[v] = false;
      }
      communities.add(members);
      // peeled nodes (still available) will be picked up in subsequent iterations as singletons or
      // smaller communities — recompute residual degrees relative to remaining nodes
      for (int i = 0; i < rows.size(); i++) {
        if (!residualAvailable[i]) {
          continue;
        }
        int deg = 0;
        for (final int j : neighbors[i]) {
          if (residualAvailable[j]) {
            deg++;
          }
        }
        residualDegree[i] = deg;
      }
    }
    return communities;
  }

  /**
   * Edge density of the induced subgraph on {@code community}: {@code 2|E| / (|V|·(|V|-1))}. A
   * single node returns 1.0 (vacuously dense). |V|<2 returns 1.0 to avoid division by zero.
   */
  private static double densityOf(@NotNull final Set<Integer> community,
      @NotNull final int[][] neighbors) {
    final int v = community.size();
    if (v < 2) {
      return 1.0;
    }
    int edgesTimesTwo = 0;
    for (final int u : community) {
      for (final int j : neighbors[u]) {
        if (community.contains(j)) {
          edgesTimesTwo++; // each undirected edge counted twice
        }
      }
    }
    return (double) edgesTimesTwo / ((double) v * (v - 1));
  }

  // ----- Compound construction -----

  private @Nullable ModularCompoundRow buildCompound(@NotNull final CompoundList targetList,
      final int compoundId, @NotNull final List<FeatureListRow> members,
      @NotNull final PolarityType polarity) {
    if (members.isEmpty()) {
      return null;
    }
    final List<CompoundFeatureMember> assigned = RoleAssigner.assignRoles(members, polarity,
        mzTolerance, rtTolerance);
    final FeatureListRow preferredRow = findRepresentativeRow(assigned);
    if (preferredRow == null) {
      logger.warning(
          () -> "No representative row found in component of size " + members.size()
              + "; skipping");
      return null;
    }
    final float confidence = computeConfidence(assigned, preferredRow);
    final Double neutralMass = resolveNeutralMass(preferredRow);
    return new ModularCompoundRow(targetList, compoundId, preferredRow, assigned, confidence,
        neutralMass);
  }

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
