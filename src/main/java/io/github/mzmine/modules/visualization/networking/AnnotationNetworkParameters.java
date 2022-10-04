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
        MS1_SIMILARITY_EDGES, MS2_SIMILARITY_EDGES},
        "https://mzmine.github.io/mzmine_documentation/visualization_modules/interactive_ion_id_netw/interactive_ion_id_netw.html");
  }
}
