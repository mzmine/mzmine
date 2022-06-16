/*
 * Copyright 2006-2022 The MZmine Development Team
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
