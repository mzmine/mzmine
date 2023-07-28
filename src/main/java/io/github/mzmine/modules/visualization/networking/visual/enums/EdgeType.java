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

package io.github.mzmine.modules.visualization.networking.visual.enums;

import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;
import java.util.Optional;

/**
 * Edge types for the network visualizer and export to graphml
 */
public enum EdgeType implements ElementType {

  FEATURE_CORRELATION, ION_IDENTITY, NETWORK_RELATIONS, MODIFIED_COSINE, MODIFIED_COSINE_NEUTRAL_M, MODIFIED_COSINE_NEUTRAL_M_TO_FEATURE, GNPS_MODIFIED_COSINE;

  public static EdgeType of(Type type) {
    return switch (type) {
      case MS1_FEATURE_CORR -> FEATURE_CORRELATION;
      case ION_IDENTITY_NET -> ION_IDENTITY;
      case MS2_COSINE_SIM -> MODIFIED_COSINE;
      case MS2_NEUTRAL_LOSS_SIM -> NETWORK_RELATIONS;
      case MS2_GNPS_COSINE_SIM -> GNPS_MODIFIED_COSINE;
    };
  }

  /**
   * Some nodes define a special UI class in graph_network_style.css
   *
   * @return the style class or empty
   */
  @Override
  public Optional<String> getUiClass() {
    return Optional.of(switch (this) {
      case FEATURE_CORRELATION -> "FEATURECORR";
      case NETWORK_RELATIONS -> "IINREL";
      case MODIFIED_COSINE_NEUTRAL_M, MODIFIED_COSINE, MODIFIED_COSINE_NEUTRAL_M_TO_FEATURE ->
          "COSINE";
      case GNPS_MODIFIED_COSINE -> "GNPS";
      case ION_IDENTITY -> "IIN";
    });
  }
}
