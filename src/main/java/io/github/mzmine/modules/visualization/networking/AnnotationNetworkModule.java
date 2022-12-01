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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.visualization.networking.visual.FeatureNetworkTab;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class AnnotationNetworkModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "Ion Identity Molecular Networks";
  private static final String MODULE_DESCRIPTION = "Visualize the results of the MS annotation module";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @NotNull
  @Override
  public ExitCode runModule(@NotNull MZmineProject project, @NotNull ParameterSet parameters,
      @NotNull Collection<Task> tasks, @NotNull Instant moduleCallDate) {
    ModularFeatureList[] pkls = parameters.getParameter(AnnotationNetworkParameters.PEAK_LISTS)
        .getValue().getMatchingFeatureLists();
    boolean connectByNetRelations = parameters.getParameter(
        AnnotationNetworkParameters.CONNECT_BY_NET_RELATIONS).getValue();
    boolean onlyBest = parameters.getParameter(AnnotationNetworkParameters.ONLY_BEST_NETWORKS)
        .getValue();
    boolean collapseNodes = parameters.getParameter(AnnotationNetworkParameters.COLLAPSE_NODES)
        .getValue();
    boolean ms2SimEdges = parameters.getParameter(AnnotationNetworkParameters.MS2_SIMILARITY_EDGES)
        .getValue();
    boolean ms1FeatureShapeEdges = parameters.getParameter(
        AnnotationNetworkParameters.MS1_SIMILARITY_EDGES).getValue();
    if (pkls != null && pkls.length > 0) {
      FeatureNetworkTab f = new FeatureNetworkTab(pkls[0], collapseNodes, connectByNetRelations,
          onlyBest, ms2SimEdges, ms1FeatureShapeEdges);
      MZmineCore.getDesktop().addTab(f);
      return ExitCode.OK;
    }
    return ExitCode.ERROR;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONFEATURELIST;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return AnnotationNetworkParameters.class;
  }
}
