/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingTask;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.msms.similarity.MS2SimilarityTask;
import io.github.mzmine.modules.dataprocessing.id_gnpsresultsimport.GNPSResultsImportTask;
import io.github.mzmine.util.CorrelationGroupingUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Relationship between two rows - describes the edge in a network where the nodes are {@link
 * FeatureListRow}
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public interface RowsRelationship {

  /**
   * Score of this row 2 row relationship
   *
   * @return the score
   */
  double getScore();

  /**
   * Get m/z delta between the two rows
   * @return b - a
   */
  default double getMzDelta() {
    return getRowB().getAverageMZ() - getRowA().getAverageMZ();
  }

  /**
   * Get absolute m/z delta between the two rows
   * @return absolute m/z delta
   */
  default double getAbsMzDelta() {
    return Math.abs(getRowB().getAverageMZ() - getRowA().getAverageMZ());
  }

  /**
   * A formatted string of the score
   *
   * @return formatted score string
   */
  default String getScoreFormatted() {
    double score = getScore();
    return Double.isNaN(score) ? "NaN"
        : MZmineCore.getConfiguration().getScoreFormat().format(score);
  }

  /**
   * Relationship type
   *
   * @return the type of this relationship
   */
  @NotNull
  Type getType();

  /**
   * The annotation of this row-2-row relationship
   *
   * @return a string representation of this ralationship
   */
  @NotNull
  String getAnnotation();

  /**
   * Row a
   *
   * @return the first row
   */
  FeatureListRow getRowA();

  /**
   * Row b
   *
   * @return the second row
   */
  FeatureListRow getRowB();


  /**
   * All types of relationships
   */
  enum Type {
    /**
     * MS1 similarity can be same retention time, feature shape correlation, intensity across
     * samples. see {@link CorrelateGroupingTask} and {@link CorrelationGroupingUtils}
     */
    MS1_FEATURE_CORR,
    /**
     * Member of the same ion identity network
     */
    ION_IDENTITY_NET,
    /**
     * MS2 spectral similarity, see {@link MS2SimilarityTask}
     */
    MS2_COSINE_SIM,
    /**
     * MS2 similarity of neutral losses see {@link MS2SimilarityTask}
     */
    MS2_NEUTRAL_LOSS_SIM,
    /**
     * GNPS modified cosine similarity, see {@link GNPSResultsImportTask}
     */
    MS2_GNPS_COSINE_SIM;

    @Override
    public String toString() {
      return switch (this) {
        case MS1_FEATURE_CORR -> "MS1 feature correlation";
        case ION_IDENTITY_NET -> "Ion identity network";
        case MS2_COSINE_SIM -> "MS2 cosine similarity";
        case MS2_NEUTRAL_LOSS_SIM -> "MS2 neutral loss cosine similarity";
        case MS2_GNPS_COSINE_SIM -> "MS2 modified cosine similarity (GNPS)";
      };
    }
  }

}
