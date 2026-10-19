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

package io.github.mzmine.modules.visualization.isotope_labeling;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.dataprocessing.id_untargetedLabeling.UntargetedLabelingParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.time.Instant;
import java.util.Collection;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Module for visualizing isotope labeling patterns
 */
public class IsotopeLabelingModule implements MZmineRunnableModule {

  private static final Logger logger = Logger.getLogger(IsotopeLabelingModule.class.getName());
  private static final String MODULE_NAME = "Isotope labeling visualizer";
  private static final String MODULE_DESCRIPTION = "Visualizes isotope labeling patterns from isotope labeling analysis";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONFEATURELIST;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return IsotopeLabelingParameters.class;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks,
      @NotNull Instant moduleCallDate) {

    // Get selected feature lists
    FeatureList[] featureLists = parameters.getParameter(IsotopeLabelingParameters.featureLists)
        .getValue().getMatchingFeatureLists();

    if (featureLists.length == 0) {
      logger.warning("No feature lists selected");
      return ExitCode.ERROR;
    }

    // Process each feature list
    for (FeatureList featureList : featureLists) {
      // Check if it has the required isotope labeling annotations
      if (!featureList.hasRowType(UntargetedLabelingParameters.isotopeClusterType)
          || !featureList.hasRowType(UntargetedLabelingParameters.isotopologueRankType)) {
        logger.warning("Feature list " + featureList.getName()
            + " does not have the required isotope labeling annotations");
        continue;
      }

      logger.info("Creating isotope labeling visualization for " + featureList.getName());

      try {
        // Create the visualization tab
        IsotopeLabelingTab tab = new IsotopeLabelingTab(featureList);

        // Configure visualization parameters - we need to set these on the controller's MODEL directly
        if (tab.getController() != null) {
          // Get visualization parameters
          String visualizationType = parameters.getParameter(
              IsotopeLabelingParameters.visualizationType).getValue();
          int maxIsotopologues = parameters.getParameter(IsotopeLabelingParameters.maxIsotopologues)
              .getValue();

          // Access the model through the controller to set parameters
          tab.getController().getModel().setVisualizationType(visualizationType);
          tab.getController().getModel().setMaxIsotopologues(maxIsotopologues);
        }

        // Add the tab to the desktop
        MZmineCore.getDesktop().addTab(tab);

      } catch (Exception e) {
        logger.severe(
            "Error creating isotope labeling visualization for " + featureList.getName() + ": "
                + e.getMessage());
      }
    }

    return ExitCode.OK;
  }
}