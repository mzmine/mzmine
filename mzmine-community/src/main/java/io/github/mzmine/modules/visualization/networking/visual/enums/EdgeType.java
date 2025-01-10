/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
import java.util.List;
import java.util.Optional;

/**
 * Edge types for the network visualizer and export to graphml
 */
public enum EdgeType implements ElementType {

  FEATURE_SHAPE_CORRELATION, ION_IDENTITY, NETWORK_RELATIONS, MS2_MODIFIED_COSINE, GNPS_MODIFIED_COSINE,
  ONLINE_REACTION, MS2Deepscore, DREAMS, OTHER;

  public static EdgeType of(String type) {
    if (type == null || type.isBlank()) {
      return OTHER;
    }

    return of(Type.parse(type));
  }

  public static EdgeType of(Type type) {
    return switch (type) {
      case MS1_FEATURE_CORR -> FEATURE_SHAPE_CORRELATION;
      case ION_IDENTITY_NET -> ION_IDENTITY;
      case MS2_COSINE_SIM -> MS2_MODIFIED_COSINE;
      case MS2_NEUTRAL_LOSS_SIM -> NETWORK_RELATIONS;
      case MS2_GNPS_COSINE_SIM -> GNPS_MODIFIED_COSINE;
      case ONLINE_REACTION -> ONLINE_REACTION;
      case MS2Deepscore -> MS2Deepscore;
      case DREAMS -> DREAMS;
      case OTHER -> OTHER;
      case null -> OTHER;
    };
  }

  public static Type toR2RType(EdgeType type) {
    return switch (type) {
      case FEATURE_SHAPE_CORRELATION -> Type.MS1_FEATURE_CORR;
      case ION_IDENTITY -> Type.ION_IDENTITY_NET;
      case MS2_MODIFIED_COSINE -> Type.MS2_COSINE_SIM;
      case NETWORK_RELATIONS -> Type.MS2_NEUTRAL_LOSS_SIM;
      case GNPS_MODIFIED_COSINE -> Type.MS2_GNPS_COSINE_SIM;
      case ONLINE_REACTION -> Type.ONLINE_REACTION;
      case MS2Deepscore -> Type.MS2Deepscore;
      case DREAMS -> Type.DREAMS;
      case OTHER -> Type.OTHER;
      case null -> Type.OTHER;
    };
  }

  public static List<EdgeType> getDefaultVisibleColumns() {
    return List.of(ION_IDENTITY, NETWORK_RELATIONS, MS2_MODIFIED_COSINE, GNPS_MODIFIED_COSINE,
        MS2Deepscore, DREAMS, OTHER);
  }

  @Override
  public String toString() {
    return toR2RType(this).toString();
  }

  /**
   * Some nodes define a special UI class in graph_network_style.css
   *
   * @return the style class or empty
   */
  @Override
  public Optional<String> getUiClass() {
    return Optional.of(switch (this) {
      case FEATURE_SHAPE_CORRELATION -> "FEATURECORR";
      case NETWORK_RELATIONS -> "IINREL";
      case MS2_MODIFIED_COSINE -> "COSINE";
      case GNPS_MODIFIED_COSINE -> "GNPS";
      case ION_IDENTITY -> "IIN";
      case ONLINE_REACTION -> "IINREL";
      case MS2Deepscore -> "MS2Deepscore";
      case DREAMS -> "DreaMS";
      case OTHER -> "OTHER";
    });
  }
}
