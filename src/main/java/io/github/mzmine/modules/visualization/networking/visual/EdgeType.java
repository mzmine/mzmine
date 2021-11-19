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

package io.github.mzmine.modules.visualization.networking.visual;

import io.github.mzmine.datamodel.features.correlation.RowsRelationship.Type;

/**
 * Edge types for the network visualizer and export to graphml
 */
public enum EdgeType {

  FEATURE_CORRELATION, ION_IDENTITY, NETWORK_RELATIONS, MS2_SIMILARITY, MS2_SIMILARITY_NEUTRAL_M,
  MS2_SIMILARITY_NEUTRAL_M_TO_FEATURE, MS2_GNPS_COSINE_SIM;

  public static EdgeType of(Type type) {
    return switch (type) {
      case MS1_FEATURE_CORR -> FEATURE_CORRELATION;
      case ION_IDENTITY_NET -> ION_IDENTITY;
      case MS2_COSINE_SIM -> MS2_SIMILARITY;
      case MS2_NEUTRAL_LOSS_SIM -> NETWORK_RELATIONS;
      case MS2_GNPS_COSINE_SIM -> MS2_GNPS_COSINE_SIM;
    };
  }
}
