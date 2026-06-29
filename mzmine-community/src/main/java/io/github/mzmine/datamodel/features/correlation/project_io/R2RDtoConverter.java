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

package io.github.mzmine.datamodel.features.correlation.project_io;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.correlation.OnlineReactionMatch;
import io.github.mzmine.datamodel.features.correlation.R2RFullCorrelationData;
import io.github.mzmine.datamodel.features.correlation.R2RMS2CosineSimilarityGNPS;
import io.github.mzmine.datamodel.features.correlation.R2RMap;
import io.github.mzmine.datamodel.features.correlation.R2RNetworkingMaps;
import io.github.mzmine.datamodel.features.correlation.R2RSimpleCorrelationData;
import io.github.mzmine.datamodel.features.correlation.R2RSimpleSimilarity;
import io.github.mzmine.datamodel.features.correlation.R2RStructureSimilarity;
import io.github.mzmine.datamodel.features.correlation.R2RSimpleSimilarityList;
import io.github.mzmine.datamodel.features.correlation.R2RSpectralSimilarity;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.correlation.SimpleRowsRelationship;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Converts between {@link R2RNetworkingMaps} (and individual {@link RowsRelationship}s) and their
 * JSON DTOs. Stays Jackson-agnostic so the data-model classes do not have to depend on the JSON
 * layer.
 */
final class R2RDtoConverter {

  private static final Logger logger = Logger.getLogger(R2RDtoConverter.class.getName());

  private R2RDtoConverter() {
  }

  @NotNull
  static R2RNetworkingMapsDto toDto(@NotNull final R2RNetworkingMaps maps) {
    final Map<String, List<R2RRelationshipDto>> mapsByType = new HashMap<>();
    for (final Entry<String, R2RMap<RowsRelationship>> entry : maps.getRowsMaps().entrySet()) {
      final List<R2RRelationshipDto> edges = new ArrayList<>(entry.getValue().size());
      for (final RowsRelationship relationship : entry.getValue().values()) {
        final R2RRelationshipDto dto = toDto(relationship);
        if (dto != null) {
          edges.add(dto);
        }
      }
      if (!edges.isEmpty()) {
        mapsByType.put(entry.getKey(), edges);
      }
    }
    return new R2RNetworkingMapsDto(mapsByType);
  }

  /**
   * Convert a single {@link RowsRelationship} to its DTO. Down-converts
   * {@link R2RFullCorrelationData} to the simple aggregated form (per project consensus — full
   * per-raw-file shape correlation is not persisted). Returns {@code null} and logs for unknown
   * concrete subclasses so they are simply skipped on save.
   */
  @Nullable
  static R2RRelationshipDto toDto(@NotNull final RowsRelationship r) {
    final int idA = r.getRowA().getID();
    final int idB = r.getRowB().getID();
    // decision: R2RFullCorrelationData is intentionally down-converted to R2RSimpleCorrelationData
    // before being persisted. The full per-raw-file map is heavy and can be recomputed.
    return switch (r) {
      case R2RFullCorrelationData full -> toDto(full.toSimpleCorrelationData());
      case R2RSimpleCorrelationData s ->
          new R2RRelationshipDto.SimpleCorrelationDto(idA, idB, s.getTotalPearsonR(),
              s.getHeightPearsonR(), s.getAvgShapeR(), s.getMinShapeR(), s.getMaxShapeR(),
              s.getAvgDPcount());
      case R2RSpectralSimilarity s ->
          new R2RRelationshipDto.SpectralSimilarityDto(idA, idB, s.getInternalType(),
              s.getSimilarity());
      case R2RSimpleSimilarity s ->
          new R2RRelationshipDto.SimpleSimilarityDto(idA, idB, s.getInternalType(),
              (float) s.getScore());
      case R2RSimpleSimilarityList s ->
          new R2RRelationshipDto.SimpleSimilarityListDto(idA, idB, parseType(s.getType()),
              s.getSimilarities().toDoubleArray());
      case R2RMS2CosineSimilarityGNPS s ->
          new R2RRelationshipDto.Ms2GnpsCosineDto(idA, idB, s.getCosineSimilarity(),
              s.getAnnotation(), s.getGNPSEdgeType());
      case OnlineReactionMatch s ->
          new R2RRelationshipDto.OnlineReactionMatchDto(idA, idB, s.getReaction(),
              s.getRowA() != s.getEductRow(), s.getTypeOfThisRow());
      case R2RStructureSimilarity s ->
          new R2RRelationshipDto.StructureSimilarityDto(idA, idB, s.getFingerprintType(),
              s.getInchiA(), s.getInchiB(), (float) s.getScore());
      case SimpleRowsRelationship s ->
          new R2RRelationshipDto.SimpleRelationshipDto(idA, idB, s.getScore(), s.getType(),
              s.getAnnotation());
    };
  }

  @NotNull
  static R2RNetworkingMaps fromDto(@NotNull final R2RNetworkingMapsDto dto,
      @NotNull final ModularFeatureList flist) {
    final R2RNetworkingMaps maps = new R2RNetworkingMaps();
    if (dto.mapsByType() == null) {
      return maps;
    }
    for (final Entry<String, List<R2RRelationshipDto>> entry : dto.mapsByType().entrySet()) {
      final String typeKey = entry.getKey();
      final R2RMap<RowsRelationship> r2rMap = new R2RMap<>();
      for (final R2RRelationshipDto edge : entry.getValue()) {
        final RowsRelationship relationship = fromDto(edge, flist);
        if (relationship != null) {
          r2rMap.add(relationship.getRowA(), relationship.getRowB(), relationship);
        }
      }
      if (!r2rMap.isEmpty()) {
        maps.addAllRowsRelationships(r2rMap, typeKey);
      }
    }
    return maps;
  }

  /**
   * Resolve both rows via {@link ModularFeatureList#findRowByID(int)} and reconstruct the concrete
   * {@link RowsRelationship}. Returns {@code null} (logged) if either row is no longer in the
   * feature list — the corresponding edge is silently dropped.
   */
  @Nullable
  static RowsRelationship fromDto(@NotNull final R2RRelationshipDto dto,
      @NotNull final ModularFeatureList flist) {
    final FeatureListRow a = flist.findRowByID(dto.idA());
    final FeatureListRow b = flist.findRowByID(dto.idB());
    if (a == null || b == null) {
      logger.fine(
          () -> "Skipping R2R edge: row %d or %d not in feature list %s".formatted(dto.idA(),
              dto.idB(), flist.getName()));
      return null;
    }
    return switch (dto) {
      case R2RRelationshipDto.SimpleRelationshipDto s ->
          new SimpleRowsRelationship(a, b, s.score(), s.type(), s.annotation());
      case R2RRelationshipDto.SimpleSimilarityDto s ->
          new R2RSimpleSimilarity(a, b, s.type(), s.similarity());
      case R2RRelationshipDto.SimpleSimilarityListDto s -> {
        final R2RSimpleSimilarityList list = new R2RSimpleSimilarityList(a, b, s.type());
        if (s.similarities() != null) {
          for (final double sim : s.similarities()) {
            list.addSimilarity(sim);
          }
        }
        yield list;
      }
      case R2RRelationshipDto.SpectralSimilarityDto s ->
          new R2RSpectralSimilarity(a, b, s.type(), s.similarity());
      case R2RRelationshipDto.Ms2GnpsCosineDto s ->
          new R2RMS2CosineSimilarityGNPS(a, b, s.cosine(), s.annotation(), s.edgeType());
      case R2RRelationshipDto.OnlineReactionMatchDto s -> {
        // isSwappedAB == true means idB is the educt; otherwise idA is the educt
        final FeatureListRow educt = s.isSwappedAB() ? b : a;
        final FeatureListRow product = s.isSwappedAB() ? a : b;
        yield new OnlineReactionMatch(educt, product, s.reaction(), s.typeOfThisRow());
      }
      case R2RRelationshipDto.SimpleCorrelationDto s ->
          new R2RSimpleCorrelationData(a, b, s.totalSim(), s.heightSim(), s.avgShapeSim(),
              s.minShapeSim(), s.maxShapeSim(), s.avgDPcount());
      case R2RRelationshipDto.StructureSimilarityDto s ->
          new R2RStructureSimilarity(a, b, s.fingerprintType(), s.inchiA(), s.inchiB(),
              s.similarity());
    };
  }

  @NotNull
  private static Type parseType(@NotNull final String typeString) {
    final Type parsed = Type.parse(typeString);
    if (parsed == null) {
      logger.fine(
          () -> "Unknown RowsRelationship.Type string '" + typeString + "' — using Type.OTHER.");
      return Type.OTHER;
    }
    return parsed;
  }
}
