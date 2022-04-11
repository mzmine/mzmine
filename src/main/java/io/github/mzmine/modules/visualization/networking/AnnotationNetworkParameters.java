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

package io.github.mzmine.modules.visualization.networking;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

public class AnnotationNetworkParameters extends SimpleParameterSet {

  /**
   * The data file.
   */
  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final BooleanParameter COLLAPSE_NODES = new BooleanParameter("Collapse nodes",
      "Collapse all nodes into neutral molecule nodes", true);

  public static final BooleanParameter MS1_SIMILARITY_EDGES =
      new BooleanParameter("MS1 feature edges", "Include feature correlation edges", false);


  public static final BooleanParameter MS2_SIMILARITY_EDGES =
      new BooleanParameter("MS2 similarity edges", "Show MS2 similarity edges", true);

  public static final BooleanParameter CONNECT_BY_NET_RELATIONS = new BooleanParameter(
      "Connect by relations", "Connect neutral molecule nodes by network relations", true);

  public static final BooleanParameter ONLY_BEST_NETWORKS =
      new BooleanParameter("Only best networks",
          "Only the networks that only contain first ranked ion identities for all rows", true);

  /**
   * Create the parameter set.
   */
  public AnnotationNetworkParameters() {
    super(new Parameter[]{PEAK_LISTS, ONLY_BEST_NETWORKS, COLLAPSE_NODES, CONNECT_BY_NET_RELATIONS,
        MS1_SIMILARITY_EDGES, MS2_SIMILARITY_EDGES});
  }
}
