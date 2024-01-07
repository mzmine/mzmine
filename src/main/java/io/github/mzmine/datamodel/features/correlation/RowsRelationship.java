/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.correlation;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.group_metacorrelate.corrgrouping.CorrelateGroupingTask;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralNetworkingTask;
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
     * MS2 spectral similarity, see {@link SpectralNetworkingTask}
     */
    MS2_COSINE_SIM,
    /**
     * MS2 similarity of neutral losses see {@link SpectralNetworkingTask}
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
        case MS2_COSINE_SIM -> "modified MS2 cosine similarity";
        case MS2_NEUTRAL_LOSS_SIM -> "MS2 neutral loss cosine similarity";
        case MS2_GNPS_COSINE_SIM -> "MS2 modified cosine similarity (GNPS)";
      };
    }
  }

}
