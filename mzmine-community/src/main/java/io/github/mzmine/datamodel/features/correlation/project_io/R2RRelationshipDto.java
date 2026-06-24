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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.mzmine.datamodel.features.correlation.OnlineReactionMatch;
import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import io.github.mzmine.datamodel.features.correlation.SpectralSimilarity;
import io.github.mzmine.modules.dataprocessing.id_online_reactivity.OnlineReaction;

/**
 * Sealed JSON DTO for a single row-to-row relationship inside a saved
 * {@link io.github.mzmine.datamodel.features.correlation.R2RNetworkingMaps}. Rows are referenced by
 * their numeric {@code getID()} only.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({ //
    @JsonSubTypes.Type(value = R2RRelationshipDto.SimpleRelationshipDto.class, name = "simple"), //
    @JsonSubTypes.Type(value = R2RRelationshipDto.SimpleSimilarityDto.class, name = "simpleSim"), //
    @JsonSubTypes.Type(value = R2RRelationshipDto.SimpleSimilarityListDto.class, name = "simpleSimList"),
    //
    @JsonSubTypes.Type(value = R2RRelationshipDto.SpectralSimilarityDto.class, name = "spectralSim"),
    //
    @JsonSubTypes.Type(value = R2RRelationshipDto.Ms2GnpsCosineDto.class, name = "ms2GnpsCos"), //
    @JsonSubTypes.Type(value = R2RRelationshipDto.OnlineReactionMatchDto.class, name = "onlineReaction"),
    //
    @JsonSubTypes.Type(value = R2RRelationshipDto.SimpleCorrelationDto.class, name = "correlation")
    //
})
@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface R2RRelationshipDto permits //
    R2RRelationshipDto.SimpleRelationshipDto, //
    R2RRelationshipDto.SimpleSimilarityDto, //
    R2RRelationshipDto.SimpleSimilarityListDto, //
    R2RRelationshipDto.SpectralSimilarityDto, //
    R2RRelationshipDto.Ms2GnpsCosineDto, //
    R2RRelationshipDto.OnlineReactionMatchDto, //
    R2RRelationshipDto.SimpleCorrelationDto {

  int idA();

  int idB();

  /**
   * DTO for {@link io.github.mzmine.datamodel.features.correlation.R2RSimpleCorrelationData}. Full
   * correlation ({@code R2RFullCorrelationData}) is intentionally down-converted to this simple
   * form on save — only the aggregated metrics survive a round-trip.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record SimpleCorrelationDto(int idA, int idB, double totalSim, double heightSim,
                              double avgShapeSim, double minShapeSim, double maxShapeSim,
                              double avgDPcount) implements R2RRelationshipDto {

  }

  /**
   * DTO for {@link io.github.mzmine.datamodel.features.correlation.SimpleRowsRelationship}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record SimpleRelationshipDto(int idA, int idB, double score, String type,
                               String annotation) implements R2RRelationshipDto {

  }

  /**
   * DTO for {@link io.github.mzmine.datamodel.features.correlation.R2RMS2CosineSimilarityGNPS}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record Ms2GnpsCosineDto(int idA, int idB, double cosine, String annotation,
                          String edgeType) implements R2RRelationshipDto {

  }

  /**
   * DTO for {@link OnlineReactionMatch}.
   * <p>
   * {@code isSwappedAB} encodes which of the two id-ordered rows is the educt: {@code false} means
   * idA is the educt, {@code true} means idB is the educt. {@code typeOfThisRow} is preserved as-is
   * for the GUI/table to keep the original educt/product role of the row this match was added to.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record OnlineReactionMatchDto(int idA, int idB, OnlineReaction reaction, boolean isSwappedAB,
                                OnlineReaction.Type typeOfThisRow) implements R2RRelationshipDto {

  }

  /**
   * DTO for {@link io.github.mzmine.datamodel.features.correlation.R2RSimpleSimilarity}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record SimpleSimilarityDto(int idA, int idB, Type type, float similarity) implements
      R2RRelationshipDto {

  }

  /**
   * DTO for {@link io.github.mzmine.datamodel.features.correlation.R2RSimpleSimilarityList}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record SimpleSimilarityListDto(int idA, int idB, Type type, double[] similarities) implements
      R2RRelationshipDto {

  }

  /**
   * DTO for {@link io.github.mzmine.datamodel.features.correlation.R2RSpectralSimilarity}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  record SpectralSimilarityDto(int idA, int idB, Type type,
                               SpectralSimilarity similarity) implements R2RRelationshipDto {

  }
}
