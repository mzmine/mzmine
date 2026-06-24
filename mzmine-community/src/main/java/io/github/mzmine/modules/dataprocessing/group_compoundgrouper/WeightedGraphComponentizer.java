package io.github.mzmine.modules.dataprocessing.group_compoundgrouper;

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.compoundlist.CompoundComponentizerStrategy;
import io.github.mzmine.datamodel.features.compoundlist.CompoundContradiction;
import io.github.mzmine.datamodel.features.compoundlist.CompoundContradiction.ContradictionType;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRepresentativeSelector;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.types.compoundlist.CompoundContradictionListType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.datamodel.identities.iontype.IonNetwork;
import io.github.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Multi-evidence weighted-graph compound componentizer.
 * <p>
 * Phase 1 — seed dense, high-confidence cores: each <em>best</em> ion identity network becomes a
 * core, then dense communities in the residual (no-best-IIN) correlation subgraph become cores
 * (heavily inter-connected residual cores are merged so dense areas form a single compound).
 * <p>
 * Phase 2 — assign each remaining "loose" row to its single best core via weighted neighbor voting
 * plus a few rounds of bounded message-passing smoothing. A row also joins its runner-up core only
 * on a genuine near-tie, and rows with a preferred annotation / defined best ion identity are forced
 * into a single core.
 * <p>
 * Phase 3 — assemble compounds (roles via {@link RoleAssigner}), optionally split a compound whose
 * members carry MS2 matches to different structures, and record annotation/RT contradictions on the
 * involved member rows with a roll-up on the compound row.
 */
public final class WeightedGraphComponentizer implements CompoundComponentizerStrategy {

  private static final Logger logger = Logger.getLogger(
      WeightedGraphComponentizer.class.getName());

  private final Config config;
  private final CompoundRepresentativeSelector selector;

  public WeightedGraphComponentizer(@NotNull final Config config,
      @NotNull final CompoundRepresentativeSelector selector) {
    this.config = config;
    this.selector = selector;
  }

  @Override
  public @Nullable String validateInputs(@NotNull final ModularFeatureList featureList) {
    if (featureList.getNumberOfRows() == 0) {
      return "CompoundGrouper requires a non-empty feature list.";
    }
    final boolean hasIin = IonNetworkLogic.streamNetworks(featureList, false).findAny().isPresent();
    final boolean hasCorrelation = featureList.getMs1CorrelationMap().map(m -> !m.isEmpty())
        .orElse(false);
    if (!hasIin && !hasCorrelation) {
      return "WeightedGraph requires Ion Identity Networking or MS1 correlation grouping output. "
          + "Run those modules first.";
    }
    return null;
  }

  @Override
  public @NotNull List<ModularCompoundRow> componentize(
      @NotNull final ModularFeatureList featureList, @NotNull final CompoundList targetList) {
    final List<FeatureListRow> rows = new ArrayList<>(featureList.getRows());
    if (rows.isEmpty()) {
      return List.of();
    }
    // make sure the source rows can carry contradiction lists
    final boolean hasContradictionType = featureList.getRowTypes().stream()
        .anyMatch(t -> t instanceof CompoundContradictionListType);
    if (!hasContradictionType) {
      featureList.addRowType(new CompoundContradictionListType());
    }

    final int n = rows.size();
    final Map<Integer, Integer> rowIdToIdx = new HashMap<>(n * 2);
    for (int i = 0; i < n; i++) {
      rowIdToIdx.put(rows.get(i).getID(), i);
    }

    // ----- graph -----
    final List<Map<Integer, Double>> adj = buildAdjacency(featureList, rows, rowIdToIdx);

    // ----- Phase 1: cores -----
    final int[] coreOf = new int[n];
    java.util.Arrays.fill(coreOf, -1);
    final List<CoreData> cores = new ArrayList<>();
    seedIinCores(featureList, rowIdToIdx, cores, coreOf);

    // residual = rows with no core yet AND no edge to any existing (IIN) core
    final boolean[] residualAvail = new boolean[n];
    for (int i = 0; i < n; i++) {
      residualAvail[i] = coreOf[i] < 0 && !hasCoreNeighbor(i, adj, coreOf);
    }
    detectResidualCores(rows, adj, residualAvail, cores, coreOf, config.minCoreDensity());
    mergeResidualCores(cores, coreOf, adj, config.coreMergeOverlap());

    // ----- Phase 2: assign loose rows -----
    final List<List<Integer>> assignedExtra = new ArrayList<>(cores.size());
    for (int c = 0; c < cores.size(); c++) {
      assignedExtra.add(new ArrayList<>());
    }
    final List<Integer> looseSingletons = new ArrayList<>();
    assignLooseRows(rows, adj, coreOf, cores, assignedExtra, looseSingletons);

    // ----- Phase 3: assemble -----
    final PolarityType polarity = deriveFeatureListPolarity(featureList);
    final List<ModularCompoundRow> result = new ArrayList<>(cores.size() + looseSingletons.size());
    int compoundId = 1;
    for (int c = 0; c < cores.size(); c++) {
      final LinkedHashSet<Integer> memberIdx = new LinkedHashSet<>(cores.get(c).base);
      memberIdx.addAll(assignedExtra.get(c));
      final List<FeatureListRow> memberRows = new ArrayList<>(memberIdx.size());
      for (final int idx : memberIdx) {
        memberRows.add(rows.get(idx));
      }
      for (final List<FeatureListRow> group : splitMembers(memberRows)) {
        final ModularCompoundRow cr = buildCompound(targetList, compoundId, group, polarity,
            featureList);
        if (cr != null) {
          result.add(cr);
          compoundId++;
        }
      }
    }
    for (final int idx : looseSingletons) {
      final ModularCompoundRow cr = buildCompound(targetList, compoundId, List.of(rows.get(idx)),
          polarity, featureList);
      if (cr != null) {
        result.add(cr);
        compoundId++;
      }
    }
    return result;
  }

  // ================= Phase 1: graph + cores =================

  private @NotNull List<Map<Integer, Double>> buildAdjacency(
      @NotNull final ModularFeatureList featureList, @NotNull final List<FeatureListRow> rows,
      @NotNull final Map<Integer, Integer> rowIdToIdx) {
    final int n = rows.size();
    final List<Map<Integer, Double>> adj = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      adj.add(new HashMap<>(2));
    }

    // correlation edges (shape)
    final Optional<R2RMap<RowsRelationship>> opMap = featureList.getMs1CorrelationMap();
    if (opMap.isPresent()) {
      for (final RowsRelationship r2r : opMap.get().values()) {
        final FeatureListRow a = r2r.getRowA();
        final FeatureListRow b = r2r.getRowB();
        if (a == null || b == null) {
          continue;
        }
        final Integer ia = rowIdToIdx.get(a.getID());
        final Integer ib = rowIdToIdx.get(b.getID());
        if (ia == null || ib == null || ia.equals(ib)) {
          continue;
        }
        final double s = r2r.getScore();
        if (!Double.isNaN(s) && s > 0) {
          addEdge(adj, ia, ib, config.wShape() * clamp01(s));
        } else {
          // ensure the structural edge exists even when the score is unavailable
          addEdge(adj, ia, ib, config.wShape() * 0.5);
        }
      }
    }

    // ion identity edges (all networks)
    IonNetworkLogic.streamNetworks(featureList, false).forEach(net -> {
      final List<Integer> idxs = new ArrayList<>();
      for (final FeatureListRow r : net.getRows()) {
        final Integer idx = rowIdToIdx.get(r.getID());
        if (idx != null) {
          idxs.add(idx);
        }
      }
      for (int a = 0; a < idxs.size(); a++) {
        for (int b = a + 1; b < idxs.size(); b++) {
          addEdge(adj, idxs.get(a), idxs.get(b), config.wIin());
        }
      }
    });

    // augment existing edges with RT coherence and isotope evidence (never creates new edges)
    final List<long[]> pairs = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      for (final int j : adj.get(i).keySet()) {
        if (i < j) {
          pairs.add(new long[]{i, j});
        }
      }
    }
    for (final long[] pair : pairs) {
      final int i = (int) pair[0];
      final int j = (int) pair[1];
      final FeatureListRow ri = rows.get(i);
      final FeatureListRow rj = rows.get(j);
      final Float rtI = ri.getAverageRT();
      final Float rtJ = rj.getAverageRT();
      if (rtI != null && rtJ != null && config.rtTolerance().checkWithinTolerance(rtI, rtJ)) {
        addEdge(adj, i, j, config.wRt());
      }
      if (RoleAssigner.looksLikeIsotopologue(ri, rj, config.mzTolerance(), config.rtTolerance())
          || RoleAssigner.looksLikeIsotopologue(rj, ri, config.mzTolerance(),
          config.rtTolerance())) {
        addEdge(adj, i, j, config.wIsotope());
      }
    }
    return adj;
  }

  private static void addEdge(@NotNull final List<Map<Integer, Double>> adj, final int i,
      final int j, final double w) {
    if (w <= 0) {
      return;
    }
    adj.get(i).merge(j, w, Double::sum);
    adj.get(j).merge(i, w, Double::sum);
  }

  private void seedIinCores(@NotNull final ModularFeatureList featureList,
      @NotNull final Map<Integer, Integer> rowIdToIdx, @NotNull final List<CoreData> cores,
      @NotNull final int[] coreOf) {
    final List<IonNetwork> best = new ArrayList<>(
        IonNetworkLogic.streamNetworks(featureList, true).toList());
    // stable compound ids: larger networks first, then smallest row id
    best.sort((a, b) -> {
      final int sizeCmp = Integer.compare(b.getRows().size(), a.getRows().size());
      return sizeCmp != 0 ? sizeCmp : Integer.compare(minRowId(a), minRowId(b));
    });
    for (final IonNetwork net : best) {
      final List<Integer> idxs = new ArrayList<>();
      for (final FeatureListRow r : net.getRows()) {
        final Integer idx = rowIdToIdx.get(r.getID());
        if (idx != null && coreOf[idx] < 0) {
          idxs.add(idx);
        }
      }
      if (idxs.isEmpty()) {
        continue;
      }
      final int cid = cores.size();
      final CoreData cd = new CoreData(net);
      for (final int idx : idxs) {
        cd.base.add(idx);
        coreOf[idx] = cid;
      }
      cores.add(cd);
    }
  }

  private static int minRowId(@NotNull final IonNetwork net) {
    int min = Integer.MAX_VALUE;
    for (final FeatureListRow r : net.getRows()) {
      min = Math.min(min, r.getID());
    }
    return min;
  }

  private static boolean hasCoreNeighbor(final int i, @NotNull final List<Map<Integer, Double>> adj,
      @NotNull final int[] coreOf) {
    for (final int j : adj.get(i).keySet()) {
      if (coreOf[j] >= 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Greedy density-first detector on the residual subgraph. Highest-degree available node seeds a
   * closed neighborhood; the weakest members are peeled until the induced density passes
   * {@code minDensity}. Committed communities (and leftover singletons) become cores.
   */
  private void detectResidualCores(@NotNull final List<FeatureListRow> rows,
      @NotNull final List<Map<Integer, Double>> adj, @NotNull final boolean[] residualAvail,
      @NotNull final List<CoreData> cores, @NotNull final int[] coreOf, final double minDensity) {
    final int n = rows.size();
    final int[] degree = new int[n];
    recomputeResidualDegrees(adj, residualAvail, degree);

    while (true) {
      int seed = -1;
      int seedDegree = -1;
      for (int i = 0; i < n; i++) {
        if (residualAvail[i] && degree[i] > seedDegree) {
          seed = i;
          seedDegree = degree[i];
        }
      }
      if (seed < 0) {
        break;
      }
      if (seedDegree == 0) {
        commitCore(cores, coreOf, residualAvail, Set.of(seed), null);
        recomputeResidualDegrees(adj, residualAvail, degree);
        continue;
      }
      final Set<Integer> community = new LinkedHashSet<>();
      community.add(seed);
      for (final int j : adj.get(seed).keySet()) {
        if (residualAvail[j]) {
          community.add(j);
        }
      }
      while (community.size() >= 2 && densityOf(community, adj) < minDensity) {
        int weakest = -1;
        int weakestDegree = Integer.MAX_VALUE;
        for (final int v : community) {
          if (v == seed) {
            continue;
          }
          int dInside = 0;
          for (final int j : adj.get(v).keySet()) {
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
      commitCore(cores, coreOf, residualAvail, community, null);
      recomputeResidualDegrees(adj, residualAvail, degree);
    }
  }

  private static void recomputeResidualDegrees(@NotNull final List<Map<Integer, Double>> adj,
      @NotNull final boolean[] residualAvail, @NotNull final int[] degree) {
    for (int i = 0; i < degree.length; i++) {
      if (!residualAvail[i]) {
        degree[i] = 0;
        continue;
      }
      int d = 0;
      for (final int j : adj.get(i).keySet()) {
        if (residualAvail[j]) {
          d++;
        }
      }
      degree[i] = d;
    }
  }

  private static void commitCore(@NotNull final List<CoreData> cores, @NotNull final int[] coreOf,
      @NotNull final boolean[] residualAvail, @NotNull final Set<Integer> members,
      @Nullable final IonNetwork network) {
    final int cid = cores.size();
    final CoreData cd = new CoreData(network);
    for (final int v : members) {
      cd.base.add(v);
      coreOf[v] = cid;
      residualAvail[v] = false;
    }
    cores.add(cd);
  }

  private static double densityOf(@NotNull final Set<Integer> community,
      @NotNull final List<Map<Integer, Double>> adj) {
    final int v = community.size();
    if (v < 2) {
      return 1.0;
    }
    int edgesTimesTwo = 0;
    for (final int u : community) {
      for (final int j : adj.get(u).keySet()) {
        if (community.contains(j)) {
          edgesTimesTwo++;
        }
      }
    }
    return (double) edgesTimesTwo / ((double) v * (v - 1));
  }

  /**
   * Merge residual (network == null) cores that are heavily inter-connected, so a dense area split
   * by the greedy detector collapses into one compound. Two cores merge when the number of
   * cross-edges between them reaches {@code overlap * min(|A|,|B|)}.
   */
  private void mergeResidualCores(@NotNull final List<CoreData> cores, @NotNull final int[] coreOf,
      @NotNull final List<Map<Integer, Double>> adj, final double overlap) {
    final int m = cores.size();
    final int[] parent = new int[m];
    for (int i = 0; i < m; i++) {
      parent[i] = i;
    }
    for (int a = 0; a < m; a++) {
      if (cores.get(a).network != null) {
        continue;
      }
      for (int b = a + 1; b < m; b++) {
        if (cores.get(b).network != null) {
          continue;
        }
        final int cross = countCrossEdges(cores.get(a).base, cores.get(b).base, adj);
        final int minSize = Math.min(cores.get(a).base.size(), cores.get(b).base.size());
        if (minSize > 0 && cross >= overlap * minSize) {
          union(parent, a, b);
        }
      }
    }
    // rebuild
    final Map<Integer, CoreData> byRoot = new LinkedHashMap<>();
    final Map<Integer, Integer> rootToNewIdx = new LinkedHashMap<>();
    final List<CoreData> merged = new ArrayList<>();
    for (int i = 0; i < m; i++) {
      final int root = find(parent, i);
      final CoreData target = byRoot.computeIfAbsent(root, r -> {
        final CoreData cd = new CoreData(cores.get(r).network);
        rootToNewIdx.put(r, merged.size());
        merged.add(cd);
        return cd;
      });
      target.base.addAll(cores.get(i).base);
      if (cores.get(i).network != null) {
        target.network = cores.get(i).network;
      }
    }
    if (merged.size() == m) {
      return; // nothing merged
    }
    cores.clear();
    cores.addAll(merged);
    java.util.Arrays.fill(coreOf, -1);
    for (int c = 0; c < cores.size(); c++) {
      for (final int idx : cores.get(c).base) {
        coreOf[idx] = c;
      }
    }
  }

  private static int countCrossEdges(@NotNull final Set<Integer> a, @NotNull final Set<Integer> b,
      @NotNull final List<Map<Integer, Double>> adj) {
    final Set<Integer> small = a.size() <= b.size() ? a : b;
    final Set<Integer> large = a.size() <= b.size() ? b : a;
    int cross = 0;
    for (final int u : small) {
      for (final int v : adj.get(u).keySet()) {
        if (large.contains(v)) {
          cross++;
        }
      }
    }
    return cross;
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

  // ================= Phase 2: loose-row assignment =================

  private void assignLooseRows(@NotNull final List<FeatureListRow> rows,
      @NotNull final List<Map<Integer, Double>> adj, @NotNull final int[] coreOf,
      @NotNull final List<CoreData> cores, @NotNull final List<List<Integer>> assignedExtra,
      @NotNull final List<Integer> looseSingletons) {
    final int n = rows.size();
    final List<Integer> loose = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      if (coreOf[i] < 0) {
        loose.add(i);
      }
    }
    if (loose.isEmpty()) {
      return;
    }

    // representatives of each core (for annotation agreement)
    final Map<Integer, FeatureListRow> coreRep = new HashMap<>();
    for (int c = 0; c < cores.size(); c++) {
      final List<FeatureListRow> baseRows = new ArrayList<>(cores.get(c).base.size());
      for (final int idx : cores.get(c).base) {
        baseRows.add(rows.get(idx));
      }
      if (!baseRows.isEmpty()) {
        coreRep.put(c, selector.pickRepresentative(baseRows));
      }
    }

    // base scores per loose row: coreIdx -> score
    final Map<Integer, Map<Integer, Double>> base = new HashMap<>();
    for (final int i : loose) {
      final Map<Integer, Double> m = new HashMap<>();
      for (final Map.Entry<Integer, Double> e : adj.get(i).entrySet()) {
        final int c = coreOf[e.getKey()];
        if (c >= 0) {
          m.merge(c, e.getValue(), Double::sum);
        }
      }
      // annotation agreement / contradiction with the core representative
      for (final Integer c : new ArrayList<>(m.keySet())) {
        final FeatureListRow rep = coreRep.get(c);
        if (rep != null) {
          final double term = annotationTerm(rows.get(i), rep);
          if (term != 0) {
            m.merge(c, term, Double::sum);
          }
        }
      }
      // size penalty on oversized cores
      for (final Integer c : new ArrayList<>(m.keySet())) {
        if (cores.get(c).base.size() > config.sizePenaltyThreshold()) {
          m.put(c, m.get(c) * (1.0 - config.sizePenaltyAlpha()));
        }
      }
      base.put(i, m);
    }

    // bounded message passing through loose-loose edges
    Map<Integer, Map<Integer, Double>> cur = deepCopy(base);
    final double d = config.mpDamping();
    for (int iter = 0; iter < config.mpIterations() && d > 0; iter++) {
      final Map<Integer, Map<Integer, Double>> next = new HashMap<>();
      for (final int i : loose) {
        final Map<Integer, Double> ni = new HashMap<>();
        for (final Map.Entry<Integer, Double> e : base.get(i).entrySet()) {
          ni.put(e.getKey(), (1.0 - d) * e.getValue());
        }
        for (final Map.Entry<Integer, Double> edge : adj.get(i).entrySet()) {
          final int j = edge.getKey();
          if (coreOf[j] >= 0) {
            continue; // only propagate from loose neighbors
          }
          final Map<Integer, Double> nj = normalizeClamp(cur.get(j));
          if (nj == null) {
            continue;
          }
          for (final Map.Entry<Integer, Double> pe : nj.entrySet()) {
            ni.merge(pe.getKey(), d * edge.getValue() * pe.getValue(), Double::sum);
          }
        }
        next.put(i, ni);
      }
      cur = next;
    }

    // assignment
    for (final int i : loose) {
      final Map<Integer, Double> post = normalizeClamp(cur.get(i));
      if (post == null || post.isEmpty()) {
        looseSingletons.add(i);
        continue;
      }
      int c1 = -1;
      int c2 = -1;
      double p1 = -1;
      double p2 = -1;
      for (final Map.Entry<Integer, Double> e : post.entrySet()) {
        if (e.getValue() > p1) {
          c2 = c1;
          p2 = p1;
          c1 = e.getKey();
          p1 = e.getValue();
        } else if (e.getValue() > p2) {
          c2 = e.getKey();
          p2 = e.getValue();
        }
      }
      if (c1 < 0) {
        looseSingletons.add(i);
        continue;
      }
      if (forcedSingle(rows.get(i))) {
        final int chosen = chooseForcedCore(rows.get(i), post.keySet(), coreRep, cores, c1);
        assignedExtra.get(chosen).add(i);
      } else {
        assignedExtra.get(c1).add(i);
        if (c2 >= 0 && p2 >= p1 - config.nearTieMargin() && p1 < config.assignmentThreshold()) {
          assignedExtra.get(c2).add(i);
        }
      }
    }
  }

  private int chooseForcedCore(@NotNull final FeatureListRow row,
      @NotNull final Set<Integer> candidates, @NotNull final Map<Integer, FeatureListRow> coreRep,
      @NotNull final List<CoreData> cores, final int fallback) {
    // 1) core sharing this row's ion network
    final IonIdentity ion = row.getBestIonIdentity();
    final IonNetwork net = ion != null ? ion.getNetwork() : null;
    if (net != null) {
      for (final int c : candidates) {
        if (net.equals(cores.get(c).network)) {
          return c;
        }
      }
    }
    // 2) core whose representative shares this row's annotation
    final String rk = anyAnnoKey(row);
    if (rk != null) {
      for (final int c : candidates) {
        final FeatureListRow rep = coreRep.get(c);
        if (rep != null && rk.equals(anyAnnoKey(rep))) {
          return c;
        }
      }
    }
    return fallback;
  }

  private static @Nullable Map<Integer, Double> normalizeClamp(
      @Nullable final Map<Integer, Double> in) {
    if (in == null || in.isEmpty()) {
      return null;
    }
    double sum = 0;
    for (final double v : in.values()) {
      sum += Math.max(0, v);
    }
    if (sum <= 0) {
      return null;
    }
    final Map<Integer, Double> out = new HashMap<>(in.size() * 2);
    for (final Map.Entry<Integer, Double> e : in.entrySet()) {
      final double v = Math.max(0, e.getValue());
      if (v > 0) {
        out.put(e.getKey(), v / sum);
      }
    }
    return out;
  }

  private static @NotNull Map<Integer, Map<Integer, Double>> deepCopy(
      @NotNull final Map<Integer, Map<Integer, Double>> in) {
    final Map<Integer, Map<Integer, Double>> out = new HashMap<>(in.size() * 2);
    for (final Map.Entry<Integer, Map<Integer, Double>> e : in.entrySet()) {
      out.put(e.getKey(), new HashMap<>(e.getValue()));
    }
    return out;
  }

  // ================= Phase 3: assembly =================

  private @NotNull List<List<FeatureListRow>> splitMembers(
      @NotNull final List<FeatureListRow> memberRows) {
    if (!config.splitOnAnnotationConflict() || memberRows.size() < 2) {
      return List.of(memberRows);
    }
    final Map<String, List<FeatureListRow>> byKey = new LinkedHashMap<>();
    final List<FeatureListRow> unannotated = new ArrayList<>();
    for (final FeatureListRow r : memberRows) {
      final String k = ms2Key(r);
      if (k != null) {
        byKey.computeIfAbsent(k, x -> new ArrayList<>()).add(r);
      } else {
        unannotated.add(r);
      }
    }
    if (byKey.size() <= 1) {
      return List.of(memberRows);
    }
    final FeatureListRow rep = selector.pickRepresentative(memberRows);
    final String repKey = ms2Key(rep);
    final String targetKey = (repKey != null && byKey.containsKey(repKey)) ? repKey
        : largestGroupKey(byKey);
    byKey.get(targetKey).addAll(unannotated);
    return new ArrayList<>(byKey.values());
  }

  private static @NotNull String largestGroupKey(
      @NotNull final Map<String, List<FeatureListRow>> byKey) {
    String bestKey = null;
    int bestSize = -1;
    for (final Map.Entry<String, List<FeatureListRow>> e : byKey.entrySet()) {
      if (e.getValue().size() > bestSize) {
        bestSize = e.getValue().size();
        bestKey = e.getKey();
      }
    }
    return bestKey;
  }

  private @Nullable ModularCompoundRow buildCompound(@NotNull final CompoundList targetList,
      final int compoundId, @NotNull final List<FeatureListRow> memberRows,
      @NotNull final PolarityType polarity, @NotNull final ModularFeatureList featureList) {
    if (memberRows.isEmpty()) {
      return null;
    }
    final List<CompoundFeatureMember> assigned = RoleAssigner.assignRoles(memberRows, polarity,
        config.mzTolerance(), config.rtTolerance(), selector);
    final FeatureListRow preferred = findRepresentativeRow(assigned);
    if (preferred == null) {
      logger.warning(() -> "No representative row in component of size " + memberRows.size());
      return null;
    }
    final List<CompoundContradiction> contradictions = detectContradictions(memberRows, compoundId);
    final float confidence = computeConfidence(assigned, preferred, contradictions);
    final Double neutralMass = resolveNeutralMass(preferred);
    final ModularCompoundRow cr = new ModularCompoundRow(targetList, compoundId, preferred, assigned,
        confidence, neutralMass);
    if (!contradictions.isEmpty()) {
      cr.set(CompoundContradictionListType.class, List.copyOf(contradictions));
      attachContradictionsToMembers(featureList, contradictions);
    }
    return cr;
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

  private @NotNull List<CompoundContradiction> detectContradictions(
      @NotNull final List<FeatureListRow> members, final int compoundId) {
    final List<CompoundContradiction> out = new ArrayList<>();

    // MS2 annotation conflict
    final Map<String, List<Integer>> ms2 = new LinkedHashMap<>();
    for (final FeatureListRow r : members) {
      final String k = ms2Key(r);
      if (k != null) {
        ms2.computeIfAbsent(k, x -> new ArrayList<>()).add(r.getID());
      }
    }
    if (ms2.size() >= 2) {
      final List<Integer> involved = new ArrayList<>();
      ms2.values().forEach(involved::addAll);
      out.add(new CompoundContradiction(ContradictionType.MS2_ANNOTATION_CONFLICT, 0.9f, compoundId,
          involved, "MS2 matches differ: " + String.join(" vs ", ms2.keySet())));
    }

    // MS1 vs MS2 conflict
    final Map<String, List<Integer>> ms1 = new LinkedHashMap<>();
    for (final FeatureListRow r : members) {
      final String k = ms1Key(r);
      if (k != null) {
        ms1.computeIfAbsent(k, x -> new ArrayList<>()).add(r.getID());
      }
    }
    if (!ms1.isEmpty() && !ms2.isEmpty()) {
      for (final String k1 : ms1.keySet()) {
        if (!ms2.containsKey(k1)) {
          final List<Integer> involved = new ArrayList<>(ms1.get(k1));
          ms2.values().forEach(involved::addAll);
          out.add(new CompoundContradiction(ContradictionType.MS1_MS2_CONFLICT, 0.6f, compoundId,
              involved, "MS1 annotation " + k1 + " not among MS2 matches " + ms2.keySet()));
          break;
        }
      }
    }

    // formula conflict
    final Map<String, List<Integer>> formulas = new LinkedHashMap<>();
    for (final FeatureListRow r : members) {
      final String f = formulaKey(r);
      if (f != null) {
        formulas.computeIfAbsent(f, x -> new ArrayList<>()).add(r.getID());
      }
    }
    if (formulas.size() >= 2) {
      final List<Integer> involved = new ArrayList<>();
      formulas.values().forEach(involved::addAll);
      out.add(new CompoundContradiction(ContradictionType.FORMULA_CONFLICT, 0.3f, compoundId,
          involved, "Incompatible formulas: " + String.join(", ", formulas.keySet())));
    }

    // RT spread
    float minRt = Float.MAX_VALUE;
    float maxRt = -Float.MAX_VALUE;
    int minRow = -1;
    int maxRow = -1;
    for (final FeatureListRow r : members) {
      final Float rt = r.getAverageRT();
      if (rt == null) {
        continue;
      }
      if (rt < minRt) {
        minRt = rt;
        minRow = r.getID();
      }
      if (rt > maxRt) {
        maxRt = rt;
        maxRow = r.getID();
      }
    }
    if (minRow >= 0 && maxRow >= 0) {
      final double spread = maxRt - minRt;
      if (spread > config.rtSpreadThreshold()) {
        final float severity = (float) Math.min(1.0, spread / (2.0 * config.rtSpreadThreshold()));
        out.add(new CompoundContradiction(ContradictionType.RT_SPREAD, severity, compoundId,
            List.of(minRow, maxRow),
            String.format("RT spread %.3f min exceeds %.3f", spread, config.rtSpreadThreshold())));
      }
    }
    return out;
  }

  private static void attachContradictionsToMembers(@NotNull final ModularFeatureList featureList,
      @NotNull final List<CompoundContradiction> contradictions) {
    for (final CompoundContradiction c : contradictions) {
      for (final int rowId : c.involvedRowIds()) {
        final FeatureListRow r = featureList.findRowByID(rowId);
        if (!(r instanceof ModularFeatureListRow mflr)) {
          continue;
        }
        final List<CompoundContradiction> existing = mflr.getOrDefault(
            CompoundContradictionListType.class, List.of());
        final List<CompoundContradiction> updated = new ArrayList<>(existing);
        updated.add(c);
        mflr.set(CompoundContradictionListType.class, List.copyOf(updated));
      }
    }
  }

  private float computeConfidence(@NotNull final List<CompoundFeatureMember> members,
      @NotNull final FeatureListRow preferredRow,
      @NotNull final List<CompoundContradiction> contradictions) {
    if (members.isEmpty()) {
      return 0f;
    }
    final float sizeScore = Math.min(1f, members.size() / 5f);
    long iinCount = 0;
    for (final CompoundFeatureMember m : members) {
      if (m.row().hasIonIdentity()) {
        iinCount++;
      }
    }
    final float iinFraction = (float) iinCount / members.size();

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
        if (config.rtTolerance().checkWithinTolerance(refRt, rt)) {
          rtCoherent++;
        }
      }
    }
    final float rtCoherence = rtTotal == 0 ? 0f : (float) rtCoherent / rtTotal;

    float penalty = 0f;
    for (final CompoundContradiction c : contradictions) {
      penalty += 0.1f * c.severity();
    }
    penalty = Math.min(0.4f, penalty);

    final float score = 0.3f * sizeScore + 0.5f * iinFraction + 0.2f * rtCoherence - penalty;
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
      return null;
    }
  }

  private static @NotNull PolarityType deriveFeatureListPolarity(
      @NotNull final ModularFeatureList featureList) {
    long positive = 0;
    long negative = 0;
    final List<RawDataFile> files = featureList.getRawDataFiles();
    if (files != null) {
      for (final RawDataFile f : files) {
        for (final PolarityType p : f.getDataPolarity()) {
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

  // ================= annotation helpers =================

  private double annotationTerm(@NotNull final FeatureListRow row,
      @NotNull final FeatureListRow rep) {
    final String rk = anyAnnoKey(row);
    final String ck = anyAnnoKey(rep);
    if (rk == null || ck == null) {
      return 0;
    }
    return rk.equals(ck) ? config.wAnnotation() : -config.wAnnotation();
  }

  private static boolean forcedSingle(@NotNull final FeatureListRow row) {
    return row.getPreferredAnnotation() != null || row.getBestIonIdentity() != null;
  }

  private static @Nullable String structureKey(@Nullable final FeatureAnnotation a) {
    if (a == null) {
      return null;
    }
    final String ik = a.getInChIKey();
    if (ik != null && !ik.isBlank()) {
      return ik.length() >= 14 ? ik.substring(0, 14) : ik;
    }
    final String name = a.getCompoundName();
    return (name != null && !name.isBlank()) ? name.trim().toLowerCase() : null;
  }

  private static @Nullable String ms2Key(@NotNull final FeatureListRow r) {
    final var matches = r.getSpectralLibraryMatches();
    return matches.isEmpty() ? null : structureKey(matches.getFirst());
  }

  private static @Nullable String ms1Key(@NotNull final FeatureListRow r) {
    final var annotations = r.getCompoundAnnotations();
    return annotations.isEmpty() ? null : structureKey(annotations.getFirst());
  }

  private static @Nullable String anyAnnoKey(@NotNull final FeatureListRow r) {
    final String k2 = ms2Key(r);
    if (k2 != null) {
      return k2;
    }
    final String k1 = ms1Key(r);
    if (k1 != null) {
      return k1;
    }
    return structureKey(r.getPreferredAnnotation());
  }

  private static @Nullable String formulaKey(@NotNull final FeatureListRow r) {
    final List<ResultFormula> formulas = r.getFormulas();
    if (formulas != null && !formulas.isEmpty()) {
      return formulas.getFirst().getFormulaAsString();
    }
    final FeatureAnnotation a = r.getPreferredAnnotation();
    return a != null ? a.getFormula() : null;
  }

  private static double clamp01(final double v) {
    return Math.max(0, Math.min(1, v));
  }

  // ================= types =================

  /**
   * A grouping core: a set of fixed base-member row indices and, for IIN cores, the seeding ion
   * network ({@code null} for correlation cores).
   */
  private static final class CoreData {

    private final Set<Integer> base = new LinkedHashSet<>();
    @Nullable
    private IonNetwork network;

    private CoreData(@Nullable final IonNetwork network) {
      this.network = network;
    }
  }

  /**
   * Immutable configuration assembled from {@link WeightedGraphComponentizerParameters}.
   */
  public record Config(@NotNull MZTolerance mzTolerance, @NotNull RTTolerance rtTolerance,
                       double wRt, double wShape, double wIin, double wIsotope, double wAnnotation,
                       double minCoreDensity, double coreMergeOverlap, double assignmentThreshold,
                       double nearTieMargin, int sizePenaltyThreshold, double sizePenaltyAlpha,
                       double rtSpreadThreshold, int mpIterations, double mpDamping,
                       boolean splitOnAnnotationConflict) {

  }
}
