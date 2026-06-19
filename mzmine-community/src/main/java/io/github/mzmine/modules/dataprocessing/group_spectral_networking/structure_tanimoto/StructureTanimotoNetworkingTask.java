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

package io.github.mzmine.modules.dataprocessing.group_spectral_networking.structure_tanimoto;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.R2RSimpleSimilarity;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.MainSpectralNetworkingParameters;
import io.github.mzmine.modules.tools.molecular_similarity.tanimoto.FingerprintType;
import io.github.mzmine.modules.tools.molecular_similarity.tanimoto.TanimotoSimilarity;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import org.jetbrains.annotations.NotNull;

/**
 * Creates {@link Type#STRUCTURE_TANIMOTO} relationships between rows based on the Tanimoto
 * similarity of the molecular structures of their preferred annotations.
 * <p>
 * Processing runs in two parallel phases: first a thread-safe cache of the top fingerprints per row
 * is built (distinct structures by InChIKey first block, limited per row), then all unique row
 * pairs are compared and the maximum Tanimoto similarity across their structures is stored as an
 * edge.
 */
public class StructureTanimotoNetworkingTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(
      StructureTanimotoNetworkingTask.class.getName());

  private final @NotNull ModularFeatureList featureList;
  private final double minSimilarity;
  private final int maxStructuresPerRow;
  // a single shared instance - internally thread-safe (one fingerprinter per thread)
  private final @NotNull TanimotoSimilarity tanimoto;
  private String description = "Structure similarity (Tanimoto) networking";

  public StructureTanimotoNetworkingTask(final ParameterSet mainParameters,
      @NotNull final ModularFeatureList featureList, @NotNull final Instant moduleCallDate,
      final Class<? extends MZmineModule> moduleClass) {
    super(null, moduleCallDate, mainParameters, moduleClass);
    this.featureList = featureList;

    final ParameterSet subParams = mainParameters.getEmbeddedParameterValue(
        MainSpectralNetworkingParameters.algorithms);
    final FingerprintType fingerprintType = subParams.getValue(
        StructureTanimotoNetworkingParameters.fingerprint);
    this.minSimilarity = subParams.getValue(StructureTanimotoNetworkingParameters.minSimilarity);
    this.maxStructuresPerRow = subParams.getValue(
        StructureTanimotoNetworkingParameters.maxStructuresPerRow);
    this.tanimoto = new TanimotoSimilarity(fingerprintType);
  }

  @Override
  protected void process() {
    final List<FeatureListRow> allRows = featureList.getRows();

    // phase 1: compute the top fingerprints per row into a thread-safe cache (parallel over rows)
    description = "Computing molecular fingerprints (" + tanimoto.getFingerprintType() + ")";
    totalItems = allRows.size();
    finishedItems.set(0);
    final Map<FeatureListRow, List<BitSet>> fingerprintCache = new ConcurrentHashMap<>();
    allRows.parallelStream().forEach(row -> {
      if (isCanceled()) {
        return;
      }
      final List<BitSet> fingerprints = computeRowFingerprints(row);
      if (!fingerprints.isEmpty()) {
        fingerprintCache.put(row, fingerprints);
      }
      incrementFinishedItems();
    });

    if (isCanceled()) {
      return;
    }

    // only rows that carry at least one structure - deterministic order by id for stable edges
    final List<FeatureListRow> rows = new ArrayList<>(fingerprintCache.keySet());
    rows.sort(Comparator.comparingInt(FeatureListRow::getID));

    // phase 2: pairwise maximum Tanimoto similarity (parallel over rows, unique pairs only)
    description = "Comparing structures (Tanimoto similarity)";
    totalItems = Math.max(1, rows.size());
    finishedItems.set(0);
    final R2RMap<RowsRelationship> edges = new R2RMap<>();
    IntStream.range(0, rows.size()).parallel().forEach(i -> {
      if (isCanceled()) {
        return;
      }
      final FeatureListRow rowA = rows.get(i);
      final List<BitSet> fingerprintsA = fingerprintCache.get(rowA);
      for (int j = i + 1; j < rows.size(); j++) {
        final FeatureListRow rowB = rows.get(j);
        final float similarity = TanimotoSimilarity.maxTanimoto(fingerprintsA,
            fingerprintCache.get(rowB));
        if (similarity >= minSimilarity) {
          edges.add(rowA, rowB,
              new R2RSimpleSimilarity(rowA, rowB, Type.STRUCTURE_TANIMOTO, similarity));
        }
      }
      incrementFinishedItems();
    });

    if (isCanceled()) {
      return;
    }

    featureList.getRowMaps().addAllRowsRelationships(edges, Type.STRUCTURE_TANIMOTO);
    logger.info(
        "Structure Tanimoto networking on %d rows with structures created %d edges (%s, min similarity %.2f)".formatted(
            rows.size(), edges.size(), tanimoto.getFingerprintType(), minSimilarity));
  }

  /**
   * The top distinct structures of a row turned into fingerprints. Streams the annotations by
   * descending confidence, keeps distinct structures by InChIKey first block, limits to
   * {@link #maxStructuresPerRow}, and fingerprints them with the shared {@link #tanimoto}
   * instance.
   */
  private @NotNull List<BitSet> computeRowFingerprints(@NotNull final FeatureListRow row) {
    final List<FeatureAnnotation> annotations = row.getAllFeatureAnnotations();
    if (annotations.isEmpty()) {
      return List.of();
    }
    final Set<String> seenKeys = new HashSet<>();
    final List<BitSet> fingerprints = new ArrayList<>(maxStructuresPerRow);
    for (final FeatureAnnotation annotation : annotations) {
      if (fingerprints.size() >= maxStructuresPerRow) {
        break;
      }
      final MolecularStructure structure = annotation.getStructure();
      if (structure == null) {
        continue;
      }
      // distinct by InChIKey first block (fall back to canonical SMILES / identity)
      if (!seenKeys.add(distinctKey(annotation, structure))) {
        continue;
      }
      final BitSet fingerprint = tanimoto.getFingerprint(structure.structure());
      if (fingerprint != null) {
        fingerprints.add(fingerprint);
      }
    }
    return fingerprints;
  }

  private static @NotNull String distinctKey(@NotNull final FeatureAnnotation annotation,
      @NotNull final MolecularStructure structure) {
    // prefer the stored InChIKey, then the structure-derived key
    String firstBlock = CompoundAnnotationUtils.inchiKeyFirstBlock(annotation.getInChIKey());
    if (firstBlock == null) {
      firstBlock = CompoundAnnotationUtils.inchiKeyFirstBlock(structure.inchiKey());
    }
    if (firstBlock != null) {
      return firstBlock;
    }
    final String smiles = structure.canonicalSmiles();
    return smiles != null && !smiles.isBlank() ? smiles
        : "id:" + System.identityHashCode(structure);
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(featureList);
  }
}
